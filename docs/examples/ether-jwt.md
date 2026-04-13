# Guía práctica: ether-jwt

**ether-jwt** provee emisión y verificación de JWT a través de las interfaces `TokenIssuer`
y `TokenVerifier`, con modelos inmutables `TokenSpec` y `TokenClaims`.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.jwt</groupId>
    <artifactId>ether-jwt</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## Emitir un token

```java
TokenIssuer issuer = new DefaultTokenIssuer(config);

String token = issuer.issue(TokenSpec.builder()
    .subject("user-123")
    .issuer("my-app")
    .audience("api")
    .ttl(Duration.ofHours(1))
    .roles("USER", "ADMIN")
    .tokenType(TokenType.ACCESS)
    .claim("tenantId", "acme")
    .build());
```

---

## Verificar un token

```java
TokenVerifier verifier = new DefaultTokenVerifier(config);

VerificationResult result = verifier.verify(token, Instant.now());

if (result.isValid()) {
    TokenClaims claims = result.claims();
    String subject = claims.subject();
    List<String> roles = claims.roles();
    String tenantId = (String) claims.extras().get("tenantId");
} else {
    // Token expirado, firma inválida, etc.
    throw new UnauthorizedException("Token inválido");
}
```

---

## `TokenSpec` — especificación del token

```java
TokenSpec spec = TokenSpec.builder()
    .subject("user-123")          // sub
    .issuer("my-app")             // iss
    .audience("api")              // aud
    .ttl(Duration.ofMinutes(30))  // exp = now + ttl
    .notBefore(Instant.now())     // nbf
    .jwtId(UUID.randomUUID().toString()) // jti
    .roles("USER")                // claim personalizado "roles"
    .tokenType(TokenType.REFRESH) // tipo de token
    .clientId("mobile-app")       // cliente OAuth
    .claim("orgId", "org-456")    // claim arbitrario
    .build();

TokenClaims claims = spec.claims(); // acceso antes de firmar
```

---

## `TokenClaims` — claims extraídos

```java
TokenClaims claims = result.claims();

String          subject    = claims.subject();
String          issuer     = claims.issuer();
String          audience   = claims.audience();
Instant         expiresAt  = claims.expiresAt();
Instant         issuedAt   = claims.issuedAt();
String          jwtId      = claims.jwtId();
List<String>    roles      = claims.roles();
TokenType       type       = claims.tokenType();
String          clientId   = claims.clientId();
Map<String,?>   extras     = claims.extras(); // claims adicionales
```

---

## Tipos de token y algoritmos

```java
// TokenType — tipo semántico del token
TokenType.ACCESS   // token de acceso (corta duración)
TokenType.REFRESH  // token de refresco (larga duración)
TokenType.ID       // token de identidad (OIDC)

// AlgorithmType — algoritmo de firma
AlgorithmType.HS256  // HMAC-SHA256 (clave simétrica)
AlgorithmType.RS256  // RSA-SHA256 (clave pública/privada)
AlgorithmType.ES256  // ECDSA-SHA256

// SignType — mecanismo de firma
SignType.SYMMETRIC   // clave compartida
SignType.ASYMMETRIC  // par de claves
```

---

## Integración con ether-di

```java
public class SecurityContainer {

    private final Lazy<TokenIssuer>   issuer   = new Lazy<>(() ->
            new DefaultTokenIssuer(config.get()));
    private final Lazy<TokenVerifier> verifier = new Lazy<>(() ->
            new DefaultTokenVerifier(config.get()));

    public TokenIssuer   tokenIssuer()   { return issuer.get(); }
    public TokenVerifier tokenVerifier() { return verifier.get(); }
}
```

---

## Más información

- [Guía ether-http-security](ether-http-security.md) — uso del JWT en el pipeline HTTP
- [Javadoc API](../api/doxygen/html/index.html)
- [Código fuente](https://github.com/rafex/ether-jwt)
