param(
    [string[]]$Exclude = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeLogs = Join-Path $repoRoot ".runtime-logs"

$services = @(
    @{ Name = 'registry-server'; Port = 8761; Pom = 'registry-server\pom.xml'; HealthUrl = 'http://localhost:8761/actuator/health'; TimeoutSeconds = 120; Profiles = 'dev' },
    @{ Name = 'config-server'; Port = 8888; Pom = 'config-server\pom.xml'; HealthUrl = 'http://localhost:8888/actuator/health'; TimeoutSeconds = 120; Profiles = 'native' },
    @{ Name = 'auth-service'; Port = 8081; Pom = 'auth-service\pom.xml'; HealthUrl = 'http://localhost:8081/actuator/health'; TimeoutSeconds = 180; Profiles = 'dev' },
    @{ Name = 'user-service'; Port = 8082; Pom = 'user-service\pom.xml'; HealthUrl = 'http://localhost:8082/actuator/health'; TimeoutSeconds = 180; Profiles = 'dev' },
    @{ Name = 'course-service'; Port = 8083; Pom = 'course-service\pom.xml'; HealthUrl = 'http://localhost:8083/actuator/health'; TimeoutSeconds = 180; Profiles = 'dev' },
    @{ Name = 'assignment-service'; Port = 8084; Pom = 'assignment-service\pom.xml'; HealthUrl = 'http://localhost:8084/actuator/health'; TimeoutSeconds = 180; Profiles = 'dev' },
    @{ Name = 'exam-service'; Port = 8085; Pom = 'exam-service\pom.xml'; HealthUrl = 'http://localhost:8085/actuator/health'; TimeoutSeconds = 180; Profiles = 'dev' },
    @{ Name = 'analysis-service'; Port = 8086; Pom = 'analysis-service\pom.xml'; HealthUrl = 'http://localhost:8086/actuator/health'; TimeoutSeconds = 180; Profiles = 'dev' },
    @{ Name = 'notification-service'; Port = 8087; Pom = 'notification-service\pom.xml'; HealthUrl = 'http://localhost:8087/actuator/health'; TimeoutSeconds = 180; Profiles = 'dev' },
    @{ Name = 'ai-service'; Port = 8088; Pom = 'ai-service\pom.xml'; HealthUrl = 'http://localhost:8088/actuator/health'; TimeoutSeconds = 180; Profiles = 'dev' },
    @{ Name = 'legacy-adapter'; Port = 8091; Pom = 'legacy-adapter\pom.xml'; HealthUrl = 'http://localhost:8091/actuator/health'; TimeoutSeconds = 180; Profiles = 'dev' },
    @{ Name = 'agent-service'; Port = 8092; Pom = 'agent-service\pom.xml'; HealthUrl = 'http://localhost:8092/actuator/health'; TimeoutSeconds = 180; Profiles = 'dev' },
    @{ Name = 'gateway'; Port = 8080; Pom = 'gateway\pom.xml'; HealthUrl = 'http://localhost:8080/actuator/health'; TimeoutSeconds = 180; Profiles = 'dev' }
)

if (-not (Test-Path $runtimeLogs)) {
    New-Item -ItemType Directory -Path $runtimeLogs | Out-Null
}

function Normalize-Name {
    param([string]$Value)

    return $Value.Trim().ToLowerInvariant()
}

$script:ExcludedTcpPortRanges = $null

function Get-ExcludedTcpPortRanges {
    if ($null -ne $script:ExcludedTcpPortRanges) {
        return $script:ExcludedTcpPortRanges
    }

    $ranges = @()
    foreach ($protocol in @('ipv4', 'ipv6')) {
        try {
            $output = netsh interface $protocol show excludedportrange protocol=tcp 2>$null
            if ($LASTEXITCODE -ne 0) {
                continue
            }

            foreach ($line in $output) {
                if ($line -match '^\s*(\d+)\s+(\d+)\s*(\*)?\s*$') {
                    $ranges += [pscustomobject]@{
                        Protocol  = $protocol
                        StartPort = [int]$Matches[1]
                        EndPort   = [int]$Matches[2]
                    }
                }
            }
        } catch {
        }
    }

    $script:ExcludedTcpPortRanges = @($ranges)
    return $script:ExcludedTcpPortRanges
}

function Test-PortExcluded {
    param([int]$Port)

    foreach ($range in Get-ExcludedTcpPortRanges) {
        if ($Port -ge $range.StartPort -and $Port -le $range.EndPort) {
            return $true
        }
    }

    return $false
}

function Find-AvailablePort {
    param(
        [int]$PreferredPort,
        [System.Collections.Generic.HashSet[int]]$ReservedPorts,
        [int]$MaxOffset = 200
    )

    for ($offset = 0; $offset -le $MaxOffset; $offset++) {
        $candidate = $PreferredPort + $offset
        if (Test-PortExcluded -Port $candidate) {
            continue
        }

        if ($ReservedPorts -and $ReservedPorts.Contains($candidate)) {
            continue
        }

        return $candidate
    }

    throw "Unable to find an available local port near $PreferredPort after checking $MaxOffset offsets."
}

function Initialize-ServiceRuntimePorts {
    param([hashtable[]]$Services)

    $reservedPorts = [System.Collections.Generic.HashSet[int]]::new()
    foreach ($service in $Services) {
        $preferredPort = [int]$service.Port
        $runtimePort = Find-AvailablePort -PreferredPort $preferredPort -ReservedPorts $reservedPorts
        $null = $reservedPorts.Add($runtimePort)
        $service.RuntimePort = $runtimePort
        $service.RuntimeHealthUrl = $service.HealthUrl -replace [regex]::Escape(":$preferredPort/"), ":$runtimePort/"
        if ($runtimePort -ne $preferredPort) {
            Write-Host ("Adjusted {0} port from {1} to {2} because port {1} is unavailable on this machine." -f $service.Name, $preferredPort, $runtimePort)
        }
    }
}

function Resolve-LauncherShell {
    $pwshCommand = Get-Command pwsh -ErrorAction SilentlyContinue
    if ($pwshCommand) {
        return $pwshCommand.Source
    }

    $powershellCommand = Get-Command powershell -ErrorAction SilentlyContinue
    if ($powershellCommand) {
        return $powershellCommand.Source
    }

    throw "PowerShell executable was not found."
}

function Stop-RepoOwnedJavaProcesses {
    param([string[]]$ServicePomPaths)

    $servicePomTokens = @($ServicePomPaths | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $repoProcesses = @(
        Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
            Where-Object {
                $_.Name -eq 'java.exe' -and
                -not [string]::IsNullOrWhiteSpace($_.CommandLine) -and
                $_.CommandLine.Contains($repoRoot) -and
                (
                    $_.CommandLine.Contains('spring-boot:run') -or
                    $_.CommandLine.Contains('spring-boot-')
                )
            }
    )

    if ($servicePomTokens.Count -gt 0) {
        $repoProcesses = @(
            $repoProcesses | Where-Object {
                $commandLine = $_.CommandLine
                $servicePomTokens | Where-Object { $commandLine.Contains($_) }
            }
        )
    }

    foreach ($process in $repoProcesses) {
        try {
            Stop-Process -Id $process.ProcessId -Force -ErrorAction Stop
            Write-Host ("Stopped repo-owned Java process before build install: PID {0}" -f $process.ProcessId)
        } catch {
            throw "Failed to stop repo-owned Java process PID $($process.ProcessId) before shared Maven install. $($_.Exception.Message)"
        }
    }
}

function Stop-SelectedServicePortProcesses {
    param([hashtable[]]$Services)

    foreach ($service in $Services) {
        $port = if ($service.ContainsKey('RuntimePort')) { [int]$service.RuntimePort } else { [int]$service.Port }
        $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
        foreach ($listener in $listeners) {
            try {
                Stop-Process -Id $listener.OwningProcess -Force -ErrorAction Stop
                Write-Host ("Stopped process listening on {0} port {1}: PID {2}" -f $service.Name, $port, $listener.OwningProcess)
            } catch {
                throw "Failed to stop process on port $port for $($service.Name) before shared Maven install. $($_.Exception.Message)"
            }
        }
    }
}

function Get-EnvironmentVariableValue {
    param([string]$Name)

    foreach ($target in @(
        [System.EnvironmentVariableTarget]::Process,
        [System.EnvironmentVariableTarget]::User,
        [System.EnvironmentVariableTarget]::Machine
    )) {
        $value = [System.Environment]::GetEnvironmentVariable($Name, $target)
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            return [pscustomobject]@{
                Name   = $Name
                Value  = $value
                Source = $target.ToString()
            }
        }
    }

    return $null
}

function Get-AgentLlmApiKeyEnvVarNames {
    $candidateFiles = @(
        (Join-Path $repoRoot "config-server\src\main\resources\config-repo\agent-service.yml"),
        (Join-Path $repoRoot "agent-service\src\main\resources\application.yml")
    )

    foreach ($candidateFile in $candidateFiles) {
        if (-not (Test-Path -LiteralPath $candidateFile)) {
            continue
        }

        foreach ($line in Get-Content -LiteralPath $candidateFile) {
            if ($line -notmatch '^\s*api-key:\s*(.+?)\s*$') {
                continue
            }

            $envVarNames = @()
            $placeholder = $Matches[1]
            foreach ($match in [System.Text.RegularExpressions.Regex]::Matches($placeholder, '\$\{([A-Z][A-Z0-9_-]*)[:}]')) {
                $name = $match.Groups[1].Value
                if (-not [string]::IsNullOrWhiteSpace($name) -and $envVarNames -notcontains $name) {
                    $envVarNames += $name
                }
            }

            if ($envVarNames.Count -gt 0) {
                return $envVarNames
            }
        }
    }

    throw "Unable to determine Agent LLM API key env var names from config. Expected an api-key placeholder in config-server\src\main\resources\config-repo\agent-service.yml or agent-service\src\main\resources\application.yml."
}

function Resolve-AgentLlmStartupState {
    $apiKeyEnvVarNames = Get-AgentLlmApiKeyEnvVarNames
    $apiKeyBinding = $null
    foreach ($name in $apiKeyEnvVarNames) {
        $binding = Get-EnvironmentVariableValue -Name $name
        if ($binding) {
            $apiKeyBinding = $binding
            break
        }
    }

    $explicitEnabled = [System.Environment]::GetEnvironmentVariable('AGENT_LLM_ENABLED', [System.EnvironmentVariableTarget]::Process)
    if (-not [string]::IsNullOrWhiteSpace($explicitEnabled)) {
        $normalized = $explicitEnabled.Trim().ToLowerInvariant()
        if ($normalized -eq 'false') {
            return [pscustomobject]@{
                Enabled        = 'false'
                ApiKeyEnvVar   = if ($apiKeyBinding) { $apiKeyBinding.Name } else { $null }
                ApiKeyValue    = if ($apiKeyBinding) { $apiKeyBinding.Value } else { $null }
                CheckedEnvVars = $apiKeyEnvVarNames
                Reason         = "Agent LLM disabled because AGENT_LLM_ENABLED=false was set explicitly in the current shell."
            }
        }

        if ($normalized -ne 'true') {
            throw "AGENT_LLM_ENABLED must be 'true' or 'false' when set explicitly in the current shell."
        }

        if (-not $apiKeyBinding) {
            throw "AGENT_LLM_ENABLED=true was set explicitly, but none of the configured Agent LLM API key env vars are set. Checked: $($apiKeyEnvVarNames -join ', ')"
        }

        return [pscustomobject]@{
            Enabled        = 'true'
            ApiKeyEnvVar   = $apiKeyBinding.Name
            ApiKeyValue    = $apiKeyBinding.Value
            CheckedEnvVars = $apiKeyEnvVarNames
            Reason         = "Agent LLM enabled because AGENT_LLM_ENABLED=true was set explicitly and API key env var '$($apiKeyBinding.Name)' was found."
        }
    }

    if ($apiKeyBinding) {
        return [pscustomobject]@{
            Enabled        = 'true'
            ApiKeyEnvVar   = $apiKeyBinding.Name
            ApiKeyValue    = $apiKeyBinding.Value
            CheckedEnvVars = $apiKeyEnvVarNames
            Reason         = "Agent LLM enabled automatically because API key env var '$($apiKeyBinding.Name)' was found from the current config."
        }
    }

    return [pscustomobject]@{
        Enabled        = 'false'
        ApiKeyEnvVar   = $null
        ApiKeyValue    = $null
        CheckedEnvVars = $apiKeyEnvVarNames
        Reason         = "Agent LLM disabled automatically because none of the configured API key env vars are set. Checked: $($apiKeyEnvVarNames -join ', ')"
    }
}

function Resolve-AgentRagStartupState {
    $explicitEnabled = [System.Environment]::GetEnvironmentVariable('AGENT_RAG_ENABLED', [System.EnvironmentVariableTarget]::Process)
    if (-not [string]::IsNullOrWhiteSpace($explicitEnabled)) {
        $normalized = $explicitEnabled.Trim().ToLowerInvariant()
        if ($normalized -eq 'false') {
            return [pscustomobject]@{
                Enabled = 'false'
                Reason  = 'Agent RAG disabled because AGENT_RAG_ENABLED=false was set explicitly in the current shell.'
            }
        }

        if ($normalized -ne 'true') {
            throw "AGENT_RAG_ENABLED must be 'true' or 'false' when set explicitly in the current shell."
        }

        return [pscustomobject]@{
            Enabled = 'true'
            Reason  = 'Agent RAG enabled because AGENT_RAG_ENABLED=true was set explicitly in the current shell.'
        }
    }

    return [pscustomobject]@{
        Enabled = 'true'
        Reason  = 'Agent RAG enabled automatically for the local dev stack.'
    }
}

function Resolve-AgentQuestionBankStartupState {
    $explicitEnabled = [System.Environment]::GetEnvironmentVariable('AGENT_QUESTION_BANK_ENABLED', [System.EnvironmentVariableTarget]::Process)
    if (-not [string]::IsNullOrWhiteSpace($explicitEnabled)) {
        $normalized = $explicitEnabled.Trim().ToLowerInvariant()
        if ($normalized -eq 'false') {
            return [pscustomobject]@{
                Enabled = 'false'
                Reason  = 'Agent question bank disabled because AGENT_QUESTION_BANK_ENABLED=false was set explicitly in the current shell.'
            }
        }

        if ($normalized -ne 'true') {
            throw "AGENT_QUESTION_BANK_ENABLED must be 'true' or 'false' when set explicitly in the current shell."
        }

        return [pscustomobject]@{
            Enabled = 'true'
            Reason  = 'Agent question bank enabled because AGENT_QUESTION_BANK_ENABLED=true was set explicitly in the current shell.'
        }
    }

    return [pscustomobject]@{
        Enabled = 'true'
        Reason  = 'Agent question bank enabled automatically for the local dev stack.'
    }
}

function Stop-PortProcess {
    param([int]$Port)

    $listeners = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        try {
            Stop-Process -Id $listener.OwningProcess -Force -ErrorAction Stop
        } catch {
            throw "Failed to stop process on port $Port. $($_.Exception.Message)"
        }
    }
}

