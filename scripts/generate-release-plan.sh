#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=./release-common.sh
source "$ROOT_DIR/scripts/release-common.sh"

MANIFEST_PATH="${RELEASE_MANIFEST_PATH:-$(release_manifest_path)}"
OUTPUT_DIR="${RELEASE_OUTPUT_DIR:-$ROOT_DIR/release-artifacts}"
BASE_REF="${1:-$(release_default_base_ref)}"
HEAD_REF="${2:-${RELEASE_HEAD_REF:-HEAD}}"

mkdir -p "$OUTPUT_DIR/changelogs"

DETECTION_JSON="$("$ROOT_DIR/scripts/detect-changed-modules.sh" "$BASE_REF" "$HEAD_REF")"
TMP_PLAN_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_PLAN_DIR"' EXIT

jq -c '.modules[]' "$MANIFEST_PATH" | while IFS= read -r module_json; do
  name="$(printf '%s' "$module_json" | jq -r '.name')"
  path="$(printf '%s' "$module_json" | jq -r '.path')"
  current_version="$(printf '%s' "$module_json" | jq -r '.currentVersion')"
  published="$(printf '%s' "$module_json" | jq -r 'if has("published") then (.published | tostring) else "true" end')"
  dependency_impact="$(printf '%s' "$module_json" | jq -r '.dependencyReleaseImpact // "patch"')"
  direct_files_json="$(printf '%s' "$DETECTION_JSON" | jq --arg name "$name" -c '.modules[] | select(.name == $name) | .changedFiles')"
  direct_changed="$(printf '%s' "$DETECTION_JSON" | jq --arg name "$name" -r '.modules[] | select(.name == $name) | .changed')"

  level="none"
  reasons=()
  commits_json='[]'

  if [ "$published" != "true" ]; then
    level="patch"
    reasons+=("initial_publish_pending")
  fi

  if [ "$direct_changed" = "true" ]; then
    docs_only=true
    build_only=true
    while IFS= read -r file; do
      [ -n "$file" ] || continue
      if ! release_is_docs_or_meta_file "$file"; then
        docs_only=false
      fi
      if ! release_is_build_file "$file"; then
        build_only=false
      fi
    done < <(printf '%s' "$direct_files_json" | jq -r '.[]')

    if [ "$docs_only" = "true" ]; then
      if [ "$published" = "true" ]; then
        level="none"
      fi
      reasons+=("docs_or_tests_only")
    elif [ "$build_only" = "true" ]; then
      level="$(release_max_level "$level" "patch")"
      reasons+=("build_metadata_changed")
    else
      commit_log="$(git -C "$ROOT_DIR" log --format='%s%n%b' "$BASE_REF".."$HEAD_REF" -- "$path")"
      level="$(release_max_level "$level" "$(release_commit_level_for_log "$commit_log")")"
      reasons+=("direct_changes")
      if [ "$level" = "major" ]; then
        reasons+=("breaking_change_detected")
      elif [ "$level" = "minor" ]; then
        reasons+=("feature_detected")
      else
        reasons+=("compatible_fix_or_refactor")
      fi
    fi

    commits_json="$(
      git -C "$ROOT_DIR" log --format='%H%x1f%s%x1f%b%x1e' "$BASE_REF".."$HEAD_REF" -- "$path" \
        | jq -R -s '
            split("\u001e")
            | map(gsub("^\n+|\n+$"; ""))
            | map(select(length > 0))
            | map(split("\u001f"))
            | map({
                sha: .[0],
                shortSha: (.[0][0:7]),
                subject: (.[1] // ""),
                body: (.[2] // "")
              })
            | map(select(.sha | test("^[0-9a-f]{7,40}$")))
          '
    )"
  fi

  if [ "$published" != "true" ] && [ "$level" != "none" ]; then
    next_version="$current_version"
  else
    next_version="$(release_bump_version "$current_version" "$level")"
  fi
  jq -n \
    --arg name "$name" \
    --arg path "$path" \
    --arg currentVersion "$current_version" \
    --arg nextVersion "$next_version" \
    --arg level "$level" \
    --arg publishedRaw "$published" \
    --arg dependencyImpact "$dependency_impact" \
    --argjson directChanged "$direct_changed" \
    --argjson directChangedFiles "$direct_files_json" \
    --argjson dependencies "$(printf '%s' "$module_json" | jq -c '.dependencies // []')" \
    --argjson reasons "$(printf '%s\n' "${reasons[@]:-}" | jq -R . | jq -s 'map(select(length > 0))')" \
    --argjson commits "$commits_json" \
    '{
      name: $name,
      path: $path,
      currentVersion: $currentVersion,
      nextVersion: $nextVersion,
      published: ($publishedRaw == "true"),
      releaseLevel: $level,
      directChanged: $directChanged,
      directChangedFiles: $directChangedFiles,
      dependencies: $dependencies,
      dependencyReleaseImpact: $dependencyImpact,
      reasons: $reasons,
      commits: $commits
    }' > "$TMP_PLAN_DIR/$name.json"
