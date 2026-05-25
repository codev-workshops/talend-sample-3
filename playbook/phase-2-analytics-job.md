# Phase 2 — Analytics Summary Job

## Prerequisites

**Phase 1A (Config Service), Phase 1B (CSV Loader Jobs), and Phase 1C (Logging Service) must all be complete and merged to `master`.**

The `spring-boot-migration/` directory must contain:
- `ContextService` (Phase 1A) — for loading job context
- `CustomerSetupJob` and `FlavorSetupJob` (Phase 1B) — these create the `customer` and `flavors` tables that this job reads from
- `JobLogService` (Phase 1C) — for logging pipeline events
- The `Job` interface in `com.migration.etl.job`

## Repository

`codev-workshops/talend-sample-3` (branch: `master`)

## Context

The original Talend job `j_usecase_analytics_summary` performs a multi-stage pipeline:
1. `tDBInput` reads from the `customer` table
2. `tDBInput` reads from the `flavors` table
3. `tMap` performs an INNER JOIN on `flavor_id = flavor`
4. `tAggregateRow` groups by `state_cd` and `flavor_name`, counting `flavor_id`
5. `tFilterRow` filters to only groups with count > 1
6. `tDBOutput` writes results to the `summary` table

See `TALEND_AWS_DI_USECASE/process/j_usecase_analytics_summary_0.1.item` for the full pipeline definition.

All of these stages can be collapsed into a single SQL statement that is functionally equivalent.

## Task

Implement `AnalyticsSummaryJob.java` in `spring-boot-migration/src/main/java/com/migration/etl/job/`.

### `AnalyticsSummaryJob.java` — `com.migration.etl.job`

- Implements the `Job` interface (`void run()`)
- Annotate with `@Component`
- Inject `JdbcTemplate` via constructor

**Execution steps** (in this exact order):

1. **Drop**: `DROP TABLE IF EXISTS summary`
2. **Create**: `CREATE TABLE summary (count_flavor_id INT, flavor_name VARCHAR(100), state_cd VARCHAR(100))`
3. **Populate**: Execute the following SQL:

```sql
INSERT INTO summary (count_flavor_id, flavor_name, state_cd)
SELECT COUNT(f.flavor_id), f.flavor_name, c.state_cd
FROM flavors f
INNER JOIN customer c ON f.flavor_id = c.flavor
GROUP BY c.state_cd, f.flavor_name
HAVING COUNT(f.flavor_id) > 1
```

4. **`dieOnError=true`**: If any SQL statement fails, throw a `RuntimeException` immediately. Do not catch or swallow errors.

### Expected Output

When run against the seed data (15 customers, 6 flavors), the `summary` table should contain exactly **4 rows**:

| count_flavor_id | flavor_name | state_cd |
|-----------------|-------------|----------|
| 2 | Vanilla | NY |
| 3 | Chocolate | CA |
| 2 | Mint Chip | TX |
| 2 | Chocolate | FL |

**Why only 4 rows?**
- Rocky Road (`flavor_id=6`) has zero customers → dropped by INNER JOIN
- Single-customer state/flavor pairs are filtered by `HAVING COUNT > 1`:
  - (IL, Strawberry), (IL, Vanilla), (CO, Strawberry), (OR, Vanilla), (FL, Cookie Dough), (TX, Strawberry)

### Unit Test — `AnalyticsSummaryJobTest.java`

In `src/test/java/com/migration/etl/job/`:

- Use `@ExtendWith(MockitoExtension.class)`
- Mock `JdbcTemplate`
- **Test 1**: Verify that DDL and INSERT SQL are executed in the correct order:
  1. First call: DROP TABLE
  2. Second call: CREATE TABLE
  3. Third call: INSERT INTO ... SELECT
- **Test 2**: Verify that when `JdbcTemplate.execute()` throws, the exception propagates (is NOT caught)
- **Test 3**: Verify the exact INSERT SQL string matches the expected query

## Reference Files

- `TALEND_AWS_DI_USECASE/process/j_usecase_analytics_summary_0.1.item` — original Talend pipeline
- `seed_data/README.md` lines 48–69 — expected output and edge cases
- `seed_data/ps_cust.csv` — 15 customer rows (used to verify expected output)
- `seed_data/ps_flavor.csv` — 6 flavor rows

## Acceptance Criteria

- All unit tests pass
- DDL executed in correct order (DROP → CREATE → INSERT)
- INSERT SQL is functionally equivalent to the 4-stage Talend pipeline
- Errors propagate (`dieOnError=true`)
- `mvn test` passes

## Commit & PR

- Branch from `master`
- Commit message: `feat: implement AnalyticsSummaryJob with aggregation SQL`
- Create a PR targeting `master`
