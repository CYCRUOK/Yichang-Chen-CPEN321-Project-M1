#!/usr/bin/env bash
#
# One-shot bootstrap for a fresh Ubuntu 22.04/24.04 cloud VM (AWS EC2, GCP
# Compute Engine, Azure VM, ...). Installs Docker, fetches the project, writes
# backend/.env from the values you pass in, and starts the backend with
# Docker Compose. Safe to re-run: every step is idempotent.
#
# Usage (run ON the VM as a sudo-capable user):
#   curl -fsSL <raw-url-of-this-script> -o cloud-setup.sh
#   bash cloud-setup.sh <git-repo-url> [<branch>]
#
# Or, if you copied the project to the VM with scp instead of git:
#   PROJECT_DIR=~/project bash cloud-setup.sh
#
# Environment overrides (all optional):
#   PROJECT_DIR        where the repo lives (default: ~/cpen321)
#   OWNER_FIRST_NAME   first name returned by GET /api/name
#   OWNER_LAST_NAME    last name returned by GET /api/name
#   JWT_SECRET         random string (generated if unset)

set -euo pipefail

REPO_URL="${1:-}"
BRANCH="${2:-main}"
PROJECT_DIR="${PROJECT_DIR:-$HOME/cpen321}"

info() { printf '\033[1;34m==> %s\033[0m\n' "$*"; }
die()  { printf '\033[1;31mERROR: %s\033[0m\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# Docker
# ---------------------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
  info "Installing Docker Engine + Compose plugin"
  sudo apt-get update -y
  sudo apt-get install -y ca-certificates curl gnupg
  sudo install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  sudo chmod a+r /etc/apt/keyrings/docker.gpg
  # shellcheck disable=SC1091
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
  sudo apt-get update -y
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  sudo usermod -aG docker "$USER"
else
  info "Docker already installed: $(docker --version)"
fi
sudo systemctl enable --now docker

# ---------------------------------------------------------------------------
# Project source
# ---------------------------------------------------------------------------
if [ -n "$REPO_URL" ]; then
  if [ -d "$PROJECT_DIR/.git" ]; then
    info "Updating $PROJECT_DIR from $REPO_URL ($BRANCH)"
    git -C "$PROJECT_DIR" fetch --all --prune
    git -C "$PROJECT_DIR" checkout "$BRANCH"
    git -C "$PROJECT_DIR" pull --ff-only
  else
    info "Cloning $REPO_URL ($BRANCH) into $PROJECT_DIR"
    sudo apt-get install -y git
    git clone --branch "$BRANCH" "$REPO_URL" "$PROJECT_DIR"
  fi
fi
[ -f "$PROJECT_DIR/docker-compose.yml" ] || die "No docker-compose.yml in $PROJECT_DIR (pass a repo URL or set PROJECT_DIR)"

# ---------------------------------------------------------------------------
# backend/.env
# ---------------------------------------------------------------------------
ENV_FILE="$PROJECT_DIR/backend/.env"
if [ ! -f "$ENV_FILE" ]; then
  info "Creating $ENV_FILE"
  cp "$PROJECT_DIR/backend/.env.example" "$ENV_FILE"
  JWT_SECRET="${JWT_SECRET:-$(head -c 48 /dev/urandom | base64 | tr -d '\n=/+')}"
  sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$JWT_SECRET|" "$ENV_FILE"
  sed -i "s|^NODE_ENV=.*|NODE_ENV=production|" "$ENV_FILE"
fi
if [ -n "${OWNER_FIRST_NAME:-}" ]; then sed -i "s|^OWNER_FIRST_NAME=.*|OWNER_FIRST_NAME=$OWNER_FIRST_NAME|" "$ENV_FILE"; fi
if [ -n "${OWNER_LAST_NAME:-}" ];  then sed -i "s|^OWNER_LAST_NAME=.*|OWNER_LAST_NAME=$OWNER_LAST_NAME|"   "$ENV_FILE"; fi

# ---------------------------------------------------------------------------
# Start
# ---------------------------------------------------------------------------
info "Starting backend (docker compose up --build -d)"
cd "$PROJECT_DIR"
sudo docker compose up --build -d

info "Waiting for http://localhost:3000/health"
for _ in $(seq 1 60); do
  if curl -sf http://localhost:3000/health >/dev/null; then
    PUBLIC_IP="$(curl -s --max-time 5 https://api64.ipify.org || echo '<unknown>')"
    info "Backend is up."
    echo "  Local:  http://localhost:3000/health"
    echo "  Public: http://$PUBLIC_IP:3000/health  (open TCP 3000 in the cloud firewall / security group)"
    echo "  Logs:   sudo docker compose -f $PROJECT_DIR/docker-compose.yml logs -f backend"
    exit 0
  fi
  sleep 1
done
die "Backend did not become healthy. Check: sudo docker compose logs backend"
