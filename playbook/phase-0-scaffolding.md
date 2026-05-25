# Phase 0 — Project Scaffolding

## Prerequisites

None. This is the first session.

## Repository

`codev-workshops/talend-sample-3` (branch: `master`)

## Context

This repository contains a Talend Data Integration 7.2.1 ETL project (`TALEND_AWS_DI_USECASE/`) that ingests CSV files into MySQL and produces analytics summaries. We are migrating it to Spring Boot 3.x with Java 17. This session creates the Maven project skeleton that all subsequent sessions will build upon.

## Task

Create a `spring-boot-migration/` directory at the repository root containing a fully compilable Spring Boot 3.x project.

### 1. `pom.xml`

Create a Maven POM with:

- **Parent**: `org.springframework.boot:spring-boot-starter-parent:3.2.0`
- **Java version**: 17
- **Group ID**: `com.migration`
- **Artifact ID**: `etl-migration`
- **Dependencies**:
  - `spring-boot-starter-jdbc`
  - `spring-boot-starter-mail`
  - `mysql-connector-j` (runtime scope)
  - `com.h2database:h2` (test scope)
  - `spring-boot-starter-test` (test scope)
  - `lombok` (optional, provided scope)
- **Build plugin**: `spring-boot-maven-plugin`

### 2. Package Structure

Create the following package hierarchy under `src/main/java/com/migration/etl/`:

```
com.migration.etl
├── EtlApplication.java          (main class with @SpringBootApplication)
├── config/                       (empty package — Phase 1A will add ContextService here)
├── job/                          (empty package — Phase 1B/2 will add job implementations)
│   └── Job.java                  (interface with single method: void run())
├── model/                        (empty package — Phase 1B/1C will add model classes)
└── service/                      (empty package — Phase 1B/1C/1D will add services)
```

#### `EtlApplication.java`

```java
package com.migration.etl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EtlApplication {
    public static void main(String[] args) {
        SpringApplication.run(EtlApplication.class, args);
    }
}
```

#### `Job.java` (in `com.migration.etl.job`)

```java
package com.migration.etl.job;

public interface Job {
    void run();
}
```

### 3. `src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/analytics_dev?noDatetimeStringSync=true&useSSL=false
    username: tadmin
    password: changeme
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: always

app:
  job:
    project: TALEND_AWS_DI_USECASE
  email:
    enabled: false
```

These values are derived from `seed_data/env_context_file.cfg`.

### 4. `src/main/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  sql:
    init:
      mode: always
```

### 5. `src/main/resources/schema.sql`

Create the initial schema with two tables used by the framework:

```sql
CREATE TABLE IF NOT EXISTS context_entries (
    context_name VARCHAR(100),
    contect_value VARCHAR(100),
    project VARCHAR(100),
    job VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS t_log_catcher_stats (
    moment DATETIME,
    pid VARCHAR(20),
    root_pid VARCHAR(20),
    father_pid VARCHAR(20),
    project VARCHAR(50),
    job VARCHAR(255),
    context VARCHAR(50),
    priority INT,
    type VARCHAR(255),
    origin VARCHAR(255),
    message VARCHAR(255),
    code INT
);
```

The `context_entries` schema comes from `seed_data/seed_context_entries.sql`. The `t_log_catcher_stats` schema is derived from `TALEND_AWS_DI_USECASE/joblets/jblt_enable_logging_0.1.item` lines 90–102 (tLogCatcher metadata columns).

> **Note**: The column `contect_value` is intentionally misspelled — it matches the original Talend DB column name.

### 6. `src/main/resources/data.sql`

Copy the contents of `seed_data/seed_context_entries.sql` but **remove** the `analytics_dev.` schema prefix from table references (Spring Boot uses the datasource's default schema). Specifically:

- Change `analytics_dev.context_entries` → `context_entries`
- Keep all INSERT statements as-is

### 7. Test Resources

Copy these files from `seed_data/` to `src/test/resources/`:

- `ps_cust.csv`
- `ps_cust_empty.csv`
- `ps_flavor.csv`
- `ps_flavor_empty.csv`

### 8. `.gitignore`

Create a `.gitignore` inside `spring-boot-migration/`:

```
target/
*.class
*.jar
*.log
.idea/
*.iml
.settings/
.project
.classpath
```

## Acceptance Criteria

- `cd spring-boot-migration && mvn compile` succeeds without errors
- Project structure matches the layout described above
- All resource files are in place
- `Job` interface exists in `com.migration.etl.job` with a `void run()` method
- `EtlApplication.java` has `@SpringBootApplication` annotation and valid `main` method

## Commit & PR

- Branch from `master`
- Commit message: `feat: scaffold Spring Boot migration project`
- Create a PR targeting `master`
