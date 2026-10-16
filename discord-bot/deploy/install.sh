#!/usr/bin/env bash
set -euo pipefail

if [[ $(id -u) != 0 ]]; then
  printf 'Execute como root.\n' >&2
  exit 1
fi
if [[ ! -f /opt/unknown-discord-bot/package-lock.json || ! -f /etc/unknown-discord-bot/bot.env ]]; then
  printf 'Envie o código e bot.env antes de instalar.\n' >&2
  exit 1
fi
if ! id unknown-discord-bot >/dev/null 2>&1; then
  useradd --system --home-dir /var/lib/unknown-discord-bot --create-home --shell /usr/sbin/nologin unknown-discord-bot
fi
install -d -m 0700 -o unknown-discord-bot -g unknown-discord-bot /var/lib/unknown-discord-bot /var/lib/unknown-discord-bot/data
chown root:root /etc/unknown-discord-bot/bot.env
chmod 0600 /etc/unknown-discord-bot/bot.env
cd /opt/unknown-discord-bot
npm ci --omit=dev --no-fund --no-audit
chown -R root:unknown-discord-bot /opt/unknown-discord-bot
chmod -R u=rwX,g=rX,o= /opt/unknown-discord-bot
install -m 0644 deploy/unknown-discord-bot.service /etc/systemd/system/unknown-discord-bot.service
systemctl daemon-reload
systemctl enable unknown-discord-bot.service
printf 'Serviço instalado. Configure a aplicação/publicação dos comandos e inicie com systemctl start unknown-discord-bot.\n'
