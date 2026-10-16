"""Produce mutually unique class seeds from restored literals, not old names."""
import argparse
import collections
import importlib.util
import json
import re
import zipfile
from pathlib import Path


def parser_from(path):
    spec = importlib.util.spec_from_file_location("cb_dump", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.parse_class


def read_jar(path, parse):
    with zipfile.ZipFile(path) as jar:
        return {info["name"]: info for name in jar.namelist() if name.endswith(".class")
                for info in [parse(jar.read(name))]}


def string_seeds(left, right):
    indexes = []
    for classes in (left, right):
        index = collections.defaultdict(set)
        for name, info in classes.items():
            # Generated global tables must never stand in for a game class.
            if len(info["fields"]) == 1 and len(info["methods"]) == 1:
                continue
            for value in info["strings"]:
                if len(value.strip()) >= 3:
                    index[value].add(name)
        indexes.append(index)
    a, b = indexes
    votes = collections.defaultdict(float)
    unique = collections.Counter()
    for value in a.keys() & b.keys():
        count = len(a[value]) * len(b[value])
        if count > 25:
            continue
        for na in a[value]:
            for nb in b[value]:
                votes[na, nb] += 1 / count
                if count == 1:
                    unique[na, nb] += 1
    ranks_a = collections.defaultdict(list)
    ranks_b = collections.defaultdict(list)
    for (na, nb), score in votes.items():
        ranks_a[na].append((score, nb))
        ranks_b[nb].append((score, na))
    seeds = {}
    for na, values in ranks_a.items():
        values.sort(reverse=True)
        score, nb = values[0]
        back = sorted(ranks_b[nb], reverse=True)
        if back[0][1] != na or unique[na, nb] < 1:
            continue
        margin_a = score - (values[1][0] if len(values) > 1 else 0)
        margin_b = score - (back[1][0] if len(back) > 1 else 0)
        # Require unique literals plus a clear, mutual lead.
        if min(margin_a, margin_b) < 0.75 or score < 1.5:
            continue
        seeds[na] = nb
    return seeds


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--vanilla", type=Path, required=True)
    parser.add_argument("--normalized", type=Path, required=True)
    parser.add_argument("--parser", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    parse = parser_from(args.parser)
    seeds = string_seeds(read_jar(args.vanilla, parse), read_jar(args.normalized, parse))
    args.out.write_text("".join(a + "\t" + b + "\n" for a, b in sorted(seeds.items())), encoding="utf-8")
    print("Restored-literal seeds:", len(seeds))


if __name__ == "__main__":
    main()
