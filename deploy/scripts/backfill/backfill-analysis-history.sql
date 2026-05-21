-- Backfill sc_analysis projections from historical Exam and Assignment data.
-- Safe to rerun: target tables use unique keys and deterministic event ids.

INSERT INTO sc_analysis.score_trends (
    student_id,
    course_id,
    class_id,
    source_type,
    source_id,
    submission_id,
    score,
    max_score,
    score_rate,
    occurred_at
)
SELECT
    s.student_id,
    e.course_id,
    MIN(ec.class_id) AS class_id,
    'exam' AS source_type,
    s.exam_id AS source_id,
    s.id AS submission_id,
    s.score,
    e.total_score AS max_score,
    CASE
        WHEN e.total_score IS NULL OR e.total_score <= 0 OR s.score IS NULL THEN CAST(0 AS DECIMAL(6,4))
        ELSE CAST(ROUND(s.score * 1.0 / e.total_score, 4) AS DECIMAL(6,4))
    END AS score_rate,
    s.submission_date AS occurred_at
FROM sc_exam.exam_submissions s
JOIN sc_exam.exams e ON e.id = s.exam_id
LEFT JOIN sc_exam.exam_classes ec ON ec.exam_id = s.exam_id
WHERE s.graded = 1
  AND s.score IS NOT NULL
GROUP BY
    s.id,
    s.student_id,
    e.course_id,
    s.exam_id,
    s.score,
    e.total_score,
    s.submission_date
ON DUPLICATE KEY UPDATE
    student_id = VALUES(student_id),
    course_id = VALUES(course_id),
    class_id = VALUES(class_id),
    source_id = VALUES(source_id),
    score = VALUES(score),
    max_score = VALUES(max_score),
    score_rate = VALUES(score_rate),
    occurred_at = VALUES(occurred_at);

DROP TABLE IF EXISTS sc_analysis._analysis_backfill_evidence;

CREATE TABLE sc_analysis._analysis_backfill_evidence (
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    knowledge_point_id BIGINT NOT NULL DEFAULT 0,
    mastery_score DECIMAL(6,4) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id BIGINT NOT NULL,
    event_id VARCHAR(150) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    evidence_order INT NOT NULL
);

INSERT INTO sc_analysis._analysis_backfill_evidence (
    student_id,
    course_id,
    class_id,
    knowledge_point_id,
    mastery_score,
    source_type,
    source_id,
    event_id,
    occurred_at,
    evidence_order
)
SELECT
    s.student_id,
    e.course_id,
    MIN(ec.class_id) AS class_id,
    0 AS knowledge_point_id,
    CASE
        WHEN e.total_score IS NULL OR e.total_score <= 0 OR s.score IS NULL THEN CAST(0 AS DECIMAL(6,4))
        ELSE CAST(ROUND(s.score * 1.0 / e.total_score, 4) AS DECIMAL(6,4))
    END AS mastery_score,
    'exam' AS source_type,
    s.exam_id AS source_id,
    CONCAT('backfill-exam-', s.id) AS event_id,
    s.submission_date AS occurred_at,
    2 AS evidence_order
FROM sc_exam.exam_submissions s
JOIN sc_exam.exams e ON e.id = s.exam_id
LEFT JOIN sc_exam.exam_classes ec ON ec.exam_id = s.exam_id
WHERE s.graded = 1
  AND s.score IS NOT NULL
GROUP BY
    s.id,
    s.student_id,
    e.course_id,
    s.exam_id,
    s.score,
    e.total_score,
    s.submission_date

UNION ALL

SELECT
    s.student_id,
    a.course_id,
    MIN(ac.class_id) AS class_id,
    0 AS knowledge_point_id,
    CAST(0.6000 AS DECIMAL(6,4)) AS mastery_score,
    'assignment' AS source_type,
    s.assignment_id AS source_id,
    CONCAT('backfill-assignment-', s.id) AS event_id,
    COALESCE(
        CAST(NULLIF(s.submission_date, '') AS DATETIME),
        s.updated_at,
        s.created_at,
        CURRENT_TIMESTAMP(6)
    ) AS occurred_at,
    1 AS evidence_order
FROM sc_assignment.assignment_submissions s
JOIN sc_assignment.assignments a ON a.id = s.assignment_id
LEFT JOIN sc_assignment.assignment_classes ac ON ac.assignment_id = s.assignment_id
GROUP BY
    s.id,
    s.student_id,
    a.course_id,
    s.assignment_id,
    s.submission_date,
    s.updated_at,
    s.created_at;

INSERT INTO sc_analysis.kp_mastery (
    student_id,
    course_id,
    class_id,
    knowledge_point_id,
    mastery_score,
    evidence_count,
    last_source_type,
    last_source_id,
    last_event_id,
    updated_at
)
SELECT
    aggregated.student_id,
    aggregated.course_id,
    latest.class_id,
    aggregated.knowledge_point_id,
    aggregated.mastery_score,
    aggregated.evidence_count,
    latest.source_type,
    latest.source_id,
    latest.event_id,
    aggregated.updated_at
FROM (
    SELECT
        student_id,
        course_id,
        knowledge_point_id,
        CAST(ROUND(AVG(mastery_score), 4) AS DECIMAL(6,4)) AS mastery_score,
        COUNT(*) AS evidence_count,
        MAX(occurred_at) AS updated_at
    FROM sc_analysis._analysis_backfill_evidence
    GROUP BY
        student_id,
        course_id,
        knowledge_point_id
) aggregated
JOIN sc_analysis._analysis_backfill_evidence latest
  ON latest.student_id = aggregated.student_id
 AND latest.course_id = aggregated.course_id
 AND latest.knowledge_point_id = aggregated.knowledge_point_id
WHERE NOT EXISTS (
    SELECT 1
    FROM sc_analysis._analysis_backfill_evidence newer
    WHERE newer.student_id = latest.student_id
      AND newer.course_id = latest.course_id
      AND newer.knowledge_point_id = latest.knowledge_point_id
      AND (
          newer.occurred_at > latest.occurred_at
          OR (
              newer.occurred_at = latest.occurred_at
              AND newer.evidence_order > latest.evidence_order
          )
          OR (
              newer.occurred_at = latest.occurred_at
              AND newer.evidence_order = latest.evidence_order
              AND newer.source_id > latest.source_id
          )
      )
)
ON DUPLICATE KEY UPDATE
    class_id = VALUES(class_id),
    mastery_score = VALUES(mastery_score),
    evidence_count = VALUES(evidence_count),
    last_source_type = VALUES(last_source_type),
    last_source_id = VALUES(last_source_id),
    last_event_id = VALUES(last_event_id),
    updated_at = VALUES(updated_at);

DROP TABLE IF EXISTS sc_analysis._analysis_backfill_evidence;
