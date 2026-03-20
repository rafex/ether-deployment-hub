#!/usr/bin/env bash
# deploy-to-github-packages.sh — Upload pre-built ether artifacts to GitHub Packages.
#
# Reads artifact coordinates from manifest.json and the version from
# release-plan.json, then uploads the artifacts already installed in the
# local Maven cache (~/.m2/repository) to GitHub Packages.
#
# Artifacts are copied to a staging directory outside ~/.m2 before upload
# (Maven refuses to deploy files that live inside the local repository path).
# Each artifact (.jar, -sources.jar, -javadoc.jar, .pom) is signed with GPG
# using mvn gpg:sign-and-deploy-file, which creates detached .asc signatures
# and uploads them alongside the artifacts.
#
# No recompilation is required; artifacts are reused from the Maven Central
# deploy step that ran earlier in the same CI job.
#
# Usage:
#   ./scripts/deploy-to-github-packages.sh \
#       <release-plan.json> \
#       <manifest.json>    \
#       <github-packages-url>
#
# Environment variables:
#   GH_PACKAGES_TOKEN     — GitHub token with write:packages permission
#   GITHUB_ACTOR          — GitHub username (set automatically in Actions)
#   MAVEN_GPG_PASSPHRASE  — GPG passphrase for signing artifacts
#   GH_SETTINGS_FILE      — path to write temp settings.xml (default: /tmp/gh-settings.xml)
#
# Exit codes:
#   0 — all modules uploaded successfully (or skipped)
#   1 — one or more modules failed

set -euo pipefail

PLAN_PATH="${1:?Usage: $0 <release-plan.json> <manifest.json> <github-packages-url>}"
MANIFEST_PATH="${2:?Manifest path required}"
GH_PKG_URL="${3:?GitHub Packages URL required}"

GH_SETTINGS_FILE="${GH_SETTINGS_FILE:-/tmp/gh-settings.xml}"
M2_REPO="${HOME}/.m2/repository"

# ── prerequisites ────────────────────────────────────────────────────────────
if [ ! -f "$PLAN_PATH" ]; then
  echo "ERROR: Release plan not found: $PLAN_PATH" >&2; exit 1
fi
if [ ! -f "$MANIFEST_PATH" ]; then
  echo "ERROR: Manifest not found: $MANIFEST_PATH" >&2; exit 1
fi
if [ -z "${GH_PACKAGES_TOKEN:-}" ]; then
  echo "ERROR: GH_PACKAGES_TOKEN is not set" >&2; exit 1
fi
if [ -z "${MAVEN_GPG_PASSPHRASE:-}" ]; then
  echo "ERROR: MAVEN_GPG_PASSPHRASE is not set — GPG signing is required" >&2; exit 1
fi

# ── write GitHub-specific settings.xml ───────────────────────────────────────
mkdir -p "$(dirname "$GH_SETTINGS_FILE")"
cat > "$GH_SETTINGS_FILE" <<SETTINGS_XML
<settings>
  <servers>
    <server>
      <id>github-ether</id>
      <username>${GITHUB_ACTOR:-github-actions}</username>
      <password>${GH_PACKAGES_TOKEN}</password>
    </server>
  </servers>
</settings>
SETTINGS_XML

# ── find a Maven executable ───────────────────────────────────────────────────
# Prefer a module's mvnw (always present in the hub checkout); fall back to
# a system-installed mvn.
find_mvn() {
  if command -v mvn >/dev/null 2>&1; then
    echo "mvn"
    return
  fi
  # Try any module's mvnw relative to the hub root
  for dir in ether-parent ether-config ether-json; do
    if [ -x "${dir}/${dir}/mvnw" ] || [ -x "${dir}/mvnw" ]; then
      local mvnw
      mvnw="$(find "${dir}" -name mvnw -maxdepth 2 | head -1)"
      if [ -n "$mvnw" ]; then
        echo "$(pwd)/$mvnw"
        return
      fi
    fi
  done
  echo "mvn"  # last resort — will fail if not found
}
MVN="$(find_mvn)"

