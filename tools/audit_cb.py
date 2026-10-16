#!/usr/bin/env python3
"""Audit CheatBreaker 1.8.9 mapping coverage for the SonyVegas runtime.

Walks every class and every (owner, member) that the SonyVegas mapping layer
resolves for Minecraft 1.8.9 and reports which of them have a CheatBreaker
allatori mapping in src/main/resources/mappings/cheatbreaker189.

Cross-references three sources:
  * the CB TSV tables (classes.tsv / fields.tsv / methods.tsv)
  * the authoritative MCP 1.8.9 SRG dump (mc_ref/srg/joined.srg)
  * the MCP 1.8.9 name tables (mc_ref/mcp/fields.csv, methods.csv)

Usage:
  py tools/audit_cb.py [--sonyvegas DIR] [--mc-ref DIR] [--only-critical]

Exit code is 0 always; use the printed summary. Run from the sonyvegas root.
"""
import argparse
import csv
import re
from collections import defaultdict
from pathlib import Path

DEFAULT_SONYVEGAS = Path(__file__).resolve().parents[1]
DEFAULT_MC_REF = Path(r"D:\sources\tampinhas\cb_mappings\mc_ref")

MCP_LITERAL = re.compile(r"^[a-z][a-zA-Z0-9_]*$")
SRG_FIELD = re.compile(r"^field_\d+_[a-z]$")
SRG_METHOD = re.compile(r"^func_\d+_[a-z]$")


def load_cb_tables(cb_dir):
    cls = {}
    fields_by_owner = defaultdict(set)
    methods_by_owner = defaultdict(set)
    cls_path = cb_dir / "classes.tsv"
    for raw in cls_path.read_text(encoding="utf-8-sig").splitlines():
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
            fields_by_owner[parts[0].split("/", 1)[0]].add(parts[1])
    for raw in (cb_dir / "methods.tsv").read_text(encoding="utf-8-sig").splitlines():
        if not raw or raw.startswith("#"):
            continue
        parts = raw.split("\t")
        if len(parts) >= 2 and "/" in parts[0]:
            methods_by_owner[parts[0].split("/", 1)[0]].add(parts[1])
    return cls, fields_by_owner, methods_by_owner


def load_mcp_names(mc_ref):
    srg_to_mcp = {}
    for name in ("fields.csv", "methods.csv"):
        path = mc_ref / "mcp" / name
        with path.open(encoding="utf-8-sig", newline="") as handle:
            for row in csv.reader(handle):
                if len(row) < 2:
                    continue
                srg, mcp = row[0], row[1]
                srg_to_mcp[srg] = mcp
    return srg_to_mcp


def load_srg(mc_ref, srg_to_mcp):
    mcp_to_notch = {}
    notch_to_mcp = {}
    srg_classes = set()
    path = mc_ref / "srg" / "joined.srg"
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if raw.startswith("CL: "):
            _, left, right = raw.split(None, 2)
            srg_classes.add(right)
            continue
        elif raw.startswith("FD: "):
            _, left, right = raw.split(None, 2)
            n_owner, n_member = left.rsplit("/", 1)
            s_owner, s_member = right.rsplit("/", 1)
        elif raw.startswith("MD: "):
            _, left, left_desc, right, right_desc = raw.split(None, 4)
            n_owner, n_member = left.rsplit("/", 1)
            s_owner, s_member = right.rsplit("/", 1)
        else:
            continue
        mcp_name = srg_to_mcp.get(s_member, s_member)
        mcp_to_notch[(s_owner, mcp_name)] = (n_owner, n_member)
        notch_to_mcp[(n_owner, n_member)] = (s_owner, mcp_name)
    return mcp_to_notch, notch_to_mcp, srg_classes


def collect_used_classes(mapped_classes_java):
    found = set()
    for line in mapped_classes_java.splitlines():
        match = re.search(r"= MappedClasses\.m\(\"([^\"]+)\"", line)
        if match:
            found.add(match.group(1))
    return found


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
            if MCP_LITERAL.match(name) or SRG_FIELD.match(name) or SRG_METHOD.match(name):
                names.append(name)
        modules.append((path.name, owners, names))
    return modules


def resolve_owner_to_allatori(owner_field, class_map, allatori):
    source = class_map.get(owner_field)
    if source is None:
        return None, None
    return source, allatori.get(source, allatori.get(source.replace(".", "/")))


def resolve_srg(member, srg_to_mcp):
    if SRG_FIELD.match(member) or SRG_METHOD.match(member):
        if member in srg_to_mcp:
            return {member}
        return set()
    return {k for k, v in srg_to_mcp.items() if v == member}


