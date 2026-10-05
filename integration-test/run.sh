#!/usr/bin/env bash
#
# Browser integration test: builds the app, starts the production server,
# runs a Playwright user journey (10 clicks, 5 screenshots) and stops the server again.
#
# Usage: integration-test/run.sh [--skip-build]
#   PORT=9123  port the server is started on
#
# Screenshots: integration-test/screenshots/   HTML report: integration-test/report/
# Server log:  integration-test/server.log

set -euo pipefail

TEST_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(dirname "$TEST_DIR")"
PORT="${PORT:-9123}"
BASE_URL="http://localhost:${PORT}"
SERVER_BIN="$ROOT_DIR/target/universal/stage/bin/mooncal"
SERVER_LOG="$TEST_DIR/server.log"
SERVER_PID=""

log() { printf '\n=== %s ===\n' "$*"; }

stop_server() {
  if [[ -n "$SERVER_PID" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
    log "Stopping server (pid $SERVER_PID)"
    kill "$SERVER_PID"
    for _ in {1..30}; do
      kill -0 "$SERVER_PID" 2>/dev/null || return 0
      sleep 1
    done
    echo "Server did not stop gracefully, killing it"
    kill -9 "$SERVER_PID" 2>/dev/null || true
  fi
}
trap stop_server EXIT INT TERM

if curl -s -o /dev/null "$BASE_URL"; then
  echo "Port $PORT is already in use. Stop the process or run with PORT=<other port>." >&2
  exit 1
fi

if [[ "${1:-}" != "--skip-build" ]]; then
  log "Building application (sbt stage, includes the Angular production build)"
  (cd "$ROOT_DIR" && sbt -batch stage)
fi
[[ -x "$SERVER_BIN" ]] || { echo "Server binary $SERVER_BIN not found, run without --skip-build" >&2; exit 1; }

log "Installing Playwright"
cd "$TEST_DIR"
npm install --no-audit --no-fund --silent
npx playwright install chromium

log "Starting server on port $PORT"
# Dummy translate deployment id: only needed for live moon-landing updates, which are not configured here
"$SERVER_BIN" -Dhttp.port="$PORT" -Dpidfile.path=/dev/null \
  -DgoogleTranslateScriptDeploymentId=integration-test > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!

for i in {1..60}; do
  if curl -sf -o /dev/null "$BASE_URL/server/status"; then
    echo "Server is up after ${i}s"
    break
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "Server exited during startup, see $SERVER_LOG:" >&2
    tail -n 30 "$SERVER_LOG" >&2
    exit 1
  fi
  if [[ $i -eq 60 ]]; then
    echo "Server did not come up within 60s, see $SERVER_LOG" >&2
    exit 1
  fi
  sleep 1
done

log "Running browser tests"
rm -rf screenshots && mkdir -p screenshots
BASE_URL="$BASE_URL" npx playwright test

log "Integration test passed"
echo "Screenshots:"
ls -1 "$TEST_DIR"/screenshots/*.png
