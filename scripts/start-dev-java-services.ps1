param(
    [string[]]$Services = @(),
    [switch]$All,
    [switch]$SkipInfra,
    [switch]$SkipBuild,
    [switch]$NoWait,
    [int]$ReadyTimeoutSeconds = 120,
    [int]$StartGapSeconds = 3
)

# Examples:
#   .\scripts\start-dev-java-services.ps1
#   .\scripts\start-dev-java-services.ps1 -Services auth-service,user-service,gateway
#   .\scripts\start-dev-java-services.ps1 -All
#   .\scripts\start-dev-java-services.ps1 -SkipInfra -SkipBuild -Services gateway

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$logRoot = Join-Path $repoRoot ".runtime-logs\java-services"

$serviceOrder = @(
    "registry-server",
    "config-server",
    "auth-service",
    "user-service",
    "course-service",
    "assignment-service",
    "exam-service",
    "analysis-service",
    "notification-service",
    "ai-service",
    "agent-service",
    "legacy-adapter",
    "gateway",
    "major_assignment"
)

$defaultServices = @(
    "registry-server",
    "config-server",
    "auth-service",
    "user-service",
    "course-service",
    "assignment-service",
    "exam-service",
    "gateway"
)

$servicePorts = @{
    "registry-server" = 8761
    "config-server" = 8888
    "auth-service" = 8081
    "user-service" = 8082
    "course-service" = 8083
    "assignment-service" = 8084
    "exam-service" = 8085
    "analysis-service" = 8086
    "notification-service" = 8087
    "ai-service" = 8088
    "agent-service" = 8092
    "legacy-adapter" = 8091
    "gateway" = 8080
    "major_assignment" = 18080
}

$serviceJarNames = @{
    "registry-server" = "registry-server-0.1.0-SNAPSHOT.jar"
    "config-server" = "config-server-0.1.0-SNAPSHOT.jar"
    "auth-service" = "auth-service-0.1.0-SNAPSHOT.jar"
    "user-service" = "user-service-0.1.0-SNAPSHOT.jar"
    "course-service" = "course-service-0.1.0-SNAPSHOT.jar"
    "assignment-service" = "assignment-service-0.1.0-SNAPSHOT.jar"
    "exam-service" = "exam-service-0.1.0-SNAPSHOT.jar"
    "analysis-service" = "analysis-service-0.1.0-SNAPSHOT.jar"
    "notification-service" = "notification-service-0.1.0-SNAPSHOT.jar"
    "ai-service" = "ai-service-0.1.0-SNAPSHOT.jar"
    "agent-service" = "agent-service-0.1.0-SNAPSHOT.jar"
    "legacy-adapter" = "legacy-adapter-0.1.0-SNAPSHOT.jar"
    "gateway" = "gateway-0.1.0-SNAPSHOT.jar"
    "major_assignment" = "major_assignment-0.0.1-SNAPSHOT.jar"
}

$serviceProfiles = @{
    "config-server" = "native"
}

function Resolve-PowerShell {
    $pwsh = Get-Command pwsh -ErrorAction SilentlyContinue
    if ($pwsh) {
        return $pwsh.Source
    }

    $powershell = Get-Command powershell -ErrorAction SilentlyContinue
    if ($powershell) {
        return $powershell.Source
    }

    throw "PowerShell executable was not found."
}

function Test-PortListening {
    param([int]$Port)

    $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
    return [bool]$listener
}

