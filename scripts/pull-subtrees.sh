#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MANIFEST_PATH="${1:-$ROOT_DIR/releases/subtrees.json}"

if [ ! -f "$MANIFEST_PATH" ]; then
  echo "Subtree manifest not found: $MANIFEST_PATH" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required to sync subtrees." >&2
  exit 1
fi

mode="${2:-status}"

fetch_url() {
  local url="$1"
  case "$url" in
    git@github.com:*) printf 'https://github.com/%s\n' "${url#git@github.com:}" ;;
    *) printf '%s\n' "$url" ;;
  esac
}

status_subtrees() {
  jq -r '.modules[] | [.name, .prefix, .branch, .importedSha, .url] | @tsv' "$MANIFEST_PATH" |
    while IFS=$'\t' read -r name prefix branch imported_sha url; do
      if [ -d "$ROOT_DIR/$prefix" ]; then
        printf '[OK]   %-28s prefix=%s branch=%s sha=%s url=%s\n' "$name" "$prefix" "$branch" "$imported_sha" "$url"
      else
        printf '[MISS] %-28s prefix=%s branch=%s sha=%s url=%s\n' "$name" "$prefix" "$branch" "$imported_sha" "$url"
      fi
    done
}

pull_subtrees() {
  jq -c '.modules[]' "$MANIFEST_PATH" |
    while IFS= read -r module_json; do
      name="$(printf '%s' "$module_json" | jq -r '.name')"
      prefix="$(printf '%s' "$module_json" | jq -r '.prefix')"
      branch="$(printf '%s' "$module_json" | jq -r '.branch')"
      url="$(printf '%s' "$module_json" | jq -r '.url')"
      remote="$(fetch_url "$url")"

      echo "=== Pulling subtree $name ($prefix <- $remote $branch) ==="
      git -C "$ROOT_DIR" subtree pull --prefix="$prefix" "$remote" "$branch" --squash
      echo
    done
}

case "$mode" in
  status)
    status_subtrees
    ;;
  pull)
    pull_subtrees
    ;;
  *)
    echo "Usage: $0 [manifest-path] [status|pull]" >&2
    exit 1
    ;;
esac
