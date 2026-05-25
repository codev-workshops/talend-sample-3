# PySpark Conversion: j_usecase_customer_setup

PySpark conversion of the Talend `j_usecase_customer_setup` ETL job from the **TALEND_AWS_DI_USECASE** project.

## What This Does

Reads a pipe-delimited CSV file containing customer data, trims all string fields, and loads the result into a MySQL table using JDBC. The job replicates the original Talend behavior including:

- Multi-phase context loading (environment file + MySQL-based job context)
- `DROP_IF_EXISTS_AND_CREATE` table strategy (JDBC overwrite mode)
- Empty-file warning (code 42)
- Connection failure exit (code 4)
- Context loading failure exit (code 8)

## Prerequisites

- **Python 3.8+**
- **PySpark >= 3.4** (`pip install pyspark`)
- **Docker & Docker Compose** (for local MySQL)
- **MySQL JDBC Driver** (`mysql-connector-j-8.0.33.jar` — auto-downloaded by `run_local.sh`)

## Quick Start (Local with Docker Compose)

```bash
cd pyspark_jobs

# Install Python dependencies
pip install -r requirements.txt

# Start MySQL and run the job
chmod +x run_local.sh
./run_local.sh
```

## Running Tests

```bash
cd pyspark_jobs

# Start MySQL
docker-compose up -d mysql

# Wait for MySQL (optional, tests wait automatically)
sleep 15

# Download JDBC driver if needed
wget -nc https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar || true

# Run tests
pip install pytest
pytest tests/ -v
```

## CLI Arguments

| Argument | Default | Description |
|---|---|---|
| `--env-context-file` | `/root/use_case/env_context_file.cfg` | Path to env context config |
| `--input-file` | From job context | Override input CSV path |
| `--target-table` | From job context | Override target MySQL table |
| `--mysql-host` | From env context | Override MySQL host |
| `--mysql-port` | From env context | Override MySQL port |
| `--mysql-db` | From env context | Override MySQL database |
| `--mysql-user` | From env context | Override MySQL user |
| `--mysql-password` | From env context | Override MySQL password |

## Talend-to-PySpark Component Mapping

| Talend Component | PySpark Equivalent |
|---|---|
| `tFileInputDelimited` (pipe CSV) | `spark.read.csv(sep="\|", header=True, schema=...)` |
| `tMap` with TRIMALL | `df.withColumn(name, trim(col(name)))` for each StringType |
| `tMysqlOutput` (DROP_IF_EXISTS_AND_CREATE) | `df.write.mode("overwrite").format("jdbc")` |
| `tMysqlOutput` (EXTENDINSERT, BATCH_SIZE=10000) | `.option("batchsize", 10000)` |
| `tContextLoad` (from file) | `load_env_context()` — reads key=value file |
| `tContextLoad` (from MySQL) | `load_job_context()` — runs SQL query via mysql-connector |
| `tWarn` (code 42, empty file) | `logging.warning(...)` |
| `tDie` (code 4, connection failure) | `sys.exit(4)` |
| `tDie` (code 8, context load failure) | `sys.exit(8)` |
| `DIE_ON_ERROR=false` on output | `try/except` around JDBC write |
| `DIE_ON_ERROR=true` on input | Exception propagates (job fails) |

## Differences / Limitations vs. Original Talend Job

1. **Encoding**: PySpark reads CSV with `encoding="ISO-8859-15"` matching Talend, but edge-case encoding differences may exist.
2. **Batch Inserts**: Talend uses `NB_ROWS_PER_INSERT=100` with `EXTENDINSERT=true`. PySpark JDBC uses `batchsize=10000` which controls commit batching, not multi-row INSERT syntax.
3. **Table Creation**: Talend generates exact DDL. PySpark `overwrite` mode creates the table using `createTableColumnTypes` to match the original schema.
4. **MySQL Connector**: The original Talend job uses the built-in Talend MySQL connector. This PySpark version uses the official MySQL JDBC driver (`mysql-connector-j`).
5. **Joblet Logging**: The original `jblt_enable_logging` joblet writes execution stats to a MySQL table. This is not replicated in the PySpark version.

## File Structure

```
pyspark_jobs/
├── j_usecase_customer_setup.py   # Main PySpark job
├── requirements.txt              # Python dependencies
├── docker-compose.yml            # Local MySQL for testing
├── run_local.sh                  # One-command local run
├── README.md                     # This file
├── init_db/
│   └── 01_seed.sql               # MySQL seed data (context_entries)
├── test_data/
│   ├── ps_cust.csv               # Sample customer data (5 rows)
│   ├── ps_cust_empty.csv         # Empty CSV (header only)
│   └── env_context_file.cfg      # Local env context config
└── tests/
    └── test_customer_setup.py    # pytest test suite
```
