# ether-crypto

Primitivas criptográficas ligeras para el ecosistema Ether. El módulo arranca con hashing de passwords basado en PBKDF2-HMAC-SHA256 y queda organizado para incorporar después HMAC, firmas, key derivation y generación de tokens aleatorios sin mezclarlo con transporte HTTP o lógica de dominio.

Compila bajo el `ether-parent` actual, alineado con **Java 25** y la línea de versión **`9.0.0-SNAPSHOT`** del resto de módulos del ecosistema.

## Maven Dependency

```xml
<dependency>
    <groupId>dev.rafex.ether.crypto</groupId>
    <artifactId>ether-crypto</artifactId>
    <version>9.0.0-SNAPSHOT</version>
</dependency>
```

## Package Overview

| Package | Purpose |
|---|---|
| `dev.rafex.ether.crypto` | Entry points and shared crypto APIs |
| `dev.rafex.ether.crypto.password` | Password hashing and verification |

La separación semántica actual deja la base para crecer con subpaquetes como `hmac`, `signature`, `kdf` y `token` sin mezclar responsabilidades.

## Current Features

- `PasswordHasher` defines the contract for password hashing and verification.
- `PasswordHasherPBKDF2` ports the PBKDF2 logic used in Kiwi and HouseDB.
- Constant-time verification via `MessageDigest.isEqual`.
- JDK-only implementation using `SecretKeyFactory` and `PBEKeySpec`.

## Example

```java
import dev.rafex.ether.crypto.password.PasswordHasherPBKDF2;

var hasher = new PasswordHasherPBKDF2(32);

byte[] salt = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
int iterations = 120_000;

var result = hasher.hash("correct horse battery staple".toCharArray(), salt, iterations);
boolean valid = hasher.verify(
    "correct horse battery staple".toCharArray(),
    result.salt(),
    result.iterations(),
    result.hash()
);
```

## Roadmap

- HMAC helpers
- Signature helpers
- Generic key derivation utilities
- Secure random token generators
