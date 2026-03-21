# Scripts de Automatización para Submódulos

Este directorio contiene scripts para automatizar la gestión de submódulos en el proyecto Ether Deployment Hub.

## Scripts Disponibles

### `push-all-submodules.sh`

Script principal para gestionar submódulos. Proporciona comandos para verificar estado, hacer push, actualizar y más.

#### Uso Básico

```bash
# Hacer ejecutable (solo primera vez)
chmod +x scripts/push-all-submodules.sh

# Verificar estado de todos los submódulos
./scripts/push-all-submodules.sh status

# Hacer push en submódulos con commits pendientes
./scripts/push-all-submodules.sh push

# Actualizar todos los submódulos a últimas versiones remotas
./scripts/push-all-submodules.sh update

# Ejecutar flujo completo (status + push + update)
./scripts/push-all-submodules.sh all

# Mostrar ayuda
./scripts/push-all-submodules.sh help
```

#### Comandos Disponibles

| Comando | Descripción |
|---------|-------------|
| `status` | Verificar estado de todos los submódulos |
| `push` | Hacer push en submódulos con commits pendientes |
| `update` | Actualizar submódulos a últimas versiones remotas |
| `init` | Inicializar y actualizar todos los submódulos |
| `clean` | Resetear submódulos (pérdida de cambios no commiteados) |
| `all` | Ejecutar flujo completo (status → push → update) |
| `help` | Mostrar ayuda |

#### Características

1. **Verificación detallada**: Muestra estado general, cambios pendientes y commits no pusheados
2. **Push selectivo**: Solo hace push en submódulos que realmente necesitan
3. **Confirmación interactiva**: Pide confirmación antes de operaciones destructivas
4. **Output colorizado**: Facilita la lectura de resultados
5. **Manejo de errores**: Sale automáticamente en caso de error

#### Ejemplo de Flujo de Trabajo

```bash
# 1. Verificar estado actual
./scripts/push-all-submodules.sh status

# 2. Hacer push en submódulos pendientes (con confirmación)
./scripts/push-all-submodules.sh push

# 3. Actualizar submódulos a últimas versiones
./scripts/push-all-submodules.sh update

# 4. Verificar estado final
./scripts/push-all-submodules.sh status

# Alternativa: Ejecutar todo en un solo comando
./scripts/push-all-submodules.sh all
```

#### Integración con CI/CD

Para integración en pipelines automatizados (sin confirmación interactiva):

```bash
# Modo no interactivo (asume 'yes' a todas las preguntas)
echo "y" | ./scripts/push-all-submodules.sh push
echo "y" | ./scripts/push-all-submodules.sh update
```

## Estructura de Submódulos

El proyecto contiene los siguientes submódulos (ordenados por prioridad de deploy):

### Alta Prioridad (Core)
1. `ether-parent` - POM padre del proyecto
2. `ether-json` - Utilidades JSON
3. `ether-jwt` - Manejo de JWT
4. `ether-http-core` - Core HTTP
5. `ether-websocket-core` - Core WebSocket
6. `ether-http-jetty12` - Implementación HTTP Jetty 12
7. `ether-websocket-jetty12` - Implementación WebSocket Jetty 12

### Media Prioridad (Extensiones)
- `ether-glowroot-jetty12` - Integración Glowroot
- `ether-config` - Gestión de configuración
- `ether-http-security` - Seguridad HTTP
- `ether-webhook` - Webhooks
- `ether-http-client` - Cliente HTTP
- `ether-http-problem` - Problemas HTTP (RFC 7807)
- `ether-observability-core` - Observabilidad
- `ether-http-openapi` - OpenAPI/Swagger
- `ether-database-core` - Core de base de datos
- `ether-jdbc` - JDBC utilities
- `ether-database-postgres` - PostgreSQL integration

### Nueva (AI y Logging)
- `ether-ai-core` - Core de inteligencia artificial
- `ether-ai-openai` - Integración OpenAI
- `ether-ai-deepseek` - Integración DeepSeek
- `ether-logging-core` - Logging estructurado

## Buenas Prácticas

### Para Desarrolladores

1. **Antes de commitear en el repositorio padre**:
   ```bash
   ./scripts/push-all-submodules.sh status
   ./scripts/push-all-submodules.sh push
   ```

2. **Al clonar el repositorio**:
   ```bash
   ./scripts/push-all-submodules.sh init
   ```

3. **Para sincronizar con remotos**:
   ```bash
   ./scripts/push-all-submodules.sh update
   ```

### Para CI/CD

1. **En pipelines de build**:
   ```bash
   # Inicializar submódulos
   ./scripts/push-all-submodules.sh init
   
   # Verificar que todo esté sincronizado
   ./scripts/push-all-submodules.sh status
   ```

2. **En pipelines de release**:
   ```bash
   # Asegurar que todos los submódulos estén pusheados
   echo "y" | ./scripts/push-all-submodules.sh push
   
   # Actualizar referencias en repositorio padre
   git add .
   git commit -m "chore: update submodule references for release"
   git push origin main
   ```

## Solución de Problemas

### Error: "detached HEAD"
```bash
# En el submódulo afectado
cd ether-database-postgres
git checkout main
git pull origin main
```

### Error: "non-fast-forward"
```bash
# Hacer pull primero para resolver conflictos
cd ether-database-postgres
git pull origin main --rebase
git push origin main
```

### Submódulo no inicializado
```bash
# Inicializar todos los submódulos
./scripts/push-all-submodules.sh init
```

## Referencias

- [Git Submodules Documentation](https://git-scm.com/book/en/v2/Git-Tools-Submodules)
- [Ether Deployment Hub AGENTS.md](../AGENTS.md)
- [Orden de deploy en releases/manifest.json](../releases/manifest.json)