package com.toolshare.workflow.repository;

import com.toolshare.workflow.model.WorkflowToolMappingRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class WorkflowToolRepository {

    private static final RowMapper<WorkflowToolMappingRecord> WORKFLOW_TOOL_ROW_MAPPER = (rs, rowNum) -> new WorkflowToolMappingRecord(
            rs.getLong("workflow_id"),
            rs.getLong("tool_id"),
            rs.getInt("sort_order")
    );

    private final JdbcTemplate jdbcTemplate;

    public WorkflowToolRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void replaceWorkflowTools(Long workflowId, List<Long> toolIds) {
        jdbcTemplate.update("DELETE FROM workflow_tools WHERE workflow_id = ?", workflowId);
        for (int i = 0; i < toolIds.size(); i++) {
            jdbcTemplate.update("""
                            INSERT INTO workflow_tools (workflow_id, tool_id, sort_order)
                            VALUES (?, ?, ?)
                            """,
                    workflowId,
                    toolIds.get(i),
                    i + 1
            );
        }
    }

    public Map<Long, List<Long>> findToolIdsByWorkflowIds(List<Long> workflowIds) {
        if (workflowIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = workflowIds.stream().map(id -> "?").reduce((left, right) -> left + "," + right).orElse("?");
        List<WorkflowToolMappingRecord> mappings = jdbcTemplate.query("""
                        SELECT workflow_id, tool_id, sort_order
                        FROM workflow_tools
                        WHERE workflow_id IN (%s)
                        ORDER BY workflow_id, sort_order ASC, tool_id ASC
                        """.formatted(placeholders),
                WORKFLOW_TOOL_ROW_MAPPER,
                workflowIds.toArray()
        );
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        mappings.forEach(mapping -> result.computeIfAbsent(mapping.workflowId(), ignored -> new ArrayList<>()).add(mapping.toolId()));
        return result;
    }

    public boolean existsByToolId(Long toolId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM workflow_tools
                        WHERE tool_id = ?
                        """,
                Integer.class,
                toolId
        );
        return count != null && count > 0;
    }
}
