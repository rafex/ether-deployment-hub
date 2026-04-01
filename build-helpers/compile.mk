.PHONY: compile validate-main-build install-all sync-manifest release-plan validate-release-plan deploy \
        compile-ether-parent compile-ether-config compile-ether-crypto compile-ether-database-core \
        compile-ether-jdbc compile-ether-database-postgres compile-ether-json compile-ether-jwt \
        compile-ether-observability-core compile-ether-logging-core compile-ether-ai-core \
        compile-ether-ai-openai compile-ether-ai-deepseek \
        compile-ether-http-core compile-ether-http-jetty12 \
        compile-ether-http-security compile-ether-http-problem \
        compile-ether-http-openapi compile-ether-http-client \
        compile-ether-websocket-core compile-ether-websocket-jetty12 \
        compile-ether-webhook compile-ether-glowroot-jetty12

define resolve_module_dir
  module_path="$(1)"; \
  if [ -x "$$module_path/mvnw" ] && [ -f "$$module_path/pom.xml" ]; then \
    echo "$$module_path"; \
  elif [ -x "$$module_path/$$module_path/mvnw" ] && [ -f "$$module_path/$$module_path/pom.xml" ]; then \
    echo "$$module_path/$$module_path"; \
  else \
    echo "Error: could not resolve Maven module dir for '$$module_path'" >&2; \
    exit 1; \
  fi
endef

define install_local_module
  module_dir="$$( $(call resolve_module_dir,$(1)) )"; \
  if [ -n "$$JAVA_HOME" ] && [ ! -x "$$JAVA_HOME/bin/java" ]; then \
    unset JAVA_HOME; \
  fi; \
  if [ -z "$$JAVA_HOME" ] && [ -x "/usr/libexec/java_home" ]; then \
    JAVA_HOME="$$(/usr/libexec/java_home -v 25 2>/dev/null || /usr/libexec/java_home)"; \
    export JAVA_HOME; \
  fi; \
  echo "Installing local module $(1) (dir: $$module_dir)"; \
  cd "$$module_dir" && \
  if [ -x "./mvnw" ] && [ -f "./.mvn/wrapper/maven-wrapper.properties" ]; then \
    ./mvnw -B -ntp -DskipTests=true -Dgpg.skip=true clean install; \
  else \
    mvn -B -ntp -DskipTests=true -Dgpg.skip=true clean install; \
  fi
endef

define compile_local_module
  module_dir="$$( $(call resolve_module_dir,$(1)) )"; \
  if [ -n "$$JAVA_HOME" ] && [ ! -x "$$JAVA_HOME/bin/java" ]; then \
    unset JAVA_HOME; \
  fi; \
  if [ -z "$$JAVA_HOME" ] && [ -x "/usr/libexec/java_home" ]; then \
    JAVA_HOME="$$(/usr/libexec/java_home -v 25 2>/dev/null || /usr/libexec/java_home)"; \
    export JAVA_HOME; \
  fi; \
  echo "Compiling module $(1) (dir: $$module_dir)"; \
  cd "$$module_dir" && \
  if [ -x "./mvnw" ] && [ -f "./.mvn/wrapper/maven-wrapper.properties" ]; then \
    ./mvnw -B -ntp -DskipTests=true clean compile; \
  else \
    mvn -B -ntp -DskipTests=true clean compile; \
  fi
endef

## compile-ether-parent: compile ether-parent
compile-ether-parent:
	@$(call compile_local_module,ether-parent)

## compile-ether-config: install parent and compile config
compile-ether-config:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-config)

## compile-ether-crypto: install parent and compile crypto
compile-ether-crypto:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-crypto)

## compile-ether-database-core: install parent and compile database-core
compile-ether-database-core:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-database-core)

## compile-ether-jdbc: install parent+database-core and compile jdbc
compile-ether-jdbc:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-database-core)
	@$(call compile_local_module,ether-jdbc)

## compile-ether-database-postgres: install parent+database-core and compile database-postgres
compile-ether-database-postgres:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-database-core)
	@$(call compile_local_module,ether-database-postgres)

## compile-ether-json: install parent and compile json
compile-ether-json:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-json)

## compile-ether-jwt: install parent+json and compile jwt
compile-ether-jwt:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-json)
	@$(call compile_local_module,ether-jwt)

## compile-ether-observability-core: install parent and compile observability-core
compile-ether-observability-core:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-observability-core)

## compile-ether-logging-core: install parent and compile logging-core
compile-ether-logging-core:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-logging-core)

## compile-ether-ai-core: install parent and compile ai-core
compile-ether-ai-core:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-ai-core)

## compile-ether-ai-openai: install parent+json+ai-core and compile ai-openai
compile-ether-ai-openai:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-json)
	@$(call install_local_module,ether-ai-core)
	@$(call compile_local_module,ether-ai-openai)

## compile-ether-ai-deepseek: install parent+json+ai-core and compile ai-deepseek
compile-ether-ai-deepseek:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-json)
	@$(call install_local_module,ether-ai-core)
	@$(call compile_local_module,ether-ai-deepseek)

