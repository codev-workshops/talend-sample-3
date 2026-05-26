package com.migration.etl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.migration.etl.config.JobConfig;
import com.migration.etl.config.MailConfig;

@SpringBootApplication
@EnableConfigurationProperties({JobConfig.class, MailConfig.class})
public class EtlApplication {
    public static void main(String[] args) {
        SpringApplication.run(EtlApplication.class, args);
    }
}
