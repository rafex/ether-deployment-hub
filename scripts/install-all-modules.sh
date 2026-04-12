#!/bin/bash

# Script para instalar todos los módulos Maven en orden estricto de dependencias
# Autor: Ether Deployment Hub
# Fecha: $(date +%Y-%m-%d)

# No salir inmediatamente en caso de error para poder capturar todos los resultados
set +e  # Desactivar salida inmediata para capturar todos los resultados
trap '' PIPE  # Ignorar SIGPIPE

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Contadores globales
TOTAL_MODULES=0
SUCCESS_MODULES=0
FAILED_MODULES=0
SKIPPED_MODULES=0

# Archivo temporal para errores
ERROR_LOG_FILE="/tmp/maven_build_errors_$$.log"
> "$ERROR_LOG_FILE"  # Limpiar archivo

# Guardar directorio raíz del repositorio
REPO_ROOT=$(pwd)

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

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_cyan() {
    echo -e "${CYAN}$1${NC}"
}

# Función para resolver el directorio del módulo Maven
resolve_module_dir() {
    local module_path="$1"
    local full_path="$REPO_ROOT/$module_path"
    
    if [ -x "$full_path/mvnw" ] && [ -f "$full_path/pom.xml" ]; then
        echo "$full_path"
    elif [ -x "$full_path/$module_path/mvnw" ] && [ -f "$full_path/$module_path/pom.xml" ]; then
        echo "$full_path/$module_path"
    else
        echo "ERROR: No se pudo resolver el directorio del módulo '$module_path'" >&2
        echo "  Buscando en: $full_path" >&2
        return 1
    fi
}

# Función para instalar un módulo
install_module() {
    local module_name="$1"
    local temp_log_file="/tmp/maven_build_${module_name}_$$.log"
    
    print_info "🔄 Instalando $module_name..."
    
    # Resolver directorio del módulo
    module_dir=$(resolve_module_dir "$module_name")
    local resolve_exit=$?
    
    if [ $resolve_exit -ne 0 ]; then
        ((TOTAL_MODULES++))
        ((FAILED_MODULES++))
        echo "MODULE:$module_name" >> "$ERROR_LOG_FILE"
        echo "ERROR:No se pudo resolver el directorio del módulo" >> "$ERROR_LOG_FILE"
        echo "END_ERROR" >> "$ERROR_LOG_FILE"
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
    
    # Ejecutar Maven y capturar salida
    # Cambiar al directorio del módulo
    cd "$module_dir" || {
        ((TOTAL_MODULES++))
        ((FAILED_MODULES++))
        echo "MODULE:$module_name" >> "$ERROR_LOG_FILE"
        echo "ERROR:No se pudo cambiar al directorio $module_dir" >> "$ERROR_LOG_FILE"
        echo "END_ERROR" >> "$ERROR_LOG_FILE"
        return 1
    }
    
    if [ -x "./mvnw" ] && [ -f "./.mvn/wrapper/maven-wrapper.properties" ]; then
        ./mvnw -B -ntp -DskipTests=true -Dgpg.skip=true clean install > "$temp_log_file" 2>&1
    else
        mvn -B -ntp -DskipTests=true -Dgpg.skip=true clean install > "$temp_log_file" 2>&1
    fi
    
    local exit_code=$?
    
    # Volver al directorio raíz del repositorio
    cd "$REPO_ROOT" || {
        print_warning "No se pudo volver al directorio raíz del repositorio"
    }
    
    ((TOTAL_MODULES++))
    
    if [ $exit_code -eq 0 ]; then
        ((SUCCESS_MODULES++))
        print_success "✅ $module_name instalado exitosamente"
        rm -f "$temp_log_file"
        return 0
    else
        ((FAILED_MODULES++))
        print_error "❌ $module_name falló (código de salida: $exit_code)"
        
        # Extraer error relevante del log
        local error_message=$(grep -A 5 "BUILD FAILURE\|ERROR\|FAILED" "$temp_log_file" | head -n 20)
        if [ -z "$error_message" ]; then
            error_message=$(tail -n 30 "$temp_log_file")
        fi
        
        # Guardar error en archivo de log
        echo "MODULE:$module_name" >> "$ERROR_LOG_FILE"
        echo "ERROR:$error_message" >> "$ERROR_LOG_FILE"
        echo "END_ERROR" >> "$ERROR_LOG_FILE"
        
        # Mostrar error en tiempo real si es el último módulo o si se desea
        print_warning "   Detalle del error:"
        echo -e "${RED}$error_message${NC}" | sed 's/^/   /'
        
        rm -f "$temp_log_file"
        return 1
    fi
}

