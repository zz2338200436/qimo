# Local Dev Stack Design

Date: 2026-06-21
Scope: local development startup workflow under `scripts/`

## Goal

Add a one-command local development entrypoint that starts Docker infrastructure plus all local JVM services from source, without depending on `major_assignment`.

## Problem Summary

The current repository favors this split workflow:

- Docker for infrastructure
- `scripts/start-idea-dev-frontend.ps1` for local frontend preview
- IntelliJ IDEA for local Java service startup

That is workable for focused debugging, but it is slow and repetitive when a developer needs the whole microservice chain running. The repository no longer wants a jar-based runtime stack launcher, but it still needs an optional local full-stack command for development.

The new workflow must preserve these constraints:

- do not move Java services into Docker for daily development
- do not make `major_assignment` part of the default startup path
- do not replace the existing retained startup script
- keep the script practical for developers who are actively editing service code

## Design Decisions

### 1. Add an optional top-level local stack script

Create a new script:

- `scripts/start-dev-local-stack.ps1`

This script becomes an optional convenience entrypoint for developers who want the full local microservice chain running. It does not replace the current documented workflow; it sits above it.

### 2. Keep the current retained scripts and reuse them

Retain:

- `scripts/start-dev-infra.ps1`
- `scripts/start-idea-dev-frontend.ps1`

The new top-level script will reuse the current scripts instead of re-implementing their responsibilities:

- infrastructure startup continues to live in `start-dev-infra.ps1`
- frontend startup continues to live in `start-idea-dev-frontend.ps1`

This keeps the current workflow intact while avoiding duplicate logic.

### 3. Docker remains infrastructure-only

The new workflow will start only these services in Docker:

- `mysql`
- `redis`
- `rabbitmq`

It will not start:

- `registry-server`
- `config-server`
- any Java business service
- `gateway`
- `major_assignment`

This is intentional. If developers are changing code in `registry-server`, `config-server`, or any service module, they should be able to restart those modules locally without Docker rebuilds.

### 4. Start all JVM services locally from source

The new script will start these modules locally:

- `registry-server`
- `config-server`
- `auth-service`
- `user-service`
- `course-service`
- `assignment-service`
- `exam-service`
- `analysis-service`
- `notification-service`
- `ai-service`
- `legacy-adapter`
- `agent-service`
- `gateway`

It will explicitly exclude:

- `major_assignment`

This list covers the current microservice development path and avoids mixing the active microservice chain with the transition monolith by default.

### 5. Build once before service startup

Before launching any local Java service, the script will run:

```powershell
mvn -DskipTests install
```

This is the selected default because the repository is a multi-module Maven build with shared dependencies and parent inheritance. A single up-front build is slower than skipping build entirely, but it is more reliable and simpler to maintain than per-service repeated installs.

### 6. Background startup with runtime logs

Each local Java service will run in the background through `Start-Process`, with logs written to:

- `.runtime-logs/<service>.out.log`
- `.runtime-logs/<service>.err.log`

This avoids opening many visible windows while still preserving per-service diagnostics.

### 7. Automatic port cleanup

If a target port is already listening, the script will stop the owning process before starting that service again. This behavior matches the intended "re-run the stack command to reset local state" workflow.

The cleanup will apply only to the exact ports owned by the known services in this startup set.

### 8. Health-based completion

The script should not report success merely because processes were launched. It must wait for:

- `http://localhost:5500`
- `http://localhost:8761`
- `http://localhost:8888/actuator/health`
- each local service health endpoint where available

If any service fails to become healthy before timeout, the script must exit with an error that identifies the failed service and points developers to that service's logs.

### 9. Support developer exclusions

The new script will support:

- `-Exclude gateway,auth-service`

This allows a developer to keep one or more services under direct IDEA control while still using the script for the rest of the stack.

This is important because the script is meant for local development, not just smoke startup.

## Expected Workflow

Default:

1. run `scripts/start-dev-local-stack.ps1`
2. Docker starts `mysql`, `redis`, and `rabbitmq`
3. frontend starts on `http://localhost:5500`
4. Maven performs one root `install`
5. local JVM services start in dependency order
6. the script waits until all services are healthy
7. the script prints the active URLs and log locations

With exclusions:

1. run `scripts/start-dev-local-stack.ps1 -Exclude gateway,auth-service`
2. Docker and frontend still start
3. all other services start locally
4. the excluded services are left for manual or IDEA startup

## Service Order

The startup order should follow dependency shape:

1. `registry-server`
2. `config-server`
3. core business services and adapters
4. `gateway`

The exact middle ordering can remain a fixed list, because the primary hard dependencies are:

- `config-server` depends on `registry-server`
- all config clients depend on `config-server`
- `gateway` depends on the downstream services being present

## Documentation Changes

Update active documentation so developers can discover the new option without changing the current recommended path:

- `frontend/README.md`

The README should continue to describe the current `start-idea-dev-frontend.ps1` flow, but add the new optional full local stack command and explain that it keeps Java services local rather than containerized.

## Out of Scope

This design does not include:

- deleting `major_assignment`
- migrating remaining legacy routes away from the transition monolith
- changing Docker topology beyond infra-only startup support
- adding a monolith-inclusive mode
- replacing IDEA as the preferred debugger for individually edited services
- adding dynamic dependency graph discovery

## Risks

1. `mvn -DskipTests install` may be slow on first run, but it is still the simplest reliable baseline.
2. Some services may have longer startup times than others, so health timeouts must be generous enough for normal local startup.
3. If local ports are occupied by unrelated processes, automatic cleanup could stop the wrong process if port ownership is not checked carefully.
4. `major_assignment` is intentionally excluded, which means this script covers the primary microservice path but not every remaining legacy compatibility scenario.

## Success Criteria

This change is successful when:

- one command can start the local microservice development stack
- Docker is used only for `mysql`, `redis`, and `rabbitmq`
- `registry-server`, `config-server`, `gateway`, and the other microservices run locally from source
- `major_assignment` is not required for the default path
- developers can exclude selected services for IDEA debugging
- the script reports success only after healthy startup
