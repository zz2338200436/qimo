$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$sessionFile = Join-Path $root ".runtime-logs\student-session-exams.json"
$sessionScript = Join-Path $root "scripts\get-dev-auth-session.ps1"

if (-not (Test-Path -LiteralPath $sessionScript)) {
    throw "Session bootstrap script not found: $sessionScript"
}

& powershell -ExecutionPolicy Bypass -File $sessionScript -Username student42 -OutFile $sessionFile | Out-Null

if (-not (Test-Path -LiteralPath $sessionFile)) {
    throw "Session file not found after refresh: $sessionFile"
}

$session = Get-Content -LiteralPath $sessionFile -Raw | ConvertFrom-Json
$token = [string]$session.accessToken

if ([string]::IsNullOrWhiteSpace($token)) {
    throw "accessToken missing in session file: $sessionFile"
}

$headers = @{
    Authorization = "Bearer $token"
}

function Assert-Success {
    param(
        [string]$Name,
        [object]$Response
    )

    if (-not $Response.success) {
        throw "$Name did not return success=true: $($Response | ConvertTo-Json -Compress -Depth 10)"
    }
}

Write-Host "Checking student exams list..."
$examList = Invoke-RestMethod `
    -UseBasicParsing `
    -Uri "http://localhost:8080/api/student/exams?page=1&size=10" `
    -Headers $headers `
    -Method GET

Assert-Success -Name "student exams list" -Response $examList

if (-not $examList.data -or -not $examList.data.content -or $examList.data.content.Count -lt 1) {
    throw "student exams list was empty: $($examList | ConvertTo-Json -Compress -Depth 10)"
}

$examId = [int64]$examList.data.content[0].id
Write-Host "Checking student exam detail for id $examId ..."
$examDetail = Invoke-RestMethod `
    -UseBasicParsing `
    -Uri "http://localhost:8080/api/student/exams/$examId" `
    -Headers $headers `
    -Method GET

Assert-Success -Name "student exam detail" -Response $examDetail

if (-not $examDetail.data -or [string]::IsNullOrWhiteSpace([string]$examDetail.data.title)) {
    throw "student exam detail missing title: $($examDetail | ConvertTo-Json -Compress -Depth 10)"
}

Write-Host "Checking student scores..."
$scores = Invoke-RestMethod `
    -UseBasicParsing `
    -Uri "http://localhost:8080/api/student/scores" `
    -Headers $headers `
    -Method GET

Assert-Success -Name "student scores" -Response $scores

if (-not $scores.data -or $scores.data.Count -lt 1) {
    throw "student scores was empty: $($scores | ConvertTo-Json -Compress -Depth 10)"
}

Write-Host "verify-student-exams-contract: PASS"
