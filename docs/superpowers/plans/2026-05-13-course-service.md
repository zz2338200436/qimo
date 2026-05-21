# Course Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first `Course_Service` slice for teacher course CRUD and course student lookup.

**Architecture:** Add `course-service-api` for DTOs and Feign contracts, then add `course-service` with a JDBC repository, application service, REST controller, Flyway schema, and tests. Keep the monolith route stable and prepare Gateway direct routing.

**Tech Stack:** Java 17, Spring Boot 3.5.3, Spring Cloud OpenFeign, Eureka Client, JDBC, Flyway, JUnit 5, MockMvc.

---

### Task 1: API and Module Skeleton

**Files:**
- Modify: `pom.xml`
- Create: `course-service-api/pom.xml`
- Create: `course-service/pom.xml`
- Create: `course-service-api/src/main/java/com/_202510007517/platform/course/api/dto/*.java`
- Create: `course-service-api/src/main/java/com/_202510007517/platform/course/api/feign/CourseFeignClient.java`

- [ ] Add both modules to the root Maven aggregator.
- [ ] Define DTOs matching the existing `Course` response surface.
- [ ] Define Feign methods for list/detail/create/update/delete/students.

### Task 2: Service Behavior

**Files:**
- Test: `course-service/src/test/java/com/_202510007517/platform/course/service/CourseApplicationServiceTest.java`
- Create: `course-service/src/main/java/com/_202510007517/platform/course/service/CourseApplicationService.java`
- Create: `course-service/src/main/java/com/_202510007517/platform/course/repository/*.java`

- [ ] Write failing tests for teacher course list and date validation.
- [ ] Implement a repository port and application service.
- [ ] Map student counts into course DTOs.

### Task 3: REST Controller and Schema

**Files:**
- Test: `course-service/src/test/java/com/_202510007517/platform/course/controller/CourseControllerTest.java`
- Create: `course-service/src/main/java/com/_202510007517/platform/course/controller/CourseController.java`
- Create: `course-service/src/main/resources/application.yml`
- Create: `course-service/src/main/resources/db/migration/V1__init_course_schema.sql`

- [ ] Write failing controller tests for response wrapper compatibility.
- [ ] Implement REST endpoints under `/api/teacher/courses`.
- [ ] Add Flyway schema for `sc_course` course/class tables.

### Task 4: Routing and Verification

**Files:**
- Modify: `gateway/src/main/resources/application.yml`
- Modify: `.kiro/specs/spring-cloud-migration/tasks.md`

- [ ] Add `course-route` before `legacy-route`.
- [ ] Mark task 21.1 complete and leave 21.2/21.3 partially complete if the service slice is not full table migration.
- [ ] Run `mvn -q -pl course-service -am test`.
- [ ] Run `mvn -q test`.
