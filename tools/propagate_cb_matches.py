"""Derive class candidates from already-matched member type signatures."""
import argparse
import collections
import re
from pathlib import Path
from seed_cb_matches import parser_from, read_jar


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--matches", type=Path, required=True)
    parser.add_argument("--vanilla", type=Path, required=True)
    parser.add_argument("--normalized", type=Path, required=True)
    parser.add_argument("--parser", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    parse = parser_from(args.parser)
    a = read_jar(args.vanilla, parse)
    b = read_jar(args.normalized, parse)
    rows = [line.split("\t") for line in args.matches.read_text().splitlines()
            if line and not line.startswith("#")]
    cm = {row[1]: row[4] for row in rows if row[0] == "CL"}
    bm = {v: k for k, v in cm.items()}
    votes = collections.defaultdict(set)
    conflicts = []

    def propose(x, y, reason):
        if not x or not y or x not in a or y not in b:
            return
        if x in cm:
            if cm[x] != y: conflicts.append((x, cm[x], y, reason))
        elif y not in bm:
            votes[x, y].add(reason)

    for row in rows:
        kind, owner, name, desc, runtime_owner, runtime_name, runtime_desc = row
        if kind == "CL":
            propose(a[owner]["super"], b[runtime_owner]["super"], "super:" + owner)
        else:
            left = re.findall(r"L([^;]+);", desc)
            right = re.findall(r"L([^;]+);", runtime_desc)
            if re.sub(r"L[^;]+;", "L@;", desc) != re.sub(r"L[^;]+;", "L@;", runtime_desc):
                continue
            for x, y in zip(left, right):
                propose(x, y, kind + ":" + owner + "/" + name + desc)
    by_a = collections.defaultdict(list)
    by_b = collections.defaultdict(list)
    for (x, y), reasons in votes.items():
        by_a[x].append((len(reasons), y))
        by_b[y].append((len(reasons), x))
    seeds = {}
    for x, rows in by_a.items():
        rows.sort(reverse=True)
        count, y = rows[0]
        other = sorted(by_b[y], reverse=True)
        if other[0][1] != x or len(rows) > 1 or len(other) > 1:
            continue
        if count >= 2:
            seeds[x] = y
    args.out.write_text("".join(x + "\t" + y + "\n" for x, y in sorted(seeds.items())), encoding="utf-8")
    print("Unanimous descriptor/hierarchy seeds:", len(seeds))
    print("Conflicting already-matched type evidence:", len(conflicts))
    for row in conflicts[:30]: print(row)


if __name__ == "__main__":
    main()