function Get-ListeningProcessDetails {
    param([int]$Port)

    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
    if ($listeners.Count -eq 0) {
        return @()
    }

    $processMap = @{}
    $processIds = @($listeners | Select-Object -ExpandProperty OwningProcess -Unique)
    if ($processIds.Count -gt 0) {
        $processes = @(Get-CimInstance Win32_Process | Where-Object { $processIds -contains $_.ProcessId })
        foreach ($process in $processes) {
            $processMap[[int]$process.ProcessId] = $process
        }
    }

    return @(
        foreach ($listener in $listeners) {
            $process = $null
            if ($processMap.ContainsKey([int]$listener.OwningProcess)) {
                $process = $processMap[[int]$listener.OwningProcess]
            }

            [pscustomobject]@{
                ProcessId   = $listener.OwningProcess
                ProcessName = if ($process) { $process.Name } else { 'unknown' }
                CommandLine = if ($process) { $process.CommandLine } else { 'unknown' }
            }
        }
    )
}

function Test-RepoOwnedProcess {
    param([pscustomobject]$ProcessInfo)

    if (-not $ProcessInfo) {
        return $false
    }

    if ([string]::IsNullOrWhiteSpace($ProcessInfo.CommandLine)) {
        return $false
    }

    $commandLine = $ProcessInfo.CommandLine
    return (
        $commandLine.Contains($repoRoot) -or
        $commandLine.Contains('spring-boot:run') -or
        $commandLine.Contains('start-dev-local-stack.ps1') -or
        $commandLine.Contains('registry-server\pom.xml') -or
        $commandLine.Contains('config-server\pom.xml') -or
        $commandLine.Contains('gateway\pom.xml')
    )
}

