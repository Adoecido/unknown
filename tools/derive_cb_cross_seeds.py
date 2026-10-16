#!/usr/bin/env python3
"""Derive a few semantically stable cross-build CheatBreaker class seeds."""

import argparse
import json
from collections import defaultdict
from pathlib import Path


def load_classes(path):
    result = {}
    for raw in path.read_text(encoding="utf-8-sig").splitlines():
        if not raw or raw.startswith("#"):
            continue
        source, runtime = raw.split("\t", 1)[:2]
        result[source] = runtime
    return result


def one(values, label):
    values = list(dict.fromkeys(values))
    if len(values) != 1:
        raise RuntimeError("%s: expected one candidate, found %s" % (label, values))
    return values[0]


def constructors(cls):
    return [method for method in cls["methods"] if method["name"] == "<init>"]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--previous-classes", type=Path, required=True)
    parser.add_argument("--new-dump", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    previous = load_classes(args.previous_classes)
    current = json.loads(args.new_dump.read_text(encoding="utf-8"))
    seeds = []

    boolean_candidates = []
    for cls in current:
        field_types = [field["desc"] for field in cls["fields"]]
        method_descs = [method["desc"] for method in cls["methods"]]
        has_synthetic_accessor = any(
            method["access"] & 0x1000 and method["access"] & 0x0008
            and method["desc"] == "(L%s;)Z" % cls["name"]
            for method in cls["methods"]
        )
        if (any(method["desc"] == "(I)V" for method in constructors(cls))
                and "I" in field_types and "Z" in field_types
                and "(Z)V" in method_descs
                and method_descs.count("()V") >= 2
                and has_synthetic_accessor):
            boolean_candidates.append(cls["name"])
    boolean_state = one(boolean_candidates, "GlStateManager.BooleanState")

    blend_candidates = []
    for cls in current:
        field_types = [field["desc"] for field in cls["fields"]]
        ctors = constructors(cls)
        has_synthetic_ctor = any(
            method["access"] & 0x1000
            and method["desc"].startswith("(L")
            and method["desc"].endswith(";)V")
            for method in ctors
        )
        if ("L%s;" % boolean_state in field_types
                and field_types.count("I") >= 4
                and any(method["desc"] == "()V" for method in ctors)
                and has_synthetic_ctor):
            blend_candidates.append(cls["name"])
    blend_state = one(blend_candidates, "GlStateManager.BlendState")

    stable = {
        "net/minecraft/client/renderer/GlStateManager$BooleanState": boolean_state,
        "net/minecraft/client/renderer/GlStateManager$BlendState": blend_state,
    }

    packet_groups = defaultdict(dict)
    packet_descriptors = {
        "(IBBBZ)V": "net/minecraft/network/play/server/S14PacketEntity$S15PacketEntityRelMove",
        "(IBBZ)V": "net/minecraft/network/play/server/S14PacketEntity$S16PacketEntityLook",
        "(IBBBBBZ)V": "net/minecraft/network/play/server/S14PacketEntity$S17PacketEntityLookMove",
    }
    for cls in current:
        for method in constructors(cls):
            if method["desc"] in packet_descriptors:
                packet_groups[cls["super"]][method["desc"]] = cls["name"]
    packet_candidates = [
        members for members in packet_groups.values()
        if all(desc in members for desc in packet_descriptors)
    ]
    if len(packet_candidates) != 1:
        raise RuntimeError(
            "S14PacketEntity subclasses: expected one family, found %d"
            % len(packet_candidates))
    family = packet_candidates[0]
    for descriptor, source in packet_descriptors.items():
        stable[source] = family[descriptor]

    used_targets = set()
    for source, new_runtime in stable.items():
        old_runtime = previous.get(source)
        if not old_runtime:
            raise RuntimeError("Previous mapping is missing %s" % source)
        if new_runtime in used_targets:
            raise RuntimeError("Duplicate derived target: %s" % new_runtime)
        used_targets.add(new_runtime)
        seeds.append((old_runtime, new_runtime, source))

    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", encoding="utf-8", newline="\n") as stream:
        stream.write("# Automatically derived from stable bytecode structures.\n")
        for old_runtime, new_runtime, source in seeds:
            stream.write("# %s\n%s\t%s\n" % (source, old_runtime, new_runtime))
    print("Derived %d stable cross-build seeds:" % len(seeds))
    for old_runtime, new_runtime, source in seeds:
        print("  %s: %s -> %s" % (source, old_runtime, new_runtime))


if __name__ == "__main__":
    main()
