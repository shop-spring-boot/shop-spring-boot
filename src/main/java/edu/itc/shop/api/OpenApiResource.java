package edu.itc.shop.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/** Serves the OpenAPI document that the build generates into META-INF/openapi.yaml. */
@Path("/openapi")
public class OpenApiResource {

    @GET
    @Produces("application/yaml")
    public Response document() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/META-INF/openapi.yaml")) {
            if (in == null) {
                throw new NotFoundException("openapi.yaml was not generated; run mvn process-classes");
            }
            return Response.ok(new String(in.readAllBytes(), StandardCharsets.UTF_8), "application/yaml").build();
        }
    }
}
