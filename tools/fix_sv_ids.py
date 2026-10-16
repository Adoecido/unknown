#!/usr/bin/env python3
"""Recompute stale SV_STR/SV_WSTR identifiers after literal edits.

gen_obf.py derives each identifier from the literal itself, so editing a
wrapped literal in place leaves a stale id and the build fails. This
helper rewrites the identifiers to the expected values without touching
the literals. Run wrap_native_strings.py first for brand-new plaintext
literals; run this for literals edited inside existing SV_STR calls.
"""

from __future__ import annotations

import hashlib
import pathlib
import re
import sys

CALL = re.compile(
    r"SV_(W?STR)\(\s*(SV_[A-F0-9]{16})\s*,\s*(L?\"(?:\\.|[^\"\\])*\")\s*\)"
)


def expected_id(kind: str, literal_token: str) -> str:
    digest = hashlib.sha256(f"{kind}:{literal_token}".encode("utf-8")).hexdigest()
    return "SV_" + digest[:16].upper()


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: fix_sv_ids.py SOURCE...", file=sys.stderr)
        return 2
    failed = 0
    for name in sys.argv[1:]:
        path = pathlib.Path(name)
        text = path.read_text(encoding="utf-8")
        changed = 0

        def fix(match: re.Match[str]) -> str:
            nonlocal changed
            kind = "WSTR" if match.group(1) == "WSTR" else "STR"
            literal = match.group(3)
            wanted = expected_id(kind, literal)
            if match.group(2) != wanted:
                changed += 1
            return f"SV_{kind}({wanted}, {literal})"

        new_text = CALL.sub(fix, text)
        if new_text != text:
            path.write_text(new_text, encoding="utf-8", newline="\n")
        print(f"{name}: {changed} stale string ids fixed")
        failed += changed
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
