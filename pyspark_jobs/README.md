# j_usecase_flavor_setup — PySpark

PySpark conversion of the Talend `j_usecase_flavor_setup` ETL job. Reads a pipe-delimited CSV file containing flavor data (id and name), trims all string fields, and writes the result to a MySQL table using `DROP_IF_EXISTS_AND_CREATE` mode with batch inserts.

## Prerequisites

- Python 3.8+
- PySpark 3.4+
- MySQL 8.0 (local systemd service **or** Docker)
- MySQL JDBC driver (`mysql-connector-j-8.0.33.jar`)

## Quick Start

```bash
cd pyspark_jobs

# Install Python dependencies
pip install -r requirements.txt

# Start MySQL (uses systemd if available, else Docker)
# Then run the job:
bash run_local.sh
```

## CLI Arguments

| Argument | Default | Description |
|---|---|---|
| `--env-context-file` | `/root/use_case/env_context_file.cfg` | Path to equals-delimited env config file |
| `--input-file` | *(from context)* | Path to pipe-delimited input CSV |
| `--target-table` | *(from context)* | MySQL target table name |
| `--db-host` | *(from context)* | MySQL host |
| `--db-port` | *(from context)* | MySQL port |
| `--db-name` | *(from context)* | MySQL database name |
| `--db-user` | *(from context)* | MySQL username |
| `--db-password` | *(from context)* | MySQL password |

Context precedence (highest to lowest): CLI args > DB context (`context_entries`) > Env file > Talend defaults.

## Talend-to-PySpark Component Mapping

| Talend Component | PySpark Equivalent |
|---|---|
| `tPrejob` | Sequential function calls before main logic |
| `jblt_load_env_specific_context` | `load_env_context()` — reads key=value config file |
| `tMysqlConnection` + `SUBJOB_ERROR` → `tDie(4)` | `test_mysql_connection()` — pre-Spark connection test |
| `jblt_load_job_specific_context` | `load_job_context()` — SQL query on `context_entries` |
| `tFileInputDelimited` (pipe, CSV, ISO-8859-15) | `spark.read.csv(sep="\|", encoding="ISO-8859-1")` |
| `TRIMALL=true` | `trim(col(name))` for each StringType field |
| `tMysqlOutput` (DROP_IF_EXISTS_AND_CREATE, EXTENDINSERT) | `df.write.jdbc(mode="overwrite", batchsize=100)` |
| `RUN_IF` (NB_LINE == 0) → `tWarn(42)` | `if row_count == 0: logger.warning(...)` |
| `tDie(4)` — connection failure | `sys.exit(4)` |
| `tDie(8)` — context load failure | `sys.exit(8)` |
| `tPostjob` → `jblt_enable_logging` → `tMysqlClose` | `spark.stop()` in finally block |

## Known Differences

- **Encoding**: Talend uses `ISO-8859-15`; PySpark does not support it directly so `ISO-8859-1` is used as the closest equivalent.
- **tWarn message**: The original Talend job warns "The Customer input file does not have any records / is mepty" — the "Customer" reference and "mepty" misspelling are intentionally preserved.
- **`contect_value`**: The `context_entries` table column is intentionally misspelled in the original Talend SQL query; this is preserved.

## Running Tests

```bash
# Ensure MySQL is running and seeded
sudo mysql < init_db/01_seed.sql

# Download JDBC driver
wget -q https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar

# Run tests
pytest -v tests/
```

## File Structure

```
pyspark_jobs/
├── .gitignore
├── README.md
├── docker-compose.yml
├── init_db/
│   └── 01_seed.sql
├── j_usecase_flavor_setup.py
├── requirements.txt
├── run_local.sh
├── test_data/
│   ├── env_context_file.cfg
│   ├── ps_flavor.csv
│   ├── ps_flavor_empty.csv
│   └── ps_flavor_whitespace.csv
└── tests/
    ├── __init__.py
    └── test_j_usecase_flavor_setup.py
```
