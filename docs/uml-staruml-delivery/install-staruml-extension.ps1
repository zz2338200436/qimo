$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Source = Join-Path $Root "staruml-extension\smart-learning-uml-generator"
$TargetRoot = Join-Path $env:APPDATA "StarUML\extensions\user"
$Target = Join-Path $TargetRoot "smart-learning-uml-generator"

if (-not (Test-Path $Source)) {
    throw "Extension folder not found: $Source. Run generate-all.ps1 first."
}

New-Item -ItemType Directory -Force -Path $TargetRoot | Out-Null
$BackupRoot = Join-Path $env:APPDATA "StarUML-disabled-extension-backups"
New-Item -ItemType Directory -Force -Path $BackupRoot | Out-Null
$LegacyBackupRoot = Join-Path (Split-Path -Parent $TargetRoot) "disabled-extension-backups"
if (Test-Path $LegacyBackupRoot) {
    Get-ChildItem -LiteralPath $LegacyBackupRoot -Directory -Filter "smart-learning-uml-generator.bak-*" -ErrorAction SilentlyContinue | ForEach-Object {
        $Destination = Join-Path $BackupRoot $_.Name
        if (Test-Path $Destination) {
            Remove-Item -LiteralPath $Destination -Recurse -Force
        }
        Move-Item -LiteralPath $_.FullName -Destination $Destination
    }
}
Get-ChildItem -LiteralPath $TargetRoot -Directory -Filter "smart-learning-uml-generator.bak-*" -ErrorAction SilentlyContinue | ForEach-Object {
    $Destination = Join-Path $BackupRoot $_.Name
    if (Test-Path $Destination) {
        Remove-Item -LiteralPath $Destination -Recurse -Force
    }
    Move-Item -LiteralPath $_.FullName -Destination $Destination
}
if (Test-Path $Target) {
    $Backup = Join-Path $BackupRoot "smart-learning-uml-generator.bak-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    Move-Item -LiteralPath $Target -Destination $Backup
    Write-Host "Existing extension backed up to:"
    Write-Host "  $Backup"
}
Copy-Item -LiteralPath $Source -Destination $Target -Recurse

Write-Host "StarUML extension installed to:" -ForegroundColor Green
Write-Host "  $Target"
Write-Host "Restart StarUML, then use Tools > 生成课程核心 9 张 UML 图 or Tools > 生成推荐 24 张 UML 图."
