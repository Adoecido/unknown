#!/usr/bin/env python3
"""Generate CheatBreaker 1.8.9 forward member tables (source name -> allatori name).

The reverse tables (fields.tsv / methods.tsv) map allatori owner/member -> srg
name, which the runtime uses for reporting. The forward direction is needed to
resolve MCP member names (hardcoded in the M* mapping modules) to their Allatori
runtime names.

Inputs:
  * src/main/resources/mappings/cheatbreaker189/{fields,methods}.tsv
        (allatori owner/member -> srgOrNotch -> notch)
  * mc_ref/mcp/{fields,methods}.csv   (MCP 1.8.9 srg -> name tables)

Outputs:
  * src/main/resources/mappings/cheatbreaker189/fields_fwd.tsv
  * src/main/resources/mappings/cheatbreaker189/methods_fwd.tsv
        rows: allatoriOwnerInternal<TAB>sourceName<TAB>allatoriMemberName
        Every member emits one row per known source key (MCP name and srg name).

Usage:
  py tools/generate_cb_member_forward.py [--sonyvegas DIR] [--mc-ref DIR]
"""
import argparse
import csv
from pathlib import Path

DEFAULT_SONYVEGAS = Path(__file__).resolve().parents[1]
DEFAULT_MC_REF = Path(r"D:\sources\tampinhas\cb_mappings\mc_ref")


def load_mcp_names(csv_path):
    """Read MCP srg->name table (searge,name,side,desc)."""
    table = {}
    with open(csv_path, newline="", encoding="utf-8-sig") as handle:
        for row in csv.DictReader(handle):
            searge = row.get("searge", "").strip()
            name = row.get("name", "").strip()
            if searge and name:
                table[searge] = name
    return table


def emit_forward(tsv_path, mcp_table, out_path, strip_descriptor=False):
    """Read reverse TSV, write forward table keyed by mcp and srg names.

    Method rows keep the Allatori descriptor as an extra column so overloads
    that share the same source name (e.g. isPotionActive(Potion) vs
    isPotionActive(int)) can be disambiguated at lookup time.
    """
    rows = []
    with open(tsv_path, encoding="utf-8-sig") as handle:
        for raw in handle:
            line = raw.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t")
            if len(parts) < 2 or "/" not in parts[0]:
                continue
            owner_internal, allatori_member = parts[0].split("/", 1)
            descriptor = ""
            if strip_descriptor:
                if " " in allatori_member:
                    allatori_member, descriptor = allatori_member.split(" ", 1)
                else:
                    allatori_member = allatori_member.split(" ", 1)[0]
            srg = parts[1].strip()
            if not owner_internal or not allatori_member or not srg:
                continue
            keys = [srg]
            mcp_name = mcp_table.get(srg)
            if mcp_name:
                keys.append(mcp_name)
            for key in dict.fromkeys(keys):
                rows.append((owner_internal, key, allatori_member, descriptor))
    rows = sorted(set(rows))
    with open(out_path, "w", encoding="utf-8", newline="") as handle:
        if strip_descriptor:
            handle.write(
                "# allatoriOwnerInternal<TAB>sourceName<TAB>allatoriMemberName<TAB>allatoriDescriptor\n")
        else:
            handle.write(
                "# allatoriOwnerInternal<TAB>sourceName<TAB>allatoriMemberName\n")
        for owner, key, member, descriptor in rows:
            if strip_descriptor:
                handle.write("%s\t%s\t%s\t%s\n" % (owner, key, member, descriptor))
            else:
                handle.write("%s\t%s\t%s\n" % (owner, key, member))
    return len(rows)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sonyvegas", default=str(DEFAULT_SONYVEGAS))
    parser.add_argument("--mc-ref", default=str(DEFAULT_MC_REF))
    args = parser.parse_args()

    cb_dir = Path(args.sonyvegas) / "src/main/resources/mappings/cheatbreaker189"
    mc_ref = Path(args.mc_ref)

    mcp_fields = load_mcp_names(mc_ref / "mcp/fields.csv")
    mcp_methods = load_mcp_names(mc_ref / "mcp/methods.csv")
    print("MCP names: fields=%d methods=%d" % (len(mcp_fields), len(mcp_methods)))

    field_rows = emit_forward(cb_dir / "fields.tsv", mcp_fields, cb_dir / "fields_fwd.tsv")
    method_rows = emit_forward(cb_dir / "methods.tsv", mcp_methods, cb_dir / "methods_fwd.tsv", strip_descriptor=True)
    print("forward rows: fields=%d methods=%d" % (field_rows, method_rows))


if __name__ == "__main__":
    main()
