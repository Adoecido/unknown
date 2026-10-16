#!/usr/bin/env python3
"""Validate a CheatBreaker patch and publish a SonyVegas runtime profile.

The worker is called by sonyvegas_server.py after an authenticated loader
uploads a patch whose SHA-256 is not present on the mapping service.
"""

from __future__ import annotations

import fcntl
import hashlib
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path


REQUIRED = (
    "anchors.properties", "classes.tsv", "fields.tsv", "fields_fwd.tsv",
    "methods.tsv", "methods_fwd.tsv", "mapping-update.json",
)
DUMP_FORMAT = 2
EXTERNAL_REFERENCE_PREFIXES = (
    "java/", "javax/", "org/lwjgl/", "org/apache/", "com/google/", "com/mojang/",
)


def run(*arguments: object) -> None:
    command = [str(value) for value in arguments]
    print("+", " ".join(command), flush=True)
    subprocess.run(command, check=True)


def matcher_heap_argument() -> str:
    """Choose a heap that fits both large and small VPS plans.

    The old fixed ``-Xmx4G`` makes the worker fail immediately on common
    1--2 GiB VPS instances.  Reserve capacity for the operating system and
    Python, cap the matcher at 3 GiB, and never ask the JVM for less than the
    amount it needs to begin a meaningful pass.
    """
    available = 0
    try:
        for line in Path("/proc/meminfo").read_text(encoding="utf-8").splitlines():
            if line.startswith("MemAvailable:"):
                available = int(line.split()[1]) * 1024
                break
    except (OSError, ValueError, IndexError):
        pass
    if available <= 0:
        return "-Xmx1536m"
    heap = max(768 * 1024 * 1024, min(3 * 1024 * 1024 * 1024, available * 2 // 3))
    return "-Xmx%dm" % max(768, heap // (1024 * 1024))


def is_freshly_compiled(output: Path, *sources: Path) -> bool:
    """Avoid recompiling the matcher helpers for every uploaded patch."""
    marker = output / "MatchCheatBreaker.class"
    if not marker.is_file():
        return False
    built_at = marker.stat().st_mtime
    return all(source.is_file() and source.stat().st_mtime <= built_at
               for source in sources)


def dump_matches_source(dump_dir: Path, source: Path) -> bool:
    """True only when a format-compatible structural dump belongs to source."""
    metadata = dump_dir / "dump-metadata.json"
    classes = dump_dir / "classes.json"
    if not metadata.is_file() or not classes.is_file() or classes.stat().st_size == 0:
        return False
    try:
        details = load_json(metadata)
        return (
            int(details.get("format") or 0) == DUMP_FORMAT
            and str(details.get("sha256") or "").lower()
            == hashlib.sha256(source.read_bytes()).hexdigest()
        )
    except (OSError, ValueError, json.JSONDecodeError):
        return False


def write_required_previous_classes(
    classes_tsv: Path, manifest: Path, destination: Path
) -> None:
    """Limit costly matching to classes that the injected runtime can use."""
    required = {
        line.strip().replace(".", "/")
        for line in manifest.read_text(encoding="utf-8-sig").splitlines()
        if line.strip() and not line.startswith("#")
    }
    # These are used by the deterministic post-matcher rules below even when
    # no Java wrapper requests them directly.
    required.update({
        "net/minecraft/network/play/client/C03PacketPlayer",
        "net/minecraft/client/renderer/GlStateManager$BooleanState",
        "net/minecraft/client/renderer/GlStateManager$1",
        "net/minecraft/item/Item",
        "net/minecraft/entity/item/EntityEnderPearl",
    })
    targets: set[str] = set()
    for line in classes_tsv.read_text(encoding="utf-8-sig").splitlines():
        if not line or line.startswith("#"):
            continue
        source, target, *_ = line.split("\t")
        if source.replace(".", "/") in required:
            targets.add(target)
    if not targets:
        raise RuntimeError(f"no required runtime classes found in {classes_tsv}")
    destination.write_text("\n".join(sorted(targets)) + "\n", encoding="utf-8")


def load_json(path: Path) -> dict[str, object]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise RuntimeError(f"invalid JSON object: {path}")
    return value


def load_json_list(path: Path) -> list[object]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, list):
        raise RuntimeError(f"invalid JSON array: {path}")
    return value


def load_class_targets(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8-sig").splitlines():
        if not raw or raw.startswith("#"):
            continue
        source, runtime = raw.split("\t", 2)[:2]
        result[source] = runtime
    return result


def required_class_sources(manifest: Path) -> set[str]:
    return {
        line.strip().replace(".", "/")
        for line in manifest.read_text(encoding="utf-8-sig").splitlines()
        if line.strip() and not line.startswith("#")
    }


def external_reference_signature(cls: dict[str, object]) -> tuple[tuple[str, str, str, str], ...]:
    """Return stable library calls that survive CheatBreaker's renaming.

    Member names inside the game archive are obfuscated on every update, but
    calls into the JDK, LWJGL, Guava, Mojang, and Apache libraries retain their
    owner/name/descriptors.  They make a conservative identity fingerprint for
    small client helpers that the generic graph matcher can leave unmatched.
    """
    result: set[tuple[str, str, str, str]] = set()
    references = cls.get("references")
    if not isinstance(references, list):
        return ()
    for reference in references:
        if not isinstance(reference, dict):
            continue
        kind = reference.get("kind")
        owner = reference.get("owner")
        name = reference.get("name")
        descriptor = reference.get("desc")
        if not all(isinstance(value, str) for value in (kind, owner, name, descriptor)):
            continue
        if owner.startswith(EXTERNAL_REFERENCE_PREFIXES):
            result.add((kind, owner, name, descriptor))
    return tuple(sorted(result))


def complete_required_external_reference_matches(
    previous_classes: Path,
    required_manifest: Path,
    old_dump: Path,
    new_dump: Path,
    cross_matches: Path,
) -> None:
    """Map uniquely fingerprinted required classes left by the matcher.

    Only an exact, non-empty external-reference signature with one unused
    target is accepted.  Ambiguous classes stay unmapped and validation still
    blocks publication; this enriches automation without guessing.
    """
    previous = load_class_targets(previous_classes)
    required = required_class_sources(required_manifest)
    old_classes = {
        str(cls["name"]): cls
        for cls in load_json_list(old_dump)
        if isinstance(cls, dict) and isinstance(cls.get("name"), str)
    }
    new_classes = [
        cls for cls in load_json_list(new_dump)
        if isinstance(cls, dict) and isinstance(cls.get("name"), str)
    ]
    cross: dict[str, str] = {}
    used: set[str] = set()
    for raw in cross_matches.read_text(encoding="utf-8-sig").splitlines():
        row = raw.split("\t")
        if len(row) == 7 and row[0] == "CL":
            cross[row[1]] = row[4]
            used.add(row[4])

    candidates_by_signature: dict[tuple[tuple[str, str, str, str], ...], list[str]] = {}
    for cls in new_classes:
        name = str(cls["name"])
        if name in used:
            continue
        signature = external_reference_signature(cls)
        if len(signature) >= 2:
            candidates_by_signature.setdefault(signature, []).append(name)

    additions: list[str] = []
    for source in sorted(required):
        old_name = previous.get(source)
        if not old_name or old_name in cross:
            continue
        old_class = old_classes.get(old_name)
        if not old_class:
            continue
        signature = external_reference_signature(old_class)
        candidates = [
            name for name in candidates_by_signature.get(signature, [])
            if name not in used
        ]
        if len(signature) < 2 or len(candidates) != 1:
            continue
        target = candidates[0]
        additions.append(f"CL\t{old_name}\t\t\t{target}\t\t")
        cross[old_name] = target
        used.add(target)
        print(
            f"Resolved {source}: {old_name} -> {target} "
            "(unique external-reference signature)",
            flush=True,
        )
    if additions:
        with cross_matches.open("a", encoding="utf-8", newline="\n") as stream:
            for line in additions:
                stream.write(line + "\n")


def has_required_cross_matches(previous_classes: Path, cross_matches: Path) -> bool:
    """Reject interrupted matcher output before trying deterministic completion.

    A non-empty ``matches.tsv`` is not a completed matcher pass.  The
    deterministic rules below need these outer classes to resolve the packet,
    item, and renderer variants.  Reusing a partial pass otherwise turns a
    recoverable interruption into an immediate ``KeyError`` on every retry.
    """
    required_sources = (
        "net/minecraft/network/play/client/C03PacketPlayer",
        "net/minecraft/client/renderer/GlStateManager$BooleanState",
        "net/minecraft/item/Item",
        "net/minecraft/entity/item/EntityEnderPearl",
        "net/minecraft/entity/projectile/EntityEgg",
        "net/minecraft/entity/projectile/EntitySnowball",
    )
    try:
        previous = load_class_targets(previous_classes)
        matched = {
            row[1]
            for raw in cross_matches.read_text(encoding="utf-8-sig").splitlines()
            if len(row := raw.split("\t")) == 7 and row[0] == "CL"
        }
    except (OSError, ValueError):
        return False
    missing = [
        source for source in required_sources
        if previous.get(source) not in matched
    ]
    if missing:
        print(
            "discarding incomplete matcher output; missing required matches: "
            + ", ".join(missing),
            flush=True,
        )
        return False
    return True


def complete_projectile_item_matches(
    previous_classes: Path, cross_matches: Path, new_dump: Path, new_jar: Path
) -> None:
    """Resolve ItemEgg/ItemSnowball through their uniquely constructed entity.

    These two item classes are nearly byte-for-byte identical and the generic
    matcher intentionally leaves them ambiguous. Their on-use methods construct
    different projectile entities, which are already resolved unambiguously.
    """
    previous = load_class_targets(previous_classes)
    cross_lines = cross_matches.read_text(encoding="utf-8-sig").splitlines()
    cross_classes: dict[str, str] = {}
    used_targets: dict[str, str] = {}
    for raw in cross_lines:
        row = raw.split("\t")
        if len(row) == 7 and row[0] == "CL":
            cross_classes[row[1]] = row[4]
            used_targets[row[4]] = row[1]

    item_base = previous["net/minecraft/item/Item"]
    new_item_base = cross_classes[item_base]
    current = json.loads(new_dump.read_text(encoding="utf-8"))
    candidates = [
        str(cls["name"]) for cls in current
        if cls.get("super") == new_item_base
    ]
    inspected_candidates = {
        candidate: subprocess.run(
            ["javap", "-classpath", str(new_jar), "-p", "-c", candidate],
            check=True, capture_output=True, text=True,
        ).stdout
        for candidate in candidates
    }
    resolved = (
        ("net/minecraft/item/ItemEgg",
         "net/minecraft/entity/projectile/EntityEgg"),
        ("net/minecraft/item/ItemSnowball",
         "net/minecraft/entity/projectile/EntitySnowball"),
    )
    additions: list[str] = []
    for item_source, entity_source in resolved:
        old_item = previous[item_source]
        if old_item in cross_classes:
            continue
        new_entity = cross_classes[previous[entity_source]]
        matches: list[str] = []
        for candidate, inspected in inspected_candidates.items():
            if f"// class {new_entity}" in inspected:
                matches.append(candidate)
        if len(matches) != 1:
            raise RuntimeError(
                f"{item_source}: expected one projectile item, found {matches}"
            )
        new_item = matches[0]
        previous_owner = used_targets.get(new_item)
        if previous_owner and previous_owner != old_item:
            raise RuntimeError(
                f"{item_source}: target already mapped from {previous_owner}"
            )
        additions.append(f"CL\t{old_item}\t\t\t{new_item}\t\t")
        cross_classes[old_item] = new_item
        used_targets[new_item] = old_item
        print(
            f"Resolved projectile item {item_source}: {old_item} -> {new_item}",
            flush=True,
        )
    if additions:
        with cross_matches.open("a", encoding="utf-8", newline="\n") as stream:
            for line in additions:
                stream.write(line + "\n")


def complete_required_structural_matches(
    previous_classes: Path, cross_matches: Path, new_dump: Path, new_jar: Path
) -> None:
    """Resolve required classes that Matcher leaves ambiguous after two passes.

    PacketPlayer variants are identified by their unique constructor shapes.
    TextureState is identified by its BooleanState field and GL_TEXTURE_2D
    initializer. ItemEnderPearl is tied to the already matched projectile class.
    Every rule must produce exactly one unused target before it is accepted.
    """
    previous = load_class_targets(previous_classes)
    lines = cross_matches.read_text(encoding="utf-8-sig").splitlines()
    cross: dict[str, str] = {}
    used: dict[str, str] = {}
    for raw in lines:
        row = raw.split("\t")
        if len(row) == 7 and row[0] == "CL":
            cross[row[1]] = row[4]
            used[row[4]] = row[1]
    current = json.loads(new_dump.read_text(encoding="utf-8"))

    additions: list[str] = []

    def add(source_name: str, candidates: list[str], reason: str) -> None:
        old = previous[source_name]
        if old in cross:
            return
        available = [name for name in candidates if name not in used]
        if len(available) != 1:
            raise RuntimeError(
                f"{source_name}: expected one {reason} candidate, found {available}"
            )
        target = available[0]
        additions.append(f"CL\t{old}\t\t\t{target}\t\t")
        cross[old] = target
        used[target] = old
        print(f"Resolved {source_name}: {old} -> {target} ({reason})", flush=True)

    packet_outer = cross[previous["net/minecraft/network/play/client/C03PacketPlayer"]]
    packet_classes = [cls for cls in current if cls.get("super") == packet_outer]
    packet_variants = (
        ("net/minecraft/network/play/client/C03PacketPlayer$C04PacketPlayerPosition", "(DDDZ)V"),
        ("net/minecraft/network/play/client/C03PacketPlayer$C05PacketPlayerLook", "(FFZ)V"),
        ("net/minecraft/network/play/client/C03PacketPlayer$C06PacketPlayerPosLook", "(DDDFFZ)V"),
    )
    for source, descriptor in packet_variants:
        add(source, [
            str(cls["name"]) for cls in packet_classes
            if any(method.get("name") == "<init>" and method.get("desc") == descriptor
                   for method in cls.get("methods", []))
        ], f"constructor {descriptor}")

    boolean_state = cross[
        previous["net/minecraft/client/renderer/GlStateManager$BooleanState"]
    ]
    marker_candidates: set[str] = set()
    for cls in current:
        fields = cls.get("fields", [])
        methods = cls.get("methods", [])
        if not any(field.get("desc") == f"L{boolean_state};" for field in fields):
            continue
        if not any(method.get("name") == "<init>" and method.get("desc") == "()V"
                   for method in methods):
            continue
        for method in methods:
            descriptor = str(method.get("desc") or "")
            if method.get("name") == "<init>" and descriptor.startswith("(L") and descriptor.endswith(";)V"):
                marker_candidates.add(descriptor[2:-3])
    if len(marker_candidates) != 1:
        raise RuntimeError(
            "could not identify GlStateManager synthetic constructor marker: "
            + repr(sorted(marker_candidates))
        )
    synthetic_marker = next(iter(marker_candidates))
    texture_candidates: list[str] = []
    for cls in current:
        fields = cls.get("fields", [])
        methods = cls.get("methods", [])
        if not any(field.get("desc") == f"L{boolean_state};" for field in fields):
            continue
        if not any(method.get("name") == "<init>" and method.get("desc") == "()V"
                   for method in methods):
            continue
        if not any(method.get("name") == "<init>"
                   and method.get("desc") == f"(L{synthetic_marker};)V"
                   for method in methods):
            continue
        inspected = subprocess.run(
            ["javap", "-classpath", str(new_jar), "-p", "-c", str(cls["name"])],
            check=True, capture_output=True, text=True,
        ).stdout
        if "sipush        3553" in inspected or "// int 3553" in inspected:
            texture_candidates.append(str(cls["name"]))
    add(
        "net/minecraft/client/renderer/GlStateManager$TextureState",
        texture_candidates, "GL_TEXTURE_2D initializer",
    )

    item_base = cross[previous["net/minecraft/item/Item"]]
    pearl_entity = cross[previous["net/minecraft/entity/item/EntityEnderPearl"]]
    pearl_candidates: list[str] = []
    for cls in current:
        if cls.get("super") != item_base:
            continue
        inspected = subprocess.run(
            ["javap", "-classpath", str(new_jar), "-p", "-c", str(cls["name"])],
            check=True, capture_output=True, text=True,
        ).stdout
        if f"// class {pearl_entity}" in inspected and "bipush        16" in inspected:
            pearl_candidates.append(str(cls["name"]))
    add("net/minecraft/item/ItemEnderPearl", pearl_candidates,
        "matched EntityEnderPearl construction")

    if additions:
        with cross_matches.open("a", encoding="utf-8", newline="\n") as stream:
            for line in additions:
                stream.write(line + "\n")


def copy_profile(map_dir: Path, destination: Path, patch_hash: str) -> None:
    temporary = destination.with_name(destination.name + ".upload")
    shutil.rmtree(temporary, ignore_errors=True)
    temporary.mkdir(parents=True)
    for name in REQUIRED:
        source = map_dir / name
        if not source.is_file() or source.stat().st_size == 0:
            raise RuntimeError(f"missing validated mapping file: {source}")
        shutil.copy2(source, temporary / name)

    def rows(name: str) -> list[list[str]]:
        result: list[list[str]] = []
        for line in (map_dir / name).read_text(encoding="utf-8-sig").splitlines():
            if line and not line.startswith("#"):
                result.append(line.split("\t"))
        return result

    runtime_json = {
        "format": "sonyvegas-cheatbreaker-runtime-v1",
        "patch_sha256": patch_hash,
        "classes": rows("classes.tsv"),
        "fields": rows("fields.tsv"),
        "forward_fields": rows("fields_fwd.tsv"),
        "methods": rows("methods.tsv"),
        "forward_methods": rows("methods_fwd.tsv"),
    }
    (temporary / "cheatbreaker-runtime.json").write_text(
        json.dumps(runtime_json, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    shutil.rmtree(destination, ignore_errors=True)
    temporary.replace(destination)


def validate_rotation_members(map_dir: Path) -> None:
    """Require the members used by the 1.8.9 motion/aim hook.

    Class mappings alone are not enough for Aura: the motion injection needs
    the walking-update method and the yaw/pitch/on-ground fields, while aim
    helpers need Entity#getPositionEyes.  Publish no profile that silently
    loses one of these mappings.
    """
    methods = (map_dir / "methods.tsv").read_text(encoding="utf-8-sig")
    fields = (map_dir / "fields.tsv").read_text(encoding="utf-8-sig")
    required_methods = ("func_175161_p", "func_174824_e")
    required_fields = ("field_70177_z", "field_70125_A", "field_70122_E")
    missing_methods = [name for name in required_methods
                       if not any(f"\t{name}\t" in line
                                  for line in methods.splitlines())]
    missing_fields = [name for name in required_fields
                      if not any(f"\t{name}\t" in line
                                 for line in fields.splitlines())]
    if missing_methods or missing_fields:
        raise RuntimeError(
            "rotation/aim mappings incomplete: "
            f"methods={missing_methods} fields={missing_fields}"
        )


def cache_worker_profile(
    profiles: Path, patch_hash: str, normalized: Path,
    resolved_matches: Path, compatibility: Path,
) -> None:
    """Atomically retain the artifacts needed to map the next patch."""
    required = (normalized, resolved_matches, compatibility)
    if any(not path.is_file() or path.stat().st_size == 0 for path in required):
        raise RuntimeError("cannot cache incomplete mapping worker profile")
    destination = profiles / patch_hash
    temporary = destination.with_name(destination.name + ".upload")
    shutil.rmtree(temporary, ignore_errors=True)
    temporary.mkdir(parents=True)
    shutil.copy2(normalized, temporary / "normalized.jar")
    shutil.copy2(resolved_matches, temporary / "resolved_matches.tsv")
    shutil.copy2(compatibility, temporary / "compatibility.json")
    shutil.rmtree(destination, ignore_errors=True)
    temporary.replace(destination)


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: process_cb_mappings.py PATCH SHA256", file=sys.stderr)
        return 2
    patch = Path(sys.argv[1]).resolve()
    expected_hash = sys.argv[2].lower()
    actual_hash = hashlib.sha256(patch.read_bytes()).hexdigest()
    if actual_hash != expected_hash:
        raise RuntimeError("uploaded patch hash changed before analysis")

    root = Path(os.environ.get(
        "SONYVEGAS_MAPPING_ROOT", "/var/lib/sonyvegas/mapping-worker"
    ))
    repository = root / "workspace" / "sonyvegas"
    cb_root = root / "workspace" / "cb_mappings"
    data_root = Path(os.environ.get("SONYVEGAS_DATA", "/var/lib/sonyvegas"))
    map_dir = repository / "src/main/resources/mappings/cheatbreaker189"
    previous_dir = repository / "src/main/resources/mappings/cheatbreaker189-previous"
    mapped_classes_source = (
        repository / "src/main/java/vegas/sony/mapping/MappedClasses.java"
    )
    required_classes_manifest = repository / "tools/cb189_required_classes.txt"
    compatibility = repository / "tools/cb189_compatibility.json"
    dump_tool = repository / "tools/dump_cb_mappings.py"
    state = cb_root / "auto_state"
    matcher = cb_root / "lib/matcher-0.1.0-win.jar"
    vanilla = cb_root / "mc_ref/1.8.9.jar"
    tools_classes = state / "tools-linux"
    profiles = state / "profiles"
    profiles.mkdir(parents=True, exist_ok=True)

    # Fail before the expensive matcher pass when the worker workspace was
    # provisioned without all source files required by update_cb_mappings.py.
    if not mapped_classes_source.is_file() and not required_classes_manifest.is_file():
        raise RuntimeError(
            "mapping worker class requirements are missing: "
            f"{mapped_classes_source} or {required_classes_manifest}"
        )

    with (root / "worker.lock").open("w") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        dump_dir = cb_root / "out"
        if dump_matches_source(dump_dir, patch):
            print(f"reusing structural dump: {expected_hash[:8]}", flush=True)
        else:
            if not dump_tool.is_file():
                raise RuntimeError(f"mapping dump tool is missing: {dump_tool}")
            run(sys.executable, dump_tool, patch, dump_dir)
        tools_classes.mkdir(parents=True, exist_ok=True)
        tool_sources = (
            repository / "tools/NormalizeCheatBreaker.java",
            repository / "tools/MatchCheatBreaker.java",
            repository / "tools/InspectCheatBreaker.java",
        )
        if is_freshly_compiled(tools_classes, *tool_sources):
            print("reusing compiled matcher helpers", flush=True)
        else:
            run("javac", "-cp", matcher, "-d", tools_classes, *tool_sources)
        metadata = load_json(map_dir / "mapping-update.json")
        current_hash = str(metadata.get("patch_sha256") or "").lower()
        if current_hash == expected_hash:
            # A prior run may have completed the atomic mapping-table write
            # but been interrupted before its worker cache was retained.  Use
            # the persisted run artifacts to complete that recovery instead
            # of treating the new map as a dead end for future patch updates.
            recovery = state / "runs" / expected_hash
            recovery_normalized = recovery / "normalized.jar"
            recovery_compatibility = recovery / "compatibility.json"
            resolved_matches = cb_root / "out" / "resolved_matches.tsv"
            if all(path.is_file() and path.stat().st_size > 0 for path in (
                recovery_normalized, recovery_compatibility, resolved_matches,
            )):
                cache_worker_profile(
                    profiles, expected_hash, recovery_normalized,
                    resolved_matches, recovery_compatibility,
                )
            copy_profile(map_dir, data_root / "mappings/profiles" / expected_hash,
                         expected_hash)
            print(f"mapping profile already current: {expected_hash[:8]}", flush=True)
            return 0

        old_profile = profiles / current_hash
        old_normalized = old_profile / "normalized.jar"
        old_matches = old_profile / "resolved_matches.tsv"
        old_compatibility = old_profile / "compatibility.json"
        for required in (old_normalized, old_matches, old_compatibility):
            if not required.is_file():
                raise RuntimeError(f"previous profile cache missing: {required}")
        old_dump = old_profile / "structural-dump"
        if dump_matches_source(old_dump, old_normalized):
            print(f"reusing reference structural dump: {current_hash[:8]}", flush=True)
        else:
            run(sys.executable, dump_tool, old_normalized, old_dump)

        work = state / "runs" / expected_hash
        cross = work / "cross"
        new_normalized = work / "normalized.jar"
        seeds = work / "stable-seeds.tsv"
        translated = work / "translated.tsv"
        new_compatibility = work / "compatibility.json"
        empty = work / "empty.tsv"
        report = work / "report.json"
        required_previous = work / "required-previous-classes.txt"
        completed_match = (
            all(path.is_file() and path.stat().st_size > 0 for path in (
                cross / "matches.tsv", cross / "member-evidence-2.tsv",
            ))
            and has_required_cross_matches(map_dir / "classes.tsv", cross / "matches.tsv")
        )
        if completed_match:
            print(f"resuming completed matcher run: {expected_hash[:8]}", flush=True)
        else:
            work.mkdir(parents=True, exist_ok=True)
            # Keep completed independent stages. If the worker is interrupted,
            # retrying the loader resumes instead of repeating the expensive
            # normalizer and seed derivation.
            if not new_normalized.is_file() or new_normalized.stat().st_size == 0:
                run("java", "-cp", f"{tools_classes}{os.pathsep}{matcher}",
                    "NormalizeCheatBreaker", patch, new_normalized)
            # These seeds depend on the active previous mapping table.  The
            # patch-specific run directory can survive a server deployment,
            # so keeping an older seed file here can point Matcher at classes
            # that disappeared from the newly selected reference profile.
            # Derivation is cheap and deterministic; refresh it before every
            # matcher pass instead of treating the file's presence as valid.
            run(
                sys.executable, repository / "tools/derive_cb_cross_seeds.py",
                "--previous-classes", map_dir / "classes.tsv",
                "--new-dump", dump_dir / "classes.json", "--out", seeds,
            )
            write_required_previous_classes(
                map_dir / "classes.tsv", required_classes_manifest, required_previous
            )
            shutil.rmtree(cross, ignore_errors=True)
            run(
                "java", matcher_heap_argument(), "-cp", f"{tools_classes}{os.pathsep}{matcher}",
                "MatchCheatBreaker", old_normalized, new_normalized, cross,
                "-", seeds, "propagate", required_previous,
            )
        complete_required_structural_matches(
            map_dir / "classes.tsv", cross / "matches.tsv",
            cb_root / "out/classes.json", new_normalized,
        )
        complete_required_external_reference_matches(
            map_dir / "classes.tsv", required_classes_manifest,
            old_dump / "classes.json", cb_root / "out/classes.json",
            cross / "matches.tsv",
        )
        complete_projectile_item_matches(
            map_dir / "classes.tsv", cross / "matches.tsv",
            cb_root / "out/classes.json", new_normalized,
        )
        empty.write_text(
            "# kind\tnotch owner\tnotch member\tnotch descriptor\t"
            "runtime owner\truntime member\truntime descriptor\n",
            encoding="utf-8",
        )
        run(
            sys.executable, repository / "tools/translate_cb_matches.py",
            "--previous", old_matches, "--cross", cross / "matches.tsv",
            "--current", empty, "--out", translated,
        )
        run(
            sys.executable, repository / "tools/translate_cb_compatibility.py",
            "--previous", old_compatibility, "--cross", cross / "matches.tsv",
            "--patch-sha256", expected_hash, "--out", new_compatibility,
        )
        update = [
            sys.executable, repository / "tools/update_cb_mappings.py",
            "--matches", translated, "--vanilla", vanilla, "--cb-jar", patch,
            "--cb-root", cb_root, "--mc-ref", cb_root / "mc_ref",
            "--compatibility", new_compatibility, "--report", report,
        ]
        # Preserve the last known-good profile before the atomic write below.
        previous_dir.mkdir(parents=True, exist_ok=True)
        for name in REQUIRED:
            source = map_dir / name
            if source.is_file():
                shutil.copy2(source, previous_dir / name)
        # --write validates all inputs before atomically replacing runtime
        # tables. Running the previous dry-run and a third --check pass made
        # every update parse both multi-megabyte jars three times.
        run(*update, "--write")
        validation = load_json(report)
        if validation.get("missing_required_classes"):
            raise RuntimeError("required mappings remain ambiguous")
        validate_rotation_members(map_dir)

        # The workspace may be provisioned by root while this service runs as
        # sonyvegas.  Metadata preservation in copy2 then fails at utime even
        # though the content itself is writable; only the file contents are
        # relevant to a future mapping pass.
        shutil.copyfile(new_compatibility, compatibility)

        cache_worker_profile(
            profiles, expected_hash, new_normalized,
            cb_root / "out/resolved_matches.tsv", new_compatibility,
        )
        copy_profile(map_dir, data_root / "mappings/profiles" / expected_hash,
                     expected_hash)
        print(f"published mapping profile: {expected_hash[:8]}", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