function Get-PortConflictMessage {
    param([int]$Port)

    $details = @(Get-ListeningProcessDetails -Port $Port)
    if ($details.Count -eq 0) {
        return $null
    }

    $detailText = ($details | ForEach-Object {
        "PID=$($_.ProcessId); Name=$($_.ProcessName); CommandLine=$($_.CommandLine)"
    }) -join ' | '

    return " Conflicting listeners: $detailText"
}

function Test-LogContainsPortInUse {
    param(
        [string]$LogPath,
        [int]$Port
    )

    if (-not (Test-Path -LiteralPath $LogPath)) {
        return $false
    }

    $pattern = "Port {0} was already in use" -f $Port
    return Select-String -Path $LogPath -Pattern $pattern -Quiet
}

function Resolve-ConflictingPort {
    param(
        [int]$Port,
        [string]$ServiceName
    )

    $details = @(Get-ListeningProcessDetails -Port $Port)
    if ($details.Count -eq 0) {
        return $false
    }

    $externalListeners = @($details | Where-Object { -not (Test-RepoOwnedProcess -ProcessInfo $_) })
    if ($externalListeners.Count -gt 0) {
        return $false
    }

    foreach ($detail in $details) {
        try {
            Stop-Process -Id $detail.ProcessId -Force -ErrorAction Stop
            Write-Host ("Stopped repo-owned listener for {0} on port {1}: PID {2} ({3})" -f $ServiceName, $Port, $detail.ProcessId, $detail.ProcessName)
        } catch {
            throw "Failed to stop repo-owned listener for $ServiceName on port $Port. PID=$($detail.ProcessId). $($_.Exception.Message)"
        }
    }

    Wait-PortReleased -Port $Port
    return $true
}

