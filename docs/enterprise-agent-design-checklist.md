# Enterprise Agent Design Checklist

## 1. Design Goal

- [ ] Add an enterprise-grade Agent capability to the smart learning platform.
- [ ] Support natural-language business operations across existing platform capabilities.
- [ ] Treat assignment publishing and assignment submission as first-wave examples, not the full Agent scope.
- [ ] Allow course, class, student, assignment, exam, analysis, warning, notification, profile, and AI generation functions to be exposed as Agent tools.
- [x] Keep the Agent as a controlled business execution layer, not a direct database writer.
- [ ] Require confirmation before all write operations.
- [ ] Preserve existing Spring Cloud service boundaries and data ownership rules.
- [ ] Make every Agent operation auditable, traceable, permission-checked, and idempotent.

## 2. Current Project Fit

- [x] Reuse `gateway` as the only public traffic entry.
- [ ] Reuse `auth-service` and `user-service` for identity, role, and profile context.
- [ ] Reuse `course-service` for course, class, and teacher ownership lookup.
- [ ] Reuse `course-service` for teacher course CRUD, class CRUD, class-student management, course assignment, majors, semesters, and student course queries.
- [ ] Reuse `assignment-service` for assignment publishing, updating, querying, deleting, submitting, grading, and knowledge-point binding.
- [ ] Reuse `exam-service` for exam publishing, updating, deleting, submitting, grading, score lookup, and knowledge-point binding.
- [x] Reuse `ai-service` for model calls, prompt execution, and generation records.
- [ ] Reuse `notification-service` for notification query, read-state updates, deletion, single send, and batch send.
- [ ] Reuse `analysis-service` for student statistics, teacher dashboard, learning summary, score trend, knowledge-point analysis, early warning query, status updates, and trigger jobs.
- [ ] Reuse `common` trace headers and exception patterns.
- [ ] Reuse `common-events` and outbox patterns for Agent action events.

## 3. Recommended Service Layout

- [x] Add `agent-service-api`.
- [x] Add `agent-service`.
- [ ] Register `agent-service` through the registry server.
- [x] Route `/api/agent/**` through the gateway.
- [ ] Keep `ai-service` focused on AI generation/model interaction.
- [x] Keep `agent-service` focused on intent recognition, tool orchestration, confirmation, execution, and audit.

Recommended dependency direction:

```text
frontend
  -> gateway
    -> agent-service
      -> ai-service
      -> auth-service
      -> user-service
      -> course-service
      -> assignment-service
      -> exam-service
      -> analysis-service
      -> notification-service
```

## 4. Module Checklist

### 4.1 agent-service-api

- [x] `AgentChatRequestDTO`
- [x] `AgentChatResponseDTO`
- [x] `AgentActionPreviewDTO`
- [x] `AgentActionConfirmDTO`
- [x] `AgentExecutionResultDTO`
- [x] `AgentSessionDTO`
- [x] `AgentMessageDTO`
- [x] `AgentActionDTO`
- [x] `AgentFeignClient`

### 4.2 agent-service Controller Layer

- [x] `AgentController`
- [x] `POST /api/agent/chat`
- [x] `POST /api/agent/actions/{actionId}/confirm`
- [x] `POST /api/agent/actions/{actionId}/cancel`
- [x] `GET /api/agent/sessions`
- [x] `GET /api/agent/sessions/{sessionId}`
- [x] `GET /api/agent/actions/{actionId}`

### 4.3 agent-service Application Layer

- [x] `AgentOrchestrator`
- [x] `IntentRecognitionService`
- [x] `SlotExtractionService`
- [x] `ContextEnrichmentService`
- [x] `ActionPlanningService`
- [x] `ActionConfirmationService`
- [x] `ActionExecutionService`
- [x] `AgentSessionService`
- [x] `AgentAuditService`
- [x] `AgentIdempotencyService`

### 4.4 Tool Layer

- [x] `AgentTool`
- [x] `ToolRegistry`
- [x] `CourseLookupTool`
- [x] `CourseDetailLookupTool`
- [ ] `TeacherCoursePermissionTool`
- [x] `CourseCreateTool`
- [x] `CourseUpdateTool`
- [x] `CourseDeleteTool`
- [x] `ClassLookupTool`
- [x] `ClassCreateTool`
- [ ] `ClassUpdateTool`
- [ ] `ClassDeleteTool`
- [ ] `ClassStudentManageTool`
- [ ] `KnowledgePointLookupTool`
- [ ] `KnowledgePointManageTool`
- [x] `StudentAssignmentLookupTool`
- [ ] `AssignmentLookupTool`
- [x] `AssignmentPublishTool`
- [x] `AssignmentUpdateTool`
- [x] `AssignmentDeleteTool`
- [x] `AssignmentSubmitTool`
- [x] `AssignmentGradeTool`
- [x] `AssignmentDetailLookupTool`
- [x] `AssignmentSubmissionLookupTool`
- [x] `ExamLookupTool`
- [x] `ExamPublishTool`
- [x] `ExamUpdateTool`
- [x] `ExamDeleteTool`
- [x] `ExamDetailLookupTool`
- [x] `ExamSubmissionLookupTool`
- [x] `ExamSubmitTool`
- [x] `ExamGradeTool`
- [x] `ScoreQueryTool`
- [x] `AnalysisQueryTool`
- [x] `KnowledgeMasteryQueryTool`
- [ ] `AnalysisTriggerTool`
- [ ] `EarlyWarningLookupTool`
- [ ] `EarlyWarningManageTool`
- [x] `AiGenerationTool`
- [x] `NotificationTool`
- [ ] `UserProfileTool`

