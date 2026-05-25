# AGENTS.md

## Repository Overview

This repository contains Talend Data Integration ETL jobs and their PySpark conversions for the TALEND_AWS_DI_USECASE project.

## Project Structure

- `TALEND_AWS_DI_USECASE/` — Original Talend job definitions (XML/XMI `.item` files)
- `seed_data/` — Sample seed data for ETL testing
- `pyspark_jobs/` — PySpark conversions of Talend jobs

## PySpark Jobs

### Build & Test

```bash
cd pyspark_jobs
pip install -r requirements.txt
pip install pytest

# Start local MySQL
docker-compose up -d mysql

# Run tests
pytest tests/ -v
```

### Lint

No specific linter is configured. Use standard Python best practices (PEP 8).

### Key Conventions

- PySpark jobs mirror Talend job behavior exactly (same error codes, same column types, same context loading)
- The column `contect_value` in `context_entries` is intentionally misspelled — it matches the original Talend schema
- CLI arguments always take precedence over context-loaded values
- Exit codes: 4 = MySQL connection failure, 8 = context loading failure, 42 = empty file warning (non-fatal)
