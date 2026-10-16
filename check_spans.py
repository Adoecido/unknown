import re
import pathlib

text = pathlib.Path('native/native_bridge.c').read_text(encoding='utf-8')

# Find spans
spans = []
begin_tag = "sv-raw-begin"
end_tag = "sv-raw-end"
index = 0
while True:
    begin = text.find("sv-raw-begin", index)
    if begin < 0:
        break
    end = text.find("sv-raw-end", begin + len("sv-raw-begin"))
    if end < 0:
        raise ValueError("sv-raw-begin without sv-raw-end")
    spans.append((begin, end + len("sv-raw-end")))
    index = end + len("sv-raw-end")

print(f"Raw spans: {spans}")

# Check line 1048 area
lines = text.split('\n')
for i in range(1040, 1060):
    if i < len(lines):
        print(f"Line {i+1}: {lines[i][:100]}")

# Check if the strings at line 1048 are inside any span
import re
TOKEN = re.compile(r'(?:L|u|U)?"(?:\\.|[^"\\])*"')
for m in re.finditer(r'(?:L|u|U)?"(?:\\.|[^"\\])*"', text):
    in_span = any(s <= m.start() < e for s, e in spans)
    if not in_span:
        line = text.count('\n', 0, m.start()) + 1
        print(f'Line {line}: UNPROTECTED: {m.group()[:80]}')