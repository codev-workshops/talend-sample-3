package com.migration.etl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.migration.etl.job.CustomerSetupJob;
import com.migration.etl.job.FlavorSetupJob;

@Configuration
public class PipelineConfig {

    @Bean
    public CustomerSetupJob customerSetupJob(JdbcTemplate jdbcTemplate,
                                             @Value("${app.csv.customer-path:seed_data/ps_cust.csv}") String customerCsvPath) {
        return new CustomerSetupJob(jdbcTemplate, customerCsvPath);
    }

    @Bean
    public FlavorSetupJob flavorSetupJob(JdbcTemplate jdbcTemplate,
                                         @Value("${app.csv.flavor-path:seed_data/ps_flavor.csv}") String flavorCsvPath) {
        return new FlavorSetupJob(jdbcTemplate, flavorCsvPath);
    }
}
