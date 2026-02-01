# Linux/macOS Bash script
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"

if [[ ! -d "$LOG_DIR" ]]; then
  echo "No logs directory found: $LOG_DIR"
  exit 0
fi

shopt -s nullglob
for pid_file in "$LOG_DIR"/*.pid; do
  pid="$(tr -d '\r\n' < "$pid_file")"
  if [[ -n "$pid" ]]; then
    echo "Stopping PID $pid from $pid_file"
    kill -9 "$pid" 2>/dev/null || true
  fi
done

echo "Done."
