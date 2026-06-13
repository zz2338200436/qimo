param(
    [ValidateSet("student", "teacher")]
    [string]$Role,
    [ValidateSet("student42", "teacher7")]
    [string]$Username = "student42",
    [string]$Password = "Teach1234",
    [string]$BaseUrl = "http://localhost:8080",
    [string]$RedisContainer = "qimo-redis",
    [string]$CaptchaPrefix = "CAPTCHA:IMG:",
    [string]$OutFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$UsernameByRole = @{
    student = "student42"
    teacher = "teacher7"
}

if ($PSBoundParameters.ContainsKey("Role")) {
    $roleKey = $Role.ToLowerInvariant()
    $usernameForRole = $UsernameByRole[$roleKey]
    if ($PSBoundParameters.ContainsKey("Username") -and $Username -ne $usernameForRole) {
        throw "Role '$Role' maps to username '$usernameForRole', but Username '$Username' was provided."
    }
    $Username = $usernameForRole
}

function Invoke-WithGatewayRetry {
    param(
        [scriptblock]$Operation,
        [int]$MaxAttempts = 5,
        [int]$DelaySeconds = 2
    )

    $attempt = 0
    while ($true) {
        $attempt++
        try {
            return & $Operation
        } catch {
            $response = $_.Exception.Response
            $statusCode = $null
            if ($response) {
                try {
                    $statusCode = [int]$response.StatusCode
                } catch {
                    $statusCode = $null
                }
            }

            $shouldRetry = $attempt -lt $MaxAttempts -and $statusCode -eq 503
            if (-not $shouldRetry) {
                throw
            }

            Write-Host "Gateway returned 503, retrying in $DelaySeconds second(s)... (attempt $attempt/$MaxAttempts)"
            Start-Sleep -Seconds $DelaySeconds
        }
    }
}

function Get-RedisCaptchaCode {
    param(
        [string]$ContainerName,
        [string]$RedisKey
    )

    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "docker command not found in PATH."
    }

    $captchaCode = docker exec $ContainerName redis-cli --raw GET $RedisKey
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to read captcha code from redis container '$ContainerName'."
    }

    $captchaCode = ($captchaCode | Out-String).Trim()
    if ([string]::IsNullOrWhiteSpace($captchaCode)) {
        throw "Captcha code for redis key '$RedisKey' was empty."
    }

    return $captchaCode
}

function New-SessionSnapshot {
    param(
        [pscustomobject]$AuthData
    )

    $activeRole = $null
    if ($AuthData.user -and $AuthData.user.activeRole) {
        $activeRole = $AuthData.user.activeRole
    } elseif ($AuthData.user -and $AuthData.user.roles -and $AuthData.user.roles.Count -gt 0) {
        $activeRole = $AuthData.user.roles[0]
    }

    [ordered]@{
        token        = $AuthData.accessToken
        refreshToken = $AuthData.refreshToken
        user         = ($AuthData.user | ConvertTo-Json -Compress -Depth 10)
        userId       = if ($AuthData.user -and $null -ne $AuthData.user.id) { [string]$AuthData.user.id } else { $null }
        activeRole   = $activeRole
        role         = $activeRole
    }
}

function Invoke-LoginAttempt {
    Write-Host "Requesting captcha from $BaseUrl/api/auth/captcha ..."
    $attemptWebSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
    $captchaResponse = Invoke-WebRequest `
        -UseBasicParsing `
        -Uri "$BaseUrl/api/auth/captcha?timestamp=$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())" `
        -Method GET `
        -WebSession $attemptWebSession

    $captchaKey = [string]$captchaResponse.Headers["X-Captcha-Key"]
    if ([string]::IsNullOrWhiteSpace($captchaKey)) {
        throw "Gateway captcha response did not include X-Captcha-Key."
    }

    $redisKey = "$CaptchaPrefix$captchaKey"
    $captchaCode = Get-RedisCaptchaCode -ContainerName $RedisContainer -RedisKey $redisKey

    Write-Host "Logging in as $Username using captcha key $captchaKey ..."
    $loginBody = @{
        username   = $Username
        password   = $Password
        captcha    = $captchaCode
        captchaKey = $captchaKey
    } | ConvertTo-Json -Compress

    $loginResponse = Invoke-RestMethod `
        -UseBasicParsing `
        -Uri "$BaseUrl/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody `
        -WebSession $attemptWebSession

    [pscustomobject]@{
        CaptchaKey    = $captchaKey
        CaptchaCode   = $captchaCode
        LoginResponse = $loginResponse
        WebSession    = $attemptWebSession
    }
}

$loginAttempt = Invoke-WithGatewayRetry -Operation { Invoke-LoginAttempt }
$captchaKey = $loginAttempt.CaptchaKey
$captchaCode = $loginAttempt.CaptchaCode
$loginResponse = $loginAttempt.LoginResponse
$webSession = $loginAttempt.WebSession

if (-not $loginResponse.success -or -not $loginResponse.data) {
    throw "Login failed: $($loginResponse | ConvertTo-Json -Compress -Depth 10)"
}

$authData = [pscustomobject]$loginResponse.data
$sessionSnapshot = New-SessionSnapshot -AuthData $authData
$cookieHeader = ($webSession.Cookies.GetCookies($BaseUrl) | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join "; "

$result = [ordered]@{
    success         = $true
    baseUrl         = $BaseUrl
    username        = $Username
    captchaKey      = $captchaKey
    captchaCode     = $captchaCode
    accessToken     = $authData.accessToken
    refreshToken    = $authData.refreshToken
    activeRole      = $sessionSnapshot.activeRole
    user            = $authData.user
    cookies         = $cookieHeader
    sessionStorage  = $sessionSnapshot
    browserScript   = "window.persistAuthSession(" + ($authData | ConvertTo-Json -Compress -Depth 10) + ");"
}

$json = $result | ConvertTo-Json -Depth 10

if ($OutFile) {
    $outDir = Split-Path -Parent $OutFile
    if ($outDir -and -not (Test-Path -LiteralPath $outDir)) {
        New-Item -ItemType Directory -Path $outDir | Out-Null
    }
    $resolvedOutFile = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutFile)
    [System.IO.File]::WriteAllText($resolvedOutFile, $json, [System.Text.UTF8Encoding]::new($false))
    Write-Host "Wrote dev auth session to $OutFile"
}

$json
