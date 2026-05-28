CREATE TABLE workflow_stars (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_workflow_stars_workflow_user UNIQUE (workflow_id, user_id),
    CONSTRAINT fk_workflow_stars_workflow FOREIGN KEY (workflow_id) REFERENCES workflows (id),
    CONSTRAINT fk_workflow_stars_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_workflow_stars_workflow_id ON workflow_stars (workflow_id);

CREATE TABLE workflow_favorites (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_workflow_favorites_workflow_user UNIQUE (workflow_id, user_id),
    CONSTRAINT fk_workflow_favorites_workflow FOREIGN KEY (workflow_id) REFERENCES workflows (id),
    CONSTRAINT fk_workflow_favorites_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_workflow_favorites_workflow_id ON workflow_favorites (workflow_id);

CREATE TABLE workflow_submissions (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    submitter_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    remark VARCHAR(1024),
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_workflow_submissions_workflow FOREIGN KEY (workflow_id) REFERENCES workflows (id),
    CONSTRAINT fk_workflow_submissions_submitter FOREIGN KEY (submitter_id) REFERENCES users (id)
);
CREATE INDEX idx_workflow_submissions_workflow_id ON workflow_submissions (workflow_id);
CREATE INDEX idx_workflow_submissions_submitter_id ON workflow_submissions (submitter_id);
CREATE INDEX idx_workflow_submissions_status ON workflow_submissions (status);

ALTER TABLE workflows ADD COLUMN IF NOT EXISTS star_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE workflows ADD COLUMN IF NOT EXISTS favorite_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE workflows ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW';
ALTER TABLE workflows ADD COLUMN IF NOT EXISTS submission_id BIGINT;

INSERT INTO system_configs (config_key, config_value, description, created_by, updated_by)
SELECT 'WORKFLOW_REVIEW_ENABLED', 'true', '控制普通用户提交工作流时是否需要管理员审核', 0, 0
WHERE NOT EXISTS (
    SELECT 1
    FROM system_configs
    WHERE config_key = 'WORKFLOW_REVIEW_ENABLED'
      AND deleted = FALSE
);
