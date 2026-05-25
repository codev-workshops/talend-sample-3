"""
Integration tests for j_usecase_flavor_setup PySpark job.

Each test uses ``spark-submit`` with the MySQL JDBC driver to run the job
against a live local MySQL instance (analytics_dev database).

Prerequisites:
  - MySQL running on localhost:3306 with user tadmin / tadmin123
  - analytics_dev database and context_entries table seeded
  - MySQL JDBC connector JAR available (downloaded by run_local.sh)
"""

import os
import subprocess
import textwrap

import mysql.connector
import pytest

SCRIPT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JOB_SCRIPT = os.path.join(SCRIPT_DIR, "j_usecase_flavor_setup.py")
TEST_DATA_DIR = os.path.join(SCRIPT_DIR, "test_data")
ENV_CTX = os.path.join(TEST_DATA_DIR, "env_context_file.cfg")
SAMPLE_CSV = os.path.join(TEST_DATA_DIR, "ps_flavor.csv")
EMPTY_CSV = os.path.join(TEST_DATA_DIR, "ps_flavor_empty.csv")
WHITESPACE_CSV = os.path.join(TEST_DATA_DIR, "ps_flavor_whitespace.csv")

DB_HOST = "localhost"
DB_PORT = 3306
DB_NAME = "analytics_dev"
DB_USER = "tadmin"
DB_PASSWORD = "tadmin123"
TARGET_TABLE = "flavors"


def _find_jdbc_jar():
    """Locate the MySQL JDBC driver JAR in the pyspark_jobs directory."""
    for entry in os.listdir(SCRIPT_DIR):
        if entry.startswith("mysql-connector") and entry.endswith(".jar"):
            return os.path.join(SCRIPT_DIR, entry)
    return None


def _get_connection():
    return mysql.connector.connect(
        host=DB_HOST,
        port=DB_PORT,
        database=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD,
    )


def _drop_target_table():
    conn = _get_connection()
    cursor = conn.cursor()
    cursor.execute(f"DROP TABLE IF EXISTS {TARGET_TABLE}")
    conn.commit()
    cursor.close()
    conn.close()


def _run_job(input_file, extra_args=None):
    """Run the PySpark job via spark-submit and return the CompletedProcess."""
    jdbc_jar = _find_jdbc_jar()
    cmd = ["spark-submit"]
    if jdbc_jar:
        cmd += ["--jars", jdbc_jar]
    cmd += [
        JOB_SCRIPT,
        "--env-context-file", ENV_CTX,
        "--input-file", input_file,
        "--target-table", TARGET_TABLE,
    ]
    if extra_args:
        cmd.extend(extra_args)
    return subprocess.run(cmd, capture_output=True, text=True, timeout=120)


def _query_table(sql):
    conn = _get_connection()
    cursor = conn.cursor()
    cursor.execute(sql)
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return rows


class TestHappyPath:
    """Run with sample data and verify correct row count and values."""

    def setup_method(self):
        _drop_target_table()

    def teardown_method(self):
        _drop_target_table()

    def test_correct_row_count(self):
        result = _run_job(SAMPLE_CSV)
        assert result.returncode == 0, result.stderr
        rows = _query_table(f"SELECT COUNT(*) FROM {TARGET_TABLE}")
        assert rows[0][0] == 6

    def test_data_values(self):
        result = _run_job(SAMPLE_CSV)
        assert result.returncode == 0, result.stderr
        rows = _query_table(
            f"SELECT flavor_id, flavor_name FROM {TARGET_TABLE} ORDER BY flavor_id"
        )
        assert rows[0] == (1, "Vanilla")
        assert rows[3] == (4, "Mint Chip")
        assert rows[5] == (6, "Rocky Road")


class TestEmptyFile:
    """Run with an empty input file and verify the warning is emitted."""

    def setup_method(self):
        _drop_target_table()

    def teardown_method(self):
        _drop_target_table()

    def test_empty_file_warns(self):
        result = _run_job(EMPTY_CSV)
        combined = result.stdout + result.stderr
        assert "does not have any records / is mepty" in combined


class TestOverwriteMode:
    """Run twice and verify the table is not doubled (DROP_IF_EXISTS_AND_CREATE)."""

    def setup_method(self):
        _drop_target_table()

    def teardown_method(self):
        _drop_target_table()

    def test_no_duplicate_rows(self):
        _run_job(SAMPLE_CSV)
        _run_job(SAMPLE_CSV)
        rows = _query_table(f"SELECT COUNT(*) FROM {TARGET_TABLE}")
        assert rows[0][0] == 6


class TestTrimFields:
    """Run with whitespace-padded data and verify trimming."""

    def setup_method(self):
        _drop_target_table()

    def teardown_method(self):
        _drop_target_table()

    def test_trimmed_values(self):
        result = _run_job(WHITESPACE_CSV)
        assert result.returncode == 0, result.stderr
        rows = _query_table(
            f"SELECT flavor_name FROM {TARGET_TABLE} ORDER BY flavor_id"
        )
        for (name,) in rows:
            assert name == name.strip(), f"Value not trimmed: '{name}'"


class TestSchemaTypes:
    """Verify target table column types match the expected DDL."""

    def setup_method(self):
        _drop_target_table()

    def teardown_method(self):
        _drop_target_table()

    def test_column_types(self):
        _run_job(SAMPLE_CSV)
        rows = _query_table(
            f"SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
            f"WHERE TABLE_SCHEMA='{DB_NAME}' AND TABLE_NAME='{TARGET_TABLE}' "
            f"ORDER BY ORDINAL_POSITION"
        )
        type_map = {name: dtype for name, dtype in rows}
        assert type_map["flavor_id"] == "int"
        assert type_map["flavor_name"] == "varchar"


class TestConnectionFailure:
    """Run with wrong credentials and verify exit code 4 (tDie)."""

    def test_bad_credentials(self):
        result = _run_job(
            SAMPLE_CSV,
            extra_args=["--db-password", "WRONG_PASSWORD"],
        )
        assert result.returncode != 0
        combined = result.stdout + result.stderr
        assert (
            "Unable to connect to mySQL Server instance" in combined
            or result.returncode == 4
        )
