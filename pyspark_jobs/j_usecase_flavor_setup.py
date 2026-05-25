"""
PySpark conversion of Talend job j_usecase_flavor_setup.

Reads a pipe-delimited flavor CSV, trims all string fields, and writes
the data to a MySQL table using DROP_IF_EXISTS_AND_CREATE semantics.

Context loading mirrors the original Talend three-phase pattern:
  1. env_context_file.cfg  (key=value file)
  2. analytics_dev.context_entries table (DB-based overrides)
  3. CLI arguments (highest priority)
"""

import argparse
import logging
import sys

from pyspark.sql import SparkSession
from pyspark.sql.functions import col, trim
from pyspark.sql.types import (
    IntegerType,
    StringType,
    StructField,
    StructType,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Schema – matches the Talend tFileInputDelimited_1 metadata exactly
# ---------------------------------------------------------------------------
FLAVOR_SCHEMA = StructType(
    [
        StructField("flavor_id", IntegerType(), nullable=True),
        StructField("flavor_name", StringType(), nullable=True),
    ]
)

# Column DDL used when PySpark recreates the target table (overwrite mode).
# Matches the original Talend tMysqlOutput column types.
COLUMN_TYPES = "flavor_id INT, flavor_name VARCHAR(100)"

# Project / job identifiers used by the job-specific context query.
PROJECT_NAME = "TALEND_AWS_DI_USECASE"
JOB_NAME = "j_usecase_flavor_setup"


# ---------------------------------------------------------------------------
# Context-loading helpers (replicate Talend joblet behaviour)
# ---------------------------------------------------------------------------
def load_env_context(filepath):
    """Read an equals-delimited key=value config file into a dict.

    Mirrors jblt_load_env_specific_context: if the file cannot be read or
    contains zero lines, the job dies with exit-code 8.
    """
    context = {}
    try:
        with open(filepath, encoding="iso-8859-1") as fh:
            for line in fh:
                line = line.strip()
                if not line or "=" not in line:
                    continue
                key, value = line.split("=", 1)
                context[key] = value
    except Exception:
        logger.error("There was an error loading context variables form file")
        sys.exit(8)

    if not context:
        logger.error("There was an error loading context variables form file")
        sys.exit(8)

    return context


def load_job_context(db_host, db_port, db_name, db_user, db_password):
    """Load job-specific context rows from analytics_dev.context_entries.

    The SQL intentionally uses the misspelled column ``contect_value``
    to match the original Talend query.
    """
    import mysql.connector

    query = (
        "SELECT context_name AS `key`, contect_value AS `value` "
        "FROM analytics_dev.context_entries "
        f"WHERE project='{PROJECT_NAME}' AND job='{JOB_NAME}'"
    )
    context = {}
    try:
        conn = mysql.connector.connect(
            host=db_host,
            port=int(db_port),
            database=db_name,
            user=db_user,
            password=db_password,
        )
        cursor = conn.cursor()
        cursor.execute(query)
        for key, value in cursor.fetchall():
            context[key] = value
        cursor.close()
        conn.close()
    except Exception:
        logger.error(
            "There was an error loading context variables form mySQL table"
        )
        sys.exit(8)

    return context


# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------
def parse_args(argv=None):
    parser = argparse.ArgumentParser(
        description="PySpark conversion of Talend j_usecase_flavor_setup"
    )
    parser.add_argument(
        "--env-context-file",
        default="/root/use_case/env_context_file.cfg",
        help="Path to the equals-delimited env context file",
    )
    parser.add_argument(
        "--input-file",
        default=None,
        help="Path to the pipe-delimited flavor CSV (overrides DB context)",
    )
    parser.add_argument(
        "--target-table",
        default=None,
        help="MySQL target table name (overrides DB context)",
    )
    parser.add_argument("--db-host", default=None, help="MySQL host override")
    parser.add_argument("--db-port", default=None, help="MySQL port override")
    parser.add_argument(
        "--db-name", default=None, help="MySQL database override"
    )
    parser.add_argument("--db-user", default=None, help="MySQL user override")
    parser.add_argument(
        "--db-password", default=None, help="MySQL password override"
    )
    return parser.parse_args(argv)


# ---------------------------------------------------------------------------
# Main entry point
# ---------------------------------------------------------------------------
def run(argv=None):
    args = parse_args(argv)

    # ---- Phase 1: env context (from file) --------------------------------
    ctx = load_env_context(args.env_context_file)

    db_host = args.db_host or ctx.get("mysql_connection_Server", "localhost")
    db_port = args.db_port or ctx.get("mysql_connection_Port", "3306")
    db_name = args.db_name or ctx.get("mysql_connection_Database", "analytics_dev")
    db_user = args.db_user or ctx.get("mysql_connection_Login", "tadmin")
    db_password = args.db_password or ctx.get("mysql_connection_Password", "")

    # ---- Phase 2: MySQL connection test ----------------------------------
    logger.info("Connecting to MySQL %s:%s/%s as %s", db_host, db_port, db_name, db_user)
    try:
        import mysql.connector

        test_conn = mysql.connector.connect(
            host=db_host,
            port=int(db_port),
            database=db_name,
            user=db_user,
            password=db_password,
        )
        test_conn.close()
    except Exception:
        logger.error("Unable to connect to mySQL Server instance.")
        sys.exit(4)

    # ---- Phase 3: job-specific context (from DB) -------------------------
    job_ctx = load_job_context(db_host, db_port, db_name, db_user, db_password)
    ctx.update(job_ctx)

    # CLI overrides have the highest priority
    flavor_input_file = args.input_file or ctx.get("flavor_input_file", "")
    target_table_name = args.target_table or ctx.get("target_table_name", "")

    if not flavor_input_file:
        logger.error("No input file specified (flavor_input_file is empty)")
        sys.exit(1)
    if not target_table_name:
        logger.error("No target table specified (target_table_name is empty)")
        sys.exit(1)

    logger.info("Input file : %s", flavor_input_file)
    logger.info("Target table: %s.%s", db_name, target_table_name)

    # ---- Phase 4: Spark read ---------------------------------------------
    spark = SparkSession.builder.appName(JOB_NAME).getOrCreate()

    # ISO-8859-15 is not supported by PySpark; use ISO-8859-1 as the closest
    # equivalent, per the playbook guidance.
    df = spark.read.csv(
        flavor_input_file,
        sep="|",
        header=True,
        schema=FLAVOR_SCHEMA,
        encoding="ISO-8859-1",
    )

    row_count = df.count()
    logger.info("Rows read: %d", row_count)

    # Replicate tWarn_1: warn (code 42) when the file is empty.
    # The original message deliberately says "Customer" and "mepty".
    if row_count == 0:
        logger.warning(
            "The Customer input file does not have any records / is mepty"
        )
        # Talend tWarn does not abort the job – processing continues (the
        # table will simply be recreated empty).

    # ---- Phase 5: Trim all string fields (TRIMALL=true) ------------------
    for field in FLAVOR_SCHEMA.fields:
        if isinstance(field.dataType, StringType):
            df = df.withColumn(field.name, trim(col(field.name)))

    # ---- Phase 6: Write to MySQL via JDBC --------------------------------
    jdbc_url = (
        f"jdbc:mysql://{db_host}:{db_port}/{db_name}"
        f"?noDatetimeStringSync=true&useSSL=false"
    )

    df.write.format("jdbc").options(
        url=jdbc_url,
        dbtable=target_table_name,
        user=db_user,
        password=db_password,
        driver="com.mysql.cj.jdbc.Driver",
        batchsize="100",
        createTableColumnTypes=COLUMN_TYPES,
    ).mode("overwrite").save()

    logger.info(
        "Successfully wrote %d rows to %s.%s",
        row_count,
        db_name,
        target_table_name,
    )

    spark.stop()

    # ---- Postjob: logging placeholder (jblt_enable_logging) --------------
    logger.info("Job %s completed successfully.", JOB_NAME)


if __name__ == "__main__":
    run()
