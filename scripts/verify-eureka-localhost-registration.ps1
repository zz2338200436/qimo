$ErrorActionPreference = "Stop"

function Assert-Contains {
    param(
        [string]$Haystack,
        [string]$Needle,
        [string]$Message
    )

    if (-not $Haystack.Contains($Needle)) {
        throw $Message
    }
}

$services = @(
    @{ Name = "COURSE-SERVICE"; Port = 8083 },
    @{ Name = "EXAM-SERVICE"; Port = 8085 }
)

foreach ($service in $services) {
    $response = Invoke-WebRequest -Uri "http://localhost:8761/eureka/apps/$($service.Name)" -UseBasicParsing
    $content = $response.Content

    Assert-Contains $content "<status>UP</status>" "$($service.Name) is not UP in Eureka."
    Assert-Contains $content "<hostName>localhost</hostName>" "$($service.Name) is not registered with localhost hostName."
    Assert-Contains $content "<ipAddr>127.0.0.1</ipAddr>" "$($service.Name) is not registered with loopback ipAddr."
    Assert-Contains $content "<port enabled=""true"">$($service.Port)</port>" "$($service.Name) is not registered on expected port $($service.Port)."
}

Write-Host "Eureka localhost registration verified for COURSE-SERVICE and EXAM-SERVICE."
