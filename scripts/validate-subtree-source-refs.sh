#!/usr/bin/env bash
set -euo pipefail

CURRENT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
SUPERPROJECT_ROOT="$(git -C "$CURRENT_ROOT" rev-parse --show-superproject-working-tree 2>/dev/null || true)"

if [ -n "$SUPERPROJECT_ROOT" ]; then
  REPO_ROOT="$(cd "$SUPERPROJECT_ROOT" && pwd)"
else
  REPO_ROOT="$CURRENT_ROOT"
fi

MANIFEST_PATH="${1:-$REPO_ROOT/releases/subtrees.json}"

if [ ! -f "$MANIFEST_PATH" ]; then
  echo "No subtree source manifest found at $MANIFEST_PATH. Skipping subtree source ref validation."
  exit 0
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required to validate subtree source refs." >&2
  exit 1
fi

echo "Validating subtree source refs from $MANIFEST_PATH"

failures=0
TMP_GIT_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_GIT_DIR"' EXIT

while IFS= read -r module_json; do
  [ -n "$module_json" ] || continue

  name="$(printf '%s' "$module_json" | jq -r '.name')"
  prefix="$(printf '%s' "$module_json" | jq -r '.prefix')"
  url="$(printf '%s' "$module_json" | jq -r '.url')"
  sha="$(printf '%s' "$module_json" | jq -r '.importedSha')"

  fetch_url="$url"
  case "$fetch_url" in
    git@github.com:*)
      fetch_url="https://github.com/${fetch_url#git@github.com:}"
      ;;
  esac

  if [ -z "$sha" ] || [ "$sha" = "null" ]; then
    echo "[FAIL] $name ($prefix) has no importedSha"
    failures=$((failures + 1))
    continue
  fi

  rm -rf "$TMP_GIT_DIR"/*
  git -C "$TMP_GIT_DIR" init --bare >/dev/null 2>&1
  if git -C "$TMP_GIT_DIR" fetch --depth=1 "$fetch_url" "$sha" >/dev/null 2>&1; then
    echo "[OK]   $name ($prefix) -> $sha"
  else
    echo "[FAIL] $name ($prefix) -> $sha is not available in remote $url"
    failures=$((failures + 1))
  fi
done < <(jq -c '.modules[]' "$MANIFEST_PATH")

if [ "$failures" -gt 0 ]; then
  echo "Subtree source ref validation failed: $failures missing ref(s)."
  exit 1
fi

echo "All subtree source refs are available in their remotes."
