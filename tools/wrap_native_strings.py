#!/usr/bin/env python3
"""Wrap runtime C string literals in the native SV_STR/SV_WSTR macros.

This is a source-maintenance helper. Include and pragma operands stay plain
because the C preprocessor must consume them before runtime expressions exist.
"""

from __future__ import annotations

import hashlib
import pathlib
import re
import sys


TOKEN = re.compile(
    r"(?P<comment>//[^\n]*(?:\n|$)|/\*.*?\*/)"
    r"|(?P<char>(?:L|u|U)?'(?:\\.|[^'\\])*')"
    r'|(?P<string>(?:L)?"(?:\\.|[^"\\])*")',
    re.DOTALL,
)
PROTECTED_START = re.compile(r"SV_(?:W?STR)\s*\(")
IGNORED_DIRECTIVE = re.compile(r"^\s*#\s*(?:include|pragma)\b")


def protected_spans(text: str) -> list[tuple[int, int]]:
    spans: list[tuple[int, int]] = []
    for start_match in PROTECTED_START.finditer(text):
        depth = 1
        index = start_match.end()
        while index < len(text) and depth:
            token = TOKEN.match(text, index)
            if token:
                index = token.end()
                continue
            if text[index] == "(":
                depth += 1
            elif text[index] == ")":
                depth -= 1
            index += 1
        if depth == 0:
            spans.append((start_match.start(), index))
    return spans


def inside_spans(position: int, spans: list[tuple[int, int]]) -> bool:
    return any(start <= position < end for start, end in spans)


def ignored_preprocessor_operand(text: str, position: int) -> bool:
    line_start = text.rfind("\n", 0, position) + 1
    return bool(IGNORED_DIRECTIVE.match(text[line_start:position]))


def only_space_or_comments(value: str) -> bool:
    index = 0
    while index < len(value):
        if value[index].isspace():
            index += 1
            continue
        token = TOKEN.match(value, index)
        if token and token.lastgroup == "comment":
            index = token.end()
            continue
        return False
    return True


def parse_literal(token: str) -> str:
    body = token[2:-1] if token.startswith("L") else token[1:-1]
    simple = {
        "a": "\a",
        "b": "\b",
        "f": "\f",
        "n": "\n",
        "r": "\r",
        "t": "\t",
        "v": "\v",
        "\\": "\\",
        "'": "'",
        '"': '"',
        "?": "?",
    }
    decoded: list[str] = []
    index = 0
    while index < len(body):
        if body[index] != "\\":
            decoded.append(body[index])
            index += 1
            continue
        index += 1
        if index >= len(body):
            raise ValueError("unterminated escape in string literal")
        escape = body[index]
        if escape in simple:
            decoded.append(simple[escape])
            index += 1
        elif escape in "01234567":
            end = index + 1
            while end < len(body) and end < index + 3 and body[end] in "01234567":
                end += 1
            decoded.append(chr(int(body[index:end], 8)))
            index = end
        elif escape == "x":
            end = index + 1
            while end < len(body) and body[end] in "0123456789abcdefABCDEF":
                end += 1
            if end == index + 1:
                raise ValueError("empty hexadecimal escape in string literal")
            decoded.append(chr(int(body[index + 1:end], 16)))
            index = end
        elif escape in "uU":
            digits = 4 if escape == "u" else 8
            end = index + 1 + digits
            decoded.append(chr(int(body[index + 1:end], 16)))
            index = end
        elif escape == "\n":
            index += 1
        elif escape == "\r" and index + 1 < len(body) and body[index + 1] == "\n":
            index += 2
        else:
            decoded.append(escape)
            index += 1
    return "".join(decoded)


def encode_literal(value: str, wide: bool) -> str:
    encoded: list[str] = []
    escapes = {
        "\\": "\\\\",
        '"': '\\"',
        "\n": "\\n",
        "\r": "\\r",
        "\t": "\\t",
        "\b": "\\b",
        "\f": "\\f",
        "\v": "\\v",
    }
    for char in value:
        if char in escapes:
            encoded.append(escapes[char])
        elif 32 <= ord(char) <= 126:
            encoded.append(char)
        elif ord(char) <= 0xFF:
            encoded.append(f"\\{ord(char):03o}")
        elif ord(char) <= 0xFFFF:
            encoded.append(f"\\u{ord(char):04X}")
        else:
            encoded.append(f"\\U{ord(char):08X}")
    return ("L" if wide else "") + '"' + "".join(encoded) + '"'


def string_id(kind: str, literal: str) -> str:
    digest = hashlib.sha256(f"{kind}:{literal}".encode("utf-8")).hexdigest()[:16]
    return "SV_" + digest.upper()


def raw_spans(text: str) -> list[tuple[int, int]]:
    """Intentional plaintext regions between sv-raw-begin/sv-raw-end markers.

    Used for literals the C language forces to stay constant (static array
    initializers, JNI registration tables). Everything else must be wrapped.
    """
    spans: list[tuple[int, int]] = []
    begin_tag = "sv-raw-begin"
    end_tag = "sv-raw-end"
    index = 0
    while True:
        begin = text.find(begin_tag, index)
        if begin < 0:
            break
        end = text.find(end_tag, begin + len(begin_tag))
        if end < 0:
            raise ValueError("sv-raw-begin without sv-raw-end")
        spans.append((begin, end + len(end_tag)))
        index = end + len(end_tag)
    return spans


def rewrite(text: str) -> tuple[str, int]:
    spans = protected_spans(text) + raw_spans(text)
    strings = [
        match
        for match in TOKEN.finditer(text)
        if match.lastgroup == "string"
        and not inside_spans(match.start(), spans)
        and not ignored_preprocessor_operand(text, match.start())
    ]
    groups: list[list[re.Match[str]]] = []
    for match in strings:
        if groups and only_space_or_comments(text[groups[-1][-1].end():match.start()]):
            groups[-1].append(match)
        else:
            groups.append([match])

    replacements: list[tuple[int, int, str]] = []
    for group in groups:
        tokens = [match.group() for match in group]
        wide = all(token.startswith("L") for token in tokens)
        if any(token.startswith("L") != wide for token in tokens):
            raise ValueError("cannot combine adjacent narrow and wide literals")
        literal = encode_literal("".join(parse_literal(token) for token in tokens), wide)
        kind = "WSTR" if wide else "STR"
        identifier = string_id(kind, literal)
        replacements.append(
            (group[0].start(), group[-1].end(), f"SV_{kind}({identifier}, {literal})")
        )

    for start, end, replacement in reversed(replacements):
        text = text[:start] + replacement + text[end:]
    return text, len(replacements)


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: wrap_native_strings.py [--check] SOURCE...", file=sys.stderr)
        return 2
    check_only = sys.argv[1] == "--check"
    names = sys.argv[2:] if check_only else sys.argv[1:]
    if not names:
        print("no source files supplied", file=sys.stderr)
        return 2
    total = 0
    for name in names:
        path = pathlib.Path(name)
        original = path.read_text(encoding="utf-8")
        rewritten, count = rewrite(original)
        if rewritten != original and not check_only:
            path.write_text(rewritten, encoding="utf-8", newline="\n")
        action = "unprotected" if check_only else "wrapped"
        print(f"{path}: {count} {action} string groups")
        total += count
    if check_only:
        if total:
            print(
                "run tools/wrap_native_strings.py on the listed sources",
                file=sys.stderr,
            )
            return 1
        print("all loader runtime strings are protected")
    else:
        print(f"wrapped {total} native string groups")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
