"""unknown dashboard, private download catalog and client activity records.

This module shares the existing SQLite database and authentication. License keys
are encrypted with a private, persistent Fernet key; it is part of data backups.
"""
from __future__ import annotations

import hashlib
import hmac
import base64
import binascii
import io
import json
import os
import re
import secrets
import shutil
import sqlite3
import struct
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.parse import parse_qs, quote
from urllib.request import Request, urlopen

from cryptography.fernet import Fernet, InvalidToken

ROLES = ("user", "tester", "developer")
KINDS = ("loader", "dll", "application")
ONLINE_SECONDS = 50
CLIENT_LIMIT = 32 * 1024 * 1024
APPLICATION_LIMIT = 256 * 1024 * 1024
DOWNLOAD_RE = re.compile(r"^dl_[0-9a-f]{32}$")
LICENSE_RE = re.compile(r"^lc_[0-9a-f]{32}$")
TOTP_STEP = 30


def totp_counter(secret: str, code: object, last_counter: int = -1) -> int | None:
    if not isinstance(code, str) or not re.fullmatch(r"[0-9]{6}", code):
        return None
    try:
        key = base64.b32decode(secret, casefold=True)
    except (ValueError, binascii.Error):
        return None
    current = int(time.time()) // TOTP_STEP
    for counter in (current - 1, current, current + 1):
        if counter <= last_counter:
            continue
        digest_bytes = hmac.new(key, struct.pack(">Q", counter), hashlib.sha1).digest()
        offset = digest_bytes[-1] & 15
        value = (struct.unpack(">I", digest_bytes[offset:offset + 4])[0] & 0x7fffffff) % 1000000
        if hmac.compare_digest(f"{value:06d}", code):
            return counter
    return None


def now() -> datetime:
    return datetime.now(timezone.utc)


def stamp(value: datetime | None = None) -> str:
    return (value or now()).isoformat().replace("+00:00", "Z")


def clean(value: object, maximum: int = 160) -> str:
    return str(value or "").replace("\x00", "").strip()[:maximum]


def diagnostic(value: object, maximum: int = 6000) -> str:
    text = clean(value, maximum)
    # Diagnostics never include access credentials or private profile payloads.
    text = re.sub(r"(?i)(Bearer\s+)\S+", r"\1[redacted]", text)
    text = re.sub(r"(?i)(token|secret|password|licenseKey)([\s=:]+)[^\s,;\"]+", r"\1\2[redacted]", text)
    text = re.sub(r"/api/v1/[^/\s]+/", "/api/v1/[redacted]/", text)
    text = re.sub(r"https?://[^\s]+", "[endpoint]", text)
    return text


