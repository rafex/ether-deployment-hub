#!/usr/bin/env bash
set -euo pipefail

CURRENT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
SUPERPROJECT_ROOT="$(git -C "$CURRENT_ROOT" rev-parse --show-superproject-working-tree 2>/dev/null || true)"

if [ -n "$SUPERPROJECT_ROOT" ]; then
  REPO_ROOT="$(cd "$SUPERPROJECT_ROOT" && pwd)"
else
  REPO_ROOT="$CURRENT_ROOT"
fi

if [ ! -f "$REPO_ROOT/.gitmodules" ]; then
  echo "No .gitmodules found at $REPO_ROOT. Skipping submodule remote ref validation."
  exit 0
fi

echo "Validating published submodule refs from $REPO_ROOT"

submodule_entries="$(
  git -C "$REPO_ROOT" config -f .gitmodules --get-regexp '^submodule\..*\.path$' || true
)"

if [ -z "$submodule_entries" ]; then
  echo "No submodules declared in .gitmodules. Skipping validation."
  exit 0
fi

failures=0
TMP_GIT_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_GIT_DIR"' EXIT

while IFS= read -r entry; do
  [ -n "$entry" ] || continue
  key="${entry%% *}"
  path="${entry#* }"
  name="${key#submodule.}"
  name="${name%.path}"
  url="$(git -C "$REPO_ROOT" config -f .gitmodules --get "submodule.${name}.url")"
  fetch_url="${url}"
  case "$fetch_url" in
    git@github.com:*)
      fetch_url="https://github.com/${fetch_url#git@github.com:}"
      ;;
  esac
  sha="$(git -C "$REPO_ROOT" ls-tree HEAD "$path" | awk '{print $3}')"

  if [ -z "${sha:-}" ]; then
    echo "[FAIL] $path — no submodule commit recorded in HEAD"
    failures=$((failures + 1))
    continue
  fi

  rm -rf "$TMP_GIT_DIR"/*
  git -C "$TMP_GIT_DIR" init --bare >/dev/null 2>&1
  if git -C "$TMP_GIT_DIR" fetch --depth=1 "$fetch_url" "$sha" >/dev/null 2>&1; then
    echo "[OK]   $path -> $sha"
  else
    echo "[FAIL] $path -> $sha is not available in remote $url"
    failures=$((failures + 1))
  fi
done <<EOF
$submodule_entries
EOF

if [ "$failures" -gt 0 ]; then
  echo "Submodule remote ref validation failed: $failures missing ref(s)."
  exit 1
fi

echo "All submodule refs recorded in HEAD are available in their remotes."
