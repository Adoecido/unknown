"""Convert reviewed MCP class lines from mappings.exemplo.txt to Matcher notch seeds."""
import argparse
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--example", type=Path, required=True)
    parser.add_argument("--joined-srg", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--additional", type=Path)
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args()

    source_to_notch = {}
    for raw in args.joined_srg.read_text(encoding="utf-8").splitlines():
        columns = raw.split()
        if len(columns) == 3 and columns[0] == "CL:":
            source_to_notch[columns[2]] = columns[1]

    seeds = {}
    for number, raw in enumerate(args.example.read_text(encoding="utf-8-sig").splitlines(), 1):
        columns = raw.split()
        if not columns or columns[0].startswith("#") or columns[0] != "C":
            continue
        if len(columns) != 3:
            raise ValueError("Invalid class line %d: %s" % (number, raw))
        source, runtime = columns[1:]
        notch = source_to_notch.get(source)
        if notch is None:
            raise ValueError("Class absent from joined.srg: " + source)
        if notch in seeds and seeds[notch] != runtime:
            raise ValueError("Conflicting seed: " + source)
        seeds[notch] = runtime

    args.out.parent.mkdir(parents=True, exist_ok=True)
    if args.additional:
        for raw in args.additional.read_text(encoding="utf-8-sig").splitlines():
            if not raw or raw.startswith("#"):
                continue
            source, runtime = raw.lstrip("!").split("\t")
            if source in seeds and seeds[source] != runtime:
                raise ValueError("Conflicting additional seed: " + source)
            seeds[source] = runtime
    prefix = "!" if args.force else ""
    args.out.write_text("".join(prefix + "%s\t%s\n" % row for row in sorted(seeds.items())), encoding="utf-8")
    print("Imported reviewed class seeds:", len(seeds))


if __name__ == "__main__":
    main()
