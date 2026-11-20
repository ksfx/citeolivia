#!/usr/bin/env bash
# Prepares a fresh Ubuntu machine/container to build, test and run CiteOlivia.
# Idempotent – safe to run again (e.g. after a container restart, to start PostgreSQL).
#
#   - installs JDK 25 and PostgreSQL (apt, needs sudo)
#   - starts PostgreSQL (containers have no systemd)
#   - creates role "citeolivia" and the databases "citeolivia" + "citeolivia_test"
#
# Override with env: DB_USER, DB_PASSWORD (defaults match application.properties)
set -euo pipefail
DB_USER="${DB_USER:-citeolivia}"
DB_PASSWORD="${DB_PASSWORD:-citeolivia}"

need=()
command -v java >/dev/null && java -version 2>&1 | grep -q 'version "25' || need+=(openjdk-25-jdk-headless)
command -v pg_ctlcluster >/dev/null || need+=(postgresql)
command -v curl >/dev/null || need+=(curl)
if ((${#need[@]})); then
  echo "Installing: ${need[*]}"
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq "${need[@]}"
fi

PG_VERSION="$(ls /usr/lib/postgresql | sort -V | tail -1)"
if ! pg_lsclusters -h | awk -v v="$PG_VERSION" '$1==v && $2=="main" && $4=="online"' | grep -q .; then
  echo "Starting PostgreSQL $PG_VERSION"
  sudo pg_ctlcluster "$PG_VERSION" main start
fi

psql_admin() { sudo -u postgres psql -v ON_ERROR_STOP=1 -qtA "$@"; }

if [[ -z "$(psql_admin -c "SELECT 1 FROM pg_roles WHERE rolname = '$DB_USER'")" ]]; then
  echo "Creating role $DB_USER"
  psql_admin -c "CREATE ROLE \"$DB_USER\" LOGIN PASSWORD '$DB_PASSWORD'"
fi
for db in citeolivia citeolivia_test; do
  if [[ -z "$(psql_admin -c "SELECT 1 FROM pg_database WHERE datname = '$db'")" ]]; then
    echo "Creating database $db"
    psql_admin -c "CREATE DATABASE \"$db\" OWNER \"$DB_USER\""
  fi
done

echo "Environment ready: $(java -version 2>&1 | head -1), PostgreSQL $PG_VERSION online."
