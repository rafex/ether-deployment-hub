.PHONY: gh-check gh-runs gh-watch gh-logs release-plan-ci publish-plan-ci publish-ci publish-gh-pkg-ci

## gh-check: verify GitHub CLI auth and repo access
gh-check:
	@command -v gh >/dev/null 2>&1 || { echo "Error: gh CLI not found"; exit 1; }
	@gh auth status >/dev/null 2>&1 || { echo "Error: gh auth status failed. Run: gh auth login"; exit 1; }
	@echo "gh CLI OK"

## gh-runs: list recent runs for publish and release plan workflows
gh-runs: gh-check
	@echo "Recent Publish Java Modules - Maven Central runs:"
	@gh run list --workflow "Publish Java Modules - Maven Central" --limit 10
	@echo
	@echo "Recent Publish Java Modules - GitHub Packages runs:"
	@gh run list --workflow "Publish Java Modules - GitHub Packages" --limit 10
	@echo
	@echo "Recent Generate Release Plan runs:"
	@gh run list --workflow "Generate Release Plan" --limit 10

## gh-watch: watch a specific run id (usage: make gh-watch RUN_ID=123)
gh-watch: gh-check
	@if [ -z "$(RUN_ID)" ]; then \
		echo "Error: RUN_ID is required. Usage: make gh-watch RUN_ID=<run-id>"; \
		exit 1; \
	fi
	@gh run watch "$(RUN_ID)"

## gh-logs: view logs for a specific run id (usage: make gh-logs RUN_ID=123)
gh-logs: gh-check
	@if [ -z "$(RUN_ID)" ]; then \
		echo "Error: RUN_ID is required. Usage: make gh-logs RUN_ID=<run-id>"; \
		exit 1; \
	fi
	@gh run view "$(RUN_ID)" --log

## _last-deploy-ref: resolve BASE_REF — explicit > last manifest commit > error
_last-deploy-ref:
	$(eval _RESOLVED_BASE := $(or \
		$(BASE_REF), \
		$(shell git log --oneline -- releases/manifest.json 2>/dev/null | head -1 | cut -d' ' -f1)))
	@if [ -z "$(_RESOLVED_BASE)" ]; then \
		echo "Error: could not determine BASE_REF. Pass BASE_REF=<sha> explicitly."; \
		exit 1; \
	fi
	@echo "BASE_REF → $(_RESOLVED_BASE)$(if $(BASE_REF), (explicit), (last manifest commit))"

## release-plan-ci: trigger Generate Release Plan workflow via gh
## Uses last manifest-update commit as BASE_REF if not specified.
release-plan-ci: gh-check _last-deploy-ref
	@echo "Triggering Generate Release Plan workflow..."
	@gh workflow run "Generate Release Plan" \
		-f base_ref=$(_RESOLVED_BASE) \
		$(if $(HEAD_REF),-f head_ref=$(HEAD_REF),)
	@echo "Latest Generate Release Plan run:"
	@gh run list --workflow "Generate Release Plan" --limit 1

## publish-plan-ci: dry run — shows what would be deployed without publishing
## Uses last manifest-update commit as BASE_REF if not specified.
publish-plan-ci: gh-check _last-deploy-ref
	@echo "Triggering Publish Java Modules - Maven Central (run_deploy=false)..."
	@gh workflow run "Publish Java Modules - Maven Central" \
		-f run_deploy=false \
		-f base_ref=$(_RESOLVED_BASE) \
		$(if $(HEAD_REF),-f head_ref=$(HEAD_REF),)
	@echo "Latest Publish Java Modules - Maven Central run:"
	@gh run list --workflow "Publish Java Modules - Maven Central" --limit 1

## publish-ci: deploy all changed modules to Maven Central since last release.
## BASE_REF defaults to the last commit that updated releases/manifest.json.
## Override: make publish-ci BASE_REF=<sha>
publish-ci: gh-check _last-deploy-ref
	@echo "Triggering Publish Java Modules - Maven Central (run_deploy=true)..."
	@gh workflow run "Publish Java Modules - Maven Central" \
		-f run_deploy=true \
		-f base_ref=$(_RESOLVED_BASE) \
		$(if $(HEAD_REF),-f head_ref=$(HEAD_REF),)
	@echo "Latest Publish Java Modules - Maven Central run:"
	@gh run list --workflow "Publish Java Modules - Maven Central" --limit 1

## publish-gh-pkg-ci: manually trigger GitHub Packages deploy from a Maven Central run (usage: make publish-gh-pkg-ci RUN_ID=123)
publish-gh-pkg-ci: gh-check
	@if [ -z "$(RUN_ID)" ]; then \
		echo "Error: RUN_ID is required. Usage: make publish-gh-pkg-ci RUN_ID=<maven-central-run-id>"; \
		exit 1; \
	fi
	@echo "Triggering Publish Java Modules - GitHub Packages (maven_central_run_id=$(RUN_ID))..."
	@gh workflow run "Publish Java Modules - GitHub Packages" \
		-f maven_central_run_id=$(RUN_ID)
	@echo "Latest Publish Java Modules - GitHub Packages run:"
	@gh run list --workflow "Publish Java Modules - GitHub Packages" --limit 1
