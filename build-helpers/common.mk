# Makefile

# Load .env into Make variables if present

ifneq (,$(wildcard .env))
  include .env
  export OSSRH_USERNAME OSSRH_PASSWORD MAVEN_GPG_PASSPHRASE
endif

# Maven deploy profile used by publish jobs
DEPLOY_PROFILE ?= deploy
DEPLOY_PROFILE_ARG := $(if $(DEPLOY_PROFILE),-P$(DEPLOY_PROFILE),)
# Skip default maven-deploy-plugin when no deploy profile is active
MAVEN_DEPLOY_SKIP ?= $(if $(DEPLOY_PROFILE),false,true)
# Server id expected in distributionManagement/repository definitions
MAVEN_SERVER_ID ?= artifactory
# Reuse existing secret names from GitHub Actions by default
MAVEN_REPO_USERNAME ?= $(OSSRH_USERNAME)
MAVEN_REPO_PASSWORD ?= $(OSSRH_PASSWORD)
# Sonatype Central server entry (required by central-publishing-maven-plugin)
CENTRAL_SERVER_ID ?= central
CENTRAL_REPO_USERNAME ?= $(MAVEN_REPO_USERNAME)
CENTRAL_REPO_PASSWORD ?= $(MAVEN_REPO_PASSWORD)
# Optional: override Artifactory base URL from CI/.env
ARTIFACTORY_BASE_URL ?=
ARTIFACTORY_BASE_URL_ARG := $(if $(ARTIFACTORY_BASE_URL),-Dartifactory.base-url=$(ARTIFACTORY_BASE_URL),)
# Optional override for Sonatype Central publish wait strategy (e.g. validated|published)
CENTRAL_WAIT_UNTIL ?=
CENTRAL_WAIT_UNTIL_ARG := $(if $(CENTRAL_WAIT_UNTIL),-Dcentral.waitUntil=$(CENTRAL_WAIT_UNTIL),)

# Control whether to skip tests: true or false
SKIP_TESTS ?= false
# Auto-fix missing Java license headers before deploy
AUTO_UPDATE_LICENSE_HEADERS ?= false
# Extra goals required by Central (sources + javadocs)
PRE_DEPLOY_GOALS ?= source:jar javadoc:jar

# Directory containing the Maven module to build/deploy
PROJECT_DIR ?= ether-parent

# Helper function to find and execute mvnw
define run_mvnw
	@# Check if mvnw exists in PROJECT_DIR, if not try PROJECT_DIR/PROJECT_DIR
	@if [ -f "$(PROJECT_DIR)/mvnw" ]; then \
		cd $(PROJECT_DIR) && ./mvnw $(1); \
	elif [ -f "$(PROJECT_DIR)/$(notdir $(PROJECT_DIR))/mvnw" ]; then \
		cd $(PROJECT_DIR)/$(notdir $(PROJECT_DIR)) && ./mvnw $(1); \
	else \
		echo "Error: mvnw not found in $(PROJECT_DIR) or $(PROJECT_DIR)/$(notdir $(PROJECT_DIR))"; \
		exit 1; \
	fi
endef

# Coordinates for Maven Central existence check
PROJECT_GROUP_ID    ?= dev.rafex.ether.parent
PROJECT_ARTIFACT_ID ?= ether-parent
REPO_URL            ?= https://repo1.maven.org/maven2
# Repository used to discover already published dependency versions
VERSION_SOURCE_REPO ?= $(if $(ARTIFACTORY_BASE_URL),$(ARTIFACTORY_BASE_URL)/libs-release-local,$(REPO_URL))
# Automatically sync parent/dependency versions from VERSION_SOURCE_REPO
SYNC_FROM_REPO      ?= true
# Parent coordinate to sync before build/deploy (empty disables it)
PARENT_COORD        ?= dev.rafex.ether.parent:ether-parent
# Dependency property mappings to sync:
# DEPENDENCY_COORDS='ether-json.version=dev.rafex.ether.json:ether-json'
DEPENDENCY_COORDS   ?=

## show-env: display loaded environment variables
show-env:
	@echo "DEPLOY_PROFILE='$(DEPLOY_PROFILE)'"
	@echo "MAVEN_DEPLOY_SKIP='$(MAVEN_DEPLOY_SKIP)'"
	@echo "MAVEN_SERVER_ID='$(MAVEN_SERVER_ID)'"
	@echo "MAVEN_REPO_USERNAME='$(MAVEN_REPO_USERNAME)'"
	@echo "MAVEN_REPO_PASSWORD length='$(shell printf '%s' "$(MAVEN_REPO_PASSWORD)" | wc -c)'"
	@echo "CENTRAL_SERVER_ID='$(CENTRAL_SERVER_ID)'"
	@echo "CENTRAL_REPO_USERNAME='$(CENTRAL_REPO_USERNAME)'"
	@echo "CENTRAL_REPO_PASSWORD length='$(shell printf '%s' "$(CENTRAL_REPO_PASSWORD)" | wc -c)'"
	@echo "ARTIFACTORY_BASE_URL='$(ARTIFACTORY_BASE_URL)'"
	@echo "MAVEN_GPG_PASSPHRASE length='$(shell printf '%s' "$(MAVEN_GPG_PASSPHRASE)" | wc -c)'"

