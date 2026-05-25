package com.migration.etl.job;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsSummaryJob implements Job {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsSummaryJob(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS summary");
        jdbcTemplate.execute("CREATE TABLE summary (count_flavor_id INT, flavor_name VARCHAR(100), state_cd VARCHAR(100))");
        jdbcTemplate.execute(
                "INSERT INTO summary (count_flavor_id, flavor_name, state_cd) "
                + "SELECT COUNT(f.flavor_id), f.flavor_name, c.state_cd "
                + "FROM flavors f "
                + "INNER JOIN customer c ON f.flavor_id = c.flavor "
                + "GROUP BY c.state_cd, f.flavor_name "
                + "HAVING COUNT(f.flavor_id) > 1");
    }
}
