package com.migration.etl.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlavorSetupJobTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private FlavorSetupJob job;

    private String csvPath() {
        return getClass().getClassLoader().getResource("ps_flavor.csv").getPath();
    }

    @BeforeEach
    void setUp() {
        job = new FlavorSetupJob(jdbcTemplate, csvPath());
    }

    @Test
    void testDropCreateAndInsert() {
        job.run();

        verify(jdbcTemplate).execute(eq("DROP TABLE IF EXISTS flavors"));
        verify(jdbcTemplate).execute(contains("CREATE TABLE flavors"));
        verify(jdbcTemplate, atLeastOnce()).update(
                eq("INSERT INTO flavors (flavor_id, flavor_name) VALUES (?, ?)"),
                anyInt(), anyString());
    }

    @Test
    void testInsertError_propagates() {
        doThrow(new RuntimeException("DB error"))
                .when(jdbcTemplate)
                .update(anyString(), anyInt(), anyString());

        assertThrows(RuntimeException.class, () -> job.run());
    }
}
