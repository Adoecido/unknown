"""Install a raw cb_mappings archive dump while preserving content-addressed backups."""
import argparse
import hashlib
import shutil
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--destination", type=Path, required=True)
    args = parser.parse_args()
    names = ("classes.json", "classes.txt", "strings_index.txt", "summary.txt")
    backup_root = args.destination / ".mapping-backups"
    for name in names:
        source, target = args.source / name, args.destination / name
        if not source.is_file():
            raise ValueError("Archive dump is missing " + name)
        if target.exists() and target.read_bytes() != source.read_bytes():
            digest = hashlib.sha256(target.read_bytes()).hexdigest()[:16]
            backup = backup_root / digest / name
            backup.parent.mkdir(parents=True, exist_ok=True)
            if not backup.exists():
                shutil.copy2(target, backup)
        shutil.copy2(source, target)
    print("Imported raw dump files:", len(names))


if __name__ == "__main__":
    main()
