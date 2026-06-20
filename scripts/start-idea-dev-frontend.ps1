param(
    [switch]$SkipDocker,
    [switch]$NoBrowser
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeLogs = Join-Path $repoRoot ".runtime-logs"
$frontendScript = Join-Path $repoRoot "scripts\frontend_dev_server.py"
$outLog = Join-Path $runtimeLogs "frontend-dev.out.log"
$errLog = Join-Path $runtimeLogs "frontend-dev.err.log"

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

function Resolve-Python {
    $pythonCommand = Get-Command python -ErrorAction SilentlyContinue
    if ($pythonCommand) {
        return $pythonCommand.Source
    }

    $python3Command = Get-Command python3 -ErrorAction SilentlyContinue
    if ($python3Command) {
        return $python3Command.Source
    }

    throw "Python was not found. Install Python or configure an IDEA Python run configuration for scripts\frontend_dev_server.py."
}

if (-not $SkipDocker) {
    & (Join-Path $repoRoot "scripts\start-dev-infra.ps1")
}

Stop-PortProcess -Port 5500
Remove-Item $outLog, $errLog -Force -ErrorAction SilentlyContinue

$python = Resolve-Python
Start-Process -FilePath $python `
    -ArgumentList @("-u", ('"' + $frontendScript + '"')) `
    -WorkingDirectory $repoRoot `
    -WindowStyle Hidden `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog | Out-Null

$deadline = (Get-Date).AddSeconds(8)
do {
    Start-Sleep -Seconds 1
    $listener = Get-NetTCPConnection -State Listen -LocalPort 5500 -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        Write-Host "Frontend dev server is running at http://localhost:5500"
        Write-Host "Start Java services from IDEA with SPRING_PROFILES_ACTIVE=dev"
        if (-not $NoBrowser) {
            Start-Process "http://localhost:5500"
        }
        exit 0
    }
} while ((Get-Date) -lt $deadline)

$stderr = if (Test-Path $errLog) { Get-Content $errLog -Tail 30 | Out-String } else { "" }
throw "Frontend dev server did not start on port 5500. $stderr"
