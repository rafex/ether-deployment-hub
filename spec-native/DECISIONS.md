# DECISIONS.md

Índice de decisiones persistentes del proyecto. Las decisiones canónicas viven
en `decisions/DEC-XXXX-<slug>.md`; no agregues contenido de decisión aquí.

## Cuando registrar aqui

Registrar una decision cuando cambie algo que futuras iniciativas
o agentes deban respetar:

- la arquitectura del sistema
- una convencion de codigo o de documentacion
- una tecnologia o dependencia base
- un tradeoff que condicione trabajo futuro

Ver `AGENTS.md` para entender la separacion semantica entre este
archivo y `SPEC.md`.

## Cuando leer este archivo

Antes de iniciar una nueva iniciativa, revisar si alguna decision
registrada condiciona el diseno o la implementacion.

## Decisiones

| ID | Estado | Título | Tags |
| --- | --- | --- | --- |
| [DEC-0001](./decisions/DEC-0001-migraci-n-de-git-submodules-a-git-subtrees.md) | accepted | Migración de Git Submodules a Git Subtrees | git, subtrees, trazabilidad |
| [DEC-0002](./decisions/DEC-0002-pipeline-de-publicaci-n-partido-en-dos-workflows.md) | accepted | Pipeline de publicación partido en dos workflows | ci, publish, maven-central, github-packages |
| [DEC-0003](./decisions/DEC-0003-chunking-de-niveles-de-despliegue.md) | accepted | Chunking de niveles de despliegue | ci, deploy, topological-sort |
| [DEC-0004](./decisions/DEC-0004-paralelismo-acotado-para-firma-gpg.md) | accepted | Paralelismo acotado para firma GPG | gpg, signing, ci |
| [DEC-0005](./decisions/DEC-0005-estrategia-dual-de-glowroot.md) | accepted | Estrategia dual de GlowRoot | glowroot, apm, jdk25 |
| [DEC-0006](./decisions/DEC-0006-target-jdk-25-lts.md) | accepted | Target JDK 25 LTS | java, jdk25 |
| [DEC-0007](./decisions/DEC-0007-versionado-independiente-y-release-policy-por-co.md) | accepted | Versionado independiente y release policy por Conventional Commits | versioning, conventional-commits, release |
| [DEC-0008](./decisions/DEC-0008-release-local-first-de-componentes.md) | accepted | Release local-first de componentes | release, local-first, cd, makefile, justfile, sops |
