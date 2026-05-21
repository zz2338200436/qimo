ALTER TABLE kp_mastery
    ADD COLUMN knowledge_point_id BIGINT NOT NULL DEFAULT 0 AFTER class_id;

ALTER TABLE kp_mastery
    DROP INDEX uk_kp_mastery_student_course;

ALTER TABLE kp_mastery
    ADD CONSTRAINT uk_kp_mastery_student_course_point
        UNIQUE (student_id, course_id, knowledge_point_id);

CREATE INDEX idx_kp_mastery_point_score
    ON kp_mastery (knowledge_point_id, mastery_score);
