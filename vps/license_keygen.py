"""Generate the VPS license keypair.

Usage (admin machine only):
    python vps/license_keygen.py

Writes:
  vps/license_private.json   (gitignored; copy this to the server ONLY)
  vps/license_public.json    (reference; the Java client embeds n/e constants)
Prints the Java constant to paste into LicenseVerifier.
"""
from __future__ import annotations

import json
from pathlib import Path

from license_crypto import generate_keypair

HERE = Path(__file__).resolve().parent


def main() -> None:
    pair = generate_keypair()
    private_path = HERE / "license_private.json"
    public_path = HERE / "license_public.json"
    private_path.write_text(json.dumps(pair, indent=2), encoding="utf-8")
    public_path.write_text(json.dumps({"kty": "RSA", "n": pair["n"], "e": pair["e"]}, indent=2),
                           encoding="utf-8")
    print("wrote", private_path)
    print("wrote", public_path)
    print()
    print('Java constant for src/main/java/vegas/sony/api/LicenseVerifier.java:')
    print(f'    private static final String PUBLIC_KEY_MODULUS_HEX = "{pair["n"]}";')
    print(f'    private static final int PUBLIC_KEY_EXPONENT = {pair["e"]};')


if __name__ == "__main__":
    main()
