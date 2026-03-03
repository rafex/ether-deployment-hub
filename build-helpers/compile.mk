.PHONY: validate-main-build compile-ether-parent compile-ether-json compile-ether-jwt compile-ether-http-core compile-ether-http-jetty12

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
  echo "Installing local module $(1) (dir: $$module_dir)"; \
  cd "$$module_dir" && ./mvnw -B -ntp -DskipTests=true -Dgpg.skip=true clean install
endef

define compile_local_module
  module_dir="$$( $(call resolve_module_dir,$(1)) )"; \
  echo "Compiling module $(1) (dir: $$module_dir)"; \
  cd "$$module_dir" && ./mvnw -B -ntp -DskipTests=true clean compile
endef

## compile-ether-parent: compile ether-parent
compile-ether-parent:
	@$(call compile_local_module,ether-parent)

## compile-ether-json: install parent and compile json
compile-ether-json:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-json)

## compile-ether-jwt: install parent+json and compile jwt
compile-ether-jwt:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-json)
	@$(call compile_local_module,ether-jwt)

## compile-ether-http-core: install parent and compile http-core
compile-ether-http-core:
	@$(call install_local_module,ether-parent)
	@$(call compile_local_module,ether-http-core)

## compile-ether-http-jetty12: install parent+http-core and compile jetty12
compile-ether-http-jetty12:
	@$(call install_local_module,ether-parent)
	@$(call install_local_module,ether-http-core)
	@$(call compile_local_module,ether-http-jetty12)

## validate-main-build: replicate CI local compile validations for main
validate-main-build: compile-ether-parent compile-ether-json compile-ether-jwt compile-ether-http-core compile-ether-http-jetty12
	@echo "Local compile validation completed."
