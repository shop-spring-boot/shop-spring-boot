package edu.itc.shop.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import edu.itc.shop.api.dto.OrderDto;
import edu.itc.shop.api.dto.PlaceOrder;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderLine;
import edu.itc.shop.domain.OrderService;
import edu.itc.shop.domain.OrderStatus;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/** Orders: place (idempotent), read, pay, cancel. Every representation carries Link headers and "_links". */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Orders", description = "Place, pay and cancel orders; follow the links in each response")
public class OrderResource {

    private final OrderService orders;
    private final IdempotencyStore idempotency;

    @Inject
    public OrderResource(OrderService orders, IdempotencyStore idempotency) {
        this.orders = orders;
        this.idempotency = idempotency;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Place an order (idempotent)",
               description = "Reserves stock and creates a NEW order. Repeating the request with the same "
                       + "Idempotency-Key and body replays the first response.")
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Order created (or replayed: Idempotency-Replayed: true)"),
        @APIResponse(responseCode = "400", description = "Invalid body (problem+json)"),
        @APIResponse(responseCode = "401", description = "Missing or invalid X-Api-Key (problem+json)"),
        @APIResponse(responseCode = "404", description = "Unknown product (problem+json)"),
        @APIResponse(responseCode = "409", description = "Not enough stock (problem+json)"),
        @APIResponse(responseCode = "415", description = "Body is not JSON (problem+json)"),
        @APIResponse(responseCode = "422", description = "Idempotency-Key reused with a different body (problem+json)"),
        @APIResponse(responseCode = "428", description = "Idempotency-Key header missing (problem+json)")
    })
    public Response place(
            @Parameter(description = "Unique key chosen by the client for this order", required = true)
            @HeaderParam("Idempotency-Key") String key,
            @Valid @NotNull PlaceOrder cmd,
            @Context UriInfo uriInfo) {
        if (key == null || key.isBlank()) {
            throw new ClientErrorException("Idempotency-Key header required", 428);
        }
        String hash = Hashes.sha256(cmd);
        boolean[] replayed = { true };
        IdempotencyStore.Stored stored = idempotency.computeIfAbsent(key, k -> {
            replayed[0] = false;
            Order order = orders.place(cmd.toLines());          // 404 / 409 inside
            return new IdempotencyStore.Stored(hash, 201, orderUri(uriInfo, order),
                    linkHeaders(order, uriInfo), toDto(order, uriInfo));
        });
        if (!stored.bodyHash().equals(hash)) {
            throw new ClientErrorException("Idempotency-Key reused with a different body", 422);
        }
        return Response.status(stored.status())
                .location(stored.location())
                .links(stored.links().toArray(Link[]::new))
                .header("Idempotency-Replayed", replayed[0])
                .entity(stored.body())
                .build();
    }

    @GET
    @Path("/{id: o-\\d+}")
    @Operation(summary = "Read an order")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "The order, with links to the next steps"),
        @APIResponse(responseCode = "404", description = "Unknown order (problem+json)")
    })
    public Response get(@PathParam("id") String id, @Context UriInfo uriInfo) {
        return view(orders.find(id), uriInfo);
    }

    @PUT
    @Path("/{id: o-\\d+}/payment")
    @Operation(summary = "Pay an order", description = "NEW becomes PAID")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "The paid order"),
        @APIResponse(responseCode = "401", description = "Missing or invalid X-Api-Key (problem+json)"),
        @APIResponse(responseCode = "404", description = "Unknown order (problem+json)"),
        @APIResponse(responseCode = "409", description = "The order is not NEW (problem+json)")
    })
    public Response pay(@PathParam("id") String id, @Context UriInfo uriInfo) {
        return view(orders.pay(id), uriInfo);
    }

    @DELETE
    @Path("/{id: o-\\d+}")
    @Operation(summary = "Cancel an order", description = "NEW becomes CANCELLED and the stock is restored")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "The cancelled order"),
        @APIResponse(responseCode = "401", description = "Missing or invalid X-Api-Key (problem+json)"),
        @APIResponse(responseCode = "404", description = "Unknown order (problem+json)"),
        @APIResponse(responseCode = "409", description = "The order is not NEW (problem+json)")
    })
    public Response cancel(@PathParam("id") String id, @Context UriInfo uriInfo) {
        return view(orders.cancel(id), uriInfo);
    }

    // ---- representation: body with "_links" plus the same relations as Link headers ----

    private Response view(Order order, UriInfo uriInfo) {
        return Response.ok(toDto(order, uriInfo))
                .links(linkHeaders(order, uriInfo).toArray(Link[]::new))
                .build();
    }

    private static URI orderUri(UriInfo uriInfo, Order order) {
        return uriInfo.getBaseUriBuilder().path("orders").path(order.id()).build();
    }

    private static URI productUri(UriInfo uriInfo, long productId) {
        return uriInfo.getBaseUriBuilder().path("products").path(Long.toString(productId)).build();
    }

    private static OrderDto toDto(Order order, UriInfo uriInfo) {
        URI self = orderUri(uriInfo, order);
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", self.toString());
        if (order.status() == OrderStatus.NEW) {
            links.put("pay", self + "/payment");
            links.put("cancel", self.toString());
        }
        for (OrderLine line : order.lines()) {
            links.put("product-" + line.productId(), productUri(uriInfo, line.productId()).toString());
        }
        return new OrderDto(order.id(), order.status(), order.total(), order.lines(), links);
    }

    private static List<Link> linkHeaders(Order order, UriInfo uriInfo) {
        URI self = orderUri(uriInfo, order);
        List<Link> links = new ArrayList<>();
        links.add(Link.fromUri(self).rel("self").build());
        if (order.status() == OrderStatus.NEW) {
            links.add(Link.fromUri(self + "/payment").rel("pay").param("method", "PUT").build());
            links.add(Link.fromUri(self).rel("cancel").param("method", "DELETE").build());
        }
        return links;
    }
}