done

changed=true
while [ "$changed" = "true" ]; do
  changed=false
  while IFS= read -r module_file; do
    name="$(jq -r '.name' "$module_file")"
    current_level="$(jq -r '.releaseLevel' "$module_file")"
    dependency_impact="$(jq -r '.dependencyReleaseImpact' "$module_file")"
    dependencies=()
    while IFS= read -r dependency; do
      dependencies+=("$dependency")
    done < <(jq -r '.dependencies[]?' "$module_file")

    for dependency in "${dependencies[@]:-}"; do
      dependency_file="$TMP_PLAN_DIR/$dependency.json"
      [ -f "$dependency_file" ] || continue
      dependency_level="$(jq -r '.releaseLevel' "$dependency_file")"
      if [ "$(release_level_rank "$dependency_level")" -eq 0 ]; then
        continue
      fi
      propagated_level="$dependency_impact"
      target_level="$(release_max_level "$current_level" "$propagated_level")"
      if [ "$target_level" != "$current_level" ]; then
        current_level="$target_level"
        if [ "$(jq -r '.published' "$module_file")" != "true" ]; then
          next_version="$(jq -r '.currentVersion' "$module_file")"
        else
          next_version="$(release_bump_version "$(jq -r '.currentVersion' "$module_file")" "$target_level")"
        fi
        jq \
          --arg level "$target_level" \
          --arg nextVersion "$next_version" \
          --arg reason "dependency_update:$dependency" \
          '.releaseLevel = $level
           | .nextVersion = $nextVersion
           | .reasons = ((.reasons + [$reason]) | unique)' "$module_file" > "${module_file}.next"
        mv "${module_file}.next" "$module_file"
        changed=true
      else
        jq \
          --arg reason "dependency_update:$dependency" \
          '.reasons = ((.reasons + [$reason]) | unique)' "$module_file" > "${module_file}.next"
        mv "${module_file}.next" "$module_file"
      fi
    done
  done < <(find "$TMP_PLAN_DIR" -name '*.json' | sort)
done

modules_json="$(jq -s 'sort_by(.name)' "$TMP_PLAN_DIR"/*.json)"
selected_count="$(printf '%s' "$modules_json" | jq '[.[] | select(.releaseLevel != "none")] | length')"
selected_names_json="$(printf '%s' "$modules_json" | jq '[.[] | select(.releaseLevel != "none") | .name]')"
deploy_order='[]'
preferred_deploy_order="$(jq -c '.deployOrder // [.modules[].name]' "$MANIFEST_PATH")"