### 4.5 Policy Layer

- [x] `AgentPermissionPolicy`
- [x] `AgentRiskPolicy`
- [x] `AgentConfirmationPolicy`
- [ ] `AgentRateLimitPolicy`
- [x] `AgentDataMaskingPolicy`

## 5. Supported Intents

### 5.1 Intent Coverage Principle

- [ ] Every existing business function can become an Agent intent if it has a stable backend API.
- [ ] Read-only intents may execute directly after permission checks.
- [x] Write intents must generate a preview and wait for confirmation.
- [x] Destructive or batch intents must require second-level confirmation or approval.
- [ ] Agent intent names must map to service-owned tools, not database tables.

### 5.2 First Enterprise Release

- [x] `PUBLISH_ASSIGNMENT`
- [x] `SUBMIT_ASSIGNMENT`
- [x] `QUERY_PENDING_ASSIGNMENTS`
- [x] `QUERY_COURSES`
- [x] `QUERY_ASSIGNMENTS`
- [x] `QUERY_EXAMS`
- [x] `PUBLISH_EXAM`
- [x] `UPDATE_EXAM`
- [x] `SUBMIT_EXAM`
- [x] `QUERY_NOTIFICATIONS`
- [x] `QUERY_STUDENT_STATS`
- [x] `GENERATE_QUESTIONS`
- [x] `GENERATE_EXAM`
- [x] `GENERATE_LEARNING_SUGGESTIONS`
- [x] `UNKNOWN`

### 5.3 Course And Class Intents

- [x] `QUERY_COURSES`
- [x] `QUERY_COURSE_DETAIL`
- [x] `CREATE_COURSE`
- [x] `UPDATE_COURSE`
- [x] `DELETE_COURSE`
- [x] `QUERY_CLASSES`
- [x] `QUERY_CLASS_DETAIL`
- [x] `CREATE_CLASS`
- [ ] `UPDATE_CLASS`
- [ ] `DELETE_CLASS`
- [ ] `ADD_STUDENT_TO_CLASS`
- [ ] `REMOVE_STUDENT_FROM_CLASS`
- [ ] `ASSIGN_COURSE_TO_CLASS`
- [ ] `UNASSIGN_COURSE_FROM_CLASS`
- [ ] `QUERY_MAJORS`
- [ ] `QUERY_SEMESTERS`

### 5.4 Knowledge Point Intents

- [ ] `QUERY_KNOWLEDGE_POINTS`
- [ ] `QUERY_KNOWLEDGE_POINT_DETAIL`
- [ ] `CREATE_KNOWLEDGE_POINT`
- [ ] `UPDATE_KNOWLEDGE_POINT`
- [ ] `DELETE_KNOWLEDGE_POINT`
- [ ] `BIND_ASSIGNMENT_KNOWLEDGE_POINTS`
- [ ] `BIND_EXAM_KNOWLEDGE_POINTS`

### 5.5 Assignment Intents

- [x] `UPDATE_ASSIGNMENT`
- [ ] `EXTEND_ASSIGNMENT_DEADLINE`
- [x] `DELETE_ASSIGNMENT`
- [x] `QUERY_ASSIGNMENT_DETAIL`
- [x] `QUERY_ASSIGNMENT_SUBMISSIONS`
- [x] `GRADE_ASSIGNMENT`

### 5.6 Exam Intents

- [x] `PUBLISH_EXAM`
- [x] `UPDATE_EXAM`
- [x] `DELETE_EXAM`
- [x] `QUERY_EXAM_DETAIL`
- [x] `QUERY_EXAM_SUBMISSIONS`
- [x] `SUBMIT_EXAM`
- [x] `GRADE_EXAM`
- [x] `QUERY_SCORES`

### 5.7 Analysis And Warning Intents

- [x] `QUERY_TEACHER_DASHBOARD`
- [x] `QUERY_LEARNING_SUMMARY`
- [x] `QUERY_SCORE_TREND`
- [x] `QUERY_STUDENT_STATS`
- [x] `QUERY_STUDY_TIME_DISTRIBUTION`
- [x] `QUERY_KNOWLEDGE_MASTERY`
- [ ] `TRIGGER_KNOWLEDGE_ANALYSIS`
- [ ] `TRIGGER_EARLY_WARNING_ANALYSIS`
- [ ] `QUERY_EARLY_WARNINGS`
- [ ] `UPDATE_EARLY_WARNING_STATUS`
- [ ] `RESOLVE_EARLY_WARNING`
- [ ] `DELETE_EARLY_WARNING`
- [ ] `EXPORT_EARLY_WARNINGS`

### 5.8 Notification Intents

- [ ] `QUERY_NOTIFICATIONS`
- [x] `QUERY_UNREAD_NOTIFICATION_COUNT`
- [x] `MARK_NOTIFICATION_READ`
- [x] `MARK_ALL_NOTIFICATIONS_READ`
- [x] `DELETE_NOTIFICATION`
- [x] `DELETE_ALL_READ_NOTIFICATIONS`
- [x] `SEND_NOTIFICATION`
- [x] `SEND_BATCH_NOTIFICATION`

### 5.9 User And Settings Intents

