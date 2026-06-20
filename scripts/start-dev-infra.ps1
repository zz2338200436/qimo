Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

Push-Location $repoRoot
try {
    docker compose up -d mysql redis rabbitmq registry-server config-server
} finally {
    Pop-Location
}

Write-Host "Development infrastructure is running:"
Write-Host "  MySQL    localhost:3306"
Write-Host "  Redis    localhost:6379"
Write-Host "  RabbitMQ amqp://localhost:5672"
Write-Host "  RabbitMQ management http://localhost:15672"
Write-Host "  Eureka   http://localhost:8761"
Write-Host "  Config   http://localhost:8888"
