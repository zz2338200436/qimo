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
    Write-Host "StarUML extension installed to:" -ForegroundColor Green
    Write-Host "  $Target"
    throw "StarUML was not found. Install StarUML, then restart it and use Tools > 生成推荐 24 张 UML 图."
}

$Running = Get-Process -Name "StarUML" -ErrorAction SilentlyContinue
if ($Running) {
    Write-Host "StarUML is already running. If the new menu is not visible, close StarUML and run this script again." -ForegroundColor Yellow
}

Start-Process -FilePath $Staruml -WindowStyle Normal
Write-Host "StarUML extension installed and StarUML opened:" -ForegroundColor Green
Write-Host "  $Target"
Write-Host "In StarUML, use Tools > 生成课程核心 9 张 UML 图 or Tools > 生成推荐 24 张 UML 图."
