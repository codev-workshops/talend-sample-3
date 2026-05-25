"""
Tests for j_usecase_flavor_setup PySpark job.

Requires:
  - MySQL running on localhost:3306 with analytics_dev database
  - context_entries table seeded (see init_db/01_seed.sql)
  - MySQL JDBC driver JAR available
"""

import os
import subprocess
import textwrap

import mysql.connector
import pytest

SCRIPT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPARK_SCRIPT = os.path.join(SCRIPT_DIR, "j_usecase_flavor_setup.py")
TEST_DATA_DIR = os.path.join(SCRIPT_DIR, "test_data")
JDBC_JAR = os.path.join(SCRIPT_DIR, "mysql-connector-j-8.0.33.jar")

DB_HOST = "localhost"
DB_PORT = 3306
DB_NAME = "analytics_dev"
DB_USER = "tadmin"
DB_PASSWORD = "tadmin123"
TARGET_TABLE = "flavors_test"

ENV_CONTEXT_FILE = os.path.join(TEST_DATA_DIR, "env_context_file.cfg")
SAMPLE_CSV = os.path.join(TEST_DATA_DIR, "ps_flavor.csv")
EMPTY_CSV = os.path.join(TEST_DATA_DIR, "ps_flavor_empty.csv")
WHITESPACE_CSV = os.path.join(TEST_DATA_DIR, "ps_flavor_whitespace.csv")

TIMEOUT = 120


def _get_connection():
    return mysql.connector.connect(
        host=DB_HOST, port=DB_PORT, database=DB_NAME,
        user=DB_USER, password=DB_PASSWORD,
    )


def _run_spark(extra_args=None):
    cmd = [
        "spark-submit", "--jars", JDBC_JAR, SPARK_SCRIPT,
        "--env-context-file", ENV_CONTEXT_FILE,
        "--target-table", TARGET_TABLE,
        "--db-host", DB_HOST,
        "--db-port", str(DB_PORT),
        "--db-name", DB_NAME,
        "--db-user", DB_USER,
        "--db-password", DB_PASSWORD,
    ]
    if extra_args:
        cmd.extend(extra_args)
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=TIMEOUT)
    return result


def _drop_table():
    try:
        conn = _get_connection()
        cursor = conn.cursor()
        cursor.execute(f"DROP TABLE IF EXISTS {TARGET_TABLE}")
        conn.commit()
        cursor.close()
        conn.close()
    except Exception:
        pass


def _query_table():
    conn = _get_connection()
    cursor = conn.cursor()
    cursor.execute(f"SELECT * FROM {TARGET_TABLE} ORDER BY flavor_id")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return rows


def _get_column_types():
    conn = _get_connection()
    cursor = conn.cursor()
    cursor.execute(
        "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH "
        "FROM INFORMATION_SCHEMA.COLUMNS "
        f"WHERE TABLE_SCHEMA='{DB_NAME}' AND TABLE_NAME='{TARGET_TABLE}' "
        "ORDER BY ORDINAL_POSITION"
    )
    cols = cursor.fetchall()
    cursor.close()
    conn.close()
    return cols


class TestHappyPath:
    def setup_method(self):
        _drop_table()

    def teardown_method(self):
        _drop_table()

    def test_loads_data_correctly(self):
        result = _run_spark(["--input-file", SAMPLE_CSV])
        output = result.stdout + result.stderr
        assert result.returncode == 0, f"Job failed:\n{output}"

        rows = _query_table()
        assert len(rows) == 6
        assert rows[0] == (1, "Vanilla")
        assert rows[5] == (6, "Rocky Road")
        assert "Successfully wrote 6 rows" in output


class TestEmptyFile:
    def setup_method(self):
        _drop_table()

    def teardown_method(self):
        _drop_table()

    def test_empty_file_warns_but_succeeds(self):
        result = _run_spark(["--input-file", EMPTY_CSV])
        output = result.stdout + result.stderr
        assert result.returncode == 0, f"Job failed:\n{output}"
        assert "The Customer input file does not have any records / is mepty" in output


class TestOverwriteMode:
    def setup_method(self):
        _drop_table()

    def teardown_method(self):
        _drop_table()

    def test_overwrite_does_not_double_data(self):
        _run_spark(["--input-file", SAMPLE_CSV])
        _run_spark(["--input-file", SAMPLE_CSV])
        rows = _query_table()
        assert len(rows) == 6


class TestTrimFields:
    def setup_method(self):
        _drop_table()

    def teardown_method(self):
        _drop_table()

    def test_trims_whitespace(self):
        result = _run_spark(["--input-file", WHITESPACE_CSV])
        output = result.stdout + result.stderr
        assert result.returncode == 0, f"Job failed:\n{output}"

        rows = _query_table()
        for row in rows:
            name = row[1]
            assert name == name.strip(), f"Field not trimmed: [{name}]"
        assert rows[0][1] == "Vanilla"
        assert rows[3][1] == "Mint Chip"


class TestSchemaTypes:
    def setup_method(self):
        _drop_table()

    def teardown_method(self):
        _drop_table()

    def test_column_types_match(self):
        _run_spark(["--input-file", SAMPLE_CSV])
        cols = _get_column_types()
        assert len(cols) == 2
        assert cols[0][0] == "flavor_id"
        assert cols[0][1] == "int"
        assert cols[1][0] == "flavor_name"
        assert cols[1][1] == "varchar"
        assert cols[1][2] == 100


class TestConnectionFailure:
    def test_bad_credentials_exit_code_4(self):
        result = _run_spark([
            "--input-file", SAMPLE_CSV,
            "--db-password", "wrong_password_xyz",
        ])
        assert result.returncode == 4, (
            f"Expected exit code 4, got {result.returncode}\n"
            f"{result.stdout + result.stderr}"
        )
        output = result.stdout + result.stderr
        assert "Unable to connect to mySQL Server instance." in output
