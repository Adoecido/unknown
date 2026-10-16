#!/usr/bin/env python3
"""Unit tests for the pure-stdlib RSA license signer (vps/license_crypto.py)."""
from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from license_crypto import (  # noqa: E402
    generate_keypair,
    sign,
    sign_license_payload,
    verify,
)


def main() -> None:
    keypair = generate_keypair()
    payload = {
        "v": 1,
        "product": "sonyvegas",
        "userId": 123456789,
        "role": "user",
        "iat": 1000000,
        "exp": 1003600,
    }
    payload["requireSignature"] = True
    doc = sign_license_payload(payload, keypair)

    # canonical round-trip re-encode must verify
    canonical = json.dumps(doc["payload"], sort_keys=True, separators=(",", ":")).encode("utf-8")
    import base64
    sig = base64.b64decode(doc["signature"])
    assert verify(canonical, sig, keypair), "valid signature rejected"

    # tampered payload must fail
    tampered = dict(payload)
    tampered["role"] = "developer"
    canonical_bad = json.dumps(tampered, sort_keys=True, separators=(",", ":")).encode("utf-8")
    assert not verify(canonical_bad, sig, keypair), "tampered payload accepted"

    # wrong key must fail
    other = generate_keypair()
    assert not verify(canonical, sig, other), "wrong key accepted"

    # direct bytes API also round-trips
    raw = sign(canonical, keypair)
    assert verify(canonical, raw, keypair), "raw sign/verify mismatch"

    print("PASS: license crypto round-trip, tamper rejection and wrong-key rejection")


if __name__ == "__main__":
    main()
