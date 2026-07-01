# Course Report Spring Cloud Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove irrelevant iFlytek/vendor-specific wording from the course report, refocus all narrative on the actual Spring Cloud project, redraw the architecture diagram with a cleaner Spring Cloud layout, and keep the repository docs consistent with the regenerated report.

**Architecture:** Keep the existing report-generation pipeline centered on `scripts/generate_course_report.py`, but tighten its narrative to describe only repository-grounded modules, flows, and runtime modes. Treat the report generator, `docs/architecture.md`, and `docs/deployment.md` as one documentation surface so the regenerated report and checked-in Markdown tell the same Spring Cloud story.

**Tech Stack:** Python (`python-docx`, Pillow), Markdown docs, PowerShell verification, Spring Cloud project structure already present in the repo.

---

### Task 1: Map report and doc cleanup scope

**Files:**
- Modify: `scripts/generate_course_report.py`
- Modify: `docs/architecture.md`
- Modify: `docs/deployment.md`
- Test: `rg` searches over report/doc keywords

- [ ] **Step 1: Identify all vendor-specific AI references**

Run:

```powershell
rg -n "讯飞|星火|iFlytek|Spark|科大讯飞|iflytek|spark" scripts docs -S
```

Expected: matching lines in `scripts/generate_course_report.py` and any repo docs that still mention those terms.

- [ ] **Step 2: Identify current architecture diagram entry points**

Run:

```powershell
rg -n "make_architecture_diagram|架构总览图|总体架构|Gateway|Eureka|Config Server" scripts/generate_course_report.py docs/architecture.md docs/deployment.md -S
```

Expected: the report generator’s diagram function plus the Markdown architecture/deployment overview sections.

- [ ] **Step 3: Confirm current report output names**

Run:

```powershell
Get-Content scripts/generate_course_report.py | Select-String -Pattern "OUTPUT =|OUTPUT_PDF ="
```

Expected: output paths pointing to the root-level `.docx` and `.pdf` report artifacts.

### Task 2: Refocus the generated report narrative on the real project

**Files:**
- Modify: `scripts/generate_course_report.py`
- Test: keyword searches against the updated file

- [ ] **Step 1: Remove the dedicated iFlytek section and replace it with project-grounded AI description**

Change the report sections that currently describe “讯飞开放平台能力” so they describe only the repository’s actual `ai-service`, `AiModelClient` abstraction, and current local/mock runtime behavior.

- [ ] **Step 2: Remove all “可替换为讯飞星火” style wording**

Update AI-related tables, appendix rows, and future-work prose so they say only what the project currently implements or can evolve internally, without naming unrelated vendors.

- [ ] **Step 3: Recenter the report’s key-technology narrative on Spring Cloud**

Strengthen wording around `Gateway`, `Eureka`, `Config Server`, `OpenFeign`, `RabbitMQ`, schema ownership, and runtime smoke coverage so the report reads as a Spring Cloud project report first, not a generic AI platform report.

- [ ] **Step 4: Verify the source file is clean**

Run:

```powershell
rg -n "讯飞|星火|iFlytek|Spark|科大讯飞|iflytek|spark" scripts/generate_course_report.py -S
```

Expected: no matches.

### Task 3: Redraw the main architecture diagram with a cleaner Spring Cloud layout

**Files:**
- Modify: `scripts/generate_course_report.py`
- Test: regenerate report assets and inspect generated image output

- [ ] **Step 1: Simplify the architecture diagram layering**

Update `make_architecture_diagram(...)` so the composition is clearly grouped into:

```text
访问层: browser/frontend
治理层: gateway, registry-server, config-server
业务服务层: auth/user/course/assignment/exam/analysis/notification/ai/agent/legacy-adapter as needed
基础设施层: mysql, redis, rabbitmq, observability
```

- [ ] **Step 2: Reduce line crossings**

Use straighter vertical/horizontal flows from `Gateway` downward to services and from services downward to infrastructure, avoiding fan-out diagonals that overlap labels.

- [ ] **Step 3: Keep labels strictly project-related**

Ensure box titles and subtitles name only modules and responsibilities that exist in this repository. Do not introduce unrelated cloud products or platform abstractions.

### Task 4: Sync Markdown docs to the same Spring Cloud framing

**Files:**
- Modify: `docs/architecture.md`
- Modify: `docs/deployment.md`
- Test: read back key sections and keyword-search for removed content

- [ ] **Step 1: Replace placeholder architecture overview**

Update `docs/architecture.md` section 2 so the Mermaid diagram and surrounding text describe the real project topology: frontend -> gateway -> registry/config/governed services -> infra.

- [ ] **Step 2: Make the architecture doc explicitly Spring Cloud centered**

Emphasize `Spring Cloud Gateway`, `Eureka Server`, `Spring Cloud Config`, `OpenFeign`, and service boundaries as the main architectural storyline.

- [ ] **Step 3: Align deployment wording with the report**

Update `docs/deployment.md` so local-stack and compose descriptions align with the same service set and do not imply unrelated vendor/platform dependencies.

- [ ] **Step 4: Verify repo docs are clean**

Run:

```powershell
rg -n "讯飞|星火|iFlytek|Spark|科大讯飞|iflytek|spark" docs -S
```

Expected: no matches in the target docs after cleanup.

### Task 5: Regenerate report artifacts and verify outputs

**Files:**
- Modify/Generate: `智能学习辅助系统-分布式框架技术项目设计文档.docx`
- Modify/Generate: `智能学习辅助系统-分布式框架技术项目设计文档.pdf` (if PDF export path is available)
- Test: `scripts/generate_course_report.py`

- [ ] **Step 1: Run the report generator**

Run:

```powershell
python .\scripts\generate_course_report.py
```

Expected: the `.docx` output path is printed and the Word report is regenerated.

- [ ] **Step 2: Check the regenerated files exist**

Run:

```powershell
Get-Item ".\智能学习辅助系统-分布式框架技术项目设计文档.docx"
Get-Item ".\智能学习辅助系统-分布式框架技术项目设计文档.pdf"
```

Expected: the `.docx` exists; the `.pdf` exists if the generator/export path supports it on this machine.

- [ ] **Step 3: Re-run keyword verification on generated-source inputs**

Run:

```powershell
rg -n "讯飞|星火|iFlytek|Spark|科大讯飞|iflytek|spark" scripts/generate_course_report.py docs/architecture.md docs/deployment.md -S
```

Expected: no matches.

- [ ] **Step 4: Summarize any generator limitations honestly**

If PDF export or visual inspection is blocked by local tooling, record that explicitly when reporting completion.
