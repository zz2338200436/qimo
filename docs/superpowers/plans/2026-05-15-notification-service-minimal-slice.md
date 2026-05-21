# Notification Service Minimal Slice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a minimal `notification-service` slice that consumes `AssignmentSubmittedEvent`, persists student notifications in `sc_notification`, and exposes the three student notification read APIs used by the existing frontend.

**Architecture:** Add two new Maven modules, reuse `common` for response envelopes and idempotent event handling, and keep the first implementation narrow: one event consumer plus read-only student notification endpoints. Do not cut Gateway traffic in this slice.

**Tech Stack:** Spring Boot 3.5, Spring Cloud Stream RabbitMQ binder, Flyway, Spring JDBC, JUnit 5, MockMvc

---

### Task 1: Scaffold Notification Modules

**Files:**
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service-api\pom.xml`
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\pom.xml`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\pom.xml`

- [ ] **Step 1: Add module entries to the aggregate build**
- [ ] **Step 2: Create `notification-service-api` Maven module**
- [ ] **Step 3: Create `notification-service` Maven module with dependencies on `common`, `common-events`, and `notification-service-api`**
- [ ] **Step 4: Verify Maven can discover the new modules**

### Task 2: Write Failing Consumer and Query Tests

**Files:**
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\test\java\com\_202510007517\platform\notification\service\AssignmentSubmittedNotificationHandlerTest.java`
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\test\java\com\_202510007517\platform\notification\web\NotificationControllerTest.java`
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\test\java\com\_202510007517\platform\notification\repository\JdbcNotificationRepositoryTest.java`

- [ ] **Step 1: Write a failing handler test for one `AssignmentSubmittedEvent` creating one student notification**
- [ ] **Step 2: Run the handler test and confirm it fails because notification classes do not exist yet**
- [ ] **Step 3: Write a failing handler idempotency test for duplicate event ids**
- [ ] **Step 4: Write failing controller tests for:**
  - `GET /api/notifications/student`
  - `GET /api/notifications/student/all`
  - `GET /api/notifications/student/unread-count`
- [ ] **Step 5: Run the controller tests and confirm they fail because the controller does not exist yet**

### Task 3: Add Notification Domain and Repository

**Files:**
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\main\java\com\_202510007517\platform\notification\NotificationServiceApplication.java`
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\main\java\com\_202510007517\platform\notification\repository\NotificationEntity.java`
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\main\java\com\_202510007517\platform\notification\repository\NotificationRepository.java`
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\main\java\com\_202510007517\platform\notification\repository\JdbcNotificationRepository.java`
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\main\resources\db\migration\V1__init_notification_schema.sql`
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\main\resources\application.yml`

- [ ] **Step 1: Implement the minimal notification entity and repository contract**
- [ ] **Step 2: Implement JDBC persistence and query methods for student notification reads**
- [ ] **Step 3: Add Flyway schema for `notifications` and `processed_event`**
- [ ] **Step 4: Add application config for port `8087`, datasource, RabbitMQ, and Eureka name**
- [ ] **Step 5: Run repository tests and make them pass**

### Task 4: Implement Event Consumer with Idempotency

**Files:**
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\main\java\com\_202510007517\platform\notification\service\AssignmentSubmittedNotificationHandler.java`
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\main\java\com\_202510007517\platform\notification\config\AssignmentSubmittedConsumerConfiguration.java`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\main\resources\application.yml`

- [ ] **Step 1: Implement the consumer handler using `IdempotentEventHandler`**
- [ ] **Step 2: Build the notification record with the fallback message text**
- [ ] **Step 3: Bind the consumer to the `assignment.submitted` stream**
- [ ] **Step 4: Run handler tests and make them pass**

### Task 5: Implement Student Read APIs

**Files:**
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\main\java\com\_202510007517\platform\notification\service\NotificationQueryService.java`
- Create: `D:\111\Distributed framework technology\JavaCode\majorassignment\notification-service\src\main\java\com\_202510007517\platform\notification\web\NotificationController.java`

- [ ] **Step 1: Implement query service methods for paged list, full list, and unread count**
- [ ] **Step 2: Implement the three student endpoints using `ResponseResult`**
- [ ] **Step 3: Run controller tests and make them pass**

### Task 6: Update Task Tracking and Verify End-to-End

**Files:**
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\.kiro\specs\spring-cloud-migration\tasks.md`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\docs\migration-plan.md`
- Modify: `D:\111\Distributed framework technology\JavaCode\majorassignment\docs\assignment-integration-checklist.md`

- [ ] **Step 1: Update task status for notification-service scaffold and first consumer slice**
- [ ] **Step 2: Run the focused Maven verification command**
- [ ] **Step 3: If local runtime is practical, emit one `AssignmentSubmittedEvent` and verify one row lands in `sc_notification.notifications`**
- [ ] **Step 4: Summarize remaining gaps explicitly: no Gateway cutover, no mark-read/delete, no teacher send APIs**
