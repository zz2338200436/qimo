# Dev Local Stack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a one-command local development stack script that starts Docker infra plus all local JVM microservices from source, supports `-Exclude`, and does not depend on `major_assignment`.

**Architecture:** Keep `scripts/start-idea-dev-frontend.ps1` and `scripts/start-dev-infra.ps1` as the retained lower-level building blocks. Add a new `scripts/start-dev-local-stack.ps1` orchestration layer that starts Docker infra, reuses the frontend script, runs one root Maven install, launches JVM services in the background with logs, and waits for health checks before returning.

**Tech Stack:** PowerShell scripts, Node.js contract verifier, Maven multi-module build, Spring Boot Actuator

---

### Task 1: Add a failing contract verifier for the new startup path

**Files:**
- Create: `scripts/verify-dev-local-stack-contract.js`
- Test: `scripts/verify-dev-local-stack-contract.js`

- [ ] **Step 1: Write the failing contract verifier**

```js
const fs = require('node:fs');

function read(path) {
  return fs.readFileSync(path, 'utf8');
}

function assertExists(path, message) {
  if (!fs.existsSync(path)) {
    throw new Error(`${message} Missing path: ${path}`);
  }
}

function assertIncludes(content, needle, message) {
  if (!content.includes(needle)) {
    throw new Error(`${message} Missing: ${needle}`);
  }
}

function assertNotIncludes(content, needle, message) {
  if (content.includes(needle)) {
    throw new Error(`${message} Unexpected: ${needle}`);
  }
}

assertExists(
  'scripts/start-dev-local-stack.ps1',
  'Local dev stack workflow should provide a top-level startup script.'
);

const localStack = read('scripts/start-dev-local-stack.ps1');
const infraScript = read('scripts/start-dev-infra.ps1');
const frontendReadme = read('frontend/README.md');

assertIncludes(
  infraScript,
  'param(',
  'Infrastructure startup script should expose parameters for stack composition.'
);
assertIncludes(
  infraScript,
  'mysql',
  'Infrastructure startup script should still support MySQL startup.'
);
assertIncludes(
  localStack,
  'start-dev-infra.ps1',
  'Local dev stack script should reuse the shared infrastructure startup script.'
);
assertIncludes(
  localStack,
  'start-idea-dev-frontend.ps1',
  'Local dev stack script should reuse the retained frontend startup script.'
);
assertIncludes(
  localStack,
  '-SkipDocker',
  'Local dev stack script should prevent duplicate Docker startup when delegating frontend startup.'
);
assertIncludes(
  localStack,
  'mvn -DskipTests install',
  'Local dev stack script should build shared Maven modules before launching services.'
);
assertIncludes(
  localStack,
  '[string[]]$Exclude',
  'Local dev stack script should support excluding selected services.'
);
assertIncludes(
  localStack,
  '.runtime-logs',
  'Local dev stack script should write service logs into .runtime-logs.'
);
assertIncludes(
  localStack,
  'registry-server',
  'Local dev stack script should include registry-server in the default local JVM service set.'
);
assertIncludes(
  localStack,
  'config-server',
  'Local dev stack script should include config-server in the default local JVM service set.'
);
assertIncludes(
  localStack,
  'gateway',
  'Local dev stack script should include gateway in the default local JVM service set.'
);
assertNotIncludes(
  localStack,
  'major_assignment',
  'Local dev stack script should not depend on the transition monolith.'
);
assertIncludes(
  frontendReadme,
  'start-dev-local-stack.ps1',
  'Frontend README should mention the optional full local stack command.'
);

console.log('Dev local stack contract OK');
```

- [ ] **Step 2: Run the verifier to confirm the initial RED state**

Run: `node .\scripts\verify-dev-local-stack-contract.js`

Expected: FAIL with a message similar to:

```text
Error: Local dev stack workflow should provide a top-level startup script. Missing path: scripts/start-dev-local-stack.ps1
```

- [ ] **Step 3: Keep this verifier as the contract for the new startup path**

The verifier must remain focused on:

- the new top-level script existing
- the new script reusing the retained scripts
- infra-only Docker composition
- `-Exclude` support
- background logging
- `major_assignment` exclusion
- README discoverability

### Task 2: Extend infrastructure startup to support infra-only composition

**Files:**
- Modify: `scripts/start-dev-infra.ps1`
- Test: `scripts/verify-dev-local-stack-contract.js`

- [ ] **Step 1: Update `start-dev-infra.ps1` to accept a service list parameter**

Replace the current fixed script body with a parameterized version shaped like:

```powershell
param(
    [string[]]$Services = @('mysql', 'redis', 'rabbitmq', 'registry-server', 'config-server')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not $Services -or $Services.Count -eq 0) {
    throw "At least one docker compose service must be specified."
}

Push-Location $repoRoot
try {
    docker compose up -d @Services
} finally {
    Pop-Location
}

Write-Host "Development infrastructure is running:"
foreach ($service in $Services) {
    Write-Host ("  {0}" -f $service)
}
```

- [ ] **Step 2: Preserve the current default behavior**

The default parameter value must still start:

```powershell
@('mysql', 'redis', 'rabbitmq', 'registry-server', 'config-server')
```

This keeps existing callers compatible.

- [ ] **Step 3: Run the contract verifier to see the next failure**

Run: `node .\scripts\verify-dev-local-stack-contract.js`

Expected: FAIL on the missing `scripts/start-dev-local-stack.ps1` or README reference, not on `start-dev-infra.ps1`.

### Task 3: Implement the top-level local stack startup script

**Files:**
- Create: `scripts/start-dev-local-stack.ps1`
- Test: `scripts/verify-dev-local-stack-contract.js`

- [ ] **Step 1: Define parameters and service metadata**

Create `scripts/start-dev-local-stack.ps1` with a service table that includes:

```powershell
param(
    [string[]]$Exclude = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeLogs = Join-Path $repoRoot ".runtime-logs"

$services = @(
    @{ Name = 'registry-server'; Port = 8761; Pom = 'registry-server\pom.xml'; HealthUrl = 'http://localhost:8761' },
    @{ Name = 'config-server'; Port = 8888; Pom = 'config-server\pom.xml'; HealthUrl = 'http://localhost:8888/actuator/health' },
    @{ Name = 'auth-service'; Port = 8081; Pom = 'auth-service\pom.xml'; HealthUrl = 'http://localhost:8081/actuator/health' },
    @{ Name = 'user-service'; Port = 8082; Pom = 'user-service\pom.xml'; HealthUrl = 'http://localhost:8082/actuator/health' },
    @{ Name = 'course-service'; Port = 8083; Pom = 'course-service\pom.xml'; HealthUrl = 'http://localhost:8083/actuator/health' },
    @{ Name = 'assignment-service'; Port = 8084; Pom = 'assignment-service\pom.xml'; HealthUrl = 'http://localhost:8084/actuator/health' },
    @{ Name = 'exam-service'; Port = 8085; Pom = 'exam-service\pom.xml'; HealthUrl = 'http://localhost:8085/actuator/health' },
    @{ Name = 'analysis-service'; Port = 8086; Pom = 'analysis-service\pom.xml'; HealthUrl = 'http://localhost:8086/actuator/health' },
    @{ Name = 'notification-service'; Port = 8087; Pom = 'notification-service\pom.xml'; HealthUrl = 'http://localhost:8087/actuator/health' },
    @{ Name = 'ai-service'; Port = 8088; Pom = 'ai-service\pom.xml'; HealthUrl = 'http://localhost:8088/actuator/health' },
    @{ Name = 'legacy-adapter'; Port = 8091; Pom = 'legacy-adapter\pom.xml'; HealthUrl = 'http://localhost:8091/actuator/health' },
    @{ Name = 'agent-service'; Port = 8092; Pom = 'agent-service\pom.xml'; HealthUrl = 'http://localhost:8092/actuator/health' },
    @{ Name = 'gateway'; Port = 8080; Pom = 'gateway\pom.xml'; HealthUrl = 'http://localhost:8080/actuator/health' }
)
```

- [ ] **Step 2: Add helper functions for filtering, cleanup, startup, and waiting**

Implement helpers with this shape:

```powershell
function Normalize-Name {
    param([string]$Value)
    return $Value.Trim().ToLowerInvariant()
}

function Stop-PortProcess {
    param([int]$Port)

    $listeners = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        Stop-Process -Id $listener.OwningProcess -Force -ErrorAction Stop
    }
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [string]$Label,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                return
            }
        } catch {
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "$Label did not become healthy at $Url within $TimeoutSeconds seconds."
}
```

- [ ] **Step 3: Reuse the retained infra and frontend scripts**

Call the retained scripts exactly through their file paths:

```powershell
& (Join-Path $repoRoot 'scripts\start-dev-infra.ps1') -Services @('mysql', 'redis', 'rabbitmq')
& (Join-Path $repoRoot 'scripts\start-idea-dev-frontend.ps1') -SkipDocker -NoBrowser
Wait-HttpOk -Url 'http://localhost:5500' -Label 'frontend dev server' -TimeoutSeconds 30
```

