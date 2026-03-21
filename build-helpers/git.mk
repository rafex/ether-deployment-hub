# Usage:
#   make new TAG=<version> MESSAGE="Your tag message"
#   make retag TAG=<version> MESSAGE="Your tag message"

.PHONY: new retag submodules-pull submodules-status submodules-push submodules-update submodules-init submodules-clean submodules-all

## new: create a new Git tag and push it to origin (does not overwrite existing tags)
new:
	@if [ -z "$(TAG)" ]; then \
		echo "Error: TAG not set. Usage: make new TAG=<version>"; \
		exit 1; \
	fi
	@if [ -z "$(MESSAGE)" ]; then \
		echo "Error: MESSAGE not set. Usage: make new TAG=<version> MESSAGE=\"Your tag message\""; \
		exit 1; \
	fi
	@if git rev-parse -q --verify "refs/tags/$(TAG)" >/dev/null; then \
		echo "Error: local tag '$(TAG)' already exists. Refusing to overwrite."; \
		exit 1; \
	fi
	@if git ls-remote --tags origin "refs/tags/$(TAG)" | grep -q .; then \
		echo "Error: remote tag '$(TAG)' already exists on origin. Refusing to overwrite."; \
		exit 1; \
	fi
	@echo "Creating tag '$(TAG)' and pushing to origin..."
	git tag -a $(TAG) -m "$(MESSAGE)"
	git push origin $(TAG)

## retag: recreate a tag locally/remotely and push it (force release re-run)
retag:
	@if [ -z "$(TAG)" ]; then \
		echo "Error: TAG not set. Usage: make retag TAG=<version> MESSAGE=\"Your tag message\""; \
		exit 1; \
	fi
	@if [ -z "$(MESSAGE)" ]; then \
		echo "Error: MESSAGE not set. Usage: make retag TAG=<version> MESSAGE=\"Your tag message\""; \
		exit 1; \
	fi
	@if git rev-parse -q --verify "refs/tags/$(TAG)" >/dev/null; then \
		echo "Deleting local tag '$(TAG)'..."; \
		git tag -d "$(TAG)"; \
	fi
	@if git ls-remote --tags origin "refs/tags/$(TAG)" | grep -q .; then \
		echo "Deleting remote tag '$(TAG)' from origin..."; \
		git push origin ":refs/tags/$(TAG)"; \
	fi
	@echo "Recreating tag '$(TAG)' and pushing to origin..."
	git tag -a "$(TAG)" -m "$(MESSAGE)"
	git push origin "$(TAG)" --force

submodules-pull:
	git submodule foreach --recursive 'git pull origin main'
	git add .
	git commit -m "Update submodules" || true

## submodules-status: check status of all submodules
submodules-status:
	@echo "=== Checking submodule status ==="
	@./scripts/push-all-submodules.sh status || true

## submodules-push: push submodules with pending commits (interactive)
submodules-push:
	@echo "=== Pushing submodules with pending commits ==="
	@./scripts/push-all-submodules.sh push

## submodules-push-non-interactive: push submodules in non-interactive mode (CI/CD)
submodules-push-non-interactive:
	@echo "=== Pushing submodules (non-interactive mode) ==="
	@./scripts/push-all-submodules.sh --non-interactive push

## submodules-update: update all submodules to latest remote versions (interactive)
submodules-update:
	@echo "=== Updating submodules to latest remote versions ==="
	@./scripts/push-all-submodules.sh update

## submodules-update-non-interactive: update submodules in non-interactive mode (CI/CD)
submodules-update-non-interactive:
	@echo "=== Updating submodules (non-interactive mode) ==="
	@./scripts/push-all-submodules.sh --non-interactive update

## submodules-clean: reset all submodules (loses uncommitted changes, interactive)
submodules-clean:
	@echo "=== Cleaning/resetting all submodules ==="
	@./scripts/push-all-submodules.sh clean

## submodules-clean-non-interactive: clean submodules in non-interactive mode
submodules-clean-non-interactive:
	@echo "=== Cleaning submodules (non-interactive mode) ==="
	@./scripts/push-all-submodules.sh --non-interactive clean

## submodules-all: run complete submodule workflow (status → push → update)
submodules-all:
	@echo "=== Running complete submodule workflow ==="
	@./scripts/push-all-submodules.sh all

## submodules-all-non-interactive: run complete workflow in non-interactive mode (CI/CD)
submodules-all-non-interactive:
	@echo "=== Running complete submodule workflow (non-interactive) ==="
	@./scripts/push-all-submodules.sh --non-interactive all

## submodules-help: show submodule management help
submodules-help:
	@echo "Submodule Management Commands:"
	@echo ""
	@echo "Interactive Commands:"
	@echo "  make submodules-status    - Check status of all submodules"
	@echo "  make submodules-push      - Push submodules with pending commits"
	@echo "  make submodules-update    - Update submodules to latest remote versions"
	@echo "  make submodules-init      - Initialize and update all submodules"
	@echo "  make submodules-clean     - Reset all submodules (loses uncommitted changes)"
	@echo "  make submodules-all       - Run complete workflow (status → push → update)"
	@echo "  make submodules-pull      - Pull all submodules and update parent"
	@echo ""
	@echo "Non-interactive Commands (CI/CD):"
	@echo "  make submodules-push-non-interactive      - Push without prompts"
	@echo "  make submodules-update-non-interactive    - Update without prompts"
	@echo "  make submodules-clean-non-interactive     - Clean without prompts"
	@echo "  make submodules-all-non-interactive       - Complete workflow without prompts"
	@echo ""
	@echo "Legacy non-interactive mode (still works):"
	@echo "  echo 'y' | make submodules-push"
	@echo "  echo 'y' | make submodules-update"