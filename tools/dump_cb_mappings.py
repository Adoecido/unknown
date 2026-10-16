#!/usr/bin/env python3
"""Read the class metadata needed by the CheatBreaker mapping tools.

The mapping worker deliberately analyses archives without loading their classes.
Keeping this parser in the product repository makes the worker self-contained:
it no longer relies on an untracked copy of ``cb_mappings/dump_cb_mappings.py``.
"""
from __future__ import annotations

import json
import struct
import sys
import zipfile
from pathlib import Path
from typing import BinaryIO


DUMP_FORMAT = 2


def _u1(stream: BinaryIO) -> int:
    value = stream.read(1)
    if len(value) != 1:
        raise ValueError("truncated class file")
    return value[0]


def _u2(stream: BinaryIO) -> int:
    value = stream.read(2)
    if len(value) != 2:
        raise ValueError("truncated class file")
    return struct.unpack(">H", value)[0]


def _u4(stream: BinaryIO) -> int:
    value = stream.read(4)
    if len(value) != 4:
        raise ValueError("truncated class file")
    return struct.unpack(">I", value)[0]


def _skip(stream: BinaryIO, size: int) -> None:
    if len(stream.read(size)) != size:
        raise ValueError("truncated class file")


def _attributes(stream: BinaryIO) -> None:
    for _ in range(_u2(stream)):
        _u2(stream)
        _skip(stream, _u4(stream))


def _member(stream: BinaryIO, pool: list[object | None]) -> dict[str, object]:
    access = _u2(stream)
    name = _utf8(pool, _u2(stream))
    descriptor = _utf8(pool, _u2(stream))
    _attributes(stream)
    return {"access": access, "name": name, "desc": descriptor}


def _utf8(pool: list[object | None], index: int) -> str:
    value = pool[index]
    if not isinstance(value, str):
        raise ValueError("invalid UTF-8 constant-pool reference")
    return value


def _class_name(pool: list[object | None], index: int) -> str | None:
    if index == 0:
        return None
    value = pool[index]
    if not isinstance(value, tuple) or value[0] != "class":
        raise ValueError("invalid class constant-pool reference")
    return _utf8(pool, int(value[1]))


def _name_and_type(pool: list[object | None], index: int) -> tuple[str, str]:
    value = pool[index]
    if not isinstance(value, tuple) or value[0] != "name_and_type":
        raise ValueError("invalid name-and-type constant-pool reference")
    return _utf8(pool, int(value[1])), _utf8(pool, int(value[2]))


def _member_reference(
    pool: list[object | None], value: tuple[object, ...]
) -> dict[str, str]:
    if len(value) != 4 or value[0] != "member":
        raise ValueError("invalid member constant-pool reference")
    kind = {9: "field", 10: "method", 11: "interface_method"}.get(int(value[1]))
    if kind is None:
        raise ValueError("invalid member constant-pool tag")
    owner = _class_name(pool, int(value[2]))
    if owner is None:
        raise ValueError("member reference without an owner")
    name, descriptor = _name_and_type(pool, int(value[3]))
    return {"kind": kind, "owner": owner, "name": name, "desc": descriptor}


def parse_class(data: bytes) -> dict[str, object]:
    """Return stable structural metadata for one JVM class file."""
    from io import BytesIO

    stream = BytesIO(data)
    if _u4(stream) != 0xCAFEBABE:
        raise ValueError("not a JVM class file")
    _u2(stream)
    _u2(stream)
    pool: list[object | None] = [None] * _u2(stream)
    index = 1
    while index < len(pool):
        tag = _u1(stream)
        if tag == 1:
            raw = stream.read(_u2(stream))
            if len(raw) == 0:
                pool[index] = ""
            else:
                pool[index] = raw.decode("utf-8", errors="replace")
        elif tag in (3, 4):
            _skip(stream, 4)
        elif tag in (5, 6):
            _skip(stream, 8)
            index += 1
        elif tag == 7:
            pool[index] = ("class", _u2(stream))
        elif tag == 8:
            pool[index] = ("string", _u2(stream))
        elif tag in (9, 10, 11):
            pool[index] = ("member", tag, _u2(stream), _u2(stream))
        elif tag == 12:
            pool[index] = ("name_and_type", _u2(stream), _u2(stream))
        elif tag in (17, 18):
            _skip(stream, 4)
        elif tag == 15:
            _skip(stream, 3)
        elif tag in (16, 19, 20):
            _skip(stream, 2)
        else:
            raise ValueError("unknown constant-pool tag %d" % tag)
        index += 1

    access = _u2(stream)
    name = _class_name(pool, _u2(stream))
    super_name = _class_name(pool, _u2(stream))
    interfaces = [_class_name(pool, _u2(stream)) for _ in range(_u2(stream))]
    fields = [_member(stream, pool) for _ in range(_u2(stream))]
    methods = [_member(stream, pool) for _ in range(_u2(stream))]
    _attributes(stream)
    strings = [
        _utf8(pool, int(value[1]))
        for value in pool
        if isinstance(value, tuple) and value[0] == "string"
    ]
    references = [
        _member_reference(pool, value)
        for value in pool
        if isinstance(value, tuple) and value[0] == "member"
    ]
    return {
        "access": access,
        "name": name,
        "super": super_name,
        "interfaces": [item for item in interfaces if item is not None],
        "fields": fields,
        "methods": methods,
        "strings": strings,
        "references": references,
    }


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: dump_cb_mappings.py INPUT_JAR OUTPUT_DIR", file=sys.stderr)
        return 2
    source = Path(sys.argv[1])
    destination = Path(sys.argv[2])
    classes: list[dict[str, object]] = []
    with zipfile.ZipFile(source) as archive:
        for entry in archive.infolist():
            if entry.is_dir() or not entry.filename.endswith(".class"):
                continue
            classes.append(parse_class(archive.read(entry)))
    classes.sort(key=lambda item: str(item["name"]))
    destination.mkdir(parents=True, exist_ok=True)
    (destination / "classes.json").write_text(
        json.dumps(classes, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    (destination / "dump-metadata.json").write_text(
        json.dumps({"format": DUMP_FORMAT,
                    "sha256": __import__("hashlib").sha256(source.read_bytes()).hexdigest()},
                   separators=(",", ":")),
        encoding="utf-8",
    )
    print("dumped %d classes" % len(classes), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
