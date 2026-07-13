package dev.rafex.etherbrain.infra.http.codec;

import dev.rafex.etherbrain.infra.http.HttpModelConfig;

/**
 * Small helpers shared by the provider codecs for endpoint construction.
 */
final class CodecEndpoints {

    private CodecEndpoints() {
    }

    /**
     * Returns the provider base URL with any trailing slashes removed.
     *
     * @param config the model configuration whose {@code endpoint} is read
     * @return the normalized base URL (no trailing {@code /})
     */
    static String base(HttpModelConfig config) {
        return config.endpoint().toString().replaceAll("/+$", "");
    }
}
