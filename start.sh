#!/usr/bin/env bash
# One command to run everything: start Docker if needed, then docker compose up.

set -e

cd "$(dirname "$0")"

# Wait for Docker daemon to be ready (poll up to 2 min)
wait_for_docker() {
  echo "Waiting for Docker to be ready..."
  local max=24
  local i=0
  while ! docker info >/dev/null 2>&1; do
    i=$((i + 1))
    if [ $i -ge $max ]; then
      echo "Docker did not start in time. Please start Docker Desktop manually and run: docker compose up -d"
      exit 1
    fi
    sleep 5
  done
  echo "Docker is ready."
}

# Check if Docker daemon is running
if ! docker info >/dev/null 2>&1; then
  echo "Docker daemon is not running."
  if [[ "$(uname)" == "Darwin" ]]; then
    echo "Opening Docker Desktop..."
    open -a Docker 2>/dev/null || true
    wait_for_docker
  else
    echo "Please start Docker (e.g. Docker Desktop) and run this script again."
    exit 1
  fi
fi

echo "Starting app, PostgreSQL, Redis, and Adminer..."
docker compose up -d --build

echo ""
echo "Done. Services are starting (app may take ~30s to be ready)."
echo "  App:     http://localhost:8080"
echo "  Swagger: http://localhost:8080/swagger-ui.html"
echo "  Adminer: http://localhost:8081  (Server: postgres, User: raguser, Password: ragpass, DB: rag_chat)"
echo ""
echo "To stop: ./quit.sh  or  docker compose down"
