// ─────────────────────────────────────────────────────────────────────────────
// Post-generation script — executed by Maven Archetype Plugin after project
// creation. Handles tasks that the plugin cannot do natively:
//   1. chmod +x mvnw  (plugin does not preserve file permissions)
//   2. Write all .gitignore files (plugin silently skips dot-files)
// ─────────────────────────────────────────────────────────────────────────────

def projectDir = new File(request.outputDirectory, request.artifactId)

// ── 1. Make mvnw executable ───────────────────────────────────────────────────
def mvnw = new File(projectDir, "mvnw")
if (mvnw.exists()) {
    mvnw.setExecutable(true, false)
    println "[archetype] chmod +x mvnw — done"
}

// ── 2. Root .gitignore ────────────────────────────────────────────────────────
new File(projectDir, ".gitignore").text = """\
# ── Maven ─────────────────────────────────────────────────────────────────────
target/
.flattened-pom.xml
release.properties
dependency-reduced-pom.xml

# Maven wrapper downloaded artifacts (jar is fetched automatically on first run)
.mvn/wrapper/maven-wrapper.jar
.mvn/timing.properties

# ── IDE — IntelliJ IDEA ───────────────────────────────────────────────────────
.idea/
*.iml
*.iws
*.ipr
out/

# ── IDE — Eclipse ─────────────────────────────────────────────────────────────
.classpath
.project
.settings/
bin/

# ── IDE — VS Code ─────────────────────────────────────────────────────────────
.vscode/
!.vscode/extensions.json

# ── IDE — NetBeans ────────────────────────────────────────────────────────────
nbproject/
nbbuild/
nbdist/
nb-configuration.xml

# ── Logs ──────────────────────────────────────────────────────────────────────
*.log
logs/

# ── Environment & secrets ─────────────────────────────────────────────────────
.env
.env.*
!.env.example
*.pem
*.key
*.p12
*.jks

# ── Docker ────────────────────────────────────────────────────────────────────
docker-compose.override.yml

# ── OWASP / Security reports ──────────────────────────────────────────────────
dependency-check-report.*

# ── Runtime / PID ─────────────────────────────────────────────────────────────
*.pid
*.pid.lock

# ── OS ────────────────────────────────────────────────────────────────────────
.DS_Store
.DS_Store?
._*
.Spotlight-V7
.Trashes
ehthumbs.db
Thumbs.db
"""
println "[archetype] .gitignore — done"

// ── 3. .mvn/wrapper/.gitignore ────────────────────────────────────────────────
def wrapperDir = new File(projectDir, ".mvn/wrapper")
wrapperDir.mkdirs()
new File(wrapperDir, ".gitignore").text = """\
# Maven Wrapper — ignore the downloaded JAR (fetched automatically on first run)
maven-wrapper.jar
MavenWrapperDownloader.java
"""
println "[archetype] .mvn/wrapper/.gitignore — done"
