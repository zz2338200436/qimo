$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Repo = Split-Path -Parent (Split-Path -Parent $Root)
$BundledPython = Join-Path $env:USERPROFILE ".cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
$SystemPython = (Get-Command python -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source)

if (Test-Path $BundledPython) {
    $Python = $BundledPython
} elseif ($SystemPython) {
    $Python = $SystemPython
} else {
    throw "Python was not found. Install Python or run this script in the Codex desktop environment."
}

Push-Location $Repo
try {
    & $Python "docs\uml-staruml-delivery\tools\generate_uml_delivery.py"
} finally {
    Pop-Location
}

$ReportFile = Get-ChildItem -Path (Join-Path $Root "report") -Filter "*.docx" | Select-Object -First 1 -ExpandProperty FullName
$PdfFile = $null
$Word = $null
$Doc = $null
try {
    $Word = New-Object -ComObject Word.Application
    $Word.Visible = $false
    $Word.DisplayAlerts = 0
    $Doc = $Word.Documents.Open($ReportFile, $false, $true)
    $PdfFile = [System.IO.Path]::ChangeExtension($ReportFile, ".pdf")
    $Doc.ExportAsFixedFormat($PdfFile, 17)
} catch {
    Write-Host "PDF export skipped. Microsoft Word COM automation is unavailable or failed." -ForegroundColor Yellow
} finally {
    if ($null -ne $Doc) {
        $Doc.Close($false) | Out-Null
    }
    if ($null -ne $Word) {
        $Word.Quit() | Out-Null
    }
}

Write-Host ""
Write-Host "UML delivery generated:" -ForegroundColor Green
Write-Host "  $Root"
Write-Host "Report:"
Write-Host "  $ReportFile"
if ($PdfFile -and (Test-Path $PdfFile)) {
    Write-Host "PDF:"
    Write-Host "  $PdfFile"
}
Write-Host "Visual QA contact sheet:"
Write-Host "  $(Join-Path $Root 'images\contact-sheet.png')"
Write-Host "StarUML model:"
Write-Host "  $(Join-Path $Root 'sources\smart-learning-system.mdj')"