function Wait-PortReleased {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $listeners = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
        if (-not $listeners) {
            return
        }

        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)

    $conflictMessage = Get-PortConflictMessage -Port $Port
    if ($conflictMessage) {
        throw "Port $Port is still in use after waiting $TimeoutSeconds seconds.$conflictMessage"
    }
    throw "Port $Port is still in use after waiting $TimeoutSeconds seconds."
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [string]$Label,
        [int]$TimeoutSeconds = 120,
        [System.Diagnostics.Process]$Process = $null,
        [int]$Port = 0,
        [string]$OutLog = ''
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if ($Process -and $Process.HasExited) {
            $exitCodeText = if ($null -ne $Process.ExitCode) { $Process.ExitCode } else { 'unknown' }
            $unknownExitCodeLabel = 'Exit code: unknown'
            $conflictMessage = $null
            if ($Port -gt 0) {
                $conflictMessage = Get-PortConflictMessage -Port $Port
            }
            if ($conflictMessage) {
                throw "$Label exited before becoming healthy at $Url. Exit code: $exitCodeText.$conflictMessage"
            }
            if ($Port -gt 0 -and -not [string]::IsNullOrWhiteSpace($OutLog) -and (Test-LogContainsPortInUse -LogPath $OutLog -Port $Port)) {
                throw "$Label exited before becoming healthy at $Url. Exit code: $exitCodeText. Port $Port was already in use according to the service log."
            }
            if ($exitCodeText -eq 'unknown') {
                throw "$Label exited before becoming healthy at $Url. $unknownExitCodeLabel."
            }
            throw "$Label exited before becoming healthy at $Url. Exit code: $exitCodeText."
        }

        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                return
            }
        } catch {
        }

        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "$Label did not become healthy at $Url within $TimeoutSeconds seconds."
}

