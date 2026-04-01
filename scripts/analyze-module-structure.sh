#!/bin/bash

# Script para analizar la estructura de módulos Maven en el repositorio
# Muestra la ubicación real de cada módulo, distinguiendo entre:
# - Submódulos de Git (dentro del repo principal)
# - Repositorios individuales (submódulos externos)

BASE_DIR="/Users/rafex/repository/github/rafex/ether/ether-deployment-hub"

echo "======================================================"
echo "ANÁLISIS DE ESTRUCTURA DE MÓDULOS MAVEN"
echo "======================================================"
echo ""

echo "📂 ESTRUCTURA DEL REPOSITORIO RAÍZ:"
echo "-----------------------------------"
ls -1 "$BASE_DIR" | grep -E "^ether-" | head -10
echo "  ... (y más módulos)"
echo ""

echo "🔍 ANÁLISIS POR MÓDULO:"
echo "======================================================"
echo ""

# Función para analizar un módulo
analyze_module() {
    local module_name="$1"
    local module_path="$2"
    
    echo "📦 MÓDULO: $module_name"
    echo "   Ruta: $module_path"
    
    # Verificar si es un submódulo de Git
    if [ -f "$module_path/.git" ]; then
        local git_content=$(cat "$module_path/.git")
        if [[ "$git_content" == gitdir:* ]]; then
            echo "   Tipo: 🏷️  SUBMÓDULO DE GIT (apunta a $git_content)"
        else
            echo "   Tipo: 📁 DIRECTORIO GIT"
        fi
    elif [ -d "$module_path/.git" ]; then
        echo "   Tipo: 📁 REPOSITORIO GIT COMPLETO"
    else
        echo "   Tipo: 📁 DIRECTORIO LOCAL"
    fi
    
    # Buscar el pom.xml (solo en ubicaciones estándar)
    local pom_path=""
    if [ -f "$module_path/pom.xml" ]; then
        pom_path="$module_path/pom.xml"
        echo "   Maven POM: ✅ Encontrado en raíz del módulo"
        echo "   Ubicación POM: $pom_path"
    elif [ -f "$module_path/ether-$module_name/pom.xml" ]; then
        pom_path="$module_path/ether-$module_name/pom.xml"
        echo "   Maven POM: ✅ Encontrado en subdirectorio ether-$module_name"
        echo "   Ubicación POM: $pom_path"
    else
        # Buscar solo en src/ (no en target/)
        local pom_search=$(find "$module_path" -path "*/target" -prune -o -name "pom.xml" -type f -print 2>/dev/null | head -1)
        if [ -n "$pom_search" ]; then
            pom_path="$pom_search"
            echo "   Maven POM: ⚠️  Encontrado en subdirectorio"
            echo "   Ubicación POM: $pom_path"
        else
            echo "   Maven POM: ❌ No encontrado"
        fi
    fi
    
    # Verificar si es POM parent
    if [ -n "$pom_path" ] && [ -f "$pom_path" ]; then
        # Usar sed en lugar de grep -oP (que no está disponible)
        local packaging=$(sed -n 's/.*<packaging>\([^<]*\)<\/packaging>.*/\1/p' "$pom_path" 2>/dev/null | head -1)
        if [ -z "$packaging" ]; then
            packaging="jar"  # Valor por defecto si no se encuentra
        fi
        
        if [ "$packaging" = "pom" ]; then
            echo "   Packaging: 📦 POM (parent)"
        else
            echo "   Packaging: 📦 $packaging"
        fi
    fi
    
    # Verificar archivos Java (excluyendo target/)
    local java_count=0
    if [ -d "$module_path/src" ]; then
        java_count=$(find "$module_path/src" -name "*.java" -type f 2>/dev/null | wc -l | tr -d ' ')
        echo "   Archivos Java: $java_count"
    elif [ -d "$module_path/ether-$module_name/src" ]; then
        java_count=$(find "$module_path/ether-$module_name/src" -name "*.java" -type f 2>/dev/null | wc -l | tr -d ' ')
        echo "   Archivos Java: $java_count"
    else
        # Buscar recursivamente (excluyendo target/)
        java_count=$(find "$module_path" -path "*/target" -prune -o -name "*.java" -type f -print 2>/dev/null | wc -l | tr -d ' ')
        if [ "$java_count" -gt 0 ]; then
            echo "   Archivos Java: $java_count (en subdirectorios)"
        else
            echo "   Archivos Java: 0"
        fi
    fi
    
    # Verificar si hay archivos de licencia (buscar en todas las ubicaciones)
    local license_found=false
    if [ -f "$module_path/LICENSE" ]; then
        license_found=true
    elif [ -f "$module_path/ether-$module_name/LICENSE" ]; then
        license_found=true
    elif [ -f "$module_path/LICENSE.txt" ]; then
        license_found=true
    elif [ -f "$module_path/ether-$module_name/LICENSE.txt" ]; then
        license_found=true
    fi
    
    if [ "$license_found" = true ]; then
        echo "   Licencia: ✅ Presente"
    else
        echo "   Licencia: ⚠️  No encontrada (usar repo raíz)"
    fi
    
    echo ""
}

# Analizar cada módulo
for module_dir in "$BASE_DIR"/ether-*; do
    if [ -d "$module_dir" ]; then
        module_name=$(basename "$module_dir")
        analyze_module "$module_name" "$module_dir"
    fi
done

echo "======================================================"
echo "RESUMEN"
echo "======================================================"
echo ""
echo "Leyenda:"
echo "  🏷️  SUBMÓDULO DE GIT - Apunta a repo externo via .git"
echo "  📁 REPOSITORIO GIT COMPLETO - Repo independiente"
echo "  📦 POM - Módulo parent de Maven"
echo "  📦 jar - Módulo compilable de Maven"
echo ""
echo "Uso: ./scripts/analyze-module-structure.sh"