function Wait-PortListening {
    param(
        [string]$ServiceName,
        [int]$Port,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if (Test-PortListening -Port $Port) {
            Write-Host ("{0} is listening on port {1}" -f $ServiceName, $Port)
            return
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw ("{0} did not listen on port {1} within {2} seconds. Check .runtime-logs\java-services\{0}.err.log" -f $ServiceName, $Port, $TimeoutSeconds)
}

function Get-TargetServices {
    if ($All) {
        return $serviceOrder
    }

    if ($Services.Count -eq 0) {
        return $defaultServices
    }

    $unknown = $Services | Where-Object { $serviceOrder -notcontains $_ }
    if ($unknown) {
        throw ("Unknown service name(s): {0}. Known services: {1}" -f (($unknown -join ", ")), ($serviceOrder -join ", "))
    }

    $selected = New-Object System.Collections.Generic.HashSet[string]
    foreach ($service in $Services) {
        [void]$selected.Add($service)
    }

    if (($selected.Count -gt 0) -and (-not $selected.Contains("registry-server"))) {
        [void]$selected.Add("registry-server")
    }
    if (($selected.Count -gt 0) -and (-not $selected.Contains("config-server"))) {
        [void]$selected.Add("config-server")
    }

    return $serviceOrder | Where-Object { $selected.Contains($_) }
}

function Build-JavaServices {
    param([string[]]$TargetServices)

    if ($SkipBuild) {
        Write-Host "Skipping Maven build because -SkipBuild was specified."
        return
    }

    $moduleList = $TargetServices -join ","
    Write-Host ("Building Java service modules: {0}" -f $moduleList)
    Push-Location $repoRoot
    try {
        & mvn -pl $moduleList -am -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw ("Maven package failed with exit code {0}" -f $LASTEXITCODE)
        }
    } finally {
        Pop-Location
    }
}

function Start-JavaService {
    param(
        [string]$ServiceName,
        [string]$PowerShellExe
    )

    $port = $servicePorts[$ServiceName]
    if (Test-PortListening -Port $port) {
        Write-Warning ("Skipping {0}; port {1} is already listening." -f $ServiceName, $port)
        return $null
    }

    $outLog = Join-Path $logRoot ("{0}.out.log" -f $ServiceName)
    $errLog = Join-Path $logRoot ("{0}.err.log" -f $ServiceName)
    Remove-Item $outLog, $errLog -Force -ErrorAction SilentlyContinue

    $escapedRepoRoot = $repoRoot.Replace("'", "''")
    $jarPath = Join-Path $repoRoot (Join-Path $ServiceName (Join-Path "target" $serviceJarNames[$ServiceName]))
    if (-not (Test-Path $jarPath)) {
        throw ("Jar not found for {0}: {1}. Run without -SkipBuild first." -f $ServiceName, $jarPath)
    }

    $escapedJarPath = $jarPath.Replace("'", "''")
    $springProfile = if ($serviceProfiles.ContainsKey($ServiceName)) { $serviceProfiles[$ServiceName] } else { "dev" }
    $command = @"
`$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath '$escapedRepoRoot'
`$env:SPRING_PROFILES_ACTIVE = '$springProfile'
`$env:CONFIG_SERVER_URL = 'http://localhost:8888'
`$env:EUREKA_SERVER_URL = 'http://localhost:8761/eureka/'
`$env:REDIS_HOST = 'localhost'
`$env:REDIS_PORT = '6379'
`$env:RABBITMQ_HOST = 'localhost'
`$env:RABBITMQ_PORT = '5672'
java -jar '$escapedJarPath'
"@

    $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))
    $process = Start-Process -FilePath $PowerShellExe `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", $encodedCommand) `
        -WorkingDirectory $repoRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -PassThru

    Write-Host ("Started {0} on port {1}. PID={2}" -f $ServiceName, $port, $process.Id)
    Write-Host ("  logs: {0}" -f $outLog)

    return $process
}

if (-not (Test-Path $logRoot)) {
    New-Item -ItemType Directory -Path $logRoot | Out-Null
}

if (-not $SkipInfra) {
    & (Join-Path $repoRoot "scripts\start-dev-infra.ps1")
}

$targetServices = @(Get-TargetServices)
$powerShellExe = Resolve-PowerShell
$started = @()

Write-Host ("Starting Java services: {0}" -f ($targetServices -join ", "))
Build-JavaServices -TargetServices $targetServices

foreach ($service in $targetServices) {
    $process = Start-JavaService -ServiceName $service -PowerShellExe $powerShellExe
    if ($process) {
        $started += [pscustomobject]@{
            Service = $service
            Port = $servicePorts[$service]
            ProcessId = $process.Id
        }
    }

    if (-not $NoWait) {
        if (($service -eq "registry-server") -or ($service -eq "config-server")) {
            Wait-PortListening -ServiceName $service -Port $servicePorts[$service] -TimeoutSeconds $ReadyTimeoutSeconds
        } else {
            Start-Sleep -Seconds $StartGapSeconds
        }
    }
}

Write-Host ""
Write-Host "Java service startup commands have been issued."
Write-Host ("Logs directory: {0}" -f $logRoot)

if ($started.Count -gt 0) {
    $started | Format-Table -AutoSize
}