function Wait-ServiceHealthy {
    param(
        [hashtable]$Service,
        [System.Diagnostics.Process]$Process,
        [string]$OutLog
    )

    try {
        $healthUrl = if ($Service.ContainsKey('RuntimeHealthUrl')) { $Service.RuntimeHealthUrl } else { $Service.HealthUrl }
        $port = if ($Service.ContainsKey('RuntimePort')) { [int]$Service.RuntimePort } else { [int]$Service.Port }
        Wait-HttpOk -Url $healthUrl -Label $Service.Name -TimeoutSeconds $Service.TimeoutSeconds -Process $Process -Port $port -OutLog $OutLog
    } catch {
        $message = $_.Exception.Message
        $port = if ($Service.ContainsKey('RuntimePort')) { [int]$Service.RuntimePort } else { [int]$Service.Port }
        if ($message -like "*exited before becoming healthy*" -and $message -like "*Conflicting listeners:*") {
            $resolved = Resolve-ConflictingPort -Port $port -ServiceName $Service.Name
            if ($resolved) {
                return $false
            }
        }
        if ($message -like "*Port $port was already in use according to the service log.*") {
            Wait-PortReleased -Port $port -TimeoutSeconds 5
            return $false
        }

        throw
    }

    return $true
}

function Wait-DockerContainerHealthy {
    param(
        [string]$ContainerName,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $status = (docker inspect --format "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}" $ContainerName 2>$null).Trim()
            if ($LASTEXITCODE -ne 0) {
                throw "Failed to inspect Docker container $ContainerName. Ensure Docker Desktop is running."
            }
            if ($status -eq 'healthy' -or $status -eq 'running') {
                return
            }
        } catch {
            if ($_.Exception.Message -like 'Failed to inspect Docker container*') {
                throw
            }
        }

        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "Docker container $ContainerName did not become healthy within $TimeoutSeconds seconds."
}

