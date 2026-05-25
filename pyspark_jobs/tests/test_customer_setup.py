"""Tests for the j_usecase_customer_setup PySpark job.

Requires a running MySQL instance (use docker-compose up -d mysql).
"""

import os
import subprocess
import sys
import textwrap
import time

import mysql.connector
import pytest

MYSQL_HOST = os.environ.get("MYSQL_HOST", "localhost")
MYSQL_PORT = int(os.environ.get("MYSQL_PORT", "3306"))
MYSQL_DB = os.environ.get("MYSQL_DB", "analytics_dev")
MYSQL_USER = os.environ.get("MYSQL_USER", "tadmin")
MYSQL_PASSWORD = os.environ.get("MYSQL_PASSWORD", "tadmin123")

TEST_DATA_DIR = os.path.join(os.path.dirname(__file__), os.pardir, "test_data")
SCRIPT_PATH = os.path.join(
    os.path.dirname(__file__), os.pardir, "j_usecase_customer_setup.py"
)

JDBC_JAR = os.environ.get(
    "MYSQL_JDBC_JAR",
    os.path.join(os.path.dirname(__file__), os.pardir, "mysql-connector-j-8.0.33.jar"),
)


def _mysql_conn():
    return mysql.connector.connect(
        host=MYSQL_HOST,
        port=MYSQL_PORT,
        database=MYSQL_DB,
        user=MYSQL_USER,
        password=MYSQL_PASSWORD,
    )


def _wait_for_mysql(timeout=60):
    """Block until MySQL is accepting connections."""
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            conn = _mysql_conn()
            conn.close()
            return
        except mysql.connector.Error:
            time.sleep(1)
    pytest.fail("MySQL did not become available within timeout")


def _run_job(extra_args=None, expect_failure=False):
    """Run the PySpark job via spark-submit and return the completed process."""
    cmd = [
        sys.executable,
        "-m",
        "pyspark",
    ]
    # Use spark-submit if available, otherwise fall back to direct python
    spark_submit = os.environ.get("SPARK_SUBMIT", "spark-submit")
    cmd = [
        spark_submit,
        "--jars",
        JDBC_JAR,
        SCRIPT_PATH,
        "--env-context-file",
        os.path.join(TEST_DATA_DIR, "env_context_file.cfg"),
        "--mysql-host",
        MYSQL_HOST,
        "--mysql-port",
        str(MYSQL_PORT),
        "--mysql-db",
        MYSQL_DB,
        "--mysql-user",
        MYSQL_USER,
        "--mysql-password",
        MYSQL_PASSWORD,
    ]
    if extra_args:
        cmd.extend(extra_args)

    result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if not expect_failure and result.returncode != 0:
        print("STDOUT:", result.stdout)
        print("STDERR:", result.stderr)
    return result


def _drop_table(table_name):
    conn = _mysql_conn()
    cursor = conn.cursor()
    cursor.execute(f"DROP TABLE IF EXISTS {table_name}")
    conn.commit()
    cursor.close()
    conn.close()


def _table_row_count(table_name):
    conn = _mysql_conn()
    cursor = conn.cursor()
    cursor.execute(f"SELECT COUNT(*) FROM {table_name}")
    count = cursor.fetchone()[0]
    cursor.close()
    conn.close()
    return count


def _fetch_all_rows(table_name):
    conn = _mysql_conn()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(f"SELECT * FROM {table_name} ORDER BY id")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return rows


def _get_column_types(table_name):
    conn = _mysql_conn()
    cursor = conn.cursor()
    cursor.execute(
        "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH "
        "FROM INFORMATION_SCHEMA.COLUMNS "
        f"WHERE TABLE_SCHEMA='{MYSQL_DB}' AND TABLE_NAME='{table_name}'"
    )
    cols = {row[0]: (row[1], row[2]) for row in cursor.fetchall()}
    cursor.close()
    conn.close()
    return cols


@pytest.fixture(scope="session", autouse=True)
def ensure_mysql():
    _wait_for_mysql()


@pytest.fixture(autouse=True)
def cleanup_customer_table():
    _drop_table("customer")
    yield
    _drop_table("customer")


class TestHappyPath:
    def test_happy_path(self):
        result = _run_job(
            extra_args=[
                "--input-file",
                os.path.join(TEST_DATA_DIR, "ps_cust.csv"),
                "--target-table",
                "customer",
            ]
        )
        assert result.returncode == 0
        assert _table_row_count("customer") == 5

        rows = _fetch_all_rows("customer")
        assert rows[0]["first_name"] == "Alice"
        assert rows[0]["last_name"] == "Smith"
        assert rows[0]["city"] == "New York"
        assert rows[0]["state_cd"] == "NY"
        assert rows[0]["flavor"] == 1
        assert rows[4]["first_name"] == "Eve"


class TestEmptyFile:
    def test_empty_file_warning(self):
        result = _run_job(
            extra_args=[
                "--input-file",
                os.path.join(TEST_DATA_DIR, "ps_cust_empty.csv"),
                "--target-table",
                "customer",
            ]
        )
        assert result.returncode == 0
        assert "code: 42" in result.stderr or "code: 42" in result.stdout
        assert _table_row_count("customer") == 0


class TestOverwriteMode:
    def test_overwrite_mode(self):
        for _ in range(2):
            result = _run_job(
                extra_args=[
                    "--input-file",
                    os.path.join(TEST_DATA_DIR, "ps_cust.csv"),
                    "--target-table",
                    "customer",
                ]
            )
            assert result.returncode == 0
        assert _table_row_count("customer") == 5


class TestTrimFields:
    def test_trim_fields(self):
        trim_csv = os.path.join(TEST_DATA_DIR, "ps_cust_trim.csv")
        with open(trim_csv, "w") as f:
            f.write("id|first_name|last_name|city|state_cd|flavor\n")
            f.write("1|  Alice  |  Smith  |  New York  |  NY  |1\n")

        try:
            result = _run_job(
                extra_args=[
                    "--input-file",
                    trim_csv,
                    "--target-table",
                    "customer",
                ]
            )
            assert result.returncode == 0
            rows = _fetch_all_rows("customer")
            assert len(rows) == 1
            assert rows[0]["first_name"] == "Alice"
            assert rows[0]["last_name"] == "Smith"
            assert rows[0]["city"] == "New York"
            assert rows[0]["state_cd"] == "NY"
        finally:
            if os.path.exists(trim_csv):
                os.remove(trim_csv)


class TestSchemaTypes:
    def test_schema_types(self):
        result = _run_job(
            extra_args=[
                "--input-file",
                os.path.join(TEST_DATA_DIR, "ps_cust.csv"),
                "--target-table",
                "customer",
            ]
        )
        assert result.returncode == 0

        col_types = _get_column_types("customer")
        assert col_types["id"][0] == "int"
        assert col_types["flavor"][0] == "int"
        assert col_types["first_name"][0] == "varchar"
        assert col_types["first_name"][1] == 100
        assert col_types["last_name"][0] == "varchar"
        assert col_types["last_name"][1] == 100
        assert col_types["city"][0] == "varchar"
        assert col_types["city"][1] == 100
        assert col_types["state_cd"][0] == "varchar"
        assert col_types["state_cd"][1] == 100


class TestConnectionFailure:
    def test_connection_failure(self):
        result = _run_job(
            extra_args=[
                "--input-file",
                os.path.join(TEST_DATA_DIR, "ps_cust.csv"),
                "--target-table",
                "customer",
                "--mysql-password",
                "wrong_password_xyz",
                "--mysql-user",
                "nonexistent_user",
            ],
            expect_failure=True,
        )
        assert result.returncode == 4