- [ ] `QUERY_MY_PROFILE`
- [ ] `UPDATE_MY_PROFILE`
- [ ] `CHANGE_PASSWORD`
- [ ] `QUERY_NOTIFICATION_SETTINGS`
- [ ] `UPDATE_NOTIFICATION_SETTINGS`
- [ ] `QUERY_PRIVACY_SETTINGS`
- [ ] `UPDATE_PRIVACY_SETTINGS`
- [ ] `EXPORT_MY_DATA`

### 5.10 AI Generation Intents

- [ ] `GENERATE_LEARNING_PLAN`
- [ ] `GENERATE_EXAM`
- [ ] `GENERATE_QUESTIONS`
- [ ] `GENERATE_LEARNING_SUGGESTIONS`
- [ ] `EXPLAIN_KNOWLEDGE_POINT`
- [ ] `GENERATE_TEACHING_PLAN`

## 6. Assignment Publishing Flow

- [ ] User enters a natural-language request.
- [ ] Agent recognizes `PUBLISH_ASSIGNMENT`.
- [ ] Agent extracts assignment slots.
- [x] Agent queries course/class context from `course-service`.
- [x] Agent validates teacher identity and ownership.
- [ ] Agent builds a preview action instead of executing immediately.
- [ ] User confirms the action.
- [ ] Agent calls `assignment-service` assignment creation API.
- [ ] Agent records action result.
- [ ] Agent writes audit log.
- [ ] Agent optionally sends notification through `notification-service`.

Required slots:

- [ ] `title`
- [ ] `description`
- [x] `courseId` or resolvable `courseName`
- [ ] `dueDate`
- [ ] `maxScore`
- [ ] `isActive`

Optional slots:

- [ ] `classIds`
- [ ] `knowledgePointIds`
- [ ] `publishDate`
- [ ] `notificationEnabled`

Existing target contract:

- [ ] `TeacherAssignmentUpsertRequestDTO`
- [ ] `POST /api/teacher/assignments`

## 7. Assignment Submission Flow

- [ ] User enters a natural-language request.
- [ ] Agent recognizes `SUBMIT_ASSIGNMENT`.
- [ ] Agent extracts submission slots.
- [x] Agent resolves assignment by ID, title, or fuzzy match.
- [ ] Agent validates student identity.
- [ ] Agent verifies the assignment is visible to the student.
- [ ] Agent checks whether the assignment has already been submitted.
- [ ] Agent builds a preview action instead of executing immediately.
- [ ] User confirms the action.
- [ ] Agent calls `assignment-service` submission API.
- [ ] Agent records action result.
- [ ] Agent writes audit log.

Required slots:

- [x] `assignmentId` or resolvable `assignmentTitle`
- [ ] `studentId`
- [ ] `content`

Optional slots:

- [ ] `submissionDate`
- [ ] `attachments`
- [ ] `overwriteExistingSubmission`

Existing target contract:

- [ ] `AssignmentSubmitRequestDTO`
- [ ] `POST /internal/assignments/{assignmentId}/submissions`

## 8. Existing Function To Agent Tool Mapping

### 8.1 Auth And User

- [ ] `QUERY_MY_PROFILE` -> `user-service` -> `GET /api/users/me`
- [ ] `UPDATE_MY_PROFILE` -> `user-service` -> `PUT /api/users/me`
- [ ] `QUERY_STUDENT_PROFILE` -> `user-service` -> `GET /api/student/profile`
- [ ] `UPDATE_STUDENT_PROFILE` -> `user-service` -> `PUT /api/student/profile`
- [ ] `CHANGE_PASSWORD` -> `auth-service` -> `POST /api/auth/change-password`
- [ ] `SWITCH_ROLE` -> `auth-service` -> `POST /api/auth/switch-role`
- [ ] `QUERY_NOTIFICATION_SETTINGS` -> `user-service` or `auth-service` compatibility endpoints
- [ ] `UPDATE_NOTIFICATION_SETTINGS` -> `user-service` or `auth-service` compatibility endpoints
- [ ] `QUERY_PRIVACY_SETTINGS` -> `user-service` -> `GET /api/student/privacy-settings`
- [ ] `UPDATE_PRIVACY_SETTINGS` -> `user-service` -> `PUT /api/student/privacy-settings`
- [ ] `EXPORT_MY_DATA` -> `user-service` -> `GET /api/student/export-data`

### 8.2 Course, Class, And Student Management

- [ ] `QUERY_COURSES` -> `course-service` -> `GET /api/teacher/courses` or `GET /api/student/courses`
- [x] `QUERY_COURSE_DETAIL` -> `course-service` -> `GET /api/teacher/courses/{courseId}` or `GET /api/student/courses/{courseId}`
- [x] `CREATE_COURSE` -> `course-service` -> `POST /api/teacher/courses`
- [x] `UPDATE_COURSE` -> `course-service` -> `PUT /api/teacher/courses/{courseId}`
- [x] `DELETE_COURSE` -> `course-service` -> `DELETE /api/teacher/courses/{courseId}`
- [x] `QUERY_CLASSES` -> `course-service` -> `GET /api/teacher/classes`
- [x] `QUERY_CLASS_DETAIL` -> `course-service` -> `GET /api/teacher/classes/{classId}`
- [x] `CREATE_CLASS` -> `course-service` -> `POST /api/teacher/classes`
- [ ] `UPDATE_CLASS` -> `course-service` -> `PUT /api/teacher/classes/{classId}`
- [ ] `DELETE_CLASS` -> `course-service` -> `DELETE /api/teacher/classes/{classId}`
- [ ] `QUERY_CLASS_STUDENTS` -> `course-service` -> `GET /api/teacher/classes/{classId}/students`
- [ ] `ADD_STUDENT_TO_CLASS` -> `course-service` -> `POST /api/teacher/classes/{classId}/students`
- [ ] `ASSIGN_COURSE_TO_CLASS` -> `course-service` -> `POST /api/teacher/course-assignments`
- [ ] `UNASSIGN_COURSE_FROM_CLASS` -> `course-service` -> `DELETE /api/teacher/class-courses/unassign`
- [ ] `QUERY_MAJORS` -> `course-service` -> `GET /api/teacher/majors`
- [ ] `QUERY_SEMESTERS` -> `course-service` -> `GET /api/system/semesters`

