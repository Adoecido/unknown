import tempfile
import unittest
from pathlib import Path
from update_cb_mappings import read_matches, remap_descriptor, save_files, table, validate


def archive(name, field="x", desc="I", access=0):
    return {name: {"fields": [{"name": field, "desc": desc, "access": access}], "methods": []}}


class MappingImportTests(unittest.TestCase):
    def test_nested_arrays_and_method_descriptor(self):
        self.assertEqual(remap_descriptor("([[La;[I)La;", {"a": "IIl"}), "([[LIIl;[I)LIIl;")

    def test_exact_field(self):
        row = ("FD", "a", "x", "I", "IIl", "y", "I")
        self.assertEqual(validate({"a": "IIl"}, [row], archive("a"), archive("IIl", "y")), ([row], []))

    def test_stale_class_fails(self):
        with self.assertRaises(ValueError):
            validate({"a": "old"}, [], archive("a"), archive("IIl"))

    def test_stale_member_fails(self):
        with self.assertRaises(ValueError):
            validate({"a": "IIl"}, [("FD", "a", "x", "I", "IIl", "old", "I")], archive("a"), archive("IIl"))

    def test_signature_change_quarantined(self):
        row = ("FD", "a", "x", "I", "IIl", "y", "F")
        good, bad = validate({"a": "IIl"}, [row], archive("a"), archive("IIl", "y", "F"))
        self.assertFalse(good)
        self.assertEqual(len(bad), 1)

    def test_explicit_adapter_still_validates_target(self):
        row = ("FD", "a", "x", "I", "IIl", "y", "F")
        self.assertEqual(validate({"a": "IIl"}, [row], archive("a"), archive("IIl", "y", "F"), {row}), ([row], []))
        with self.assertRaises(ValueError):
            validate({"a": "IIl"}, [row], archive("a"), archive("IIl", "y", "I"), {row})

    def test_static_mismatch_quarantined(self):
        row = ("FD", "a", "x", "I", "IIl", "y", "I")
        self.assertEqual(len(validate({"a": "IIl"}, [row], archive("a"), archive("IIl", "y", access=8))[1]), 1)

    def test_duplicate_runtime_class_fails(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "matches.tsv"
            path.write_text("CL\ta\t\t\tIIl\t\t\nCL\tb\t\t\tIIl\t\t\n")
            with self.assertRaises(ValueError):
                read_matches(path)

    def test_conflicting_member_fails(self):
        source = archive("a")
        source["a"]["fields"].append({"name": "z", "desc": "I", "access": 0})
        rows = [("FD", "a", n, "I", "IIl", "y", "I") for n in ("x", "z")]
        with self.assertRaises(ValueError):
            validate({"a": "IIl"}, rows, source, archive("IIl", "y"))

    def test_deterministic_tables(self):
        self.assertEqual(table("test", [("b", "2"), ("a", "1"), ("a", "1")]), "# test\na\t1\nb\t2\n")

    def test_backups_and_idempotence(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            save_files(root, {"map.tsv": "old"}, root / "backup")
            save_files(root, {"map.tsv": "new"}, root / "backup")
            save_files(root, {"map.tsv": "new"}, root / "backup")
            backups = list((root / "backup").glob("*/map.tsv"))
            self.assertEqual(len(backups), 1)
            self.assertEqual(backups[0].read_text(), "old")
            self.assertEqual((root / "map.tsv").read_text(), "new")


if __name__ == "__main__":
    unittest.main()