def is_189_anchored(member, owner_source, srg_to_mcp, mcp_to_notch):
    if SRG_FIELD.match(member) or SRG_METHOD.match(member):
        return member in srg_to_mcp
    if owner_source:
        return (owner_source.replace(".", "/"), member) in mcp_to_notch
    return False


def member_candidates(member, srg_to_mcp, mcp_to_notch, owner_source):
    candidates = {member}
    candidates.update(resolve_srg(member, srg_to_mcp))
    if owner_source:
        notch = mcp_to_notch.get((owner_source.replace(".", "/"), member))
        if notch:
            candidates.add(notch[1])
    return candidates


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sonyvegas", default=str(DEFAULT_SONYVEGAS))
    parser.add_argument("--mc-ref", default=str(DEFAULT_MC_REF))
    parser.add_argument("--only-critical", action="store_true")
    args = parser.parse_args()

    sony = Path(args.sonyvegas)
    cb_dir = sony / "src" / "main" / "resources" / "mappings" / "cheatbreaker189"
    mc_ref = Path(args.mc_ref)

    allatori, cb_fields, cb_methods = load_cb_tables(cb_dir)
    srg_to_mcp = load_mcp_names(mc_ref)
    mcp_to_notch, _notch_to_mcp, srg_classes = load_srg(mc_ref, srg_to_mcp)

    class_map = {}
    mapped_classes_path = sony / "src" / "main" / "java" / "vegas" / "sony" / "mapping" / "MappedClasses.java"
    for line in mapped_classes_path.read_text(encoding="utf-8").splitlines():
        match = re.search(r"([A-Za-z0-9_]+) = MappedClasses\.m\(\"([^\"]+)\"", line)
        if match:
            class_map[match.group(1)] = match.group(2)

    used_classes = collect_used_classes(mapped_classes_path.read_text(encoding="utf-8"))
    print("=" * 78)
    print("SECTION 1 - classes used by SonyVegas without a CB class mapping")
    print("=" * 78)
    missing_classes = []
    for source in sorted(used_classes):
        source = source.strip()
        slash = source.replace(".", "/")
        if slash not in srg_classes:
            continue
        if source in allatori or slash in allatori:
            continue
        missing_classes.append(source)
    for source in missing_classes:
        print(f"  MISSING {source}")
    print(f"  total 1.8.9 classes used: {len([c for c in used_classes if c.strip().replace('.', '/') in srg_classes])}  missing: {len(missing_classes)}")

    modules = collect_mapping_modules(
        sony / "src" / "main" / "java" / "vegas" / "sony" / "mapping" / "mappings")

    print()
    print("=" * 78)
    print("SECTION 2 - per mapping module, members without a CB field/method mapping")
    print("=" * 78)
    total_members = 0
    total_missing = 0
    for module_name, owners, names in modules:
        owner_reports = []
        for owner_field in owners or ["?"]:
            source, allatori_name = resolve_owner_to_allatori(owner_field, class_map, allatori)
            slash = source.replace(".", "/") if source else None
            if allatori_name is None and (slash is None or slash not in srg_classes):
                continue
            owner_reports.append((owner_field, source, allatori_name))
        if not owner_reports:
            continue
        if args.only_critical and not any(o[0] == "q0" for o in owner_reports):
            continue
        missing_here = []
        skipped = 0
        for member in dict.fromkeys(names):
            total_members += 1
            anchored = False
            for owner_field, source, allatori_name in owner_reports:
                if is_189_anchored(member, source, srg_to_mcp, mcp_to_notch):
                    anchored = True
                    break
            if not anchored:
                skipped += 1
                continue
            present = False
            for owner_field, source, allatori_name in owner_reports:
                if allatori_name is None:
                    continue
                for cand in member_candidates(member, srg_to_mcp, mcp_to_notch, source):
                    if cand in cb_fields.get(allatori_name, ()) or cand in cb_methods.get(allatori_name, ()):
                        present = True
                        break
                if present:
                    break
            if not present:
                missing_here.append(member)
                total_missing += 1
        if missing_here or not args.only_critical:
            label = ", ".join(
                f"{of}->{an or 'NO-CLASS-MAPPING'}" for of, _, an in owner_reports)
            print(f"\n[{module_name}] owner(s): {label}")
            if missing_here:
                for member in missing_here:
                    print(f"    MISSING {member}")
            else:
                print("    ok: all resolvable 1.8.9 members are mapped")
            if skipped:
                print(f"    (skipped {skipped} non-1.8.9/version-guarded members)")

    print()
    print("=" * 78)
    print(f"SUMMARY  members scanned: {total_members}  missing: {total_missing}")
    print("=" * 78)
    print("classes without CB mapping:", len(missing_classes))


if __name__ == "__main__":
    main()