### 8.3 Knowledge Points

- [ ] `QUERY_KNOWLEDGE_POINTS` -> `course-service` -> `GET /api/teacher/knowledge-points`
- [ ] `QUERY_COURSE_KNOWLEDGE_POINTS` -> `course-service` -> `GET /api/teacher/knowledge-points/course/{courseId}`
- [ ] `QUERY_KNOWLEDGE_POINT_DETAIL` -> `course-service` -> `GET /api/teacher/knowledge-points/{knowledgePointId}`
- [ ] `CREATE_KNOWLEDGE_POINT` -> `course-service` -> `POST /api/teacher/knowledge-points`
- [ ] `UPDATE_KNOWLEDGE_POINT` -> `course-service` -> `PUT /api/teacher/knowledge-points/{knowledgePointId}`
- [ ] `DELETE_KNOWLEDGE_POINT` -> `course-service` -> `DELETE /api/teacher/knowledge-points/{knowledgePointId}`
- [ ] `BIND_ASSIGNMENT_KNOWLEDGE_POINTS` -> `assignment-service` -> `POST /api/teacher/knowledge-points/assignment/{assignmentId}`
- [ ] `BIND_EXAM_KNOWLEDGE_POINTS` -> `exam-service` -> `POST /api/teacher/knowledge-points/exam/{examId}`

### 8.4 Assignments

- [x] `QUERY_ASSIGNMENTS` -> `assignment-service` -> `GET /api/teacher/assignments` or `GET /api/student/assignments`
- [x] `QUERY_ASSIGNMENT_DETAIL` -> `assignment-service` -> `GET /api/teacher/assignments/{assignmentId}` or `GET /api/student/assignments/{assignmentId}`
- [ ] `PUBLISH_ASSIGNMENT` -> `assignment-service` -> `POST /api/teacher/assignments`
- [x] `UPDATE_ASSIGNMENT` -> `assignment-service` -> `PUT /api/teacher/assignments/{assignmentId}`
- [x] `DELETE_ASSIGNMENT` -> `assignment-service` -> `DELETE /api/teacher/assignments/{assignmentId}`
- [x] `QUERY_ASSIGNMENT_SUBMISSIONS` -> `assignment-service` -> `GET /api/teacher/assignments/{assignmentId}/submissions`
- [x] `SUBMIT_ASSIGNMENT` -> `assignment-service` -> `POST /internal/assignments/{assignmentId}/submissions`
- [x] `GRADE_ASSIGNMENT` -> `assignment-service` -> `PUT /api/teacher/submissions/{submissionId}/grade`

### 8.5 Exams

- [x] `QUERY_EXAMS` -> `exam-service` -> `GET /api/teacher/exams` or `GET /api/student/exams`
- [x] `QUERY_EXAM_DETAIL` -> `exam-service` -> `GET /internal/exams/{examId}/teacher`
- [x] `PUBLISH_EXAM` -> `exam-service` -> `POST /internal/exams/teacher`
- [x] `UPDATE_EXAM` -> `exam-service` -> `PUT /internal/exams/{examId}/teacher`
- [x] `DELETE_EXAM` -> `exam-service` -> `DELETE /internal/exams/{examId}/teacher`
- [x] `SUBMIT_EXAM` -> `exam-service` -> `POST /api/student/exams/{examId}/submit`
- [x] `QUERY_EXAM_SUBMISSIONS` -> `exam-service` -> `GET /api/teacher/exams/{examId}/submissions`
- [x] `GRADE_EXAM` -> `exam-service` -> `PUT /api/teacher/exams/grade/{submissionId}`
- [x] `QUERY_SCORES` -> `exam-service` -> `GET /api/student/scores`

### 8.6 Analysis And Early Warnings

- [x] `QUERY_TEACHER_DASHBOARD` -> `analysis-service` -> `GET /api/teacher/dashboard`
- [x] `QUERY_LEARNING_SUMMARY` -> `analysis-service` -> `GET /api/teacher/learning-summary`
- [x] `QUERY_SCORE_TREND` -> `analysis-service` -> `GET /api/teacher/score-trend`
- [x] `QUERY_STUDENT_STATS` -> `analysis-service` -> `GET /api/student/stats`
- [x] `QUERY_STUDY_TIME_DISTRIBUTION` -> `analysis-service` -> `GET /api/student/study-time-distribution`
- [ ] `QUERY_STUDENT_KNOWLEDGE_POINTS` -> `analysis-service` -> `GET /api/student/knowledge-points`
- [x] `QUERY_KNOWLEDGE_MASTERY` -> `analysis-service` -> `GET /api/teacher/knowledge-points/mastery/student/{studentId}/course/{courseId}`
- [ ] `TRIGGER_KNOWLEDGE_ANALYSIS` -> `analysis-service` -> `POST /api/teacher/analysis/knowledge-points/trigger`
- [ ] `TRIGGER_EARLY_WARNING_ANALYSIS` -> `analysis-service` -> `POST /api/teacher/analysis/warnings/trigger`
- [ ] `QUERY_EARLY_WARNINGS` -> `analysis-service` -> `GET /api/early-warnings/teacher/list` or `GET /api/student/early-warnings`
- [ ] `UPDATE_EARLY_WARNING_STATUS` -> `analysis-service` -> `PUT /api/early-warnings/teacher/status/{warningId}`
- [ ] `RESOLVE_EARLY_WARNING` -> `analysis-service` -> `PUT /api/teacher/early-warnings/{warningId}/resolve`
- [ ] `DELETE_EARLY_WARNING` -> `analysis-service` -> `DELETE /api/early-warnings/teacher/{warningId}`
- [ ] `EXPORT_EARLY_WARNINGS` -> `analysis-service` -> `GET /api/early-warnings/teacher/export`

