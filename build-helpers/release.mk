# Release build/apply facade for local Maven Central releases.
# Keep logic in scripts; this file only defines variables and orchestrates
# existing build-helpers/compile.mk targets.

BASE_REF ?=
HEAD_REF ?=
LOG_FILE ?= /tmp/ether-deployment-hub/release-$(shell date +%Y%m%d-%H%M%S).log

.PHONY: release-build release-apply

## release-build: sync manifest, generate release plan and validate collisions (no deploy)
release-build: sync-manifest release-plan validate-release-plan
	@echo "Release build completed. Log: $(LOG_FILE)"

## release-apply: apply generated release plan to module POMs and update manifest
release-apply:
	@./scripts/apply-release-plan.sh release-artifacts/release-plan.json releases/manifest.json --log-file $(LOG_FILE)
