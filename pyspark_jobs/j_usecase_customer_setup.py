"""PySpark conversion of the Talend job j_usecase_customer_setup.

Reads a pipe-delimited CSV of customer data, trims string fields,
and writes the result to a MySQL table using JDBC.
"""

import argparse
import logging
import sys

import mysql.connector
from pyspark.sql import SparkSession
from pyspark.sql.functions import col, trim
from pyspark.sql.types import IntegerType, StringType, StructField, StructType

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger(__name__)

CUSTOMER_SCHEMA = StructType([
    StructField("id", IntegerType(), True),
    StructField("first_name", StringType(), True),
    StructField("last_name", StringType(), True),
    StructField("city", StringType(), True),
    StructField("state_cd", StringType(), True),
    StructField("flavor", IntegerType(), True),
])

ENV_CONTEXT_KEYS = [
    "mysql_connection_Server",
    "mysql_connection_Port",
    "mysql_connection_Database",
    "mysql_connection_Login",
    "mysql_connection_Password",
    "mysql_connection_AdditionalParams",
]

JOB_CONTEXT_QUERY = (
    "SELECT context_name AS `key`, contect_value AS `value` "
    "FROM analytics_dev.context_entries "
    "WHERE project='TALEND_AWS_DI_USECASE' AND job='j_usecase_customer_setup'"
)


def parse_args(argv=None):
    parser = argparse.ArgumentParser(
        description="PySpark conversion of Talend j_usecase_customer_setup"
    )
    parser.add_argument(
        "--env-context-file",
        default="/root/use_case/env_context_file.cfg",
        help="Path to the env context config file (key=value format)",
    )
    parser.add_argument("--input-file", default=None, help="Override cust_input_file")
    parser.add_argument("--target-table", default=None, help="Override target_table_name")
    parser.add_argument("--mysql-host", default=None, help="Override MySQL host")
    parser.add_argument("--mysql-port", default=None, help="Override MySQL port")
    parser.add_argument("--mysql-db", default=None, help="Override MySQL database")
    parser.add_argument("--mysql-user", default=None, help="Override MySQL user")
    parser.add_argument("--mysql-password", default=None, help="Override MySQL password")
    return parser.parse_args(argv)


def load_env_context(filepath):
    """Load equals-delimited key=value config file into a dict."""
    context = {}
    try:
        with open(filepath, "r") as fh:
            for line in fh:
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                if "=" not in line:
                    continue
                key, value = line.split("=", 1)
                context[key.strip()] = value.strip()
    except (OSError, IOError) as exc:
        logger.error("There was an error loading context variables from file: %s", exc)
        sys.exit(8)

    if not context:
        logger.error("There was an error loading context variables from file")
        sys.exit(8)

    return context


def load_job_context(mysql_host, mysql_port, mysql_db, mysql_user, mysql_password):
    """Connect to MySQL and load job-specific context entries."""
    try:
        conn = mysql.connector.connect(
            host=mysql_host,
            port=int(mysql_port),
            database=mysql_db,
            user=mysql_user,
            password=mysql_password,
        )
    except mysql.connector.Error as exc:
        logger.error("Unable to connect to mySQL Server instance: %s", exc)
        sys.exit(4)

    try:
        cursor = conn.cursor()
        cursor.execute(JOB_CONTEXT_QUERY)
        rows = cursor.fetchall()
        context = {row[0]: row[1] for row in rows}
        cursor.close()
        conn.close()
        return context
    except mysql.connector.Error as exc:
        logger.error(
            "There was an error loading context variables from mySQL table: %s", exc
        )
        sys.exit(8)


def run(argv=None):
    args = parse_args(argv)

    # 1. Load env context
    env_ctx = load_env_context(args.env_context_file)

    # 2. Resolve MySQL connection parameters (CLI overrides env context)
    mysql_host = args.mysql_host or env_ctx.get("mysql_connection_Server", "localhost")
    mysql_port = args.mysql_port or env_ctx.get("mysql_connection_Port", "3306")
    mysql_db = args.mysql_db or env_ctx.get("mysql_connection_Database", "analytics_dev")
    mysql_user = args.mysql_user or env_ctx.get("mysql_connection_Login", "tadmin")
    mysql_password = args.mysql_password or env_ctx.get(
        "mysql_connection_Password", "tadmin123"
    )

    # 3. Load job context from MySQL
    job_ctx = load_job_context(mysql_host, mysql_port, mysql_db, mysql_user, mysql_password)

    # 4. Resolve job-level parameters (CLI overrides job context)
    input_file = args.input_file or job_ctx.get("cust_input_file", "")
    target_table = args.target_table or job_ctx.get("target_table_name", "customer")

    if not input_file:
        logger.error("No input file specified (cust_input_file is empty)")
        sys.exit(1)

    logger.info("Input file: %s", input_file)
    logger.info("Target table: %s", target_table)

    # 5. Create SparkSession
    spark = SparkSession.builder.appName("j_usecase_customer_setup").getOrCreate()

    # 6. Read pipe-delimited CSV (DIE_ON_ERROR=true on input: let exceptions propagate)
    df = spark.read.csv(
        input_file,
        sep="|",
        header=True,
        schema=CUSTOMER_SCHEMA,
        encoding="ISO-8859-15",
    )

    # 7. Trim all string columns (TRIMALL=true in Talend)
    for field in CUSTOMER_SCHEMA.fields:
        if isinstance(field.dataType, StringType):
            df = df.withColumn(field.name, trim(col(field.name)))

    # 8. Warn if empty (tWarn code 42)
    row_count = df.count()
    if row_count == 0:
        logger.warning(
            "The Customer input file does not have any records / is empty (code: 42)"
        )

    logger.info("Row count: %d", row_count)

    # 9. Write to MySQL (DIE_ON_ERROR=false on output: log error but don't exit)
    jdbc_url = f"jdbc:mysql://{mysql_host}:{mysql_port}/{mysql_db}"
    try:
        df.write.format("jdbc").option("url", jdbc_url).option(
            "dbtable", target_table
        ).option("user", mysql_user).option("password", mysql_password).option(
            "driver", "com.mysql.cj.jdbc.Driver"
        ).option(
            "batchsize", 10000
        ).option(
            "truncate", "false"
        ).option(
            "createTableColumnTypes",
            "id INT, first_name VARCHAR(100), last_name VARCHAR(100), "
            "city VARCHAR(100), state_cd VARCHAR(100), flavor INT",
        ).mode(
            "overwrite"
        ).save()
        logger.info("Successfully wrote %d rows to %s", row_count, target_table)
    except Exception as exc:
        logger.error("Error writing to MySQL table %s: %s", target_table, exc)

    # 10. Stop Spark
    spark.stop()


if __name__ == "__main__":
    run()
