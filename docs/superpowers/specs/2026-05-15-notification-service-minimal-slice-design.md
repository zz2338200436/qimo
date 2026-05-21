# Notification Service Minimal Slice Design

Date: 2026-05-15

## Goal

Build the smallest useful `notification-service` slice that closes the current event-driven migration path:

- `assignment-service` already emits `AssignmentSubmittedEvent`
- RabbitMQ relay is already proven at runtime
- the next step is to prove a consumer can receive the event idempotently and persist a notification in `sc_notification`

This slice intentionally does **not** migrate the full notification domain. It focuses on:

- `notification-service` and `notification-service-api` module skeletons
- `AssignmentSubmittedEvent` consumption
- persistence to `sc_notification.notifications`
- idempotency via `processed_event`
- the minimum student notification read APIs needed by the existing frontend

## Scope

In scope:

- add `notification-service-api`
- add `notification-service`
- add Flyway `V1__init_notification_schema.sql`
- consume `AssignmentSubmittedEvent`
- generate one student-facing notification record per event
- add read APIs:
  - `GET /api/notifications/student`
  - `GET /api/notifications/student/all`
  - `GET /api/notifications/student/unread-count`
- add tests for:
  - event consumption and idempotency
  - repository mapping
  - controller envelope shape

Out of scope:

- teacher send notification APIs
- notification mark-read / delete APIs
- Gateway route cutover
- batch or broadcast notifications
- consumers for `ExamFinishedEvent` or `EarlyWarningRaisedEvent`
- UI edits

## Assumptions

This slice uses one explicit product assumption:

- `AssignmentSubmittedEvent` creates a **student-facing** informational notification in the notification center, confirming submission receipt.

Reasoning:

- the current student notification page already exists and has stable read APIs
- the current teacher notification page is primarily oriented around manual sending, not inbox reading
- this lets us verify end-to-end event consumption without first inventing a new teacher notification inbox contract

Suggested notification content:

- `type`: `assignment`
- `title`: `作业提交成功`
- `content`: `您提交的作业《{assignmentTitle or fallback}》已收到，请等待教师批改`
- `relatedId`: `assignmentId`

If assignment title enrichment is not available inside this slice, use a safe fallback:

- `您提交的作业已收到，请等待教师批改`

## Data Model

`sc_notification.notifications`

- `id` bigint primary key auto increment
- `student_id` bigint not null
- `teacher_id` bigint null
- `type` varchar(50) not null
- `title` varchar(200) not null
- `content` text not null
- `related_id` bigint null
- `is_read` bit not null default 0
- `created_at` datetime(6) not null default current timestamp(6)

`sc_notification.processed_event`

- reused local idempotency table pattern from `common`
- unique key on `event_id`

## Service Design

`notification-service-api`

- holds DTOs only for notification read responses if needed by Feign later
- no business implementation

`notification-service`

- Spring Boot service on port `8087`
- service name `notification-service`
- depends on `common` and `common-events`
- binds a consumer to the `assignment.submitted` event stream

Main components:

- `NotificationServiceApplication`
- `NotificationRepository`
- `JdbcNotificationRepository`
- `NotificationQueryService`
- `AssignmentSubmittedNotificationHandler`
- `NotificationController`

Idempotency:

- event handler wraps processing with `IdempotentEventHandler`
- duplicate event ids are ignored safely

## Event Handling Flow

1. RabbitMQ delivers `AssignmentSubmittedEvent`
2. handler checks `processed_event`
3. if new:
   - build student notification record
   - insert into `notifications`
   - mark event processed
4. if duplicate:
   - do nothing

This handler should be transaction-bound so notification insert and processed-event insert succeed or fail together.

## API Shape

All APIs continue the existing `ResponseResult` envelope.

`GET /api/notifications/student`

- request:
  - `page` default `1`
  - `size` default `10`
  - `filter` default `all`
  - `X-User-Id` required
- response:
  - `notifications`
  - `total`
  - `page`
  - `size`

`GET /api/notifications/student/all`

- response:
  - list of notifications for current student

`GET /api/notifications/student/unread-count`

- response:
  - integer unread count

## Routing and Cutover

This slice does **not** switch Gateway route ownership yet.

Reason:

- we should first prove local service correctness and event persistence
- current task is consumer-side integration, not traffic cutover

Route cutover can be a later step after:

- service tests pass
- local runtime event consumption is verified
- student notification page can read from the new service without regressions

## Testing Strategy

Follow narrow TDD:

1. failing test for consuming `AssignmentSubmittedEvent`
2. verify duplicate event id does not create duplicate notification
3. failing controller tests for the three student read APIs
4. minimal implementation to pass

Verification for this slice:

- Maven tests for `notification-service` and dependencies
- local runtime smoke:
  - insert or emit one `AssignmentSubmittedEvent`
  - verify one row in `sc_notification.notifications`
  - verify duplicate delivery does not create a second row

## Risks

- current event payload has no assignment title, so title/content enrichment may need fallback wording
- route is not cut over in this slice, so frontend still will not use the new service automatically
- if we later decide submission notifications should target teachers instead, this slice will need contract adjustment

## Recommendation

Implement this exact minimal slice first, then decide between:

- expanding notification reads and write operations
- adding `ExamFinishedEvent` and `EarlyWarningRaisedEvent` consumers
- or cutting `notification-route` in Gateway
