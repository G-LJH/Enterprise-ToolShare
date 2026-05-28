ALTER TABLE import_tasks
    ADD COLUMN file_name VARCHAR(256);

ALTER TABLE import_tasks
    ADD COLUMN success_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE import_tasks
    ADD COLUMN failure_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE import_tasks
    ADD COLUMN error_report_path VARCHAR(512);

ALTER TABLE import_tasks
    ADD COLUMN detail TEXT;

ALTER TABLE export_tasks
    ADD COLUMN file_name VARCHAR(256);

ALTER TABLE export_tasks
    ADD COLUMN success_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE export_tasks
    ADD COLUMN failure_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE export_tasks
    ADD COLUMN error_report_path VARCHAR(512);

ALTER TABLE export_tasks
    ADD COLUMN detail TEXT;
