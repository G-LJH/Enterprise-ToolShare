-- 用户与角色
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    nickname VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(256),
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

-- 工具与标签
CREATE TABLE tools (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    summary VARCHAR(512),
    description TEXT,
    owner_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_tools_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);
CREATE INDEX idx_tools_owner_id ON tools (owner_id);
CREATE INDEX idx_tools_status ON tools (status);

CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(256),
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE tool_tags (
    tool_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tool_id, tag_id),
    CONSTRAINT fk_tool_tags_tool FOREIGN KEY (tool_id) REFERENCES tools (id),
    CONSTRAINT fk_tool_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
);

-- 提交与交互
CREATE TABLE tool_submissions (
    id BIGSERIAL PRIMARY KEY,
    tool_id BIGINT NOT NULL,
    submitter_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    remark VARCHAR(1024),
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_tool_submissions_tool FOREIGN KEY (tool_id) REFERENCES tools (id),
    CONSTRAINT fk_tool_submissions_submitter FOREIGN KEY (submitter_id) REFERENCES users (id)
);
CREATE INDEX idx_tool_submissions_tool_id ON tool_submissions (tool_id);
CREATE INDEX idx_tool_submissions_submitter_id ON tool_submissions (submitter_id);

CREATE TABLE tool_stars (
    id BIGSERIAL PRIMARY KEY,
    tool_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tool_stars_tool_user UNIQUE (tool_id, user_id),
    CONSTRAINT fk_tool_stars_tool FOREIGN KEY (tool_id) REFERENCES tools (id),
    CONSTRAINT fk_tool_stars_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_tool_stars_tool_id ON tool_stars (tool_id);

CREATE TABLE tool_favorites (
    id BIGSERIAL PRIMARY KEY,
    tool_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tool_favorites_tool_user UNIQUE (tool_id, user_id),
    CONSTRAINT fk_tool_favorites_tool FOREIGN KEY (tool_id) REFERENCES tools (id),
    CONSTRAINT fk_tool_favorites_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_tool_favorites_tool_id ON tool_favorites (tool_id);

CREATE TABLE tool_comments (
    id BIGSERIAL PRIMARY KEY,
    tool_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_tool_comments_tool FOREIGN KEY (tool_id) REFERENCES tools (id),
    CONSTRAINT fk_tool_comments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_tool_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES tool_comments (id)
);
CREATE INDEX idx_tool_comments_tool_id ON tool_comments (tool_id);

-- 工作流与配置
CREATE TABLE workflows (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    description VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE workflow_tools (
    workflow_id BIGINT NOT NULL,
    tool_id BIGINT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (workflow_id, tool_id),
    CONSTRAINT fk_workflow_tools_workflow FOREIGN KEY (workflow_id) REFERENCES workflows (id),
    CONSTRAINT fk_workflow_tools_tool FOREIGN KEY (tool_id) REFERENCES tools (id)
);

CREATE TABLE system_configs (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(128) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(512),
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 日志与任务
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(128) NOT NULL,
    object_type VARCHAR(64) NOT NULL,
    object_id VARCHAR(64),
    operator_id BIGINT,
    operator_name VARCHAR(64),
    detail TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE operation_logs (
    id BIGSERIAL PRIMARY KEY,
    operation VARCHAR(128) NOT NULL,
    module VARCHAR(64),
    user_id BIGINT,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE import_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(256) NOT NULL,
    file_path VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    requester_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE export_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(256) NOT NULL,
    file_path VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    requester_id BIGINT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
