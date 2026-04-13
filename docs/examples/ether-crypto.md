# Guía práctica: ether-crypto

**ether-crypto** provee hashing y verificación de contraseñas con PBKDF2 sobre
`javax.crypto`, sin dependencias externas.

## Instalación

```xml
<dependency>
    <groupId>dev.rafex.ether.crypto</groupId>
    <artifactId>ether-crypto</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

---

## `PasswordHasher` — interfaz principal

```java
public interface PasswordHasher {
    PasswordHash hash(char[] password, byte[] salt, int iterations);
    boolean verify(char[] password, byte[] salt, int iterations, byte[] expectedHash);
}
```

---

## Uso básico con PBKDF2

```java
PasswordHasher hasher = new PasswordHasherPBKDF2();

// Generar sal aleatoria
byte[] salt = new byte[16];
new SecureRandom().nextBytes(salt);

// Hashear contraseña (limpiar array al terminar)
char[] password = "mi-contraseña-segura".toCharArray();
PasswordHash hash = hasher.hash(password, salt, 310_000);
Arrays.fill(password, '\0');

// Guardar en BD: hash.value() + salt (en Base64 o hex)
byte[] storedHash = hash.value();
```

---

## Verificación

```java
// Al hacer login — recuperar salt y hash de la BD
char[] inputPassword = request.password().toCharArray();
boolean valid = hasher.verify(inputPassword, storedSalt, 310_000, storedHash);
Arrays.fill(inputPassword, '\0');

if (!valid) {
    throw new UnauthorizedException("Contraseña incorrecta");
}
```

---

## Integración con ether-di

```java
public class SecurityContainer {

    private final Lazy<PasswordHasher> hasher = new Lazy<>(PasswordHasherPBKDF2::new);

    public PasswordHasher passwordHasher() { return hasher.get(); }
}
```

---

## Buenas prácticas

- Usa al menos **310 000 iteraciones** (recomendación OWASP 2023 para PBKDF2-HMAC-SHA256)
- Genera sal con `SecureRandom`, nunca con `Random`
- Limpia los arrays de contraseña con `Arrays.fill(password, '\0')` inmediatamente tras usar
- Guarda sal y hash por separado (o codificados juntos en Base64)

---

## Más información

- [Javadoc API](../api/doxygen/html/index.html)
- [Código fuente](https://github.com/rafex/ether-crypto)
