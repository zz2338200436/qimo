# API Test Coverage Matrix

> Version: v0.1
> Last updated: 2026-05-21
> Scope: Spring Cloud services exposed through `gateway`

## Scope

The current Spring Cloud side exposes 135 unique public `/api/**` controller mappings across:

| Area | Modules |
| --- | --- |
| Auth and profile | `auth-service`, `user-service` |
| Course and class | `course-service` |
| Assignment | `assignment-service` |
| Exam | `exam-service` |
| Analysis and warning | `analysis-service` |
| Notification | `notification-service` |
| AI | `ai-service` |
| Gateway edge | `gateway` browser error endpoints |

`/internal/**` endpoints are intentionally excluded from gateway smoke coverage because they are service-to-service Feign/internal maintenance APIs.

## Automated Runtime Coverage

Primary runtime command:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\get-dev-auth-session.ps1 -Role teacher -OutFile .\.runtime-logs\teacher-session-full-smoke.json
powershell -ExecutionPolicy Bypass -File .\scripts\get-dev-auth-session.ps1 -Role student -OutFile .\.runtime-logs\student-session-full-smoke.json
node .\scripts\verify-gateway-api-smoke.js
```

| Group | Coverage |
| --- | --- |
| Auth | Captcha, login-derived sessions, `/api/auth/me`, invalid refresh token, forbidden role switch, weak student password compatibility failure, notification settings compatibility |
| Student reads | Courses, course detail, assignments, assignment detail, submissions, exams, exam detail, scores, stats, study time distribution, knowledge points, knowledge point detail, missing knowledge point 404 envelope, early warnings |
| Student writes | Assignment submit, exam submit, profile update, notification settings update, privacy settings update, avatar compatibility update, current user profile update |
| Teacher course/class | Course list/detail/create/update/delete, course students, class list/detail/create/update/delete, class students, class name check, majors, course assignment create/list/delete, class-course unassign compatibility |
| Teacher knowledge points | List, course list, detail, create/update/delete, assignment knowledge point list/save, exam knowledge point list/save |
| Teacher assignment | Assignment list/detail/create/update/delete, assignment submissions, submission detail, compatibility submission detail, grading |
| Teacher exam | Exam list/detail/create/update/delete, submissions list/detail, grading |
| User compatibility | `/api/users/me`, `/api/users/{id}`, `/api/users/students/{studentId}`, teacher student detail/update, student class compatibility |
| Analysis/warnings | Dashboard, learning summary, score trend, early warning stats/list/pending/detail/create/status/resolve/delete/export, course warning list, warning trigger, knowledge trigger, student trigger, teacher knowledge point analysis |
| Notification | Send, send batch, list, all list, unread count, mark one read, mark all read, delete created notifications |
| AI | Generate questions, generate exam, learning suggestions |
| Gateway edge | Browser error report, batch report, list, detail |

## Not Auto-Run In Shared Dev Data

These endpoints exist and should be tested only in an isolated database or with throwaway users because they mutate shared demo credentials or bulk user data.

| Endpoint | Reason |
| --- | --- |
| `POST /api/auth/change-password` | Changes the active account password and revokes the current token |
| `POST /api/student/change-password` success path | Changes `student42` password and revokes the token |
| `POST /api/auth/logout` success path | Revokes the access token used by the smoke run |
| `POST /api/auth/refresh` success path | Consumes the refresh token, which can make later checks flaky in the same run |
| `POST /api/auth/switch-role` success path | Requires a multi-role seed user; current smoke accounts are single-role |
| `DELETE /api/notifications/delete-all-read` | Bulk-deletes all read notifications for the demo student, not just smoke-created data |
| `DELETE /api/teacher/exams/submissions/{submissionId}` | Deletes a submission record that may be needed for grading/history checks |
| `PUT /api/teacher/exams/submissions/{submissionId}` | Direct submission edit path is lower priority than the supported grading path and should use isolated exam data |

## Verification Policy

- Runtime smoke scripts may create data only when they can clean it up in `finally`.
- Shared account credentials (`student42`, `teacher7`) must not be permanently changed by automated smoke tests.
- Compatibility endpoints that deliberately return failure envelopes are counted as covered when the expected status/body contract is asserted.
- Endpoint claims should be backed by either `verify-gateway-api-smoke.js`, targeted Maven controller tests, or an explicit entry in the isolated-test table above.
