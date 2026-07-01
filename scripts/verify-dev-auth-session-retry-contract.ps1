$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$scriptPath = Join-Path $repoRoot "scripts\get-dev-auth-session.ps1"
$content = Get-Content -LiteralPath $scriptPath -Raw

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

Assert-Contains `
    -Haystack $content `
    -Needle "function Invoke-LoginAttempt" `
    -Message "get-dev-auth-session.ps1 should wrap captcha fetch, Redis captcha read, and login POST in one retryable login attempt."

Assert-Contains `
    -Haystack $content `
    -Needle "Invoke-WithGatewayRetry -Operation { Invoke-LoginAttempt }" `
    -Message "get-dev-auth-session.ps1 should retry the full login attempt, not only reuse a previously consumed captcha."

Assert-Contains `
    -Haystack $content `
    -Needle 'Requesting captcha from $BaseUrl/api/auth/captcha' `
    -Message "get-dev-auth-session.ps1 should still request captcha through the gateway auth route."

Assert-Contains `
    -Haystack $content `
    -Needle '$captchaResponse = Invoke-WebRequest' `
    -Message "get-dev-auth-session.ps1 should request a fresh captcha inside Invoke-LoginAttempt."

Assert-Contains `
    -Haystack $content `
    -Needle '$loginResponse = Invoke-RestMethod' `
    -Message "get-dev-auth-session.ps1 should submit login inside Invoke-LoginAttempt after reading the matching captcha code."

Write-Host "dev auth session retry contract OK"
