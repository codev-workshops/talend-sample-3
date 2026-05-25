#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Start MySQL
docker-compose up -d mysql

# Wait for healthy
echo "Waiting for MySQL to be ready..."
docker-compose exec mysql mysqladmin ping --wait=30

# Download MySQL JDBC driver if not present
if [ ! -f mysql-connector-j-8.0.33.jar ]; then
  echo "Downloading MySQL JDBC driver..."
  wget https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar
fi

# Run PySpark job
spark-submit \
  --jars mysql-connector-j-8.0.33.jar \
  j_usecase_customer_setup.py \
  --env-context-file test_data/env_context_file.cfg \
  --input-file test_data/ps_cust.csv
