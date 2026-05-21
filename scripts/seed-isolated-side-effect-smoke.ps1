param(
    [string]$ContainerName = "qimo-mysql",
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "root"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlPath = Join-Path $scriptDir "seed-isolated-side-effect-smoke.sql"

if (-not (Test-Path -LiteralPath $sqlPath)) {
    throw "Seed SQL not found: $sqlPath"
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker command not found in PATH."
}

$sql = Get-Content -LiteralPath $sqlPath -Raw
$sql | docker exec -i $ContainerName mysql "-u$MysqlUser" "-p$MysqlPassword"

if ($LASTEXITCODE -ne 0) {
    throw "Failed to apply isolated side-effect smoke seed to container '$ContainerName'."
}

Write-Host "Applied isolated side-effect smoke seed from $sqlPath to $ContainerName."
