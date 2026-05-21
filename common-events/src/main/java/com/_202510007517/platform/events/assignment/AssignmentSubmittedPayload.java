package com._202510007517.platform.events.assignment;

import java.time.Instant;
import java.util.Objects;

public record AssignmentSubmittedPayload(
        Long assignmentId,
        Long submissionId,
        Long studentId,
        Long courseId,
        Long classId,
        boolean late,
        Instant submittedAt) {

    public AssignmentSubmittedPayload {
        Objects.requireNonNull(assignmentId, "assignmentId must not be null");
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(courseId, "courseId must not be null");
        Objects.requireNonNull(classId, "classId must not be null");
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
    }
}
