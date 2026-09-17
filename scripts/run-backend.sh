#!/usr/bin/env bash
#
# Assumes config (e.g., backend/.env) is already setup, starts docker compose.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

info()  { printf '\033[34m==>\033[0m %s\n' "$*"; }
die()   { printf '\033[31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# Prerequisites
# ---------------------------------------------------------------------------
command -v docker >/dev/null 2>&1 || die "Docker not found."
docker info >/dev/null 2>&1       || die "Docker is not running."
command -v curl >/dev/null 2>&1    || die "curl not found."

[[ -f backend/.env ]] || die "Missing backend/.env — follow the student setup guide first."

BACKEND_PORT="$(grep -E '^PORT=' backend/.env | head -1 | cut -d= -f2- | tr -d ' "' || true)"
BACKEND_HEALTH_URL="${BACKEND_HEALTH_URL:-http://localhost:${BACKEND_PORT:-3000}/health}"

# ---------------------------------------------------------------------------
# Backend
# ---------------------------------------------------------------------------

info "Starting backend (docker compose up --build -d)..."
docker compose up --build -d

info "Waiting for $BACKEND_HEALTH_URL ..."
for _ in $(seq 1 120); do
  curl -sf "$BACKEND_HEALTH_URL" >/dev/null 2>&1 && break
  sleep 1
done
curl -sf "$BACKEND_HEALTH_URL" >/dev/null 2>&1 || die "Backend not healthy. Try: docker compose logs backend"

echo
info "Backend is up. Stop with: docker compose down"
