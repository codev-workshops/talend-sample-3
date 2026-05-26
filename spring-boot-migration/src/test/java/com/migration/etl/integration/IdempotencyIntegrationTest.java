package com.migration.etl.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
class IdempotencyIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String custPath() {
        return getClass().getClassLoader().getResource("ps_cust.csv").getPath();
    }

    private String flavorPath() {
        return getClass().getClassLoader().getResource("ps_flavor.csv").getPath();
    }

    @Test
    void runningPipelineTwice_shouldNotDuplicateData() {
        // First run
        new CustomerSetupJob(jdbcTemplate, custPath()).run();
        new FlavorSetupJob(jdbcTemplate, flavorPath()).run();
        new AnalyticsSummaryJob(jdbcTemplate).run();

        // Second run
        new CustomerSetupJob(jdbcTemplate, custPath()).run();
        new FlavorSetupJob(jdbcTemplate, flavorPath()).run();
        new AnalyticsSummaryJob(jdbcTemplate).run();

        Integer customerCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer", Integer.class);
        Integer flavorCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flavors", Integer.class);
        Integer summaryCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM summary", Integer.class);

        assertThat(customerCount).isEqualTo(15);
        assertThat(flavorCount).isEqualTo(6);
        assertThat(summaryCount).isEqualTo(4);
    }
}
