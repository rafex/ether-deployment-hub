.PHONY: gh-check gh-runs gh-watch gh-logs release-plan-ci publish-plan-ci publish-ci

## gh-check: verify GitHub CLI auth and repo access
gh-check:
	@command -v gh >/dev/null 2>&1 || { echo "Error: gh CLI not found"; exit 1; }
	@gh auth status >/dev/null 2>&1 || { echo "Error: gh auth status failed. Run: gh auth login"; exit 1; }
	@echo "gh CLI OK"

## gh-runs: list recent runs for publish and release plan workflows
gh-runs: gh-check
	@echo "Recent Publish Java Modules runs:"
	@gh run list --workflow "Publish Java Modules" --limit 10
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

## release-plan-ci: trigger Generate Release Plan workflow via gh
release-plan-ci: gh-check
	@echo "Triggering Generate Release Plan workflow..."
	@if [ -n "$(BASE_REF)" ] || [ -n "$(HEAD_REF)" ]; then \
		gh workflow run "Generate Release Plan" \
			$(if $(BASE_REF),-f base_ref=$(BASE_REF),) \
			$(if $(HEAD_REF),-f head_ref=$(HEAD_REF),); \
	else \
		gh workflow run "Generate Release Plan"; \
	fi
	@echo "Latest Generate Release Plan run:"
	@gh run list --workflow "Generate Release Plan" --limit 1

## publish-plan-ci: trigger Publish Java Modules with run_deploy=false (dry run)
publish-plan-ci: gh-check
	@echo "Triggering Publish Java Modules (run_deploy=false)..."
	@gh workflow run "Publish Java Modules" \
		-f run_deploy=false \
		$(if $(BASE_REF),-f base_ref=$(BASE_REF),) \
		$(if $(HEAD_REF),-f head_ref=$(HEAD_REF),)
	@echo "Latest Publish Java Modules run:"
	@gh run list --workflow "Publish Java Modules" --limit 1

## publish-ci: trigger Publish Java Modules with run_deploy=true (real deploy)
publish-ci: gh-check
	@echo "Triggering Publish Java Modules (run_deploy=true)..."
	@gh workflow run "Publish Java Modules" \
		-f run_deploy=true \
		$(if $(BASE_REF),-f base_ref=$(BASE_REF),) \
		$(if $(HEAD_REF),-f head_ref=$(HEAD_REF),)
	@echo "Latest Publish Java Modules run:"
	@gh run list --workflow "Publish Java Modules" --limit 1
