#!/bin/bash

# Script para verificar el estado de todos los submódulos en el repositorio Ether-Hub
# Verifica:
# 1. Cambios pendientes en working directory
# 2. Sincronización con origin/main
# 3. Commits pendientes de push/pull
# 4. Presencia de licencias MIT en archivos Java

BASE_DIR="/Users/rafex/repository/github/rafex/ether/ether-deployment-hub"
MODULES=(
    "ether-ai-core"
    "ether-ai-deepseek"
    "ether-ai-openai"
    "ether-archetype"
    "ether-config"
    "ether-crypto"
    "ether-database-core"
    "ether-database-postgres"
    "ether-glowroot-jetty12"
    "ether-http-client"
    "ether-http-core"
    "ether-http-jetty12"
    "ether-http-openapi"
    "ether-http-problem"
    "ether-http-security"
    "ether-jdbc"
    "ether-json"
    "ether-jwt"
    "ether-logging-core"
    "ether-observability-core"
    "ether-parent"
    "ether-webhook"
    "ether-websocket-core"
    "ether-websocket-jetty12"
)

echo "======================================================"
echo "VERIFICACIÓN DE SUBMÓDULOS"
echo "======================================================"
echo ""

for module in "${MODULES[@]}"; do
    echo "🔍 $module"
    echo "----------------------------------------"
    
    # Verificar si hay cambios pendientes
    cd "$BASE_DIR/$module"
    STATUS=$(git status --porcelain)
    
    if [ -n "$STATUS" ]; then
        echo "  ❌ CAMBIOS PENDIENTES:"
        echo "$STATUS" | sed 's/^/     /'
    else
        echo "  ✅ Sin cambios en working directory"
    fi
    
    # Verificar si está sync con origin/main
    git fetch origin main > /dev/null 2>&1
    LOCAL=$(git rev-parse main 2>/dev/null || echo "N/A")
    REMOTE=$(git rev-parse origin/main 2>/dev/null || echo "N/A")
    
    if [ "$LOCAL" = "$REMOTE" ]; then
        echo "  ✅ Sync con origin/main"
    else
        echo "  ⚠️  Diferente de origin/main:"
        echo "     Local:  $LOCAL"
        echo "     Remoto: $REMOTE"
    fi
    
    # Verificar si hay commits pendientes de push
    BEHIND=$(git rev-list --count origin/main..main 2>/dev/null || echo "0")
    AHEAD=$(git rev-list --count main..origin/main 2>/dev/null || echo "0")
    
    if [ "$BEHIND" -gt 0 ]; then
        echo "  ⚠️  $BEHIND commit(s) para pull desde origin/main"
    fi
    
    if [ "$AHEAD" -gt 0 ]; then
        echo "  ⚠️  $AHEAD commit(s) para push a origin/main"
    fi
    
    if [ "$BEHIND" -eq 0 ] && [ "$AHEAD" -eq 0 ]; then
        echo "  ✅ No hay commits pendientes de push/pull"
    fi
    
    # Verificar presencia de licencias MIT (excepto ether-parent que es POM)
    if [ "$module" != "ether-parent" ]; then
        # Buscar archivos Java con licencia recursivamente en todo el directorio del submódulo
        if find . -name "*.java" -type f -exec grep -l "Copyright (C)" {} \; 2>/dev/null | head -1 > /dev/null; then
            echo "  ✅ Licencia MIT encontrada en archivos Java"
        else
            echo "  ❌ Licencia MIT NO encontrada en archivos Java"
        fi
    else
        echo "  ℹ️  Módulo POM (sin código Java)"
    fi
    
    echo ""
done

echo "======================================================"
echo "VERIFICACIÓN COMPLETADA"
echo "======================================================"
echo ""
echo "Resumen:"
echo "- Todos los submódulos deberían estar sync con origin/main"
echo "- No debería haber cambios pendientes en working directory"
echo "- Todos los archivos Java deberían tener licencia MIT"
echo ""
echo "Uso: ./scripts/verify-submodules.sh"