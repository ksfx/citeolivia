#!/usr/bin/env bash
# Restores the shared CiteOlivia database from a backup made by scripts/backup-db.sh.
# DANGER: replaces ALL current data (for all agents). A safety backup of the current state is made first.
#
#   scripts/restore-db.sh <backup.dump>
set -euo pipefail
BASEDIR="$(cd "$(dirname "$0")/.." && pwd)"
DB_NAME="${DB_NAME:-citeolivia}"
DB_USER="${DB_USER:-citeolivia}"
DB_PASSWORD="${DB_PASSWORD:-citeolivia}"

FILE="${1:-}"
if [[ -z "$FILE" || ! -f "$FILE" ]]; then
  echo "Usage: scripts/restore-db.sh <backup.dump>   (list backups: scripts/backup-db.sh --list)" >&2
  exit 1
fi

echo "Safety backup of the current state first:"
"$BASEDIR/scripts/backup-db.sh"

"$BASEDIR/scripts/stop.sh"
PGPASSWORD="$DB_PASSWORD" pg_restore -h localhost -U "$DB_USER" -d "$DB_NAME" --clean --if-exists --no-owner "$FILE"
echo "Restored $FILE into $DB_NAME. Start the app again with: scripts/run.sh --no-build"
