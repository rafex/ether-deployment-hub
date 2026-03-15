#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=./release-common.sh
source "$ROOT_DIR/scripts/release-common.sh"

MANIFEST_PATH="${RELEASE_MANIFEST_PATH:-$(release_manifest_path)}"
BASE_REF="${1:-$(release_default_base_ref)}"
HEAD_REF="${2:-${RELEASE_HEAD_REF:-HEAD}}"

if [ ! -f "$MANIFEST_PATH" ]; then
  echo "Manifest not found: $MANIFEST_PATH" >&2
  exit 1
fi

CHANGED_FILES=()
while IFS= read -r file; do
  CHANGED_FILES+=("$file")
done < <(git -C "$ROOT_DIR" diff --name-only "$BASE_REF" "$HEAD_REF" --)
TMP_MODULES="$(mktemp)"
TMP_MATCHED="$(mktemp)"
trap 'rm -f "$TMP_MODULES" "$TMP_MATCHED"' EXIT
: > "$TMP_MODULES"
: > "$TMP_MATCHED"

while IFS= read -r module_json; do
  name="$(printf '%s' "$module_json" | jq -r '.name')"
  path="$(printf '%s' "$module_json" | jq -r '.path')"

  module_files=()
  for file in "${CHANGED_FILES[@]:-}"; do
    if [ "$file" = "$path" ] || [[ "$file" == "$path/"* ]]; then
      module_files+=("$file")
      printf '%s\n' "$file" >> "$TMP_MATCHED"
    fi
  done

  jq -n \
    --arg name "$name" \
    --arg path "$path" \
    --argjson changed "$( [ "${#module_files[@]}" -gt 0 ] && echo true || echo false )" \
    --argjson changedFiles "$(printf '%s\n' "${module_files[@]:-}" | jq -R . | jq -s 'map(select(length > 0))')" \
    '{
      name: $name,
      path: $path,
      changed: $changed,
      changedFiles: $changedFiles
    }' >> "$TMP_MODULES"
done < <(jq -c '.modules[]' "$MANIFEST_PATH")

shared_files_json="$(
  if [ "${#CHANGED_FILES[@]}" -eq 0 ]; then
    printf '[]\n'
  else
    printf '%s\n' "${CHANGED_FILES[@]}" | sort -u > "${TMP_MATCHED}.all"
    sort -u "$TMP_MATCHED" > "${TMP_MATCHED}.uniq"
    comm -23 "${TMP_MATCHED}.all" "${TMP_MATCHED}.uniq" | jq -R . | jq -s 'map(select(length > 0))'
  fi
)"

modules_json="$(jq -s '.' "$TMP_MODULES")"
changed_files_json="$(printf '%s\n' "${CHANGED_FILES[@]:-}" | jq -R . | jq -s 'map(select(length > 0))')"

jq -n \
  --arg baseRef "$BASE_REF" \
  --arg headRef "$HEAD_REF" \
  --argjson changedFiles "$changed_files_json" \
  --argjson sharedFiles "$shared_files_json" \
  --argjson modules "$modules_json" \
  '{
    baseRef: $baseRef,
    headRef: $headRef,
    changedFiles: $changedFiles,
    sharedFiles: $sharedFiles,
    modules: $modules
  }'
