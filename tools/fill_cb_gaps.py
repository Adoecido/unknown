#!/usr/bin/env python3
"""Fill CheatBreaker 1.8.9 member mapping gaps that SonyVegas actually uses.

Detects members of the SonyVegas mapping layer (mapping/mappings/*.java) that
resolve to a real 1.8.9 member (SRG/notch via mc_ref) but are missing from the
CB tables, then derives the missing Allatori names from the Crimson dump
(cb_mappings/out/classes.json) by translating the member descriptor through the
CB class table and matching it on the Allatori owner class.

Fields are disambiguated by type + elimination (all but one already-mapped).

Usage:
  py tools/fill_cb_gaps.py [--sonyvegas DIR] [--mc-ref DIR] [--classes-json FILE]
                           [--write]
"""
import argparse
import csv
import json
import re
from pathlib import Path

DEFAULT_SONYVEGAS = Path(__file__).resolve().parents[1]
DEFAULT_MC_REF = Path(r"D:\sources\tampinhas\cb_mappings\mc_ref")
DEFAULT_JSON = Path(r"D:\sources\tampinhas\cb_mappings\out\classes.json")

SRG_FIELD = re.compile(r"^field_\d+_[a-z]$")
SRG_METHOD = re.compile(r"^func_\d+_[a-z]$")

# Notch-typed fields whose type is not recoverable from joined.srg FD lines.
KNOWN_FIELD_TYPES = {
    "net/minecraft/client/renderer/EntityRenderer": {
        "field_78530_s": "F",
    },
    "net/minecraft/client/settings/GameSettings": {
        "field_152395_am": "Lnet/minecraft/client/settings/KeyBinding;",
    },
}


def load_cb_tables(cb_dir):
    cls = {}
    field_rows = []
    method_rows = []
    for raw in (cb_dir / "classes.tsv").read_text(encoding="utf-8-sig").splitlines():
        if not raw or raw.startswith("#"):
            continue
        parts = raw.split("\t")
        if len(parts) < 2:
            continue
        source, allatori = parts[0], parts[1]
        cls[source] = allatori
        cls[source.replace("/", ".")] = allatori
        cls[source.replace(".", "/")] = allatori
    for raw in (cb_dir / "fields.tsv").read_text(encoding="utf-8-sig").splitlines():
        if not raw or raw.startswith("#"):
            continue
        parts = raw.split("\t")
        if len(parts) >= 2 and "/" in parts[0]:
            field_rows.append(parts)
    for raw in (cb_dir / "methods.tsv").read_text(encoding="utf-8-sig").splitlines():
        if not raw or raw.startswith("#"):
            continue
        parts = raw.split("\t")
        if len(parts) >= 2 and "/" in parts[0]:
            method_rows.append(parts)
    return cls, field_rows, method_rows


def load_mcp_names(mc_ref):
    srg_to_mcp = {}
    for name in ("fields.csv", "methods.csv"):
        with (mc_ref / "mcp" / name).open(encoding="utf-8-sig", newline="") as handle:
            for row in csv.reader(handle):
                if len(row) >= 2:
                    srg_to_mcp[row[0]] = row[1]
    return srg_to_mcp


def load_srg(mc_ref):
    fields = {}
    methods = {}
    for raw in (mc_ref / "srg" / "joined.srg").read_text(
            encoding="utf-8", errors="replace").splitlines():
        if raw.startswith("FD: "):
            _, left, right = raw.split(None, 2)
            n_owner, n_member = left.rsplit("/", 1)
            s_owner, s_member = right.rsplit("/", 1)
            fields[(s_owner, s_member)] = (n_owner, n_member)
        elif raw.startswith("MD: "):
            _, left, left_desc, right, right_desc = raw.split(None, 4)
            n_owner, n_member = left.rsplit("/", 1)
            s_owner, s_member = right.rsplit("/", 1)
            methods[(s_owner, s_member)] = (n_owner, n_member, left_desc)
    return fields, methods


def resolve_srg(member, srg_to_mcp):
    if SRG_FIELD.match(member) or SRG_METHOD.match(member):
        if member in srg_to_mcp:
            return {member}
        return set()
    return {k for k, v in srg_to_mcp.items() if v == member}


def translate_desc(desc, allatori):
    out = []
    i = 0
    while i < len(desc):
        ch = desc[i]
        if ch == "L":
            end = desc.index(";", i)
            name = desc[i + 1:end]
            mapped = allatori.get(name, allatori.get(name.replace("/", ".")))
            out.append("L" + (mapped or name) + ";")
            i = end + 1
        else:
            out.append(ch)
            i += 1
    return "".join(out)


