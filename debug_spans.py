import re

with open('native/native_bridge.c', 'r', encoding='utf-8') as f:
    text = f.read()

# Find raw spans
spans = []
begin_tag = "sv-raw-begin"
end_tag = "sv-raw-end"
index = 0
while True:
    begin = text.find("sv-raw-begin", index)
    if begin < 0:
        break
    end = text.find(end_tag, begin + len("sv-raw-end"))
    if end < 0:
        raise ValueError("sv-raw-begin without sv-raw-end")
    spans.append((begin, end + len(end_tag)))
    index = end + len("sv-raw-end")

print("Raw spans:", spans)

# Check protected spans
PROTECTED_START = re.compile(r"SV_(?:W?STR)\s*\(")
spans_all = []
for m in re.finditer(r"SV_(?:W?STR)\s*\(", text):
    depth = 1
    index = m.end()
    while index < len(text) and depth:
        if text[index] == "(":
            depth += 1
        elif text[index] == ")":
            depth -= 1
        index += 1
    if depth == 0:
        spans.append((m.start(), index))

all_spans = spans + [(m.start(), i) for m in re.finditer(r"SV_(?:W?STR)\s*\(", text) for i in [m.end()] if False]  # placeholder

# Actually combine properly
all_spans = []
for m in re.finditer(r"SV_(?:W?STR)\s*\(", text):
    depth = 1
    index = m.end()
    while index < len(text) and depth:
        if text[index] == "(":
            depth += 1
        elif text[index] == ")":
            depth -= 1
        index += 1
    if depth == 0:
        all_spans.append((m.start(), index))

# Add raw spans
all_spans.extend(spans)

# Check specific strings at line 1046-1050 area
lines = open('native/native_bridge.c', encoding='utf-8').readlines()
for i in range(1040, 1060):
    if i < len(lines):
        print(f"{i+1}: {lines[i].rstrip()}")

# Check if strings at line 1046-1050 are inside any span
TOKEN = re.compile(r'(?:L|u|U)?"(?:\\.|[^"\\])*"')
for m in re.finditer(r'(?:L|u|U)?"(?:\\.|[^"\\])*"', text):
    in_span = any(s <= m.start() < e for s, e in spans)
    if not in_span:
        line = text.count('\n', 0, m.start()) + 1
        print(f'Line {line}: UNPROTECTED: {m.group()[:100]}')