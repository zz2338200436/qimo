param(
    [string]$Mysql = "mysql",
    [string]$HostName = "127.0.0.1",
    [int]$Port = 3306,
    [string]$Username = "dev_user",
    [string]$Password = "dev_only_pwd"
)

$ErrorActionPreference = "Stop"

$scriptPath = Join-Path $PSScriptRoot "seed-teacher-dashboard-scores.sql"
if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "Seed SQL not found: $scriptPath"
}

& $Mysql `
    --host=$HostName `
    --port=$Port `
    --user=$Username `
    --password=$Password `
    --default-character-set=utf8mb4 `
    --execute="SOURCE $($scriptPath.Replace('\', '/'))"
