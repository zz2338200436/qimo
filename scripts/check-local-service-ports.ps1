param(
    [int[]]$Ports = @(5500, 8080, 8081, 8082, 8083, 8084, 8085, 8761)
)

$rows = foreach ($port in $Ports) {
    $conn = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    [pscustomobject]@{
        Port = $port
        Listening = [bool]$conn
        ProcessId = if ($conn) { $conn.OwningProcess } else { $null }
    }
}

$rows | Format-Table -AutoSize
