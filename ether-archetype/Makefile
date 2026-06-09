# Set the directory for this project (flat structure — pom.xml is at the root)
PROJECT_DIR         := ether-archetype
PROJECT_GROUP_ID    := dev.rafex.ether
PROJECT_ARTIFACT_ID := ether-hexagonal-archetype
# Archetypes have no Java sources — skip source:jar and javadoc:jar
PRE_DEPLOY_GOALS    :=

# Include shared build logic
include ../build-helpers/common.mk
include ../build-helpers/git.mk

# Archetype has no mvnw wrapper — delegate to system mvn.
# Must cd into PROJECT_DIR first because pom.xml lives there, not at repo root.
define run_mvnw
	cd $(PROJECT_DIR) && mvn $(1)
endef
