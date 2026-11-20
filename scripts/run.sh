#!/usr/bin/env bash
# (Re)starts CiteOlivia as a plain JAR on port 8080, detached from the shell.
#
# All agents of the project share ONE container (same user, same PostgreSQL, same port 8080).
# There is exactly one CiteOlivia instance: running this script restarts it for everyone.
# Runtime state (JAR copy, PID, log) lives outside the source tree in ~/.local/state/cite-olivia,
# so a later build cannot break the running instance. A lock prevents concurrent deployments.
#
#   scripts/run.sh            build JAR (without tests) and (re)start
#   scripts/run.sh --no-build (re)start the last built JAR from target/
#
# Before stopping the running instance, the shared database is backed up (scripts/backup-db.sh).
#
# Override with env: STATE_DIR, SERVER_PORT, DB_URL, DB_USER, DB_PASSWORD, SKIP_BACKUP=1
set -euo pipefail
BASEDIR="$(cd "$(dirname "$0")/.." && pwd)"
STATE_DIR="${STATE_DIR:-${XDG_STATE_HOME:-$HOME/.local/state}/cite-olivia}"
cd "$BASEDIR"

mkdir -p "$STATE_DIR"
exec 9>"$STATE_DIR/deploy.lock"
if ! flock -n 9; then
  echo "Another deployment is running right now – try again in a moment." >&2
  exit 1
fi

# A stopped process may linger as zombie (container init does not reap it) – treat it as stopped.
alive() {
  [[ -r "/proc/$1/status" ]] && ! grep -q '^State:[[:space:]]*Z' "/proc/$1/status"
}

if [[ "${1:-}" != "--no-build" ]]; then
  ./mvnw -q -B -DskipTests package
fi

# Safety backup of the shared database before every deployment (new migrations may change the schema).
if [[ "${SKIP_BACKUP:-}" != "1" ]]; then
  "$BASEDIR/scripts/backup-db.sh" || { echo "Backup failed – deployment aborted (SKIP_BACKUP=1 to override)." >&2; exit 1; }
fi

"$BASEDIR/scripts/stop.sh"

cp target/cite-olivia-0.0.1-SNAPSHOT.jar "$STATE_DIR/cite-olivia.jar"
setsid nohup java -jar "$STATE_DIR/cite-olivia.jar" > "$STATE_DIR/app.log" 2>&1 < /dev/null 9>&- &
echo $! > "$STATE_DIR/app.pid"
echo "Started PID $(cat "$STATE_DIR/app.pid") on port ${SERVER_PORT:-8080} (log: $STATE_DIR/app.log)"