def collect_mapping_modules(mappings_dir):
    modules = []
    for path in sorted(mappings_dir.glob("*.java")):
        text = path.read_text(encoding="utf-8")
        owners = []
        for line in text.splitlines():
            match = re.search(r"super\(MappedClasses\.([A-Za-z0-9_]+)\);", line)
            if match:
                owners.append(match.group(1))
        names = []
        for match in re.finditer(r"\"([^\"]+)\"", text):
            name = match.group(1)
            if re.match(r"^[a-z][a-zA-Z0-9_]*$", name) or SRG_FIELD.match(name) or SRG_METHOD.match(name):
                names.append(name)
        modules.append((path.name, owners, names))
    return modules


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sonyvegas", default=str(DEFAULT_SONYVEGAS))
    parser.add_argument("--mc-ref", default=str(DEFAULT_MC_REF))
    parser.add_argument("--classes-json", default=str(DEFAULT_JSON))
    parser.add_argument("--write", action="store_true",
                        help="append rows to the CB tables")
    args = parser.parse_args()

    sony = Path(args.sonyvegas)
    cb_dir = sony / "src" / "main" / "resources" / "mappings" / "cheatbreaker189"
    mc_ref = Path(args.mc_ref)

    allatori, field_rows, method_rows = load_cb_tables(cb_dir)
    srg_to_mcp = load_mcp_names(mc_ref)
    fields_srg, methods_srg = load_srg(mc_ref)
    dump = {e["name"]: e for e in json.load(open(args.classes_json, encoding="utf-8"))}

    used_field_names = {r[0].split("/", 1)[0] for r in field_rows}
    used_method_names = {r[0].split("/", 1)[0] for r in method_rows}
    cb_fields_owner = {}
    cb_methods_owner = {}
    for parts in field_rows:
        owner, name = parts[0].split("/", 1)
        cb_fields_owner.setdefault(owner, set()).add(name)
    for parts in method_rows:
        owner, rest = parts[0].split("/", 1)
        cb_methods_owner.setdefault(owner, set()).add(rest.split(" ", 1)[0])

    class_map = {}
    mc_path = sony / "src" / "main" / "java" / "vegas" / "sony" / "mapping" / "MappedClasses.java"
    for line in mc_path.read_text(encoding="utf-8").splitlines():
        match = re.search(r"([A-Za-z0-9_]+) = MappedClasses\.m\(\"([^\"]+)\"", line)
        if match:
            class_map[match.group(1)] = match.group(2)

    rows_field = []
    rows_method = []
    for module_name, owners, names in collect_mapping_modules(
            sony / "src" / "main" / "java" / "vegas" / "sony" / "mapping" / "mappings"):
        owner_allatori = None
        owner_source = None
        for owner_field in owners or ["?"]:
            source = class_map.get(owner_field)
            if not source:
                continue
            mapped = allatori.get(source, allatori.get(source.replace(".", "/")))
            if mapped:
                owner_allatori, owner_source = mapped, source
                break
        if owner_allatori is None:
            continue
        for member in dict.fromkeys(names):
            for srg_name in resolve_srg(member, srg_to_mcp):
                if srg_name.startswith("field_") and (owner_source, srg_name) in fields_srg:
                    (n_owner, n_member) = fields_srg[(owner_source, srg_name)]
                    if allatori.get(n_owner) != owner_allatori:
                        continue
                    expected_type = KNOWN_FIELD_TYPES.get(
                        owner_source, {}).get(srg_name)
                    if not expected_type:
                        continue
                    expected = translate_desc(expected_type, allatori)
                    unmapped = []
                    for f in dump.get(owner_allatori, {}).get("fields", []):
                        if f["desc"] != expected:
                            continue
                        if f["name"] in cb_fields_owner.get(owner_allatori, set()):
                            continue
                        unmapped.append(f["name"])
                    if len(unmapped) == 1:
                        name = unmapped[0]
                        row = f"{owner_allatori}/{name}\t{srg_name}\t{n_member}\t{expected}"
                        print(f"[{module_name}] FIELD {srg_name} -> {row}")
                        rows_field.append(row)
                    else:
                        print(f"[{module_name}] FIELD {srg_name}: ambiguous/unfillable "
                              f"({len(unmapped)} unmapped {expected} candidates)")
                elif srg_name.startswith("func_") and (owner_source, srg_name) in methods_srg:
                    (n_owner, n_member, notch_desc) = methods_srg[(owner_source, srg_name)]
                    if allatori.get(n_owner) != owner_allatori:
                        continue
                    translated = translate_desc(notch_desc, allatori)
                    candidates = []
                    for m in dump.get(owner_allatori, {}).get("methods", []):
                        if m["desc"] == translated and m["name"] not in cb_methods_owner.get(
                                owner_allatori, set()):
                            candidates.append(m["name"])
                    if len(candidates) == 1:
                        name = candidates[0]
                        row = f"{owner_allatori}/{name} {translated}\t{srg_name}\t{n_member}"
                        print(f"[{module_name}] METHOD {srg_name} -> {row}")
                        rows_method.append(row)
                    else:
                        print(f"[{module_name}] METHOD {srg_name}: ambiguous/unfillable "
                              f"({len(candidates)} candidates)")
                break

    if args.write:
        with (cb_dir / "fields.tsv").open("a", encoding="utf-8") as handle:
            for row in rows_field:
                handle.write(row + "\n")
        with (cb_dir / "methods.tsv").open("a", encoding="utf-8") as handle:
            for row in rows_method:
                handle.write(row + "\n")
        print(f"appended {len(rows_field)} field row(s) and {len(rows_method)} method row(s)")


if __name__ == "__main__":
    main()
