package ${package}.handlers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /health — liveness probe.
 * <p>
 * Returns {@code 200 OK} with a plain-text body.
 * No authentication required.
 * </p>
 */
public final class HealthHandler {

    public void handle(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("OK");
    }
}
