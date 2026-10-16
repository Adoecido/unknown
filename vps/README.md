# SonyVegas on a persistent VM

This directory replaces the former managed Function, database and object
bucket with one small service backed by SQLite and private files on the VM.

Runtime layout:

- Nginx public listener: port `8080`, reverse proxy and edge rate limits
- API process: `/opt/sonyvegas/sonyvegas_server.py`, loopback port `8081`
- admin site: `/opt/sonyvegas/admin_dashboard.html`, served at `/admin`
- database and payloads: `/var/lib/sonyvegas`
- secret environment: `/etc/sonyvegas/backend.env`
- license signer: `/opt/sonyvegas/license_crypto.py`
- private signing key: `/opt/sonyvegas/license_private.json` (`0640`, `root:sonyvegas`)
- public HTTPS route: Freestyle TLS to VM port `8080`
- browser desktop: GNOME + TigerVNC on loopback, exposed through noVNC on `6080`

The transitional endpoint `/v1/functions/sonyvegas-auth/executions` preserves
the old response envelope for already-built clients. New builds use
`/v1/control` directly. The loader's
temporary payload tokens remain two-minute, one-use tokens and control sessions
remain valid for twelve hours.

Deploy the signed-license server before distributing a client that requires
the signature. Include `license_crypto.py` and the existing matching
`license_private.json` alongside the server in `/root/sonyvegas-deploy`;
`install.sh` requires and installs these files. Generating another key pair
requires rebuilding the client's embedded public key. See `docs/PROTECTION.md`.

Use `scripts/vps_admin.ps1` for licenses, users, payload uploads, cleanup and
database exports. The configured Windows machine keeps the admin token in the
current user's DPAPI-encrypted `~/.sonyvegas/admin-token.clixml`, never in the
repository. `SONYVEGAS_ADMIN_TOKEN` remains available as a process-only
override.

Run `scripts/vps_admin.ps1 dashboard` to open the web control center without
putting the long-lived admin token in the URL. The script requests a one-minute,
single-use ticket; the browser exchanges it for a four-hour host-only,
HttpOnly, Secure, SameSite session cookie. A direct visit to `https://crimsonv2.style.dev/admin`
also accepts the admin token on its login screen and never stores that token in
browser storage.

The dashboard manages device status and roles, revokes device sessions, creates
and revokes licenses, provisions separate named administrator tokens, reports
payload health and displays the audit stream. Each administrator uses their own
token, so administrative changes are attributed to that person's name. New
administrator tokens are shown once and only their SHA-256 hashes are retained.
Every active administrator token can also upload the release or developer DLL;
the existing PE signature, size, rate-limit and audit checks still apply.
Use `scripts/vps_admin.ps1 create-admin "Name"` to provision and locally encrypt
another token, `list-admins` to inspect accounts, and `copy-admin-token socio`
to put a saved token on the clipboard without printing it.
Audited client events include successful loader opens, payload downloads,
completed Java injection startup, config save/load/delete and party
create/join/leave/sync. Administrative changes are audited as well. Audit rows
are retained for 180 days and included in the JSON export.

The HTTP service applies separate request limits to login, control, payload and
administrative routes. Eight login attempts per source in five minutes are
allowed before a temporary `429` block. Request workers are capped at 64 and
slow connections time out. Browser mutations require same-origin requests,
while scripts authenticate with Bearer tokens. Responses include HSTS, a
hash-based Content Security Policy, anti-framing, no-sniff, no-referrer,
permissions and no-index policies. The systemd service runs without Linux
capabilities under a read-only filesystem sandbox; only `/var/lib/sonyvegas`
is writable. Application source is readable only by root and the service group.
Nginx is the only public listener and adds a second per-client rate-limit layer;
the Python API is bound to loopback and cannot be reached directly from the TLS
route.

The Freestyle free VM currently has a plan-enforced automatic deletion date.
Download `/var/lib/sonyvegas/sonyvegas.sqlite3` and both payload files regularly,
and move to a non-expiring plan before using this as the only production copy.

The desktop is served at `https://crimsonv2-desktop.style.dev/vnc.html`. Raw
VNC never leaves loopback. The same generated 6-8 character password protects
the VNC viewer and the `desktop` user's sudo access.
