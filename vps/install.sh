#!/usr/bin/env bash
set -euo pipefail

test "$(id -u)" -eq 0
test -f /root/sonyvegas-deploy/sonyvegas_server.py
test -f /root/sonyvegas-deploy/admin_features.py
test -f /root/sonyvegas-deploy/admin_assets/dashboard.js
test -f /root/sonyvegas-deploy/admin_assets/dashboard.css
test -f /root/sonyvegas-deploy/nginx_limits.conf
test -f /root/sonyvegas-deploy/nginx_site.conf
test -f /root/sonyvegas-deploy/nginx_proxy.conf
test -f /root/sonyvegas-deploy/license_crypto.py
test -f /root/sonyvegas-deploy/license_private.json
test -f /root/sonyvegas-deploy/admin_dashboard.html
test -f /root/sonyvegas-deploy/sonyvegas.service
test -f /root/sonyvegas-deploy/nginx.conf
test -f /root/sonyvegas-deploy/process_cb_mappings.py
test -f /root/sonyvegas-deploy/run_mapping_queue.sh
test -f /root/sonyvegas-deploy/backend.env || test -f /etc/sonyvegas/backend.env

if ! command -v nginx >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1; then
  export DEBIAN_FRONTEND=noninteractive
  apt-get update
  apt-get install -y --no-install-recommends nginx openjdk-21-jdk-headless
fi
if ! /usr/bin/python3 -c 'import qrcode' >/dev/null 2>&1; then
  export DEBIAN_FRONTEND=noninteractive
  apt-get update
  apt-get install -y --no-install-recommends python3-qrcode
fi

id sonyvegas >/dev/null 2>&1 || useradd --system --home-dir /var/lib/sonyvegas --shell /usr/sbin/nologin sonyvegas
install -d -m 0750 -o root -g sonyvegas /opt/sonyvegas
install -d -m 0750 -o root -g sonyvegas /opt/sonyvegas/admin_assets
install -d -m 0700 -o sonyvegas -g sonyvegas /var/lib/sonyvegas /var/lib/sonyvegas/payloads /var/lib/sonyvegas/auxiliary
install -d -m 0755 -o root -g root /var/www/sonyvegas-downloads
install -d -m 0700 -o sonyvegas -g sonyvegas \
  /var/lib/sonyvegas/mappings /var/lib/sonyvegas/mappings/incoming \
  /var/lib/sonyvegas/mappings/logs \
  /var/lib/sonyvegas/mappings/profiles /var/lib/sonyvegas/mapping-worker
install -d -m 0700 /etc/sonyvegas

winrar_path=/var/www/sonyvegas-downloads/winrar-x64.exe
winrar_sha256=8ff0daf3ed564cc743c0e23ff2e253997ffc74460f9673f0b6dd037b2db4ce7b
if ! test -f "$winrar_path" || ! echo "$winrar_sha256  $winrar_path" | sha256sum --check --status; then
  winrar_tmp=$(mktemp)
  trap 'rm -f "$winrar_tmp"' EXIT
  curl -fsSL --retry 3 --connect-timeout 15 --max-time 120 \
    https://www.rarlab.com/rar/winrar-x64-723.exe -o "$winrar_tmp"
  echo "$winrar_sha256  $winrar_tmp" | sha256sum --check --status
  test "$(od -An -tx1 -N2 "$winrar_tmp" | tr -d ' \n')" = 4d5a
  install -m 0644 -o root -g root "$winrar_tmp" "$winrar_path"
  rm -f "$winrar_tmp"
  trap - EXIT
fi

install -m 0750 -o root -g sonyvegas /root/sonyvegas-deploy/sonyvegas_server.py /opt/sonyvegas/sonyvegas_server.py
install -m 0640 -o root -g sonyvegas /root/sonyvegas-deploy/admin_features.py /opt/sonyvegas/admin_features.py
for asset in dashboard.js dashboard.css space-mono-bold.ttf space-mono-regular.ttf; do
  install -m 0640 -o root -g sonyvegas "/root/sonyvegas-deploy/admin_assets/$asset" "/opt/sonyvegas/admin_assets/$asset"
done
install -m 0640 -o root -g sonyvegas /root/sonyvegas-deploy/license_crypto.py /opt/sonyvegas/license_crypto.py
install -m 0640 -o root -g sonyvegas /root/sonyvegas-deploy/license_private.json /opt/sonyvegas/license_private.json
install -m 0640 -o root -g sonyvegas /root/sonyvegas-deploy/admin_dashboard.html /opt/sonyvegas/admin_dashboard.html
install -m 0750 -o root -g sonyvegas /root/sonyvegas-deploy/process_cb_mappings.py /opt/sonyvegas/process_cb_mappings.py
install -m 0750 -o root -g sonyvegas /root/sonyvegas-deploy/run_mapping_queue.sh /opt/sonyvegas/run_mapping_queue.sh
if test -f /root/sonyvegas-deploy/backend.env; then
  install -m 0600 /root/sonyvegas-deploy/backend.env /etc/sonyvegas/backend.env
fi
install -m 0644 /root/sonyvegas-deploy/sonyvegas.service /etc/systemd/system/sonyvegas.service
install -m 0644 /root/sonyvegas-deploy/sonyvegas-mappings.service /etc/systemd/system/sonyvegas-mappings.service
install -m 0644 /root/sonyvegas-deploy/sonyvegas-mappings.path /etc/systemd/system/sonyvegas-mappings.path
install -m 0644 /root/sonyvegas-deploy/nginx_limits.conf /etc/nginx/conf.d/unknown-limits.conf
install -m 0644 /root/sonyvegas-deploy/nginx_site.conf /etc/nginx/sites-available/unknown
install -m 0644 /root/sonyvegas-deploy/nginx_proxy.conf /etc/nginx/snippets/unknown_proxy.conf
ln -sfn /etc/nginx/sites-available/unknown /etc/nginx/sites-enabled/unknown

systemctl daemon-reload
systemctl enable sonyvegas.service
systemctl enable --now sonyvegas-mappings.path
systemctl enable nginx.service
nginx -t
systemctl restart sonyvegas.service
systemctl restart nginx.service

for _ in $(seq 1 30); do
  curl -fsS -H 'Host: crimsonv2.style.dev' http://127.0.0.1:8080/health >/dev/null && exit 0
  sleep 1
done

journalctl -u sonyvegas.service --no-pager -n 100
journalctl -u nginx.service --no-pager -n 100
exit 1
