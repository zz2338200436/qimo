$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Model = Join-Path $Root "sources\smart-learning-system.mdj"
$OutDir = Join-Path $Root "staruml-exported-images"

if (-not (Test-Path $Model)) {
    throw "StarUML model not found: $Model. Run generate-all.ps1 first."
}

$Staruml = (Get-Command staruml -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source)
if (-not $Staruml) {
    $Candidates = @(
        "C:\Program Files\StarUML\StarUML.exe",
        "C:\Program Files (x86)\StarUML\StarUML.exe"
    )
    foreach ($Candidate in $Candidates) {
        if (Test-Path $Candidate) {
            $Staruml = $Candidate
            break
        }
    }
}

if (-not $Staruml) {
    throw "StarUML CLI was not found. Install StarUML and ensure the 'staruml' command or StarUML.exe is available."
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# StarUML official CLI image command exports all diagrams when selector defaults to @Diagram.
# The output option is a file-name template, so every selected diagram becomes one PNG.
$OutputPattern = Join-Path $OutDir '<%=filenamify(element.name)%>.png'
& $Staruml image $Model -f png -o $OutputPattern

Write-Host "StarUML images exported to:" -ForegroundColor Green
Write-Host "  $OutDir"
