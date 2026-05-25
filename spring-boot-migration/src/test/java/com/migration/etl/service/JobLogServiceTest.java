package com.migration.etl.service;

import com.migration.etl.model.LogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobLogServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private JobLogService jobLogService;

    @Test
    void log_shouldInsertAllTwelveColumns() {
        LocalDateTime moment = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        LogEntry entry = new LogEntry(
                moment, "pid1", "root1", "father1",
                "TestProject", "TestJob", "Default",
                6, "tWarn", "component1", "Test message", 42
        );

        jobLogService.log(entry);

        String expectedSql = "INSERT INTO t_log_catcher_stats (moment, pid, root_pid, father_pid, project, job, context, priority, type, origin, message, code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        verify(jdbcTemplate).update(
                eq(expectedSql),
                eq(moment),
                eq("pid1"),
                eq("root1"),
                eq("father1"),
                eq("TestProject"),
                eq("TestJob"),
                eq("Default"),
                eq(6),
                eq("tWarn"),
                eq("component1"),
                eq("Test message"),
                eq(42)
        );
    }

    @Test
    void log_shouldSwallowExceptionWhenDatabaseFails() {
        LogEntry entry = new LogEntry(
                LocalDateTime.now(), "pid1", "root1", "father1",
                "TestProject", "TestJob", "Default",
                6, "tWarn", "component1", "Test message", 42
        );

        when(jdbcTemplate.update(any(String.class),
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any()))
                .thenThrow(new DataAccessException("DB connection failed") {});

        assertDoesNotThrow(() -> jobLogService.log(entry));
    }

    @Test
    void logConvenience_shouldConstructLogEntryAndDelegate() {
        jobLogService.log("MyProject", "MyJob", "Something happened", 100, 5, "tDie");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(sqlCaptor.capture(),
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any());

        String sql = sqlCaptor.getValue();
        assertEquals(
                "INSERT INTO t_log_catcher_stats (moment, pid, root_pid, father_pid, project, job, context, priority, type, origin, message, code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                sql
        );
    }
}
