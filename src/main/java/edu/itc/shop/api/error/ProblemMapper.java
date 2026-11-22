package edu.itc.shop.api.error;

import java.util.UUID;

import edu.itc.shop.api.dto.Problem;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Catch-all: every error leaves the API as application/problem+json. */
@Provider
public class ProblemMapper implements ExceptionMapper<Throwable> {

    private static final System.Logger LOG = System.getLogger("api");

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable ex) {
        String path = uriInfo.getRequestUri().getPath();
        if (ex instanceof WebApplicationException w) {
            int status = w.getResponse().getStatus();
            if (status < 400) {
                return w.getResponse();   // redirects and similar are not errors
            }
            String title = reasonPhrase(status);
            var problem = new Problem("about:blank", title, status, w.getMessage(), path, null);
            return Response.status(statusType(status, title)).type(Problem.MEDIA_TYPE).entity(problem).build();
        }
        if (isMalformedBody(ex)) {
            var problem = new Problem("about:blank", "Bad Request", 400, "Malformed request body", path, null);
            return Response.status(400).type(Problem.MEDIA_TYPE).entity(problem).build();
        }
        // Anything else is our bug: log the details, tell the client only a reference id.
        String reference = UUID.randomUUID().toString();
        LOG.log(System.Logger.Level.ERROR, "reference " + reference, ex);
        var problem = new Problem("about:blank", "Internal Server Error", 500,
                "reference " + reference, path, null);
        return Response.status(500).type(Problem.MEDIA_TYPE).entity(problem).build();
    }

    // A body that cannot be parsed (broken JSON or XML) is a client error, not a server bug.
    private static boolean isMalformedBody(Throwable ex) {
        Throwable t = ex;
        for (int depth = 0; t != null && depth < 10; depth++, t = t.getCause()) {
            String name = t.getClass().getName();
            if (name.startsWith("jakarta.json.") || name.startsWith("org.eclipse.yasson.")
                    || name.startsWith("jakarta.xml.bind.")) {
                return true;
            }
        }
        return false;
    }

    // Response.Status has no entry for some codes (422); give those a proper reason phrase too,
    // so the status line reads "422 Unprocessable Content" and the problem title matches it.
    private static String reasonPhrase(int status) {
        Response.Status known = Response.Status.fromStatusCode(status);
        if (known != null) {
            return known.getReasonPhrase();
        }
        return status == 422 ? "Unprocessable Content" : "Error";
    }

    private static Response.StatusType statusType(int status, String reason) {
        return new Response.StatusType() {
            @Override public int getStatusCode() { return status; }
            @Override public Response.Status.Family getFamily() { return Response.Status.Family.familyOf(status); }
            @Override public String getReasonPhrase() { return reason; }
        };
    }
}