### 8.7 Notifications

- [x] `QUERY_NOTIFICATIONS` -> `notification-service` -> `GET /api/notifications/student` or `GET /api/notifications/student/all`
- [x] `QUERY_UNREAD_NOTIFICATION_COUNT` -> `notification-service` -> `GET /api/notifications/student/unread-count`
- [x] `MARK_NOTIFICATION_READ` -> `notification-service` -> `PUT /api/notifications/{notificationId}/read`
- [x] `MARK_ALL_NOTIFICATIONS_READ` -> `notification-service` -> `PUT /api/notifications/read-all`
- [x] `DELETE_NOTIFICATION` -> `notification-service` -> `DELETE /api/notifications/{notificationId}`
- [x] `DELETE_ALL_READ_NOTIFICATIONS` -> `notification-service` -> `DELETE /api/notifications/delete-all-read`
- [x] `SEND_NOTIFICATION` -> `notification-service` -> `POST /api/notifications/teacher/send`
- [x] `SEND_BATCH_NOTIFICATION` -> `notification-service` -> `POST /api/notifications/teacher/send-batch`

### 8.8 AI Generation

- [x] `GENERATE_QUESTIONS` -> `ai-service` -> `POST /api/ai/generate-questions`
- [x] `GENERATE_EXAM` -> `ai-service` -> `POST /api/ai/generate-exam`
- [x] `GENERATE_LEARNING_SUGGESTIONS` -> `ai-service` -> `POST /api/ai/learning-suggestions`

## 9. Confirmation Rules

- [x] Read-only query actions can return directly.
- [x] All write actions must return an `AgentActionPreviewDTO`.
- [x] Assignment publishing requires confirmation.
- [x] Assignment updating requires confirmation.
- [x] Assignment submission requires confirmation.
- [x] Deleting an assignment requires confirmation.
- [x] Assignment grading requires confirmation.
- [x] Exam publishing requires confirmation.
- [x] Exam updating requires confirmation.
- [x] Exam deletion requires confirmation.
- [x] Exam grading requires confirmation.
- [ ] Course, class, knowledge-point, exam, warning, profile, and settings write actions require confirmation.
- [x] Notification mark-all-read action requires confirmation.
- [ ] Overwriting an existing submission requires second-level confirmation.
- [x] Deleting an assignment is classified as a critical operation.
- [ ] Deleting course, class, knowledge point, exam, warning, or notification resources requires second-level confirmation.
- [x] Batch notification sending requires second-level confirmation.
- [ ] Password change requires explicit identity verification flow.
- [x] Batch operations require second-level confirmation and operation count display.
- [x] Confirmation must use an idempotency key.
- [x] Confirmation must expire after a configured timeout.

## 10. Risk Levels

- [ ] `LOW`: read-only query.
- [ ] `MEDIUM`: create normal business resource, submit assignment/exam, mark notifications read, send notification, update profile.
- [x] Mark-all-notifications-read is classified as `MEDIUM`.
- [x] Send-notification is classified as `MEDIUM`.
- [x] `HIGH`: update existing course/class/assignment/exam, overwrite submission, grade work, trigger analysis.
- [ ] `CRITICAL`: delete resources, batch operations, irreversible operations, password/security settings changes.

## 11. Security Checklist

- [ ] Agent inherits current user identity from gateway trace/auth headers.
- [ ] Agent does not use super-admin credentials for normal business operations.
- [x] Teacher actions require teacher role.
- [x] Student actions require student role.
- [ ] Teacher can only operate owned courses/classes.
- [x] Teacher assignment publishing validates the target course belongs to the current teacher.
- [x] Agent exam publishing validates the target course belongs to the current teacher.
- [x] Agent assignment submission validates that the target assignment is visible to the current student.
- [x] Agent exam submission validates that the target exam is visible to the current student.
- [ ] Teacher can only grade submissions under owned courses/classes/exams/assignments.
- [ ] Notification sending is restricted by teacher ownership and recipient scope.
- [ ] Analysis trigger actions are restricted to teacher-owned classes/courses.
- [ ] Model output is never trusted as authorization proof.
- [ ] Backend services perform final permission checks.
- [x] Prompt input and model output are logged with sensitive data masking.
- [x] Agent message, action, and audit payloads are persisted with sensitive data masking.
- [ ] No Agent tool directly writes another service database.
- [ ] All write operations produce audit logs.

## 12. Data Model Checklist

### 12.1 `agent_sessions`

