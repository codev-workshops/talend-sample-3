# Phase 1C — Logging Service

## Prerequisites

**Phase 0 (Scaffolding) must be complete and merged to `master`.**

The `spring-boot-migration/` directory must exist with the Maven project skeleton, including the `t_log_catcher_stats` table definition in `schema.sql`.

## Repository

`codev-workshops/talend-sample-3` (branch: `master`)

## Context

The original Talend project uses a joblet (`jblt_enable_logging`) to capture runtime events — Java exceptions, tDie errors, tWarn messages, and tActionFailure events — and write them to a MySQL table called `t_log_catcher_stats`. The joblet is configured at `TALEND_AWS_DI_USECASE/joblets/jblt_enable_logging_0.1.item`:

- **Lines 83–103**: tLogCatcher component — captures all event types (Java exceptions, tDie, tWarn, tActionFailure) and outputs a 12-column schema
- **Lines 105–125**: tMysqlOutput component — writes to `t_log_catcher_stats` with `TABLE_ACTION=CREATE_IF_NOT_EXISTS`, `DATA_ACTION=INSERT`, `DIE_ON_ERROR=false`

The critical behavior: **logging must never cause the main pipeline to fail**. If the database write fails, the error is silently swallowed.

## Task

Implement the following classes in `spring-boot-migration/`.

### 1. `LogEntry.java` — `com.migration.etl.model`

Model class with 12 fields matching the tLogCatcher schema (from `jblt_enable_logging_0.1.item` lines 90–102):

| Field | Type | Length | Notes |
|-------|------|--------|-------|
| `moment` | `LocalDateTime` | — | Format: `yyyy-MM-dd HH:mm:ss` |
| `pid` | `String` | 20 | Process ID |
| `root_pid` | `String` | 20 | Root process ID |
| `father_pid` | `String` | 20 | Parent process ID |
| `project` | `String` | 50 | Project name |
| `job` | `String` | 255 | Job name |
| `context` | `String` | 50 | Execution context |
| `priority` | `Integer` | — | Log priority level |
| `type` | `String` | 255 | Event type |
| `origin` | `String` | 255 | Event origin |
| `message` | `String` | 255 | Log message |
| `code` | `Integer` | — | Event code |

Include constructors, getters, and setters.

### 2. `JobLogService.java` — `com.migration.etl.service`

- Annotate with `@Service`
- Inject `JdbcTemplate` via constructor
- Implement method:

```java
public void log(LogEntry entry)
```

**Table handling**:
- The table `t_log_catcher_stats` is created by `schema.sql` at startup (CREATE_IF_NOT_EXISTS behavior)
- No need to check/create the table at runtime — it will already exist

**Data action**: INSERT the LogEntry into `t_log_catcher_stats` with all 12 columns.

**Critical requirement — non-fatal logging**:
- Wrap the entire `log()` method body in a try-catch that catches **ALL exceptions** (`Exception` or `Throwable`)
- On failure: log the error via SLF4J (`log.error(...)`) but **never propagate the exception**
- This matches the original Talend behavior where `DIE_ON_ERROR=false` on the logging output

Also implement a convenience method:

```java
public void log(String project, String job, String message, int code, int priority, String type)
```

This should construct a `LogEntry` with sensible defaults (current timestamp, generated PID, etc.) and delegate to the primary `log(LogEntry)` method.

### 3. Unit Test — `JobLogServiceTest.java`

In `src/test/java/com/migration/etl/service/`:

- Use `@ExtendWith(MockitoExtension.class)`
- Mock `JdbcTemplate`
- **Test 1**: Verify INSERT SQL is correct — check that `jdbcTemplate.update()` is called with the right SQL and 12 parameters
- **Test 2**: Verify exception swallowing — configure the mocked `jdbcTemplate.update()` to throw a `DataAccessException`, then call `log()` and assert that **no exception propagates** to the caller
- **Test 3**: Verify the convenience method constructs a valid LogEntry and delegates to the primary method

## Reference Files

- `TALEND_AWS_DI_USECASE/joblets/jblt_enable_logging_0.1.item`:
  - Lines 83–103: tLogCatcher schema (12 columns)
  - Lines 105–125: tMysqlOutput config (CREATE_IF_NOT_EXISTS, INSERT, DIE_ON_ERROR=false)

## Acceptance Criteria

- All unit tests pass
- INSERT SQL includes all 12 columns
- When `JdbcTemplate` throws, the exception is caught and **not propagated**
- SLF4J error log is emitted on failure
- `mvn test` passes

## Commit & PR

- Branch from `master`
- Commit message: `feat: implement JobLogService for pipeline event logging`
- Create a PR targeting `master`
