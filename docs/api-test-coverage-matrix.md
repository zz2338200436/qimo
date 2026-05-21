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

## Isolated Side-Effect Coverage

These endpoints are intentionally kept out of the shared-account smoke run, but are automated with an isolated fixture user and isolated exam/notification data:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\seed-isolated-side-effect-smoke.ps1
node .\scripts\verify-gateway-isolated-side-effects.js
```

| Endpoint | Reason |
| --- | --- |
| `POST /api/auth/change-password` | Verifies password change, old access-token revocation, and password restoration for `isolated_api_smoke` |
| `POST /api/student/change-password` success path | Verifies the legacy compatibility success envelope and restores the isolated password |
| `POST /api/auth/logout` success path | Verifies access-token revocation and refresh-token deletion |
| `POST /api/auth/refresh` success path | Verifies refresh success and one-time-use rejection of the old refresh token |
| `POST /api/auth/switch-role` success path | Uses the isolated multi-role user to switch `TEACHER -> STUDENT -> TEACHER` and verify old token revocation |
| `DELETE /api/notifications/delete-all-read` | Creates isolated notifications, marks them read, bulk-deletes only isolated read data, and verifies removal |
| `PUT /api/teacher/exams/submissions/{submissionId}` | Updates the isolated exam submission directly and verifies persisted score/comment |
| `DELETE /api/teacher/exams/submissions/{submissionId}` | Deletes the isolated exam submission and verifies it is no longer readable |

## Remaining Manual/Out-Of-Scope Items

No public `/api/**` controller mapping is intentionally left untested solely because it is high-side-effect. `/internal/**` service-to-service endpoints remain outside gateway smoke scope.

## Verification Policy

- Runtime smoke scripts may create data only when they can clean it up in `finally`.
- Shared account credentials (`student42`, `teacher7`) must not be permanently changed by automated smoke tests.
- Compatibility endpoints that deliberately return failure envelopes are counted as covered when the expected status/body contract is asserted.
- Endpoint claims should be backed by `verify-gateway-api-smoke.js`, `verify-gateway-isolated-side-effects.js`, targeted Maven controller tests, or an explicit entry in the manual/out-of-scope section above.