function Start-ServiceProcess {
    param(
        [hashtable]$Service,
        [string]$LauncherShell,
        [pscustomobject]$AgentLlmStartupState,
        [pscustomobject]$AgentRagStartupState,
        [pscustomobject]$AgentQuestionBankStartupState,
        [string]$ConfigServerUrl,
        [string]$EurekaServerUrl,
        [string]$AgentServiceUrl
    )

    $serviceName = $Service.Name
    $outLog = Join-Path $runtimeLogs "$serviceName.out.log"
    $errLog = Join-Path $runtimeLogs "$serviceName.err.log"
    $launcherScript = Join-Path $runtimeLogs "$serviceName.launcher.ps1"
    $pomPath = Join-Path $repoRoot $Service.Pom
    $runtimePort = if ($Service.ContainsKey('RuntimePort')) { [int]$Service.RuntimePort } else { [int]$Service.Port }
    $commandLines = @(
        "`$env:SERVER_PORT = '$runtimePort'",
        "`$env:SPRING_PROFILES_ACTIVE = '$($Service.Profiles)'",
        "`$env:CONFIG_SERVER_URL = '$ConfigServerUrl'",
        "`$env:EUREKA_SERVER_URL = '$EurekaServerUrl'"
    )
    if ($serviceName -eq 'agent-service') {
        $commandLines += "`$env:AGENT_LLM_ENABLED = '$($AgentLlmStartupState.Enabled)'"
        $commandLines += "`$env:AGENT_RAG_ENABLED = '$($AgentRagStartupState.Enabled)'"
        $commandLines += "`$env:AGENT_QUESTION_BANK_ENABLED = '$($AgentQuestionBankStartupState.Enabled)'"
        if ($AgentLlmStartupState.Enabled -eq 'true' -and -not [string]::IsNullOrWhiteSpace($AgentLlmStartupState.ApiKeyEnvVar)) {
            $escapedApiKeyValue = $AgentLlmStartupState.ApiKeyValue.Replace("'", "''")
            $commandLines += ("[System.Environment]::SetEnvironmentVariable('{0}', '{1}', [System.EnvironmentVariableTarget]::Process)" -f $AgentLlmStartupState.ApiKeyEnvVar, $escapedApiKeyValue)
        }
    }
    if ($serviceName -eq 'gateway' -and -not [string]::IsNullOrWhiteSpace($AgentServiceUrl)) {
        $commandLines += "`$env:AGENT_SERVICE_URL = '$AgentServiceUrl'"
    }
    $commandLines += "& mvn -f '$pomPath' spring-boot:run"

    Stop-PortProcess -Port $runtimePort
    Wait-PortReleased -Port $runtimePort
    Remove-Item $outLog, $errLog, $launcherScript -Force -ErrorAction SilentlyContinue
    Set-Content -LiteralPath $launcherScript -Value $commandLines -Encoding UTF8

    return Start-Process -FilePath $LauncherShell `
        -ArgumentList @('-NoLogo', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"{0}"' -f $launcherScript)) `
        -WorkingDirectory $repoRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -PassThru
}