class AdminBackendFeatures:
    def initialize_admin_features(self) -> None:
        self.download_dir = self.data_dir / "downloads"
        self.download_dir.mkdir(mode=0o700, exist_ok=True)
        with self.lock, self.connect() as connection:
            for table, columns in {
                "licenses": {"key_ciphertext": "TEXT", "key_hint": "TEXT"},
                "devices": {"last_ip": "TEXT", "last_login_at": "TEXT"},
            }.items():
                existing = {row["name"] for row in connection.execute(f"PRAGMA table_info({table})")}
                for name, sql_type in columns.items():
                    if name not in existing:
                        connection.execute(f"ALTER TABLE {table} ADD COLUMN {name} {sql_type}")
            connection.executescript("""
                CREATE TABLE IF NOT EXISTS client_presence (
                    instance_id TEXT PRIMARY KEY, device_id TEXT NOT NULL,
                    version TEXT NOT NULL, client TEXT NOT NULL, java_version TEXT NOT NULL,
                    started_at TEXT NOT NULL, last_seen_at TEXT NOT NULL,
                    source_ip TEXT NOT NULL, status TEXT NOT NULL,
                    stopped_at TEXT, reason TEXT,
                    FOREIGN KEY(device_id) REFERENCES devices(id) ON DELETE CASCADE
                );
                CREATE INDEX IF NOT EXISTS idx_client_presence_seen ON client_presence(status,last_seen_at);
                CREATE TABLE IF NOT EXISTS client_close_requests (
                    instance_id TEXT PRIMARY KEY, device_id TEXT NOT NULL,
                    requested_at TEXT NOT NULL, expires_at TEXT NOT NULL,
                    admin_id TEXT NOT NULL,
                    FOREIGN KEY(instance_id) REFERENCES client_presence(instance_id) ON DELETE CASCADE,
                    FOREIGN KEY(device_id) REFERENCES devices(id) ON DELETE CASCADE
                );
                CREATE TABLE IF NOT EXISTS downloads (
                    id TEXT PRIMARY KEY, name TEXT NOT NULL, kind TEXT NOT NULL,
                    role TEXT NOT NULL, version TEXT NOT NULL, filename TEXT NOT NULL,
                    notes TEXT NOT NULL, enabled INTEGER NOT NULL DEFAULT 0,
                    uploaded INTEGER NOT NULL DEFAULT 0, size INTEGER NOT NULL DEFAULT 0,
                    sha256 TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS download_tokens (
                    id TEXT PRIMARY KEY, download_id TEXT NOT NULL, device_id TEXT NOT NULL,
                    expires_at TEXT NOT NULL,
                    FOREIGN KEY(download_id) REFERENCES downloads(id) ON DELETE CASCADE,
                    FOREIGN KEY(device_id) REFERENCES devices(id) ON DELETE CASCADE
                );
            """)
            presence_columns = {row["name"] for row in connection.execute("PRAGMA table_info(client_presence)")}
            for column in ("player_name", "player_uuid"):
                if column not in presence_columns:
                    connection.execute(f"ALTER TABLE client_presence ADD COLUMN {column} TEXT NOT NULL DEFAULT ''")
            if "supports_remote_close" not in presence_columns:
                connection.execute("ALTER TABLE client_presence ADD COLUMN supports_remote_close INTEGER NOT NULL DEFAULT 0")
            if "probe_id" not in presence_columns:
                connection.execute("ALTER TABLE client_presence ADD COLUMN probe_id TEXT NOT NULL DEFAULT ''")
            admin_columns = {row["name"] for row in connection.execute("PRAGMA table_info(admins)")}
            if "totp_secret" not in admin_columns:
                connection.execute("ALTER TABLE admins ADD COLUMN totp_secret TEXT")
            if "totp_last_counter" not in admin_columns:
                connection.execute("ALTER TABLE admins ADD COLUMN totp_last_counter INTEGER NOT NULL DEFAULT -1")
            connection.execute("CREATE TABLE IF NOT EXISTS admin_mfa_limits ("
                               "admin_id TEXT PRIMARY KEY, failures INTEGER NOT NULL,"
                               "window_started TEXT NOT NULL, locked_until TEXT)")
            connection.execute("CREATE TABLE IF NOT EXISTS admin_mfa_recovery ("
                               "admin_id TEXT NOT NULL, code_hash TEXT PRIMARY KEY,"
                               "created_at TEXT NOT NULL,"
                               "FOREIGN KEY(admin_id) REFERENCES admins(id) ON DELETE CASCADE)")
            protected_keys = connection.execute(
                "SELECT COUNT(*) FROM licenses WHERE key_ciphertext IS NOT NULL"
            ).fetchone()[0]
        key_path = self.data_dir / "license-encryption.key"
        if not key_path.exists():
            if protected_keys:
                raise RuntimeError("Restore license-encryption.key from the data backup")
            fd = os.open(key_path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
            with os.fdopen(fd, "wb") as handle:
                handle.write(Fernet.generate_key())
        self.license_cipher = Fernet(key_path.read_bytes().strip())
        self.mfa_recovery_pepper = hashlib.sha256(key_path.read_bytes().strip() + b":mfa-recovery").digest()
        self.download_browser_sessions = {}
        self.skin_cache = {}
        self.online_probe_id = ""
        self.online_probe_expires_at = now()
        self.pending_admin_mfa = {}

    def admin_mfa_enabled(self, admin_id: str) -> bool:
        with self.connect() as connection:
            row = connection.execute("SELECT totp_secret FROM admins WHERE id=? AND status='active'",
                                     (admin_id,)).fetchone()
        return bool(row and row["totp_secret"])

    def verify_admin_mfa(self, admin_id: str, code: object, setup_secret: str | None = None) -> bool:
        current = now()
        with self.lock, self.connect() as connection:
            row = connection.execute("SELECT status,totp_secret,totp_last_counter FROM admins WHERE id=?",
                                     (admin_id,)).fetchone()
            if not row or row["status"] != "active":
                return False
            limit = connection.execute("SELECT * FROM admin_mfa_limits WHERE admin_id=?",
                                       (admin_id,)).fetchone()
            if limit and limit["locked_until"] and limit["locked_until"] > stamp(current):
                return False
            if limit and limit["window_started"] < stamp(current - timedelta(minutes=10)):
                connection.execute("DELETE FROM admin_mfa_limits WHERE admin_id=?", (admin_id,))
                limit = None
            secret = setup_secret
            if secret is None and row["totp_secret"]:
                try:
                    secret = self.license_cipher.decrypt(row["totp_secret"].encode("ascii")).decode("ascii")
                except (InvalidToken, ValueError):
                    return False
            counter = totp_counter(secret or "", code,
                                   -1 if setup_secret is not None else row["totp_last_counter"])
            recovery_hash = None
            if setup_secret is None and isinstance(code, str):
                normalized = code.upper().replace("-", "").replace(" ", "")
                if re.fullmatch(r"[A-Z2-7]{13}", normalized):
                    candidate = hmac.new(self.mfa_recovery_pepper,
                                         f"{admin_id}:{normalized}".encode("ascii"), hashlib.sha256).hexdigest()
                    if connection.execute("SELECT 1 FROM admin_mfa_recovery WHERE admin_id=? AND code_hash=?",
                                          (admin_id, candidate)).fetchone():
                        recovery_hash = candidate
            if counter is None and recovery_hash is None:
                failures = (limit["failures"] if limit else 0) + 1
                connection.execute("INSERT INTO admin_mfa_limits(admin_id,failures,window_started,locked_until) "
                                   "VALUES(?,?,?,?) ON CONFLICT(admin_id) DO UPDATE SET "
                                   "failures=excluded.failures,window_started=excluded.window_started,"
                                   "locked_until=excluded.locked_until",
                                   (admin_id, failures, limit["window_started"] if limit else stamp(current),
                                    stamp(current + timedelta(minutes=10)) if failures >= 5 else None))
                return False
            if setup_secret is not None:
                if row["totp_secret"]:
                    return False
                connection.execute("UPDATE admins SET totp_secret=?,totp_last_counter=? WHERE id=?",
                                   (self.license_cipher.encrypt(secret.encode("ascii")).decode("ascii"), counter, admin_id))
            elif recovery_hash is not None:
                connection.execute("DELETE FROM admin_mfa_recovery WHERE admin_id=? AND code_hash=?",
                                   (admin_id, recovery_hash))
            else:
                connection.execute("UPDATE admins SET totp_last_counter=? WHERE id=? AND totp_last_counter<?",
                                   (counter, admin_id, counter))
            connection.execute("DELETE FROM admin_mfa_limits WHERE admin_id=?", (admin_id,))
            return True

    def create_admin_recovery_codes(self, connection, admin_id: str) -> list[str]:
        codes = []
        connection.execute("DELETE FROM admin_mfa_recovery WHERE admin_id=?", (admin_id,))
        for _ in range(8):
            raw = base64.b32encode(secrets.token_bytes(8)).decode("ascii").rstrip("=")
            code = raw[:4] + "-" + raw[4:8] + "-" + raw[8:]
            code_hash = hmac.new(self.mfa_recovery_pepper,
                                 f"{admin_id}:{raw}".encode("ascii"), hashlib.sha256).hexdigest()
            connection.execute("INSERT INTO admin_mfa_recovery(admin_id,code_hash,created_at) VALUES(?,?,?)",
                               (admin_id, code_hash, stamp()))
            codes.append(code)
        return codes

    @staticmethod
    def admin_mfa_setup_data(name: str, secret: str) -> dict:
        import qrcode
        from qrcode.image.svg import SvgPathImage
        label = quote("unknown:" + name, safe="")
        uri = f"otpauth://totp/{label}?secret={secret}&issuer=unknown&algorithm=SHA1&digits=6&period=30"
        image = qrcode.make(uri, image_factory=SvgPathImage, box_size=6, border=3)
        output = io.BytesIO()
        image.save(output)
        return {"ok": True, "secret": secret,
                "qr": "data:image/svg+xml;base64," + base64.b64encode(output.getvalue()).decode("ascii")}

    def request_online_probe(self) -> dict:
        with self.lock:
            self.online_probe_id = secrets.token_urlsafe(18)
            self.online_probe_expires_at = now() + timedelta(seconds=15)
            return {"ok": True, "requestedAt": stamp(), "responseWindowSeconds": 15}

    def client_control_state(self, connection, device: dict, instance: object) -> dict:
        session = clean(instance, 64)
        if not re.fullmatch(r"[a-zA-Z0-9_-]{8,64}", session):
            return {}
        presence = connection.execute(
            "SELECT 1 FROM client_presence WHERE instance_id=? AND device_id=? AND status='running'",
            (session, device["id"]),
        ).fetchone()
        if not presence:
            return {}
        response = {}
        if self.online_probe_id and now() < self.online_probe_expires_at:
            response["onlineProbeId"] = self.online_probe_id
        response["closeRequested"] = connection.execute(
            "SELECT 1 FROM client_close_requests WHERE instance_id=? AND device_id=? AND expires_at>?",
            (session, device["id"], stamp()),
        ).fetchone() is not None
        return response

    def player_skin(self, player_uuid: str) -> bytes | None:
        """Return the skin PNG registered with Mojang for a known player UUID."""
        if not re.fullmatch(r"[0-9a-f]{32}", player_uuid):
            return None
        current = now()
        with self.lock:
            cached = self.skin_cache.get(player_uuid)
            if cached and cached[0] > current:
                return cached[1]
        skin = None
        try:
            profile_url = "https://sessionserver.mojang.com/session/minecraft/profile/" + player_uuid
            with urlopen(Request(profile_url, headers={"User-Agent": "unknown-admin/1"}), timeout=5) as response:
                profile = json.loads(response.read(16385))
            properties = profile.get("properties", [])
            encoded = next(value.get("value", "") for value in properties if value.get("name") == "textures")
            textures = json.loads(base64.b64decode(encoded, validate=True))
            skin_url = textures.get("textures", {}).get("SKIN", {}).get("url", "")
            match = re.fullmatch(r"https?://textures\.minecraft\.net/texture/([0-9a-f]{64})", skin_url)
            if match:
                with urlopen(Request("https://textures.minecraft.net/texture/" + match[1],
                                     headers={"User-Agent": "unknown-admin/1"}), timeout=5) as response:
                    candidate = response.read(262145)
                if len(candidate) <= 262144 and candidate.startswith(b"\x89PNG\r\n\x1a\n"):
                    skin = candidate
        except (binascii.Error, KeyError, StopIteration, TypeError, ValueError, OSError, TimeoutError):
            pass
        with self.lock:
            if len(self.skin_cache) >= 1024:
                self.skin_cache = {key: value for key, value in self.skin_cache.items() if value[0] > current}
            self.skin_cache[player_uuid] = (current + (timedelta(hours=6) if skin else timedelta(minutes=5)), skin)
        return skin

    def download_browser_principal(self, cookies: str) -> dict | None:
        token = ""
        from http.cookies import SimpleCookie
        try:
            parsed = SimpleCookie(cookies or "")
            value = parsed.get("__Host-unknown-download")
            token = value.value if value else ""
        except Exception:
            return None
        with self.lock:
            session = self.download_browser_sessions.get(hashlib.sha256(token.encode()).hexdigest())
        if not session or session[1] <= stamp():
            return None
        with self.connect() as connection:
            license = connection.execute("SELECT * FROM licenses WHERE id=?", (session[0],)).fetchone()
            if not license or license["status"] not in ("unused", "used") or (license["expires_at"] and license["expires_at"] <= stamp()):
                return None
            if license["status"] == "used":
                device = connection.execute("SELECT id,username,status,role FROM devices WHERE license_id=?", (license["id"],)).fetchone()
                if not device or device["status"] != "active":
                    return None
                return dict(device)
            return {"id": license["id"], "username": "unknown", "role": license["role"]}

    def download_browser_login(self, key: str, source_ip: str) -> tuple[str, dict] | None:
        if not key or len(key) > 128:
            return None
        with self.lock, self.connect() as connection:
            license = connection.execute("SELECT * FROM licenses WHERE id=?", (self.license_id(key),)).fetchone()
            if not license or license["status"] not in ("unused", "used") or (license["expires_at"] and license["expires_at"] <= stamp()):
                return None
            device = connection.execute("SELECT id,username,status,role FROM devices WHERE license_id=?", (license["id"],)).fetchone()
            if device and device["status"] != "active":
                return None
            principal = dict(device) if device else {"id": license["id"], "username": "unknown", "role": license["role"]}
            token = secrets.token_urlsafe(32)
            for session_key, (_, expiry) in list(self.download_browser_sessions.items()):
                if expiry <= stamp():
                    self.download_browser_sessions.pop(session_key, None)
            if len(self.download_browser_sessions) >= 2000:
                return None
            self.download_browser_sessions[hashlib.sha256(token.encode()).hexdigest()] = (license["id"], stamp(now() + timedelta(hours=2)))
            self.audit_event(connection, "download.browser.login", principal, {"licenseId": license["id"]}, source_ip)
        return token, principal

    def device_license_active(self, connection, device) -> bool:
        row = connection.execute(
            "SELECT status,expires_at,bound_device_id FROM licenses WHERE id=?", (device["license_id"],)
        ).fetchone()
        return bool(row and row["status"] == "used" and row["bound_device_id"] == device["id"]
                    and (not row["expires_at"] or row["expires_at"] > stamp()))

    def record_login(self, connection, device_id: str, source_ip: str) -> None:
        connection.execute("UPDATE devices SET last_login_at=?,last_ip=? WHERE id=?",
                           (stamp(), source_ip[:64], device_id))

    def dashboard_licenses(self) -> list[dict]:
        with self.connect() as connection:
            rows = connection.execute(
                "SELECT l.id,l.status,l.role,l.note,l.bound_device_id,l.expires_at,l.redeemed_at,"
                "l.created_at,l.key_hint,(l.key_ciphertext IS NOT NULL) AS can_copy,d.username "
                "FROM licenses l LEFT JOIN devices d ON d.id=l.bound_device_id ORDER BY l.created_at DESC"
            ).fetchall()
        return [dict(row) for row in rows]

    def copy_license(self, license_id: str, admin: dict) -> str | None:
        if not LICENSE_RE.fullmatch(license_id):
            raise ValueError("invalid_license")
        with self.lock, self.connect() as connection:
            row = connection.execute("SELECT key_ciphertext FROM licenses WHERE id=?", (license_id,)).fetchone()
            if not row or not row[0]:
                return None
            try:
                key = self.license_cipher.decrypt(row[0].encode("ascii")).decode("utf-8")
            except InvalidToken as error:
                raise ValueError("license_key_unavailable") from error
            self.audit_event(connection, "admin.license.copied", admin, {"licenseId": license_id})
            return key

    def change_license(self, license_id: str, action: str, admin: dict) -> bool:
        if not LICENSE_RE.fullmatch(license_id):
            raise ValueError("invalid_license")
        with self.lock, self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            row = connection.execute("SELECT * FROM licenses WHERE id=?", (license_id,)).fetchone()
            if not row:
                return False
            device = connection.execute("SELECT * FROM devices WHERE license_id=?", (license_id,)).fetchone()
            if action in ("disable", "delete"):
                connection.execute("UPDATE licenses SET status='disabled' WHERE id=?", (license_id,))
                if device:
                    self.party_leave(connection, dict(device))
                    connection.execute("DELETE FROM sessions WHERE device_id=?", (device["id"],))
                    connection.execute("DELETE FROM payload_tokens WHERE device_id=?", (device["id"],))
                    connection.execute("DELETE FROM download_tokens WHERE device_id=?", (device["id"],))
                    connection.execute("UPDATE client_presence SET status='revoked',stopped_at=? WHERE device_id=? AND status='running'",
                                       (stamp(), device["id"]))
            elif action == "enable":
                if row["expires_at"] and row["expires_at"] <= stamp():
                    raise ValueError("license_expired")
                connection.execute("UPDATE licenses SET status=? WHERE id=?",
                                   ("used" if device else "unused", license_id))
            else:
                raise ValueError("invalid_action")
            if action == "delete":
                # Retain the username/HWID and historical logs; the deleted key
                # can no longer authorize this account. Configs remain intact.
                connection.execute("UPDATE devices SET status='disabled' WHERE license_id=?", (license_id,))
                connection.execute("DELETE FROM licenses WHERE id=?", (license_id,))
            self.audit_event(connection, "admin.license." + {"disable": "disabled", "enable": "enabled", "delete": "deleted"}[action],
                             admin, {"licenseId": license_id, "username": device["username"] if device else None})
            return True

    def client_event(self, connection, action: str, body: dict, device: dict, source_ip: str) -> tuple[int, dict]:
        instance = clean(body.get("clientSession"), 64)
        if not re.fullmatch(r"[a-zA-Z0-9_-]{8,64}", instance):
            return 400, {"ok": False, "error": "invalid_client_event"}
        existing = connection.execute("SELECT * FROM client_presence WHERE instance_id=?", (instance,)).fetchone()
        if existing and existing["device_id"] != device["id"]:
            return 400, {"ok": False, "error": "invalid_client_event"}
        if action in ("client.injected", "client.heartbeat", "client.probe"):
            if action == "client.probe" and (clean(body.get("probeId"), 64) != self.online_probe_id
                    or not self.online_probe_id or now() >= self.online_probe_expires_at):
                return 409, {"ok": False, "error": "probe_expired"}
            player_name = clean(body.get("playerName"), 16)
            if not re.fullmatch(r"[a-zA-Z0-9_]{1,16}", player_name):
                player_name = ""
            player_uuid = clean(body.get("playerUuid"), 36).lower().replace("-", "")
            if not re.fullmatch(r"[0-9a-f]{32}", player_uuid) or player_uuid == "0" * 32:
                player_uuid = ""
            if not existing:
                if action != "client.injected":
                    return 409, {"ok": False, "error": "presence_missing"}
                # Bound the number of active instances and rows per account.
                connection.execute("DELETE FROM client_presence WHERE device_id=? AND instance_id IN "
                                   "(SELECT instance_id FROM client_presence WHERE device_id=? ORDER BY last_seen_at DESC LIMIT -1 OFFSET 20)",
                                   (device["id"], device["id"]))
                connection.execute(
                    "INSERT INTO client_presence(instance_id,device_id,version,client,java_version,started_at,last_seen_at,source_ip,status) "
                    "VALUES(?,?,?,?,?,?,?,?, 'running')",
                    (instance, device["id"], clean(body.get("version"), 32), clean(body.get("client"), 64),
                     clean(body.get("javaVersion"), 64), stamp(), stamp(), source_ip[:64]),
                )
                self.audit_event(connection, "client.injected", device,
                                 {"clientSession": instance, "version": clean(body.get("version"), 32),
                                  "client": clean(body.get("client"), 64), "javaVersion": clean(body.get("javaVersion"), 64)}, source_ip)
            elif existing["status"] == "running":
                connection.execute("UPDATE client_presence SET last_seen_at=?,source_ip=? WHERE instance_id=?",
                                   (stamp(), source_ip[:64], instance))
            else:
                return 409, {"ok": False, "error": "presence_stopped"}
            connection.execute("UPDATE client_presence SET player_name=?,player_uuid=?,supports_remote_close=? WHERE instance_id=?",
                               (player_name, player_uuid, int(body.get("supportsRemoteClose") is True), instance))
            if action == "client.probe":
                connection.execute("UPDATE client_presence SET probe_id=? WHERE instance_id=?",
                                   (self.online_probe_id, instance))
            connection.execute("UPDATE devices SET last_seen_at=?,last_ip=? WHERE id=?", (stamp(), source_ip[:64], device["id"]))
            close_requested = connection.execute(
                "SELECT 1 FROM client_close_requests WHERE instance_id=? AND device_id=? AND expires_at>?",
                (instance, device["id"], stamp()),
            ).fetchone() is not None
            return 200, {"ok": True, "role": device["role"],
                         "closeRequested": close_requested}
        if action not in ("client.stopped", "client.error", "client.crashed", "loader.failed"):
            return 400, {"ok": False, "error": "invalid_client_event"}
        details = {"clientSession": instance, "phase": clean(body.get("phase"), 64),
                   "code": clean(body.get("code"), 64), "message": diagnostic(body.get("message"), 512),
                   "stack": diagnostic(body.get("stack")), "client": clean(body.get("client"), 64),
                   "javaVersion": clean(body.get("javaVersion"), 64)}
        if action in ("client.stopped", "client.crashed") and existing:
            if existing["status"] != "running":
                return 200, {"ok": True}
            connection.execute("UPDATE client_presence SET status=?,stopped_at=?,reason=? WHERE instance_id=?",
                               ("crashed" if action == "client.crashed" else "stopped", stamp(), details["message"], instance))
            connection.execute("DELETE FROM client_close_requests WHERE instance_id=?", (instance,))
        self.audit_event(connection, action, device, details, source_ip)
        return 200, {"ok": True}

    def dashboard_users(self) -> list[dict]:
        with self.connect() as connection:
            rows = connection.execute(
                "SELECT d.id,d.username,d.status,d.role,d.license_id,d.last_ip,d.last_login_at,d.last_seen_at,d.activated_at,"
                "l.status AS license_status,l.expires_at,"
                "((SELECT COUNT(*) FROM configs c WHERE c.device_id=d.id) + "
                "(SELECT COUNT(*) FROM legacy_private_data lp,json_each(CASE WHEN json_valid(lp.payload) THEN lp.payload ELSE '{}' END,'$.profiles') j WHERE lp.device_id=d.id)) AS config_count,"
                "EXISTS(SELECT 1 FROM client_presence cp WHERE cp.device_id=d.id AND cp.status='running' AND cp.probe_id=? AND cp.probe_id<>'') AS online "
                "FROM devices d LEFT JOIN licenses l ON l.id=d.license_id ORDER BY COALESCE(d.last_login_at,d.activated_at) DESC",
                (self.online_probe_id,),
            ).fetchall()
        return [dict(row) for row in rows]

    def dashboard_online(self) -> list[dict]:
        if not self.online_probe_id:
            return []
        with self.connect() as connection:
            rows = connection.execute(
                "SELECT cp.*,d.username,d.role,"
                "EXISTS(SELECT 1 FROM client_close_requests cr WHERE cr.instance_id=cp.instance_id AND cr.expires_at>?) AS close_requested "
                "FROM client_presence cp JOIN devices d ON d.id=cp.device_id "
                "JOIN licenses l ON l.id=d.license_id WHERE cp.status='running' AND cp.probe_id=? "
                "AND d.status='active' AND l.status='used' AND (l.expires_at IS NULL OR l.expires_at>?) ORDER BY cp.started_at DESC",
                (stamp(), self.online_probe_id, stamp()),
            ).fetchall()
        return [dict(row) for row in rows]

    def request_client_close(self, instance_id: str, admin: dict) -> dict | None:
        if not re.fullmatch(r"[a-zA-Z0-9_-]{8,64}", instance_id):
            return None
        with self.lock, self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            row = connection.execute(
                "SELECT cp.device_id,d.username FROM client_presence cp "
                "JOIN devices d ON d.id=cp.device_id JOIN licenses l ON l.id=d.license_id "
                "WHERE cp.instance_id=? AND cp.status='running' AND cp.supports_remote_close=1 AND cp.probe_id=? AND cp.last_seen_at>=? "
                "AND d.status='active' AND l.status='used' AND (l.expires_at IS NULL OR l.expires_at>?)",
                (instance_id, self.online_probe_id, stamp(now() - timedelta(seconds=ONLINE_SECONDS)), stamp()),
            ).fetchone()
            if not row:
                return None
            connection.execute("DELETE FROM client_close_requests WHERE expires_at<=?", (stamp(),))
            connection.execute(
                "INSERT INTO client_close_requests(instance_id,device_id,requested_at,expires_at,admin_id) "
                "VALUES(?,?,?,?,?) ON CONFLICT(instance_id) DO UPDATE SET requested_at=excluded.requested_at,"
                "expires_at=excluded.expires_at,admin_id=excluded.admin_id",
                (instance_id, row["device_id"], stamp(), stamp(now() + timedelta(minutes=2)), admin["id"]),
            )
            self.audit_event(connection, "admin.client.close_requested", admin,
                             {"clientSession": instance_id, "deviceId": row["device_id"],
                              "username": row["username"]})
            return {"ok": True, "clientSession": instance_id}

    def dashboard_overview(self) -> dict:
        today = now().replace(hour=0, minute=0, second=0, microsecond=0)
        start = today - timedelta(days=6)
        with self.connect() as connection:
            counts = {"users": connection.execute("SELECT COUNT(*) FROM devices").fetchone()[0],
                      "licenses": connection.execute("SELECT COUNT(*) FROM licenses").fetchone()[0],
                      "availableLicenses": connection.execute("SELECT COUNT(*) FROM licenses WHERE status='unused' AND (expires_at IS NULL OR expires_at>?)", (stamp(),)).fetchone()[0],
                      "configs": connection.execute("SELECT (SELECT COUNT(*) FROM configs) + (SELECT COUNT(*) FROM legacy_private_data lp,json_each(CASE WHEN json_valid(lp.payload) THEN lp.payload ELSE '{}' END,'$.profiles'))").fetchone()[0],
                      "communityConfigs": connection.execute("SELECT COUNT(*) FROM community_configs").fetchone()[0],
                      "openParties": connection.execute("SELECT COUNT(*) FROM parties WHERE status='open'").fetchone()[0],
                      "injectionsToday": connection.execute("SELECT COUNT(*) FROM audit_events WHERE event_type='client.injected' AND created_at>=?", (stamp(today),)).fetchone()[0],
                      "errorsToday": connection.execute("SELECT COUNT(*) FROM audit_events WHERE event_type IN ('loader.failed','client.crashed','client.error') AND created_at>=?", (stamp(today),)).fetchone()[0]}
            counts["onlineUsers"] = len({row["device_id"] for row in self.dashboard_online()})
            daily = connection.execute(
                "SELECT substr(created_at,1,10) AS day,event_type,COUNT(*) AS count FROM audit_events "
                "WHERE created_at>=? AND event_type IN ('loader.opened','client.injected','config.created','config.saved','config.loaded','client.crashed','loader.failed') "
                "GROUP BY day,event_type", (stamp(start),)
            ).fetchall()
        activity = []
        for offset in range(7):
            day = (start + timedelta(days=offset)).date().isoformat()
            activity.append({"day": day, "opened": sum(row["count"] for row in daily if row["day"] == day and row["event_type"] == "loader.opened"),
                             "used": sum(row["count"] for row in daily if row["day"] == day and row["event_type"] == "client.injected"),
                             "configs": sum(row["count"] for row in daily if row["day"] == day and row["event_type"].startswith("config."))})
        return {"ok": True, "counts": counts, "activity": activity, "generatedAt": stamp(), "onlineTimeoutSeconds": ONLINE_SECONDS}

    def dashboard_audit(self, query: dict) -> dict:
        try:
            limit = min(200, max(1, int(query.get("limit", ["100"])[0])))
        except ValueError:
            limit = 100
        event, search, before = clean(query.get("event", [""])[0], 64), clean(query.get("q", [""])[0], 128), query.get("before", [""])[0]
        where, args = [], []
        if event == "errors":
            where.append("event_type IN ('client.error','client.crashed','loader.failed')")
        elif event:
            where.append("event_type LIKE ?")
            args.append(event + ("%" if "." not in event else ""))
        if search:
            where.append("(actor_username LIKE ? OR actor_device_id LIKE ? OR source_ip LIKE ? OR details LIKE ?)")
            args.extend(["%" + search + "%"] * 4)
        if before.isdigit():
            where.append("id<?")
            args.append(int(before))
        with self.connect() as connection:
            rows = connection.execute("SELECT * FROM audit_events" + (" WHERE " + " AND ".join(where) if where else "") + " ORDER BY id DESC LIMIT ?", (*args, limit + 1)).fetchall()
        events = []
        for row in rows[:limit]:
            entry = dict(row)
            try:
                entry["details"] = json.loads(entry["details"])
            except (ValueError, TypeError):
                entry["details"] = {"message": "Registro legado incompleto"}
            events.append(entry)
        return {"ok": True, "events": events, "nextCursor": str(events[-1]["id"]) if len(rows) > limit else None}

    @staticmethod
    def visible_roles(role: str) -> tuple:
        return ROLES[:ROLES.index(role) + 1] if role in ROLES else ("user",)

    def catalog(self, role: str | None = None) -> list[dict]:
        with self.connect() as connection:
            rows = connection.execute("SELECT * FROM downloads ORDER BY updated_at DESC").fetchall()
        return [dict(row) for row in rows if role is None or (row["enabled"] and row["uploaded"] and row["role"] in self.visible_roles(role))]

    def catalog_path(self, download_id: str) -> Path:
        if not DOWNLOAD_RE.fullmatch(download_id):
            raise ValueError("invalid_download")
        return self.download_dir / (download_id + ".bin")

    def selected_payload(self, role: str) -> Path | None:
        with self.connect() as connection:
            # A tester uses the tester binary, then user. Developers use their
            # binary, then user; users never receive privileged binaries.
            for track in (role, "user") if role != "user" else ("user",):
                rows = connection.execute("SELECT id FROM downloads WHERE kind='dll' AND role=? AND enabled=1 AND uploaded=1 ORDER BY updated_at DESC", (track,)).fetchall()
                for row in rows:
                    path = self.catalog_path(row["id"])
                    if path.is_file():
                        return path
            if connection.execute("SELECT 1 FROM downloads WHERE kind='dll' LIMIT 1").fetchone():
                return None
        preferred = self.payload_dir / ("developer.dll" if role == "developer" else "tester.dll" if role == "tester" else "release.dll")
        fallback = self.payload_dir / "release.dll"
        return preferred if preferred.is_file() else fallback if fallback.is_file() else None

    def consume_client_payload_token(self, payload_id: str, token: str, source_ip: str) -> Path | None:
        if not DOWNLOAD_RE.fullmatch(payload_id) or not token or len(token) > 128:
            return None
        with self.lock, self.connect() as connection:
            row = connection.execute("SELECT * FROM payload_tokens WHERE id=?", (hashlib.sha256(token.encode()).hexdigest(),)).fetchone()
            if not row or row["payload_id"] != payload_id:
                return None
            connection.execute("DELETE FROM payload_tokens WHERE id=?", (row["id"],))
            device = connection.execute("SELECT * FROM devices WHERE id=?", (row["device_id"],)).fetchone()
            item = connection.execute("SELECT * FROM downloads WHERE id=?", (payload_id,)).fetchone()
            path = self.catalog_path(payload_id)
            if (row["expires_at"] <= stamp() or not device or device["status"] != "active"
                    or not self.device_license_active(connection, device) or not item or not item["enabled"]
                    or item["kind"] != "dll" or item["role"] not in self.visible_roles(device["role"])
                    or not path.is_file()):
                return None
            self.audit_event(connection, "payload.downloaded", device,
                             {"downloadId": payload_id, "name": item["name"], "version": item["version"], "bytes": item["size"]}, source_ip)
            return path

    def create_download(self, body: dict, admin: dict) -> dict:
        name, kind, role = clean(body.get("name"), 80), body.get("kind"), body.get("role")
        filename = clean(body.get("filename"), 120)
        if not name or kind not in KINDS or role not in ROLES:
            raise ValueError("invalid_download_metadata")
        if not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9._ -]{0,119}", filename):
            raise ValueError("invalid_filename")
        if (kind == "dll" and not filename.lower().endswith(".dll")) or (kind == "loader" and not filename.lower().endswith(".exe")):
            raise ValueError("invalid_file_type")
        row_id = "dl_" + secrets.token_hex(16)
        with self.lock, self.connect() as connection:
            if connection.execute("SELECT COUNT(*) FROM downloads WHERE uploaded=0").fetchone()[0] >= 30:
                raise ValueError("too_many_pending_uploads")
            connection.execute("INSERT INTO downloads(id,name,kind,role,version,filename,notes,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)",
                               (row_id, name, kind, role, clean(body.get("version"), 40), filename, clean(body.get("notes"), 400), stamp(), stamp()))
            self.audit_event(connection, "admin.download.created", admin, {"downloadId": row_id, "name": name, "role": role, "kind": kind})
        return {"ok": True, "id": row_id}

    def write_download(self, download_id: str, data: bytes, start: int, end: int, total: int, admin: dict) -> dict:
        path = self.catalog_path(download_id)
        partial = path.with_suffix(".part")
        with self.lock, self.connect() as connection:
            row = connection.execute("SELECT * FROM downloads WHERE id=?", (download_id,)).fetchone()
            if not row or row["uploaded"]:
                raise ValueError("upload_unavailable")
            maximum = APPLICATION_LIMIT if row["kind"] == "application" else CLIENT_LIMIT
            if total < 1 or total > maximum or end < start or end >= total or len(data) != end - start + 1:
                raise ValueError("invalid_upload_size")
            if row["size"] not in (0, total):
                raise ValueError("upload_size_changed")
            if start != (partial.stat().st_size if partial.exists() else 0):
                raise ValueError("upload_offset_mismatch")
            if start == 0 and shutil.disk_usage(self.download_dir).free < total + 256 * 1024 * 1024:
                raise ValueError("insufficient_storage")
            with partial.open("ab") as handle:
                handle.write(data)
            connection.execute("UPDATE downloads SET size=?,updated_at=? WHERE id=?", (total, stamp(), download_id))
            complete = end + 1 == total
            if complete:
                with partial.open("rb") as handle:
                    if row["kind"] in ("loader", "dll") and (total < 1024 or handle.read(2) != b"MZ"):
                        partial.unlink()
                        connection.execute("UPDATE downloads SET size=0 WHERE id=?", (download_id,))
                        raise ValueError("invalid_pe_file")
                    handle.seek(0)
                    sha = hashlib.file_digest(handle, "sha256").hexdigest()
                partial.replace(path)
                if row["kind"] == "dll":
                    connection.execute("UPDATE downloads SET enabled=0 WHERE kind='dll' AND role=? AND id<>?", (row["role"], download_id))
                connection.execute("UPDATE downloads SET uploaded=1,enabled=1,sha256=?,updated_at=? WHERE id=?", (sha, stamp(), download_id))
                self.audit_event(connection, "admin.download.published", admin, {"downloadId": download_id, "name": row["name"], "role": row["role"], "bytes": total, "sha256": sha})
        return {"ok": True, "id": download_id, "received": end + 1, "complete": complete}

    def update_download(self, download_id: str, body: dict, admin: dict) -> bool:
        self.catalog_path(download_id)
        values = {}
        for field in ("name", "role", "version", "notes", "enabled"):
            if field in body:
                if field == "role" and body[field] not in ROLES:
                    raise ValueError("invalid_role")
                if field == "enabled" and not isinstance(body[field], bool):
                    raise ValueError("invalid_status")
                values[field] = int(body[field]) if field == "enabled" else clean(body[field], 400 if field == "notes" else 80)
        if not values or ("name" in values and not values["name"]):
            raise ValueError("no_changes")
        with self.lock, self.connect() as connection:
            row = connection.execute("SELECT * FROM downloads WHERE id=?", (download_id,)).fetchone()
            if row and row["kind"] == "dll" and row["uploaded"] and values.get("enabled", row["enabled"]):
                connection.execute("UPDATE downloads SET enabled=0 WHERE kind='dll' AND role=? AND id<>?", (values.get("role", row["role"]), download_id))
            changed = connection.execute("UPDATE downloads SET " + ",".join(field + "=?" for field in values) + ",updated_at=? WHERE id=?",
                                         (*values.values(), stamp(), download_id)).rowcount
            if changed:
                self.audit_event(connection, "admin.download.updated", admin, {"downloadId": download_id, **values})
        return bool(changed)

    def delete_download(self, download_id: str, admin: dict) -> bool:
        path = self.catalog_path(download_id)
        with self.lock, self.connect() as connection:
            row = connection.execute("SELECT name,role,kind FROM downloads WHERE id=?", (download_id,)).fetchone()
            if not row:
                return False
            connection.execute("DELETE FROM downloads WHERE id=?", (download_id,))
            self.audit_event(connection, "admin.download.deleted", admin,
                             {"downloadId": download_id, "name": row["name"], "role": row["role"], "kind": row["kind"]})
            path.unlink(missing_ok=True)
            path.with_suffix(".part").unlink(missing_ok=True)
        return True

    def delete_downloads(self, download_ids: object, admin: dict) -> dict:
        if not isinstance(download_ids, list) or not 1 <= len(download_ids) <= 200:
            raise ValueError("invalid_download_selection")
        if any(not isinstance(download_id, str) or not DOWNLOAD_RE.fullmatch(download_id) for download_id in download_ids):
            raise ValueError("invalid_download_selection")
        ids = list(dict.fromkeys(download_ids))
        placeholders = ",".join("?" for _ in ids)
        with self.lock, self.connect() as connection:
            rows = connection.execute(
                f"SELECT id,name,role,kind FROM downloads WHERE id IN ({placeholders})", ids).fetchall()
            if len(rows) != len(ids):
                raise ValueError("not_found")
            for row in rows:
                connection.execute("DELETE FROM downloads WHERE id=?", (row["id"],))
                self.audit_event(connection, "admin.download.deleted", admin,
                                 {"downloadId": row["id"], "name": row["name"],
                                  "role": row["role"], "kind": row["kind"]})
        files_not_removed = []
        for download_id in ids:
            path = self.catalog_path(download_id)
            try:
                path.unlink(missing_ok=True)
                path.with_suffix(".part").unlink(missing_ok=True)
            except OSError:
                files_not_removed.append(download_id)
        return {"ok": True, "deleted": ids, "filesNotRemoved": files_not_removed}

    def authorize_download(self, connection, download_id: str, device: dict) -> dict | None:
        if not DOWNLOAD_RE.fullmatch(download_id):
            return None
        row = connection.execute("SELECT * FROM downloads WHERE id=? AND enabled=1 AND uploaded=1", (download_id,)).fetchone()
        if not row or row["role"] not in self.visible_roles(device["role"]) or not self.catalog_path(download_id).is_file():
            return None
        token = secrets.token_urlsafe(32)
        connection.execute("DELETE FROM download_tokens WHERE expires_at<=? OR (device_id=? AND download_id=?)", (stamp(), device["id"], download_id))
        connection.execute("INSERT INTO download_tokens VALUES(?,?,?,?)", (hashlib.sha256(token.encode()).hexdigest(), download_id, device["id"], stamp(now() + timedelta(minutes=2))))
        return {"id": download_id, "url": "/v1/downloads/" + download_id + "/file?token=" + quote(token),
                "filename": row["filename"], "sha256": row["sha256"], "bytes": row["size"]}

    def consume_catalog_token(self, download_id: str, token: str, source_ip: str) -> dict | None:
        if not DOWNLOAD_RE.fullmatch(download_id) or len(token) > 128:
            return None
        with self.lock, self.connect() as connection:
            row = connection.execute("SELECT dt.device_id,dt.expires_at,dl.* FROM download_tokens dt JOIN downloads dl ON dl.id=dt.download_id WHERE dt.id=? AND dt.download_id=?",
                                     (hashlib.sha256(token.encode()).hexdigest(), download_id)).fetchone()
            if not row:
                return None
            connection.execute("DELETE FROM download_tokens WHERE id=?", (hashlib.sha256(token.encode()).hexdigest(),))
            device = connection.execute("SELECT * FROM devices WHERE id=?", (row["device_id"],)).fetchone()
            if (row["expires_at"] <= stamp() or not row["enabled"] or not row["uploaded"] or not device
                    or device["status"] != "active" or not self.device_license_active(connection, device)
                    or row["role"] not in self.visible_roles(device["role"])):
                return None
            self.audit_event(connection, "download.downloaded", device, {"downloadId": download_id, "name": row["name"]}, source_ip)
            return dict(row)


