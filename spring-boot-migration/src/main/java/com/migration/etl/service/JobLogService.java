package com.migration.etl.service;

import com.migration.etl.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class JobLogService {

    private static final Logger log = LoggerFactory.getLogger(JobLogService.class);

    private static final String INSERT_SQL =
            "INSERT INTO t_log_catcher_stats (moment, pid, root_pid, father_pid, project, job, context, priority, type, origin, message, code) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public JobLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void log(LogEntry entry) {
        try {
            jdbcTemplate.update(INSERT_SQL,
                    entry.getMoment(),
                    entry.getPid(),
                    entry.getRootPid(),
                    entry.getFatherPid(),
                    entry.getProject(),
                    entry.getJob(),
                    entry.getContext(),
                    entry.getPriority(),
                    entry.getType(),
                    entry.getOrigin(),
                    entry.getMessage(),
                    entry.getCode());
        } catch (Exception e) {
            log.error("Failed to write log entry to t_log_catcher_stats: {}", e.getMessage(), e);
        }
    }

    public void log(String project, String job, String message, int code, int priority, String type) {
        String pid = UUID.randomUUID().toString().substring(0, 20);
        LogEntry entry = new LogEntry(
                LocalDateTime.now(),
                pid,
                pid,
                pid,
                project,
                job,
                "Default",
                priority,
                type,
                job,
                message,
                code
        );
        log(entry);
    }
}
