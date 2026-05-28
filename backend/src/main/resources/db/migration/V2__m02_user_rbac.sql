ALTER TABLE users
    ADD COLUMN user_code VARCHAR(32);

ALTER TABLE users
    ADD COLUMN real_name VARCHAR(64);

ALTER TABLE users
    ALTER COLUMN real_name SET DEFAULT '';

UPDATE users
SET real_name = COALESCE(NULLIF(nickname, ''), username)
WHERE real_name IS NULL OR real_name = '';

ALTER TABLE users
    ALTER COLUMN real_name SET NOT NULL;

CREATE UNIQUE INDEX uq_users_user_code ON users (user_code);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_real_name ON users (real_name);

ALTER TABLE roles
    ADD COLUMN code VARCHAR(64);

UPDATE roles
SET code = UPPER(name)
WHERE code IS NULL;

ALTER TABLE roles
    ALTER COLUMN code SET NOT NULL;

CREATE UNIQUE INDEX uq_roles_code ON roles (code);
