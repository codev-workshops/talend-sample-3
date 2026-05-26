package com.migration.etl.integration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;

import com.migration.etl.config.MailConfig;

@TestConfiguration
@EnableConfigurationProperties(MailConfig.class)
public class TestMailConfig {
}
