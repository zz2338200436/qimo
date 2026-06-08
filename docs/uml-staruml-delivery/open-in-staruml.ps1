$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Model = Join-Path $Root "sources\smart-learning-system.mdj"

if (-not (Test-Path $Model)) {
    throw "StarUML model not found: $Model. Run generate-all.ps1 first."
}

function Resolve-StarUmlExecutable {
    $Command = Get-Command staruml -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source
    if ($Command) {
        return $Command
    }

    $Candidates = @(
        "C:\Program Files\StarUML\StarUML.exe",
        "C:\Program Files (x86)\StarUML\StarUML.exe",
        "$env:LOCALAPPDATA\Programs\StarUML\StarUML.exe"
    )
    foreach ($Candidate in $Candidates) {
        if ($Candidate -and (Test-Path $Candidate)) {
            return $Candidate
        }
    }
    return $null
}

$Staruml = Resolve-StarUmlExecutable
if (-not $Staruml) {
    throw "StarUML was not found. Install StarUML or add the 'staruml' command to PATH, then rerun this script."
}

Start-Process -FilePath $Staruml -ArgumentList @($Model) -WindowStyle Normal
Write-Host "Opened StarUML model:" -ForegroundColor Green
Write-Host "  $Model"
