# Linux/macOS Bash script
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
CP_FILE="$ROOT_DIR/target/classpath.txt"
mkdir -p "$LOG_DIR"

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: java not found in PATH." >&2
  exit 1
fi
if ! command -v mvn >/dev/null 2>&1; then
  echo "ERROR: mvn not found in PATH." >&2
  exit 1
fi

cd "$ROOT_DIR"

echo "Building project..."
mvn -q -DskipTests package
mvn -q -DincludeScope=runtime -Dmdep.outputFile="$CP_FILE" dependency:build-classpath

if [[ ! -f "$CP_FILE" ]]; then
  echo "ERROR: classpath file not generated: $CP_FILE" >&2
  exit 1
fi

DEP_CP="$(tr -d '\r\n' < "$CP_FILE")"
FULL_CP="target/classes:${DEP_CP}"

start_service() {
  local name="$1"
  local main_class="$2"
  local log_file="$LOG_DIR/${name}.log"
  local err_file="$LOG_DIR/${name}.err.log"
  echo "Starting $name..."
  nohup java -cp "$FULL_CP" "$main_class" >"$log_file" 2>"$err_file" &
  echo $! >"$LOG_DIR/${name}.pid"
  sleep 1
}

echo "Launching services in order..."
start_service "center-server" "org.centerServer.Main"
start_service "edge-server-2" "org.edgeServer2.Main"
start_service "edge-server-4" "org.edgeServer4.Main"
start_service "edge-server-1" "org.edgeServer1.Main"
start_service "edge-server-3" "org.edgeServer3.Main"
start_service "data-client-server" "org.dataClient.ServerMain"

echo "All services started. Logs in: $LOG_DIR"
