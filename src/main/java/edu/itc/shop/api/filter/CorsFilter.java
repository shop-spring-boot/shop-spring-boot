package edu.itc.shop.api.filter;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/** CORS headers on every response; preflight (OPTIONS) requests are answered here and never reach a resource. */
@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        if ("OPTIONS".equals(req.getMethod())) {
            req.abortWith(Response.ok().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        var h = res.getHeaders();
        h.putSingle("Access-Control-Allow-Origin", "http://localhost:5173");
        h.putSingle("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE");
        h.putSingle("Access-Control-Allow-Headers", "Content-Type,X-Api-Key,X-Correlation-Id,If-Match");
        h.putSingle("Access-Control-Expose-Headers", "Location,ETag,Link,X-Total-Count,X-Correlation-Id");
    }
}
