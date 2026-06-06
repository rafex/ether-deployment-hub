package dev.rafex.ether.crypto.password;

/*-
 * #%L
 * ether-crypto
 * %%
 * Copyright (C) 2025 - 2026 Raúl Eduardo González Argote
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Arrays;

/**
 * PBKDF2-HMAC-SHA256 password hasher ported from Kiwi and HouseDB.
 */
public final class PasswordHasherPBKDF2 implements PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private final int derivedKeyBytes;

    /**
     * Creates a new PBKDF2 password hasher.
     *
     * @param derivedKeyBytes The number of bytes for the derived key (minimum 16).
     * @throws IllegalArgumentException if {@code derivedKeyBytes} is less than 16.
     */
    public PasswordHasherPBKDF2(final int derivedKeyBytes) {
        if (derivedKeyBytes < 16) {
            throw new IllegalArgumentException("derivedKeyBytes demasiado pequeño");
        }
        this.derivedKeyBytes = derivedKeyBytes;
    }

    /**
     * Verifies a password against an expected hash using PBKDF2-HMAC-SHA256.
     *
     * @param password      The password to verify.
     * @param salt          The random salt bytes.
     * @param iterations    The number of PBKDF2 iterations.
     * @param expectedHash  The expected hash bytes.
     * @return {@code true} if the password matches the expected hash, {@code false} otherwise.
     */
    @Override
    public boolean verify(final char[] password, final byte[] salt, final int iterations, final byte[] expectedHash) {
        if (password == null || salt == null || expectedHash == null || iterations <= 0) {
            return false;
        }

        final var derivedKey = derive(password, salt, iterations, expectedHash.length);
        try {
            return MessageDigest.isEqual(derivedKey, expectedHash);
        } finally {
            Arrays.fill(derivedKey, (byte) 0);
        }
    }

    /**
     * Hashes a password using PBKDF2-HMAC-SHA256.
     *
     * @param password  The password to hash.
     * @param salt      The random salt bytes.
     * @param iterations The number of PBKDF2 iterations.
     * @return The resulting {@code PasswordHash}.
     * @throws IllegalArgumentException if password is null, salt is null or empty, or iterations is not positive.
     */
    @Override
    public PasswordHash hash(final char[] password, final byte[] salt, final int iterations) {
        if (password == null) {
            throw new IllegalArgumentException("password no puede ser null");
        }
        if (salt == null || salt.length == 0) {
            throw new IllegalArgumentException("salt no puede ser null o vacio");
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations debe ser mayor que cero");
        }

        final var derivedKey = derive(password, salt, iterations, derivedKeyBytes);
        return new PasswordHash(derivedKey, salt, iterations);
    }

    private static byte[] derive(final char[] password, final byte[] salt, final int iterations,
            final int outLenBytes) {
        try {
            final var spec = new PBEKeySpec(password, salt, iterations, outLenBytes * 8);
            final var secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            return secretKeyFactory.generateSecret(spec).getEncoded();
        } catch (final Exception e) {
            throw new IllegalStateException("PBKDF2 derivation failed", e);
        }
    }
}
