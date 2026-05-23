Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$scriptPath = Join-Path $repoRoot "scripts\start-java-service.ps1"
$jarRelativePath = "registry-server\target\registry-server-0.1.0-SNAPSHOT.jar"

$output = & powershell -ExecutionPolicy Bypass -File $scriptPath `
    -ServiceName "contract-start-java-service" `
    -JarRelativePath $jarRelativePath `
    -JvmArguments '-Xms64m','-Xmx128m'

if ($LASTEXITCODE -ne 0) {
    throw "start-java-service.ps1 exited with code $LASTEXITCODE.`n$output"
}

$text = ($output | Out-String)

if ($text -match 'Unexpected token' -or $text -match 'ParserError') {
    throw "start-java-service.ps1 should not emit PowerShell parser errors when JvmArguments are provided.`n$text"
}

if ($text -notmatch 'RUNNING:\d+' -and $text -notmatch 'EXITED:\d+') {
    throw "start-java-service.ps1 should report the child process state.`n$text"
}

Write-Host "PASS: start-java-service.ps1 accepts JvmArguments without parser errors."