if [ "$selected_count" -gt 0 ]; then
  unresolved_names="$(printf '%s' "$modules_json" | jq -r '[.[] | select(.releaseLevel != "none") | .name]')"
  ordered_names='[]'
  while [ "$(printf '%s' "$unresolved_names" | jq 'length')" -gt 0 ]; do
    ready_now="$(printf '%s' "$modules_json" | jq -r \
      --argjson unresolved "$unresolved_names" \
      --argjson ordered "$ordered_names" '
      [
        .[]
        | select(.name as $n | (($unresolved | index($n)) != null))
        | select(
            [.dependencies[]? as $d | select(($unresolved | index($d)) != null) | $d]
            | length == 0
          )
        | .name
      ]')"

    if [ "$(printf '%s' "$ready_now" | jq 'length')" -eq 0 ]; then
      echo "Unable to compute dependency order for selected modules" >&2
      echo "Remaining modules: $unresolved_names" >&2
      exit 1
    fi
    ready_now="$(jq -cn \
      --argjson ready "$ready_now" \
      --argjson preferred "$preferred_deploy_order" \
      '([ $preferred[] as $name | select(($ready | index($name)) != null) | $name ]
        + [ $ready[] as $name | select(($preferred | index($name)) == null) | $name ])')"

    ordered_names="$(jq -cn --argjson left "$ordered_names" --argjson right "$ready_now" '$left + $right')"
    unresolved_names="$(jq -cn --argjson unresolved "$unresolved_names" --argjson ready "$ready_now" '
      [ $unresolved[] as $u | select($ready | index($u) | not) | $u ]')"
  done
  deploy_order="$(jq -cn \
    --argjson ordered "$ordered_names" \
    --argjson selected "$selected_names_json" \
    '[ $ordered[] as $name | select(($selected | index($name)) != null) | $name ]')"
fi

plan_json="$(jq -n \
  --arg baseRef "$BASE_REF" \
  --arg headRef "$HEAD_REF" \
  --argjson selectedCount "$selected_count" \
  --argjson deployOrder "$deploy_order" \
  --argjson changes "$DETECTION_JSON" \
  --argjson modules "$modules_json" \
  '{
    baseRef: $baseRef,
    headRef: $headRef,
    selectedCount: $selectedCount,
    deployOrder: $deployOrder,
    changes: $changes,
    modules: $modules
  }')"

printf '%s\n' "$plan_json" > "$OUTPUT_DIR/release-plan.json"

{
  echo "# Release Plan"
  echo
  echo "- Base ref: \`$BASE_REF\`"
  echo "- Head ref: \`$HEAD_REF\`"
  echo "- Modules selected: \`$selected_count\`"
  echo "- Deploy order: \`$(printf '%s' "$plan_json" | jq -r '.deployOrder | join(", ")')\`"
  echo
  echo "| Module | Current | Next | Level | Reasons |"
  echo "|---|---|---|---|---|"
  printf '%s' "$plan_json" | jq -r '
    .modules[] |
    "| \(.name) | \(.currentVersion) | " +
    (if .releaseLevel == "none" then "-" else .nextVersion end) +
    " | \(.releaseLevel) | " +
    (if (.reasons | length) == 0 then "none" else (.reasons | join(", ")) end) + " |"
  '
} > "$OUTPUT_DIR/release-plan.md"

printf '%s' "$plan_json" | jq -c '.modules[]' | while IFS= read -r module; do
  name="$(printf '%s' "$module" | jq -r '.name')"
  current_version="$(printf '%s' "$module" | jq -r '.currentVersion')"
  next_version="$(printf '%s' "$module" | jq -r '.nextVersion')"
  release_level="$(printf '%s' "$module" | jq -r '.releaseLevel')"
  {
    echo "# $name"
    echo
    echo "- Current version: \`$current_version\`"
    echo "- Planned version: \`$( [ "$release_level" = "none" ] && printf '%s' "$current_version" || printf '%s' "$next_version" )\`"
    echo "- Release level: \`$release_level\`"
    echo "- Reasons: $(printf '%s' "$module" | jq -r 'if (.reasons | length) == 0 then "none" else (.reasons | join(", ")) end')"
    echo
    echo "## Changed Files"
    printf '%s' "$module" | jq -r '
      if (.directChangedFiles | length) == 0 then
        "- none"
      else
        .directChangedFiles[] | "- `\(.)`"
      end
    '
    echo
    echo "## Commits"
    printf '%s' "$module" | jq -r '
      if (.commits | length) == 0 then
        "- none"
      else
        .commits[] | "- `\(.shortSha)` \(.subject)"
      end
    '
  } > "$OUTPUT_DIR/changelogs/$name.md"
done

echo "Generated release plan:"
echo "  - $OUTPUT_DIR/release-plan.json"
echo "  - $OUTPUT_DIR/release-plan.md"
echo "  - $OUTPUT_DIR/changelogs/"