## compile-ether-http-core: install parent and compile http-core
compile-ether-http-core:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-http-core)

## compile-ether-http-security: install parent and compile http-security
compile-ether-http-security:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-http-security)

## compile-ether-http-problem: install parent+json+http-core and compile http-problem
compile-ether-http-problem:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-json)
	@$(call install_local_module,ether-http-core)
	@$(call compile_local_module,ether-http-problem)

## compile-ether-http-openapi: install parent+json and compile http-openapi
compile-ether-http-openapi:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-json)
	@$(call compile_local_module,ether-http-openapi)

## compile-ether-http-client: install parent+json and compile http-client
compile-ether-http-client:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-json)
	@$(call compile_local_module,ether-http-client)

## compile-ether-http-jetty12: install all direct deps and compile jetty12
compile-ether-http-jetty12:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-config)
	@$(call install_local_module,ether-json)
	@$(call install_local_module,ether-observability-core)
	@$(call install_local_module,ether-http-core)
	@$(call install_local_module,ether-http-security)
	@$(call install_local_module,ether-http-problem)
	@$(call compile_local_module,ether-http-jetty12)

## compile-ether-websocket-core: install parent and compile websocket-core
compile-ether-websocket-core:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-websocket-core)

## compile-ether-websocket-jetty12: install parent+websocket-core and compile websocket jetty12
compile-ether-websocket-jetty12:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-websocket-core)
	@$(call compile_local_module,ether-websocket-jetty12)

## compile-ether-webhook: install parent+json+http-client and compile webhook
compile-ether-webhook:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-json)
	@$(call install_local_module,ether-http-client)
	@$(call compile_local_module,ether-webhook)

## compile-ether-glowroot-jetty12: install all upstream modules and compile glowroot-jetty12
## NOTE: glowroot depends on http-core, http-jetty12, websocket-core, websocket-jetty12 and json,
##       so it is placed last in the dependency chain.
compile-ether-glowroot-jetty12:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-json)
	@$(call install_local_module,ether-http-core)
	@$(call install_local_module,ether-websocket-core)
	@$(call install_local_module,ether-http-jetty12)
	@$(call install_local_module,ether-websocket-jetty12)
	@$(call compile_local_module,ether-glowroot-jetty12)

## compile: compile all modules in dependency order
compile: compile-ether-parent compile-ether-config compile-ether-crypto compile-ether-database-core \
         compile-ether-jdbc compile-ether-database-postgres compile-ether-json compile-ether-jwt \
         compile-ether-observability-core compile-ether-http-core compile-ether-http-security \
         compile-ether-http-problem compile-ether-http-openapi compile-ether-http-client \
         compile-ether-logging-core compile-ether-ai-core compile-ether-ai-openai compile-ether-ai-deepseek \
         compile-ether-websocket-core compile-ether-http-jetty12 compile-ether-websocket-jetty12 \
         compile-ether-webhook compile-ether-glowroot-jetty12
	@echo "Compiled all modules in dependency order."

## validate-main-build: install all modules in strict dependency order (mvn clean install -DskipTests)
## Uses install-all so each module is available in the local Maven repo before the next one compiles.
## This avoids dependency resolution failures for inter-module SNAPSHOT dependencies that are not
## yet published to Maven Central.
validate-main-build: install-all
	@echo "Local compile validation completed."

## install-all: mvn clean install for every module in strict dependency order
## Order: parent → config → crypto → database-core → jdbc → database-postgres → json → jwt → observability-core → http-core
##        → http-security → http-problem → http-openapi → http-client → logging-core
##        → ai-core → ai-openai → ai-deepseek
##        → websocket-core → http-jetty12 → websocket-jetty12 → webhook
##        → glowroot-jetty12
install-all:
	@echo "==> Installing all modules..."
	@./scripts/install-all-modules.sh

## sync-manifest: synchronize releases/manifest.json against Maven Central
sync-manifest:
	@echo "Synchronizing manifest from Maven Central..."
	@./scripts/sync-manifest-from-central.sh

## release-plan: generate release plan artifacts for BASE_REF..HEAD_REF (defaults handled by script)
release-plan:
	@echo "Generating release plan..."
	@if [ -n "$(BASE_REF)" ] || [ -n "$(HEAD_REF)" ]; then \
		./scripts/generate-release-plan.sh "$(if $(BASE_REF),$(BASE_REF),)" "$(if $(HEAD_REF),$(HEAD_REF),)"; \
	else \
		./scripts/generate-release-plan.sh; \
	fi
	@echo "Release plan generated under release-artifacts/."

## validate-release-plan: verify planned versions do not collide in Maven Central
validate-release-plan:
	@echo "Validating release plan against Maven Central..."
	@./scripts/validate-release-plan-against-central.sh

## deploy: prepare hub artifacts and validations before triggering GitHub Actions publish workflow
deploy: sync-manifest validate-main-build release-plan validate-release-plan
	@echo "Deploy preparation completed."
	@echo "Next step: trigger GitHub Actions with 'make publish-ci' or run the Publish Java Modules workflow manually."
