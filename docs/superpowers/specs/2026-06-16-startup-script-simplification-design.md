# Startup Script Simplification Design

Date: 2026-06-16
Scope: local development startup workflow under `scripts/`

## Goal

Reduce the local development entrypoint to one useful script so daily work no longer depends on jar packaging or multiple competing startup paths.

## Problem Summary

The repository currently mixes three startup directions:

- `scripts/start-idea-dev-frontend.ps1` for Docker infrastructure plus local frontend preview
- `scripts/start-runtime-smoke-stack.ps1` for full jar-based runtime smoke startup
- `scripts/start-java-service.ps1` as a reusable jar launcher

This creates the wrong default for local development. The repository already leans toward "Docker infra + frontend script + Java services started from IDEA", but the extra jar launchers still make the workflow look heavier than it should be.

## Approaches Considered

### 1. Keep all scripts and document one as preferred

Pros:

- smallest file churn
- no contract script changes needed

Cons:

- daily development still has multiple competing entrypoints
- jar-based startup remains visually equal to the recommended IDEA flow

### 2. Keep one daily-dev script and remove jar startup scripts

Pros:

- one clear local development entrypoint
- aligns with the existing IDEA-oriented config and verification scripts
- removes jar packaging pressure from normal debugging

Cons:

- jar smoke startup becomes a manual or future rebuild task instead of an existing one-command script
- some docs and verification scripts must be updated together

### 3. Replace jar scripts with `spring-boot:run` scripts

Pros:

- still scriptable without packaging jars
- keeps Java startup in source form

Cons:

- still leaves multiple startup paths
- weaker than IDEA for multi-service local debugging on this Windows repo
- adds script maintenance without solving the "one obvious entrypoint" problem

## Recommendation

Choose approach 2.

Keep `scripts/start-idea-dev-frontend.ps1` as the only retained startup script for day-to-day local development. Java services should be started from IntelliJ IDEA with the existing `dev` profile and config-server settings.

## Design

### Single retained script

Retain:

- `scripts/start-idea-dev-frontend.ps1`

Its role remains:

- start Docker infrastructure containers needed for local development
- start the local Python frontend preview on `http://localhost:5500`
- print the expected IDEA environment for Java services

### Scripts to remove

Delete:

- `scripts/start-runtime-smoke-stack.ps1`
- `scripts/start-java-service.ps1`

These scripts are both jar-oriented and no longer match the intended local development workflow.

### Reference cleanup

Update repository references that still point at removed scripts or assume jar-based startup:

- `frontend/README.md`
- `scripts/verify-start-java-service-contract.ps1`
- `scripts/verify-runtime-smoke-stack-agent-contract.js`
- `scripts/verify-runtime-smoke-stack-jvm-contract.js`
- `scripts/verify-runtime-jvm-limits.js`
- `scripts/generate_course_report.py`

Any documentation that describes jar startup as a normal local developer path should be corrected to the retained IDEA workflow, or reduced to historical context if it is a delivery record.

### Verification direction

Keep the IDEA-path verification, because it matches the retained workflow:

- `scripts/verify-idea-dev-workflow-contract.js`
- `scripts/verify-idea-dev-runtime-smoke.js`

After cleanup, the expected local flow is:

1. run `scripts/start-idea-dev-frontend.ps1`
2. start Java services from IDEA with `SPRING_PROFILES_ACTIVE=dev` and `CONFIG_SERVER_URL=http://localhost:8888`
3. run `node scripts/verify-idea-dev-runtime-smoke.js`

## In Scope

- script cleanup for local development startup
- removal of jar-oriented startup scripts
- cleanup of direct references to those removed scripts
- documentation and verification alignment with the IDEA workflow

## Out of Scope

- changing Docker service topology
- changing service ports or dev profile semantics
- creating new IntelliJ run configurations
- replacing removed jar smoke scripts with another automated full-stack launcher in this pass
- business code changes in Java services or frontend pages

## Risks and Constraints

1. Some historical docs record old commands as evidence. Those sections should not be rewritten in a way that falsifies prior work.
2. The repo is already dirty, so cleanup must stay tightly scoped to startup-script-related files.
3. Some verifier files exist only to validate removed scripts. Those should be removed instead of being repurposed into misleading checks.

## Verification Plan

1. Re-run `node .\scripts\verify-idea-dev-workflow-contract.js`
2. Confirm `frontend/README.md` points only to the retained startup path
3. Check repository search results so removed script names no longer appear in active local-dev guidance
4. If the current local environment is already up, optionally re-run `node .\scripts\verify-idea-dev-runtime-smoke.js`

## Success Criteria

This change is successful when:

- there is one obvious local startup script for developers
- the retained script matches the intended "infra + frontend, Java from IDEA" workflow
- jar launcher scripts are removed
- active docs and verification files no longer steer developers toward jar packaging for normal local work