# ── helper: deploy one module ─────────────────────────────────────────────────
deploy_module() {
  local name="$1"
  local group_id="$2"
  local artifact_id="$3"
  local version="$4"

  local group_path="${group_id//./\/}"
  local m2_dir="${M2_REPO}/${group_path}/${artifact_id}/${version}"
  local pom_file="${m2_dir}/${artifact_id}-${version}.pom"
  local jar_file="${m2_dir}/${artifact_id}-${version}.jar"
  local sources_file="${m2_dir}/${artifact_id}-${version}-sources.jar"
  local javadoc_file="${m2_dir}/${artifact_id}-${version}-javadoc.jar"

  if [ ! -f "$pom_file" ]; then
    echo "  [GH WARN] POM not in ~/.m2: $pom_file — skipping $name" >&2
    return 0   # warn but do not fail; artifact may have been skipped in Central
  fi

  # Maven refuses to deploy:deploy-file from inside the local repository path.
  # Copy artifacts to a staging area outside ~/.m2 to avoid the restriction:
  #   "Cannot deploy artifact from the local repository"
  local stage_dir="${RUNNER_TEMP:-/tmp}/gh-stage/${name}"
  mkdir -p "$stage_dir"
  cp "$pom_file" "$stage_dir/"
  local staged_pom="${stage_dir}/${artifact_id}-${version}.pom"
  local staged_jar="${stage_dir}/${artifact_id}-${version}.jar"
  local staged_sources="${stage_dir}/${artifact_id}-${version}-sources.jar"
  local staged_javadoc="${stage_dir}/${artifact_id}-${version}-javadoc.jar"
  [ -f "$jar_file" ]     && cp "$jar_file"     "$staged_jar"
  [ -f "$sources_file" ] && cp "$sources_file" "$staged_sources"
  [ -f "$javadoc_file" ] && cp "$javadoc_file" "$staged_javadoc"

  local extra_args=()
  [ -f "$staged_sources" ] && extra_args+=("-Dsources=${staged_sources}")
  [ -f "$staged_javadoc" ] && extra_args+=("-Djavadoc=${staged_javadoc}")

  # gpg:sign-and-deploy-file signs every artifact with a detached .asc signature
  # and uploads both the artifact and its signature in one step.
  # The passphrase is read from MAVEN_GPG_PASSPHRASE to avoid pinentry prompts.
  if [ -f "$staged_jar" ]; then
    "$MVN" -B -ntp gpg:sign-and-deploy-file \
      -Dfile="$staged_jar"                        \
      -DpomFile="$staged_pom"                     \
      -Durl="$GH_PKG_URL"                         \
      -DrepositoryId=github-ether                 \
      -Dgpg.passphrase="${MAVEN_GPG_PASSPHRASE}"  \
      "${extra_args[@]}"                          \
      --settings "$GH_SETTINGS_FILE"
  else
    # POM-only (BOM) artifact — sign and deploy staged copy
    "$MVN" -B -ntp gpg:sign-and-deploy-file \
      -Dfile="$staged_pom"                        \
      -DpomFile="$staged_pom"                     \
      -Dpackaging=pom                             \
      -Durl="$GH_PKG_URL"                         \
      -DrepositoryId=github-ether                 \
      -Dgpg.passphrase="${MAVEN_GPG_PASSPHRASE}"  \
      --settings "$GH_SETTINGS_FILE"
  fi
}

# ── iterate all releasable modules, deploy in parallel ────────────────────────
pids=()
names=()
log_files=()
LOG_DIR="${RUNNER_TEMP:-/tmp}/gh-deploy-logs"
mkdir -p "$LOG_DIR"

while IFS= read -r module_json; do
  name=$(printf '%s' "$module_json"     | jq -r '.name')
  release_level=$(printf '%s' "$module_json" | jq -r '.releaseLevel')

  if [ "$release_level" = "none" ]; then
    continue
  fi

  # Look up groupId / artifactId from the manifest (not present in plan)
  manifest_entry=$(jq -c --arg n "$name" '.modules[] | select(.name == $n)' "$MANIFEST_PATH")
  if [ -z "$manifest_entry" ]; then
    echo "  [GH WARN] $name not found in manifest — skipping" >&2
    continue
  fi
  group_id=$(printf '%s'    "$manifest_entry" | jq -r '.groupId')
  artifact_id=$(printf '%s' "$manifest_entry" | jq -r '.artifactId')
  version=$(printf '%s'     "$module_json"    | jq -r '.nextVersion')

  log_file="$LOG_DIR/gh-${name}.log"
  echo "  [GH START] ${name} ${group_id}:${artifact_id}:${version}"

  (deploy_module "$name" "$group_id" "$artifact_id" "$version") \
    >"$log_file" 2>&1 &

  pids+=($!)
  names+=("$name")
  log_files+=("$log_file")

done < <(jq -c '.modules[]' "$PLAN_PATH")

# ── collect results ───────────────────────────────────────────────────────────
any_failed=false
total_ok=0; total_skip=0; total_fail=0
ok_modules=(); skip_modules=(); fail_modules=()

for i in "${!pids[@]}"; do
  module="${names[$i]}"
  log_file="${log_files[$i]}"

  if wait "${pids[$i]}"; then
    echo "  [GH OK]   $module"
    ok_modules+=("$module")
    (( total_ok++ )) || true
  else
    if grep -Eq "already exists|409|Conflict" "$log_file" 2>/dev/null; then
      echo "  [GH SKIP] $module — already exists in GitHub Packages"
      skip_modules+=("$module — already in GitHub Packages")
      (( total_skip++ )) || true
    else
      echo "  [GH FAIL] $module"
      echo "──── $log_file ────"
      cat "$log_file"
      echo "────────────────────"
      fail_modules+=("$module")
      (( total_fail++ )) || true
      any_failed=true
    fi
  fi
done

echo ""
echo "════════════════════════════════════════════════════════"
echo " GitHub Packages — Deploy summary"
echo "════════════════════════════════════════════════════════"
echo " ✅ published : $total_ok"
for m in "${ok_modules[@]}";   do echo "      • $m"; done
echo " ⏭  skipped   : $total_skip"
for m in "${skip_modules[@]}"; do echo "      • $m"; done
echo " ❌ failed    : $total_fail"
for m in "${fail_modules[@]}"; do echo "      • $m"; done
echo "════════════════════════════════════════════════════════"

if $any_failed; then
  echo "ERROR: One or more modules failed to deploy to GitHub Packages" >&2
  exit 1
fi
