package com.migration.etl.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerSetupJobTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private CustomerSetupJob job;

    private String csvPath() {
        return getClass().getClassLoader().getResource("ps_cust.csv").getPath();
    }

    @BeforeEach
    void setUp() {
        job = new CustomerSetupJob(jdbcTemplate, csvPath());
    }

    @Test
    void testDropCreateAndInsert() {
        job.run();

        verify(jdbcTemplate).execute(eq("DROP TABLE IF EXISTS customer"));
        verify(jdbcTemplate).execute(contains("CREATE TABLE customer"));
        verify(jdbcTemplate, atLeastOnce()).update(
                eq("INSERT INTO customer (id, first_name, last_name, city, state_cd, flavor) VALUES (?, ?, ?, ?, ?, ?)"),
                anyInt(), anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void testInsertError_doesNotPropagate() {
        doThrow(new RuntimeException("DB error"))
                .when(jdbcTemplate)
                .update(anyString(), anyInt(), anyString(), anyString(), anyString(), anyString(), anyInt());

        assertDoesNotThrow(() -> job.run());
    }
}
