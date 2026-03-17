#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

PLAN_PATH="${1:-$ROOT_DIR/release-artifacts/release-plan.json}"
MANIFEST_PATH="${2:-$ROOT_DIR/releases/manifest.json}"
DRY_RUN="${APPLY_RELEASE_PLAN_DRY_RUN:-false}"

if [ ! -f "$PLAN_PATH" ]; then
  echo "Release plan not found: $PLAN_PATH" >&2
  exit 1
fi
if [ ! -f "$MANIFEST_PATH" ]; then
  echo "Manifest not found: $MANIFEST_PATH" >&2
  exit 1
fi

planned_version() {
  local module="$1"
  local in_plan
  in_plan="$(jq --arg module "$module" -r '.modules[] | select(.name == $module) | if .releaseLevel == "none" then .currentVersion else .nextVersion end' "$PLAN_PATH" | head -n 1)"
  if [ -n "${in_plan:-}" ]; then
    printf '%s\n' "$in_plan"
    return 0
  fi
  jq --arg module "$module" -r '.modules[] | select(.name == $module) | .currentVersion' "$MANIFEST_PATH" | head -n 1
}

set_project_version() {
  local pom="$1"
  local artifact="$2"
  local new_version="$3"
  if [ "$DRY_RUN" = "true" ]; then
    echo "[dry-run] set project version in $pom ($artifact -> $new_version)"
    return 0
  fi
  perl -0777 -i -pe "s{(<artifactId>\\Q$artifact\\E</artifactId>\\s*<version>)[^<]+(</version>)}{\$1$new_version\$2}s" "$pom"
}

set_parent_version() {
  local pom="$1"
  local new_version="$2"
  if [ "$DRY_RUN" = "true" ]; then
    echo "[dry-run] set parent version in $pom (ether-parent -> $new_version)"
    return 0
  fi
  perl -0777 -i -pe "s{(<parent>\\s*<groupId>dev\\.rafex\\.ether\\.parent</groupId>\\s*<artifactId>ether-parent</artifactId>\\s*<version>)[^<]+(</version>)}{\$1$new_version\$2}s" "$pom"
}

set_property_version() {
  local pom="$1"
  local property="$2"
  local new_version="$3"
  if [ "$DRY_RUN" = "true" ]; then
    echo "[dry-run] set property in $pom ($property -> $new_version)"
    return 0
  fi
  perl -0777 -i -pe "s{(<\\Q$property\\E>)[^<]+(</\\Q$property\\E>)}{\$1$new_version\$2}g" "$pom"
}

parent_version="$(planned_version "ether-parent")"

while IFS= read -r module; do
  name="$(printf '%s' "$module" | jq -r '.name')"
  artifact_id="$(printf '%s' "$module" | jq -r '.artifactId')"
  pom_rel="$(printf '%s' "$module" | jq -r '.pomPath')"
  uses_parent="$(printf '%s' "$module" | jq -r '.usesParent // false')"
  release_level="$(jq --arg module "$name" -r '.modules[] | select(.name == $module) | .releaseLevel // "none"' "$PLAN_PATH" | head -n 1)"
  module_version="$(planned_version "$name")"

  pom_path="$ROOT_DIR/$pom_rel"
  if [ ! -f "$pom_path" ]; then
    echo "Skipping $name (pom not found: $pom_path)"
    continue
  fi

  if [ "$release_level" != "none" ]; then
    set_project_version "$pom_path" "$artifact_id" "$module_version"
  fi

  if [ "$uses_parent" = "true" ]; then
    set_parent_version "$pom_path" "$parent_version"
  fi

  while IFS= read -r dep_entry; do
    dep_name="$(printf '%s' "$dep_entry" | cut -d'=' -f1)"
    dep_property="$(printf '%s' "$dep_entry" | cut -d'=' -f2)"
    dep_version="$(planned_version "$dep_name")"
    set_property_version "$pom_path" "$dep_property" "$dep_version"
  done < <(printf '%s' "$module" | jq -r '.internalDependencyProperties // {} | to_entries[] | "\(.key)=\(.value)"')
done < <(jq -c '.modules[]' "$MANIFEST_PATH")

echo "Applied release plan versions to pom files:"
echo "  - plan: $PLAN_PATH"
echo "  - manifest: $MANIFEST_PATH"
