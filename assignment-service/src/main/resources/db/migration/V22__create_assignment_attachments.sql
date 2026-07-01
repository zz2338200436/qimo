CREATE TABLE IF NOT EXISTS assessment_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assessment_type VARCHAR(32) NOT NULL,
    assessment_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    relative_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(255) NULL,
    file_size BIGINT NULL,
    uploaded_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_assessment_attachments_owner (assessment_type, assessment_id)
);
