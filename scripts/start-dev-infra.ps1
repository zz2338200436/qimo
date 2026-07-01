param(
    [string[]]$Services = @('mysql', 'redis', 'rabbitmq', 'registry-server', 'config-server')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not $Services -or $Services.Count -eq 0) {
    throw "At least one docker compose service must be specified."
}

Push-Location $repoRoot
try {
    docker compose up -d @Services
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

Write-Host "Development infrastructure is running:"

$details = @{
    'mysql' = "  MySQL    localhost:3306"
    'redis' = "  Redis    localhost:6379"
    'rabbitmq' = "  RabbitMQ amqp://localhost:5672"
    'registry-server' = "  Eureka   http://localhost:8761"
    'config-server' = "  Config   http://localhost:8888"
}

foreach ($service in $Services) {
    if ($details.ContainsKey($service)) {
        Write-Host $details[$service]
        if ($service -eq 'rabbitmq') {
            Write-Host "  RabbitMQ management http://localhost:15672"
        }
        continue
    }

    Write-Host ("  {0}" -f $service)
}
