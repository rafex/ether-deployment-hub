

# Usage: make new TAG=<version>


.PHONY: new

## new: create or update a Git tag and push it forcefully to origin
new:
	@if [ -z "$(TAG)" ]; then \
		echo "Error: TAG not set. Usage: make new TAG=<version>"; \
		exit 1; \
	fi
	@echo "Creating tag '$(TAG)' and pushing to origin..."
	git tag -f $(TAG)
	git push --force origin $(TAG)