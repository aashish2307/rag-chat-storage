#!/usr/bin/env bash
# Stop local app (ports 8080, 8081) and Docker Compose stack.

set -e

echo "Stopping processes on 8080 and 8081..."
for port in 8080 8081; do
  pid=$(lsof -ti :$port 2>/dev/null || true)
  if [ -n "$pid" ]; then
    kill -9 $pid 2>/dev/null && echo "  Killed PID $pid on port $port" || true
  fi
done

echo "Stopping Docker Compose..."
cd "$(dirname "$0")"
docker compose down 2>/dev/null && echo "  Docker stack stopped." || echo "  (Docker not running or not installed)"

echo "Done. Ports 8080 and 8081 should be free."
