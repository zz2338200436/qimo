# Frontend Extraction

This directory is the stage-2 extraction target for the legacy static frontend that previously lived only under
`major_assignment/src/main/resources/static/`.

## Deployment artifact

The current deployable frontend root is:

`frontend/dist`

For local smoke testing, the repository now starts a lightweight preview server on `http://localhost:5500`
that serves `frontend/dist` and proxies `/api/**` to the Gateway at `http://localhost:8080`.
This keeps browser-based verification aligned with the production "frontend -> gateway -> services" path
without reintroducing page-level hardcoded backend hosts.

For day-to-day IDEA development, use Docker only for infrastructure and run the static frontend locally:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-idea-dev-frontend.ps1
```

This is the only retained local startup script. Start Java services from IDEA rather than jar launchers.

The script starts `mysql`, `redis`, `rabbitmq`, `registry-server`, and `config-server`, then starts the
Python frontend server on `http://localhost:5500`. Start Java business services from IDEA with
`SPRING_PROFILES_ACTIVE=dev` and `CONFIG_SERVER_URL=http://localhost:8888`.

After the IDEA Java services are running, verify the development path with:

```powershell
node .\scripts\verify-idea-dev-runtime-smoke.js
```

This checks `5500 -> gateway -> auth-service` captcha loading, `X-Captcha-Key` exposure, gateway static
frontend routing, and Agent panel assets.

To verify the Agent panel against IDEA-started services, run the Java services with the same dev profile and
disable external LLM calls for deterministic local checks:

```text
SPRING_PROFILES_ACTIVE=dev
CONFIG_SERVER_URL=http://localhost:8888
AGENT_LLM_ENABLED=false
```

Generate fresh browser sessions before each Agent smoke run. The helper requests a new captcha for every
gateway retry, reads the matching Redis captcha value, and writes the authenticated session snapshot:

```powershell
.\scripts\get-dev-auth-session.ps1 -Role teacher -OutFile .runtime-logs\teacher-session-agent-runtime.json
.\scripts\get-dev-auth-session.ps1 -Role student -OutFile .runtime-logs\student-session-agent-runtime.json
```

Then run the Agent runtime smoke:

```powershell
node .\scripts\verify-agent-idea-runtime-smoke.js .runtime-logs\teacher-session-agent-runtime.json .runtime-logs\student-session-agent-runtime.json
```

This covers teacher read-only Agent queries, assignment action preview and confirmation, session detail loading,
and student pending-assignment Agent queries through the real `5500 -> gateway -> agent-service` path.

Nginx deployment config now lives at:

`deploy/nginx/nginx.conf`

That config points the static root at `/usr/share/nginx/html` and proxies `/api/**` traffic to the Gateway upstream.

## Current transition state

The files in `frontend/dist` are a byte-for-byte mirror of the legacy static assets at the time of extraction.

To avoid breaking the monolith during the transition:

- the original files are still kept in `major_assignment/src/main/resources/static/`
- `frontend/dist` is now the externalized frontend deployment target
- Spring Boot static resource mapping is now disabled with `spring.web.resources.add-mappings=false`
- the monolith-side static files remain only as a temporary sync source until jar resource cleanup lands

## Included entry pages

Public entry:

- `index.html`
- `student-login.html`
- `teacher-login.html`

Student pages:

- `student-dashboard.html`
- `student-courses.html`
- `student-assignments.html`
- `student-stats.html`
- `student-notifications.html`
- `student-ai-assistant.html`
- `student-settings.html`

Teacher pages:

- `teacher-dashboard.html`
- `teacher-courses.html`
- `teacher-assignments.html`
- `teacher-student-dashboard.html`
- `teacher-warning.html`
- `teacher-knowledge.html`
- `teacher-ai-tools.html`
- `teacher-notifications.html`
- `teacher-settings.html`

## Local smoke coverage

Browser-level JWT smoke scripts currently cover:

- `scripts/verify-teacher-jwt-pages.js`
  - `teacher-dashboard.html`
  - `teacher-courses.html`
  - `teacher-assignments.html`
  - `teacher-knowledge.html`
  - `teacher-warning.html`
  - `teacher-student-dashboard.html`
  - `teacher-notifications.html`
  - `teacher-settings.html`
  - `teacher-ai-tools.html`
- `scripts/verify-student-jwt-pages.js`
  - `student-dashboard.html`
  - `student-courses.html`
  - `student-assignments.html`
  - `student-notifications.html`
  - `student-settings.html`

These scripts are intended to validate the real preview path:

`frontend/dist (5500) -> /api proxy -> gateway (8080) -> microservices`

Shared assets:

- `api.js`
- `i18n.js`
- `styles.css`
- `components/**`
- `i18n/**`
- `lib/**`

## Sync note

If we need to refresh the extracted frontend before the monolith static mapping is disabled, copy the contents of
`major_assignment/src/main/resources/static/` into `frontend/dist/` again so both locations stay aligned.
