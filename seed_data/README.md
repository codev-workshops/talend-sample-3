# Seed Data for Talend AWS DI Use Case

This directory contains seed data files that match the schemas and formats expected by the three Talend ETL jobs in `TALEND_AWS_DI_USECASE`.

## Files

| File | Format | Purpose |
|------|--------|---------|
| `ps_flavor.csv` | Pipe-delimited, ISO-8859-15 | Flavor reference data (6 rows) |
| `ps_cust.csv` | Pipe-delimited, ISO-8859-15 | Customer data (15 rows) with FK to flavors |
| `ps_cust_empty.csv` | Pipe-delimited, ISO-8859-15 | Empty customer file (header only) for tWarn zero-rows testing |
| `ps_flavor_empty.csv` | Pipe-delimited, ISO-8859-15 | Empty flavor file (header only) for tWarn zero-rows testing |
| `env_context_file.cfg` | Key=value (equals-delimited) | Environment-level MySQL connection parameters |
| `seed_context_entries.sql` | SQL | Creates and populates the `context_entries` table with job-specific context rows |

## File Placement

On the Talend runtime host, copy the CSV data files to:

```
/root/use_case/ps_flavor.csv
/root/use_case/ps_cust.csv
```

These paths match the `cust_input_file` and `flavor_input_file` context entries in the SQL seed script.

## Running the SQL Seed Script

Connect to your MySQL instance and execute:

```bash
mysql -u tadmin -p analytics_dev < seed_data/seed_context_entries.sql
```

This will:
1. Create the `context_entries` table if it does not exist.
2. Delete any existing rows for the `TALEND_AWS_DI_USECASE` project.
3. Insert context rows for all three jobs: `j_usecase_customer_setup`, `j_usecase_flavor_setup`, and `j_usecase_analytics_summary`.

> **Note:** The column `contect_value` is intentionally misspelled to match the existing query in `jblt_load_job_specific_context`.

## Environment Context File

`env_context_file.cfg` provides MySQL connection parameters. Update the placeholder values (`localhost`, `tadmin`, `changeme`) to match your environment before running the jobs.

## Expected Analytics Summary Output

After running all three jobs in sequence (`customer_setup` → `flavor_setup` → `analytics_summary`), the `summary` table should contain exactly 4 rows:

| count_flavor_id | flavor_name | state_cd |
|-----------------|-------------|----------|
| 2 | Vanilla | NY |
| 3 | Chocolate | CA |
| 2 | Mint Chip | TX |
| 2 | Chocolate | FL |

Only state/flavor combinations with more than 1 customer pass the analytics filter.

## Edge Cases Covered

- **Unreferenced flavor**: Rocky Road (`flavor_id=6`) has zero customers and is dropped by the inner join in the analytics job.
- **Single-customer state/flavor pairs**: The following combinations have only 1 customer and are filtered out by the `count > 1` condition:
  - (IL, Strawberry) — Frank Miller
  - (IL, Vanilla) — Grace Lee
  - (CO, Strawberry) — Noah Thomas
  - (OR, Vanilla) — Olivia Garcia
  - (FL, Cookie Dough) — Karen White
  - (TX, Strawberry) — Jack Taylor
- **Zero-rows path**: `ps_cust_empty.csv` and `ps_flavor_empty.csv` contain headers only, for testing the tWarn component behavior when input files have no data rows.
