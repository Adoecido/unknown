"""Keep only non-conflicting, currently-unmapped Matcher class seeds."""
import argparse
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--matches", type=Path, required=True)
    parser.add_argument("--seeds", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    forward, reverse = {}, {}
    for raw in args.matches.read_text(encoding="utf-8-sig").splitlines():
        row = raw.split("\t")
        if len(row) == 7 and row[0] == "CL":
            forward[row[1]], reverse[row[4]] = row[4], row[1]
    accepted, same, conflicts = [], 0, []
    for raw in args.seeds.read_text(encoding="utf-8-sig").splitlines():
        if not raw or raw.startswith("#"):
            continue
        row = raw.split("\t")
        if len(row) != 2:
            raise ValueError("Expected class seed: " + raw)
        source, target = row
        if forward.get(source) == target:
            same += 1
        elif source in forward or target in reverse:
            conflicts.append((source, forward.get(source, ""), target, reverse.get(target, "")))
        else:
            accepted.append((source, target))
    args.out.write_text("".join("%s\t%s\n" % row for row in sorted(set(accepted))), encoding="utf-8")
    print("New seeds:", len(set(accepted)), "already matched:", same, "conflicts quarantined:", len(conflicts))
    for row in conflicts[:20]:
        print("CONFLICT", *row, sep="\t")


if __name__ == "__main__":
    main()