- [x] `id`
- [x] `user_id`
- [x] `user_role`
- [x] `status`
- [x] `pending_intent`
- [x] `pending_slots_json`
- [x] `created_at`
- [x] `updated_at`

### 12.2 `agent_messages`

- [x] `id`
- [x] `session_id`
- [x] `role`
- [x] `content`
- [x] `metadata_json`
- [x] `created_at`

### 12.3 `agent_actions`

- [ ] `id`
- [ ] `session_id`
- [ ] `intent`
- [ ] `status`
- [ ] `risk_level`
- [ ] `preview_json`
- [ ] `request_json`
- [ ] `result_json`
- [ ] `idempotency_key`
- [ ] `expires_at`
- [ ] `confirmed_at`
- [ ] `executed_at`
- [ ] `created_at`

### 12.4 `agent_audit_logs`

- [ ] `id`
- [ ] `action_id`
- [ ] `user_id`
- [ ] `user_role`
- [ ] `operation`
- [ ] `target_service`
- [ ] `target_resource`
- [ ] `trace_id`
- [ ] `success`
- [ ] `error_message`
- [ ] `created_at`

## 13. API Checklist

### 13.1 Chat API

- [x] `POST /api/agent/chat`
- [ ] Accepts natural-language input.
- [ ] Accepts optional `sessionId`.
- [ ] Returns a normal text response for read-only or incomplete actions.
- [x] Returns an action preview for executable write actions.
- [x] Returns missing slot prompts when required data is absent.

### 13.2 Confirm API

- [x] `POST /api/agent/actions/{actionId}/confirm`
- [x] Validates action ownership.
- [x] Validates action status is `PENDING_CONFIRMATION` or `PENDING_SECOND_CONFIRMATION`.
- [x] Validates idempotency key.
- [x] Re-runs permission and risk checks before execution.
- [x] Returns `PENDING_SECOND_CONFIRMATION` for critical actions before target service execution.
- [x] Calls target service only after validation passes.
- [x] Stores execution result.

### 13.3 History API

- [x] `GET /api/agent/sessions`
- [x] `GET /api/agent/sessions/{sessionId}`
- [x] `GET /api/agent/actions/{actionId}`
- [x] Users can only view their own Agent sessions.
- [x] Users can only confirm actions created in their own Agent sessions.

## 14. Model Output Contract

- [x] Add LangChain4j as the optional Agent understanding/planning layer after core tools are sufficiently covered.
- [x] Configure LangChain4j with OpenAI-compatible endpoint `https://token-plan-cn.xiaomimimo.com/v1`.
- [x] Read the local model API key only from `XIAOMI_API_KEY`.
- [x] Use `mimo-v2.5` as the default local compatible model.
- [x] Enable the LangChain4j understanding layer by default when `XIAOMI_API_KEY` is available.
- [x] Use LangChain4j for natural-language intent recognition and slot extraction as the first understanding-layer scope.
- [ ] Use LangChain4j for follow-up questions, tool planning, and RAG knowledge retrieval.
- [x] Keep LangChain4j output as structured `intent + confidence + slots + missingSlots`, not as direct business execution.
- [x] Route every LangChain4j-planned action back through `AgentPermissionPolicy`, risk classification, confirmation, idempotency, and audit.
- [x] Filter LangChain4j output to an allowed top-level business slot set before enrichment, validation, persistence, or tool execution.
- [x] Model output must be JSON.
- [x] Model output must contain `intent`.
- [x] Model output must contain `confidence`.
- [x] Model output must contain `slots`.
- [x] Model output must contain `missingSlots`.
- [x] Invalid JSON must fall back to rule-based recognition.
- [x] Low confidence must fall back to rule-based recognition.
- [x] Backend validates every field before creating an action preview.
- [x] Run real-model smoke test with `agent.llm.enabled=true` and `XIAOMI_API_KEY` available in the service process.
- [x] Run real-model LangChain4j accuracy gate with representative Chinese Agent commands before frontend-to-backend smoke testing.

Example:

```json
{
  "intent": "PUBLISH_ASSIGNMENT",
  "confidence": 0.92,
  "slots": {
    "title": "Spring Cloud 注册中心实验",
    "courseName": "Java 企业开发",
    "dueDate": "2026-06-13 22:00",
    "maxScore": 100
  },
  "missingSlots": []
}
```

## 15. Frontend Checklist

- [x] Add shared `agent-chat-panel.js`.
- [x] Add shared `agent-action-preview-modal.js`.
- [x] Add shared `agent-history-panel.js`.
- [x] Integrate teacher Agent entry into `teacher-ai-tools.html` or a dedicated teacher Agent page.
- [x] Integrate student Agent entry into `student-ai-assistant.html` or a dedicated student Agent page.
- [x] Render missing slot prompts clearly.
- [x] Render action preview cards before write operations.
- [x] Render confirm/cancel buttons.
- [x] Cancel buttons call the Agent cancellation endpoint.
- [x] Render execution result status.
- [x] Render staged second-confirmation status for critical operations.
- [x] Render action history.
- [x] Disable confirm button after first click.

## 16. Observability Checklist

- [ ] Propagate `traceId` from gateway to `agent-service`.
- [ ] Propagate `traceId` from `agent-service` to target services.
- [x] Log intent recognition result.
- [ ] Log action preview creation.
- [ ] Log confirmation.
- [ ] Log target service execution result.
- [ ] Mask sensitive content in logs.
- [ ] Add metrics for action counts by intent.
- [ ] Add metrics for success and failure counts.
- [ ] Add metrics for model latency.
- [ ] Add metrics for confirmation cancellation count.

