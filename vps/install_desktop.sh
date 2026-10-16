#!/usr/bin/env bash
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive

password_file=/root/sonyvegas-deploy/desktop-password
test "$(id -u)" -eq 0
test -f "$password_file"
desktop_password="$(tr -d '\r\n' < "$password_file")"
if [[ ! "$desktop_password" =~ ^[[:graph:]]{6,8}$ ]]; then
  echo "Desktop password must contain 6-8 printable ASCII characters" >&2
  exit 2
fi
trap 'rm -f "$password_file"' EXIT

apt-get update -qq
apt-get install -y -qq --no-install-recommends \
  ca-certificates curl dbus-x11 \
  gnome-session gnome-shell gnome-terminal nautilus \
  tigervnc-standalone-server tigervnc-common tigervnc-tools \
  novnc websockify x11-utils xdotool scrot \
  fonts-liberation fonts-cantarell sudo

id desktop >/dev/null 2>&1 || useradd --create-home --shell /bin/bash desktop
echo "desktop:${desktop_password}" | chpasswd
usermod -aG sudo desktop
printf '%s\n' 'desktop ALL=(ALL:ALL) ALL' > /etc/sudoers.d/desktop
chmod 0440 /etc/sudoers.d/desktop
visudo -cf /etc/sudoers.d/desktop

install -d -m 0700 -o desktop -g desktop /home/desktop/.vnc
printf '%s\n' "$desktop_password" | tigervncpasswd -f > /home/desktop/.vnc/passwd
chown desktop:desktop /home/desktop/.vnc/passwd
chmod 0600 /home/desktop/.vnc/passwd

cat > /usr/share/gnome-session/sessions/gnome-vnc.session <<'EOF'
[GNOME Session]
Name=GNOME VNC
RequiredComponents=org.gnome.Shell;org.gnome.SettingsDaemon.A11ySettings;org.gnome.SettingsDaemon.Color;org.gnome.SettingsDaemon.Datetime;org.gnome.SettingsDaemon.Housekeeping;org.gnome.SettingsDaemon.Keyboard;org.gnome.SettingsDaemon.MediaKeys;org.gnome.SettingsDaemon.PrintNotifications;org.gnome.SettingsDaemon.Rfkill;org.gnome.SettingsDaemon.ScreensaverProxy;org.gnome.SettingsDaemon.Sharing;org.gnome.SettingsDaemon.Smartcard;org.gnome.SettingsDaemon.Sound;org.gnome.SettingsDaemon.UsbProtection;org.gnome.SettingsDaemon.Wacom;org.gnome.SettingsDaemon.XSettings;
EOF

cat > /usr/local/bin/start-gnome-tigervnc <<'EOF'
#!/bin/bash
set -euo pipefail

export USER=desktop
export HOME=/home/desktop
export DISPLAY=:1
export XDG_RUNTIME_DIR=/run/desktop-gnome
export XDG_SESSION_TYPE=x11
export XDG_CURRENT_DESKTOP=GNOME
export XDG_SESSION_DESKTOP=gnome
export GDMSESSION=gnome
export LIBGL_ALWAYS_SOFTWARE=1

mkdir -p "$XDG_RUNTIME_DIR" "$HOME/.vnc"
chmod 700 "$XDG_RUNTIME_DIR"
rm -f /tmp/.X1-lock /tmp/.X11-unix/X1

vnc_pid=
desktop_pid=

/usr/bin/Xtigervnc :1 \
  -geometry 1440x900 \
  -depth 24 \
  -rfbport 5901 \
  -localhost \
  -SecurityTypes VncAuth \
  -PasswordFile "$HOME/.vnc/passwd" \
  -AlwaysShared=1 &
vnc_pid=$!

cleanup() {
  if [ -n "${desktop_pid:-}" ]; then kill "$desktop_pid" 2>/dev/null || true; fi
  if [ -n "${vnc_pid:-}" ]; then kill "$vnc_pid" 2>/dev/null || true; fi
}
trap cleanup EXIT INT TERM

for _ in $(seq 1 30); do
  xdpyinfo >/dev/null 2>&1 && break
  sleep 1
done
xdpyinfo >/dev/null

dbus-launch --exit-with-session gnome-session --session=gnome-vnc &
desktop_pid=$!

while kill -0 "$vnc_pid" 2>/dev/null && kill -0 "$desktop_pid" 2>/dev/null; do
  sleep 2
done
exit 1
EOF
chmod 0755 /usr/local/bin/start-gnome-tigervnc

cat > /etc/systemd/system/tigervnc-gnome.service <<'EOF'
[Unit]
Description=GNOME desktop on TigerVNC
After=network.target

[Service]
Type=simple
User=desktop
Environment=HOME=/home/desktop
RuntimeDirectory=desktop-gnome
RuntimeDirectoryMode=0700
ExecStart=/usr/local/bin/start-gnome-tigervnc
Restart=always
RestartSec=3
KillMode=control-group
TimeoutStopSec=10

[Install]
WantedBy=multi-user.target
EOF

cat > /etc/systemd/system/novnc.service <<'EOF'
[Unit]
Description=noVNC web client
After=tigervnc-gnome.service
Requires=tigervnc-gnome.service

[Service]
ExecStart=/usr/bin/websockify --web /usr/share/novnc 6080 localhost:5901
Restart=always
RestartSec=3
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true

[Install]
WantedBy=multi-user.target
EOF

systemctl mask --now upower.service 2>/dev/null || true
systemctl daemon-reload
systemctl enable --now tigervnc-gnome.service novnc.service

for _ in $(seq 1 120); do
  if curl -fsS http://127.0.0.1:6080/vnc.html >/dev/null \
      && runuser -u desktop -- env DISPLAY=:1 xdpyinfo >/dev/null 2>&1 \
      && pgrep -x gnome-shell >/dev/null; then
    exit 0
  fi
  sleep 2
done

journalctl -u tigervnc-gnome -u novnc --no-pager -n 240
exit 1