$knownServiceNames = @{}
foreach ($service in $services) {
    $knownServiceNames[(Normalize-Name $service.Name)] = $true
}

$excludeEntries = @()
foreach ($entry in $Exclude) {
    foreach ($name in $entry.Split(',', [System.StringSplitOptions]::RemoveEmptyEntries)) {
        $excludeEntries += $name.Trim()
    }
}

$excludedNames = @{}
foreach ($name in $excludeEntries) {
    $normalized = Normalize-Name $name
    if (-not $knownServiceNames.ContainsKey($normalized)) {
        throw "Unknown service name in -Exclude: $name"
    }
    $excludedNames[$normalized] = $true
}

$selectedServices = @(
    foreach ($service in $services) {
        if (-not $excludedNames.ContainsKey((Normalize-Name $service.Name))) {
            $service
        }
    }
)

Initialize-ServiceRuntimePorts -Services $selectedServices

$launcherShell = Resolve-LauncherShell
$agentLlmStartupState = Resolve-AgentLlmStartupState
Write-Host $agentLlmStartupState.Reason
$agentRagStartupState = Resolve-AgentRagStartupState
Write-Host $agentRagStartupState.Reason
$agentQuestionBankStartupState = Resolve-AgentQuestionBankStartupState
Write-Host $agentQuestionBankStartupState.Reason

