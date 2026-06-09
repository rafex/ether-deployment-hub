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

/**
 * Contract for password hashing and verification.
 */
public interface PasswordHasher {

    /**
     * Hashes a password using the specified salt and iterations.
     *
     * @param password  The password to hash.
     * @param salt      The random salt bytes.
     * @param iterations The number of PBKDF2 iterations.
     * @return The resulting {@code PasswordHash}.
     */
    PasswordHash hash(char[] password, byte[] salt, int iterations);

    /**
     * Verifies a password against an expected hash.
     *
     * @param password      The password to verify.
     * @param salt          The random salt bytes.
     * @param iterations    The number of PBKDF2 iterations.
     * @param expectedHash  The expected hash bytes.
     * @return {@code true} if the password matches the expected hash, {@code false} otherwise.
     */
    boolean verify(char[] password, byte[] salt, int iterations, byte[] expectedHash);
}
