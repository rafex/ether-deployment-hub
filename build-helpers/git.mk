# Usage:
#   make new TAG=<version> MESSAGE="Your tag message"
#   make retag TAG=<version> MESSAGE="Your tag message"

.PHONY: new retag submodules-pull

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