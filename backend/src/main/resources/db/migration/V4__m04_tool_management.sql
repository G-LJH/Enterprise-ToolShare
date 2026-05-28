ALTER TABLE tools RENAME COLUMN title TO name;
ALTER TABLE tools RENAME COLUMN owner_id TO recommender_id;

ALTER TABLE tools
    ADD COLUMN url VARCHAR(512);

ALTER TABLE tools
    ADD COLUMN usage_guide TEXT;

ALTER TABLE tools
    ADD COLUMN star_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE tools
    ADD COLUMN favorite_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE tools
    ADD COLUMN comment_count INTEGER NOT NULL DEFAULT 0;

UPDATE tools
SET url = CONCAT('https://placeholder.local/tool/', id)
WHERE url IS NULL OR url = '';

ALTER TABLE tools
    ALTER COLUMN url SET NOT NULL;

ALTER TABLE tools
    DROP CONSTRAINT fk_tools_owner;

ALTER TABLE tools
    ADD CONSTRAINT fk_tools_recommender FOREIGN KEY (recommender_id) REFERENCES users (id);

ALTER INDEX idx_tools_owner_id RENAME TO idx_tools_recommender_id;

CREATE INDEX idx_tools_name ON tools (name);