.PHONY: show-env write-settings sync-pom-versions set-version update-license-headers build deploy show-version valid-version

#
# Raw Git tag, or default "0.0.0"
TAG_RAW := $(shell git describe --tags --abbrev=0 2>/dev/null || echo "0.0.0")
# Final tag without leading "v" or "V"
TAG := $(shell echo "$(TAG_RAW)" | sed -E 's/^[vV]//')

# Calculate next snapshot (0.3.0 → 0.4.0-SNAPSHOT)
define next_snapshot
  major=$$(echo $(TAG) | cut -d. -f1); \
  minor=$$(echo $(TAG) | cut -d. -f2); \
  echo $${major}.$$((minor+1)).0-SNAPSHOT
endef

DEV_SNAPSHOT := $(shell $(next_snapshot))



# Base version: user can pass VERSION=... on command line
BASE_VERSION := $(VERSION)
# Normalize user-provided version: accepts vX.Y.Z or X.Y.Z
BASE_VERSION_NORMALIZED := $(shell echo "$(BASE_VERSION)" | sed -E 's/^[vV]//')
# Force exact semver from VERSION without adding build date suffix
FORCE_EXACT_VERSION ?= false
# Build date suffix (YYYYMMDD)
BUILD_DATE := $(shell date +%Y%m%d)
# Final version:
# - when FORCE_EXACT_VERSION=true, require VERSION and use it as-is (normalized)
# - otherwise keep historical behavior (tag/date)
ifeq ($(FORCE_EXACT_VERSION),true)
FINAL_VERSION := $(BASE_VERSION_NORMALIZED)
else
FINAL_VERSION := $(if $(BASE_VERSION_NORMALIZED),$(BASE_VERSION_NORMALIZED),$(TAG))-v$(BUILD_DATE)
endif

ifneq ($(FORCE_EXACT_VERSION),false)
ifeq ($(strip $(FINAL_VERSION)),)
$(error FORCE_EXACT_VERSION=true requires VERSION to be provided)
endif
endif

## write-settings: generate ~/.m2/settings.xml using configured repository credentials
write-settings:
	@echo "Writing settings.xml..."
	@mkdir -p ~/.m2
	@printf '<settings>\n  <servers>\n    <server>\n      <id>%s</id>\n      <username>%s</username>\n      <password>%s</password>\n    </server>\n    <server>\n      <id>%s</id>\n      <username>%s</username>\n      <password>%s</password>\n    </server>\n  </servers>\n</settings>\n' "$(MAVEN_SERVER_ID)" "$(MAVEN_REPO_USERNAME)" "$(MAVEN_REPO_PASSWORD)" "$(CENTRAL_SERVER_ID)" "$(CENTRAL_REPO_USERNAME)" "$(CENTRAL_REPO_PASSWORD)" > ~/.m2/settings.xml

## set-version: update project version in POM based on Git tag (in $(PROJECT_DIR))
set-version:
	@echo "Setting project version to $(FINAL_VERSION)..."
	$(call run_mvnw,versions:set -DnewVersion=$(FINAL_VERSION) -DgenerateBackupPoms=true)

## update-license-headers: add missing license headers in Java sources/tests when enabled
update-license-headers:
	@if [ "$(AUTO_UPDATE_LICENSE_HEADERS)" = "true" ]; then \
		echo "Updating missing license headers..."; \
		$(call run_mvnw,-B -ntp license:update-file-header); \
	else \
		echo "Skipping license header update (AUTO_UPDATE_LICENSE_HEADERS=$(AUTO_UPDATE_LICENSE_HEADERS))"; \
	fi

