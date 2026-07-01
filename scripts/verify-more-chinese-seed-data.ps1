param(
    [string]$SeedPath = "scripts/seed-more-chinese-data.sql",
    [string]$MainSqlPath = "major_assignment.sql"
)

$ErrorActionPreference = "Stop"

function U {
    param([int[]]$Codes)
    return [string]::Concat(($Codes | ForEach-Object { [char]$_ }))
}

$requiredMarkers = @(
    "-- seed-more-chinese-data:start",
    "-- seed-more-chinese-data:end",
    (U 0x738B,0x660E,0x8FDC),
    (U 0x674E,0x96E8,0x6850),
    (U 0x8D75,0x601D,0x6DB5),
    (U 0x9648,0x5609,0x5B81),
    (U 0x5206,0x5E03,0x5F0F,0x6846,0x67B6,0x6280,0x672F),
    (U 0x667A,0x6167,0x8BFE,0x5802,0x6F14,0x793A,0x73ED),
    (U 0x5206,0x5E03,0x5F0F,0x6846,0x67B6,0x6280,0x672F,0x4F5C,0x4E1A,0xFF1A,0x670D,0x52A1,0x6CE8,0x518C,0x4E0E,0x53D1,0x73B0),
    (U 0x5206,0x5E03,0x5F0F,0x6846,0x67B6,0x6280,0x672F,0x9636,0x6BB5,0x6D4B,0x9A8C),
    (U 0x670D,0x52A1,0x6CE8,0x518C,0x4E0E,0x53D1,0x73B0),
    (U 0x7CFB,0x7EDF,0x901A,0x77E5,0xFF1A,0x5B66,0x4E60,0x6570,0x636E,0x5DF2,0x66F4,0x65B0),
    (U 0x8FDE,0x7EED,0x4E24,0x6B21,0x4F5C,0x4E1A,0x4F4E,0x4E8E,0x53CA,0x683C,0x7EBF)
)

function Assert-FileContainsMarkers {
    param(
        [string]$Path,
        [string[]]$Markers
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Missing required file: $Path"
    }

    $content = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
    foreach ($marker in $Markers) {
        if (-not $content.Contains($marker)) {
            throw "Missing marker '$marker' in $Path"
        }
    }
}

Assert-FileContainsMarkers -Path $SeedPath -Markers $requiredMarkers
Assert-FileContainsMarkers -Path $MainSqlPath -Markers $requiredMarkers

Write-Host "Chinese seed data markers found in $SeedPath and $MainSqlPath"
