ALTER TABLE agent_sessions
    ADD COLUMN pending_intent VARCHAR(128) NULL;

ALTER TABLE agent_sessions
    ADD COLUMN pending_slots_json TEXT NULL;