class AdminRequestFeatures:
    def send_download_file(self, path: Path, filename: str) -> None:
        if not path.is_file():
            self.send_json(404, {"ok": False, "error": "download_unavailable"})
            return
        # Stream applications to avoid allocating hundreds of MB per request.
        with path.open("rb") as handle:
            self.send_response(200)
            self.send_header("Content-Type", "application/octet-stream")
            self.send_header("Content-Length", str(os.fstat(handle.fileno()).st_size))
            self.send_header("Content-Disposition", "attachment; filename*=UTF-8''" + quote(filename))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.send_header("Referrer-Policy", "no-referrer")
            self.send_header("Strict-Transport-Security", "max-age=31536000")
            self.end_headers()
            try:
                shutil.copyfileobj(handle, self.wfile, 128 * 1024)
            except (BrokenPipeError, ConnectionResetError):
                pass

    def feature_get(self, parsed) -> bool:
        if parsed.path == "/v1/admin/mfa/setup":
            if not self.rate_limit("admin-mfa-setup", 12, 60):
                return True
            setup = self.backend.admin_mfa_setup(self.headers.get("Cookie"))
            self.send_json(200 if setup else 401, setup or {"ok": False, "error": "unauthorized"})
            return True
        assets = {"/admin/assets/unknown-logo.png": ("unknown-logo.png", "image/png"),
                  "/admin/assets/dashboard.css": ("dashboard.css", "text/css; charset=utf-8"),
                  "/admin/assets/dashboard.js": ("dashboard.js", "text/javascript; charset=utf-8"),
                  "/admin/assets/downloads.js": ("downloads.js", "text/javascript; charset=utf-8"),
                  "/admin/assets/space-mono-regular.ttf": ("space-mono-regular.ttf", "font/ttf"),
                  "/admin/assets/space-mono-bold.ttf": ("space-mono-bold.ttf", "font/ttf")}
        if parsed.path in assets:
            filename, mime = assets[parsed.path]
            path = Path(__file__).parent / "admin_assets" / filename
            if path.is_file():
                self.send_bytes(200, path.read_bytes(), mime)
            else:
                self.send_json(404, {"ok": False, "error": "asset_unavailable"})
            return True
        if parsed.path in ("/downloads", "/downloads/"):
            self.send_bytes(200, (Path(__file__).parent / "downloads.html").read_bytes(), "text/html; charset=utf-8")
            return True
        if parsed.path in ("/downloads/auxiliary", "/downloads/winrar"):
            if not self.rate_limit("auxiliary-download", 30, 60):
                return True
            principal = self.backend.download_browser_principal(self.headers.get("Cookie", ""))
            authorization = self.headers.get("Authorization", "")
            if not principal and authorization.startswith("Bearer "):
                principal = self.backend.mapping_device(authorization[7:])
            if not principal:
                self.send_json(401, {"ok": False, "error": "license_required"})
                return True
            applications = [item for item in self.backend.catalog(principal["role"])
                            if item["kind"] == "application"]
            item = next((entry for role in reversed(self.backend.visible_roles(principal["role"]))
                         for entry in applications if entry["role"] == role), None)
            if not item:
                self.send_json(404, {"ok": False, "error": "download_unavailable"})
            else:
                with self.backend.connect() as connection:
                    self.backend.audit_event(connection, "download.downloaded", principal,
                                             {"downloadId": item["id"], "name": item["name"]}, self.source_ip)
                self.send_download_file(self.backend.catalog_path(item["id"]), item["filename"])
            return True
        skin_match = re.fullmatch(r"/v1/admin/skins/([0-9a-f]{32})\.png", parsed.path)
        if skin_match:
            if not self.rate_limit("admin-skin", 120, 60) or not self.admin():
                return True
            skin = self.backend.player_skin(skin_match[1])
            if skin:
                self.send_bytes(200, skin, "image/png")
            else:
                self.send_json(404, {"ok": False, "error": "skin_unavailable"})
            return True
        browser_file = re.fullmatch(r"/v1/downloads/(dl_[0-9a-f]{32})/browser-file", parsed.path)
        if parsed.path == "/v1/downloads/catalog" or browser_file:
            if not self.rate_limit("download-browser", 120, 60):
                return True
            principal = self.backend.download_browser_principal(self.headers.get("Cookie", ""))
            if not principal:
                self.send_json(401, {"ok": False, "error": "license_required"})
                return True
            items = self.backend.catalog(principal["role"])
            if browser_file:
                item = next((item for item in items if item["id"] == browser_file[1]), None)
                if not item:
                    self.send_json(403, {"ok": False, "error": "download_unavailable"})
                else:
                    with self.backend.connect() as connection:
                        self.backend.audit_event(connection, "download.downloaded", principal, {"downloadId": item["id"], "name": item["name"]}, self.source_ip)
                    self.send_download_file(self.backend.catalog_path(item["id"]), item["filename"])
            else:
                self.send_json(200, {"ok": True, "user": principal, "downloads": items})
            return True
        if parsed.path.startswith("/v1/admin/"):
            endpoints = {"/v1/admin/overview": (None, self.backend.dashboard_overview),
                         "/v1/admin/licenses": ("licenses", self.backend.dashboard_licenses),
                         "/v1/admin/users": ("users", self.backend.dashboard_users),
                         "/v1/admin/online": ("clients", self.backend.dashboard_online),
                         "/v1/admin/downloads": ("downloads", self.backend.catalog),
                         "/v1/admin/audit": (None, lambda: self.backend.dashboard_audit(parse_qs(parsed.query)))}
            match = re.fullmatch(r"/v1/admin/downloads/(dl_[0-9a-f]{32})/file", parsed.path)
            if parsed.path in endpoints or match:
                if not self.rate_limit("admin-read", 180, 60) or not self.admin():
                    return True
                if match:
                    row = next((r for r in self.backend.catalog() if r["id"] == match[1]), None)
                    if not row or not row["uploaded"]:
                        self.send_json(404, {"ok": False, "error": "download_unavailable"})
                    else:
                        self.send_download_file(self.backend.catalog_path(row["id"]), row["filename"])
                else:
                    key, operation = endpoints[parsed.path]
                    result = operation()
                    self.send_json(200, {"ok": True, key: result} if key else result)
                return True
        match = re.fullmatch(r"/v1/downloads/(dl_[0-9a-f]{32})/file", parsed.path)
        if match:
            if not self.rate_limit("catalog-download", 60, 60):
                return True
            row = self.backend.consume_catalog_token(match[1], parse_qs(parsed.query).get("token", [""])[0], self.source_ip)
            if row:
                self.send_download_file(self.backend.catalog_path(row["id"]), row["filename"])
            else:
                self.send_json(403, {"ok": False, "error": "invalid_download_token"})
            return True
        return False

    def feature_post(self, parsed) -> bool:
        if parsed.path == "/v1/admin/mfa/cancel":
            if not self.rate_limit("admin-mfa-cancel", 12, 60) or not self.require_json():
                return True
            if self.headers.get("X-SonyVegas-Admin") != "1" or not self.same_origin():
                self.send_json(403, {"ok": False, "error": "invalid_origin"})
                return True
            self.read_json()
            self.backend.revoke_admin_session(self.headers.get("Cookie"))
            self.send_json(200, {"ok": True}, {"Set-Cookie": [
                "sv_admin=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict",
                "__Host-sv_admin=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict"]})
            return True
        if parsed.path == "/v1/admin/mfa/confirm":
            if not self.rate_limit("admin-mfa-confirm", 6, 300) or not self.require_json():
                return True
            if self.headers.get("X-SonyVegas-Admin") != "1" or not self.same_origin():
                self.send_json(403, {"ok": False, "error": "invalid_origin"})
                return True
            confirmation = self.backend.confirm_admin_mfa(self.headers.get("Cookie"), self.read_json().get("code"))
            self.send_json(200 if confirmation else 400,
                           {"ok": True, **confirmation} if confirmation else {"ok": False, "error": "invalid_mfa_code"})
            return True
        if parsed.path == "/v1/admin/online/probe":
            if not self.rate_limit("admin-probe", 20, 60) or not self.admin_mutation() or not self.require_json():
                return True
            self.read_json()
            self.send_json(202, self.backend.request_online_probe())
            return True
        close_request = re.fullmatch(r"/v1/admin/online/([a-zA-Z0-9_-]{8,64})/close", parsed.path)
        if close_request:
            if not self.rate_limit("admin-write", 60, 60) or not self.owner_mutation() or not self.require_json():
                return True
            self.read_json()
            result = self.backend.request_client_close(close_request[1], self._admin_identity)
            self.send_json(202 if result else 404,
                           result or {"ok": False, "error": "session_not_online"})
            return True
        if parsed.path in ("/v1/downloads/login", "/v1/downloads/logout"):
            if not self.rate_limit("download-login", 12, 60) or not self.require_json():
                return True
            if not self.same_origin():
                self.send_json(403, {"ok": False, "error": "invalid_origin"})
                return True
            body = self.read_json()
            if parsed.path.endswith("/logout"):
                from http.cookies import SimpleCookie
                try:
                    cookie = SimpleCookie(self.headers.get("Cookie", ""))
                    token = cookie.get("__Host-unknown-download")
                    with self.backend.lock:
                        if token:
                            self.backend.download_browser_sessions.pop(hashlib.sha256(token.value.encode()).hexdigest(), None)
                except Exception:
                    pass
                self.send_json(200, {"ok": True}, {"Set-Cookie": "__Host-unknown-download=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=Strict"})
                return True
            login = self.backend.download_browser_login(clean(body.get("licenseKey"), 129), self.source_ip)
            if not login:
                self.send_json(401, {"ok": False, "error": "invalid_license"})
            else:
                token, principal = login
                self.send_json(200, {"ok": True, "user": principal}, {"Set-Cookie": f"__Host-unknown-download={token}; Path=/; Max-Age=7200; Secure; HttpOnly; SameSite=Strict"})
            return True
        license = re.fullmatch(r"/v1/admin/licenses/(lc_[0-9a-f]{32})/(copy|disable|enable|delete)", parsed.path)
        download = re.fullmatch(r"/v1/admin/downloads/(dl_[0-9a-f]{32})(/delete)?", parsed.path)
        bulk_delete = parsed.path == "/v1/admin/downloads/bulk-delete"
        if not license and not download and parsed.path != "/v1/admin/downloads" and not bulk_delete:
            return False
        if not self.rate_limit("admin-write", 60, 60) or not self.admin_mutation() or not self.require_json():
            return True
        body = self.read_json()
        if license:
            if license[2] == "copy":
                key = self.backend.copy_license(license[1], self._admin_identity)
                self.send_json(200 if key else 409, {"ok": bool(key), "licenseKey": key, "error": None if key else "license_key_unavailable"})
            else:
                changed = self.backend.change_license(license[1], license[2], self._admin_identity)
                self.send_json(200 if changed else 404, {"ok": changed, "error": None if changed else "not_found"})
        elif bulk_delete:
            try:
                self.send_json(200, self.backend.delete_downloads(body.get("ids"), self._admin_identity))
            except ValueError as error:
                self.send_json(400, {"ok": False, "error": str(error)})
        elif download:
            changed = (self.backend.delete_download(download[1], self._admin_identity) if download[2]
                       else self.backend.update_download(download[1], body, self._admin_identity))
            self.send_json(200 if changed else 404, {"ok": changed, "error": None if changed else "not_found"})
        else:
            self.send_json(201, self.backend.create_download(body, self._admin_identity))
        return True

    def feature_put(self, parsed) -> bool:
        match = re.fullmatch(r"/v1/admin/downloads/(dl_[0-9a-f]{32})/file", parsed.path)
        if not match:
            return False
        if not self.rate_limit("catalog-upload", 180, 60) or not self.admin_mutation():
            return True
        try:
            content_range = re.fullmatch(r"bytes (\d+)-(\d+)/(\d+)", self.headers.get("Content-Range", ""))
            if not content_range:
                raise ValueError("content_range_required")
            result = self.backend.write_download(match[1], self.read_body(5 * 1024 * 1024),
                                                 *map(int, content_range.groups()), self._admin_identity)
            self.send_json(200 if result["complete"] else 202, result)
        except OverflowError:
            self.send_json(413, {"ok": False, "error": "upload_chunk_too_large"})
        except ValueError as error:
            self.send_json(400, {"ok": False, "error": str(error)})
        except OSError:
            self.send_json(507, {"ok": False, "error": "storage_unavailable"})
        return True
