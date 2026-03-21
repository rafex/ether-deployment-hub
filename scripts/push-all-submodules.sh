#!/bin/bash

# Script para automatizar operaciones con submódulos
# Autor: Automatización de submódulos Ether
# Fecha: $(date +%Y-%m-%d)

set -e  # Salir en caso de error
trap '' PIPE  # Ignorar SIGPIPE para evitar errores con pipes en Make

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para imprimir mensajes
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Función para verificar estado de submódulos
check_submodules_status() {
    print_info "Verificando estado de submódulos..."
    
    echo ""
    print_info "=== Estado general de submódulos ==="
    git submodule status
    
    echo ""
    print_info "=== Cambios pendientes en submódulos ==="
    git submodule foreach --recursive 'echo "=== $name ===" && git status --short 2>/dev/null || echo "  (no git repo)"'
    
    echo ""
    print_info "=== Commits no pusheados ==="
    git submodule foreach --recursive 'echo "=== $name ===" && if git log --oneline origin/main..HEAD 2>/dev/null | grep -q .; then echo "  Commits ahead:" && git log --oneline origin/main..HEAD 2>/dev/null; else echo "  Synchronized"; fi'
}

# Función para hacer push en submódulos con commits pendientes
push_submodules() {
    print_info "Buscando submódulos con commits pendientes..."
    
    local submodules_to_push=()
    
    # Obtener lista de submódulos con commits pendientes
    while IFS= read -r line; do
        if [[ -n "$line" ]]; then
            submodules_to_push+=("$line")
        fi
    done < <(git submodule foreach --recursive --quiet 'if git log --oneline origin/main..HEAD 2>/dev/null | grep -q .; then echo $name; fi')
    
    if [ ${#submodules_to_push[@]} -eq 0 ]; then
        print_success "Todos los submódulos están sincronizados con sus remotos."
        return 0
    fi
    
    print_info "Encontrados ${#submodules_to_push[@]} submódulos con commits pendientes:"
    for submodule in "${submodules_to_push[@]}"; do
        echo "  - $submodule"
    done
    
    # Modo no interactivo
    if [ "$NON_INTERACTIVE" = "true" ]; then
        print_info "Modo no interactivo: procediendo automáticamente..."
        for submodule in "${submodules_to_push[@]}"; do
            print_info "Haciendo push en $submodule..."
            (cd "$submodule" && git push origin main)
            if [ $? -eq 0 ]; then
                print_success "Push completado para $submodule"
            else
                print_error "Error al hacer push en $submodule"
                return 1
            fi
        done
        print_success "Todos los pushes completados exitosamente."
        return 0
    fi
    
    # Modo interactivo
    echo ""
    print_warning "¿Deseas proceder con el push? (s/n)"
    read -r response
    
    if [[ "$response" =~ ^[Ss]$ ]]; then
        for submodule in "${submodules_to_push[@]}"; do
            print_info "Haciendo push en $submodule..."
            (cd "$submodule" && git push origin main)
            if [ $? -eq 0 ]; then
                print_success "Push completado para $submodule"
            else
                print_error "Error al hacer push en $submodule"
                return 1
            fi
        done
        
        print_success "Todos los pushes completados exitosamente."
    else
        print_info "Operación cancelada por el usuario."
    fi
}

# Función para actualizar todos los submódulos
update_all_submodules() {
    print_info "Actualizando todos los submódulos..."
    
    # Modo no interactivo
    if [ "$NON_INTERACTIVE" = "true" ]; then
        print_info "Modo no interactivo: procediendo automáticamente..."
        git submodule update --remote --recursive
        
        if [ $? -eq 0 ]; then
            print_success "Submódulos actualizados exitosamente."
            
            # Mostrar cambios
            echo ""
            print_info "=== Cambios aplicados ==="
            git status
        else
            print_error "Error al actualizar submódulos."
            return 1
        fi
        return 0
    fi
    
    # Modo interactivo
    echo ""
    print_warning "¿Deseas actualizar todos los submódulos a sus últimas versiones remotas? (s/n)"
    read -r response
    
    if [[ "$response" =~ ^[Ss]$ ]]; then
        git submodule update --remote --recursive
        
        if [ $? -eq 0 ]; then
            print_success "Submódulos actualizados exitosamente."
            
            # Mostrar cambios
            echo ""
            print_info "=== Cambios aplicados ==="
            git status
        else
            print_error "Error al actualizar submódulos."
            return 1
        fi
    else
        print_info "Operación cancelada por el usuario."
    fi
}

# Función para inicializar submódulos
init_submodules() {
    print_info "Inicializando submódulos..."
    git submodule init
    git submodule update --recursive
    
    print_success "Submódulos inicializados y actualizados."
}

# Función para limpiar submódulos (reset)
clean_submodules() {
    print_warning "ADVERTENCIA: Esta operación reseteará todos los submódulos a sus commits registrados."
    print_warning "Se perderán todos los cambios no commiteados en los submódulos."
    
    # Modo no interactivo
    if [ "$NON_INTERACTIVE" = "true" ]; then
        print_info "Modo no interactivo: procediendo automáticamente..."
        print_info "Limpiando submódulos..."
        git submodule foreach --recursive 'git reset --hard'
        git submodule update --recursive
        
        print_success "Submódulos limpiados exitosamente."
        return 0
    fi
    
    # Modo interactivo
    echo ""
    print_warning "¿Estás seguro de que deseas continuar? (s/n)"
    read -r response
    
    if [[ "$response" =~ ^[Ss]$ ]]; then
        print_info "Limpiando submódulos..."
        git submodule foreach --recursive 'git reset --hard'
        git submodule update --recursive
        
        print_success "Submódulos limpiados exitosamente."
    else
        print_info "Operación cancelada por el usuario."
    fi
}

# Función para mostrar ayuda
show_help() {
    echo "Uso: $0 [OPCIONES] [COMANDO]"
    echo ""
    echo "Opciones:"
    echo "  --non-interactive  Ejecutar en modo no interactivo (para CI/CD)"
    echo ""
    echo "Comandos disponibles:"
    echo "  status     - Verificar estado de todos los submódulos"
    echo "  push       - Hacer push en submódulos con commits pendientes"
    echo "  update     - Actualizar todos los submódulos a últimas versiones remotas"
    echo "  init       - Inicializar y actualizar todos los submódulos"
    echo "  clean      - Resetear todos los submódulos (pérdida de cambios no commiteados)"
    echo "  all        - Ejecutar status, push y update en secuencia"
    echo "  help       - Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  $0 status                    # Verificar estado"
    echo "  $0 push                      # Hacer push en submódulos pendientes"
    echo "  $0 --non-interactive push    # Push en modo no interactivo (CI/CD)"
    echo "  $0 all                       # Ejecutar flujo completo"
    echo "  $0 --non-interactive all     # Flujo completo no interactivo"
}

# Función principal para ejecutar flujo completo
run_all() {
    print_info "=== EJECUTANDO FLUJO COMPLETO DE SUBMÓDULOS ==="
    
    echo ""
    check_submodules_status
    
    echo ""
    push_submodules
    
    echo ""
    update_all_submodules
    
    echo ""
    print_info "=== FLUJO COMPLETADO ==="
    check_submodules_status
}

# Variables globales
NON_INTERACTIVE="false"

# Procesar opciones
while [[ $# -gt 0 ]]; do
    case $1 in
        --non-interactive)
            NON_INTERACTIVE="true"
            shift
            ;;
        --help|-h)
            show_help
            exit 0
            ;;
        -*)
            print_error "Opción no reconocida: $1"
            show_help
            exit 1
            ;;
        *)
            break
            ;;
    esac
done

# Manejo de comandos
COMMAND="${1:-help}"

case "$COMMAND" in
    status)
        check_submodules_status
        ;;
    push)
        push_submodules
        ;;
    update)
        update_all_submodules
        ;;
    init)
        init_submodules
        ;;
    clean)
        clean_submodules
        ;;
    all)
        run_all
        ;;
    help)
        show_help
        ;;
    *)
        print_error "Comando no reconocido: $COMMAND"
        echo ""
        show_help
        exit 1
        ;;
esac

exit 0