package com.migration.etl.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ContextService {

    private final JdbcTemplate jdbcTemplate;

    public ContextService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, String> getJobContext(String project, String jobName) {
        String sql = "SELECT context_name, contect_value "
                   + "FROM context_entries "
                   + "WHERE project = ? AND job = ?";

        Map<String, String> context = new HashMap<>();
        jdbcTemplate.query(sql, new Object[]{project, jobName}, (rs) -> {
            context.put(rs.getString("context_name"), rs.getString("contect_value"));
        });
        return context;
    }
}