Suggested metrics:

- [ ] `agent.intent.count`
- [ ] `agent.action.preview.count`
- [ ] `agent.action.confirm.count`
- [ ] `agent.action.success.count`
- [ ] `agent.action.failure.count`
- [ ] `agent.model.latency`
- [ ] `agent.confirmation.cancel.count`

## 17. Event Checklist

- [ ] Publish `AgentActionExecutedEvent` after successful execution.
- [ ] Publish `AgentActionFailedEvent` after failed execution if needed.
- [ ] Publish domain-specific business events only through the owning service.
- [ ] Use outbox for reliable event publication.
- [ ] Make event consumers idempotent.

## 18. Testing Checklist

### 18.1 Unit Tests

- [x] Intent recognition fallback behavior.
- [x] Slot extraction validation.
- [x] Typed slot validation rejects malformed model output before action preview creation.
- [x] Real-model LangChain4j intent and slot accuracy gate.
- [x] Missing slot detection.
- [x] Permission policy decisions.
- [x] Assignment publishing rejects courses not owned by the current teacher.
- [x] Assignment updating validates required slots and delegates to `assignment-service`.
- [x] Assignment deletion validates required slots and delegates to `assignment-service`.
- [x] Assignment grading validates required slots and delegates to `assignment-service`.
- [x] Assignment detail lookup validates required slots and delegates to `assignment-service`.
- [x] Assignment submission lookup validates required slots and delegates to `assignment-service`.
- [x] Assignment submission rejects assignments not visible to the current student.
- [x] Pending assignment query uses `assignment-service` student-visible assignment API with `submitted=false`.
- [x] Agent answers pending assignment queries as read-only `DATA` without creating a pending action.
- [x] Course detail lookup validates required slots and delegates to `course-service`.
- [x] Class lookup delegates to `course-service`.
- [x] Class detail lookup validates required slots and delegates to `course-service`.
- [x] Tool registry resolves course, assignment, exam, notification, learning stats, and AI generation tools by intent.
- [x] Exam submission rejects exams not visible to the current student.
- [x] Exam publishing rejects courses not owned by the current teacher.
- [x] Exam updating validates required slots and delegates to `exam-service`.
- [x] Exam deletion validates required slots and delegates to `exam-service`.
- [x] Exam detail lookup validates required slots and delegates to `exam-service`.
- [x] Exam submission lookup validates required slots and delegates to `exam-service`.
- [x] Exam grading validates required slots and delegates to `exam-service`.
- [x] Score query delegates to `exam-service`.
- [x] Unread notification count delegates to `notification-service`.
- [x] Study time distribution query delegates to `analysis-service`.
- [x] Knowledge mastery query validates required slots and delegates to `analysis-service`.
- [x] Mark-all-notifications-read validates confirmation and delegates to `notification-service`.
- [x] Delete-notification validates confirmation and delegates to `notification-service`.
- [x] Delete-all-read-notifications validates confirmation and delegates to `notification-service`.
- [x] Risk policy decisions.
- [x] Agent action mapper marks `UPDATE_EXAM` as `HIGH` risk and confirmation-required.
- [x] Agent action mapper marks `DELETE_EXAM` as `CRITICAL` risk and confirmation-required.
- [x] Agent action mapper marks `UPDATE_ASSIGNMENT` as `HIGH` risk and confirmation-required.
- [x] Agent action mapper marks `DELETE_ASSIGNMENT` as `CRITICAL` risk and confirmation-required.
- [x] Agent action mapper marks `GRADE_ASSIGNMENT` as `HIGH` risk and confirmation-required.
- [x] Agent action mapper marks `GRADE_EXAM` as `HIGH` risk and confirmation-required.
- [x] Idempotency key generation and reuse.
- [x] Action preview creation.
- [x] Confirm execution state transitions.

### 18.2 Controller Tests

- [x] `POST /api/agent/chat` returns missing slot prompt.
- [x] `POST /api/agent/chat` returns action preview.
- [x] `POST /api/agent/actions/{actionId}/confirm` executes valid action.
- [x] Confirm rejects actions owned by another user.
- [x] Confirm rejects expired actions.
- [x] Confirm rejects mismatched idempotency keys.

### 18.3 Integration Tests

- [x] Agent publishes, updates, deletes, grades, and queries assignment details/submissions through `assignment-service`.
- [x] Agent submits assignment through `assignment-service`.
- [x] Agent queries course details through `course-service`.
- [x] Agent queries classes through `course-service`.
- [x] Agent queries class details through `course-service`.
- [ ] Agent creates courses through `course-service`.
- [ ] Agent manages classes and class students through `course-service`.
- [x] Agent publishes, updates, deletes, grades, submits, and queries exam details/submissions/scores through `exam-service`.
- [x] Agent grades exams through `exam-service`.
- [ ] Agent queries learning stats and triggers analysis through `analysis-service`.
- [x] Agent queries knowledge mastery through `analysis-service`.
- [ ] Agent reads and sends notifications through `notification-service`.
- [ ] Agent updates user profile/settings through `user-service` or `auth-service`.
- [ ] Agent generates questions, exams, and learning suggestions through `ai-service`.
- [x] Agent resolves course context through `course-service`.
- [x] Agent resolves student assignment context through `assignment-service`.
- [x] Agent records session, message, action, and audit data.

### 18.4 Browser Smoke Tests

