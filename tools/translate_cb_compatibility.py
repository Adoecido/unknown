#!/usr/bin/env python3
"""Carry explicitly reviewed compatibility exceptions to a new CB build."""

import argparse
import json
from pathlib import Path


def matcher_rows(path):
    for raw in path.read_text(encoding="utf-8-sig").splitlines():
        if not raw or raw.startswith("#"):
            continue
        row = tuple(raw.split("\t"))
        if len(row) != 7:
            raise ValueError("Invalid matcher row: " + raw)
        yield row


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--previous", type=Path, required=True)
    parser.add_argument("--cross", type=Path, required=True)
    parser.add_argument("--patch-sha256", required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    previous = json.loads(args.previous.read_text(encoding="utf-8"))
    cross = {
        (row[0], row[1], row[2], row[3]): row[4:]
        for row in matcher_rows(args.cross)
        if row[0] in ("FD", "MD")
    }
    members = []
    for item in previous.get("members", []):
        match = item["match"]
        translated = cross.get(tuple(match[0:1] + match[4:7]))
        if translated is None:
            raise RuntimeError(
                "Reviewed compatibility member no longer matched: %s" % match)
        copy = dict(item)
        copy["match"] = match[:4] + list(translated)
        members.append(copy)

    result = {
        "patch_sha256": args.patch_sha256.lower(),
        "members": members,
        "unavailable": previous.get("unavailable", []),
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print("Translated %d reviewed compatibility member(s)." % len(members))


if __name__ == "__main__":
    main()
