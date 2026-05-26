package com.migration.etl.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.migration.etl.job.AnalyticsSummaryJob;
import com.migration.etl.job.CustomerSetupJob;
import com.migration.etl.job.FlavorSetupJob;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMailConfig.class)
class AnalyticsSummaryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedData() {
        String custPath = getClass().getClassLoader().getResource("ps_cust.csv").getPath();
        String flavorPath = getClass().getClassLoader().getResource("ps_flavor.csv").getPath();

        new CustomerSetupJob(jdbcTemplate, custPath).run();
        new FlavorSetupJob(jdbcTemplate, flavorPath).run();
        new AnalyticsSummaryJob(jdbcTemplate).run();
    }

    @Test
    void shouldHaveExactly4SummaryRows() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM summary", Integer.class);
        assertThat(count).isEqualTo(4);
    }

    @Test
    void shouldContainVanillaNY() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM summary WHERE flavor_name = 'Vanilla' AND state_cd = 'NY'");
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("count_flavor_id")).intValue()).isEqualTo(2);
    }

    @Test
    void shouldContainChocolateCA() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM summary WHERE flavor_name = 'Chocolate' AND state_cd = 'CA'");
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("count_flavor_id")).intValue()).isEqualTo(3);
    }

    @Test
    void shouldContainMintChipTX() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM summary WHERE flavor_name = 'Mint Chip' AND state_cd = 'TX'");
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("count_flavor_id")).intValue()).isEqualTo(2);
    }

    @Test
    void shouldContainChocolateFL() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM summary WHERE flavor_name = 'Chocolate' AND state_cd = 'FL'");
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("count_flavor_id")).intValue()).isEqualTo(2);
    }

    @Test
    void shouldNotContainRockyRoad() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM summary WHERE flavor_name = 'Rocky Road'");
        assertThat(rows).isEmpty();
    }

    @Test
    void shouldNotContainSingleCustomerCombinations() {
        // These all have only 1 customer and should be filtered by HAVING > 1
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM summary WHERE state_cd = 'IL' AND flavor_name = 'Strawberry'")).isEmpty();
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM summary WHERE state_cd = 'IL' AND flavor_name = 'Vanilla'")).isEmpty();
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM summary WHERE state_cd = 'CO' AND flavor_name = 'Strawberry'")).isEmpty();
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM summary WHERE state_cd = 'OR' AND flavor_name = 'Vanilla'")).isEmpty();
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM summary WHERE state_cd = 'FL' AND flavor_name = 'Cookie Dough'")).isEmpty();
        assertThat(jdbcTemplate.queryForList(
                "SELECT * FROM summary WHERE state_cd = 'TX' AND flavor_name = 'Strawberry'")).isEmpty();
    }
}
