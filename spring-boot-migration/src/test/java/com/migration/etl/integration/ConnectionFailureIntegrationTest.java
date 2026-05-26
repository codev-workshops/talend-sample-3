package com.migration.etl.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import com.migration.etl.job.CustomerSetupJob;

class ConnectionFailureIntegrationTest {

    @Test
    void shouldFailWithClearError_whenDatasourceIsInvalid() {
        JdbcTemplate badJdbc = new JdbcTemplate(
                DataSourceBuilder.create()
                        .url("jdbc:h2:tcp://invalid-host:9999/nonexistent")
                        .username("sa")
                        .password("")
                        .driverClassName("org.h2.Driver")
                        .build());

        String csvPath = getClass().getClassLoader().getResource("ps_cust.csv").getPath();
        CustomerSetupJob job = new CustomerSetupJob(badJdbc, csvPath);

        assertThatThrownBy(job::run)
                .isInstanceOf(Exception.class);
    }
}
