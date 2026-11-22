package edu.itc.shop.api;

import java.net.URI;
import java.util.List;

import edu.itc.shop.api.dto.ReviewInput;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.Review;
import edu.itc.shop.domain.ReviewRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Sub-resource of a product. It has no @Path: ProductResource returns it from a locator method.
 * A new instance is created per request, so it may hold the Product as a field.
 */
public class ReviewResource {

    private final Product product;
    private final ReviewRepository reviews;

    public ReviewResource(Product product, ReviewRepository reviews) {
        this.product = product;
        this.reviews = reviews;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Review> list() {
        return reviews.findByProduct(product.id());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(@Valid @NotNull ReviewInput in, @Context UriInfo uriInfo) {
        Review saved = reviews.add(product.id(), in.stars(), in.author(), in.text());
        URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(saved.id())).build();
        return Response.created(location).entity(saved).build();
    }
}
