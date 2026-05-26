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

import com.migration.etl.job.FlavorSetupJob;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMailConfig.class)
class FlavorLoadIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String csvPath() {
        return getClass().getClassLoader().getResource("ps_flavor.csv").getPath();
    }

    @Test
    void shouldLoad6FlavorRows() {
        FlavorSetupJob job = new FlavorSetupJob(jdbcTemplate, csvPath());
        job.run();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flavors", Integer.class);
        assertThat(count).isEqualTo(6);
    }

    @Test
    void shouldContainVanilla() {
        FlavorSetupJob job = new FlavorSetupJob(jdbcTemplate, csvPath());
        job.run();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM flavors WHERE flavor_id = 1");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("flavor_name")).isEqualTo("Vanilla");
    }

    @Test
    void shouldContainRockyRoad() {
        FlavorSetupJob job = new FlavorSetupJob(jdbcTemplate, csvPath());
        job.run();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM flavors WHERE flavor_id = 6");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("flavor_name")).isEqualTo("Rocky Road");
    }
}
