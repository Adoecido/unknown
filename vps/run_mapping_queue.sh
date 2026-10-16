#!/usr/bin/env bash
set -uo pipefail

incoming=/var/lib/sonyvegas/mappings/incoming
state=/var/lib/sonyvegas/mappings/state.json
archive=/var/lib/sonyvegas/mappings/archive
logs=/var/lib/sonyvegas/mappings/logs
mkdir -p "$archive"
mkdir -p "$logs"

shopt -s nullglob
for patch in "$incoming"/*.patch; do
  hash="$(basename "$patch" .patch)"
  log="$logs/$hash.log"
  printf '{"sha256":"%s","state":"processing","error":null,"updatedAt":"%s"}\n' "$hash" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$state"
  if timeout --signal=TERM --kill-after=15s 45m /usr/bin/python3 /opt/sonyvegas/process_cb_mappings.py "$patch" "$hash" >"$log" 2>&1; then
    printf '{"sha256":"%s","state":"ready","error":null,"updatedAt":"%s"}\n' "$hash" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "$state"
    mv -f "$patch" "$archive/$hash.patch"
  else
    detail="$(tail -n 25 "$log" | tr '\n' ' ' | tr -cd '[:print:]' | cut -c1-700)"
    error="mapping_worker_failed${detail:+: $detail}"
    /usr/bin/python3 - "$state" "$hash" "$error" <<'PY'
import json
import sys
from datetime import datetime, timezone

path, patch_hash, error = sys.argv[1:]
with open(path, "w", encoding="utf-8") as stream:
    json.dump({"sha256": patch_hash, "state": "failed", "error": error,
               "updatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")}, stream,
              separators=(",", ":"))
    stream.write("\n")
PY
    mv -f "$patch" "$archive/$hash.failed.patch"
  fi
done
