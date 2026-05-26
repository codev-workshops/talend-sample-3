package com.migration.etl.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.migration.etl.job.CustomerSetupJob;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMailConfig.class)
class CustomerLoadIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String csvPath() {
        return getClass().getClassLoader().getResource("ps_cust.csv").getPath();
    }

    @Test
    void shouldLoad15CustomerRows() {
        CustomerSetupJob job = new CustomerSetupJob(jdbcTemplate, csvPath());
        job.run();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer", Integer.class);
        assertThat(count).isEqualTo(15);
    }

    @Test
    void shouldContainFirstRow() {
        CustomerSetupJob job = new CustomerSetupJob(jdbcTemplate, csvPath());
        job.run();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM customer WHERE id = 1");
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("first_name")).isEqualTo("Alice");
        assertThat(row.get("last_name")).isEqualTo("Smith");
        assertThat(row.get("city")).isEqualTo("New York");
        assertThat(row.get("state_cd")).isEqualTo("NY");
        assertThat(((Number) row.get("flavor")).intValue()).isEqualTo(1);
    }

    @Test
    void shouldContainLastRow() {
        CustomerSetupJob job = new CustomerSetupJob(jdbcTemplate, csvPath());
        job.run();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM customer WHERE id = 15");
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("first_name")).isEqualTo("Olivia");
        assertThat(row.get("last_name")).isEqualTo("Garcia");
        assertThat(row.get("city")).isEqualTo("Portland");
        assertThat(row.get("state_cd")).isEqualTo("OR");
        assertThat(((Number) row.get("flavor")).intValue()).isEqualTo(1);
    }
}
