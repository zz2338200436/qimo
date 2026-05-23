Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$python = (Get-Command python -ErrorAction Stop).Source
$frontendScript = Join-Path $repoRoot "scripts\frontend_dev_server.py"
$outLog = Join-Path $repoRoot ".runtime-logs\frontend-path-contract.out.log"
$errLog = Join-Path $repoRoot ".runtime-logs\frontend-path-contract.err.log"

Remove-Item $outLog, $errLog -Force -ErrorAction SilentlyContinue

$proc = Start-Process -FilePath $python `
    -ArgumentList @('-u', ('"' + $frontendScript + '"')) `
    -WorkingDirectory $repoRoot `
    -WindowStyle Hidden `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -PassThru

Start-Sleep -Seconds 3

$stderr = if (Test-Path $errLog) { (Get-Content $errLog -Raw) } else { '' }

if ($stderr -match "can't open file '.*Distributed'") {
    throw "frontend_dev_server.py path should remain intact even when repo path contains spaces.`n$stderr"
}

if ($stderr -match 'WinError 10048') {
    Write-Host "SKIP: frontend dev server launch preserved the script path; port 5500 was already in use."
    exit 0
}

if ($proc -and -not $proc.HasExited) {
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
}

Write-Host "PASS: frontend dev server launch preserves script path with spaces."
