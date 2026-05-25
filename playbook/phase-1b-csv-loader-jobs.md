# Phase 1B — CSV Loader Jobs

## Prerequisites

**Phase 0 (Scaffolding) must be complete and merged to `master`.**

The `spring-boot-migration/` directory must exist with the Maven project skeleton. Test CSV files should be in `src/test/resources/`.

## Repository

`codev-workshops/talend-sample-3` (branch: `master`)

## Context

The original Talend project has two setup jobs that ingest pipe-delimited CSV files into MySQL:

- `j_usecase_customer_setup` — reads `ps_cust.csv` and loads into the `customer` table
- `j_usecase_flavor_setup` — reads `ps_flavor.csv` and loads into the `flavors` table

Key configuration from `TALEND_AWS_DI_USECASE/process/j_usecase_customer_setup_0.1.item`:
- **Lines 117–139**: `tFileInputDelimited` config — pipe delimiter (`|`), 1 header row, ISO-8859-15 encoding, trim all fields
- **Lines 197–214**: `tMysqlOutput` config — table action is `DROP_IF_EXISTS_AND_CREATE`, data action is `INSERT`, `DIE_ON_ERROR=false`
- **Line 350**: tWarn message for empty file: `"The Customer input file does not have any records / is mepty"` with code 42

## Task

Implement the following classes in `spring-boot-migration/`.

### 1. `CsvLoaderService.java` — `com.migration.etl.service`

A generic pipe-delimited CSV reader service:

- Constructor/config takes a file path (String)
- Reads file with **ISO-8859-15** encoding
- **Pipe (`|`) delimiter**
- **Skips 1 header row**
- **Trims all fields**
- Returns `List<String[]>` (each array is one row's fields)
- If the file has 0 data rows (only header):
  - Log a WARN with message: `"The Customer input file does not have any records / is mepty"` (preserve the typo exactly)
  - Log code: `42`
  - Return an empty list (do NOT throw)

### 2. `Customer.java` — `com.migration.etl.model`

Model class with fields:
- `id` — Integer
- `first_name` — String (VARCHAR 100)
- `last_name` — String (VARCHAR 100)
- `city` — String (VARCHAR 100)
- `state_cd` — String (VARCHAR 100)
- `flavor` — Integer

Include a constructor, getters, and setters (or use Lombok if available in the project).

### 3. `Flavor.java` — `com.migration.etl.model`

Model class with fields:
- `flavor_id` — Integer
- `flavor_name` — String (VARCHAR 100)

Include a constructor, getters, and setters.

### 4. `CustomerSetupJob.java` — `com.migration.etl.job`

Implements the `Job` interface (`void run()`):

1. Use `CsvLoaderService` to read CSV from a configurable path (inject via constructor or application properties)
2. Execute `DROP TABLE IF EXISTS customer`
3. Execute `CREATE TABLE customer (id INT, first_name VARCHAR(100), last_name VARCHAR(100), city VARCHAR(100), state_cd VARCHAR(100), flavor INT)`
4. Batch INSERT all rows from the CSV
5. **`dieOnError=false`**: Catch and log individual insert errors — do NOT throw exceptions. If an insert fails, log the error and continue with the next row.
6. Inject `JdbcTemplate` for database operations

Column types are derived from the tFileInputDelimited schema at `j_usecase_customer_setup_0.1.item` lines 178–184.

### 5. `FlavorSetupJob.java` — `com.migration.etl.job`

Same pattern as CustomerSetupJob but for flavors:

1. Read `ps_flavor.csv` using `CsvLoaderService`
2. Execute `DROP TABLE IF EXISTS flavors`
3. Execute `CREATE TABLE flavors (flavor_id INT, flavor_name VARCHAR(100))`
4. Batch INSERT all rows
5. **`dieOnError=true`**: If any insert fails, throw a `RuntimeException` immediately

### 6. Unit Tests

In `src/test/java/`:

#### `CsvLoaderServiceTest.java`
- Test parsing `ps_cust.csv` from test resources — expect 15 data rows
- Test parsing `ps_flavor.csv` — expect 6 data rows
- Test parsing `ps_cust_empty.csv` — expect 0 rows and a WARN log message containing `"is mepty"`
- Test parsing `ps_flavor_empty.csv` — expect 0 rows

#### `CustomerSetupJobTest.java`
- Mock `JdbcTemplate`
- Verify DROP TABLE, CREATE TABLE, and INSERT SQL statements are executed
- Verify that when JdbcTemplate throws on an INSERT, the exception is caught (not propagated)

#### `FlavorSetupJobTest.java`
- Mock `JdbcTemplate`
- Verify DROP TABLE, CREATE TABLE, and INSERT SQL statements
- Verify that when JdbcTemplate throws on an INSERT, the exception IS propagated

## Reference Files

- `seed_data/ps_cust.csv` — 15 data rows, pipe-delimited
- `seed_data/ps_flavor.csv` — 6 data rows, pipe-delimited
- `seed_data/ps_cust_empty.csv` — header only
- `seed_data/ps_flavor_empty.csv` — header only
- `TALEND_AWS_DI_USECASE/process/j_usecase_customer_setup_0.1.item`:
  - Lines 117–139: tFileInputDelimited config (CSV parsing)
  - Lines 178–184: Column schema (id, first_name, last_name, city, state_cd, flavor)
  - Lines 197–214: tMysqlOutput config (DROP_IF_EXISTS_AND_CREATE, INSERT, DIE_ON_ERROR=false)
  - Line 350: tWarn empty file message and code 42

## Acceptance Criteria

- All unit tests pass
- CSV files parse correctly (15 rows from `ps_cust.csv`, 6 from `ps_flavor.csv`)
- Empty file triggers WARN log with message containing `"is mepty"` and code 42
- CustomerSetupJob swallows insert errors (`dieOnError=false`)
- FlavorSetupJob propagates insert errors (`dieOnError=true`)
- `mvn test` passes

## Commit & PR

- Branch from `master`
- Commit message: `feat: implement CSV loader service and setup jobs`
- Create a PR targeting `master`
