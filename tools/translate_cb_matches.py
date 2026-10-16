"""Carry reviewed vanilla matches across two structurally matched CB builds."""
import argparse
from pathlib import Path


def rows(path):
    result = []
    for raw in path.read_text(encoding="utf-8-sig").splitlines():
        if not raw or raw.startswith("#"):
            continue
        row = tuple(raw.split("\t"))
        if len(row) != 7 or row[0] not in ("CL", "FD", "MD"):
            raise ValueError("Invalid Matcher row: " + raw)
        result.append(row)
    return result


def source_key(row):
    return (row[0], row[1]) if row[0] == "CL" else row[:4]


def target_key(row):
    return (row[0], row[4]) if row[0] == "CL" else (row[0],) + row[4:]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--previous", type=Path, required=True)
    parser.add_argument("--cross", type=Path, required=True)
    parser.add_argument("--current", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    cross_classes, cross_members = {}, {}
    for row in rows(args.cross):
        if row[0] == "CL":
            cross_classes[row[1]] = row[4]
        else:
            cross_members[(row[0], row[1], row[2], row[3])] = row[4:]

    translated = []
    for row in rows(args.previous):
        new_owner = cross_classes.get(row[4])
        if not new_owner:
            continue
        if row[0] == "CL":
            translated.append(("CL", row[1], "", "", new_owner, "", ""))
            continue
        member = cross_members.get((row[0], row[4], row[5], row[6]))
        if member and member[0] == new_owner:
            translated.append(row[:4] + member)

    accepted = list(rows(args.current))
    by_source = {source_key(row): target_key(row) for row in accepted}
    by_target = {target_key(row): source_key(row) for row in accepted}
    added = same = conflicts = owner_conflicts = 0
    for row in translated:
        skey, tkey = source_key(row), target_key(row)
        if by_source.get(skey) == tkey:
            same += 1
            continue
        if skey in by_source or tkey in by_target:
            conflicts += 1
            continue
        if row[0] != "CL":
            owner = by_source.get(("CL", row[1]))
            if owner != ("CL", row[4]):
                owner_conflicts += 1
                continue
        accepted.append(row)
        by_source[skey], by_target[tkey] = tkey, skey
        added += 1

    accepted.sort(key=lambda row: (row[1], row[0], row[2], row[3], row[4], row[5], row[6]))
    header = "# kind\tnotch owner\tnotch member\tnotch descriptor\truntime owner\truntime member\truntime descriptor\n"
    args.out.write_text(header + "".join("\t".join(row) + "\n" for row in accepted), encoding="utf-8")
    print("Cross-build candidates:", len(translated), "added:", added,
          "already present:", same, "conflicts quarantined:", conflicts,
          "owner mismatches quarantined:", owner_conflicts)


if __name__ == "__main__":
    main()
