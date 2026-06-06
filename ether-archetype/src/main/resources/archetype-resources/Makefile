# Makefile for ${rootArtifactId}
# Build system — compile, test, package, quality checks
#
# Responsibility split:
#   make  → build system  (compile, test, package, quality)
#   just  → task runner   (run, docker, db, dev lifecycle)
#   just CAN call make; make must NOT call just
#
# Requires: Maven wrapper (./mvnw)

MVN := ./mvnw

.PHONY: help build test clean format format-check quality install install-full package

help:
	@echo ""
	@echo "Build targets for ${rootArtifactId}:"
	@echo "  build         Compile all modules"
	@echo "  test          Run unit tests"
	@echo "  clean         Remove build artifacts"
	@echo "  format        Apply Spotless code formatting"
	@echo "  format-check  Check Spotless formatting (no modifications)"
	@echo "  quality       Full quality checks (spotless, checkstyle, owasp)"
	@echo "  install       Install all modules to local Maven repo (skip tests)"
	@echo "  install-full  Install with tests"
	@echo "  package       Package transport-jetty as fat jar"
	@echo ""
	@echo "  For application lifecycle (run, docker, db...) use: just --list"
	@echo ""

build:
	$(MVN) compile

test:
	$(MVN) test

clean:
	$(MVN) clean

format:
	$(MVN) spotless:apply

format-check:
	$(MVN) spotless:check

quality:
	$(MVN) -Pquality verify

install:
	$(MVN) clean install -DskipTests

install-full:
	$(MVN) clean install

package:
	$(MVN) clean package -pl ${rootArtifactId}-transport-jetty -am
