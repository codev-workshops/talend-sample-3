"""
PySpark conversion of Talend job j_usecase_flavor_setup.

Reads a pipe-delimited CSV of flavor data, trims all string fields,
and writes the result to a MySQL table using DROP_IF_EXISTS_AND_CREATE.
"""

import argparse
import logging
import os
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

FLAVOR_SCHEMA = StructType(
    [
        StructField("flavor_id", IntegerType(), True),
        StructField("flavor_name", StringType(), True),
    ]
)

COLUMN_TYPES = "flavor_id INT, flavor_name VARCHAR(100)"

PROJECT_NAME = "TALEND_AWS_DI_USECASE"
JOB_NAME = "j_usecase_flavor_setup"


def load_env_context(filepath):
    """Load key=value config file into a dict (jblt_load_env_specific_context)."""
    ctx = {}
    if not os.path.isfile(filepath):
        logger.error("There was an error loading context variables form file")
        sys.exit(8)
    with open(filepath, encoding="iso-8859-1") as fh:
        for line in fh:
            line = line.strip()
            if not line or "=" not in line:
                continue
            key, value = line.split("=", 1)
            ctx[key] = value
    if not ctx:
        logger.error("There was an error loading context variables form file")
        sys.exit(8)
    return ctx


def load_job_context(db_host, db_port, db_name, db_user, db_password):
    """Load job-specific context from the context_entries table (jblt_load_job_specific_context)."""
    import mysql.connector

    query = (
        "SELECT context_name AS `key`, contect_value AS `value` "
        "FROM analytics_dev.context_entries "
        f"WHERE project='{PROJECT_NAME}' AND job='{JOB_NAME}'"
    )
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
        ctx = {row[0]: row[1] for row in cursor.fetchall()}
        cursor.close()
        conn.close()
        return ctx
    except Exception:
        logger.error("There was an error loading context variables form mySQL table")
        sys.exit(8)


def test_mysql_connection(db_host, db_port, db_name, db_user, db_password):
    """Pre-Spark connection test (tMysqlConnection with SUBJOB_ERROR -> tDie code 4)."""
    import mysql.connector

    try:
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


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description="PySpark: j_usecase_flavor_setup")
    parser.add_argument("--env-context-file", default="/root/use_case/env_context_file.cfg")
    parser.add_argument("--input-file", default=None)
    parser.add_argument("--target-table", default=None)
    parser.add_argument("--db-host", default=None)
    parser.add_argument("--db-port", default=None)
    parser.add_argument("--db-name", default=None)
    parser.add_argument("--db-user", default=None)
    parser.add_argument("--db-password", default=None)
    return parser.parse_args(argv)


def run(argv=None):
    args = parse_args(argv)

    # --- Phase 1: Load env-specific context from file ---
    logger.info("Loading env-specific context from: %s", args.env_context_file)
    env_ctx = load_env_context(args.env_context_file)

    # Defaults from Talend context parameters
    ctx = {
        "mysql_connection_Server": "18.224.55.71",
        "mysql_connection_Port": "3306",
        "mysql_connection_Database": "analytics_dev",
        "mysql_connection_Login": "tadmin",
        "mysql_connection_Password": "",
        "mysql_connection_AdditionalParams": "noDatetimeStringSync=true&useSSL=false",
        "target_table_name": "",
        "flavor_input_file": "",
    }

    # Apply env context overrides
    ctx.update(env_ctx)

    # --- Phase 2: Pre-Spark connection test ---
    db_host = args.db_host or ctx["mysql_connection_Server"]
    db_port = args.db_port or ctx["mysql_connection_Port"]
    db_name = args.db_name or ctx["mysql_connection_Database"]
    db_user = args.db_user or ctx["mysql_connection_Login"]
    db_password = args.db_password or ctx["mysql_connection_Password"]

    logger.info("Testing MySQL connection to %s:%s/%s", db_host, db_port, db_name)
    test_mysql_connection(db_host, db_port, db_name, db_user, db_password)
    logger.info("MySQL connection test passed.")

    # --- Phase 3: Load job-specific context from DB ---
    logger.info("Loading job-specific context from context_entries table.")
    job_ctx = load_job_context(db_host, db_port, db_name, db_user, db_password)
    ctx.update(job_ctx)

    # --- Phase 4: Apply CLI overrides (highest precedence) ---
    input_file = args.input_file or ctx.get("flavor_input_file", "")
    target_table = args.target_table or ctx.get("target_table_name", "")

    if not input_file:
        logger.error("No input file specified.")
        sys.exit(1)
    if not target_table:
        logger.error("No target table specified.")
        sys.exit(1)

    logger.info("Input file: %s", input_file)
    logger.info("Target table: %s", target_table)

    # --- Phase 5: Spark session and main processing ---
    jdbc_url = f"jdbc:mysql://{db_host}:{db_port}/{db_name}?{ctx['mysql_connection_AdditionalParams']}"

    spark = SparkSession.builder.appName("j_usecase_flavor_setup").getOrCreate()
    try:
        df = (
            spark.read.format("csv")
            .option("sep", "|")
            .option("header", "true")
            .option("encoding", "ISO-8859-1")
            .schema(FLAVOR_SCHEMA)
            .load(input_file)
        )

        row_count = df.count()
        logger.info("Rows read: %d", row_count)

        if row_count == 0:
            logger.warning("The Customer input file does not have any records / is mepty")

        # TRIMALL: trim all StringType fields
        for field in FLAVOR_SCHEMA.fields:
            if isinstance(field.dataType, StringType):
                df = df.withColumn(field.name, trim(col(field.name)))

        # Write to MySQL (DROP_IF_EXISTS_AND_CREATE = overwrite, EXTENDINSERT batchsize=100)
        (
            df.write.format("jdbc")
            .option("url", jdbc_url)
            .option("dbtable", target_table)
            .option("user", db_user)
            .option("password", db_password)
            .option("driver", "com.mysql.cj.jdbc.Driver")
            .option("createTableColumnTypes", COLUMN_TYPES)
            .option("batchsize", 100)
            .mode("overwrite")
            .save()
        )

        logger.info("Successfully wrote %d rows to table '%s'.", row_count, target_table)
    finally:
        spark.stop()
        logger.info("Spark session stopped.")


if __name__ == "__main__":
    run()
