# Phase 3B — Integration Tests

## Prerequisites

**All Phase 1 sessions (1A, 1B, 1C) and Phase 2 must be complete and merged to `master`.**

The `spring-boot-migration/` directory must contain all implemented services and jobs. Phase 1D (Email) is not required for integration tests.

## Repository

`codev-workshops/talend-sample-3` (branch: `master`)

## Context

The integration tests validate the complete data pipeline using H2 in MySQL compatibility mode. They exercise the full flow: CSV parsing → table creation → data insertion → analytics aggregation → result validation. The expected outputs are documented in `seed_data/README.md` lines 48–69.

## Task

Create integration tests in `spring-boot-migration/src/test/java/com/migration/etl/` using H2 in MySQL compatibility mode. All tests should use:

```java
@SpringBootTest
@ActiveProfiles("test")
```

The test profile uses H2 with `MODE=MySQL` (configured in `application-test.yml`).

### Test 1 — Customer Load (`CustomerLoadIntegrationTest.java`)

1. Run `CustomerSetupJob` with `ps_cust.csv` from test resources (configure the CSV path to point to `src/test/resources/ps_cust.csv` or use classpath resource loading)
2. Assert the `customer` table has **15 rows**
3. Spot-check specific rows:
   - Row 1: `(1, Alice, Smith, New York, NY, 1)`
   - Row 15: `(15, Olivia, Garcia, Portland, OR, 1)`

### Test 2 — Flavor Load (`FlavorLoadIntegrationTest.java`)

1. Run `FlavorSetupJob` with `ps_flavor.csv` from test resources
2. Assert the `flavors` table has **6 rows**
3. Spot-check:
   - `flavor_id=1` → `Vanilla`
   - `flavor_id=6` → `Rocky Road`

### Test 3 — Analytics Summary (`AnalyticsSummaryIntegrationTest.java`)

1. First seed the data: run `CustomerSetupJob` (with `ps_cust.csv`) and `FlavorSetupJob` (with `ps_flavor.csv`)
2. Then run `AnalyticsSummaryJob`
3. Assert the `summary` table has exactly **4 rows**
4. Assert exact values present:
   - `(2, Vanilla, NY)`
   - `(3, Chocolate, CA)`
   - `(2, Mint Chip, TX)`
   - `(2, Chocolate, FL)`
5. Assert Rocky Road is **absent** from the summary (no customer references it)
6. Assert these single-customer combinations are **not present** (filtered by HAVING > 1):
   - `(IL, Strawberry)` — Frank Miller
   - `(IL, Vanilla)` — Grace Lee
   - `(CO, Strawberry)` — Noah Thomas
   - `(OR, Vanilla)` — Olivia Garcia
   - `(FL, Cookie Dough)` — Karen White
   - `(TX, Strawberry)` — Jack Taylor

### Test 4 — Zero Rows (`ZeroRowsIntegrationTest.java`)

1. Run `CustomerSetupJob` with `ps_cust_empty.csv` (header only, 0 data rows)
2. Assert **no exception** is thrown
3. Assert `customer` table has **0 rows**
4. Assert a WARN was logged (containing `"is mepty"`)
5. Repeat the same test for `FlavorSetupJob` with `ps_flavor_empty.csv`:
   - Assert no exception
   - Assert `flavors` table has 0 rows

### Test 5 — Idempotency (`IdempotencyIntegrationTest.java`)

1. Run the full pipeline: CustomerSetupJob → FlavorSetupJob → AnalyticsSummaryJob
2. Run the full pipeline again (second time)
3. Assert the `summary` table still has exactly **4 rows** (not 8)
4. Assert the `customer` table still has exactly **15 rows** (not 30)
5. Assert the `flavors` table still has exactly **6 rows** (not 12)

This works because the jobs use `DROP TABLE IF EXISTS` before `CREATE TABLE`, ensuring clean state on each run.

### Test 6 — Connection Failure (`ConnectionFailureIntegrationTest.java`)

1. Configure an invalid datasource (e.g., wrong host/port or use a separate test config)
2. Assert the application fails with a clear error
3. This can be a unit-style test that verifies `PipelineRunner` behavior when `JdbcTemplate` operations fail, or a Spring Boot test with a deliberately broken datasource URL

## Reference Files

- `seed_data/README.md` lines 48–69 — expected outputs and edge cases
- `seed_data/ps_cust.csv` — 15 customer rows
- `seed_data/ps_flavor.csv` — 6 flavor rows
- `seed_data/ps_cust_empty.csv` — header only
- `seed_data/ps_flavor_empty.csv` — header only

## Test Data Summary

| File | Rows | Notes |
|------|------|-------|
| `ps_cust.csv` | 15 | Pipe-delimited, ISO-8859-15 |
| `ps_flavor.csv` | 6 | Pipe-delimited, ISO-8859-15 |
| `ps_cust_empty.csv` | 0 (header only) | Triggers WARN |
| `ps_flavor_empty.csv` | 0 (header only) | Triggers WARN |

## Acceptance Criteria

- All 6 test categories pass
- `mvn test` completes successfully
- Tests use H2 in MySQL compatibility mode (no real MySQL required)
- Tests validate exact row counts and specific data values
- Idempotency test proves the pipeline can run multiple times safely

## Commit & PR

- Branch from `master`
- Commit message: `test: add integration tests for full ETL pipeline`
- Create a PR targeting `master`
