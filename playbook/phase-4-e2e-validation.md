# Phase 4 — End-to-End Validation

## Prerequisites

**All previous phases (0, 1A–1D, 2, 3A, 3B) must be complete and merged to `master`.**

The `spring-boot-migration/` directory must contain the fully implemented and tested application, including the `Dockerfile` and `docker-compose.yml` from Phase 3A.

## Repository

`codev-workshops/talend-sample-3` (branch: `master`)

## Context

This is the final validation phase. It runs the complete Spring Boot application against a real MySQL 8 instance to verify end-to-end correctness. The expected outputs match those documented in `seed_data/README.md`.

## Task

### Option A: Docker Compose (Preferred)

If `docker-compose.yml` from Phase 3A is available:

1. Build the application: `cd spring-boot-migration && mvn clean package -DskipTests`
2. Start the full stack: `docker-compose up --build -d`
3. Wait for MySQL to be healthy and the app to complete
4. Run the validation queries (see step 6 below)
5. Shut down: `docker-compose down`

### Option B: Manual Setup

1. **Start MySQL 8 Docker container**:
   ```bash
   docker run --name test-mysql \
     -e MYSQL_ROOT_PASSWORD=root \
     -e MYSQL_DATABASE=analytics_dev \
     -e MYSQL_USER=tadmin \
     -e MYSQL_PASSWORD=changeme \
     -p 3306:3306 -d mysql:8
   ```

2. **Wait for MySQL to be ready**:
   ```bash
   # Poll until MySQL accepts connections
   until docker exec test-mysql mysqladmin ping -h localhost --silent; do
     sleep 2
   done
   ```

3. **Seed the context entries table**:
   ```bash
   docker exec -i test-mysql mysql -utadmin -pchangeme analytics_dev < seed_data/seed_context_entries.sql
   ```

4. **Copy CSV files to an accessible path** and configure the app:
   ```bash
   mkdir -p /tmp/etl-input
   cp seed_data/ps_cust.csv /tmp/etl-input/
   cp seed_data/ps_flavor.csv /tmp/etl-input/
   ```

5. **Run the Spring Boot application**:
   ```bash
   cd spring-boot-migration
   mvn clean package -DskipTests
   java -jar target/*.jar \
     --spring.datasource.url=jdbc:mysql://localhost:3306/analytics_dev?noDatetimeStringSync=true\&useSSL=false \
     --spring.datasource.username=tadmin \
     --spring.datasource.password=changeme \
     --app.csv.customer-path=/tmp/etl-input/ps_cust.csv \
     --app.csv.flavor-path=/tmp/etl-input/ps_flavor.csv
   ```

6. **Validate by querying MySQL**:

   ```bash
   docker exec -i test-mysql mysql -utadmin -pchangeme analytics_dev -e "
     SELECT 'customer count' AS test, COUNT(*) AS result FROM customer
     UNION ALL
     SELECT 'flavor count', COUNT(*) FROM flavors
     UNION ALL
     SELECT 'summary count', COUNT(*) FROM summary
     UNION ALL
     SELECT 'log count >= 1', CASE WHEN COUNT(*) >= 1 THEN 'PASS' ELSE 'FAIL' END FROM t_log_catcher_stats;
   "
   ```

   **Expected results**:
   | Test | Expected |
   |------|----------|
   | `SELECT COUNT(*) FROM customer;` | **15** |
   | `SELECT COUNT(*) FROM flavors;` | **6** |
   | `SELECT COUNT(*) FROM summary;` | **4** |
   | `SELECT COUNT(*) FROM t_log_catcher_stats;` | **>= 1** |

   **Validate summary rows**:
   ```bash
   docker exec -i test-mysql mysql -utadmin -pchangeme analytics_dev -e "
     SELECT * FROM summary ORDER BY state_cd, flavor_name;
   "
   ```

   Expected exact output:
   | count_flavor_id | flavor_name | state_cd |
   |-----------------|-------------|----------|
   | 3 | Chocolate | CA |
   | 2 | Chocolate | FL |
   | 2 | Vanilla | NY |
   | 2 | Mint Chip | TX |

7. **Idempotency check** — run the app a second time:
   ```bash
   java -jar target/*.jar \
     --spring.datasource.url=jdbc:mysql://localhost:3306/analytics_dev?noDatetimeStringSync=true\&useSSL=false \
     --spring.datasource.username=tadmin \
     --spring.datasource.password=changeme \
     --app.csv.customer-path=/tmp/etl-input/ps_cust.csv \
     --app.csv.flavor-path=/tmp/etl-input/ps_flavor.csv
   ```

   Re-run the summary query — should still be exactly **4 rows** (not 8).

8. **Document results** — write `spring-boot-migration/VALIDATION.md`:

   Include:
   - Date and environment details
   - MySQL version used
   - All query results (pass/fail for each)
   - Idempotency result
   - Any issues encountered and how they were resolved
   - Overall status: PASS or FAIL

9. **Cleanup**:
   ```bash
   docker stop test-mysql && docker rm test-mysql
   ```

## Acceptance Criteria

- `customer` table: 15 rows
- `flavors` table: 6 rows
- `summary` table: exactly 4 rows with correct values
- `t_log_catcher_stats` table: at least 1 row
- Idempotency: summary still has 4 rows after second run
- `VALIDATION.md` documents all results
- All SQL validations pass

## Commit & PR

- Branch from `master`
- Commit message: `docs: add E2E validation results`
- Create a PR targeting `master`
