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

import java.util.Arrays;

/**
 * Immutable password hash material.
 *
 * @param hash         The derived key bytes.
 * @param salt         The random salt bytes.
 * @param iterations   The number of PBKDF2 iterations.
 */
public record PasswordHash(byte[] hash, byte[] salt, int iterations) {

    /**
     * Constructs a new {@code PasswordHash} and defensively copies the input arrays.
     */
    public PasswordHash {
        hash = Arrays.copyOf(hash, hash.length);
        salt = Arrays.copyOf(salt, salt.length);
    }

    /**
     * Returns a defensive copy of the hash bytes.
     *
     * @return A copy of the derived key bytes.
     */
    @Override
    public byte[] hash() {
        return Arrays.copyOf(hash, hash.length);
    }

    /**
     * Returns a defensive copy of the salt bytes.
     *
     * @return A copy of the random salt bytes.
     */
    @Override
    public byte[] salt() {
        return Arrays.copyOf(salt, salt.length);
    }
}
