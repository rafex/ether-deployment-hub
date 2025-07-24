# Usage: make new TAG=<version> MESSAGE="Your tag message"

.PHONY: new

## new: create or update a Git tag and push it forcefully to origin
new:
	@if [ -z "$(TAG)" ]; then \
		echo "Error: TAG not set. Usage: make new TAG=<version>"; \
		exit 1; \
	fi
	@if [ -z "$(MESSAGE)" ]; then \
		echo "Error: MESSAGE not set. Usage: make new TAG=<version> MESSAGE=\"Your tag message\""; \
		exit 1; \
	fi
	@echo "Creating tag '$(TAG)' and pushing to origin..."
	git tag -a -f $(TAG) -m "$(MESSAGE)"
	git push --force origin $(TAG)