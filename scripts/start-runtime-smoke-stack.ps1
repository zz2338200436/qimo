param(
    [switch]$EnableExamRelay
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$java = 'D:\111\java\Program Files\Java\jdk-20\bin\java.exe'
$pythonCommand = Get-Command python -ErrorAction SilentlyContinue
$python3Command = Get-Command python3 -ErrorAction SilentlyContinue
$python = $null
if ($pythonCommand) {
    $python = $pythonCommand.Source
} elseif ($python3Command) {
    $python = $python3Command.Source
}
$runtimeLogs = Join-Path $repoRoot '.runtime-logs'

if (-not (Test-Path $runtimeLogs)) {
    New-Item -ItemType Directory -Path $runtimeLogs | Out-Null
}

function Stop-PortProcess {
    param([int]$Port)

    $listeners = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        try {
            Stop-Process -Id $listener.OwningProcess -Force -ErrorAction Stop
        } catch {
            Write-Warning ("Failed to stop process on port {0}: {1}" -f $Port, $_.Exception.Message)
        }
    }
}

function Start-JarService {
    param(
        [string]$Name,
        [string]$JarRelativePath,
        [int]$Port,
        [int]$WaitSeconds = 8,
        [hashtable]$EnvironmentVariables = @{}
    )

    Stop-PortProcess -Port $Port

    $jarPath = Join-Path $repoRoot $JarRelativePath
    if (-not (Test-Path $jarPath)) {
        throw "Jar not found: $jarPath"
    }
    $jarDir = Split-Path -Parent $jarPath
    $jarName = Split-Path -Leaf $jarPath

    $outLog = Join-Path $runtimeLogs "$Name.out.log"
    $errLog = Join-Path $runtimeLogs "$Name.err.log"
    Remove-Item $outLog, $errLog -Force -ErrorAction SilentlyContinue

    $commandParts = @(
        ('$ErrorActionPreference = ''Stop''')
        ('Set-Location -LiteralPath ''{0}''' -f $jarDir.Replace("'", "''"))
    )
    foreach ($key in $EnvironmentVariables.Keys) {
        $value = $EnvironmentVariables[$key].Replace("'", "''")
        $commandParts += ('$env:{0} = ''{1}''' -f $key, $value)
    }
    $commandParts += ('& ''{0}'' -jar ''{1}''' -f $java.Replace("'", "''"), $jarName.Replace("'", "''"))
    $command = $commandParts -join '; '

    Start-Process -FilePath powershell `
        -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', $command) `
        -WorkingDirectory $jarDir `
        -WindowStyle Hidden `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog | Out-Null

    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    do {
        Start-Sleep -Seconds 1
        $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($listener) {
            return
        }
    } while ((Get-Date) -lt $deadline)
}

function Start-Frontend {
    if (-not $python) {
        throw "python executable not found in PATH."
    }

    Stop-PortProcess -Port 5500

    $outLog = Join-Path $runtimeLogs 'frontend-smoke.out.log'
    $errLog = Join-Path $runtimeLogs 'frontend-smoke.err.log'
    Remove-Item $outLog, $errLog -Force -ErrorAction SilentlyContinue

    Start-Process -FilePath $python `
        -ArgumentList @('-m', 'http.server', '5500') `
        -WorkingDirectory (Join-Path $repoRoot 'frontend\dist') `
        -WindowStyle Hidden `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog | Out-Null

    Start-Sleep -Seconds 3
}

Start-Frontend
Start-JarService -Name 'registry-smoke' -JarRelativePath 'registry-server\target\registry-server-0.1.0-SNAPSHOT.jar' -Port 8761 -WaitSeconds 10
Start-JarService -Name 'user-smoke' -JarRelativePath 'user-service\target\user-service-0.1.0-SNAPSHOT.jar' -Port 8082 -WaitSeconds 8
Start-JarService -Name 'auth-smoke' -JarRelativePath 'auth-service\target\auth-service-0.1.0-SNAPSHOT.jar' -Port 8081 -WaitSeconds 8
Start-JarService -Name 'course-smoke' -JarRelativePath 'course-service\target\course-service-0.1.0-SNAPSHOT.jar' -Port 8083 -WaitSeconds 8
Start-JarService -Name 'notification-smoke' -JarRelativePath 'notification-service\target\notification-service-0.1.0-SNAPSHOT.jar' -Port 8087 -WaitSeconds 8

$examEnv = @{}
if ($EnableExamRelay) {
    $examEnv['PLATFORM_OUTBOX_RELAY_ENABLED'] = 'true'
}
Start-JarService -Name 'exam-smoke' -JarRelativePath 'exam-service\target\exam-service-0.1.0-SNAPSHOT.jar' -Port 8085 -WaitSeconds 8 -EnvironmentVariables $examEnv
Start-JarService -Name 'gateway-smoke' -JarRelativePath 'gateway\target\gateway-0.1.0-SNAPSHOT.jar' -Port 8080 -WaitSeconds 10

powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'check-local-service-ports.ps1')
