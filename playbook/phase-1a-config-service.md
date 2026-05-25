# Phase 1A — Config / Context Service

## Prerequisites

**Phase 0 (Scaffolding) must be complete and merged to `master`.**

The `spring-boot-migration/` directory must exist with the Maven project skeleton, including the `context_entries` table in `schema.sql` and seed data in `data.sql`.

## Repository

`codev-workshops/talend-sample-3` (branch: `master`)

## Context

The original Talend project uses a joblet (`jblt_load_job_specific_context`) to load runtime configuration from a MySQL table called `context_entries`. Each job queries this table at startup to get parameters like target table names and input file paths. The SQL is at `TALEND_AWS_DI_USECASE/joblets/jblt_load_job_specific_context_0.1.item` line 110:

```sql
select context_name as `key`, contect_value as `value`
from analytics_dev.context_entries
where project='...' and job='...'
```

> **Note**: `contect_value` is intentionally misspelled — it matches the actual DB column name.

## Task

Implement `ContextService.java` in the `com.migration.etl.config` package inside `spring-boot-migration/`.

### 1. `ContextService.java`

```java
package com.migration.etl.config;
```

- Annotate with `@Service`
- Inject `JdbcTemplate` via constructor
- Implement method:

```java
public Map<String, String> getJobContext(String project, String jobName)
```

- SQL query (use parameterized query with `?` placeholders):

```sql
SELECT context_name AS key, contect_value AS value
FROM context_entries
WHERE project = ? AND job = ?
```

- Map each row to a key-value pair and return as `Map<String, String>`
- Do NOT include the `analytics_dev.` schema prefix — Spring Boot uses the datasource's default schema

### 2. `JobConfig.java` (optional but recommended)

If useful, create a `@ConfigurationProperties(prefix = "app.job")` bean in `com.migration.etl.config` to hold the default project name:

```java
@ConfigurationProperties(prefix = "app.job")
public class JobConfig {
    private String project = "TALEND_AWS_DI_USECASE";
    // getter and setter
}
```

Make sure to annotate the main application class or a config class with `@EnableConfigurationProperties(JobConfig.class)`.

### 3. Unit Test — `ContextServiceTest.java`

In `src/test/java/com/migration/etl/config/`:

- Use `@ExtendWith(MockitoExtension.class)`
- Mock `JdbcTemplate`
- Verify the correct SQL string and parameter binding
- Test that `getJobContext("TALEND_AWS_DI_USECASE", "j_usecase_customer_setup")` returns the expected map

### 4. Integration Test — `ContextServiceIntegrationTest.java`

In `src/test/java/com/migration/etl/config/`:

- Use `@SpringBootTest` with the test profile (`@ActiveProfiles("test")`)
- The H2 database will be auto-populated from `schema.sql` and `data.sql`
- Test that `getJobContext("TALEND_AWS_DI_USECASE", "j_usecase_customer_setup")` returns:
  ```
  {target_table_name=customer, cust_input_file=/root/use_case/ps_cust.csv}
  ```
- Test that `getJobContext("TALEND_AWS_DI_USECASE", "j_usecase_flavor_setup")` returns:
  ```
  {target_table_name=flavors, flavor_input_file=/root/use_case/ps_flavor.csv}
  ```

## Reference Files

- `seed_data/seed_context_entries.sql` — table schema and sample data
- `TALEND_AWS_DI_USECASE/joblets/jblt_load_job_specific_context_0.1.item` line 110 — original SQL query

## Acceptance Criteria

- All unit tests pass
- All integration tests pass
- `getJobContext("TALEND_AWS_DI_USECASE", "j_usecase_customer_setup")` returns `{target_table_name=customer, cust_input_file=/root/use_case/ps_cust.csv}`
- `getJobContext("TALEND_AWS_DI_USECASE", "j_usecase_flavor_setup")` returns `{target_table_name=flavors, flavor_input_file=/root/use_case/ps_flavor.csv}`
- `mvn test` passes

## Commit & PR

- Branch from `master`
- Commit message: `feat: implement ContextService for job configuration loading`
- Create a PR targeting `master`