## sync-pom-versions: update parent and dependency properties using latest versions from repository
sync-pom-versions:
	@if [ "$(SYNC_FROM_REPO)" != "true" ]; then \
		echo "Skipping dependency sync (SYNC_FROM_REPO=$(SYNC_FROM_REPO))"; \
		exit 0; \
	fi
	@echo "Syncing parent/dependency versions from $(VERSION_SOURCE_REPO)..."
	@# First, find the actual pom.xml location
	@if [ -f "$(PROJECT_DIR)/pom.xml" ]; then \
		project_pom="$(PROJECT_DIR)/pom.xml"; \
	elif [ -f "$(PROJECT_DIR)/$(notdir $(PROJECT_DIR))/pom.xml" ]; then \
		project_pom="$(PROJECT_DIR)/$(notdir $(PROJECT_DIR))/pom.xml"; \
	else \
		echo "Error: pom.xml not found in $(PROJECT_DIR) or $(PROJECT_DIR)/$(notdir $(PROJECT_DIR))"; \
		exit 1; \
	fi
	@repo_url="$(VERSION_SOURCE_REPO)"; \
	if [ -n "$(PARENT_COORD)" ]; then \
		parent_group_id="$(word 1,$(subst :, ,$(PARENT_COORD)))"; \
		parent_artifact_id="$(word 2,$(subst :, ,$(PARENT_COORD)))"; \
		if grep -q "<parent>" "$$project_pom" && grep -q "<groupId>$$parent_group_id</groupId>" "$$project_pom" && grep -q "<artifactId>$$parent_artifact_id</artifactId>" "$$project_pom"; then \
			if parent_version="$$(./scripts/latest-maven-version.sh "$$repo_url" "$$parent_group_id" "$$parent_artifact_id" 2>/dev/null)"; then \
				echo "Updating parent $$parent_group_id:$$parent_artifact_id -> $$parent_version"; \
				$(call run_mvnw,-B -ntp versions:update-parent -DparentVersion="[$$parent_version]" -DallowSnapshots=false -DgenerateBackupPoms=true); \
			else \
				echo "Skipping parent sync for $$parent_group_id:$$parent_artifact_id (not found in $$repo_url)"; \
			fi; \
		fi; \
	fi; \
	for mapping in $(DEPENDENCY_COORDS); do \
		prop_name="$${mapping%%=*}"; \
		coord="$${mapping##*=}"; \
		dep_group_id="$${coord%%:*}"; \
		dep_artifact_id="$${coord##*:}"; \
		if dep_version="$$(./scripts/latest-maven-version.sh "$$repo_url" "$$dep_group_id" "$$dep_artifact_id" 2>/dev/null)"; then \
			echo "Updating property $$prop_name -> $$dep_version ($$dep_group_id:$$dep_artifact_id)"; \
			$(call run_mvnw,-B -ntp versions:set-property -Dproperty="$$prop_name" -DnewVersion="$$dep_version" -DgenerateBackupPoms=true); \
		else \
			echo "Skipping property $$prop_name (artifact $$dep_group_id:$$dep_artifact_id not found in $$repo_url)"; \
		fi; \
	done

## build: update version and compile+test project (in $(PROJECT_DIR))
build: sync-pom-versions set-version
	@echo "Building project version $(FINAL_VERSION)..."
	$(call run_mvnw,clean verify)

## deploy: write settings and set version, then deploy using profile $(DEPLOY_PROFILE) (in $(PROJECT_DIR))
deploy: write-settings sync-pom-versions set-version update-license-headers
	@echo "Deploying version $(FINAL_VERSION)..."
	$(call run_mvnw,clean $(PRE_DEPLOY_GOALS) deploy $(DEPLOY_PROFILE_ARG) $(ARTIFACTORY_BASE_URL_ARG) $(CENTRAL_WAIT_UNTIL_ARG) -DskipTests=$(SKIP_TESTS) -Dgpg.skip=false -Dmaven.deploy.skip=$(MAVEN_DEPLOY_SKIP))
	@echo "Reverting POM changes after deploy..."
	$(call run_mvnw,versions:revert)

## show-version: display the computed version that will be used
show-version:
	@echo "Using version: $(FINAL_VERSION)"

## valid-version: check that FINAL_VERSION does not already exist in Maven Central
valid-version:
	@echo "Checking Maven Central for existing version $(FINAL_VERSION)..."
	@GROUP_PATH=$$(echo $(PROJECT_GROUP_ID) | tr '.' '/'); \
	URL=$(REPO_URL)/$$GROUP_PATH/$(PROJECT_ARTIFACT_ID)/$(FINAL_VERSION)/$(PROJECT_ARTIFACT_ID)-$(FINAL_VERSION).pom; \
	HTTP_CODE=$$(curl -o /dev/null -s -w "%{http_code}" $$URL); \
	if [ "$$HTTP_CODE" -eq "404" ]; then \
	  echo "✅ Version $(FINAL_VERSION) not found; OK to deploy."; \
	  exit 0; \
	else \
	  echo "❌ Version $(FINAL_VERSION) already exists (HTTP $$HTTP_CODE); abort."; \
	  exit 1; \
	fi
