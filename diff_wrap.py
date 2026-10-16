import difflib

with open('tools/wrap_native_strings.py') as f1, open('wrap_native_strings_old.py') as f2:
    diff = difflib.unified_diff(f2.readlines(), f1.readlines(), fromfile='old', tofile='new')
    for line in diff:
        print(line, end='')