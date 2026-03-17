#!/usr/bin/env bash
set -euo pipefail

release_root_dir() {
  cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd
}

release_manifest_path() {
  printf '%s/releases/manifest.json\n' "$(release_root_dir)"
}

release_level_rank() {
  case "${1:-none}" in
    none) printf '0\n' ;;
    patch) printf '1\n' ;;
    minor) printf '2\n' ;;
    major) printf '3\n' ;;
    *)
      echo "Unknown release level: ${1:-}" >&2
      exit 1
      ;;
  esac
}

release_max_level() {
  local left="${1:-none}"
  local right="${2:-none}"
  if [ "$(release_level_rank "$left")" -ge "$(release_level_rank "$right")" ]; then
    printf '%s\n' "$left"
  else
    printf '%s\n' "$right"
  fi
}

release_normalize_semver() {
  local version="${1:-0.0.0}"
  version="${version#v}"
  version="${version%%-*}"
  IFS='.' read -r major minor patch <<< "$version"
  major="${major:-0}"
  minor="${minor:-0}"
  patch="${patch:-0}"
  printf '%s.%s.%s\n' "$major" "$minor" "$patch"
}

release_bump_version() {
  local version
  local level
  version="$(release_normalize_semver "${1:-0.0.0}")"
  level="${2:-none}"

  IFS='.' read -r major minor patch <<< "$version"
  case "$level" in
    none) ;;
    patch) patch=$((patch + 1)) ;;
    minor)
      minor=$((minor + 1))
      patch=0
      ;;
    major)
      major=$((major + 1))
      minor=0
      patch=0
      ;;
    *)
      echo "Unknown release level: $level" >&2
      exit 1
      ;;
  esac
  printf '%s.%s.%s\n' "$major" "$minor" "$patch"
}

release_default_base_ref() {
  if [ -n "${RELEASE_BASE_REF:-}" ]; then
    printf '%s\n' "$RELEASE_BASE_REF"
    return 0
  fi

  if git rev-parse --verify HEAD^ >/dev/null 2>&1; then
    git rev-parse HEAD^
    return 0
  fi

  git rev-list --max-parents=0 HEAD | tail -n 1
}

release_is_docs_or_meta_file() {
  local relative_path="${1:-}"
  case "$relative_path" in
    README.md|README.template.md|LICENSE|LICENSE.txt|.gitignore|.githooks/*|.github/*|docs/*|scripts/*|release-artifacts/*|releases/*)
      return 0
      ;;
    */README.md|*/LICENSE|*/LICENSE.txt|*/.gitignore|*/docs/*|*/src/test/*|*/.settings/*|*/.project|*/.classpath|*/.factorypath)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

release_is_build_file() {
  local relative_path="${1:-}"
  case "$relative_path" in
    */pom.xml|*/Makefile|build-helpers/*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

release_commit_level_for_log() {
  local log_text="${1:-}"
  if printf '%s' "$log_text" | grep -Eq 'BREAKING CHANGE|^[a-z]+(\([^)]+\))?!:'; then
    printf 'major\n'
    return 0
  fi
  if printf '%s' "$log_text" | grep -Eq '^feat(\([^)]+\))?:'; then
    printf 'minor\n'
    return 0
  fi
  printf 'patch\n'
}
