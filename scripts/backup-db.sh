#!/usr/bin/env bash
# Backs up the shared CiteOlivia database (users, projects, references, PDFs, sessions) with pg_dump.
#
#   scripts/backup-db.sh              create a backup
#   scripts/backup-db.sh --list       list existing backups
#
# Backups: $BACKUP_DIR (default ~/.local/state/cite-olivia/backups), custom format (*.dump),
# the newest $KEEP (default 20) are kept. Restore: see scripts/restore-db.sh.
set -euo pipefail
DB_NAME="${DB_NAME:-citeolivia}"
DB_USER="${DB_USER:-citeolivia}"
DB_PASSWORD="${DB_PASSWORD:-citeolivia}"
STATE_DIR="${STATE_DIR:-${XDG_STATE_HOME:-$HOME/.local/state}/cite-olivia}"
BACKUP_DIR="${BACKUP_DIR:-$STATE_DIR/backups}"
KEEP="${KEEP:-20}"

mkdir -p "$BACKUP_DIR"
if [[ "${1:-}" == "--list" ]]; then
  ls -lh "$BACKUP_DIR"/*.dump 2>/dev/null || echo "No backups in $BACKUP_DIR"
  exit 0
fi

FILE="$BACKUP_DIR/${DB_NAME}-$(date +%Y%m%d-%H%M%S).dump"
PGPASSWORD="$DB_PASSWORD" pg_dump -h localhost -U "$DB_USER" -d "$DB_NAME" --format=custom --file="$FILE.tmp"
mv "$FILE.tmp" "$FILE"
echo "Backup: $FILE ($(du -h "$FILE" | cut -f1))"

# keep only the newest $KEEP backups
ls -1t "$BACKUP_DIR"/"$DB_NAME"-*.dump 2>/dev/null | tail -n +"$((KEEP + 1))" | xargs -r rm -f
