package edu.itc.shop.api.filter;

import java.io.IOException;
import java.util.UUID;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/** Gives every request a correlation id and writes one access-log line per response. */
@Provider
@Priority(Priorities.USER)
public class CorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static final String HEADER = "X-Correlation-Id";
    private static final String ID = "correlationId";
    private static final String START = "startNanos";
    private static final System.Logger LOG = System.getLogger("api");

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        String id = req.getHeaderString(HEADER);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        req.setProperty(ID, id);
        req.setProperty(START, System.nanoTime());
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        // A request rejected by the API-key filter never reaches filter(req) above,
        // so the id and the start time may be missing here.
        String id = (String) req.getProperty(ID);
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        Object start = req.getProperty(START);
        long ms = start == null ? 0 : (System.nanoTime() - (Long) start) / 1_000_000;

        res.getHeaders().putSingle(HEADER, id);
        LOG.log(System.Logger.Level.INFO, "{0} {1} -> {2} [{3}] {4} ms",
                req.getMethod(), req.getUriInfo().getRequestUri().getPath(), res.getStatus(), id, ms);
    }
}
