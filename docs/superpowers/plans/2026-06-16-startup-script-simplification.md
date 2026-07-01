# Startup Script Simplification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse local development startup to one retained script, remove jar-based launch helpers, and align active docs and workflow verification with the IDEA-based path.

**Architecture:** Keep `scripts/start-idea-dev-frontend.ps1` as the single local startup entrypoint. Use `scripts/verify-idea-dev-workflow-contract.js` as the contract test for the retained workflow, then delete legacy jar startup scripts and the verifier files that only exist to validate them. Update the active docs that currently steer developers to the removed path.

**Tech Stack:** PowerShell scripts, Node.js verification scripts, Markdown docs, Python report generator

---

### Task 1: Extend the IDEA workflow contract to cover legacy-script removal

**Files:**
- Modify: `scripts/verify-idea-dev-workflow-contract.js`
- Test: `scripts/verify-idea-dev-workflow-contract.js`

- [ ] **Step 1: Add failing assertions for the desired end state**

```js
function assertExists(path, message) {
  if (!fs.existsSync(path)) {
    throw new Error(`${message} Missing path: ${path}`);
  }
}

function assertNotExists(path, message) {
  if (fs.existsSync(path)) {
    throw new Error(`${message} Unexpected path: ${path}`);
  }
}

const frontendReadme = fs.existsSync('frontend/README.md')
  ? read('frontend/README.md')
  : '';
const manualAcceptance = fs.existsSync('docs/manual-acceptance-test-plan.md')
  ? read('docs/manual-acceptance-test-plan.md')
  : '';
const reportGenerator = fs.existsSync('scripts/generate_course_report.py')
  ? read('scripts/generate_course_report.py')
  : '';

assertExists(
  'scripts/start-idea-dev-frontend.ps1',
  'IDEA dev workflow should keep the single retained startup script.'
);
assertNotExists(
  'scripts/start-runtime-smoke-stack.ps1',
  'IDEA dev workflow should not keep the legacy jar smoke-stack launcher.'
);
assertNotExists(
  'scripts/start-java-service.ps1',
  'IDEA dev workflow should not keep the legacy single-service jar launcher.'
);
assertNotIncludes(
  manualAcceptance,
  'verify-runtime-jvm-limits.js',
  'Manual acceptance guidance should not point to the removed jar-launcher verifier.'
);
assertNotIncludes(
  reportGenerator,
  'start-runtime-smoke-stack.ps1',
  'Report generator should not describe jar-smoke startup as the local workflow.'
);
```

- [ ] **Step 2: Run the contract to verify it fails before implementation**

Run: `node .\scripts\verify-idea-dev-workflow-contract.js`

Expected: FAIL because `scripts/start-runtime-smoke-stack.ps1`, `scripts/start-java-service.ps1`, and the stale doc references still exist.

- [ ] **Step 3: Keep the failure output as the acceptance target for implementation**

Expected failure shape:

```text
Error: IDEA dev workflow should not keep the legacy jar smoke-stack launcher. Unexpected path: scripts/start-runtime-smoke-stack.ps1
```

### Task 2: Remove jar-based launchers and update active guidance

**Files:**
- Delete: `scripts/start-runtime-smoke-stack.ps1`
- Delete: `scripts/start-java-service.ps1`
- Delete: `scripts/verify-start-java-service-contract.ps1`
- Delete: `scripts/verify-runtime-smoke-stack-agent-contract.js`
- Delete: `scripts/verify-runtime-smoke-stack-jvm-contract.js`
- Delete: `scripts/verify-runtime-jvm-limits.js`
- Modify: `frontend/README.md`
- Modify: `docs/manual-acceptance-test-plan.md`
- Modify: `scripts/generate_course_report.py`

- [ ] **Step 1: Delete the legacy startup and legacy-script-only verifier files**

Remove these files exactly:

```text
scripts/start-runtime-smoke-stack.ps1
scripts/start-java-service.ps1
scripts/verify-start-java-service-contract.ps1
scripts/verify-runtime-smoke-stack-agent-contract.js
scripts/verify-runtime-smoke-stack-jvm-contract.js
scripts/verify-runtime-jvm-limits.js
```

- [ ] **Step 2: Make the frontend README state the retained local path clearly**

Ensure the local development section contains this retained command and wording:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-idea-dev-frontend.ps1
```

And add one explicit sentence:

```text
This is the only retained local startup script. Start Java services from IDEA rather than jar launchers.
```

- [ ] **Step 3: Update the manual acceptance guide to the retained workflow**

Replace the precondition and regression-evidence wording with:

```md
- 运行 `powershell -ExecutionPolicy Bypass -File .\scripts\start-idea-dev-frontend.ps1` 启动基础设施和前端预览。
- 通过 IDEA 以 `SPRING_PROFILES_ACTIVE=dev` 和 `CONFIG_SERVER_URL=http://localhost:8888` 启动 Java 服务。
```

Replace the removed verifier in the command list:

```powershell
node scripts\verify-idea-dev-workflow-contract.js
node scripts\verify-idea-dev-runtime-smoke.js
```

- [ ] **Step 4: Update the report generator to describe IDEA startup instead of jar smoke startup**

Replace the old run steps with this shape:

```python
["1", "powershell -ExecutionPolicy Bypass -File scripts/start-idea-dev-frontend.ps1", "启动 MySQL、Redis、RabbitMQ、registry-server、config-server 和前端 5500 预览。"],
["2", "在 IDEA 中以 SPRING_PROFILES_ACTIVE=dev、CONFIG_SERVER_URL=http://localhost:8888 启动 Java 服务", "启动 gateway 与各业务服务。"],
["3", "powershell -ExecutionPolicy Bypass -File scripts/get-dev-auth-session.ps1 -Role teacher", "获取教师端 JWT 会话。"],
["4", "powershell -ExecutionPolicy Bypass -File scripts/get-dev-auth-session.ps1 -Role student", "获取学生端 JWT 会话。"],
["5", "node scripts/verify-idea-dev-runtime-smoke.js", "验证 5500 -> gateway -> auth-service/agent-service 开发链路。"],
```

Replace the startup-complexity mitigation row with:

```python
["本地多服务启动复杂", "服务数量多、端口和依赖多。", "保留 start-idea-dev-frontend.ps1 负责基础设施和前端，并通过 IDEA 运行 Java 服务。", "降低日常开发和调试成本。"],
```

### Task 3: Re-run the workflow contract and search for active stale references

**Files:**
- Test: `scripts/verify-idea-dev-workflow-contract.js`
- Test: `scripts/verify-idea-dev-runtime-smoke.js`

- [ ] **Step 1: Re-run the contract test**

Run: `node .\scripts\verify-idea-dev-workflow-contract.js`

Expected: PASS with `IDEA dev workflow contract OK`

- [ ] **Step 2: Search for removed script names in active guidance**

Run:

```powershell
rg -n "start-runtime-smoke-stack|start-java-service|verify-runtime-jvm-limits|verify-start-java-service-contract|verify-runtime-smoke-stack-agent-contract|verify-runtime-smoke-stack-jvm-contract" -S .\frontend .\scripts .\docs\manual-acceptance-test-plan.md
```

Expected: no matches

- [ ] **Step 3: Re-run the retained runtime smoke if the current local services are already up**

Run: `node .\scripts\verify-idea-dev-runtime-smoke.js`

Expected: PASS with `IDEA dev runtime smoke passed`
