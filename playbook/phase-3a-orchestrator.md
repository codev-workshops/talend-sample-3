# Phase 3A — Orchestrator & Docker Deployment

## Prerequisites

**All Phase 1 sessions (1A, 1B, 1C, 1D) and Phase 2 must be complete and merged to `master`.**

The `spring-boot-migration/` directory must contain:
- `ContextService` (Phase 1A)
- `CsvLoaderService`, `CustomerSetupJob`, `FlavorSetupJob` (Phase 1B)
- `JobLogService` (Phase 1C)
- `StatusMailService` (Phase 1D)
- `AnalyticsSummaryJob` (Phase 2)
- All models, the `Job` interface, and configuration classes

## Repository

`codev-workshops/talend-sample-3` (branch: `master`)

## Context

The original Talend jobs follow a specific execution flow defined in `TALEND_AWS_DI_USECASE/process/j_usecase_customer_setup_0.1.item` lines 309–429:

```
tPrejob → load_env_context → open_mysql → load_job_context → [main logic] → tPostjob → send_status_mail → enable_logging → close_connection
```

Error codes from the original Talend project:
- **Code 4**: MySQL connection failure (`tDie` at line 341: `"Unable to connect to mySQL Server instance."`)
- **Code 8**: Context loading failure (error loading context variables)

## Task

### 1. `PipelineRunner.java` — `com.migration.etl`

Implements `CommandLineRunner` (Spring Boot):

```java
@Component
public class PipelineRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        // execution logic
    }
}
```

**Execution order**:

1. **Verify MySQL connectivity**: Attempt a simple query (e.g., `SELECT 1`). If it fails, throw a `RuntimeException` with a clear error message and code 4.
2. **Load context**: Call `ContextService.getJobContext()` for each job. If context loading fails, throw with a message containing `"error loading context variables"` and code 8.
3. **Run CustomerSetupJob**: `customerSetupJob.run()`
4. **Run FlavorSetupJob**: `flavorSetupJob.run()`
5. **Run AnalyticsSummaryJob**: `analyticsSummaryJob.run()`
6. **Log results**: Use `JobLogService` to log a completion entry
7. **Send email** (optional): Call `StatusMailService.sendStatusMail()` with a summary message

Inject all job beans and services via constructor injection.

**Error handling**:
- MySQL connection failure at startup → fail with clear error (code 4)
- Context load failure → throw with message containing `"error loading context variables"` (code 8)
- Individual job failures propagate as-is (unless the job itself swallows them, like CustomerSetupJob does)

### 2. `Dockerfile` — `spring-boot-migration/Dockerfile`

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/*.jar app.jar

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 3. `docker-compose.yml` — `spring-boot-migration/docker-compose.yml`

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: analytics_dev
      MYSQL_USER: tadmin
      MYSQL_PASSWORD: changeme
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/analytics_dev?noDatetimeStringSync=true&useSSL=false
      SPRING_DATASOURCE_USERNAME: tadmin
      SPRING_DATASOURCE_PASSWORD: changeme
    volumes:
      - ./src/test/resources:/data/input:ro
    # Override CSV paths to point to mounted volume
    # Use: -Dapp.csv.customer-path=/data/input/ps_cust.csv
    #       -Dapp.csv.flavor-path=/data/input/ps_flavor.csv

volumes:
  mysql-data:
```

### 4. Unit Test — `PipelineRunnerTest.java`

In `src/test/java/com/migration/etl/`:

- Mock all job beans and services
- **Test 1**: Verify execution order — CustomerSetupJob before FlavorSetupJob before AnalyticsSummaryJob
- **Test 2**: Verify that when the datasource is unreachable, the runner fails with a clear error message
- **Test 3**: Verify that when context loading throws, the error message contains `"error loading context variables"`

## Reference Files

- `TALEND_AWS_DI_USECASE/process/j_usecase_customer_setup_0.1.item`:
  - Lines 309–429: Job execution flow (tPrejob → ... → tPostjob)
  - Line 341: tDie for MySQL connection failure (code 4, message: "Unable to connect to mySQL Server instance.")
- `seed_data/env_context_file.cfg` — MySQL connection parameters for docker-compose

## Acceptance Criteria

- `mvn package` produces a runnable fat JAR (use `-DskipTests` if needed, since integration tests may require a real DB)
- `PipelineRunner` executes jobs in the correct order
- MySQL connection failure produces a clear error with code 4
- Context load failure includes message `"error loading context variables"` with code 8
- `Dockerfile` builds successfully
- `docker-compose.yml` is valid YAML and defines both services correctly
- `mvn test` passes (unit tests)

## Commit & PR

- Branch from `master`
- Commit message: `feat: implement PipelineRunner orchestrator with Docker deployment`
- Create a PR targeting `master`