Write-Host "Starting Docker infrastructure: mysql, redis, rabbitmq"
& (Join-Path $repoRoot "scripts\start-dev-infra.ps1") -Services @('mysql', 'redis', 'rabbitmq')
Wait-DockerContainerHealthy -ContainerName 'qimo-mysql' -TimeoutSeconds 120
Wait-DockerContainerHealthy -ContainerName 'qimo-redis' -TimeoutSeconds 60
Wait-DockerContainerHealthy -ContainerName 'qimo-rabbitmq' -TimeoutSeconds 120

Write-Host "Starting frontend dev server on http://localhost:5500"
& (Join-Path $repoRoot "scripts\start-idea-dev-frontend.ps1") -SkipDocker -NoBrowser
Wait-HttpOk -Url 'http://localhost:5500' -Label 'frontend dev server' -TimeoutSeconds 30

Stop-SelectedServicePortProcesses -Services $selectedServices
Stop-RepoOwnedJavaProcesses -ServicePomPaths ($selectedServices | ForEach-Object { Join-Path $repoRoot $_.Pom })

Write-Host "Building shared Maven modules once: mvn -DskipTests install"
Push-Location $repoRoot
try {
    mvn -DskipTests install
} finally {
    Pop-Location
}

$registryService = $selectedServices | Where-Object { $_.Name -eq 'registry-server' } | Select-Object -First 1
$configServerService = $selectedServices | Where-Object { $_.Name -eq 'config-server' } | Select-Object -First 1
$runtimeRegistryPort = if ($registryService) { [int]$registryService.RuntimePort } else { 8761 }
$runtimeConfigServerPort = if ($configServerService) { [int]$configServerService.RuntimePort } else { 8888 }
$runtimeEurekaServerUrl = "http://localhost:$runtimeRegistryPort/eureka/"
$runtimeConfigServerUrl = "http://localhost:$runtimeConfigServerPort"
$agentService = $selectedServices | Where-Object { $_.Name -eq 'agent-service' } | Select-Object -First 1
$runtimeAgentServicePort = if ($agentService) { [int]$agentService.RuntimePort } else { 8092 }
$runtimeAgentServiceUrl = "http://localhost:$runtimeAgentServicePort"

foreach ($service in $selectedServices) {
    $runtimePort = if ($service.ContainsKey('RuntimePort')) { [int]$service.RuntimePort } else { [int]$service.Port }
    Write-Host ("Starting {0} on port {1}" -f $service.Name, $runtimePort)
    $attempt = 0
    $healthy = $false
    do {
        $attempt++
        $process = Start-ServiceProcess -Service $service -LauncherShell $launcherShell -AgentLlmStartupState $agentLlmStartupState -AgentRagStartupState $agentRagStartupState -AgentQuestionBankStartupState $agentQuestionBankStartupState -ConfigServerUrl $runtimeConfigServerUrl -EurekaServerUrl $runtimeEurekaServerUrl -AgentServiceUrl $runtimeAgentServiceUrl
        $outLog = Join-Path $runtimeLogs "$($service.Name).out.log"

        try {
            $healthy = Wait-ServiceHealthy -Service $service -Process $process -OutLog $outLog
        } catch {
            $errLog = Join-Path $runtimeLogs "$($service.Name).err.log"
            throw "$($_.Exception.Message) Check logs: $outLog ; $errLog"
        }
    } while (-not $healthy -and $attempt -lt 2)
}

Write-Host "Local dev stack is running."
Write-Host "Frontend: http://localhost:5500"
if (-not $excludedNames.ContainsKey('gateway')) {
    Write-Host "Gateway:  http://localhost:8080"
}
if (-not $excludedNames.ContainsKey('config-server')) {
    Write-Host ("Config:   http://localhost:{0}" -f $runtimeConfigServerPort)
}
if (-not $excludedNames.ContainsKey('registry-server')) {
    Write-Host ("Eureka:   http://localhost:{0}" -f $runtimeRegistryPort)
}
Write-Host "Logs:     $runtimeLogs"
