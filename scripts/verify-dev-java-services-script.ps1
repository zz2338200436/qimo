Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$scriptPath = Join-Path $repoRoot "scripts\start-dev-java-services.ps1"

if (-not (Test-Path $scriptPath)) {
    throw "Missing scripts\start-dev-java-services.ps1"
}

$content = Get-Content -LiteralPath $scriptPath -Raw

$requiredSnippets = @(
    'scripts\start-dev-infra.ps1',
    'registry-server',
    'config-server',
    'auth-service',
    'user-service',
    'course-service',
    'assignment-service',
    'exam-service',
    'gateway',
    '.runtime-logs\java-services',
    '-DskipTests package',
    'java -jar',
    'SkipBuild',
    'SPRING_PROFILES_ACTIVE',
    'CONFIG_SERVER_URL'
)

foreach ($snippet in $requiredSnippets) {
    if ($content -notlike "*$snippet*") {
        throw "scripts\start-dev-java-services.ps1 is missing expected content: $snippet"
    }
}

$errors = $null
[System.Management.Automation.Language.Parser]::ParseFile($scriptPath, [ref]$null, [ref]$errors) | Out-Null
if ($errors.Count -gt 0) {
    $errors | Format-List *
    throw "scripts\start-dev-java-services.ps1 has PowerShell syntax errors"
}

Write-Host "start-dev-java-services.ps1 contract OK"
