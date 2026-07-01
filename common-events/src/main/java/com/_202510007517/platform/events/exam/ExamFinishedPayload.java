package com._202510007517.platform.events.exam;

import java.time.Instant;
import java.util.Objects;

public record ExamFinishedPayload(
        Long examId,
        Long submissionId,
        Long studentId,
        Long courseId,
        Long classId,
        Integer score,
        Integer maxScore,
        Instant finishedAt) {

    public ExamFinishedPayload {
        Objects.requireNonNull(examId, "examId must not be null");
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(courseId, "courseId must not be null");
        Objects.requireNonNull(classId, "classId must not be null");
        Objects.requireNonNull(score, "score must not be null");
        Objects.requireNonNull(maxScore, "maxScore must not be null");
        Objects.requireNonNull(finishedAt, "finishedAt must not be null");
    }
}
