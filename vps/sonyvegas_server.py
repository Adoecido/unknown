#!/usr/bin/env python3
"""Standalone SonyVegas control API for a single persistent Linux VM.

The public loader endpoint intentionally keeps the small legacy execution
envelope during the migration.  Existing clients therefore need only a host
change while the database, sessions and payload storage are fully local.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import ipaddress
import json
import math
import os
import re
import secrets
import sqlite3
import threading
import time
import uuid
from collections import deque
from contextlib import contextmanager
from datetime import datetime, timedelta, timezone
from http import HTTPStatus
from http.cookies import SimpleCookie
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, unquote, urlsplit
from admin_features import AdminBackendFeatures, AdminRequestFeatures, ROLES, DOWNLOAD_RE

# Signed license support (pure stdlib RSA/SHA-256). The private key lives only
# in vps/license_private.json (gitignored) on the operator/VPS machine.
try:
    from license_crypto import load_key, sign_license_payload
except ImportError:  # pragma: no cover - defensive only
    load_key = None
    sign_license_payload = None


MAX_JSON_BYTES = 1024 * 1024
MAX_CONFIG_BYTES = 512 * 1024
MAX_COMMAND_BYTES = 16 * 1024
MAX_COMMUNITY_DESCRIPTION = 160
MAX_COMMUNITY_CONFIGS_PER_USER = 20
MAX_PAYLOAD_BYTES = 32 * 1024 * 1024
MAX_MAPPING_PATCH_BYTES = 16 * 1024 * 1024
SESSION_LIFETIME = timedelta(hours=12)
PAYLOAD_TOKEN_LIFETIME = timedelta(minutes=2)
ADMIN_SESSION_LIFETIME = timedelta(hours=4)
ADMIN_TICKET_LIFETIME = timedelta(minutes=1)
AUDIT_RETENTION = timedelta(days=180)
HWID_RE = re.compile(r"^hw_[0-9a-f]{32}$")
PARTY_CODE_RE = re.compile(r"^[A-Z0-9]{6}$")
COMMUNITY_CONFIG_RE = re.compile(r"^cc_[0-9a-f]{32}$")


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


def iso(value: datetime | None = None) -> str:
    return (value or utcnow()).isoformat().replace("+00:00", "Z")


def parse_time(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
        return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
    except (TypeError, ValueError):
        return None


def digest(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def safe_field(value: object, maximum: int) -> bool:
    return (
        isinstance(value, str)
        and 0 < len(value) <= maximum
        and not any(char in value for char in "\r\n\t|")
    )


def json_bytes(value: object) -> bytes:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


class SlidingWindowRateLimiter:
    """Small in-process limiter for the single-service VM deployment."""

    def __init__(self) -> None:
        self.lock = threading.Lock()
        self.buckets: dict[str, tuple[deque[float], int]] = {}
        self.checks = 0

    def allow(self, key: str, limit: int, window_seconds: int) -> tuple[bool, int]:
        now = time.monotonic()
        cutoff = now - window_seconds
        with self.lock:
            events, _ = self.buckets.setdefault(key, (deque(), window_seconds))
            while events and events[0] <= cutoff:
                events.popleft()
            if len(events) >= limit:
                return False, max(1, math.ceil(window_seconds - (now - events[0])))
            events.append(now)
            self.buckets[key] = (events, window_seconds)
            self.checks += 1
            if self.checks % 2048 == 0:
                for bucket_key, (bucket_events, bucket_window) in list(self.buckets.items()):
                    bucket_cutoff = now - bucket_window
                    while bucket_events and bucket_events[0] <= bucket_cutoff:
                        bucket_events.popleft()
                    if not bucket_events:
                        self.buckets.pop(bucket_key, None)
            return True, 0


class SonyVegasBackend(AdminBackendFeatures):
    def __init__(self, data_dir: str | Path, admin_token: str):
        self.data_dir = Path(data_dir)
        self.payload_dir = self.data_dir / "payloads"
        self.mapping_dir = self.data_dir / "mappings"
        self.database_path = self.data_dir / "sonyvegas.sqlite3"
        self.admin_token = admin_token
        # Scoped publisher credential: never expose the administrative token to the bot.
        self.invsee_bot_token = os.environ.get("SONYVEGAS_INVSEE_BOT_TOKEN", "")
        self.lock = threading.RLock()
        self.admin_sessions: dict[str, tuple[datetime, dict[str, str]]] = {}
        self.admin_tickets: dict[str, tuple[datetime, dict[str, str]]] = {}
        self.data_dir.mkdir(parents=True, exist_ok=True)
        self.payload_dir.mkdir(parents=True, exist_ok=True)
        (self.mapping_dir / "incoming").mkdir(parents=True, exist_ok=True)
        (self.mapping_dir / "profiles").mkdir(parents=True, exist_ok=True)
        self._initialize()
        self.initialize_admin_features()

    @contextmanager
    def connect(self):
        connection = sqlite3.connect(self.database_path, timeout=15)
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA foreign_keys = ON")
        connection.execute("PRAGMA busy_timeout = 15000")
        try:
            with connection:
                yield connection
        finally:
            connection.close()

    def _initialize(self) -> None:
        schema = """
        PRAGMA journal_mode = WAL;
        CREATE TABLE IF NOT EXISTS licenses (
            id TEXT PRIMARY KEY,
            status TEXT NOT NULL,
            role TEXT NOT NULL,
            note TEXT,
            bound_device_id TEXT,
            expires_at TEXT,
            redeemed_at TEXT,
            created_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS devices (
            id TEXT PRIMARY KEY,
            username TEXT NOT NULL,
            status TEXT NOT NULL,
            role TEXT NOT NULL,
            license_id TEXT NOT NULL,
            secret_hash TEXT,
            last_seen_at TEXT,
            activated_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS sessions (
            id TEXT PRIMARY KEY,
            device_id TEXT NOT NULL,
            expires_at TEXT NOT NULL,
            kind TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS sessions_device_idx ON sessions(device_id);
        CREATE TABLE IF NOT EXISTS invsee_links (
            device_id TEXT PRIMARY KEY REFERENCES devices(id) ON DELETE CASCADE,
            code_hash TEXT UNIQUE,
            code_expires_at TEXT,
            guild_id TEXT,
            channel_id TEXT,
            payload TEXT,
            expires_at TEXT
        );
        CREATE INDEX IF NOT EXISTS invsee_ticket_idx ON invsee_links(guild_id,channel_id);
        CREATE TABLE IF NOT EXISTS cape_presence (
            device_id TEXT PRIMARY KEY,
            server_key TEXT NOT NULL,
            player_name TEXT NOT NULL,
            style TEXT NOT NULL,
            last_seen_at TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS cape_presence_server_idx
            ON cape_presence(server_key, last_seen_at);
        CREATE TABLE IF NOT EXISTS payload_tokens (
            id TEXT PRIMARY KEY,
            payload_id TEXT NOT NULL,
            device_id TEXT,
            expires_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS configs (
            id TEXT PRIMARY KEY,
            device_id TEXT NOT NULL,
            slot INTEGER NOT NULL,
            name TEXT NOT NULL,
            payload TEXT NOT NULL,
            version INTEGER NOT NULL,
            updated_at TEXT NOT NULL,
            UNIQUE(device_id, slot)
        );
        CREATE TABLE IF NOT EXISTS legacy_private_data (
            device_id TEXT PRIMARY KEY,
            payload TEXT NOT NULL,
            updated_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS community_configs (
            id TEXT PRIMARY KEY,
            owner_device_id TEXT NOT NULL,
            owner_username TEXT NOT NULL,
            name TEXT NOT NULL,
            description TEXT NOT NULL,
            payload TEXT NOT NULL,
            version INTEGER NOT NULL,
            downloads INTEGER NOT NULL DEFAULT 0,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS community_configs_updated_idx
            ON community_configs(updated_at DESC);
        CREATE INDEX IF NOT EXISTS community_configs_owner_idx
            ON community_configs(owner_device_id, updated_at DESC);
        CREATE TABLE IF NOT EXISTS parties (
            id TEXT PRIMARY KEY,
            join_code TEXT NOT NULL UNIQUE,
            leader_device_id TEXT NOT NULL,
            status TEXT NOT NULL,
            max_members INTEGER NOT NULL,
            revision INTEGER NOT NULL,
            updated_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS party_members (
            id TEXT PRIMARY KEY,
            party_id TEXT NOT NULL,
            device_id TEXT NOT NULL UNIQUE,
            display_name TEXT NOT NULL,
            role TEXT NOT NULL,
            presence TEXT NOT NULL,
            last_seen_at TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS party_members_party_idx ON party_members(party_id);
        CREATE TABLE IF NOT EXISTS party_events (
            id TEXT PRIMARY KEY,
            party_id TEXT NOT NULL,
            target_device_id TEXT NOT NULL,
            actor_device_id TEXT NOT NULL,
            type TEXT NOT NULL,
            payload TEXT NOT NULL,
            sequence INTEGER NOT NULL,
            consumed INTEGER NOT NULL DEFAULT 0,
            created_at TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS party_events_target_idx
            ON party_events(target_device_id, consumed);
        CREATE TABLE IF NOT EXISTS party_game_states (
            party_id TEXT NOT NULL,
            game TEXT NOT NULL,
            payload TEXT NOT NULL,
            owner_device_id TEXT,
            revision INTEGER NOT NULL,
            updated_at TEXT NOT NULL,
            PRIMARY KEY(party_id, game)
        );
        CREATE TABLE IF NOT EXISTS audit_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            event_type TEXT NOT NULL,
            actor_device_id TEXT,
            actor_username TEXT,
            actor_role TEXT,
            source_ip TEXT,
            details TEXT NOT NULL,
            created_at TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS audit_events_created_idx
            ON audit_events(created_at DESC);
        CREATE INDEX IF NOT EXISTS audit_events_type_idx
            ON audit_events(event_type, created_at DESC);
        CREATE TABLE IF NOT EXISTS admins (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            token_hash TEXT NOT NULL UNIQUE,
            status TEXT NOT NULL,
            access_level TEXT NOT NULL DEFAULT 'admin',
            created_at TEXT NOT NULL,
            last_login_at TEXT
        );
        CREATE INDEX IF NOT EXISTS idx_admins_status
            ON admins(status);
        """
        with self.lock, self.connect() as connection:
            connection.executescript(schema)
            payload_columns = {
                row["name"] for row in connection.execute("PRAGMA table_info(payload_tokens)")
            }
            if "device_id" not in payload_columns:
                connection.execute("ALTER TABLE payload_tokens ADD COLUMN device_id TEXT")
            admin_columns = {
                row["name"] for row in connection.execute("PRAGMA table_info(admins)")
            }
            if "access_level" not in admin_columns:
                connection.execute(
                    "ALTER TABLE admins ADD COLUMN access_level TEXT NOT NULL DEFAULT 'admin'"
                )
            if self.admin_token and not connection.execute(
                "SELECT 1 FROM admins LIMIT 1"
            ).fetchone():
                connection.execute(
                    "INSERT INTO admins(id,name,token_hash,status,access_level,created_at) "
                    "VALUES(?,?,?,?,?,?)",
                    (
                        "ad_" + digest(self.admin_token)[:32],
                        "lzr",
                        digest(self.admin_token),
                        "active",
                        "owner",
                        iso(),
                    ),
                )
            if self.admin_token:
                connection.execute(
                    "UPDATE admins SET access_level='owner' WHERE token_hash=?",
                    (digest(self.admin_token),),
                )
                connection.execute(
                    "UPDATE admins SET name='lzr' WHERE token_hash=? AND name='Proprietário'",
                    (digest(self.admin_token),),
                )
            connection.execute("PRAGMA optimize")

    @staticmethod
    def license_id(license_key: str) -> str:
        return "lc_" + digest(license_key)[:32]

    @staticmethod
    def session_id(token: str) -> str:
        return "ss_" + digest(token)[:32]

    @staticmethod
    def config_id(device_id: str, slot: int) -> str:
        return "cfg_" + digest(f"{device_id}:{slot}")[:32]

    @staticmethod
    def member_id(device_id: str) -> str:
        return "pm_" + digest(device_id)[:32]

    @staticmethod
    def _cookie_token(cookie_header: str | None) -> str:
        if not cookie_header:
            return ""
        try:
            cookie = SimpleCookie()
            cookie.load(cookie_header)
            value = cookie.get("sv_admin") or cookie.get("__Host-sv_admin")
            return value.value if value else ""
        except Exception:
            return ""

    def _reap_admin_credentials(self) -> None:
        now = utcnow()
        for store in (self.admin_sessions, self.admin_tickets):
            for key, (expiry, _) in list(store.items()):
                if expiry <= now:
                    store.pop(key, None)
                    self.pending_admin_mfa.pop(key, None)

    def resolve_admin_token(self, token: object) -> dict[str, str] | None:
        if not safe_field(token, 256):
            return None
        token_text = str(token)
        with self.connect() as connection:
            row = connection.execute(
                "SELECT id,name,status,access_level FROM admins WHERE token_hash=?",
                (digest(token_text),),
            ).fetchone()
        if row:
            if row["status"] != "active":
                return None
            return {
                "id": row["id"],
                "name": row["name"],
                "username": row["name"],
                "role": "admin",
                "access": row["access_level"],
            }
        return None

    def admin_principal(
        self, header: str | None, cookie_header: str | None = None
    ) -> dict[str, str] | None:
        token = header[7:] if header and header.startswith("Bearer ") else self._cookie_token(cookie_header)
        if not safe_field(token, 128):
            return None
        with self.lock:
            self._reap_admin_credentials()
            session = self.admin_sessions.get(digest(token))
            if not session or session[0] <= utcnow() or session[1].get("_mfa_pending"):
                return None
            return dict(session[1])

    def admin_authorized(self, header: str | None, cookie_header: str | None = None) -> bool:
        return self.admin_principal(header, cookie_header) is not None

    def create_admin_ticket(self, principal: dict[str, str]) -> str:
        token = secrets.token_urlsafe(32)
        with self.lock:
            self._reap_admin_credentials()
            self.admin_tickets[digest(token)] = (
                utcnow() + ADMIN_TICKET_LIFETIME,
                {**principal, "_mfa_ticket": True},
            )
        return token

    def create_admin_session(self, body: dict[str, object]) -> dict[str, object] | None:
        supplied = body.get("adminToken")
        principal = self.resolve_admin_token(supplied)
        ticket = body.get("ticket")
        used_ticket = False
        if not principal and safe_field(ticket, 128):
            with self.lock:
                self._reap_admin_credentials()
                ticket_data = self.admin_tickets.get(digest(str(ticket)))
                if ticket_data and ticket_data[0] > utcnow():
                    principal = dict(ticket_data[1])
                    used_ticket = True
        if not principal:
            return None
        ticket_verified = bool(principal.pop("_mfa_ticket", False))
        if self.admin_mfa_enabled(principal["id"]):
            if not ticket_verified and not body.get("totp"):
                return {"mfaRequired": True}
            if not ticket_verified and not self.verify_admin_mfa(principal["id"], body.get("totp")):
                return None
            needs_setup = False
        else:
            needs_setup = True
        if used_ticket:
            with self.lock:
                self.admin_tickets.pop(digest(str(ticket)), None)
        token = secrets.token_urlsafe(32)
        session_principal = dict(principal)
        if needs_setup:
            session_principal["_mfa_pending"] = True
        with self.lock:
            self.admin_sessions[digest(token)] = (
                utcnow() + (timedelta(minutes=10) if needs_setup else ADMIN_SESSION_LIFETIME),
                session_principal,
            )
            if needs_setup:
                self.pending_admin_mfa[digest(token)] = (
                    utcnow() + timedelta(minutes=10),
                    base64.b32encode(secrets.token_bytes(20)).decode("ascii"),
                )
        if not needs_setup:
            with self.lock, self.connect() as connection:
                connection.execute(
                    "UPDATE admins SET last_login_at=? WHERE id=?", (iso(), principal["id"])
                )
        return {"token": token, "principal": principal, "mfaSetupRequired": needs_setup}

    def admin_mfa_setup(self, cookie_header: str | None) -> dict | None:
        token = self._cookie_token(cookie_header)
        if not safe_field(token, 128):
            return None
        key = digest(token)
        with self.lock:
            self._reap_admin_credentials()
            session = self.admin_sessions.get(key)
            if not session or not session[1].get("_mfa_pending"):
                return None
            pending = self.pending_admin_mfa.get(key)
            if not pending or pending[0] <= utcnow():
                pending = (utcnow() + timedelta(minutes=10),
                           base64.b32encode(secrets.token_bytes(20)).decode("ascii"))
                self.pending_admin_mfa[key] = pending
            name = session[1]["name"]
            secret = pending[1]
        return self.admin_mfa_setup_data(name, secret)

    def confirm_admin_mfa(self, cookie_header: str | None, code: object) -> dict | None:
        token = self._cookie_token(cookie_header)
        if not safe_field(token, 128):
            return None
        key = digest(token)
        with self.lock:
            self._reap_admin_credentials()
            session = self.admin_sessions.get(key)
            pending = self.pending_admin_mfa.get(key)
            if not session or not session[1].get("_mfa_pending") or not pending or pending[0] <= utcnow():
                return None
            principal = dict(session[1])
            if not self.verify_admin_mfa(principal["id"], code, pending[1]):
                return None
            principal.pop("_mfa_pending", None)
            for other_key, (_, other) in list(self.admin_sessions.items()):
                if other_key != key and other.get("id") == principal["id"]:
                    self.admin_sessions.pop(other_key, None)
                    self.pending_admin_mfa.pop(other_key, None)
            self.pending_admin_mfa.pop(key, None)
            self.admin_sessions[key] = (utcnow() + ADMIN_SESSION_LIFETIME, principal)
            with self.connect() as connection:
                connection.execute("UPDATE admins SET last_login_at=? WHERE id=?", (iso(), principal["id"]))
                recovery_codes = self.create_admin_recovery_codes(connection, principal["id"])
                self.audit_event(connection, "admin.mfa.enabled", principal)
            return {"admin": principal, "recoveryCodes": recovery_codes}

    def revoke_admin_session(self, cookie_header: str | None) -> None:
        token = self._cookie_token(cookie_header)
        if safe_field(token, 128):
            with self.lock:
                self.admin_sessions.pop(digest(token), None)
                self.pending_admin_mfa.pop(digest(token), None)

    @staticmethod
    def audit_event(
        connection: sqlite3.Connection,
        event_type: str,
        device: dict[str, object] | sqlite3.Row | None = None,
        details: dict[str, object] | None = None,
        source_ip: str = "",
    ) -> None:
        actor = dict(device) if device is not None else {}
        serialized = json.dumps(details or {}, ensure_ascii=False, separators=(",", ":"))
        connection.execute(
            "INSERT INTO audit_events(event_type,actor_device_id,actor_username,actor_role,source_ip,details,created_at) "
            "VALUES(?,?,?,?,?,?,?)",
            (
                event_type,
                actor.get("id") or actor.get("device_id"),
                str(actor.get("username") or actor.get("display_name") or "admin"),
                str(actor.get("role") or ""),
                source_ip[:64],
                serialized[:8192],
                iso(),
            ),
        )

    def payload_path(self, role: str) -> Path | None:
        return self.selected_payload(role)

    def issue_session(self, connection: sqlite3.Connection, device_id: str, kind: str) -> str:
        token = secrets.token_urlsafe(32)
        connection.execute(
            "INSERT INTO sessions(id, device_id, expires_at, kind) VALUES(?,?,?,?)",
            (self.session_id(token), device_id, iso(utcnow() + SESSION_LIFETIME), kind),
        )
        return token

    def build_loader_authorization(
        self, connection: sqlite3.Connection, device: dict[str, object], new_secret: str = ""
    ) -> str:
        payload = self.payload_path(str(device.get("role") or "user"))
        if not payload:
            return "ERROR\tpayload_unavailable"
        payload_id = payload.stem
        file_token = secrets.token_urlsafe(32)
        connection.execute(
            "INSERT INTO payload_tokens(id, payload_id, device_id, expires_at) VALUES(?,?,?,?)",
            (digest(file_token), payload_id, device["id"], iso(utcnow() + PAYLOAD_TOKEN_LIFETIME)),
        )
        session_token = self.issue_session(connection, str(device["id"]), "loader")
        username = re.sub(r"[\r\n\t|]", "_", str(device.get("username") or "user"))
        role = str(device.get("role") or "user")
        return f"OK\t{username}\t{payload_id}\t{file_token}\t{session_token}\t{new_secret or '-'}\t{role}"

    def build_signed_license(self, device: dict[str, object]) -> dict[str, object] | None:
        """RSA-SHA256 signed license payload embedded in the /authenticated data.

        Returns None when no private key is installed so legacy deployments keep
        working; an updated client must be rolled out only after the server has
        this key configured (fail closed).
        """
        if load_key is None or sign_license_payload is None:
            return None
        private_key = load_key(Path(__file__).resolve().parent / "license_private.json")
        if private_key is None or "d" not in private_key:
            return None
        payload = {
            "v": 1,
            "product": "sonyvegas",
            "userId": int(digest(str(device["id"]))[:15], 16),
            "role": str(device["role"] or "user"),
            "iat": int(utcnow().timestamp()),
            "exp": int((utcnow() + SESSION_LIFETIME).timestamp()),
        }
        payload["requireSignature"] = True
        return sign_license_payload(payload, private_key)

    def authorize_session(self, connection: sqlite3.Connection, token: object) -> dict[str, object] | None:
        if not safe_field(token, 128):
            return None
        session_id = self.session_id(str(token))
        row = connection.execute(
            "SELECT s.device_id, s.expires_at, d.username, d.role, d.status, d.license_id "
            "FROM sessions s JOIN devices d ON d.id=s.device_id WHERE s.id=?",
            (session_id,),
        ).fetchone()
        if not row:
            return None
        expires_at = parse_time(row["expires_at"])
        if not expires_at or expires_at <= utcnow():
            connection.execute("DELETE FROM sessions WHERE id=?", (session_id,))
            return None
        if row["status"] != "active" or not HWID_RE.fullmatch(row["device_id"]):
            return None
        if not self.device_license_active(connection, {"id": row["device_id"], "license_id": row["license_id"]}):
            return None
        return {
            "id": row["device_id"],
            "username": row["username"] or "user",
            "role": row["role"] or "user",
        }

    def check_device(self, body: dict[str, object], source_ip: str = "") -> str:
        hwid = body.get("hwid")
        if not isinstance(hwid, str) or not HWID_RE.fullmatch(hwid):
            return "ERROR\tinvalid_hwid"
        with self.lock, self.connect() as connection:
            row = connection.execute("SELECT * FROM devices WHERE id=?", (hwid,)).fetchone()
            if not row:
                return "NOT_FOUND"
            if row["status"] != "active":
                return "ERROR\tdevice_disabled"
            if not self.device_license_active(connection, row):
                return "ERROR\tlicense_disabled_or_expired"
            if not safe_field(row["secret_hash"], 64):
                return "CLAIM_REQUIRED"
            supplied = body.get("deviceSecret")
            if not safe_field(supplied, 128) or not hmac.compare_digest(
                digest(str(supplied)), row["secret_hash"]
            ):
                return "ERROR\tdevice_secret_invalid"
            username = body.get("username")
            registered_username = str(row["username"] or "")
            if username is not None and not safe_field(username, 32):
                return "ERROR\tinvalid_username"
            if username is not None and str(username).casefold() != registered_username.casefold():
                return "ERROR\tusername_mismatch"
            connection.execute("UPDATE devices SET last_seen_at=? WHERE id=?", (iso(), hwid))
            response = self.build_loader_authorization(connection, dict(row))
            if response.startswith("OK\t"):
                self.record_login(connection, hwid, source_ip)
                self.audit_event(connection, "loader.opened", row, source_ip=source_ip)
            return response

    def claim_device(self, body: dict[str, object], source_ip: str = "") -> str:
        hwid, license_key, username = (
            body.get("hwid"), body.get("licenseKey"), body.get("username")
        )
        if (
            not isinstance(hwid, str)
            or not HWID_RE.fullmatch(hwid)
            or not safe_field(license_key, 128)
            or not safe_field(username, 32)
        ):
            return "ERROR\tinvalid_input"
        license_id = self.license_id(str(license_key))
        with self.lock, self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            license_row = connection.execute("SELECT * FROM licenses WHERE id=?", (license_id,)).fetchone()
            device = connection.execute("SELECT * FROM devices WHERE id=?", (hwid,)).fetchone()
            if (
                not license_row
                or not device
                or license_row["status"] != "used"
                or license_row["bound_device_id"] != hwid
                or device["license_id"] != license_id
                or safe_field(device["secret_hash"], 64)
            ):
                return "ERROR\tinvalid_license"
            if str(username).casefold() != str(device["username"] or "").casefold():
                return "ERROR\tusername_mismatch"
            if device["status"] != "active" or not self.device_license_active(connection, device):
                return "ERROR\tlicense_disabled_or_expired"
            device_secret = secrets.token_urlsafe(32)
            connection.execute(
                "UPDATE devices SET secret_hash=?, last_seen_at=? WHERE id=?",
                (digest(device_secret), iso(), hwid),
            )
            device = connection.execute("SELECT * FROM devices WHERE id=?", (hwid,)).fetchone()
            response = self.build_loader_authorization(connection, dict(device), device_secret)
            if response.startswith("OK\t"):
                self.record_login(connection, hwid, source_ip)
                self.audit_event(connection, "loader.opened", device, {"claim": True}, source_ip)
            return response

    def activate_device(self, body: dict[str, object], source_ip: str = "") -> str:
        hwid, license_key, username = body.get("hwid"), body.get("licenseKey"), body.get("username")
        if (
            not isinstance(hwid, str)
            or not HWID_RE.fullmatch(hwid)
            or not safe_field(license_key, 128)
            or not safe_field(username, 32)
        ):
            return "ERROR\tinvalid_input"
        license_id = self.license_id(str(license_key))
        now = iso()
        with self.lock, self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            license_row = connection.execute("SELECT * FROM licenses WHERE id=?", (license_id,)).fetchone()
            if not license_row:
                return "ERROR\tinvalid_license"
            if license_row["status"] != "unused":
                return "ERROR\tlicense_used"
            expires_at = parse_time(license_row["expires_at"])
            if expires_at and expires_at <= utcnow():
                return "ERROR\tlicense_expired"
            if connection.execute("SELECT 1 FROM devices WHERE id=?", (hwid,)).fetchone():
                return "ERROR\tdevice_already_registered"
            device_secret = secrets.token_urlsafe(32)
            connection.execute(
                "INSERT INTO devices(id,username,status,role,license_id,secret_hash,last_seen_at,activated_at) "
                "VALUES(?,?,?,?,?,?,?,?)",
                (hwid, username, "active", license_row["role"], license_id, digest(device_secret), now, now),
            )
            connection.execute(
                "UPDATE licenses SET status='used', bound_device_id=?, redeemed_at=? WHERE id=?",
                (hwid, now, license_id),
            )
            response = self.build_loader_authorization(
                connection,
                {"id": hwid, "username": username, "role": license_row["role"]},
                device_secret,
            )
            if response.startswith("OK\t"):
                self.record_login(connection, hwid, source_ip)
                device = {"id": hwid, "username": username, "role": license_row["role"]}
                self.audit_event(connection, "user.activated", device, {"licenseId": license_id}, source_ip)
                self.audit_event(connection, "loader.opened", device, {"activation": True}, source_ip)
            return response

    def config_action(
        self,
        connection: sqlite3.Connection,
        body: dict[str, object],
        device: dict[str, object],
        source_ip: str = "",
    ) -> tuple[int, dict[str, object]]:
        slot = body.get("slot")
        if isinstance(slot, bool) or not isinstance(slot, int) or slot < 0 or slot > 3:
            return 400, {"ok": False, "error": "invalid_slot"}
        row_id = self.config_id(str(device["id"]), slot)
        row = connection.execute("SELECT * FROM configs WHERE id=?", (row_id,)).fetchone()
        action = body.get("action")
        if action == "config.load":
            if not row:
                return 404, {"ok": False, "error": "config_not_found"}
            try:
                payload = json.loads(row["payload"])
            except json.JSONDecodeError:
                return 500, {"ok": False, "error": "invalid_stored_config"}
            self.audit_event(
                connection,
                "config.loaded",
                device,
                {"slot": slot, "name": row["name"], "version": row["version"]},
                source_ip,
            )
            return 200, {"ok": True, "config": {"slot": slot, "name": row["name"], "version": row["version"], "payload": payload}}
        if action == "config.delete":
            deleted = connection.execute(
                "DELETE FROM configs WHERE id=? AND device_id=?", (row_id, device["id"])
            ).rowcount
            self.audit_event(
                connection,
                "config.deleted",
                device,
                {"slot": slot, "name": row["name"] if row else "", "deleted": bool(deleted)},
                source_ip,
            )
            return 200, {"ok": True}
        payload = body.get("payload")
        if action != "config.save" or not safe_field(body.get("name"), 32) or not isinstance(payload, dict):
            return 400, {"ok": False, "error": "invalid_config"}
        serialized = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
        if len(serialized.encode("utf-8")) > MAX_CONFIG_BYTES:
            return 413, {"ok": False, "error": "config_too_large"}
        version = int(row["version"]) + 1 if row else 1
        connection.execute(
            "INSERT INTO configs(id,device_id,slot,name,payload,version,updated_at) VALUES(?,?,?,?,?,?,?) "
            "ON CONFLICT(id) DO UPDATE SET name=excluded.name,payload=excluded.payload,"
            "version=excluded.version,updated_at=excluded.updated_at",
            (row_id, device["id"], slot, body["name"], serialized, version, iso()),
        )
        self.audit_event(
            connection,
            "config.saved" if row else "config.created",
            device,
            {"slot": slot, "name": body["name"], "version": version},
            source_ip,
        )
        return 200, {"ok": True, "slot": slot, "version": version}

    def list_configs(self, connection: sqlite3.Connection, device: dict[str, object]) -> tuple[int, dict[str, object]]:
        rows = connection.execute(
            "SELECT slot,name,version,updated_at FROM configs WHERE device_id=? ORDER BY slot LIMIT 4",
            (device["id"],),
        ).fetchall()
        return 200, {"ok": True, "configs": [
            {"slot": row["slot"], "name": row["name"], "version": row["version"], "updatedAt": row["updated_at"]}
            for row in rows
        ]}

    def community_list(
        self,
        connection: sqlite3.Connection,
        body: dict[str, object],
        device: dict[str, object],
    ) -> tuple[int, dict[str, object]]:
        query = body.get("query") or ""
        if not isinstance(query, str) or len(query) > 64 or any(char in query for char in "\r\n\t|"):
            return 400, {"ok": False, "error": "invalid_query"}
        pattern = f"%{query.strip()}%"
        rows = connection.execute(
            "SELECT id,owner_device_id,owner_username,name,description,version,downloads,created_at,updated_at "
            "FROM community_configs WHERE name LIKE ? OR description LIKE ? OR owner_username LIKE ? "
            "ORDER BY updated_at DESC LIMIT 60",
            (pattern, pattern, pattern),
        ).fetchall()
        return 200, {"ok": True, "configs": [
            {
                "id": row["id"],
                "owner": row["owner_username"],
                "name": row["name"],
                "description": row["description"],
                "version": row["version"],
                "downloads": row["downloads"],
                "createdAt": row["created_at"],
                "updatedAt": row["updated_at"],
                "mine": row["owner_device_id"] == device["id"],
            }
            for row in rows
        ]}

    def community_action(
        self,
        connection: sqlite3.Connection,
        body: dict[str, object],
        device: dict[str, object],
        source_ip: str = "",
    ) -> tuple[int, dict[str, object]]:
        action = body.get("action")
        if action == "community.publish":
            name = body.get("name")
            description = body.get("description") or ""
            payload = body.get("payload")
            if (
                not safe_field(name, 32)
                or not isinstance(description, str)
                or len(description) > MAX_COMMUNITY_DESCRIPTION
                or any(char in description for char in "\r\n\t|")
                or not isinstance(payload, dict)
            ):
                return 400, {"ok": False, "error": "invalid_community_config"}
            serialized = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
            if len(serialized.encode("utf-8")) > MAX_CONFIG_BYTES:
                return 413, {"ok": False, "error": "config_too_large"}
            owned = connection.execute(
                "SELECT COUNT(*) FROM community_configs WHERE owner_device_id=?",
                (device["id"],),
            ).fetchone()[0]
            if owned >= MAX_COMMUNITY_CONFIGS_PER_USER:
                return 409, {"ok": False, "error": "community_limit_reached"}
            config_id = "cc_" + secrets.token_hex(16)
            now = iso()
            connection.execute(
                "INSERT INTO community_configs(id,owner_device_id,owner_username,name,description,payload,version,downloads,created_at,updated_at) "
                "VALUES(?,?,?,?,?,?,1,0,?,?)",
                (config_id, device["id"], device["username"], name, description, serialized, now, now),
            )
            self.audit_event(
                connection,
                "community.published",
                device,
                {"configId": config_id, "name": name},
                source_ip,
            )
            return 200, {"ok": True, "config": {"id": config_id, "name": name, "version": 1}}

        config_id = body.get("configId")
        if not isinstance(config_id, str) or not COMMUNITY_CONFIG_RE.fullmatch(config_id):
            return 400, {"ok": False, "error": "invalid_community_config"}
        row = connection.execute(
            "SELECT * FROM community_configs WHERE id=?", (config_id,)
        ).fetchone()
        if not row:
            return 404, {"ok": False, "error": "community_config_not_found"}
        if action == "community.load":
            try:
                payload = json.loads(row["payload"])
            except json.JSONDecodeError:
                return 500, {"ok": False, "error": "invalid_stored_config"}
            connection.execute(
                "UPDATE community_configs SET downloads=downloads+1 WHERE id=?",
                (config_id,),
            )
            self.audit_event(
                connection,
                "community.downloaded",
                device,
                {"configId": config_id, "name": row["name"], "owner": row["owner_username"]},
                source_ip,
            )
            return 200, {"ok": True, "config": {
                "id": config_id,
                "name": row["name"],
                "owner": row["owner_username"],
                "version": row["version"],
                "payload": payload,
            }}
        if action == "community.delete":
            if row["owner_device_id"] != device["id"]:
                return 403, {"ok": False, "error": "owner_required"}
            connection.execute("DELETE FROM community_configs WHERE id=?", (config_id,))
            self.audit_event(
                connection,
                "community.deleted",
                device,
                {"configId": config_id, "name": row["name"]},
                source_ip,
            )
            return 200, {"ok": True}
        return 400, {"ok": False, "error": "unknown_action"}

    def party_state(self, connection: sqlite3.Connection, device: dict[str, object]) -> tuple[int, dict[str, object]]:
        member = connection.execute("SELECT * FROM party_members WHERE device_id=?", (device["id"],)).fetchone()
        if not member:
            return 200, {"ok": True, "party": None}
        party = connection.execute("SELECT * FROM parties WHERE id=?", (member["party_id"],)).fetchone()
        if not party:
            connection.execute("DELETE FROM party_members WHERE device_id=?", (device["id"],))
            return 200, {"ok": True, "party": None}
        connection.execute(
            "UPDATE party_members SET presence='online',last_seen_at=? WHERE device_id=?",
            (iso(), device["id"]),
        )
        members = connection.execute(
            "SELECT id,display_name,role,presence FROM party_members WHERE party_id=? "
            "ORDER BY CASE role WHEN 'leader' THEN 0 ELSE 1 END, display_name",
            (party["id"],),
        ).fetchall()
        basketball_row = connection.execute(
            "SELECT payload FROM party_game_states WHERE party_id=? AND game='basketball'",
            (party["id"],),
        ).fetchone()
        basketball = None
        if basketball_row:
            try:
                basketball = json.loads(basketball_row["payload"])
            except json.JSONDecodeError:
                basketball = None
        return 200, {"ok": True, "party": {
            "id": party["id"], "code": party["join_code"], "status": party["status"],
            "leaderDeviceId": party["leader_device_id"], "myRole": member["role"],
            "revision": party["revision"],
            "basketball": basketball,
            "members": [{"id": row["id"], "name": row["display_name"], "role": row["role"], "presence": row["presence"]} for row in members],
        }}

    def party_create(
        self,
        connection: sqlite3.Connection,
        body: dict[str, object],
        device: dict[str, object],
        source_ip: str = "",
    ) -> tuple[int, dict[str, object]]:
        if connection.execute("SELECT 1 FROM party_members WHERE device_id=?", (device["id"],)).fetchone():
            return 409, {"ok": False, "error": "already_in_party"}
        raw_max = body.get("maxMembers", 8)
        max_members = max(2, min(16, raw_max if isinstance(raw_max, int) and not isinstance(raw_max, bool) else 8))
        alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        for _ in range(10):
            code = "".join(secrets.choice(alphabet) for _ in range(6))
            party_id = "p_" + code
            try:
                connection.execute(
                    "INSERT INTO parties(id,join_code,leader_device_id,status,max_members,revision,updated_at) "
                    "VALUES(?,?,?,?,?,?,?)",
                    (party_id, code, device["id"], "open", max_members, 1, iso()),
                )
                break
            except sqlite3.IntegrityError:
                continue
        else:
            return 503, {"ok": False, "error": "party_code_failed"}
        connection.execute(
            "INSERT INTO party_members(id,party_id,device_id,display_name,role,presence,last_seen_at) "
            "VALUES(?,?,?,?,?,?,?)",
            (self.member_id(str(device["id"])), party_id, device["id"], device["username"], "leader", "online", iso()),
        )
        self.audit_event(
            connection,
            "party.created",
            device,
            {"partyId": party_id, "code": code, "maxMembers": max_members},
            source_ip,
        )
        return self.party_state(connection, device)

    def party_join(
        self,
        connection: sqlite3.Connection,
        body: dict[str, object],
        device: dict[str, object],
        source_ip: str = "",
    ) -> tuple[int, dict[str, object]]:
        code = str(body.get("code") or "").strip().upper()
        if not PARTY_CODE_RE.fullmatch(code):
            return 400, {"ok": False, "error": "invalid_party_code"}
        if connection.execute("SELECT 1 FROM party_members WHERE device_id=?", (device["id"],)).fetchone():
            return 409, {"ok": False, "error": "already_in_party"}
        party = connection.execute("SELECT * FROM parties WHERE id=?", ("p_" + code,)).fetchone()
        if not party:
            return 404, {"ok": False, "error": "party_not_found"}
        if party["status"] == "closed":
            return 409, {"ok": False, "error": "party_closed"}
        count = connection.execute("SELECT COUNT(*) FROM party_members WHERE party_id=?", (party["id"],)).fetchone()[0]
        if count >= party["max_members"]:
            return 409, {"ok": False, "error": "party_full"}
        connection.execute(
            "INSERT INTO party_members(id,party_id,device_id,display_name,role,presence,last_seen_at) VALUES(?,?,?,?,?,?,?)",
            (self.member_id(str(device["id"])), party["id"], device["id"], device["username"], "member", "online", iso()),
        )
        connection.execute("UPDATE parties SET revision=revision+1,updated_at=? WHERE id=?", (iso(), party["id"]))
        self.audit_event(
            connection,
            "party.joined",
            device,
            {"partyId": party["id"], "code": code},
            source_ip,
        )
        return self.party_state(connection, device)

    def party_leave(
        self,
        connection: sqlite3.Connection,
        device: dict[str, object],
        source_ip: str = "",
    ) -> tuple[int, dict[str, object]]:
        member = connection.execute("SELECT * FROM party_members WHERE device_id=?", (device["id"],)).fetchone()
        if not member:
            return 200, {"ok": True, "party": None}
        connection.execute("DELETE FROM party_members WHERE device_id=?", (device["id"],))
        remaining = connection.execute(
            "SELECT * FROM party_members WHERE party_id=? ORDER BY last_seen_at DESC", (member["party_id"],)
        ).fetchall()
        if not remaining:
            connection.execute("DELETE FROM party_events WHERE party_id=?", (member["party_id"],))
            connection.execute("DELETE FROM party_game_states WHERE party_id=?", (member["party_id"],))
            connection.execute("DELETE FROM parties WHERE id=?", (member["party_id"],))
        else:
            owned_game = connection.execute(
                "SELECT payload FROM party_game_states "
                "WHERE party_id=? AND game='basketball' AND owner_device_id=?",
                (member["party_id"], device["id"]),
            ).fetchone()
            if owned_game:
                try:
                    released = json.loads(owned_game["payload"])
                    released["state"] = "IDLE"
                    released["owner"] = ""
                    released["velX"] = released["velY"] = released["velZ"] = 0.0
                    released["revision"] = int(released.get("revision", 0)) + 1
                    connection.execute(
                        "UPDATE party_game_states SET payload=?,owner_device_id=NULL,revision=?,updated_at=? "
                        "WHERE party_id=? AND game='basketball'",
                        (json.dumps(released, ensure_ascii=False, separators=(",", ":")),
                         released["revision"], iso(), member["party_id"]),
                    )
                except (json.JSONDecodeError, TypeError, ValueError):
                    connection.execute(
                        "DELETE FROM party_game_states WHERE party_id=? AND game='basketball'",
                        (member["party_id"],),
                    )
            party = connection.execute("SELECT * FROM parties WHERE id=?", (member["party_id"],)).fetchone()
            if party and party["leader_device_id"] == device["id"]:
                next_leader = remaining[0]["device_id"]
                connection.execute("UPDATE party_members SET role='leader' WHERE device_id=?", (next_leader,))
                connection.execute(
                    "UPDATE parties SET leader_device_id=?,revision=revision+1,updated_at=? WHERE id=?",
                    (next_leader, iso(), member["party_id"]),
                )
            else:
                connection.execute("UPDATE parties SET revision=revision+1,updated_at=? WHERE id=?", (iso(), member["party_id"]))
        self.audit_event(
            connection,
            "party.left",
            device,
            {"partyId": member["party_id"], "role": member["role"]},
            source_ip,
        )
        return 200, {"ok": True, "party": None}

    def party_kick(self, connection: sqlite3.Connection, body: dict[str, object],
                   device: dict[str, object], source_ip: str = "") -> tuple[int, dict[str, object]]:
        leader = connection.execute("SELECT * FROM party_members WHERE device_id=?", (device["id"],)).fetchone()
        member_id = str(body.get("memberId") or "")
        if not leader:
            return 404, {"ok": False, "error": "not_in_party"}
        if leader["role"] != "leader":
            return 403, {"ok": False, "error": "leader_required"}
        target = connection.execute("SELECT * FROM party_members WHERE id=? AND party_id=?", (member_id, leader["party_id"])).fetchone()
        if not target:
            return 404, {"ok": False, "error": "member_not_found"}
        if target["device_id"] == device["id"]:
            return 409, {"ok": False, "error": "cannot_kick_leader"}
        connection.execute("DELETE FROM party_members WHERE id=?", (member_id,))
        connection.execute("UPDATE parties SET revision=revision+1,updated_at=? WHERE id=?", (iso(), leader["party_id"]))
        self.audit_event(connection, "party.kicked", device,
                         {"partyId": leader["party_id"], "memberId": member_id}, source_ip)
        return self.party_state(connection, device)

    def party_command(
        self,
        connection: sqlite3.Connection,
        body: dict[str, object],
        device: dict[str, object],
        source_ip: str = "",
    ) -> tuple[int, dict[str, object]]:
        member = connection.execute("SELECT * FROM party_members WHERE device_id=?", (device["id"],)).fetchone()
        if not member:
            return 404, {"ok": False, "error": "not_in_party"}
        payload = body.get("payload")
        if not isinstance(payload, dict):
            return 400, {"ok": False, "error": "invalid_command"}
        basketball = payload.get("basketball")
        is_basketball_update = isinstance(basketball, dict) and set(payload) == {"basketball"}
        if member["role"] != "leader" and not is_basketball_update:
            return 403, {"ok": False, "error": "leader_required"}
        if is_basketball_update:
            required_numbers = ("hoopX", "hoopY", "hoopZ", "forwardX", "forwardZ",
                                "ballX", "ballY", "ballZ", "velX", "velY", "velZ")
            if (basketball.get("state") not in ("IDLE", "HELD", "FLYING")
                    or not isinstance(basketball.get("active", True), bool)
                    or not isinstance(basketball.get("owner", ""), str)
                    or len(basketball.get("owner", "")) > 64
                    or (basketball.get("state") in ("HELD", "FLYING")
                        and not basketball.get("owner"))
                    or not isinstance(basketball.get("score", 0), int)
                    or not 0 <= basketball.get("score", 0) <= 1_000_000
                    or any(not isinstance(basketball.get(name), (int, float))
                           or not math.isfinite(float(basketball[name]))
                           or abs(float(basketball[name])) > 30_000_000
                           for name in required_numbers)):
                return 400, {"ok": False, "error": "invalid_command"}
        serialized = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
        if len(serialized.encode("utf-8")) > MAX_COMMAND_BYTES:
            return 413, {"ok": False, "error": "command_too_large"}
        party = connection.execute("SELECT * FROM parties WHERE id=?", (member["party_id"],)).fetchone()
        if not party:
            return 500, {"ok": False, "error": "party_state_failed"}
        state = payload.get("state")
        is_target_update = isinstance(state, dict) and "partyTarget" in state
        if is_target_update:
            selection = state["partyTarget"]
            if (not isinstance(selection, dict) or selection.get("code") != party["join_code"]
                    or not isinstance(selection.get("name"), str)
                    or (selection["name"] and not re.fullmatch(r"[A-Za-z0-9_]{1,16}", selection["name"]))
                    or not isinstance(state.get("modules"), list) or len(state["modules"]) != 1
                    or not isinstance(state["modules"][0], dict)
                    or type(state["modules"][0].get("enabled")) is not bool):
                return 400, {"ok": False, "error": "invalid_command"}
        sequence = int(party["revision"]) + 1
        if is_basketball_update:
            existing_game = connection.execute(
                "SELECT owner_device_id,payload FROM party_game_states "
                "WHERE party_id=? AND game='basketball'",
                (party["id"],),
            ).fetchone()
            if existing_game and existing_game["owner_device_id"] not in (None, device["id"]):
                try:
                    existing_state = json.loads(existing_game["payload"]).get("state")
                except (json.JSONDecodeError, AttributeError):
                    existing_state = None
                if existing_state == "HELD":
                    return 409, {"ok": False, "error": "basketball_owned"}
            basketball = dict(basketball)
            basketball["revision"] = sequence
            payload = {"basketball": basketball}
            serialized = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
            owner_device_id = (device["id"] if basketball.get("active", True)
                               and basketball["state"] in ("HELD", "FLYING") else None)
            connection.execute(
                "INSERT INTO party_game_states(party_id,game,payload,owner_device_id,revision,updated_at) "
                "VALUES(?,?,?,?,?,?) ON CONFLICT(party_id,game) DO UPDATE SET "
                "payload=excluded.payload,owner_device_id=excluded.owner_device_id,"
                "revision=excluded.revision,updated_at=excluded.updated_at",
                (party["id"], "basketball",
                 json.dumps(basketball, ensure_ascii=False, separators=(",", ":")),
                 owner_device_id, sequence, iso()),
            )
        members = connection.execute("SELECT device_id FROM party_members WHERE party_id=?", (party["id"],)).fetchall()
        now = iso()
        for target in members:
            if target["device_id"] == device["id"]:
                continue
            if is_basketball_update:
                connection.execute(
                    "DELETE FROM party_events WHERE party_id=? AND target_device_id=? "
                    "AND type='basketball'",
                    (party["id"], target["device_id"]),
                )
            elif is_target_update:
                # Offline members need the latest leader target, not a backlog of old targets.
                connection.execute(
                    "DELETE FROM party_events WHERE party_id=? AND target_device_id=? AND type='target'",
                    (party["id"], target["device_id"]),
                )
            connection.execute(
                "INSERT INTO party_events(id,party_id,target_device_id,actor_device_id,type,payload,sequence,consumed,created_at) "
                "VALUES(?,?,?,?,?,?,?,?,?)",
                ("pe_" + secrets.token_hex(16), party["id"], target["device_id"], device["id"],
                 "basketball" if is_basketball_update else "target" if is_target_update else "command", serialized, sequence, 0, now),
            )
        connection.execute("UPDATE parties SET revision=?,updated_at=? WHERE id=?", (sequence, now, party["id"]))
        if not is_basketball_update and not is_target_update:
            self.audit_event(
                connection,
                "party.synced",
                device,
                {"partyId": party["id"], "sequence": sequence, "recipients": max(0, len(members) - 1)},
                source_ip,
            )
        return 200, {"ok": True, "sequence": sequence}

    def party_poll(self, connection: sqlite3.Connection, device: dict[str, object], body: dict[str, object] | None = None) -> tuple[int, dict[str, object]]:
        acknowledged_delivery = body is not None and "ack" in body
        if acknowledged_delivery:
            acknowledgements = body["ack"]
            if not isinstance(acknowledgements, list) or len(acknowledgements) > 50 or any(
                not isinstance(event_id, str) or len(event_id) > 64 for event_id in acknowledgements
            ):
                return 400, {"ok": False, "error": "invalid_ack"}
            for event_id in acknowledgements:
                connection.execute(
                    "UPDATE party_events SET consumed=1 WHERE id=? AND target_device_id=?",
                    (event_id, device["id"]),
                )
        rows = connection.execute(
            "SELECT * FROM party_events WHERE target_device_id=? AND consumed=0 ORDER BY created_at,sequence,rowid LIMIT 50",
            (device["id"],),
        ).fetchall()
        events: list[dict[str, object]] = []
        for row in rows:
            try:
                payload = json.loads(row["payload"])
            except json.JSONDecodeError:
                payload = None
            events.append({"id": row["id"], "type": row["type"], "payload": payload, "sequence": row["sequence"]})
            if not acknowledged_delivery:
                connection.execute("UPDATE party_events SET consumed=1 WHERE id=?", (row["id"],))
        return 200, {"ok": True, "events": events}

    def route_control(
        self, body: dict[str, object], source_ip: str = ""
    ) -> tuple[int, str | dict[str, object]]:
        action = body.get("action")
        if action in ("auth.check", "check"):
            return 200, self.check_device(body, source_ip)
        if action == "auth.claim":
            return 200, self.claim_device(body, source_ip)
        if action in ("auth.activate", "activate"):
            return 200, self.activate_device(body, source_ip)
        with self.lock, self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            device = self.authorize_session(connection, body.get("token"))
            if not device:
                return 401, {"ok": False, "error": "unauthorized"}
            # The JNI control bridge shipped requests as {action, token,
            # arguments:{...}} while older clients send argument fields at the
            # top level. Accept both; outer action/token always take precedence.
            arguments = body.get("arguments")
            if isinstance(arguments, dict):
                body = {**arguments, **body}
            if action == "config.list":
                return self.list_configs(connection, device)
            if action in ("config.save", "config.load", "config.delete"):
                return self.config_action(connection, body, device, source_ip)
            if action == "community.list":
                return self.community_list(connection, body, device)
            if action in ("community.publish", "community.load", "community.delete"):
                return self.community_action(connection, body, device, source_ip)
            if action in ("client.injected", "client.heartbeat", "client.probe", "client.stopped", "client.error", "client.crashed", "loader.failed"):
                return self.client_event(connection, action, body, device, source_ip)
            if action == "downloads.list":
                return 200, {"ok": True, "downloads": self.catalog(device["role"])}
            if action == "downloads.authorize":
                download = self.authorize_download(connection, str(body.get("id") or ""), device)
                return (200, {"ok": True, "download": download}) if download else (403, {"ok": False, "error": "download_unavailable"})
            if action == "loader.latest":
                current_sha = str(body.get("sha256") or "").lower()
                if not re.fullmatch(r"[0-9a-f]{64}", current_sha):
                    return 400, {"ok": False, "error": "invalid_loader_hash"}
                visible = self.visible_roles(str(device.get("role") or "user"))
                placeholders = ",".join("?" for _ in visible)
                candidates = connection.execute(
                    f"SELECT id,sha256 FROM downloads WHERE kind='loader' AND enabled=1 "
                    f"AND uploaded=1 AND role IN ({placeholders}) "
                    "ORDER BY updated_at DESC,created_at DESC,id DESC", visible
                ).fetchall()
                for candidate in candidates:
                    if not self.catalog_path(candidate["id"]).is_file():
                        continue
                    expected_sha = str(candidate["sha256"] or "").lower()
                    if not re.fullmatch(r"[0-9a-f]{64}", expected_sha):
                        continue
                    if current_sha == expected_sha:
                        return 200, "CURRENT"
                    download = self.authorize_download(connection, candidate["id"], device)
                    if download:
                        return 200, f"UPDATE\t{expected_sha}\t{download['bytes']}\t{download['url']}"
                return 200, "NONE"
            if action in ("cape.presence", "144"):
                return self.cape_presence(connection, body, device)
            if action in ("145", "146", "147"):
                return self.invsee_client(connection, action, device)
            if action == "party.state":
                status, response = self.party_state(connection, device)
                if status == 200:
                    response.update(self.client_control_state(connection, device, body.get("clientSession")))
                return status, response
            if action == "party.create":
                return self.party_create(connection, body, device, source_ip)
            if action == "party.join":
                return self.party_join(connection, body, device, source_ip)
            if action == "party.leave":
                return self.party_leave(connection, device, source_ip)
            if action == "party.kick":
                return self.party_kick(connection, body, device, source_ip)
            if action == "party.command":
                return self.party_command(connection, body, device, source_ip)
            if action == "party.poll":
                return self.party_poll(connection, device, body)
            return 400, {"ok": False, "error": "unknown_action"}

    def invsee_client(self, connection, action, device):
        device_id = device["id"]
        if action == "145":
            code = "".join(secrets.choice("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")
                           for _ in range(30))
            expires = utcnow() + timedelta(minutes=10)
            connection.execute(
                "INSERT INTO invsee_links(device_id,code_hash,code_expires_at) VALUES(?,?,?) "
                "ON CONFLICT(device_id) DO UPDATE SET code_hash=excluded.code_hash,"
                "code_expires_at=excluded.code_expires_at,guild_id=NULL,channel_id=NULL,"
                "payload=NULL,expires_at=NULL", (device_id, digest(code), iso(expires)))
            return 200, {"ok": True, "code": code, "expiresAt": int(expires.timestamp() * 1000)}
        if action == "147":
            connection.execute("DELETE FROM invsee_links WHERE device_id=?", (device_id,))
            return 200, {"ok": True}
        row = connection.execute("SELECT * FROM invsee_links WHERE device_id=?", (device_id,)).fetchone()
        if not row:
            return 200, {"ok": True, "status": "unpaired"}
        if not row["channel_id"]:
            expires = parse_time(row["code_expires_at"])
            return 200, {"ok": True, "status": "pending" if expires and expires > utcnow() else "code_expired"}
        if not row["payload"]:
            return 200, {"ok": True, "status": "waiting"}
        expires = parse_time(row["expires_at"])
        if not expires or expires <= utcnow():
            return 200, {"ok": True, "status": "stale"}
        return 200, {"ok": True, "status": "ready", **json.loads(row["payload"])}

    def invsee_bot(self, body, authorization):
        secret = self.invsee_bot_token
        if (len(secret) < 32 or secret == self.admin_token
                or not isinstance(authorization, str)
                or not hmac.compare_digest(authorization.encode("utf-8"), ("Bearer " + secret).encode("utf-8"))):
            return 401, {"ok": False, "error": "unauthorized"}
        guild, channel = body.get("guildId"), body.get("channelId")
        if not all(isinstance(value, str) and re.fullmatch(r"[0-9]{17,20}", value)
                   for value in (guild, channel)):
            return 400, {"ok": False, "error": "invalid_ticket"}
        action = body.get("action")
        with self.lock, self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            if action == "bind":
                code = body.get("code")
                if not isinstance(code, str) or not re.fullmatch(r"[A-Za-z0-9]{30}", code):
                    return 400, {"ok": False, "error": "invalid_code"}
                row = connection.execute("SELECT * FROM invsee_links WHERE code_hash=?", (digest(code),)).fetchone()
                if not row or (parse_time(row["code_expires_at"]) or utcnow()) <= utcnow():
                    return 400, {"ok": False, "error": "code_expired_or_used"}
                connection.execute("UPDATE invsee_links SET code_hash=NULL,code_expires_at=NULL,"
                                   "guild_id=?,channel_id=? WHERE device_id=?", (guild, channel, row["device_id"]))
                return 200, {"ok": True}
            if action == "clear":
                connection.execute("UPDATE invsee_links SET payload=NULL,expires_at=NULL "
                                   "WHERE guild_id=? AND channel_id=?", (guild, channel))
                return 200, {"ok": True}
            if action != "publish":
                return 400, {"ok": False, "error": "unknown_action"}
            players, dead, ttl = body.get("players"), body.get("deadPlayers", []), body.get("ttlSeconds", 180)
            if (not isinstance(players, list) or len(players) > 100
                    or not isinstance(dead, list) or len(dead) > 100
                    or type(ttl) is not int or not 30 <= ttl <= 900):
                return 400, {"ok": False, "error": "invalid_report"}
            seen, cleaned = set(), []
            for player in players:
                if not isinstance(player, dict):
                    return 400, {"ok": False, "error": "invalid_report"}
                name, sets = player.get("name"), player.get("sets")
                if (not isinstance(name, str) or not re.fullmatch(r"[A-Za-z0-9_]{1,16}", name)
                        or type(sets) is not int or not 0 <= sets <= 100000
                        or name.lower() in seen):
                    return 400, {"ok": False, "error": "invalid_report"}
                armor = {}
                for piece in ("helmet", "chestplate", "leggings", "boots"):
                    count = player.get(piece, 0)
                    if type(count) is not int or not 0 <= count <= 100000:
                        return 400, {"ok": False, "error": "invalid_report"}
                    armor[piece] = count
                seen.add(name.lower())
                cleaned.append({"name": name, "sets": sets, **armor})
            if any(not isinstance(name, str) or not re.fullmatch(r"[A-Za-z0-9_]{1,16}", name)
                   or name.lower() in seen for name in dead):
                return 400, {"ok": False, "error": "invalid_report"}
            cleaned.sort(key=lambda player: player["sets"])
            now = utcnow()
            expires = now + timedelta(seconds=ttl)
            payload = {"players": cleaned, "deadPlayers": list(dict.fromkeys(dead)),
                       "updatedAt": int(now.timestamp() * 1000), "expiresAt": int(expires.timestamp() * 1000)}
            connection.execute("UPDATE invsee_links SET payload=?,expires_at=? "
                               "WHERE guild_id=? AND channel_id=?",
                               (json_bytes(payload).decode("utf-8"), iso(expires), guild, channel))
            return 200, {"ok": True}

    def cape_presence(
        self, connection: sqlite3.Connection, body: dict[str, object],
        device: dict[str, object]
    ) -> tuple[int, dict[str, object]]:
        server_key = str(body.get("server") or "").lower()
        player_name = str(body.get("player") or "")
        style = str(body.get("style") or "")
        visible = body.get("visible", True)
        if not re.fullmatch(r"[0-9a-f]{64}", server_key):
            return 400, {"ok": False, "error": "invalid_server"}
        if not re.fullmatch(r"[A-Za-z0-9_]{1,16}", player_name):
            return 400, {"ok": False, "error": "invalid_player"}
        if not isinstance(visible, bool):
            return 400, {"ok": False, "error": "invalid_visibility"}
        legacy_style = style in ("tampinhas_group", "tampinhas_logo")
        style_ids = {
            "122": "122",
            "123": "123",
            "718": "718",
            "436": "436",
            "953": "953",
            "tampinhas_group": "122",
            "tampinhas_logo": "123",
        }
        if visible and style not in style_ids:
            return 400, {"ok": False, "error": "invalid_cape"}
        if visible:
            style = style_ids[style]
        now = utcnow()
        cutoff = iso(now - timedelta(seconds=45))
        connection.execute(
            "DELETE FROM cape_presence WHERE last_seen_at<?", (cutoff,)
        )
        if visible:
            connection.execute(
                "INSERT INTO cape_presence(device_id,server_key,player_name,style,last_seen_at) "
                "VALUES(?,?,?,?,?) ON CONFLICT(device_id) DO UPDATE SET "
                "server_key=excluded.server_key,player_name=excluded.player_name,"
                "style=excluded.style,last_seen_at=excluded.last_seen_at",
                (device["id"], server_key, player_name, style, iso(now)),
            )
        else:
            connection.execute("DELETE FROM cape_presence WHERE device_id=?", (device["id"],))
        rows = connection.execute(
            "SELECT player_name,style FROM cape_presence "
            "WHERE server_key=? AND last_seen_at>=? ORDER BY player_name COLLATE NOCASE",
            (server_key, cutoff),
        ).fetchall()
        return 200, {
            "ok": True,
            "players": [
                {
                    "name": row["player_name"],
                    "style": (
                        {"122": "tampinhas_group", "123": "tampinhas_logo"}
                        .get(row["style"], row["style"])
                        if legacy_style else
                        {"tampinhas_group": "122", "tampinhas_logo": "123"}
                        .get(row["style"], row["style"])
                    ),
                }
                for row in rows
            ],
        }

    def create_license(
        self,
        role: str,
        note: str = "",
        expires_at: str | None = None,
        license_key: str | None = None,
        admin: dict[str, str] | None = None,
    ) -> dict[str, object]:
        if role not in ROLES:
            raise ValueError("invalid_role")
        key = license_key or secrets.token_hex(24)
        if not safe_field(key, 128) or len(key) < 20:
            raise ValueError("invalid_license")
        if expires_at and (not parse_time(expires_at) or parse_time(expires_at) <= utcnow()):
            raise ValueError("invalid_expiry")
        expires_at = iso(parse_time(expires_at)) if expires_at else None
        row_id = self.license_id(key)
        with self.lock, self.connect() as connection:
            connection.execute(
                "INSERT INTO licenses(id,status,role,note,expires_at,created_at,key_ciphertext,key_hint) VALUES(?,?,?,?,?,?,?,?)",
                (row_id, "unused", role, note[:256], expires_at, iso(),
                 self.license_cipher.encrypt(key.encode("utf-8")).decode("ascii"), key[-6:]),
            )
            self.audit_event(
                connection,
                "admin.license.created",
                admin,
                details={"licenseId": row_id, "role": role, "expiresAt": expires_at or ""},
            )
        return {"ok": True, "licenseKey": key, "id": row_id, "role": role}

    def list_admins(self) -> list[dict[str, object]]:
        with self.connect() as connection:
            rows = connection.execute(
                "SELECT id,name,status,access_level,created_at,last_login_at FROM admins "
                "ORDER BY CASE access_level WHEN 'owner' THEN 0 ELSE 1 END, created_at"
            ).fetchall()
        return [dict(row) for row in rows]

    def create_admin(
        self, name: object, actor: dict[str, str] | None = None
    ) -> dict[str, object]:
        normalized_name = str(name).strip() if isinstance(name, str) else ""
        if not safe_field(normalized_name, 32):
            raise ValueError("invalid_admin_name")
        token = "svadm_" + secrets.token_urlsafe(32)
        admin_id = "ad_" + secrets.token_hex(16)
        created_at = iso()
        with self.lock, self.connect() as connection:
            connection.execute(
                "INSERT INTO admins(id,name,token_hash,status,access_level,created_at) "
                "VALUES(?,?,?,?,?,?)",
                (admin_id, normalized_name, digest(token), "active", "admin", created_at),
            )
            self.audit_event(
                connection,
                "admin.account.created",
                actor,
                {"adminId": admin_id, "name": normalized_name},
            )
        return {
            "ok": True,
            "adminToken": token,
            "admin": {
                "id": admin_id,
                "name": normalized_name,
                "status": "active",
                "access_level": "admin",
                "created_at": created_at,
                "last_login_at": None,
            },
        }

    def revoke_admin(self, admin_id: str, actor: dict[str, str]) -> bool:
        if not re.fullmatch(r"ad_[0-9a-f]{32}", admin_id):
            raise ValueError("invalid_admin")
        if actor.get("id") == admin_id:
            raise ValueError("cannot_revoke_self")
        with self.lock, self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            active_count = connection.execute(
                "SELECT COUNT(*) FROM admins WHERE status='active'"
            ).fetchone()[0]
            if active_count <= 1:
                raise ValueError("last_admin")
            row = connection.execute("SELECT name,status FROM admins WHERE id=?", (admin_id,)).fetchone()
            if not row:
                return False
            if row["status"] != "active":
                return True
            connection.execute("UPDATE admins SET status='revoked' WHERE id=?", (admin_id,))
            for session_id, (_, principal) in list(self.admin_sessions.items()):
                if principal.get("id") == admin_id:
                    self.admin_sessions.pop(session_id, None)
            for ticket_id, (_, principal) in list(self.admin_tickets.items()):
                if principal.get("id") == admin_id:
                    self.admin_tickets.pop(ticket_id, None)
            self.audit_event(
                connection,
                "admin.account.revoked",
                actor,
                {"adminId": admin_id, "name": row["name"]},
            )
            return True

    def payload_status(self) -> dict[str, dict[str, object]]:
        payloads: dict[str, dict[str, object]] = {}
        for role in ("release", "tester", "developer"):
            path = self.selected_payload("user" if role == "release" else role)
            payloads[role] = {
                "present": bool(path and path.is_file()),
                "bytes": path.stat().st_size if path and path.is_file() else 0,
                "updatedAt": iso(datetime.fromtimestamp(path.stat().st_mtime, timezone.utc))
                if path and path.is_file() else None,
            }
        return payloads

    def admin_overview(self) -> dict[str, object]:
        now = iso()
        online_since = iso(utcnow() - timedelta(minutes=15))
        today = utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
        with self.connect() as connection:
            counts = {
                "users": connection.execute("SELECT COUNT(*) FROM devices").fetchone()[0],
                "activeUsers": connection.execute(
                    "SELECT COUNT(*) FROM devices WHERE status='active'"
                ).fetchone()[0],
                "onlineUsers": connection.execute(
                    "SELECT COUNT(*) FROM devices WHERE status='active' AND last_seen_at>=?",
                    (online_since,),
                ).fetchone()[0],
                "configs": connection.execute("SELECT COUNT(*) FROM configs").fetchone()[0],
                "communityConfigs": connection.execute(
                    "SELECT COUNT(*) FROM community_configs"
                ).fetchone()[0],
                "openParties": connection.execute(
                    "SELECT COUNT(*) FROM parties WHERE status='open'"
                ).fetchone()[0],
                "injectionsToday": connection.execute(
                    "SELECT COUNT(*) FROM audit_events WHERE event_type='client.injected' AND created_at>=?",
                    (iso(today),),
                ).fetchone()[0],
                "unusedLicenses": connection.execute(
                    "SELECT COUNT(*) FROM licenses WHERE status='unused' "
                    "AND (expires_at IS NULL OR expires_at>?)",
                    (now,),
                ).fetchone()[0],
                "admins": connection.execute(
                    "SELECT COUNT(*) FROM admins WHERE status='active'"
                ).fetchone()[0],
            }
        return {"ok": True, "counts": counts, "payloads": self.payload_status(), "generatedAt": now}

    def list_users(self) -> list[dict[str, object]]:
        now = iso()
        with self.connect() as connection:
            rows = connection.execute(
                "SELECT d.id,d.username,d.status,d.role,d.license_id,d.last_seen_at,d.activated_at,"
                "(SELECT COUNT(*) FROM configs c WHERE c.device_id=d.id) AS config_count,"
                "(SELECT COUNT(*) FROM sessions s WHERE s.device_id=d.id AND s.expires_at>?) AS session_count,"
                "(SELECT p.join_code FROM party_members pm JOIN parties p ON p.id=pm.party_id "
                " WHERE pm.device_id=d.id LIMIT 1) AS party_code "
                "FROM devices d ORDER BY COALESCE(d.last_seen_at,d.activated_at) DESC LIMIT 2000",
                (now,),
            ).fetchall()
        return [dict(row) for row in rows]

    def list_audit(self, query: dict[str, list[str]]) -> list[dict[str, object]]:
        try:
            limit = max(1, min(500, int(query.get("limit", ["200"])[0])))
        except ValueError:
            limit = 200
        event_filter = query.get("event", [""])[0].strip()[:64]
        search = query.get("q", [""])[0].strip()[:128]
        where: list[str] = []
        arguments: list[object] = []
        if event_filter:
            where.append("event_type LIKE ?")
            arguments.append(event_filter + ("%" if "." not in event_filter else ""))
        if search:
            where.append("(actor_username LIKE ? OR actor_device_id LIKE ? OR details LIKE ?)")
            pattern = f"%{search}%"
            arguments.extend((pattern, pattern, pattern))
        clause = " WHERE " + " AND ".join(where) if where else ""
        arguments.append(limit)
        with self.connect() as connection:
            rows = connection.execute(
                "SELECT id,event_type,actor_device_id,actor_username,actor_role,source_ip,details,created_at "
                f"FROM audit_events{clause} ORDER BY id DESC LIMIT ?",
                arguments,
            ).fetchall()
        events: list[dict[str, object]] = []
        for row in rows:
            event = dict(row)
            try:
                event["details"] = json.loads(str(event["details"]))
            except json.JSONDecodeError:
                event["details"] = {}
            events.append(event)
        return events

    def update_user(
        self,
        device_id: str,
        body: dict[str, object],
        admin: dict[str, str] | None = None,
    ) -> dict[str, object] | None:
        if not HWID_RE.fullmatch(device_id):
            raise ValueError("invalid_device")
        fields: list[str] = []
        values: list[object] = []
        if "status" in body:
            if body["status"] not in ("active", "disabled"):
                raise ValueError("invalid_status")
            fields.append("status=?")
            values.append(body["status"])
        if "role" in body:
            if body["role"] not in ROLES:
                raise ValueError("invalid_role")
            fields.append("role=?")
            values.append(body["role"])
        if "username" in body:
            if not safe_field(body["username"], 32):
                raise ValueError("invalid_username")
            fields.append("username=?")
            values.append(body["username"])
        if not fields:
            raise ValueError("no_changes")
        with self.lock, self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            before = connection.execute("SELECT * FROM devices WHERE id=?", (device_id,)).fetchone()
            if not before:
                return None
            values.append(device_id)
            connection.execute(f"UPDATE devices SET {','.join(fields)} WHERE id=?", values)
            if body.get("role") in ROLES:
                connection.execute(
                    "UPDATE licenses SET role=? WHERE id=?", (body["role"], before["license_id"])
                )
            if body.get("status") == "disabled":
                connection.execute("DELETE FROM sessions WHERE device_id=?", (device_id,))
                connection.execute("DELETE FROM payload_tokens WHERE device_id=?", (device_id,))
                self.party_leave(connection, dict(before))
            after = connection.execute(
                "SELECT id,username,status,role,license_id,last_seen_at,activated_at FROM devices WHERE id=?",
                (device_id,),
            ).fetchone()
            self.audit_event(
                connection,
                "admin.user.updated",
                admin,
                details={
                    "deviceId": device_id,
                    "status": after["status"],
                    "role": after["role"],
                    "username": after["username"],
                },
            )
            return dict(after)

    def revoke_user_sessions(
        self, device_id: str, admin: dict[str, str] | None = None
    ) -> int | None:
        if not HWID_RE.fullmatch(device_id):
            raise ValueError("invalid_device")
        with self.lock, self.connect() as connection:
            if not connection.execute("SELECT 1 FROM devices WHERE id=?", (device_id,)).fetchone():
                return None
            removed = connection.execute("DELETE FROM sessions WHERE device_id=?", (device_id,)).rowcount
            connection.execute("DELETE FROM payload_tokens WHERE device_id=?", (device_id,))
            connection.execute("DELETE FROM download_tokens WHERE device_id=?", (device_id,))
            connection.execute("UPDATE client_presence SET status='revoked',stopped_at=? WHERE device_id=? AND status='running'", (iso(), device_id))
            self.audit_event(
                connection,
                "admin.sessions.revoked",
                admin,
                details={"deviceId": device_id, "sessions": removed},
            )
            return removed

    def revoke_user_access(
        self, device_id: str, admin: dict[str, str] | None = None
    ) -> dict[str, object] | None:
        """Remove every live account binding while retaining an audit trail."""
        if not HWID_RE.fullmatch(device_id):
            raise ValueError("invalid_device")
        with self.lock, self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            device = connection.execute(
                "SELECT * FROM devices WHERE id=?", (device_id,)
            ).fetchone()
            if not device:
                return None

            device_snapshot = dict(device)
            license_id = str(device_snapshot["license_id"])
            self.party_leave(connection, device_snapshot)
            removed = {
                "sessions": connection.execute(
                    "DELETE FROM sessions WHERE device_id=?", (device_id,)
                ).rowcount,
                "payloadTokens": connection.execute(
                    "DELETE FROM payload_tokens WHERE device_id=?", (device_id,)
                ).rowcount,
                "configs": connection.execute(
                    "DELETE FROM configs WHERE device_id=?", (device_id,)
                ).rowcount,
                "legacyPrivateData": connection.execute(
                    "DELETE FROM legacy_private_data WHERE device_id=?", (device_id,)
                ).rowcount,
                "communityConfigs": connection.execute(
                    "DELETE FROM community_configs WHERE owner_device_id=?", (device_id,)
                ).rowcount,
                "partyEvents": connection.execute(
                    "DELETE FROM party_events WHERE target_device_id=? OR actor_device_id=?",
                    (device_id, device_id),
                ).rowcount,
            }
            connection.execute(
                "UPDATE licenses SET status='revoked',bound_device_id=NULL WHERE id=?",
                (license_id,),
            )
            connection.execute("DELETE FROM devices WHERE id=?", (device_id,))
            self.audit_event(
                connection,
                "admin.user.access_revoked",
                admin,
                details={
                    "deviceId": device_id,
                    "username": device_snapshot["username"],
                    "licenseId": license_id,
                    **removed,
                },
            )
            return {
                "deviceId": device_id,
                "username": device_snapshot["username"],
                "licenseId": license_id,
                "licenseStatus": "revoked",
                "removed": removed,
            }

    def revoke_license(
        self, license_id: str, admin: dict[str, str] | None = None
    ) -> bool:
        if not re.fullmatch(r"lc_[0-9a-f]{32}", license_id):
            raise ValueError("invalid_license")
        with self.lock, self.connect() as connection:
            changed = connection.execute(
                "UPDATE licenses SET status='revoked' WHERE id=?", (license_id,)
            ).rowcount
            if changed:
                self.audit_event(
                    connection,
                    "admin.license.revoked",
                    admin,
                    details={"licenseId": license_id},
                )
            return bool(changed)

    def list_table(self, table: str) -> list[dict[str, object]]:
        allowed = {"licenses", "devices", "configs", "community_configs", "parties", "party_members", "audit_events"}
        allowed.update({"client_presence", "downloads"})
        if table not in allowed:
            raise ValueError("invalid_table")
        with self.connect() as connection:
            rows = connection.execute(f"SELECT * FROM {table} ORDER BY rowid DESC LIMIT 1000").fetchall()
            return [dict(row) for row in rows]

    def cleanup(self, admin: dict[str, str] | None = None) -> dict[str, object]:
        now = iso()
        with self.lock, self.connect() as connection:
            sessions = connection.execute("DELETE FROM sessions WHERE expires_at<=?", (now,)).rowcount
            tokens = connection.execute("DELETE FROM payload_tokens WHERE expires_at<=?", (now,)).rowcount
            events = connection.execute("DELETE FROM party_events WHERE consumed=1", ()).rowcount
            audit_events = connection.execute(
                "DELETE FROM audit_events WHERE created_at<?", (iso(utcnow() - AUDIT_RETENTION),)
            ).rowcount
            removed = {
                "sessions": sessions,
                "payloadTokens": tokens,
                "events": events,
                "auditEvents": audit_events,
            }
            self.audit_event(connection, "admin.cleanup", admin, details=removed)
        return {"ok": True, "removed": removed}

    def consume_payload_token(self, payload_id: str, token: str, source_ip: str = "") -> Path | None:
        if DOWNLOAD_RE.fullmatch(payload_id):
            return self.consume_client_payload_token(payload_id, token, source_ip)
        if payload_id not in ("release", "tester", "developer") or not safe_field(token, 128):
            return None
        with self.lock, self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            token_id = digest(token)
            row = connection.execute("SELECT * FROM payload_tokens WHERE id=?", (token_id,)).fetchone()
            if not row or row["payload_id"] != payload_id:
                return None
            connection.execute("DELETE FROM payload_tokens WHERE id=?", (token_id,))
            expiry = parse_time(row["expires_at"])
            if not expiry or expiry <= utcnow():
                return None
            path = self.payload_dir / f"{payload_id}.dll"
            if not path.is_file():
                return None
            device = None
            if row["device_id"]:
                device = connection.execute(
                    "SELECT * FROM devices WHERE id=?", (row["device_id"],)
                ).fetchone()
            if not device or device["status"] != "active" or not self.device_license_active(connection, device):
                return None
            self.audit_event(
                connection,
                "payload.downloaded",
                device,
                {"payload": payload_id, "bytes": path.stat().st_size},
                source_ip,
            )
            return path

    def mapping_device(self, token: object) -> dict[str, object] | None:
        with self.lock, self.connect() as connection:
            return self.authorize_session(connection, token)

    def legacy_api(
        self, token: str, action: str, body: dict[str, object] | None = None, source_ip: str = ""
    ) -> tuple[int, dict[str, object]]:
        """Serve the Java client's authenticated account and private-profile API."""
        with self.lock, self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            device = self.authorize_session(connection, token)
            if not device:
                return 401, {"successful": False, "error": "unauthorized"}
            if action == "authenticated":
                row = connection.execute(
                    "SELECT activated_at FROM devices WHERE id=?", (device["id"],)
                ).fetchone()
                created = parse_time(row["activated_at"] if row else None) or utcnow()
                data: dict[str, object] = {
                    "userId": int(digest(str(device["id"]))[:15], 16),
                    "username": device["username"],
                    "accountCreation": created.isoformat(timespec="milliseconds").replace("+00:00", "Z"),
                    "licensed": True, "registered": True,
                    "profiles": True, "banned": False,
                }
                # Signed license contract: the client must verify the RSA
                # signature against the compiled-in public key; without this
                # field an up-to-date client refuses to activate (fail closed).
                license_doc = self.build_signed_license(device)
                if license_doc is not None:
                    data["license"] = license_doc
                return 200, {"successful": True, "data": data}

            row = connection.execute(
                "SELECT payload FROM legacy_private_data WHERE device_id=?", (device["id"],)
            ).fetchone()
            try:
                data = json.loads(row["payload"]) if row else {}
            except (TypeError, json.JSONDecodeError):
                return 500, {"successful": False, "error": "stored_profile_invalid"}
            if not isinstance(data, dict):
                return 500, {"successful": False, "error": "stored_profile_invalid"}
            data.setdefault("friends", [])
            data.setdefault("otherData", [])
            data.setdefault("profiles", {})
            if action == "profile/private/all":
                if data["profiles"]:
                    self.audit_event(connection, "config.loaded", device, {"source": "profiles", "count": len(data["profiles"])}, source_ip)
                return 200, {"successful": True, "data": {
                    **data, "publicProfiles": {},
                }}
            if action == "profile/private/reserve":
                return 200, {"successful": True, "data": str(uuid.uuid4())}
            if not isinstance(body, dict):
                return 400, {"successful": False, "error": "invalid_request"}
            if action == "profile/private/save/user":
                friends, other_data = body.get("friends"), body.get("otherData")
                if not isinstance(friends, list) or not isinstance(other_data, list):
                    return 400, {"successful": False, "error": "invalid_user_data"}
                data["friends"], data["otherData"] = friends, other_data
                result: object = True
            elif action == "profile/private/save/profile":
                updates, deletions = body.get("updatedProfiles"), body.get("deletedProfiles")
                if not isinstance(updates, list) or not isinstance(deletions, list):
                    return 400, {"successful": False, "error": "invalid_profile_data"}
                profiles = data["profiles"]
                if not isinstance(profiles, dict):
                    return 500, {"successful": False, "error": "stored_profile_invalid"}
                for profile_id in deletions:
                    try:
                        removed_profile = profiles.pop(str(uuid.UUID(str(profile_id))), None)
                        if removed_profile:
                            self.audit_event(connection, "config.deleted", device, {"source": "profiles", "profileId": str(profile_id)}, source_ip)
                    except (ValueError, TypeError):
                        return 400, {"successful": False, "error": "invalid_profile_id"}
                saved: dict[str, object] = {}
                for profile in updates:
                    if not isinstance(profile, dict) or not isinstance(profile.get("data"), dict):
                        return 400, {"successful": False, "error": "invalid_profile_data"}
                    try:
                        profile_id = str(uuid.UUID(str(profile.get("profileId") or uuid.uuid4())))
                        owner_id = str(uuid.UUID(str(profile.get("uuid"))))
                    except (ValueError, TypeError):
                        return 400, {"successful": False, "error": "invalid_profile_id"}
                    if not safe_field(profile.get("name"), 48) or not safe_field(profile.get("cv"), 32):
                        return 400, {"successful": False, "error": "invalid_profile_data"}
                    stored = {"uuid": owner_id, "profileId": profile_id,
                              "name": profile["name"], "cv": profile["cv"],
                              "data": profile["data"]}
                    previous = profiles.get(profile_id)
                    if previous != stored:
                        self.audit_event(connection, "config.saved" if previous else "config.created", device,
                                         {"source": "profiles", "profileId": profile_id, "name": profile["name"]}, source_ip)
                    profiles[profile_id] = stored
                    saved[profile_id] = stored
                result = saved
            else:
                return 404, {"successful": False, "error": "not_found"}
            encoded = json_bytes(data)
            if len(encoded) > MAX_CONFIG_BYTES:
                return 413, {"successful": False, "error": "config_too_large"}
            connection.execute(
                "INSERT INTO legacy_private_data(device_id,payload,updated_at) VALUES(?,?,?) "
                "ON CONFLICT(device_id) DO UPDATE SET payload=excluded.payload,updated_at=excluded.updated_at",
                (device["id"], encoded.decode("utf-8"), iso()),
            )
            return 200, {"successful": True, "data": result}

    def mapping_profile(self, patch_sha256: str) -> Path | None:
        if not re.fullmatch(r"[0-9a-f]{64}", patch_sha256):
            return None
        profile = self.mapping_dir / "profiles" / patch_sha256
        required = (
            "anchors.properties", "classes.tsv", "fields.tsv", "fields_fwd.tsv",
            "methods.tsv", "methods_fwd.tsv", "mapping-update.json",
        )
        return profile if all((profile / name).is_file() for name in required) else None

    def mapping_status(self, patch_sha256: str) -> dict[str, object]:
        profile = self.mapping_profile(patch_sha256)
        state_path = self.mapping_dir / "state.json"
        state: dict[str, object] = {}
        if state_path.is_file():
            try:
                loaded = json.loads(state_path.read_text(encoding="utf-8"))
                if isinstance(loaded, dict):
                    state = loaded
            except (OSError, json.JSONDecodeError):
                pass
        if str(state.get("sha256") or "").lower() != patch_sha256:
            state = {}
        return {
            "ok": True,
            "sha256": patch_sha256,
            "ready": profile is not None,
            "state": "ready" if profile else str(state.get("state") or "missing"),
            "error": None if profile else state.get("error"),
        }

    def submit_mapping_patch(
        self, patch_sha256: str, payload: bytes, device: dict[str, object], source_ip: str
    ) -> dict[str, object]:
        if not re.fullmatch(r"[0-9a-f]{64}", patch_sha256):
            raise ValueError("invalid_mapping_hash")
        actual_hash = hashlib.sha256(payload).hexdigest()
        if actual_hash != patch_sha256 or not payload.startswith(b"PK\x03\x04"):
            raise ValueError("invalid_mapping_patch")
        if self.mapping_profile(patch_sha256):
            return self.mapping_status(patch_sha256)
        target = self.mapping_dir / "incoming" / f"{patch_sha256}.patch"
        if target.is_file():
            return self.mapping_status(patch_sha256)
        temporary = target.with_suffix(".upload")
        temporary.write_bytes(payload)
        os.chmod(temporary, 0o600)
        temporary.replace(target)
        state_path = self.mapping_dir / "state.json"
        state_path.write_text(json.dumps({
            "sha256": patch_sha256, "state": "queued", "error": None,
            "updatedAt": iso(),
        }), encoding="utf-8")
        with self.lock, self.connect() as connection:
            self.audit_event(
                connection, "mapping.patch.uploaded", device,
                {"sha256": patch_sha256, "bytes": len(payload)}, source_ip,
            )
        return self.mapping_status(patch_sha256)


class SonyVegasHandler(AdminRequestFeatures, BaseHTTPRequestHandler):
    server_version = "server"
    sys_version = ""

    def version_string(self) -> str:
        return "server"

    def setup(self) -> None:
        super().setup()
        self.connection.settimeout(15)

    @property
    def backend(self) -> SonyVegasBackend:
        return self.server.backend  # type: ignore[attr-defined]

    def log_message(self, fmt: str, *args: object) -> None:
        # Request-by-request diagnostics are disabled in release builds.
        pass

    @property
    def source_ip(self) -> str:
        peer = str(self.client_address[0])
        forwarded = self.headers.get("X-SonyVegas-Client-IP", "")
        if peer in ("127.0.0.1", "::1") and forwarded:
            try:
                return str(ipaddress.ip_address(forwarded.strip()))
            except ValueError:
                pass
        return peer[:64]

    def rate_limit(self, bucket: str, limit: int, window_seconds: int) -> bool:
        allowed, retry_after = self.server.rate_limiter.allow(  # type: ignore[attr-defined]
            f"{bucket}:{self.source_ip}", limit, window_seconds
        )
        if allowed:
            return True
        self.close_connection = True
        self.send_json(
            429,
            {"ok": False, "error": "rate_limited", "retryAfter": retry_after},
            {
                "Retry-After": str(retry_after),
                "RateLimit-Limit": str(limit),
                "RateLimit-Reset": str(retry_after),
            },
        )
        return False

    def require_json(self) -> bool:
        content_type = self.headers.get("Content-Type", "").split(";", 1)[0].strip().lower()
        if content_type == "application/json":
            return True
        self.send_json(415, {"ok": False, "error": "json_content_type_required"})
        return False

    def same_origin(self) -> bool:
        origin = self.headers.get("Origin", "")
        host = self.headers.get("Host", "")
        fetch_site = self.headers.get("Sec-Fetch-Site", "")
        try:
            origin_host = urlsplit(origin).netloc.lower()
        except ValueError:
            origin_host = ""
        return (
            bool(origin_host)
            and origin_host == host.lower()
            and fetch_site.lower() in ("", "same-origin")
        )

    @staticmethod
    def csp_hashes(body: bytes, tag: bytes) -> str:
        pattern = rb"<" + tag + rb"(?:\s[^>]*)?>(.*?)</" + tag + rb">"
        values = []
        for match in re.finditer(pattern, body, re.DOTALL | re.IGNORECASE):
            encoded = base64.b64encode(hashlib.sha256(match.group(1)).digest()).decode("ascii")
            values.append(f"'sha256-{encoded}'")
        return " ".join(values)

    def send_bytes(
        self,
        status: int,
        body: bytes,
        content_type: str,
        extra_headers: dict[str, str | list[str]] | None = None,
    ) -> None:
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("Pragma", "no-cache")
        self.send_header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "DENY")
        self.send_header("X-Permitted-Cross-Domain-Policies", "none")
        self.send_header("X-Robots-Tag", "noindex, nofollow, noarchive")
        self.send_header("X-XSS-Protection", "0")
        self.send_header("Referrer-Policy", "no-referrer")
        self.send_header("Cross-Origin-Opener-Policy", "same-origin")
        self.send_header("Cross-Origin-Resource-Policy", "same-origin")
        self.send_header(
            "Permissions-Policy",
            "camera=(), microphone=(), geolocation=(), payment=(), usb=(), browsing-topics=()",
        )
        if content_type.startswith("text/html"):
            scripts = self.csp_hashes(body, b"script")
            styles = self.csp_hashes(body, b"style")
            self.send_header(
                "Content-Security-Policy",
                f"default-src 'none'; script-src 'self' {scripts}; style-src 'self' {styles}; "
                "img-src 'self' data:; connect-src 'self'; font-src 'self'; object-src 'none'; "
                "frame-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'; "
                "manifest-src 'none'; worker-src 'none'; upgrade-insecure-requests",
            )
        for name, value in (extra_headers or {}).items():
            if isinstance(value, list):
                for item in value:
                    self.send_header(name, item)
            else:
                self.send_header(name, value)
        self.end_headers()
        self.wfile.write(body)

    def send_json(
        self,
        status: int,
        value: object,
        extra_headers: dict[str, str | list[str]] | None = None,
    ) -> None:
        self.send_bytes(
            status, json_bytes(value), "application/json; charset=utf-8", extra_headers
        )

    def read_body(self, maximum: int = MAX_JSON_BYTES) -> bytes:
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError as error:
            raise ValueError("invalid_content_length") from error
        if length < 0 or length > maximum:
            raise OverflowError("body_too_large")
        return self.rfile.read(length)

    def read_json(self) -> dict[str, object]:
        raw = self.read_body()
        try:
            value = json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as error:
            raise ValueError("invalid_json") from error
        if not isinstance(value, dict):
            raise ValueError("invalid_json")
        return value

    def admin(self) -> bool:
        principal = self.backend.admin_principal(
            self.headers.get("Authorization"), self.headers.get("Cookie")
        )
        if principal:
            self._admin_identity = principal
            return True
        self.send_json(401, {"ok": False, "error": "unauthorized"})
        return False

    def admin_mutation(self) -> bool:
        authorization = self.headers.get("Authorization")
        if authorization:
            principal = self.backend.admin_principal(authorization)
            if principal:
                self._admin_identity = principal
                return True
        if self.headers.get("X-SonyVegas-Admin") != "1":
            self.send_json(403, {"ok": False, "error": "csrf_required"})
            return False
        if not self.same_origin():
            self.send_json(403, {"ok": False, "error": "invalid_origin"})
            return False
        return self.admin()

    def owner_mutation(self) -> bool:
        if not self.admin_mutation():
            return False
        if self._admin_identity.get("access") != "owner":
            self.send_json(403, {"ok": False, "error": "owner_required"})
            return False
        return True

    def do_GET(self) -> None:
        if not self.rate_limit("all", 1200, 60):
            return
        parsed = urlsplit(self.path)
        if self.feature_get(parsed):
            return
        legacy_get = re.fullmatch(
            r"/api/v1/([^/]+)/(authenticated|profile/private/all)", parsed.path
        )
        if legacy_get:
            if not self.rate_limit("legacy-api", 240, 60):
                return
            status, result = self.backend.legacy_api(legacy_get.group(1), legacy_get.group(2), source_ip=self.source_ip)
            self.send_json(status, result)
            return
        if parsed.path == "/health":
            if not self.rate_limit("health", 120, 60):
                return
            self.send_json(200, {"ok": True, "service": "unknown-control"})
            return
        if parsed.path == "/v1/mappings/cheatbreaker/status":
            if not self.rate_limit("mapping-status", 240, 60):
                return
            query = parse_qs(parsed.query)
            token = query.get("token", [""])[0]
            patch_sha256 = query.get("sha256", [""])[0].lower()
            if not self.backend.mapping_device(token):
                self.send_json(401, {"ok": False, "error": "unauthorized"})
                return
            if not re.fullmatch(r"[0-9a-f]{64}", patch_sha256):
                self.send_json(400, {"ok": False, "error": "invalid_mapping_hash"})
                return
            self.send_json(200, self.backend.mapping_status(patch_sha256))
            return
        mapping_file = re.fullmatch(
            r"/v1/mappings/cheatbreaker/([0-9a-f]{64})/([^/]+)", parsed.path
        )
        if mapping_file:
            if not self.rate_limit("mapping-download", 180, 60):
                return
            token = parse_qs(parsed.query).get("token", [""])[0]
            if not self.backend.mapping_device(token):
                self.send_json(401, {"ok": False, "error": "unauthorized"})
                return
            patch_sha256, file_name = mapping_file.groups()
            allowed = {
                "anchors.properties", "classes.tsv", "fields.tsv", "fields_fwd.tsv",
                "methods.tsv", "methods_fwd.tsv", "mapping-update.json",
            }
            profile = self.backend.mapping_profile(patch_sha256)
            if file_name not in allowed or not profile:
                self.send_json(404, {"ok": False, "error": "mapping_unavailable"})
                return
            self.send_bytes(200, (profile / file_name).read_bytes(), "application/octet-stream")
            return
        if parsed.path in ("/", "/admin", "/admin/"):
            if not self.rate_limit("admin-page", 60, 60):
                return
            dashboard = Path(__file__).with_name("admin_dashboard.html")
            if not dashboard.is_file():
                self.send_json(503, {"ok": False, "error": "dashboard_unavailable"})
                return
            self.send_bytes(200, dashboard.read_bytes(), "text/html; charset=utf-8")
            return
        match = re.fullmatch(r"/v1/storage/buckets/payloads/files/([^/]+)/download", parsed.path)
        direct_match = re.fullmatch(r"/v1/payloads/(release|tester|developer|dl_[0-9a-f]{32})/download", parsed.path)
        if direct_match:
            match = direct_match
        if match:
            if not self.rate_limit("payload", 60, 60):
                return
            token = parse_qs(parsed.query).get("token", [""])[0]
            path = self.backend.consume_payload_token(
                unquote(match.group(1)), token, self.source_ip
            )
            if not path:
                self.send_json(403, {"ok": False, "error": "invalid_download_token"})
                return
            self.send_download_file(path, "unknown.dll")
            return
        if parsed.path.startswith("/v1/admin/"):
            if not self.rate_limit("admin-read", 180, 60):
                return
            if not self.admin():
                return
            if parsed.path == "/v1/admin/status":
                self.send_json(200, {"ok": True, "payloads": self.backend.payload_status()})
                return
            if parsed.path == "/v1/admin/overview":
                self.send_json(200, self.backend.admin_overview())
                return
            if parsed.path == "/v1/admin/admins":
                self.send_json(200, {
                    "ok": True,
                    "admins": self.backend.list_admins(),
                    "currentAdmin": dict(self._admin_identity),
                })
                return
            if parsed.path == "/v1/admin/licenses":
                self.send_json(200, {"ok": True, "licenses": self.backend.list_table("licenses")})
                return
            if parsed.path == "/v1/admin/devices":
                self.send_json(200, {"ok": True, "devices": self.backend.list_users()})
                return
            if parsed.path == "/v1/admin/users":
                self.send_json(200, {"ok": True, "users": self.backend.list_users()})
                return
            if parsed.path == "/v1/admin/audit":
                self.send_json(
                    200,
                    {"ok": True, "events": self.backend.list_audit(parse_qs(parsed.query))},
                )
                return
            if parsed.path == "/v1/admin/export":
                self.send_json(200, {"version": 2, "exportedAt": iso(), "tables": {
                    name: self.backend.list_table(name)
                    for name in (
                        "licenses", "devices", "configs", "community_configs", "parties", "party_members", "audit_events", "client_presence", "downloads"
                    )
                }})
                return
        self.send_json(404, {"ok": False, "error": "not_found"})

    def do_POST(self) -> None:
        if not self.rate_limit("all", 1200, 60):
            return
        parsed = urlsplit(self.path)
        try:
            if self.feature_post(parsed):
                return
            legacy_post = re.fullmatch(
                r"/api/v1/([^/]+)/profile/private/(save/user|save/profile|reserve)/?",
                parsed.path,
            )
            if legacy_post:
                if not self.rate_limit("legacy-api", 240, 60):
                    return
                action = "profile/private/" + legacy_post.group(2)
                if action == "profile/private/reserve":
                    body = {}
                else:
                    if not self.require_json():
                        return
                    body = self.read_json()
                status, result = self.backend.legacy_api(
                    legacy_post.group(1), action, body, self.source_ip
                )
                self.send_json(status, result)
                return
            if parsed.path == "/v1/admin/login":
                if not self.rate_limit("admin-login", 8, 300):
                    return
                if not self.require_json():
                    return
                body = self.read_json()
                login = self.backend.create_admin_session(body)
                if not login:
                    with self.backend.lock, self.backend.connect() as connection:
                        self.backend.audit_event(
                            connection,
                            "admin.login.failed",
                            details={"method": "ticket" if body.get("ticket") else "token"},
                            source_ip=self.source_ip,
                        )
                    self.send_json(401, {"ok": False, "error": "unauthorized"})
                    return
                if login.get("mfaRequired"):
                    self.send_json(200, {"ok": True, "mfaRequired": True})
                    return
                principal = login["principal"]
                if not login.get("mfaSetupRequired"):
                    with self.backend.lock, self.backend.connect() as connection:
                        self.backend.audit_event(
                            connection,
                            "admin.login",
                            principal,
                            details={"method": "ticket" if body.get("ticket") else "token", "mfa": True},
                            source_ip=self.source_ip,
                        )
                self.send_json(
                    200,
                    {"ok": True, "admin": principal if not login.get("mfaSetupRequired") else None,
                     "mfaSetupRequired": bool(login.get("mfaSetupRequired")),
                     "sessionToken": login["token"] if self.headers.get("X-SonyVegas-CLI") == "1" and not login.get("mfaSetupRequired") else None},
                    {"Set-Cookie": [
                        (
                            f"sv_admin={login['token']}; Path=/; "
                            f"Max-Age={int(ADMIN_SESSION_LIFETIME.total_seconds())}; "
                            "HttpOnly; Secure; SameSite=Strict"
                        ),
                        (
                            f"__Host-sv_admin={login['token']}; Path=/; "
                            f"Max-Age={int(ADMIN_SESSION_LIFETIME.total_seconds())}; "
                            "HttpOnly; Secure; SameSite=Strict"
                        ),
                    ]},
                )
                return
            if parsed.path == "/v1/invsee/sync":
                if not self.rate_limit("invsee", 120, 60) or not self.require_json():
                    return
                status, result = self.backend.invsee_bot(self.read_json(), self.headers.get("Authorization"))
                self.send_json(status, result)
                return
            if parsed.path in ("/v1/functions/sonyvegas-auth/executions", "/v1/control"):
                if not self.rate_limit("control", 600, 60):
                    return
                if not self.require_json():
                    return
                outer = self.read_json()
                if parsed.path.endswith("/executions"):
                    raw_inner = outer.get("body")
                    if not isinstance(raw_inner, str):
                        raise ValueError("invalid_request")
                    inner = json.loads(raw_inner)
                    if not isinstance(inner, dict):
                        raise ValueError("invalid_request")
                else:
                    inner = outer
                status, result = self.backend.route_control(inner, self.source_ip)
                if parsed.path.endswith("/executions"):
                    response_body = result if isinstance(result, str) else json_bytes(result).decode("utf-8")
                    self.send_json(201, {
                        "status": "completed",
                        "responseStatusCode": status,
                        "responseBody": response_body,
                    })
                elif isinstance(result, str):
                    self.send_bytes(status, result.encode("utf-8"), "text/plain; charset=utf-8")
                else:
                    self.send_json(status, result)
                return
            if parsed.path.startswith("/v1/admin/"):
                if not self.rate_limit("admin-write", 60, 60):
                    return
                if not self.admin_mutation():
                    return
                if parsed.path == "/v1/admin/logout":
                    self.read_body()
                    self.backend.revoke_admin_session(self.headers.get("Cookie"))
                    self.send_json(
                        200,
                        {"ok": True},
                        {"Set-Cookie": [
                            "sv_admin=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict",
                            "__Host-sv_admin=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict",
                        ]},
                    )
                    return
                if parsed.path == "/v1/admin/browser-tickets":
                    self.read_body()
                    self.send_json(201, {
                        "ok": True,
                        "ticket": self.backend.create_admin_ticket(self._admin_identity),
                    })
                    return
                if parsed.path == "/v1/admin/admins":
                    if self._admin_identity.get("access") != "owner":
                        self.send_json(403, {"ok": False, "error": "owner_required"})
                        return
                    body = self.read_json()
                    created = self.backend.create_admin(body.get("name"), self._admin_identity)
                    self.send_json(201, created)
                    return
                if parsed.path == "/v1/admin/licenses":
                    body = self.read_json()
                    created = self.backend.create_license(
                        str(body.get("role") or "user"),
                        str(body.get("note") or "Created by vps_admin.ps1"),
                        str(body["expiresAt"]) if body.get("expiresAt") else None,
                        str(body["licenseKey"]) if body.get("licenseKey") else None,
                        self._admin_identity,
                    )
                    self.send_json(201, created)
                    return
                if parsed.path == "/v1/admin/cleanup":
                    self.read_body()
                    self.send_json(200, self.backend.cleanup(self._admin_identity))
                    return
                match = re.fullmatch(r"/v1/admin/users/(hw_[0-9a-f]{32})", parsed.path)
                if match:
                    updated = self.backend.update_user(
                        match.group(1), self.read_json(), self._admin_identity
                    )
                    self.send_json(
                        200 if updated else 404,
                        {"ok": bool(updated), "user": updated, "error": None if updated else "not_found"},
                    )
                    return
                match = re.fullmatch(
                    r"/v1/admin/users/(hw_[0-9a-f]{32})/sessions/revoke", parsed.path
                )
                if match:
                    self.read_body()
                    removed = self.backend.revoke_user_sessions(
                        match.group(1), self._admin_identity
                    )
                    self.send_json(
                        200 if removed is not None else 404,
                        {
                            "ok": removed is not None,
                            "removed": removed or 0,
                            "error": None if removed is not None else "not_found",
                        },
                    )
                    return
                match = re.fullmatch(
                    r"/v1/admin/users/(hw_[0-9a-f]{32})/access/revoke", parsed.path
                )
                if match:
                    self.read_body()
                    revoked = self.backend.revoke_user_access(
                        match.group(1), self._admin_identity
                    )
                    self.send_json(
                        200 if revoked else 404,
                        {
                            "ok": revoked is not None,
                            "revoked": revoked,
                            "error": None if revoked else "not_found",
                        },
                    )
                    return
                match = re.fullmatch(r"/v1/admin/licenses/([^/]+)/revoke", parsed.path)
                if match:
                    self.read_body()
                    changed = self.backend.revoke_license(
                        unquote(match.group(1)), self._admin_identity
                    )
                    self.send_json(200 if changed else 404, {"ok": changed})
                    return
                match = re.fullmatch(r"/v1/admin/admins/(ad_[0-9a-f]{32})/revoke", parsed.path)
                if match:
                    if self._admin_identity.get("access") != "owner":
                        self.send_json(403, {"ok": False, "error": "owner_required"})
                        return
                    self.read_body()
                    changed = self.backend.revoke_admin(match.group(1), self._admin_identity)
                    self.send_json(200 if changed else 404, {"ok": changed})
                    return
            self.send_json(404, {"ok": False, "error": "not_found"})
        except OverflowError:
            self.send_json(413, {"ok": False, "error": "body_too_large"})
        except (ValueError, json.JSONDecodeError, sqlite3.IntegrityError) as error:
            self.send_json(400, {"ok": False, "error": str(error) or "invalid_request"})
        except Exception as error:  # keep internals out of the wire response
            print(f"request failed: {type(error).__name__}: {error}", flush=True)
            self.send_json(500, {"ok": False, "error": "internal"})

    def do_PUT(self) -> None:
        if not self.rate_limit("all", 1200, 60):
            return
        parsed = urlsplit(self.path)
        if self.feature_put(parsed):
            return
        mapping_match = re.fullmatch(
            r"/v1/mappings/cheatbreaker/([0-9a-f]{64})", parsed.path
        )
        if mapping_match:
            if not self.rate_limit("mapping-upload", 12, 300):
                return
            token = parse_qs(parsed.query).get("token", [""])[0]
            device = self.backend.mapping_device(token)
            if not device:
                self.send_json(401, {"ok": False, "error": "unauthorized"})
                return
            if self.headers.get("Content-Type", "").split(";", 1)[0].strip().lower() != "application/octet-stream":
                self.send_json(415, {"ok": False, "error": "binary_content_type_required"})
                return
            try:
                payload = self.read_body(MAX_MAPPING_PATCH_BYTES)
                result = self.backend.submit_mapping_patch(
                    mapping_match.group(1), payload, device, self.source_ip
                )
                self.send_json(200 if result["ready"] else 202, result)
            except OverflowError:
                self.send_json(413, {"ok": False, "error": "mapping_patch_too_large"})
            except ValueError as error:
                self.send_json(400, {"ok": False, "error": str(error)})
            return
        match = re.fullmatch(r"/v1/admin/payloads/(release|tester|developer)", parsed.path)
        if not match:
            self.send_json(404, {"ok": False, "error": "not_found"})
            return
        if not self.rate_limit("admin-write", 60, 60):
            return
        if not self.admin_mutation():
            return
        if self.headers.get("Content-Type", "").split(";", 1)[0].strip().lower() != "application/octet-stream":
            self.send_json(415, {"ok": False, "error": "binary_content_type_required"})
            return
        try:
            payload = self.read_body(MAX_PAYLOAD_BYTES)
            target = self.backend.payload_dir / f"{match.group(1)}.dll"
            temporary = target.with_suffix(".upload")
            range_header = self.headers.get("Content-Range", "")
            range_match = re.fullmatch(r"bytes (\d+)-(\d+)/(\d+)", range_header)
            if range_header and not range_match:
                self.send_json(400, {"ok": False, "error": "invalid_content_range"})
                return
            if range_match:
                start, end, total = map(int, range_match.groups())
                if total < 1024 or total > MAX_PAYLOAD_BYTES or end < start or end - start + 1 != len(payload):
                    self.send_json(400, {"ok": False, "error": "invalid_content_range"})
                    return
                with self.backend.lock:
                    if start == 0:
                        temporary.write_bytes(payload)
                    elif not temporary.is_file() or temporary.stat().st_size != start:
                        self.send_json(409, {"ok": False, "error": "upload_offset_mismatch"})
                        return
                    else:
                        with temporary.open("ab") as output:
                            output.write(payload)
                    if temporary.stat().st_size != end + 1:
                        self.send_json(409, {"ok": False, "error": "upload_size_mismatch"})
                        return
                    complete = end + 1 == total
                    if complete:
                        with temporary.open("rb") as uploaded:
                            if uploaded.read(2) != b"MZ":
                                temporary.unlink(missing_ok=True)
                                self.send_json(400, {"ok": False, "error": "invalid_pe_payload"})
                                return
                        os.chmod(temporary, 0o600)
                        temporary.replace(target)
                if not complete:
                    self.send_json(202, {"ok": True, "complete": False, "received": end + 1, "total": total})
                    return
                digest_value = hashlib.sha256(target.read_bytes()).hexdigest()
                with self.backend.lock, self.backend.connect() as connection:
                    self.backend.audit_event(
                        connection,
                        "admin.payload.updated",
                        self._admin_identity,
                        details={
                            "role": match.group(1),
                            "bytes": total,
                            "sha256": digest_value,
                        },
                        source_ip=self.source_ip,
                    )
                self.send_json(200, {"ok": True, "complete": True, "role": match.group(1), "bytes": total, "sha256": digest_value})
                return
            if len(payload) < 1024 or not payload.startswith(b"MZ"):
                self.send_json(400, {"ok": False, "error": "invalid_pe_payload"})
                return
            temporary.write_bytes(payload)
            os.chmod(temporary, 0o600)
            temporary.replace(target)
            digest_value = hashlib.sha256(payload).hexdigest()
            with self.backend.lock, self.backend.connect() as connection:
                self.backend.audit_event(
                    connection,
                    "admin.payload.updated",
                    self._admin_identity,
                    details={
                        "role": match.group(1),
                        "bytes": len(payload),
                        "sha256": digest_value,
                    },
                    source_ip=self.source_ip,
                )
            self.send_json(200, {"ok": True, "complete": True, "role": match.group(1), "bytes": len(payload), "sha256": digest_value})
        except OverflowError:
            self.send_json(413, {"ok": False, "error": "payload_too_large"})


class SonyVegasServer(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = True
    request_queue_size = 128

    def __init__(self, address: tuple[str, int], backend: SonyVegasBackend):
        self.rate_limiter = SlidingWindowRateLimiter()
        self.worker_slots = threading.BoundedSemaphore(64)
        super().__init__(address, SonyVegasHandler)
        self.backend = backend

    def process_request(self, request: object, client_address: object) -> None:
        if not self.worker_slots.acquire(blocking=False):
            self.shutdown_request(request)  # type: ignore[arg-type]
            return
        try:
            super().process_request(request, client_address)  # type: ignore[arg-type]
        except Exception:
            self.worker_slots.release()
            raise

    def process_request_thread(self, request: object, client_address: object) -> None:
        try:
            super().process_request_thread(request, client_address)  # type: ignore[arg-type]
        finally:
            self.worker_slots.release()


def main() -> None:
    data_dir = os.environ.get("SONYVEGAS_DATA_DIR", "/var/lib/sonyvegas")
    admin_token = os.environ.get("SONYVEGAS_ADMIN_TOKEN", "")
    if len(admin_token) < 32:
        raise SystemExit("SONYVEGAS_ADMIN_TOKEN must contain at least 32 characters")
    host = os.environ.get("SONYVEGAS_HOST", "0.0.0.0")
    port = int(os.environ.get("SONYVEGAS_PORT", "8080"))
    backend = SonyVegasBackend(data_dir, admin_token)
    server = SonyVegasServer((host, port), backend)
    print(f"SonyVegas control API listening on {host}:{port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
