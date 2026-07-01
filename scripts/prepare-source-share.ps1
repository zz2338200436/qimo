param(
    [switch]$Execute,
    [switch]$IncludeGitMetadata,
    [switch]$IncludeCourseworkArtifacts,
    [switch]$IncludeMysqlData,
    [switch]$IncludeFrontendBuild
)

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))

function Test-InRepo {
    param(
        [Parameter(Mandatory = $true)]
        [string]$CandidatePath
    )

    $fullPath = [System.IO.Path]::GetFullPath($CandidatePath)
    return $fullPath.StartsWith($repoRoot, [System.StringComparison]::OrdinalIgnoreCase)
}

$items = New-Object System.Collections.Generic.List[object]

function Add-ExistingPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RelativePath,
        [Parameter(Mandatory = $true)]
        [string]$Reason
    )

    $fullPath = Join-Path $repoRoot $RelativePath
    if ((Test-Path -LiteralPath $fullPath) -and (Test-InRepo -CandidatePath $fullPath)) {
        $items.Add([pscustomobject]@{
                FullName = $fullPath
                Reason   = $Reason
            })
    }
}

function Add-MatchingFiles {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Pattern,
        [Parameter(Mandatory = $true)]
        [string]$Reason
    )

    Get-ChildItem -LiteralPath $repoRoot -Recurse -Force -File -Filter $Pattern -ErrorAction SilentlyContinue |
        ForEach-Object {
            if (Test-InRepo -CandidatePath $_.FullName) {
                $items.Add([pscustomobject]@{
                        FullName = $_.FullName
                        Reason   = $Reason
                    })
            }
        }
}

function Add-MatchingDirectories {
    param(
        [Parameter(Mandatory = $true)]
        [string]$DirectoryName,
        [Parameter(Mandatory = $true)]
        [string]$Reason
    )

    Get-ChildItem -LiteralPath $repoRoot -Recurse -Force -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -eq $DirectoryName } |
        ForEach-Object {
            if (Test-InRepo -CandidatePath $_.FullName) {
                $items.Add([pscustomobject]@{
                        FullName = $_.FullName
                        Reason   = $Reason
                    })
            }
        }
}

Add-ExistingPath -RelativePath ".idea" -Reason "IDE metadata"
Add-ExistingPath -RelativePath ".worktrees" -Reason "Local worktree scratch"
Add-ExistingPath -RelativePath ".runtime-logs" -Reason "Runtime logs"
Add-ExistingPath -RelativePath ".firecrawl" -Reason "Tool cache"
Add-ExistingPath -RelativePath ".codex-mcp" -Reason "Tool cache"
Add-ExistingPath -RelativePath ".claude" -Reason "Local agent cache"
Add-ExistingPath -RelativePath ".agents" -Reason "Local agent workspace"
Add-ExistingPath -RelativePath ".superpowers" -Reason "Local agent workspace"
Add-ExistingPath -RelativePath ".workbuddy" -Reason "Local agent workspace"
Add-ExistingPath -RelativePath "output" -Reason "Runtime output"
Add-ExistingPath -RelativePath ".report_assets" -Reason "Report intermediate assets"
Add-ExistingPath -RelativePath ".report_tools" -Reason "Report helper workspace"
Add-ExistingPath -RelativePath "uml-image-review" -Reason "UML review artifacts"
Add-ExistingPath -RelativePath "word-preview-single-final" -Reason "Word preview artifacts"
Add-ExistingPath -RelativePath "word-preview-single-final-v2" -Reason "Word preview artifacts"
Add-ExistingPath -RelativePath "word-preview-single-final-v3" -Reason "Word preview artifacts"
Add-ExistingPath -RelativePath "data\\redis" -Reason "Redis runtime data"
Add-ExistingPath -RelativePath "scripts\\__pycache__" -Reason "Python bytecode cache"

if ($IncludeGitMetadata) {
    Add-ExistingPath -RelativePath ".git" -Reason "Git metadata"
}

if ($IncludeMysqlData) {
    Add-ExistingPath -RelativePath "data\\mysql" -Reason "MySQL local data directory"
}

if ($IncludeFrontendBuild) {
    Add-ExistingPath -RelativePath "frontend\\dist" -Reason "Frontend build output"
}

Add-MatchingDirectories -DirectoryName "target" -Reason "Maven build output"
Add-MatchingDirectories -DirectoryName "node_modules" -Reason "Node dependency cache"

Add-MatchingFiles -Pattern "hs_err_pid*.log" -Reason "JVM crash log"
Add-MatchingFiles -Pattern "replay_pid*.log" -Reason "JVM replay log"
Add-MatchingFiles -Pattern "*.log" -Reason "Runtime or tool log"
Add-MatchingFiles -Pattern "~$*.doc*" -Reason "Office temporary file"
Add-MatchingFiles -Pattern "dump.rdb" -Reason "Redis dump"

if ($IncludeCourseworkArtifacts) {
    Get-ChildItem -LiteralPath $repoRoot -Force -File -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Extension -in @(".doc", ".docx", ".pdf", ".png", ".jpg", ".jpeg") -and
            $_.Name -notin @("README.md")
        } |
        ForEach-Object {
            if (Test-InRepo -CandidatePath $_.FullName) {
                $items.Add([pscustomobject]@{
                        FullName = $_.FullName
                        Reason   = "Coursework or screenshot artifact"
                    })
            }
        }
}

$planned = $items |
    Sort-Object FullName -Unique

if (-not $planned) {
    Write-Host "No matching cleanup targets found under $repoRoot"
    exit 0
}

$directoryCount = ($planned | Where-Object { Test-Path -LiteralPath $_.FullName -PathType Container }).Count
$fileCount = ($planned | Where-Object { Test-Path -LiteralPath $_.FullName -PathType Leaf }).Count

Write-Host "Repository root: $repoRoot"
Write-Host "Planned cleanup targets: $($planned.Count) items ($directoryCount directories, $fileCount files)"
Write-Host ""

$planned |
    Select-Object FullName, Reason |
    Format-Table -AutoSize

if (-not $Execute) {
    Write-Host ""
    Write-Host "Preview only. Nothing has been deleted."
    Write-Host "Run with -Execute to delete the items above."
    Write-Host "Optional switches:"
    Write-Host "  -IncludeGitMetadata      Also remove .git"
    Write-Host "  -IncludeCourseworkArtifacts  Also remove root-level report/screenshot files"
    Write-Host "  -IncludeMysqlData        Also remove data\\mysql"
    Write-Host "  -IncludeFrontendBuild    Also remove frontend\\dist"
    exit 0
}

foreach ($item in $planned) {
    if (-not (Test-InRepo -CandidatePath $item.FullName)) {
        throw "Refusing to delete path outside repository root: $($item.FullName)"
    }

    if (Test-Path -LiteralPath $item.FullName) {
        Remove-Item -LiteralPath $item.FullName -Recurse -Force
    }
}

Write-Host ""
Write-Host "Cleanup finished."
