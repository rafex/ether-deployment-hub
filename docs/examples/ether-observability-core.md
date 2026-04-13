# Guía práctica: ether-observability-core

**ether-observability-core** provee contratos para health checks (liveness/readiness),
generación de request IDs y registro de tiempos de operación, sin dependencias externas.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.observability.core</groupId>
    <artifactId>ether-observability-core</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## Health checks — `ProbeCheck` y `ProbeAggregator`

### Implementar una sonda

```java
// ProbeCheck es una interfaz funcional
ProbeCheck dbCheck = () -> {
    try {
        db.query(SqlQuery.of("SELECT 1"), rs -> null);
        return ProbeResult.up("database");
    } catch (Exception e) {
        return ProbeResult.down("database", e.getMessage());
    }
};

ProbeCheck cacheCheck = () -> {
    boolean ok = cache.ping();
    return ok
        ? ProbeResult.up("cache")
        : ProbeResult.degraded("cache", "Cache lento o no disponible");
};
```

### Agregar sondas

```java
List<ProbeCheck> checks = List.of(dbCheck, cacheCheck);

// Liveness — ¿está viva la JVM/proceso?
ProbeReport liveness  = ProbeAggregator.aggregate(ProbeKind.LIVENESS, checks);

// Readiness — ¿puede atender tráfico?
ProbeReport readiness = ProbeAggregator.aggregate(ProbeKind.READINESS, checks);
```

### Interpretar el resultado

```java
ProbeReport report = ProbeAggregator.aggregate(ProbeKind.READINESS, checks);

ProbeStatus overall = report.status();   // UP, DOWN, DEGRADED
ProbeKind   kind    = report.kind();     // LIVENESS o READINESS

for (ProbeResult r : report.results()) {
    System.out.printf("%s → %s (%s)%n", r.name(), r.status(), r.detail());
}

// Mapa nombre → estado
Map<String, ProbeStatus> byName = report.byName();
// {"database": UP, "cache": DEGRADED}
```

### `ProbeStatus`

| Estado | Significado |
|---|---|
| `UP` | El componente funciona correctamente |
| `DOWN` | El componente no responde — impacto crítico |
| `DEGRADED` | Funcionamiento parcial — impacto no crítico |

---

## `RequestIdGenerator` — IDs de correlación

```java
RequestIdGenerator gen = new UuidRequestIdGenerator();
String requestId = gen.generate();
// "550e8400-e29b-41d4-a716-446655440000"

// Usado en middleware de logging
LOG.info("Procesando request id={}", requestId);
```

---

## `TimingRecorder` — medir duración de operaciones

```java
TimingRecorder recorder = /* inyectado */;

TimingSample sample = recorder.start("user.create");
try {
    userService.create(request);
} finally {
    recorder.stop(sample); // registra duración
}
```

---

## Exponer endpoints de salud con ether-http-jetty12

```java
// ether-http-jetty12 usa ProbeCheck internamente en EnhancedHealthHandler
// Solo necesitas registrar tus checks en el contenedor:
public class AppContainer {

    public List<ProbeCheck> livenessChecks() {
        return List.of(() -> ProbeResult.up("app"));
    }

    public List<ProbeCheck> readinessChecks() {
        return List.of(dbCheck, cacheCheck);
    }
}
```

Los endpoints `/health/live` y `/health/ready` devuelven JSON con el `ProbeReport`.

---

## Más información

- [Guía ether-http-jetty12](ether-http-jetty12.md) — endpoints de salud integrados
- [Javadoc API](../api/doxygen/html/index.html)
