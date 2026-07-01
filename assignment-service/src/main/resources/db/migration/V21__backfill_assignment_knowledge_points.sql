CREATE TABLE IF NOT EXISTS assignment_knowledge_points (
    assignment_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assignment_id, knowledge_point_id),
    KEY idx_assignment_knowledge_points_point (knowledge_point_id)
);
