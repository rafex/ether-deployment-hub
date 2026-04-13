# Guía práctica: ether-http-security

**ether-http-security** provee políticas de seguridad HTTP composables: CORS, cabeceras de seguridad,
filtrado por IP, proxies de confianza y rate limiting.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.http.security</groupId>
    <artifactId>ether-http-security</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## `HttpSecurityProfile` — perfil de seguridad completo

```java
// Perfil por defecto (para desarrollo/testing)
HttpSecurityProfile profile = HttpSecurityProfile.defaults();

// Perfil personalizado
HttpSecurityProfile profile = new HttpSecurityProfile(
    corsPolicy,
    securityHeadersPolicy,
    trustedProxies,
    ipPolicy,
    rateLimitPolicy
);
```

---

## CORS — `CorsPolicy`

```java
CorsPolicy cors = CorsPolicy.builder()
    .allowOrigins("https://app.example.com", "https://admin.example.com")
    .allowMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowHeaders("Authorization", "Content-Type", "X-Request-Id")
    .exposeHeaders("X-Total-Count", "X-Request-Id")
    .allowCredentials(true)
    .maxAge(Duration.ofHours(1))
    .build();
```

---

## Cabeceras de seguridad — `SecurityHeadersPolicy`

Inyecta automáticamente cabeceras como `Strict-Transport-Security`, `X-Content-Type-Options`,
`X-Frame-Options`, `Content-Security-Policy`:

```java
SecurityHeadersPolicy headers = SecurityHeadersPolicy.builder()
    .strictTransportSecurity("max-age=31536000; includeSubDomains")
    .contentTypeOptions("nosniff")
    .frameOptions("DENY")
    .contentSecurityPolicy("default-src 'self'")
    .referrerPolicy("strict-origin-when-cross-origin")
    .build();
```

---

## Filtrado por IP — `IpPolicy`

```java
// Lista blanca: solo estas IPs pueden acceder
IpPolicy allowList = IpPolicy.allowList(
    "10.0.0.0/8",
    "192.168.1.0/24",
    "203.0.113.5"
);

// Lista negra: bloquear estas IPs
IpPolicy blockList = IpPolicy.blockList("185.220.101.0/24");
```

---

## Proxies de confianza — `TrustedProxyPolicy`

Configura qué proxies pueden establecer cabeceras `X-Forwarded-For` y `X-Real-IP`:

```java
TrustedProxyPolicy proxies = TrustedProxyPolicy.of(
    "10.0.0.1",      // load balancer interno
    "172.16.0.0/12"  // rango de proxies corporativos
);
```

---

## Rate limiting — `RateLimitPolicy`

```java
// Global: 1000 req/min para toda la aplicación
RateLimitPolicy global = RateLimitPolicy.global(1000, Duration.ofMinutes(1));

// Por IP: 100 req/min por dirección IP
RateLimitPolicy perIp = RateLimitPolicy.perIp(100, Duration.ofMinutes(1));

// Por usuario (requiere auth previa): 500 req/min por usuario
RateLimitPolicy perUser = RateLimitPolicy.perUser(500, Duration.ofMinutes(1));
```

---

## Integración con ether-http-jetty12

```java
public class AppContainer {

    public HttpSecurityProfile securityProfile() {
        return new HttpSecurityProfile(
            CorsPolicy.builder()
                .allowOrigins("https://app.example.com")
                .allowMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .build(),
            SecurityHeadersPolicy.builder()
                .strictTransportSecurity("max-age=31536000")
                .contentTypeOptions("nosniff")
                .frameOptions("DENY")
                .build(),
            TrustedProxyPolicy.of("10.0.0.0/8"),
            IpPolicy.allowAll(),
            RateLimitPolicy.perIp(200, Duration.ofMinutes(1))
        );
    }
}
```

---

## Más información

- [Guía ether-http-core](ether-http-core.md) — contratos `HttpExchange` y `Middleware`
- [Guía ether-http-jetty12](ether-http-jetty12.md) — integración con el servidor Jetty
- [Guía ether-jwt](ether-jwt.md) — autenticación JWT
- [Javadoc API](../api/doxygen/html/index.html)
