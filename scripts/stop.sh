#!/usr/bin/env bash
# Stops THE CiteOlivia instance (shared by all agents in the project container), started by scripts/run.sh.
set -euo pipefail
STATE_DIR="${STATE_DIR:-${XDG_STATE_HOME:-$HOME/.local/state}/cite-olivia}"
PID_FILE="$STATE_DIR/app.pid"

alive() {
  [[ -r "/proc/$1/status" ]] && ! grep -q '^State:[[:space:]]*Z' "/proc/$1/status"
}

if [[ -f "$PID_FILE" ]] && alive "$(cat "$PID_FILE")"; then
  PID="$(cat "$PID_FILE")"
  echo "Stopping CiteOlivia (PID $PID)"
  kill "$PID"
  for _ in $(seq 1 30); do
    alive "$PID" || break
    sleep 1
  done
  if alive "$PID"; then
    echo "Force killing $PID"
    kill -9 "$PID"
  fi
fi
rm -f "$PID_FILE"
