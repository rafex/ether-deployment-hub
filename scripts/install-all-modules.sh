#!/usr/bin/env bash
# install-all-modules.sh — Install all Maven modules in strict dependency order.
#
# The install order is derived dynamically from the actual inter-module
# dependency graph: every module POM under the module's project directory is
# parsed, internal artifactIds are mapped back to module names, and Kahn's
# topological sort produces a build order where all dependencies are installed
# before the modules that need them.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly REPO_ROOT
MANIFEST_PATH="$REPO_ROOT/releases/manifest.json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_cyan() {
    echo -e "${CYAN}$1${NC}"
}

# Temporary directory for per-module logs and intermediate files.
TMP_DIR="$(mktemp -d)"
cleanup() {
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

# Compute the topological install order from the real POM dependency graph.
# Outputs one line per module: "<module-name>|<project-dir>"
compute_install_order() {
    python3 - "$MANIFEST_PATH" "$REPO_ROOT" <<'PYEOF'
import json
import os
import sys
import xml.etree.ElementTree as ET

manifest_path = sys.argv[1]
repo_root = sys.argv[2]

with open(manifest_path, encoding="utf-8") as f:
    manifest = json.load(f)

modules = manifest.get("modules", [])
deploy_order = manifest.get("deployOrder") or [m["name"] for m in modules]
preferred_index = {name: idx for idx, name in enumerate(deploy_order)}

artifact_to_name: dict[str, str] = {}
name_to_dir: dict[str, str] = {}

for module in modules:
    name = module["name"]
    artifact = module.get("artifactId", name)
    artifact_to_name[artifact] = name
    name_to_dir[name] = module.get("projectDir") or module.get("path", name)

# Sub-artifacts (e.g. ether-brain children) also belong to their parent module.
for module in modules:
    name = module["name"]
    for sub in module.get("subArtifacts", []):
        if sub not in artifact_to_name:
            artifact_to_name[sub] = name

NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def internal_dependencies(module_name: str) -> list[str]:
    """Return internal module dependencies declared in all POMs under projectDir."""
    project_dir = os.path.join(repo_root, name_to_dir[module_name])
    if not os.path.isdir(project_dir):
        print(f"ERROR: project directory not found for {module_name}: {project_dir}", file=sys.stderr)
        sys.exit(1)

    deps: set[str] = set()

    for dirpath, _, filenames in os.walk(project_dir):
        # Skip generated build output directories.
        if "/target/" in dirpath or dirpath.endswith("/target"):
            continue
        for filename in filenames:
            if filename != "pom.xml":
                continue
            pom_path = os.path.join(dirpath, filename)
            try:
                tree = ET.parse(pom_path)
            except ET.ParseError as exc:
                print(f"ERROR: failed to parse {pom_path}: {exc}", file=sys.stderr)
                sys.exit(1)

            root = tree.getroot()

            # Parent POM reference.
            parent_art = root.find("m:parent/m:artifactId", NS)
            if parent_art is not None and parent_art.text:
                art = parent_art.text.strip()
                if art in artifact_to_name:
                    dep_name = artifact_to_name[art]
                    if dep_name != module_name:
                        deps.add(dep_name)

            # Direct dependencies (not dependencyManagement).
            deps_el = root.find("m:dependencies", NS)
            if deps_el is not None:
                for dep in deps_el.findall("m:dependency", NS):
                    art_el = dep.find("m:artifactId", NS)
                    if art_el is None or not art_el.text:
                        continue
                    art = art_el.text.strip()
                    if art in artifact_to_name:
                        dep_name = artifact_to_name[art]
                        if dep_name != module_name:
                            deps.add(dep_name)

    return list(deps)


dep_map: dict[str, list[str]] = {
    module["name"]: internal_dependencies(module["name"])
    for module in modules
}

# Kahn's algorithm — tie-break using the manifest deployOrder for a stable,
# deterministic order.
order: list[str] = []
assigned: set[str] = set()
remaining: set[str] = set(dep_map.keys())

while remaining:
    ready = [name for name in remaining if all(d in assigned for d in dep_map[name])]
    if not ready:
        # Cycle or unresolvable fallback: still emit the remaining modules in
        # manifest order so the failure is visible and deterministic.
        ready = [name for name in deploy_order if name in remaining]
        if not ready:
            ready = sorted(remaining)
        print(
            "WARNING: dependency cycle or unresolved dependency detected; "
            "falling back to manifest deployOrder for remaining modules",
            file=sys.stderr,
        )

    ready.sort(key=lambda name: (preferred_index.get(name, len(preferred_index)), name))

    for name in ready:
        assigned.add(name)
        order.append(name)
        remaining.discard(name)

for name in order:
    print(f"{name}|{name_to_dir[name]}")
PYEOF
}

# Counters and result lists.
TOTAL_MODULES=0
SUCCESS_MODULES=0
FAILED_MODULES=0

MODULE_ERROR_DIR="$TMP_DIR/errors"
mkdir -p "$MODULE_ERROR_DIR"

declare -a MODULES=()
declare -a SUCCESSFUL_MODULES=()
declare -a FAILED_MODULE_NAMES=()

# Resolve JAVA_HOME for Java 25 if needed.
ensure_java_home() {
    if [ -n "${JAVA_HOME:-}" ] && [ ! -x "$JAVA_HOME/bin/java" ]; then
        unset JAVA_HOME
    fi
    if [ -z "${JAVA_HOME:-}" ] && [ -x "/usr/libexec/java_home" ]; then
        JAVA_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null || /usr/libexec/java_home)"
        export JAVA_HOME
    fi
}

