param(
    [string]$OllamaExe = "D:\111\Ollama\Ollama\ollama.exe",
    [string]$JavaExe = "D:\111\java\Program Files\Java\jdk-20\bin\java.exe",
    [string]$EmbeddingModel = "qwen3-embedding:0.6b",
    [string]$RagDocumentPath = "docs/rag/system-platform-knowledge.md",
    [int]$OllamaPort = 11436,
    [int]$AgentPort = 8092,
    [int]$OllamaReadyTimeoutSeconds = 40,
    [int]$AgentReadyTimeoutSeconds = 80
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeRoot = Join-Path $repoRoot ".runtime-logs"
$ollamaLogRoot = Join-Path $runtimeRoot "ollama"
$javaLogRoot = Join-Path $runtimeRoot "java-services"
$agentJar = Join-Path $repoRoot "agent-service\target\agent-service-0.1.0-SNAPSHOT.jar"
$ollamaBaseUrl = "http://127.0.0.1:$OllamaPort"

function Resolve-PowerShell {
    $pwsh = Get-Command pwsh -ErrorAction SilentlyContinue
    if ($pwsh) {
        return $pwsh.Source
    }

    $powershell = Get-Command powershell -ErrorAction SilentlyContinue
    if ($powershell) {
        return $powershell.Source
    }

    throw "PowerShell executable was not found."
}

function Ensure-Directory {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Test-PortListening {
    param([int]$Port)

    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Wait-HttpReady {
    param(
        [string]$Url,
        [int]$TimeoutSeconds,
        [scriptblock]$SuccessPredicate
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 3
            if (& $SuccessPredicate $response) {
                return $response
            }
        } catch {
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "Timed out waiting for $Url"
}

function Stop-ProcessByPort {
    param([int]$Port)

    $listeners = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        Stop-Process -Id $listener.OwningProcess -Force -ErrorAction SilentlyContinue
    }
}

function Start-OllamaCpuServer {
    if (-not (Test-Path -LiteralPath $OllamaExe)) {
        throw "Ollama executable not found: $OllamaExe"
    }

    Stop-ProcessByPort -Port $OllamaPort

    $outLog = Join-Path $ollamaLogRoot "ollama-cpu-$OllamaPort.out.log"
    $errLog = Join-Path $ollamaLogRoot "ollama-cpu-$OllamaPort.err.log"
    Remove-Item $outLog, $errLog -Force -ErrorAction SilentlyContinue

    $shell = Resolve-PowerShell
    $escapedOllamaExe = $OllamaExe.Replace("'", "''")
    $escapedOutLog = $outLog.Replace("'", "''")
    $escapedErrLog = $errLog.Replace("'", "''")
    $command = @"
`$env:OLLAMA_HOST = '127.0.0.1:$OllamaPort'
`$env:OLLAMA_LLM_LIBRARY = 'cpu_avx2'
`$env:OLLAMA_CONTEXT_LENGTH = '2048'
& '$escapedOllamaExe' serve 1>> '$escapedOutLog' 2>> '$escapedErrLog'
"@
    $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))

    Start-Process -FilePath $shell `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", $encodedCommand) `
        -WorkingDirectory $repoRoot `
        -WindowStyle Hidden | Out-Null

    Wait-HttpReady -Url "$ollamaBaseUrl/api/tags" -TimeoutSeconds $OllamaReadyTimeoutSeconds -SuccessPredicate {
        param($response)
        return $null -ne $response.models
    } | Out-Null
}

function Start-AgentServiceWithCpuRag {
    if (-not (Test-Path -LiteralPath $JavaExe)) {
        throw "Java executable not found: $JavaExe"
    }
    if (-not (Test-Path -LiteralPath $agentJar)) {
        throw "Agent service jar not found: $agentJar"
    }

    Stop-ProcessByPort -Port $AgentPort

    $outLog = Join-Path $javaLogRoot "agent-service.rag-cpu.out.log"
    $errLog = Join-Path $javaLogRoot "agent-service.rag-cpu.err.log"
    Remove-Item $outLog, $errLog -Force -ErrorAction SilentlyContinue

    $shell = Resolve-PowerShell
    $escapedJavaExe = $JavaExe.Replace("'", "''")
    $escapedAgentJar = $agentJar.Replace("'", "''")
    $escapedOutLog = $outLog.Replace("'", "''")
    $escapedErrLog = $errLog.Replace("'", "''")
    $command = @"
Set-Location -LiteralPath '$($repoRoot.Replace("'", "''"))'
`$env:SPRING_PROFILES_ACTIVE = 'dev'
`$env:CONFIG_SERVER_URL = 'http://localhost:8888'
`$env:EUREKA_SERVER_URL = 'http://localhost:8761/eureka/'
`$env:REDIS_HOST = 'localhost'
`$env:REDIS_PORT = '6379'
`$env:RABBITMQ_HOST = 'localhost'
`$env:RABBITMQ_PORT = '5672'
`$env:AGENT_RAG_ENABLED = 'true'
`$env:AGENT_RAG_DOCUMENT_PATH = '$RagDocumentPath'
`$env:OLLAMA_BASE_URL = '$ollamaBaseUrl'
`$env:OLLAMA_EMBEDDING_MODEL = '$EmbeddingModel'
& '$escapedJavaExe' -Xms96m -Xmx224m -XX:MaxMetaspaceSize=160m -Xss256k -XX:ReservedCodeCacheSize=48m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=2 -jar '$escapedAgentJar' 1>> '$escapedOutLog' 2>> '$escapedErrLog'
"@
    $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))

    Start-Process -FilePath $shell `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", $encodedCommand) `
        -WorkingDirectory $repoRoot `
        -WindowStyle Hidden | Out-Null

    Wait-HttpReady -Url "http://127.0.0.1:$AgentPort/actuator/health" -TimeoutSeconds $AgentReadyTimeoutSeconds -SuccessPredicate {
        param($response)
        return $response.status -eq 'UP'
    } | Out-Null
}

Ensure-Directory -Path $runtimeRoot
Ensure-Directory -Path $ollamaLogRoot
Ensure-Directory -Path $javaLogRoot

Start-OllamaCpuServer
Start-AgentServiceWithCpuRag

Write-Host "Agent RAG CPU startup complete:"
Write-Host "  Ollama CPU:  $ollamaBaseUrl"
Write-Host "  Agent health: http://127.0.0.1:$AgentPort/actuator/health"
Write-Host "  Ollama logs: $ollamaLogRoot"
Write-Host "  Agent logs:  $javaLogRoot"