# Función para imprimir resumen final
print_summary() {
    echo ""
    echo "╔══════════════════════════════════════════════════════════════════╗"
    echo "║                    RESUMEN DE COMPILACIÓN                        ║"
    echo "╚══════════════════════════════════════════════════════════════════╝"
    echo ""
    
    # Mostrar módulos exitosos
    if [ $SUCCESS_MODULES -gt 0 ]; then
        print_cyan "✅ MÓDULOS COMPILADOS CORRECTAMENTE ($SUCCESS_MODULES/$TOTAL_MODULES):"
        echo "   ------------------------------------------------"
        
        # Leer módulos exitosos (los que no están en el log de errores)
        for module in "${modules[@]}"; do
            if ! grep -q "MODULE:$module" "$ERROR_LOG_FILE"; then
                echo "   ✅ $module"
            fi
        done
        echo ""
    fi
    
    # Mostrar módulos fallidos
    if [ $FAILED_MODULES -gt 0 ]; then
        print_error "❌ MÓDULOS CON ERRORES ($FAILED_MODULES/$TOTAL_MODULES):"
        echo "   ------------------------------------------------"
        
        # Leer módulos fallidos del archivo de log
        local current_module=""
        while IFS= read -r line; do
            if [[ "$line" == MODULE:* ]]; then
                current_module="${line#MODULE:}"
                echo "   ❌ $current_module"
            fi
        done < "$ERROR_LOG_FILE"
        echo ""
        
        # Mostrar detalles de errores
        print_error "DETALLES DE ERRORES:"
        echo "   ==================="
        
        local current_module=""
        local error_content=""
        while IFS= read -r line; do
            if [[ "$line" == MODULE:* ]]; then
                # Mostrar error anterior si existe
                if [ -n "$current_module" ] && [ -n "$error_content" ]; then
                    echo ""
                    echo -e "${RED}❌ $current_module:${NC}"
                    echo "$error_content" | sed 's/^/   /'
                fi
                current_module="${line#MODULE:}"
                error_content=""
            elif [[ "$line" == ERROR:* ]]; then
                error_content="${line#ERROR:}"
            elif [[ "$line" == "END_ERROR" ]]; then
                # Mostrar error actual
                if [ -n "$current_module" ] && [ -n "$error_content" ]; then
                    echo ""
                    echo -e "${RED}❌ $current_module:${NC}"
                    echo "$error_content" | sed 's/^/   /'
                fi
                current_module=""
                error_content=""
            fi
        done < "$ERROR_LOG_FILE"
        echo ""
    fi
    
    # Estadísticas finales
    echo "╔══════════════════════════════════════════════════════════════════╗"
    echo "║                     ESTADÍSTICAS FINALES                         ║"
    echo "╠══════════════════════════════════════════════════════════════════╣"
    printf "║  %-30s %4d  ║\n" "Total de módulos:" "$TOTAL_MODULES"
    printf "║  %-30s %4d  ║\n" "✅ Compilados correctamente:" "$SUCCESS_MODULES"
    printf "║  %-30s %4d  ║\n" "❌ Con errores:" "$FAILED_MODULES"
    echo "╚══════════════════════════════════════════════════════════════════╝"
    echo ""
    
    # Limpiar archivo de errores
    rm -f "$ERROR_LOG_FILE"
    
    if [ $FAILED_MODULES -eq 0 ]; then
        print_success "🎉 ¡Todos los módulos se compilaron exitosamente!"
        return 0
    else
        print_error "⚠️  Algunos módulos no se pudieron compilar. Revisa los errores anteriores."
        return 1
    fi
}

# Función principal
main() {
    echo ""
    print_info "╔══════════════════════════════════════════════════════════════════╗"
    print_info "║         INICIANDO INSTALACIÓN DE TODOS LOS MÓDULOS              ║"
    print_info "╚══════════════════════════════════════════════════════════════════╝"
    echo ""
    print_info "Orden de compilación:"
    print_cyan "   parent → di → config → crypto → database-core → jdbc → database-postgres"
    print_cyan "   → json → jwt → observability-core → http-core → http-security"
    print_cyan "   → http-problem → http-openapi → http-client → logging-core → brain"
    print_cyan "   → ai-core → ai-openai → ai-deepseek → websocket-core"
    print_cyan "   → http-jetty12 → websocket-jetty12 → webhook → glowroot-jetty12"
    print_cyan "   → archetype"
    echo ""
    
    # Array de módulos en orden estricto de dependencias
    modules=(
        "ether-parent"
        "ether-di"
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
        "ether-brain"
        "ether-ai-core"
        "ether-ai-openai"
        "ether-ai-deepseek"
        "ether-websocket-core"
        "ether-http-jetty12"
        "ether-websocket-jetty12"
        "ether-webhook"
        "ether-glowroot-jetty12"
        "ether-archetype"
    )
    
    # Instalar cada módulo
    for module in "${modules[@]}"; do
        install_module "$module"
    done
    
    # Imprimir resumen
    print_summary
    exit_code=$?
    
    echo ""
    exit $exit_code
}

# Ejecutar función principal
main "$@"
