#!/bin/bash

# Script para encontrar y mostrar la ubicación exacta de los módulos Maven
# Objetivo: Encontrar el pom.xml y mostrar la estructura de árbol donde se encuentra

BASE_DIR="/Users/rafex/repository/github/rafex/ether/ether-deployment-hub"

echo "======================================================"
echo "UBICACIÓN DE MÓDULOS MAVEN"
echo "======================================================"
echo ""
echo "Objetivo: Encontrar el archivo pom.xml de cada módulo"
echo ""

# Analizar cada módulo
for module_dir in "$BASE_DIR"/ether-*; do
    if [ ! -d "$module_dir" ]; then
        continue
    fi
    
    module_name=$(basename "$module_dir")
    
    echo "═══════════════════════════════════════════════════════════"
    echo "MÓDULO: $module_name"
    echo "═══════════════════════════════════════════════════════════"
    
    # Buscar el pom.xml en las ubicaciones conocidas
    pom_path=""
    
    # Opción 1: En el subdirectorio interno ether-<modulo>
    # Primero verificar si hay un subdirectorio con el mismo nombre
    internal_dir="$module_dir/$(ls -1 "$module_dir" | grep "^$module_name$" | head -1)"
    
    if [ -d "$internal_dir" ] && [ -f "$internal_dir/pom.xml" ]; then
        pom_path="$internal_dir/pom.xml"
        echo ""
        echo "📦 ESTRUCTURA DE ARCHIVOS:"
        echo ""
        echo "$module_name/"
        echo "├── $module_name/"
        echo "│   ├── pom.xml  ✅ (MÓDULO MAVEN ENCONTRADO)"
        echo "│   ├── src/"
        
        # Contar archivos Java
        if [ -d "$internal_dir/src" ]; then
            java_count=$(find "$internal_dir/src" -name "*.java" -type f 2>/dev/null | wc -l | tr -d ' ')
            echo "│   │   └── java/  ($java_count archivos)"
        fi
        
        # Verificar LICENSE en el directorio padre
        if [ -f "$module_dir/LICENSE" ]; then
            echo "│   └── ... (otros archivos)"
            echo "│"
            echo "└── LICENSE  (en directorio padre)"
        else
            echo "│   └── ... (otros archivos)"
            echo "│"
            echo "└── LICENSE  ❌ (no encontrado)"
        fi
        
    # Opción 2: En el directorio raíz del módulo
    elif [ -f "$module_dir/pom.xml" ]; then
        pom_path="$module_dir/pom.xml"
        echo ""
        echo "📦 ESTRUCTURA DE ARCHIVOS:"
        echo ""
        echo "$module_name/"
        echo "├── pom.xml  ✅ (MÓDULO MAVEN ENCONTRADO)"
        echo "├── src/"
        
        # Contar archivos Java
        if [ -d "$module_dir/src" ]; then
            java_count=$(find "$module_dir/src" -name "*.java" -type f 2>/dev/null | wc -l | tr -d ' ')
            echo "│   └── java/  ($java_count archivos)"
        fi
        
        # Verificar LICENSE
        if [ -f "$module_dir/LICENSE" ]; then
            echo "└── LICENSE"
        else
            echo "└── LICENSE  ❌ (no encontrado)"
        fi
        
    else
        echo ""
        echo "❌ NO SE ENCONTRÓ EL MÓDULO MAVEN (pom.xml)"
        echo ""
        echo "Estructura encontrada:"
        ls -1 "$module_dir" 2>/dev/null | head -10
    fi
    
    # Mostrar información del módulo Maven encontrado
    if [ -n "$pom_path" ]; then
        echo ""
        echo "📍 UBICACIÓN EXACTA DEL MÓDULO MAVEN:"
        echo "   $pom_path"
        echo ""
        
        # Leer información del POM
        # Buscar el artifactId del proyecto (después de <parent>)
        artifact_id=$(sed -n '/<\/parent>/,/<\/project>/p' "$pom_path" | grep -m 1 "<artifactId>" | sed 's/.*<artifactId>\([^<]*\)<\/artifactId>.*/\1/' 2>/dev/null)
        version=$(sed -n '/<\/parent>/,/<\/project>/p' "$pom_path" | grep -m 1 "<version>" | sed 's/.*<version>\([^<]*\)<\/version>.*/\1/' 2>/dev/null)
        packaging=$(sed -n '/<\/parent>/,/<\/project>/p' "$pom_path" | grep -m 1 "<packaging>" | sed 's/.*<packaging>\([^<]*\)<\/packaging>.*/\1/' 2>/dev/null)
        
        if [ -n "$artifact_id" ]; then
            echo "📦 Artifact ID: $artifact_id"
        fi
        
        if [ -n "$version" ]; then
            echo "🏷️  Version: $version"
        fi
        
        if [ -n "$packaging" ]; then
            echo "📦 Packaging: $packaging"
        elif [ -n "$artifact_id" ]; then
            echo "📦 Packaging: jar (por defecto)"
        fi
    fi
    
    echo ""
    echo ""
done

echo "======================================================"
echo "RESUMEN"
echo "======================================================"
echo ""
echo "Estructura típica encontrada:"
echo ""
echo "  ether-<modulo>/"
echo "  ├── ether-<modulo>/  ← Directorio interno con el código real"
echo "  │   ├── pom.xml     ← Archivo Maven (MÓDULO MAVEN)"
echo "  │   ├── src/        ← Código fuente"
echo "  │   └── ..."
echo "  └── LICENSE         ← Licencia (en el directorio padre)"
echo ""
echo "O también:"
echo ""
echo "  ether-<modulo>/"
echo "  ├── pom.xml         ← Archivo Maven (MÓDULO MAVEN)"
echo "  ├── src/            ← Código fuente"
echo "  └── LICENSE         ← Licencia"
echo ""
echo "Uso: ./scripts/analyze-module-structure.sh"