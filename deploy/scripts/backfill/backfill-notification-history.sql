-- Backfill sc_notification notifications from the legacy monolith database.
-- Safe to rerun: legacy notifications are deduplicated by stable business columns.

INSERT INTO sc_notification.notifications (
    student_id,
    teacher_id,
    type,
    title,
    content,
    related_id,
    is_read,
    created_at
)
SELECT
    n.student_id,
    n.teacher_id,
    n.type,
    n.title,
    n.content,
    n.related_id,
    COALESCE(n.is_read, 0) AS is_read,
    COALESCE(n.created_at, CURRENT_TIMESTAMP(6)) AS created_at
FROM major_assignment.notifications n
WHERE n.student_id IS NOT NULL
  AND n.type IS NOT NULL
  AND n.title IS NOT NULL
  AND n.content IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sc_notification.notifications existing
      WHERE existing.student_id = n.student_id
        AND (
            existing.teacher_id = n.teacher_id
            OR (existing.teacher_id IS NULL AND n.teacher_id IS NULL)
        )
        AND existing.type = n.type
        AND existing.title = n.title
        AND (
            existing.related_id = n.related_id
            OR (existing.related_id IS NULL AND n.related_id IS NULL)
        )
        AND existing.created_at = COALESCE(n.created_at, existing.created_at)
  );
