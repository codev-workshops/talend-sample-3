# j_usecase_flavor_setup — PySpark Conversion

PySpark port of the Talend `j_usecase_flavor_setup` ETL job. The job reads a
pipe-delimited flavor CSV file, trims all string columns, and writes the data
to a MySQL table using **DROP_IF_EXISTS_AND_CREATE** semantics. Context
variables are loaded in three phases: env config file, database
(`context_entries`), and CLI overrides.

## Prerequisites

| Tool | Version |
|------|---------|
| Python | 3.9+ |
| PySpark | 3.4+ |
| Docker / Docker Compose | (for local MySQL) |
| MySQL JDBC driver | auto-downloaded by `run_local.sh` |

## Quick Start

```bash
cd pyspark_jobs
pip install -r requirements.txt

# Option A: use local MySQL (blueprint already provisions it)
bash run_local.sh

# Option B: use Docker
docker-compose up -d mysql
# wait for healthy, then:
bash run_local.sh
```

## CLI Arguments

| Argument | Default | Description |
|----------|---------|-------------|
| `--env-context-file` | `/root/use_case/env_context_file.cfg` | Path to the equals-delimited env config file |
| `--input-file` | *(from DB context)* | Path to the pipe-delimited flavor CSV |
| `--target-table` | *(from DB context)* | MySQL target table name |
| `--db-host` | *(from env context)* | MySQL host override |
| `--db-port` | *(from env context)* | MySQL port override |
| `--db-name` | *(from env context)* | MySQL database override |
| `--db-user` | *(from env context)* | MySQL user override |
| `--db-password` | *(from env context)* | MySQL password override |

## Talend-to-PySpark Component Mapping

| Talend Component | PySpark Equivalent |
|---|---|
| `tPrejob` | Sequential function calls before main logic |
| `jblt_load_env_specific_context` | `load_env_context()` — reads `key=value` file |
| `tMysqlConnection` | `mysql.connector.connect()` test; JDBC URL for Spark |
| `jblt_load_job_specific_context` | `load_job_context()` — SQL query via `mysql.connector` |
| `tFileInputDelimited` (pipe, TRIMALL) | `spark.read.csv(sep="\|", header=True)` + `trim()` |
| `tMysqlOutput` (DROP_IF_EXISTS_AND_CREATE, EXTENDINSERT) | `df.write.format("jdbc").mode("overwrite")` with `batchsize=100` |
| `tWarn` (code 42) | `logger.warning(...)` |
| `tDie` (code 4) | `sys.exit(4)` |
| `tDie` (code 8) | `sys.exit(8)` |
| `tMysqlClose` | `spark.stop()` |
| `jblt_enable_logging` | `logger.info(...)` |

## Running Tests

```bash
cd pyspark_jobs

# Ensure MySQL is running and seeded
# (the blueprint or docker-compose handles this)

# Download JDBC driver if not present
[ -f mysql-connector-j-8.0.33.jar ] || \
  wget -q https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar

pytest tests/ -v
```

## Known Differences vs. Original Talend Job

1. **Encoding**: Talend uses `ISO-8859-15`; PySpark does not support it
   natively, so `ISO-8859-1` is used as the closest equivalent.
2. **Preserved quirks**: The `tWarn` message says "Customer" (not "Flavor")
   and "mepty" (not "empty") — intentionally matching the original Talend job.
3. **Context query**: The SQL uses `contect_value` (typo preserved from the
   original `context_entries` DDL).
4. **Logging joblet**: `jblt_enable_logging` is a logging-setup joblet in
   Talend. In PySpark it is a no-op placeholder (Python `logging` is
   configured at startup).

## File Structure

```
pyspark_jobs/
  j_usecase_flavor_setup.py   # Main PySpark job script
  requirements.txt             # Python dependencies
  docker-compose.yml           # Local MySQL for testing
  run_local.sh                 # One-command local execution
  .gitignore                   # Ignore JARs, caches, etc.
  init_db/
    01_seed.sql                # Context entries seed data
  test_data/
    ps_flavor.csv              # Sample 6-row input
    ps_flavor_empty.csv        # Header-only input
    ps_flavor_whitespace.csv   # Whitespace-padded input
    env_context_file.cfg       # Local env context config
  tests/
    __init__.py
    test_j_usecase_flavor_setup.py  # pytest integration tests
```
