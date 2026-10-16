import re, pathlib

text = pathlib.Path('native/loader_bootstrap.c').read_text(encoding='utf-8')
spans = []
for m in re.finditer(r'SV_(?:W?STR)\s*\(', text):
    depth = 1
    i = m.end()
    while i < len(text) and depth:
        if text[i] == '(':
            depth += 1
        elif text[i] == ')':
            depth -= 1
        i += 1
    spans.append((m.start(), i))

TOKEN = re.compile(r'(?P<string>(?:L|u|U)?"(?:\\.|[^"\\])*")')

for m in re.finditer(r'(?:L|u|U)?"(?:\\.|[^"\\])*"', text):
    if not any(s <= m.start() < e for s, e in spans):
        if not re.search(r'SV_(?:W?STR)\s*\(', text[max(0,m.start()-20):m.start()]):
            line = text.count('\n', 0, m.start()) + 1
            print(f'Line {line}: {m.group()[:100]}')