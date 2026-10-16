#!/usr/bin/env python3
"""Import reviewed bytecode matches after validating against the original patch.

Dry-run unless --write is supplied. See cheatbreaker-mappings.md. Declaration
order is never used to guess members; incompatible signatures are quarantined.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import json
import re
from pathlib import Path
from generate_cb_member_forward import load_mcp_names
from seed_cb_matches import parser_from, read_jar

ROOT = Path(__file__).resolve().parents[1]
CB_ROOT = ROOT.parent / "cb_mappings"
MAP_DIR = ROOT / "src/main/resources/mappings/cheatbreaker189"
OBJECT_TYPE = re.compile(r"L([^;]+);")
ANCHORS = {
    "minecraftClass": "net/minecraft/client/Minecraft",
    "entityClass": "net/minecraft/entity/Entity",
    "entityLivingBaseClass": "net/minecraft/entity/EntityLivingBase",
    "entityPlayerClass": "net/minecraft/entity/player/EntityPlayer",
    "abstractClientPlayerClass": "net/minecraft/client/entity/AbstractClientPlayer",
    "entityPlayerSPClass": "net/minecraft/client/entity/EntityPlayerSP",
    "entityOtherPlayerMPClass": "net/minecraft/client/entity/EntityOtherPlayerMP",
    "worldClass": "net/minecraft/world/World",
    "worldClientClass": "net/minecraft/client/multiplayer/WorldClient",
    "enumFacingClass": "net/minecraft/util/EnumFacing",
}


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_srg(path):
    classes, fields, methods = {}, {}, {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        p = raw.split()
        if not p:
            continue
        if p[0] == "CL:":
            classes[p[1]] = p[2]
        elif p[0] == "FD:":
            owner, name = p[1].rsplit("/", 1)
            fields[owner, name] = p[2].rsplit("/", 1)[1]
        elif p[0] == "MD:":
            owner, name = p[1].rsplit("/", 1)
            methods[owner, name, p[2]] = p[3].rsplit("/", 1)[1]
    return classes, fields, methods


def remap_descriptor(desc, classes):
    return OBJECT_TYPE.sub(lambda m: "L" + classes.get(m[1], m[1]) + ";", desc)


def read_matches(path):
    classes, members, reverse = {}, [], {}
    for number, raw in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        if not raw or raw.startswith("#"):
            continue
        row = raw.split("\t")
        if len(row) != 7 or row[0] not in ("CL", "FD", "MD"):
            raise ValueError("Invalid match at %s:%d" % (path, number))
        kind, owner, name, desc, target, member, target_desc = row
        if kind == "CL":
            if owner in classes and classes[owner] != target:
                raise ValueError("Conflicting class " + owner)
            if target in reverse and reverse[target] != owner:
                raise ValueError("Multiple source classes mapped to " + target)
            classes[owner], reverse[target] = target, owner
        else:
            members.append(tuple(row))
    return classes, sorted(set(members))


def validate(classes, members, vanilla, runtime, adapted=()):
    """Fail on stale symbols; quarantine changed/unresolved signatures."""
    for owner, target in classes.items():
        if owner not in vanilla or target not in runtime:
            raise ValueError("Class absent from archive: %s -> %s" % (owner, target))
    indexes = [{(kind, owner, item["name"], item["desc"]): item
                for owner, info in archive.items()
                for kind, key in (("FD", "fields"), ("MD", "methods"))
                for item in info[key]} for archive in (vanilla, runtime)]
    accepted, rejected, seen_source, seen_target = [], [], {}, {}
    for row in members:
        kind, owner, name, desc, target, member, target_desc = row
        a, b = (kind, owner, name, desc), (kind, target, member, target_desc)
        if classes.get(owner) != target or a not in indexes[0] or b not in indexes[1]:
            raise ValueError("Member absent or wrong owner: " + repr(row))
        if a in seen_source and seen_source[a] != b or b in seen_target and seen_target[b] != a:
            raise ValueError("Conflicting member: " + repr(row))
        seen_source[a], seen_target[b] = b, a
        if remap_descriptor(desc, classes) != target_desc and row not in adapted:
            rejected.append({"match": row, "reason": "descriptor changed or referenced class unmatched"})
        elif bool(indexes[0][a]["access"] & 8) != bool(indexes[1][b]["access"] & 8):
            rejected.append({"match": row, "reason": "static/instance mismatch"})
        else:
            accepted.append(row)
    return accepted, rejected


def used_classes(path):
    if path.is_file():
        return {name.replace(".", "/") for name in re.findall(
            r'MappedClasses\.m\("([^"]+)"', path.read_text(encoding="utf-8"))}
    manifest = ROOT / "tools/cb189_required_classes.txt"
    if manifest.is_file():
        return {
            line.strip().replace(".", "/")
            for line in manifest.read_text(encoding="utf-8-sig").splitlines()
            if line.strip() and not line.startswith("#")
        }
    raise FileNotFoundError(
        "MappedClasses.java and cb189_required_classes.txt are unavailable"
    )


def table(header, rows):
    return "# " + header + "\n" + "".join("\t".join(row) + "\n" for row in sorted(set(rows)))


def render(classes, members, source_classes, srg_fields, srg_methods, mcp_fields, mcp_methods, runtime):
    class_rows, field_rows, method_rows, field_fwd, method_fwd = [], [], [], [], []
    source_targets, resolved = {}, []
    for owner, target in sorted(classes.items()):
        source = source_classes.get(owner)
        if source is not None:
            source_targets[source] = target
            class_rows.extend((alias, target) for alias in {owner, source, source.replace("/", ".")})
    for kind, owner, name, desc, target, member, target_desc in members:
        if owner not in source_classes:
            continue
        if kind == "FD":
            srg = srg_fields.get((owner, name))
            mcp = mcp_fields.get(srg, srg)
        else:
            srg = srg_methods.get((owner, name, desc))
            if srg is None and (name.startswith("<") or len(name) > 2):
                srg = name
            mcp = mcp_methods.get(srg, srg)
        if srg is None:
            continue
        resolved.append({"kind": kind, "source_owner": source_classes[owner],
                         "notch_owner": owner, "notch_name": name, "notch_desc": desc,
                         "srg": srg, "mcp": mcp, "runtime_owner": target,
                         "runtime_name": member, "runtime_desc": target_desc})
        if kind == "FD":
            field_rows.append((target + "/" + member, srg, name, target_desc))
            field_fwd.extend((target, key, member) for key in {srg, mcp})
        else:
            method_rows.append((target + "/" + member + " " + target_desc, srg, name))
            method_fwd.extend((target, key, member, target_desc) for key in {srg, mcp})
    anchors = {key: source_targets[source] for key, source in ANCHORS.items()}
    mc = anchors["minecraftClass"]
    for key, srg, kind, descriptor in (
            ("minecraftGetter", "func_71410_x", "MD", "()L" + mc + ";"),
            ("minecraftInstanceField", "field_71432_P", "FD", "L" + mc + ";")):
        candidates = [r for r in resolved if r["runtime_owner"] == mc and r["kind"] == kind
                      and r["srg"] == srg and r["runtime_desc"] == descriptor]
        if len(candidates) != 1:
            raise ValueError("Missing/ambiguous startup anchor " + key)
        name = candidates[0]["runtime_name"]
        key_list = "methods" if kind == "MD" else "fields"
        if not any(x["name"] == name and x["desc"] == descriptor and x["access"] & 8
                   for x in runtime[mc][key_list]):
            raise ValueError("Startup anchor must be static: " + key)
        anchors[key] = name
    outputs = {
        "classes.tsv": table("sourceInternal<TAB>allatoriInternal", class_rows),
        "fields.tsv": table("allatoriOwner/allatoriName<TAB>srgOrNotch<TAB>notchName<TAB>allatoriDesc", field_rows),
        "methods.tsv": table("allatoriOwner/allatoriName allatoriDesc<TAB>srgOrNotch<TAB>notchName", method_rows),
        "fields_fwd.tsv": table("allatoriOwnerInternal<TAB>sourceName<TAB>allatoriMemberName", field_fwd),
        "methods_fwd.tsv": table("allatoriOwnerInternal<TAB>sourceName<TAB>allatoriMemberName<TAB>allatoriDescriptor", method_fwd),
        "anchors.properties": "".join(k + "=" + v + "\n" for k, v in sorted(anchors.items())),
    }
    field_keys = collections.defaultdict(set)
    for key, srg, _, _ in field_rows:
        field_keys[key].add(srg)
    if any(len(values) > 1 for values in field_keys.values()):
        raise ValueError("Ambiguous owner/name in reverse field table")
    return outputs, source_targets, resolved


def save_files(directory, outputs, backup_dir):
    """Save deterministic generated data, preserving rollback copies outside resources."""
    directory.mkdir(parents=True, exist_ok=True)
    for name, content in sorted(outputs.items()):
        path = directory / name
        data = content.encode("utf-8")
        if path.exists():
            old = path.read_bytes()
            if old == data:
                continue
            backup = backup_dir / hashlib.sha256(old).hexdigest()[:16] / name
            backup.parent.mkdir(parents=True, exist_ok=True)
            if not backup.exists():
                backup.write_bytes(old)
        path.write_bytes(data)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--matches", type=Path, required=True)
    parser.add_argument("--vanilla", type=Path, required=True)
    parser.add_argument("--cb-jar", type=Path, required=True)
    parser.add_argument("--cb-root", type=Path, default=CB_ROOT)
    parser.add_argument("--mc-ref", type=Path)
    parser.add_argument("--map-dir", type=Path, default=MAP_DIR)
    parser.add_argument("--dump-dir", type=Path)
    parser.add_argument("--report", type=Path)
    parser.add_argument("--expected-sha256", help="Reject a patch different from the reviewed version")
    parser.add_argument("--compatibility", type=Path, default=ROOT / "tools/cb189_compatibility.json")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true", help="Fail if committed runtime tables differ from the validated inputs")
    args = parser.parse_args()
    if args.write and args.check:
        parser.error("--write and --check are mutually exclusive")
    patch_hash = sha256(args.cb_jar)
    if args.expected_sha256 and patch_hash != args.expected_sha256.lower():
        raise ValueError("Patch SHA-256 differs from the reviewed patch")
    parse = parser_from(args.cb_root / "dump_cb_mappings.py")
    vanilla, runtime = read_jar(args.vanilla, parse), read_jar(args.cb_jar, parse)
    mc_ref = args.mc_ref or args.cb_root / "mc_ref"
    source_classes, srg_fields, srg_methods = load_srg(mc_ref / "srg/joined.srg")
    classes, members = read_matches(args.matches)
    compatibility = json.loads(args.compatibility.read_text(encoding="utf-8"))
    if compatibility["patch_sha256"] != patch_hash:
        raise ValueError("Compatibility notes belong to another patch; review them before updating")
    adapted = {tuple(entry["match"]) for entry in compatibility["members"]}
    members = sorted(set(members) | adapted)
    accepted, rejected = validate(classes, members, vanilla, runtime, adapted)
    outputs, source_targets, resolved = render(classes, accepted, source_classes, srg_fields, srg_methods,
        load_mcp_names(mc_ref / "mcp/fields.csv"), load_mcp_names(mc_ref / "mcp/methods.csv"), runtime)
    required = used_classes(ROOT / "src/main/java/vegas/sony/mapping/MappedClasses.java") & set(source_classes.values())
    missing_used = sorted(required - source_targets.keys())
    missing_classes = sorted(source for notch, source in source_classes.items()
                             if notch in vanilla and notch not in classes)
    matched_keys = {(r["kind"], r["notch_owner"], r["notch_name"], r["notch_desc"]) for r in resolved}
    unmatched = [{"kind": kind, "notch_owner": owner, "source_owner": source_classes[owner],
                  "notch_name": item["name"], "notch_desc": item["desc"],
                  "srg": (srg_fields.get((owner, item["name"])) if kind == "FD"
                          else srg_methods.get((owner, item["name"], item["desc"])))}
                 for owner, info in sorted(vanilla.items()) if owner in source_classes
                 for kind, key in (("FD", "fields"), ("MD", "methods")) for item in info[key]
                 if (kind, owner, item["name"], item["desc"]) not in matched_keys]
    counts = collections.Counter(r["kind"] for r in resolved)
    report = {"patch_sha256": patch_hash, "vanilla_sha256": sha256(args.vanilla),
              "matches_sha256": sha256(args.matches), "patch_classes": len(runtime),
              "mapped_classes": len(source_targets), "mapped_fields": counts["FD"],
              "mapped_methods": counts["MD"], "required_classes": len(required),
              "missing_required_classes": missing_used, "unmatched_classes": missing_classes,
              "adapted_members": compatibility["members"], "unavailable": compatibility["unavailable"],
              "rejected_members": rejected, "unmatched_members": unmatched}
    outputs["mapping-update.json"] = json.dumps({k: v for k, v in report.items()
        if k not in ("unmatched_members", "rejected_members")}, indent=2) + "\n"
    if args.report:
        args.report.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in report.items()
                      if k not in ("unmatched_members", "rejected_members", "unmatched_classes")}, indent=2))
    print("Quarantined signatures:", len(rejected), "Unmatched vanilla members:", len(unmatched))
    if missing_used:
        raise ValueError("Refusing update: required classes are missing")
    if args.check:
        different = [name for name, value in outputs.items() if not (args.map_dir / name).exists()
                     or (args.map_dir / name).read_text(encoding="utf-8-sig") != value]
        if different:
            raise ValueError("Runtime tables differ from validated inputs: " + ", ".join(different))
        print("PASS: all runtime tables and startup anchors match the validated patch.")
        return
    if not args.write:
        print("Dry run: no runtime tables or dump files changed.")
        return
    dump_dir = args.dump_dir or args.cb_root / "out"
    inverse = {target: owner for owner, target in classes.items()}
    member_info = {(r["kind"], r["runtime_owner"], r["runtime_name"], r["runtime_desc"]): r for r in resolved}
    enriched = []
    for target, info in sorted(runtime.items()):
        value = dict(info)
        notch = inverse.get(target)
        value["notch"], value["minecraft"] = notch, source_classes.get(notch)
        for kind, key in (("FD", "fields"), ("MD", "methods")):
            value[key] = [dict(item, mapping=member_info.get((kind, target, item["name"], item["desc"])))
                          for item in info[key]]
        enriched.append(value)
    dump_outputs = {
        "resolved_matches.tsv": args.matches.read_text(encoding="utf-8"),
        "resolved_mappings.json": json.dumps(resolved, indent=1) + "\n",
        "resolved_mappings.tsv": table("kind<TAB>sourceOwner<TAB>mcpName<TAB>srgName<TAB>runtimeOwner<TAB>runtimeName<TAB>runtimeDescriptor",
            [(r["kind"], r["source_owner"], r["mcp"], r["srg"], r["runtime_owner"], r["runtime_name"], r["runtime_desc"]) for r in resolved]),
        "classes_resolved.json": json.dumps(enriched, ensure_ascii=False, indent=1) + "\n",
        "mapping-update.json": json.dumps(report, indent=2) + "\n",
        "mc_classes.txt": "# Matched against patch SHA-256 " + patch_hash + "\n\n" + "".join(
            source + "\n" + target + "\n\n" for source, target in sorted(source_targets.items())),
        "mc_classes_unmatched.txt": "# Unresolved vanilla client classes; no guessed mapping\n" + "\n".join(missing_classes) + "\n",
    }
    save_files(args.map_dir, outputs, dump_dir / ".mapping-backups")
    save_files(dump_dir, dump_outputs, dump_dir / ".mapping-backups")
    print("Updated runtime tables:", args.map_dir)
    print("Enriched dump (raw classes.json preserved):", dump_dir)


if __name__ == "__main__":
    main()
