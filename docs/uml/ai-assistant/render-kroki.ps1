$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$outDir = Join-Path $root "out"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$files = Get-ChildItem -Path $root -Filter "*.puml" | Where-Object { $_.Name -ne "00-style.puml" } | Sort-Object Name

foreach ($file in $files) {
    $content = Get-Content -Raw -LiteralPath $file.FullName
    $svg = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "https://kroki.io/plantuml/svg" -ContentType "text/plain; charset=utf-8" -Body $content
    $svgPath = Join-Path $outDir ($file.BaseName + ".svg")
    Set-Content -LiteralPath $svgPath -Value $svg.Content -Encoding UTF8

    $pngPath = Join-Path $outDir ($file.BaseName + ".png")
    Invoke-WebRequest -UseBasicParsing -Method Post -Uri "https://kroki.io/plantuml/png" -ContentType "text/plain; charset=utf-8" -Body $content -OutFile $pngPath
}

Write-Host "Rendered $($files.Count) diagrams to $outDir"
