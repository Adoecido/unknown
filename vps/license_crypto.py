"""Pure-stdlib RSA-PKCS#1 v1.5 (SHA-256) license signer for the VPS.

The private key never enters git: it lives only on the server in
vps/license_private.json (gitignored). See vps/license_keygen.py to generate
a pair and print the Java-ready public constants for LicenseVerifier.

Key JSON format (both files): {"kty":"RSA","n":"<hex>","e":"65537","d":"<hex>"}
(public file omits "d").
"""
from __future__ import annotations

import base64
import hashlib
import json
import secrets
from pathlib import Path

DIGESTINFO_PREFIX = bytes.fromhex("3031300d060960864801650304020105000420")


def _mod_pow_int(x: int, e: int, n: int) -> int:
    return pow(x, e, n)


def _emsa_pkcs1v15_encode(digest: bytes, key_bytes: int) -> int:
    if key_bytes < len(digest) + len(DIGESTINFO_PREFIX) + 11:
        raise ValueError("key too small for digest")
    padding = b"\xff" * (key_bytes - len(digest) - len(DIGESTINFO_PREFIX) - 3)
    return int.from_bytes(b"\x00\x01" + padding + b"\x00" + DIGESTINFO_PREFIX + digest, "big")


def sign(payload: bytes, private: dict) -> bytes:
    n, d = int(private["n"], 16), int(private["d"], 16)
    key_bytes = (n.bit_length() + 7) // 8
    em = _emsa_pkcs1v15_encode(hashlib.sha256(payload).digest(), key_bytes)
    return pow(em, d, n).to_bytes(key_bytes, "big")


def verify(payload: bytes, signature: bytes, public: dict) -> bool:
    try:
        # note: "e" is stored as a decimal string, "n" as hex
        n, e = int(public["n"], 16), int(public["e"])
        key_bytes = (n.bit_length() + 7) // 8
        if len(signature) != key_bytes:
            return False
        em = pow(int.from_bytes(signature, "big"), e, n)
        expected = _emsa_pkcs1v15_encode(hashlib.sha256(payload).digest(), key_bytes)
        return secrets.compare_digest(em.to_bytes(key_bytes, "big"),
                                      expected.to_bytes(key_bytes, "big"))
    except (KeyError, ValueError, OverflowError):
        return False


def generate_keypair(bits: int = 2048) -> dict:
    """DEV/ADMIN helper: small-e RSA keygen with Miller-Rabin primality."""
    def is_probable_prime(candidate: int, rounds: int = 16) -> bool:
        if candidate < 4:
            return False
        small_primes = (3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37)
        for p in small_primes:
            if candidate % p == 0:
                return candidate == p
        d = candidate - 1
        r = 0
        while d % 2 == 0:
            d //= 2
            r += 1
        for _ in range(rounds):
            a = secrets.randbelow(candidate - 3) + 2
            x = pow(a, d, candidate)
            if x == 1 or x == candidate - 1:
                continue
            for _ in range(r - 1):
                x = (x * x) % candidate
                if x == candidate - 1:
                    break
            else:
                return False
        return True

    def random_prime(bits_: int) -> int:
        while True:
            candidate = secrets.randbits(bits_) | (1 << (bits_ - 1)) | 1
            if is_probable_prime(candidate):
                return candidate

    while True:
        p = random_prime(bits // 2)
        q = random_prime(bits // 2)
        if p == q:
            continue
        n = p * q
        phi = (p - 1) * (q - 1)
        e = 65537
        if phi % e == 0:
            continue
        d = pow(e, -1, phi)
        return {
            "kty": "RSA",
            "n": format(n, "x"),
            "e": str(e),
            "d": format(d, "x"),
        }


def load_key(path: Path) -> dict | None:
    if not path.is_file():
        return None
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return None
    if not isinstance(data, dict) or "n" not in data:
        return None
    return data


def sign_license_payload(payload: dict, private_key: dict) -> dict:
    """Canonical JSON bytes signed with RSA/SHA-256; result nests into data.license."""
    canonical = json.dumps(payload, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return {
        "payload": payload,
        "signature": base64.b64encode(sign(canonical, private_key)).decode("ascii"),
    }
