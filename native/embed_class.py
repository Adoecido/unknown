#!/usr/bin/env python3
"""Convert MemoryJarLoader and its compiler-generated helpers into C data."""

import pathlib
import sys


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: embed_class.py INPUT.class OUTPUT.h", file=sys.stderr)
        return 2

    source = pathlib.Path(sys.argv[1])
    output = pathlib.Path(sys.argv[2])
    classes = [source]
    classes.extend(sorted(
        (
            candidate for candidate in source.parent.glob(source.stem + "$*.class")
            if candidate != source
        ),
        key=lambda candidate: (candidate.stem.count("$"), candidate.name),
    ))

    def symbol_for(path):
        return "g_mem_loader_" + "".join(
            character if character.isalnum() else "_"
            for character in path.stem
        ) + "_class"

    def append_bytes(lines, symbol, data):
        lines.append(f"static const unsigned char {symbol}[] = {{")
        for offset in range(0, len(data), 16):
            chunk = data[offset:offset + 16]
            lines.append("    " + ", ".join(
                f"0x{value:02x}" for value in chunk) + ",")
        lines.append("};")
        lines.append(f"static const unsigned int {symbol}_size = {len(data)};")
        lines.append("")

    lines = [
        "/* Auto-generated from the MemoryJarLoader class family. */",
        "#ifndef MEMORY_JAR_LOADER_CLASS_H",
        "#define MEMORY_JAR_LOADER_CLASS_H",
        "",
        "typedef struct sonyvegas_embedded_loader_class {",
        "    const char *binary_name;",
        "    const unsigned char *bytes;",
        "    unsigned int size;",
        "} sonyvegas_embedded_loader_class;",
        "",
    ]
    primary_data = source.read_bytes()
    append_bytes(lines, "g_mem_loader_class", primary_data)
    helper_symbols = []
    for helper in classes[1:]:
        helper_symbol = symbol_for(helper)
        helper_data = helper.read_bytes()
        helper_symbols.append((helper, helper_symbol, len(helper_data)))
        append_bytes(lines, helper_symbol, helper_data)
    lines.append("static const sonyvegas_embedded_loader_class g_mem_loader_helper_classes[] = {")
    for helper, helper_symbol, helper_size in helper_symbols:
        lines.append(
            f'    {{"{helper.stem}", {helper_symbol}, {helper_size}}},')
    lines.append("};")
    lines.append(
        "static const unsigned int g_mem_loader_helper_class_count = "
        f"{len(helper_symbols)};")
    lines.extend([
        "#endif",
        "",
    ])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines), encoding="ascii")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
