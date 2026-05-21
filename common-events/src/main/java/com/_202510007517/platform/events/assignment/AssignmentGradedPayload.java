package com._202510007517.platform.events.assignment;

import java.time.Instant;

public record AssignmentGradedPayload(
        Long assignmentId,
        Long submissionId,
        Long studentId,
        Long courseId,
        Integer score,
        String teacherComment,
        Instant gradedAt) {
}
