#!/bin/bash

# Script para instalar todos los módulos Maven en orden estricto de dependencias
# Autor: Ether Deployment Hub
# Fecha: $(date +%Y-%m-%d)

set -e  # Salir en caso de error
trap '' PIPE  # Ignorar SIGPIPE

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funciones de logging
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Función para resolver el directorio del módulo Maven
resolve_module_dir() {
    local module_path="$1"
    
    if [ -x "$module_path/mvnw" ] && [ -f "$module_path/pom.xml" ]; then
        echo "$module_path"
    elif [ -x "$module_path/$module_path/mvnw" ] && [ -f "$module_path/$module_path/pom.xml" ]; then
        echo "$module_path/$module_path"
    else
        print_error "No se pudo resolver el directorio del módulo '$module_path'"
        return 1
    fi
}

# Función para instalar un módulo
install_module() {
    local module_name="$1"
    
    print_info "Instalando $module_name..."
    
    # Resolver directorio del módulo
    module_dir=$(resolve_module_dir "$module_name")
    if [ $? -ne 0 ]; then
        return 1
    fi
    
    # Configurar JAVA_HOME para Java 25
    if [ -n "$JAVA_HOME" ] && [ ! -x "$JAVA_HOME/bin/java" ]; then
        unset JAVA_HOME
    fi
    if [ -z "$JAVA_HOME" ] && [ -x "/usr/libexec/java_home" ]; then
        JAVA_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null || /usr/libexec/java_home)"
        export JAVA_HOME
    fi
    
    # Ejecutar Maven
    cd "$module_dir"
    if [ -x "./mvnw" ] && [ -f "./.mvn/wrapper/maven-wrapper.properties" ]; then
        ./mvnw -B -ntp -DskipTests=true -Dgpg.skip=true clean install
    else
        mvn -B -ntp -DskipTests=true -Dgpg.skip=true clean install
    fi
    
    if [ $? -eq 0 ]; then
        print_success "$module_name instalado exitosamente"
        return 0
    else
        print_error "Falló la instalación de $module_name"
        return 1
    fi
}

# Función principal
main() {
    print_info "=== INICIANDO INSTALACIÓN DE TODOS LOS MÓDULOS ==="
    print_info "Orden: parent → config → crypto → database-core → jdbc → database-postgres → json → jwt → observability-core → http-core → http-security → http-problem → http-openapi → http-client → logging-core → ai-core → ai-openai → ai-deepseek → websocket-core → http-jetty12 → websocket-jetty12 → webhook → glowroot-jetty12"
    
    # Array de módulos en orden estricto de dependencias
    local modules=(
        "ether-parent"
        "ether-config"
        "ether-crypto"
        "ether-database-core"
        "ether-jdbc"
        "ether-database-postgres"
        "ether-json"
        "ether-jwt"
        "ether-observability-core"
        "ether-http-core"
        "ether-http-security"
        "ether-http-problem"
        "ether-http-openapi"
        "ether-http-client"
        "ether-logging-core"
        "ether-ai-core"
        "ether-ai-openai"
        "ether-ai-deepseek"
        "ether-websocket-core"
        "ether-http-jetty12"
        "ether-websocket-jetty12"
        "ether-webhook"
        "ether-glowroot-jetty12"
    )
    
    # Instalar cada módulo
    for module in "${modules[@]}"; do
        if ! install_module "$module"; then
            print_error "La instalación falló en el módulo: $module"
            exit 1
        fi
    done
    
    print_success "Todos los módulos instalados exitosamente en el repositorio local de Maven"
}

# Ejecutar función principal
main "$@"