- [ ] **Step 4: Run one root Maven install before launching services**

Add:

```powershell
Push-Location $repoRoot
try {
    mvn -DskipTests install
} finally {
    Pop-Location
}
```

- [ ] **Step 5: Start each included service in the background with logs**

Use `Start-Process` with `mvn -f <pom> spring-boot:run`, environment setup, and per-service log files:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:CONFIG_SERVER_URL = 'http://localhost:8888'

$outLog = Join-Path $runtimeLogs "$($service.Name).out.log"
$errLog = Join-Path $runtimeLogs "$($service.Name).err.log"
Remove-Item $outLog, $errLog -Force -ErrorAction SilentlyContinue

Start-Process -FilePath 'mvn' `
    -ArgumentList @('-f', (Join-Path $repoRoot $service.Pom), 'spring-boot:run') `
    -WorkingDirectory $repoRoot `
    -WindowStyle Hidden `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog | Out-Null
```

- [ ] **Step 6: Wait for services in dependency order**

Wait in this order:

```powershell
registry-server
config-server
auth-service
user-service
course-service
assignment-service
exam-service
analysis-service
notification-service
ai-service
legacy-adapter
agent-service
gateway
```

Skip health waits for excluded services.

- [ ] **Step 7: Print concise success output**

Print at minimum:

```powershell
Write-Host 'Local dev stack is running.'
Write-Host 'Frontend: http://localhost:5500'
Write-Host 'Gateway:  http://localhost:8080'
Write-Host 'Config:   http://localhost:8888'
Write-Host 'Eureka:   http://localhost:8761'
Write-Host "Logs:     $runtimeLogs"
```

- [ ] **Step 8: Run the contract verifier**

Run: `node .\scripts\verify-dev-local-stack-contract.js`

Expected: PASS with `Dev local stack contract OK`

### Task 4: Document the optional full local stack workflow

**Files:**
- Modify: `frontend/README.md`
- Test: `scripts/verify-dev-local-stack-contract.js`

- [ ] **Step 1: Add the optional startup path to the README**

Add a short section after the retained frontend script command with wording shaped like:

```md
If you want one command for the full local microservice development chain, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-dev-local-stack.ps1
```

This starts Docker only for `mysql`, `redis`, and `rabbitmq`, then runs the frontend preview and all microservices locally from source. Use `-Exclude` to leave selected services under direct IDEA control.
```

- [ ] **Step 2: Keep the retained workflow text intact**

Do not remove the current statement that `start-idea-dev-frontend.ps1` is the retained local startup script. The new README text must present the full local stack as an optional convenience entrypoint, not as a replacement for the existing documented flow.

- [ ] **Step 3: Re-run the contract verifier**

Run: `node .\scripts\verify-dev-local-stack-contract.js`

Expected: PASS with `Dev local stack contract OK`

### Task 5: Verify the script behavior with a focused startup run

**Files:**
- Test: `scripts/start-dev-local-stack.ps1`
- Test: `scripts/verify-dev-local-stack-contract.js`

- [ ] **Step 1: Run the contract verifier fresh**

Run: `node .\scripts\verify-dev-local-stack-contract.js`

Expected: PASS with `Dev local stack contract OK`

- [ ] **Step 2: Run the new stack script with exclusions to keep the verification focused**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-dev-local-stack.ps1 -Exclude user-service,course-service,assignment-service,exam-service,analysis-service,notification-service,ai-service,legacy-adapter,agent-service
```

Expected:

- Docker starts `mysql`, `redis`, and `rabbitmq`
- frontend starts on `5500`
- local JVM startup covers `registry-server`, `config-server`, `auth-service`, and `gateway`
- the script exits only after those services are healthy

- [ ] **Step 3: Verify the focused runtime endpoints**

Run:

```powershell
curl.exe -I http://localhost:5500
curl.exe -I http://localhost:8761
curl.exe -I http://localhost:8888/actuator/health
curl.exe -I http://localhost:8081/actuator/health
curl.exe -I http://localhost:8080/api/public/captcha
```

Expected:

- `5500` returns `200`
- `8761` returns `200`
- `8888/actuator/health` returns `200`
- `8081/actuator/health` returns `200`
- `8080/api/public/captcha` returns `200`

- [ ] **Step 4: Commit**

```bash
git add scripts/start-dev-infra.ps1 scripts/start-dev-local-stack.ps1 scripts/verify-dev-local-stack-contract.js frontend/README.md docs/superpowers/specs/2026-06-21-dev-local-stack-design.md docs/superpowers/plans/2026-06-21-dev-local-stack-implementation.md
git commit -m "feat: add local microservice stack startup script"
```
