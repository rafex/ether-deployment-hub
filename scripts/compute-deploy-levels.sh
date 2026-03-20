#!/usr/bin/env bash
# compute-deploy-levels.sh — Compute parallel deploy levels from a release plan.
#
# Reads release-plan.json and groups modules into topological levels where
# modules at the same level have no inter-dependencies and can be deployed
# in parallel.
#
# Usage: ./compute-deploy-levels.sh <release-plan.json>
#
# Output (stdout): JSON of the form:
#   {
#     "levels": [
#       ["ether-parent"],
#       ["ether-config", "ether-json", ...],
#       ...
#     ]
#   }
#
# Exit codes:
#   0 — success
#   1 — bad arguments or plan file not found

set -euo pipefail

PLAN_PATH="${1:?Usage: $0 <release-plan.json>}"

if [ ! -f "$PLAN_PATH" ]; then
  echo "Release plan not found: $PLAN_PATH" >&2
  exit 1
fi

python3 - "$PLAN_PATH" <<'PYEOF'
import json
import sys

with open(sys.argv[1]) as f:
    plan = json.load(f)

# Only modules that are actually being released
releasable = [
    m for m in plan.get("modules", [])
    if m.get("releaseLevel", "none") != "none"
]

release_names = {m["name"] for m in releasable}

# Dependency map: only ether-* deps that are also in the release set
dep_map: dict[str, list[str]] = {
    m["name"]: [
        d for d in (m.get("dependencies") or [])
        if d in release_names
    ]
    for m in releasable
}

# Kahn's algorithm — assign each module to a level
levels: list[list[str]] = []
assigned: dict[str, int] = {}
remaining = set(dep_map.keys())

while remaining:
    # Modules whose all deps are already assigned
    current_level = sorted(
        name for name in remaining
        if all(d in assigned for d in dep_map[name])
    )
    if not current_level:
        # Safety valve: cycle or unresolvable — dump all remaining
        current_level = sorted(remaining)

    level_idx = len(levels)
    for name in current_level:
        assigned[name] = level_idx

    levels.append(current_level)
    remaining -= set(current_level)

print(json.dumps({"levels": levels}, indent=2))
PYEOF
