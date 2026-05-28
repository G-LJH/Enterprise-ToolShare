INSERT INTO system_configs (config_key, config_value, description, created_by, updated_by)
SELECT 'TOOL_REVIEW_ENABLED', 'true', '控制普通用户提交工具时是否需要管理员审核', 0, 0
WHERE NOT EXISTS (
    SELECT 1
    FROM system_configs
    WHERE config_key = 'TOOL_REVIEW_ENABLED'
      AND deleted = FALSE
);
