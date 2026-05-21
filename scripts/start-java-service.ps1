param(
    [Parameter(Mandatory = $true)]
    [string]$ServiceName,

    [Parameter(Mandatory = $true)]
    [string]$JarRelativePath,

    [string]$StdoutLog,
    [string]$StderrLog,

    [hashtable]$EnvironmentVariables = @{}
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$java = 'D:\111\java\Program Files\Java\jdk-20\bin\java.exe'
$jarPath = Join-Path $repoRoot $JarRelativePath

if (-not (Test-Path $jarPath)) {
    throw "Jar not found: $jarPath"
}

$jarDir = Split-Path -Parent $jarPath
$jarName = Split-Path -Leaf $jarPath
$runtimeLogsDir = Join-Path $repoRoot '.runtime-logs'

if (-not (Test-Path $runtimeLogsDir)) {
    New-Item -ItemType Directory -Path $runtimeLogsDir | Out-Null
}

if ([string]::IsNullOrWhiteSpace($StdoutLog)) {
    $StdoutLog = Join-Path $runtimeLogsDir "$ServiceName.out.log"
}
if ([string]::IsNullOrWhiteSpace($StderrLog)) {
    $StderrLog = Join-Path $runtimeLogsDir "$ServiceName.err.log"
}

Remove-Item $StdoutLog, $StderrLog -Force -ErrorAction SilentlyContinue

$command = @(
    '$ErrorActionPreference = ''Stop'''
    ('$env:Path = ''{0}'' + '';'' + $env:Path' -f (Split-Path -Parent $java))
)

foreach ($key in $EnvironmentVariables.Keys) {
    $value = $EnvironmentVariables[$key].ToString().Replace("'", "''")
    $command += ('$env:{0} = ''{1}''' -f $key, $value)
}

$jarDirLiteral = $jarDir.Replace("'", "''")
$jarNameLiteral = $jarName.Replace("'", "''")
$stdoutLiteral = $StdoutLog.Replace("'", "''")
$stderrLiteral = $StderrLog.Replace("'", "''")

$command += @(
    ('Set-Location -LiteralPath ''{0}''' -f $jarDirLiteral)
    ('$out = ''{0}''' -f $stdoutLiteral)
    ('$err = ''{0}''' -f $stderrLiteral)
    ('$proc = Start-Process -FilePath ''{0}'' -ArgumentList @(''-jar'', ''{1}'') -WorkingDirectory (Get-Location).Path -WindowStyle Hidden -RedirectStandardOutput $out -RedirectStandardError $err -PassThru' -f $java.Replace("'", "''"), $jarNameLiteral)
    'Start-Sleep -Seconds 2'
    'if ($proc.HasExited) {'
    '  Write-Output ("EXITED:{0}" -f $proc.Id)'
    '} else {'
    '  Write-Output ("RUNNING:{0}" -f $proc.Id)'
    '}'
)

$encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes(($command -join "`n")))

powershell -EncodedCommand $encoded
