package com.migration.etl.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.migration.etl.job.CustomerSetupJob;
import com.migration.etl.job.FlavorSetupJob;
import com.migration.etl.service.CsvLoaderService;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMailConfig.class)
class ZeroRowsIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String emptyCustPath() {
        return getClass().getClassLoader().getResource("ps_cust_empty.csv").getPath();
    }

    private String emptyFlavorPath() {
        return getClass().getClassLoader().getResource("ps_flavor_empty.csv").getPath();
    }

    @Test
    void customerJob_withEmptyCsv_shouldNotThrow() {
        assertDoesNotThrow(() -> {
            CustomerSetupJob job = new CustomerSetupJob(jdbcTemplate, emptyCustPath());
            job.run();
        });
    }

    @Test
    void customerJob_withEmptyCsv_shouldHaveZeroRows() {
        CustomerSetupJob job = new CustomerSetupJob(jdbcTemplate, emptyCustPath());
        job.run();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    void customerJob_withEmptyCsv_shouldLogWarning() {
        Logger csvLogger = (Logger) LoggerFactory.getLogger(CsvLoaderService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        csvLogger.addAppender(appender);

        try {
            CustomerSetupJob job = new CustomerSetupJob(jdbcTemplate, emptyCustPath());
            job.run();

            boolean hasWarn = appender.list.stream()
                    .anyMatch(e -> e.getLevel() == Level.WARN
                            && e.getFormattedMessage().contains("is mepty"));
            assertThat(hasWarn).isTrue();
        } finally {
            csvLogger.detachAppender(appender);
        }
    }

    @Test
    void flavorJob_withEmptyCsv_shouldNotThrow() {
        assertDoesNotThrow(() -> {
            FlavorSetupJob job = new FlavorSetupJob(jdbcTemplate, emptyFlavorPath());
            job.run();
        });
    }

    @Test
    void flavorJob_withEmptyCsv_shouldHaveZeroRows() {
        FlavorSetupJob job = new FlavorSetupJob(jdbcTemplate, emptyFlavorPath());
        job.run();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flavors", Integer.class);
        assertThat(count).isEqualTo(0);
    }
}
