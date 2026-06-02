param(
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $repoRoot 'frontend\dist'
$targetRoot = Join-Path $repoRoot 'major_assignment\src\main\resources\static'

if (-not (Test-Path $sourceRoot)) {
    throw "Source frontend directory not found: $sourceRoot"
}

if (-not (Test-Path $targetRoot)) {
    throw "Target static directory not found: $targetRoot"
}

$includePaths = @(
    'api.js',
    'common-ui.js',
    'default-avatar.svg',
    'i18n.js',
    'index.html',
    'load-menu.js',
    'styles.css',
    'student-ai-assistant.html',
    'student-assignments.html',
    'student-courses.html',
    'student-dashboard.html',
    'student-login.html',
    'student-notifications.html',
    'student-settings.html',
    'student-stats.html',
    'teacher-ai-tools.html',
    'teacher-assignments.html',
    'teacher-assignments-assignment-crud.js',
    'teacher-assignments-assignment-publish.js',
    'teacher-assignments-lists.js',
    'teacher-assignments-exam-crud.js',
    'teacher-assignments-exam-publish.js',
    'teacher-assignments-grading.js',
    'teacher-assignments-shell.js',
    'teacher-assignments-submission-filters.js',
    'teacher-courses-assignments.js',
    'teacher-courses-classes.js',
    'teacher-courses-class-crud.js',
    'teacher-courses-courses.js',
    'teacher-courses-course-crud.js',
    'teacher-courses-class-edit.js',
    'teacher-courses-students.js',
    'teacher-courses-validation.js',
    'teacher-courses-metadata.js',
    'teacher-courses-shell.js',
    'teacher-courses.html',
    'teacher-dashboard.html',
    'teacher-knowledge.html',
    'teacher-login.html',
    'teacher-notifications.html',
    'teacher-settings.html',
    'teacher-student-dashboard.html',
    'teacher-warning.html',
    'components',
    'fonts',
    'i18n',
    'lib'
)

function Get-FileHashMap {
    param(
        [string]$BasePath
    )

    $fileMap = @{}
    Get-ChildItem -Path $BasePath -Recurse -File | ForEach-Object {
        $relativePath = $_.FullName.Substring($BasePath.Length).TrimStart('\')
        $fileMap[$relativePath] = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
    }
    return $fileMap
}

function Sync-Path {
    param(
        [string]$RelativePath,
        [bool]$CheckOnlyMode
    )

    $sourcePath = Join-Path $sourceRoot $RelativePath
    $targetPath = Join-Path $targetRoot $RelativePath

    if (-not (Test-Path $sourcePath)) {
        throw "Missing source path: $sourcePath"
    }

    $sourceItem = Get-Item -LiteralPath $sourcePath

    if ($sourceItem.PSIsContainer) {
        if (-not (Test-Path $targetPath)) {
            if ($CheckOnlyMode) {
                Write-Host "[DIFF] Missing target directory: $RelativePath"
                return $true
            }
            New-Item -ItemType Directory -Path $targetPath -Force | Out-Null
        }

        $sourceHashes = Get-FileHashMap -BasePath $sourcePath
        $targetHashes = @{}
        if (Test-Path $targetPath) {
            $targetHashes = Get-FileHashMap -BasePath $targetPath
        }

        $hasDiff = $false

        foreach ($relativeFile in $sourceHashes.Keys) {
            if (-not $targetHashes.ContainsKey($relativeFile) -or $targetHashes[$relativeFile] -ne $sourceHashes[$relativeFile]) {
                if ($CheckOnlyMode) {
                    Write-Host "[DIFF] $RelativePath\$relativeFile"
                }
                $hasDiff = $true
            }
        }

        foreach ($relativeFile in $targetHashes.Keys) {
            if (-not $sourceHashes.ContainsKey($relativeFile)) {
                if ($CheckOnlyMode) {
                    Write-Host "[EXTRA] $RelativePath\$relativeFile"
                }
                $hasDiff = $true
            }
        }

        if ($hasDiff -and -not $CheckOnlyMode) {
            if (Test-Path $targetPath) {
                Remove-Item -LiteralPath $targetPath -Recurse -Force
            }
            Copy-Item -LiteralPath $sourcePath -Destination $targetPath -Recurse -Force
            Write-Host "[SYNC] $RelativePath"
        } elseif (-not $hasDiff -and -not $CheckOnlyMode) {
            Write-Host "[OK]   $RelativePath"
        }

        return $hasDiff
    }

    $sourceHash = (Get-FileHash -LiteralPath $sourcePath -Algorithm SHA256).Hash
    $targetHash = $null

    if (Test-Path $targetPath) {
        $targetHash = (Get-FileHash -LiteralPath $targetPath -Algorithm SHA256).Hash
    }

    if ($sourceHash -eq $targetHash) {
        if (-not $CheckOnlyMode) {
            Write-Host "[OK]   $RelativePath"
        }
        return $false
    }

    if ($CheckOnlyMode) {
        Write-Host "[DIFF] $RelativePath"
        return $true
    }

    $targetParent = Split-Path -Parent $targetPath
    if (-not (Test-Path $targetParent)) {
        New-Item -ItemType Directory -Path $targetParent -Force | Out-Null
    }

    Copy-Item -LiteralPath $sourcePath -Destination $targetPath -Force
    Write-Host "[SYNC] $RelativePath"
    return $true
}

$hasAnyDiff = $false
foreach ($relativePath in $includePaths) {
    if (Sync-Path -RelativePath $relativePath -CheckOnlyMode:$CheckOnly) {
        $hasAnyDiff = $true
    }
}

if ($CheckOnly) {
    if ($hasAnyDiff) {
        Write-Host "Frontend assets and Spring static assets are out of sync."
        exit 1
    }

    Write-Host "Frontend assets and Spring static assets are already in sync."
    exit 0
}

Write-Host "Frontend asset sync completed."
