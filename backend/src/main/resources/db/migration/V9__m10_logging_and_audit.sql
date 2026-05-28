CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_object_type ON audit_logs (object_type);

CREATE INDEX IF NOT EXISTS idx_operation_logs_created_at ON operation_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_operation_logs_module ON operation_logs (module);
CREATE INDEX IF NOT EXISTS idx_operation_logs_success ON operation_logs (success);
