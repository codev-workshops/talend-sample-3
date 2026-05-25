#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

JDBC_JAR="mysql-connector-j-8.0.33.jar"
JDBC_URL="https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/${JDBC_JAR}"

# ---- Start MySQL (use local service or Docker) --------------------------
if command -v systemctl &>/dev/null && systemctl is-active --quiet mysql 2>/dev/null; then
  echo "MySQL is already running via systemd."
elif command -v docker-compose &>/dev/null || command -v docker &>/dev/null; then
  echo "Starting MySQL via docker-compose ..."
  docker-compose up -d mysql

  echo "Waiting for MySQL to become healthy ..."
  for i in $(seq 1 30); do
    if docker-compose exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
      echo "MySQL is ready."
      break
    fi
    sleep 2
  done
else
  echo "ERROR: Neither systemd MySQL nor Docker is available."
  exit 1
fi

# ---- Download JDBC driver if not present ---------------------------------
if [ ! -f "$JDBC_JAR" ]; then
  echo "Downloading MySQL JDBC driver ..."
  wget -q "$JDBC_URL"
fi

# ---- Run PySpark job -----------------------------------------------------
echo "Running j_usecase_flavor_setup ..."
spark-submit \
  --jars "$JDBC_JAR" \
  j_usecase_flavor_setup.py \
  --env-context-file test_data/env_context_file.cfg \
  --input-file test_data/ps_flavor.csv

echo "Done."
