Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$scriptPath = Join-Path $repoRoot "scripts\start-agent-rag-cpu.ps1"

if (-not (Test-Path $scriptPath)) {
    throw "Missing scripts\start-agent-rag-cpu.ps1"
}

$content = Get-Content -LiteralPath $scriptPath -Raw

$requiredSnippets = @(
    '11436',
    'OLLAMA_LLM_LIBRARY',
    'cpu_avx2',
    'AGENT_RAG_ENABLED',
    'AGENT_RAG_DOCUMENT_PATH',
    'docs/rag/system-platform-knowledge.md',
    'OLLAMA_BASE_URL',
    'qwen3-embedding:0.6b',
    'ollamaLogRoot',
    'javaLogRoot',
    'agent-service-0.1.0-SNAPSHOT.jar'
)

foreach ($snippet in $requiredSnippets) {
    if ($content -notlike "*$snippet*") {
        throw "scripts\start-agent-rag-cpu.ps1 is missing expected content: $snippet"
    }
}

$errors = $null
[System.Management.Automation.Language.Parser]::ParseFile($scriptPath, [ref]$null, [ref]$errors) | Out-Null
if ($errors.Count -gt 0) {
    $errors | Format-List *
    throw "scripts\start-agent-rag-cpu.ps1 has PowerShell syntax errors"
}

Write-Host "start-agent-rag-cpu.ps1 contract OK"