- [x] Teacher sends natural-language assignment publishing command.
- [x] Teacher sees action preview.
- [x] Teacher confirms action.
- [x] New assignment appears in teacher assignment list.
- [x] Student sends natural-language assignment submission command.
- [x] Student sees action preview.
- [x] Student confirms action.
- [x] Submission appears in assignment detail.
- [ ] Teacher sends natural-language course or class management command.
- [ ] Teacher confirms course/class write action.
- [x] Teacher sends natural-language exam publishing command.
- [x] New Agent-published exam appears in teacher exam list.
- [x] Student sends natural-language exam submission command.
- [x] Submission appears in exam submission records.
- [ ] Student asks Agent to mark notifications read.
- [ ] Teacher asks Agent to send a notification to a class.
- [ ] Student asks Agent to summarize learning stats.
- [x] Browser smoke verifies teacher and student Agent panels render with authenticated sessions.
- [x] Browser smoke verifies teacher and student Agent history panels render with authenticated sessions.
- [x] Browser smoke verifies teacher read-only Agent command returns data.
- [x] Browser smoke verifies student pending assignment Agent command returns data.

## 19. Rollout Plan

### Current Priority Adjustment

- [x] Treat the basic Agent execution layer as runnable baseline: chat entry, intent routing, permission checks, risk classification, read execution, write preview, confirmation, idempotency, and audit are already in place.
- [x] Prioritize LangChain4j as the next major milestone before continuing broad tool expansion.
- [x] Keep the current rule-based recognizer as fallback while LangChain4j is introduced.
- [x] Use LangChain4j to improve natural-language understanding and slot extraction.
- [x] Use LangChain4j/rule output plus session context for missing-slot follow-up.
- [ ] Use LangChain4j for deeper multi-turn planning.
- [x] Keep all LangChain4j output routed through the existing Agent execution layer.
- [ ] Continue adding remaining business tools after the LangChain4j understanding layer is in place.
- [x] Run end-to-end smoke tests through frontend, gateway, `agent-service`, and target services before calling the enterprise Agent flow complete.

### Phase 1: Enterprise Minimum Closed Loop

- [x] Add `agent-service-api`.
- [x] Add `agent-service`.
- [x] Add gateway route `/api/agent/**`.
- [x] Add Agent session/action/audit schema.
- [x] Implement `PUBLISH_ASSIGNMENT`.
- [x] Implement `SUBMIT_ASSIGNMENT`.
- [x] Implement read-only tool queries for courses, assignments, exams, notifications, and learning stats.
- [x] Implement AI generation tool forwarding for questions, exams, and learning suggestions.
- [x] Implement confirmation flow.
- [x] Implement audit logging.
- [x] Add backend tests.
- [x] Add basic teacher/student frontend entry.

### Phase 2: Context Enrichment

- [x] Introduce LangChain4j behind `IntentRecognitionService` / `SlotExtractionService` as the first Phase 2 priority.
- [x] Add structured model output parsing for `intent`, `confidence`, `slots`, and `missingSlots`.
- [x] Add confidence thresholds and fallback to rule-based recognition when model output is invalid or low-confidence.
- [x] Add missing-slot follow-up questions before creating action previews.
- [x] Add multi-turn context so the Agent can complete commands across several user messages.
- [x] Add course fuzzy matching.
- [x] Add pending assignment matching.
- [ ] Add LangChain4j RAG retrieval for platform help, course material, rules, and learning guidance without bypassing business tools.
- [ ] Add action history page.
- [ ] Add failure retry where safe.
- [ ] Add notification integration after assignment publishing.
- [ ] Add course/class management tools.
- [ ] Add remaining exam grading and deletion tools.
- [ ] Add knowledge-point management tools.
- [ ] Add profile/settings tools.

### Phase 3: Enterprise Governance

- [ ] Add risk policy configuration.
- [ ] Add approval workflow for high-risk actions.
- [ ] Add batch operation limits.
- [ ] Add detailed observability metrics.
- [ ] Add Agent event publication.
- [ ] Add admin audit search.
- [ ] Add approval workflow for critical deletes and batch notifications.
- [ ] Add operation allowlist configuration per role and tenant.

## 20. Non-Goals

- [ ] Do not let the model directly execute SQL.
- [ ] Do not let LangChain4j directly execute write operations or call target service APIs outside the Agent tool layer.
- [ ] Do not let the Agent bypass existing service permission checks.
- [ ] Do not store business data in Agent tables as the source of truth.
- [ ] Do not execute write operations without confirmation.
- [ ] Do not implement all existing platform operations in the first release.
- [ ] Do not expose unstable or internal maintenance endpoints to the Agent unless wrapped by a safe tool.
- [ ] Do not merge Agent orchestration into `ai-service` for the enterprise version.

## 21. Recommended First Scope

- [x] New independent `agent-service` and `agent-service-api`.
- [x] Natural-language command input.
- [x] Assignment publishing for teachers.
- [x] Assignment submission for students.
- [x] Read-only query tools for current courses, assignments, exams, notifications, and learning stats.
- [x] AI generation tools for questions, exams, and learning suggestions.
- [x] Operation preview.
- [x] Explicit confirmation.
- [x] Audit log.
- [x] Idempotency.
- [x] Permission checks.
- [x] Basic frontend panel for teacher and student.
- [x] LangChain4j understanding-layer skeleton for enterprise-grade natural-language interaction.
- [x] Real-model smoke test for LangChain4j understanding layer.
- [x] End-to-end smoke test from frontend Agent chat to target business services.
