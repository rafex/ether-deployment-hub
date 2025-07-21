# Makefile

# Load .env into Make variables if present
ifneq (,$(wildcard .env))
  include .env
  export OSSRH_USERNAME OSSRH_PASSWORD MAVEN_GPG_PASSPHRASE
endif

## show-env: display loaded environment variables
show-env:
	@echo "OSSRH_USERNAME='$(OSSRH_USERNAME)'"
	@echo "OSSRH_PASSWORD length='$(shell printf '%s' "$(OSSRH_PASSWORD)" | wc -c)'"
	@echo "GPG_PASSPHRASE length='$(shell printf '%s' "$(GPG_PASSPHRASE)" | wc -c)'"

.PHONY: show-env write-settings set-version build deploy

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

# Build date for version suffix
BUILD_DATE := $(shell date +%Y%m%d)
# Version with date suffix (e.g., 4.0.0-v2025-07-21)
VERSION := $(TAG)-v$(BUILD_DATE)

## write-settings: generate ~/.m2/settings.xml using OSSRH credentials
write-settings:
	@echo "Writing settings.xml..."
	@mkdir -p ~/.m2
	@printf '<settings>\n  <servers>\n    <server>\n      <id>central</id>\n      <!-- usa tu Portal User Token -->\n      <username>%s</username>\n      <password>%s</password>\n    </server>\n  </servers>\n</settings>\n' "$(OSSRH_USERNAME)" "$(OSSRH_PASSWORD)" > ~/.m2/settings.xml

## set-version: update project version in POM based on Git tag
set-version:
	@echo "Setting project version to $(VERSION)..."
	cd ether-parent && mvn versions:set -DnewVersion=$(VERSION) -DgenerateBackupPoms=false

## build: update version and compile+test project
build: set-version
	@echo "Building project version $(VERSION)..."
	cd ether-parent && mvn clean verify

## deploy: write settings and set version, then deploy to Maven Central
deploy: write-settings set-version
	@echo "Deploying version $(VERSION)..."
	cd ether-parent && mvn clean deploy -DskipTests -Dgpg.skip=false
