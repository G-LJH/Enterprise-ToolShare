package com.toolshare.systemconfig.repository;

import com.toolshare.systemconfig.model.SystemConfigRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SystemConfigRepository {

    private static final RowMapper<SystemConfigRecord> SYSTEM_CONFIG_ROW_MAPPER = (rs, rowNum) -> new SystemConfigRecord(
            rs.getLong("id"),
            rs.getString("config_key"),
            rs.getString("config_value"),
            rs.getString("description"),
            rs.getBoolean("deleted"),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public SystemConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<SystemConfigRecord> findActiveByKey(String configKey) {
        List<SystemConfigRecord> records = jdbcTemplate.query("""
                        SELECT id, config_key, config_value, description, deleted, created_at, updated_at
                        FROM system_configs
                        WHERE config_key = ?
                          AND deleted = FALSE
                        """,
                SYSTEM_CONFIG_ROW_MAPPER,
                configKey
        );
        return records.stream().findFirst();
    }

    public void updateValue(String configKey, String configValue, Long operatorId) {
        jdbcTemplate.update("""
                        UPDATE system_configs
                        SET config_value = ?, updated_by = ?, updated_at = NOW()
                        WHERE config_key = ?
                          AND deleted = FALSE
                        """,
                configValue,
                operatorId,
                configKey
        );
    }
}