# Install a single module and capture its Maven output.
install_module() {
    local module_name="$1"
    local module_dir="$2"
    local temp_log_file="$TMP_DIR/${module_name}.log"

    print_info "Installing $module_name..."

    ensure_java_home

    local maven_cmd
    if [ -x "$module_dir/mvnw" ] && [ -f "$module_dir/.mvn/wrapper/maven-wrapper.properties" ]; then
        maven_cmd="./mvnw -B -ntp -DskipTests=true -Dgpg.skip=true clean install"
    else
        maven_cmd="mvn -B -ntp -DskipTests=true -Dgpg.skip=true clean install"
    fi

    local exit_code=0
    # shellcheck disable=SC2086
    if (cd "$module_dir" && $maven_cmd) > "$temp_log_file" 2>&1; then
        exit_code=0
    else
        exit_code=$?
    fi

    ((TOTAL_MODULES++))

    if [ "$exit_code" -eq 0 ]; then
        ((SUCCESS_MODULES++))
        SUCCESSFUL_MODULES+=("$module_name")
        print_success "$module_name installed successfully"
        rm -f "$temp_log_file"
        return 0
    fi

    ((FAILED_MODULES++))
    FAILED_MODULE_NAMES+=("$module_name")

    local error_message
    error_message=$(grep -E -A 5 "BUILD FAILURE|ERROR|FAILED" "$temp_log_file" | head -n 20 || true)
    if [ -z "$error_message" ]; then
        error_message=$(tail -n 30 "$temp_log_file" || true)
    fi
    printf '%s\n' "$error_message" > "$MODULE_ERROR_DIR/$module_name.txt"

    print_error "$module_name failed (exit code: $exit_code)"
    print_warning "Error details:"
    echo -e "${RED}$error_message${NC}" | sed 's/^/   /'

    return 1
}

print_summary() {
    echo ""
    echo "==================================================================="
    echo "                      BUILD SUMMARY                                "
    echo "==================================================================="
    echo ""

    if [ ${#SUCCESSFUL_MODULES[@]} -gt 0 ]; then
        print_cyan "Modules installed successfully (${#SUCCESSFUL_MODULES[@]}/$TOTAL_MODULES):"
        echo "   ------------------------------------------------"
        for module in "${SUCCESSFUL_MODULES[@]}"; do
            echo "   - $module"
        done
        echo ""
    fi

    if [ ${#FAILED_MODULE_NAMES[@]} -gt 0 ]; then
        print_error "Modules with errors (${#FAILED_MODULE_NAMES[@]}/$TOTAL_MODULES):"
        echo "   ------------------------------------------------"
        for module in "${FAILED_MODULE_NAMES[@]}"; do
            echo "   - $module"
        done
        echo ""

        print_error "Error details:"
        echo "   ==================="
        for module in "${FAILED_MODULE_NAMES[@]}"; do
            echo ""
            echo -e "${RED}$module:${NC}"
            sed 's/^/   /' "$MODULE_ERROR_DIR/$module.txt"
        done
        echo ""
    fi

    echo "==================================================================="
    echo "                       FINAL STATISTICS                            "
    echo "==================================================================="
    printf "  %-40s %4d\n" "Total modules:" "$TOTAL_MODULES"
    printf "  %-40s %4d\n" "Installed successfully:" "$SUCCESS_MODULES"
    printf "  %-40s %4d\n" "With errors:" "$FAILED_MODULES"
    echo "==================================================================="
    echo ""

    if [ "$FAILED_MODULES" -eq 0 ]; then
        print_success "All modules were installed successfully."
        return 0
    fi

    print_error "Some modules could not be installed. Review the errors above."
    return 1
}

main() {
    if [ ! -f "$MANIFEST_PATH" ]; then
        print_error "Manifest not found: $MANIFEST_PATH"
        exit 1
    fi

    echo ""
    print_info "Starting install of all Maven modules"
    print_info "Manifest: $MANIFEST_PATH"
    echo ""

    local order_file="$TMP_DIR/install-order.txt"
    if ! compute_install_order > "$order_file"; then
        print_error "Failed to compute module install order"
        exit 1
    fi

    MODULES=()
    while IFS='|' read -r module_name module_dir; do
        [ -n "$module_name" ] || continue
        MODULES+=("$module_name|$module_dir")
    done < "$order_file"

    print_info "Computed install order (${#MODULES[@]} modules):"
    local order_display=""
    for entry in "${MODULES[@]}"; do
        local name="${entry%%|*}"
        order_display="$order_display $name"
    done
    print_cyan "  $order_display"
    echo ""

    for entry in "${MODULES[@]}"; do
        local name="${entry%%|*}"
        local dir="${entry#*|}"
        local full_dir="$REPO_ROOT/$dir"

        # Continue on individual module failures so the summary reports all.
        if ! install_module "$name" "$full_dir"; then
            continue
        fi
    done

    print_summary
}

main "$@"
