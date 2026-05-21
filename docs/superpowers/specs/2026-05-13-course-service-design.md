# Course Service Design

> Version: v0.1
> Date: 2026-05-13

## Goal

Split the course ownership boundary into `Course_Service` while keeping existing front-end paths stable during migration.

## Scope

`Course_Service` owns course, class, major, class-student, and class-course assignment data. This pass implements the first working slice: teacher course list/detail/create/update/delete and course student lookup. Class and assignment tables are included in the schema and repository boundary so follow-up endpoints can be added without another ownership shift.

## Architecture

- `course-service-api` contains Feign interfaces and DTOs.
- `course-service` exposes `/api/teacher/courses/**`, registers as `course-service`, and uses JDBC/Flyway like `user-service`.
- `major_assignment` keeps existing routes during the transition and can bridge to `Course_Service` before Gateway direct routing is enabled.
- `gateway` will add a `course-route` to `lb://course-service` after service behavior is verified.

## Data Boundary

Owned tables are `courses`, `course_classes`, `class_courses`, `class_students`, and `majors` in `sc_course`. User data is read through `User_Service` contracts or stable user IDs. Cross-domain cleanup for assignments, exams, and analysis is not expanded in this slice.

## Testing

The first tests cover `CourseApplicationService` and `CourseController` response compatibility. Repository SQL is kept straightforward and can be covered by integration tests once the schema split is connected to a real migration database.
