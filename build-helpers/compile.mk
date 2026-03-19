.PHONY: compile validate-main-build install-all \
        compile-ether-parent compile-ether-config compile-ether-database-core \
        compile-ether-jdbc compile-ether-database-postgres compile-ether-json compile-ether-jwt \
        compile-ether-observability-core \
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
    JAVA_HOME="$$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home)"; \
    export JAVA_HOME; \
  fi; \
  echo "Installing local module $(1) (dir: $$module_dir)"; \
  cd "$$module_dir" && ./mvnw -B -ntp -DskipTests=true -Dgpg.skip=true clean install
endef

define compile_local_module
  module_dir="$$( $(call resolve_module_dir,$(1)) )"; \
  if [ -n "$$JAVA_HOME" ] && [ ! -x "$$JAVA_HOME/bin/java" ]; then \
    unset JAVA_HOME; \
  fi; \
  if [ -z "$$JAVA_HOME" ] && [ -x "/usr/libexec/java_home" ]; then \
    JAVA_HOME="$$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home)"; \
    export JAVA_HOME; \
  fi; \
  echo "Compiling module $(1) (dir: $$module_dir)"; \
  cd "$$module_dir" && ./mvnw -B -ntp -DskipTests=true clean compile
endef

## compile-ether-parent: compile ether-parent
compile-ether-parent:
	@$(call compile_local_module,ether-parent)

## compile-ether-config: install parent and compile config
compile-ether-config:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-config)

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

## compile-ether-http-jetty12: install parent+http-core and compile jetty12
compile-ether-http-jetty12:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-json)
	@$(call install_local_module,ether-http-core)
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
compile: compile-ether-parent compile-ether-config compile-ether-database-core \
         compile-ether-jdbc compile-ether-database-postgres compile-ether-json compile-ether-jwt \
         compile-ether-observability-core compile-ether-http-core compile-ether-http-security \
         compile-ether-http-problem compile-ether-http-openapi compile-ether-http-client \
         compile-ether-websocket-core compile-ether-http-jetty12 compile-ether-websocket-jetty12 \
         compile-ether-webhook compile-ether-glowroot-jetty12
	@echo "Compiled all modules in dependency order."

## validate-main-build: replicate CI local compile validations for main
validate-main-build: compile-ether-parent compile-ether-config compile-ether-database-core \
                     compile-ether-jdbc compile-ether-database-postgres compile-ether-json compile-ether-jwt \
                     compile-ether-observability-core compile-ether-http-core compile-ether-http-security \
                     compile-ether-http-problem compile-ether-http-openapi compile-ether-http-client \
                     compile-ether-websocket-core compile-ether-http-jetty12 compile-ether-websocket-jetty12 \
                     compile-ether-webhook compile-ether-glowroot-jetty12
	@echo "Local compile validation completed."

## install-all: mvn clean install for every module in strict dependency order
## Order: parent → config → database-core → jdbc → database-postgres → json → jwt → observability-core → http-core
##        → http-security → http-problem → http-openapi → http-client
##        → websocket-core → http-jetty12 → websocket-jetty12 → webhook
##        → glowroot-jetty12
install-all:
	@echo "==> Installing ether-parent"
	@$(call install_local_module,ether-parent)
	@echo "==> Installing ether-config"
	@$(call install_local_module,ether-config)
	@echo "==> Installing ether-database-core"
	@$(call install_local_module,ether-database-core)
	@echo "==> Installing ether-jdbc"
	@$(call install_local_module,ether-jdbc)
	@echo "==> Installing ether-database-postgres"
	@$(call install_local_module,ether-database-postgres)
	@echo "==> Installing ether-json"
	@$(call install_local_module,ether-json)
	@echo "==> Installing ether-jwt"
	@$(call install_local_module,ether-jwt)
	@echo "==> Installing ether-observability-core"
	@$(call install_local_module,ether-observability-core)
	@echo "==> Installing ether-http-core"
	@$(call install_local_module,ether-http-core)
	@echo "==> Installing ether-http-security"
	@$(call install_local_module,ether-http-security)
	@echo "==> Installing ether-http-problem"
	@$(call install_local_module,ether-http-problem)
	@echo "==> Installing ether-http-openapi"
	@$(call install_local_module,ether-http-openapi)
	@echo "==> Installing ether-http-client"
	@$(call install_local_module,ether-http-client)
	@echo "==> Installing ether-websocket-core"
	@$(call install_local_module,ether-websocket-core)
	@echo "==> Installing ether-http-jetty12"
	@$(call install_local_module,ether-http-jetty12)
	@echo "==> Installing ether-websocket-jetty12"
	@$(call install_local_module,ether-websocket-jetty12)
	@echo "==> Installing ether-webhook"
	@$(call install_local_module,ether-webhook)
	@echo "==> Installing ether-glowroot-jetty12"
	@$(call install_local_module,ether-glowroot-jetty12)
	@echo "All modules installed to local Maven repository."
