ALTER TABLE workflows
    ADD COLUMN scenario VARCHAR(256);

ALTER TABLE workflows
    ADD COLUMN steps TEXT;

ALTER TABLE workflows
    ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE workflows
    ADD COLUMN creator_id BIGINT;

UPDATE workflows
SET scenario = COALESCE(scenario, '通用场景'),
    steps = COALESCE(steps, description),
    creator_id = COALESCE(creator_id, created_by);

ALTER TABLE workflows
    ALTER COLUMN scenario SET NOT NULL;

ALTER TABLE workflows
    ALTER COLUMN steps SET NOT NULL;

ALTER TABLE workflows
    ADD CONSTRAINT fk_workflows_creator FOREIGN KEY (creator_id) REFERENCES users (id);

CREATE INDEX idx_workflows_featured ON workflows (featured);
CREATE INDEX idx_workflows_scenario ON workflows (scenario);
