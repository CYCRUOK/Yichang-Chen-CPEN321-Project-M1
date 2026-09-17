#!/usr/bin/env bash
#
# Puts the backend behind HTTPS/WSS on a cloud VM: installs nginx + certbot,
# obtains a Let's Encrypt certificate for DOMAIN and proxies
#   https://DOMAIN/...   -> http://127.0.0.1:3000/...
#   wss://DOMAIN/ws      -> ws://127.0.0.1:3000/ws   (WebSocket upgrade)
#
# Run ON the VM after scripts/cloud-setup.sh, with TCP 80 and 443 open in the
# cloud firewall:
#   DOMAIN=<name> EMAIL=<you@example.com> bash cloud-https.sh
#
# DOMAIN can be a real name pointing at this VM, or a zero-setup one such as
# <public-ip-with-dashes>.sslip.io (e.g. 34-12-34-56.sslip.io), which resolves
# to the IP encoded in it. Re-running is safe; certbot renews automatically.

set -euo pipefail

DOMAIN="${DOMAIN:?set DOMAIN, e.g. DOMAIN=34-12-34-56.sslip.io}"
EMAIL="${EMAIL:?set EMAIL for Lets Encrypt expiry notices}"
BACKEND_PORT="${BACKEND_PORT:-3000}"

info() { printf '\033[1;34m==> %s\033[0m\n' "$*"; }
die()  { printf '\033[1;31mERROR: %s\033[0m\n' "$*" >&2; exit 1; }

curl -sf "http://127.0.0.1:${BACKEND_PORT}/health" >/dev/null || die "backend is not answering on :${BACKEND_PORT}; run cloud-setup.sh first"

info "Installing nginx + certbot"
sudo apt-get update -y
sudo apt-get install -y nginx certbot python3-certbot-nginx

info "Writing nginx site for ${DOMAIN}"
sudo tee /etc/nginx/sites-available/cpen321 >/dev/null <<EOF
server {
    listen 80;
    listen [::]:80;
    server_name ${DOMAIN};

    location / {
        proxy_pass http://127.0.0.1:${BACKEND_PORT};
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        # WebSocket upgrade for /ws (harmless for plain HTTP requests)
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }
}
EOF
sudo ln -sf /etc/nginx/sites-available/cpen321 /etc/nginx/sites-enabled/cpen321
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl enable --now nginx
sudo systemctl reload nginx

info "Requesting a Let's Encrypt certificate (certbot --nginx)"
sudo certbot --nginx --non-interactive --agree-tos --redirect -m "$EMAIL" -d "$DOMAIN"

info "Verifying"
curl -sf "https://${DOMAIN}/health" && echo
echo "  API:  https://${DOMAIN}/api/name"
echo "  WS:   wss://${DOMAIN}/ws"
echo "  Cert: $(sudo certbot certificates 2>/dev/null | grep -m1 'Expiry Date' | sed 's/^ *//')"
echo "  Auto-renewal: $(systemctl is-enabled certbot.timer 2>/dev/null || echo 'via cron')"
