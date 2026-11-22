package edu.itc.shop.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import edu.itc.shop.api.dto.ProductDto;
import edu.itc.shop.api.dto.ProductV2;
import edu.itc.shop.api.dto.StockChange;
import edu.itc.shop.api.filter.Compress;
import edu.itc.shop.api.params.ProductFilter;
import edu.itc.shop.domain.DuplicateSkuException;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.ProductRepository;
import edu.itc.shop.domain.ReviewRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/products")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ProductResource {

    private final ProductRepository repo;
    private final ReviewRepository reviews;

    @Inject
    public ProductResource(ProductRepository repo, ReviewRepository reviews) {
        this.repo = repo;
        this.reviews = reviews;
    }

    @GET
    @Compress
    public Response list(@BeanParam @Valid ProductFilter f, @Context UriInfo uriInfo) {
        List<Product> all = f.apply(repo.findAll());                 // filtered + sorted
        int total = all.size();
        int lastPage = Math.max(0, (total - 1) / f.size);
        List<ProductDto> page = all.stream()
                .skip((long) f.page * f.size).limit(f.size)
                .map(ProductDto::from).toList();

        var links = new ArrayList<Link>();
        links.add(link(uriInfo, 0, "first"));
        if (f.page > 0) {
            links.add(link(uriInfo, f.page - 1, "prev"));
        }
        if (f.page < lastPage) {
            links.add(link(uriInfo, f.page + 1, "next"));
        }
        links.add(link(uriInfo, lastPage, "last"));
        return Response.ok(new GenericEntity<List<ProductDto>>(page) { })
                .links(links.toArray(Link[]::new))
                .header("X-Total-Count", total)
                .build();
    }

    private static Link link(UriInfo uriInfo, int page, String rel) {
        return Link.fromUriBuilder(uriInfo.getRequestUriBuilder().replaceQueryParam("page", page))
                .rel(rel).build();
    }

    // Version 1 is the default: plain JSON clients (qs=1.0) and the explicit v1 type get it.
    @GET
    @Path("/{id: \\d+}")
    @Produces({"application/json; qs=1.0", "application/vnd.shop.v1+json", MediaType.APPLICATION_XML})
    public Response one(@PathParam("id") long id, @Context Request request) {
        Product p = find(id);
        EntityTag tag = tagOf(p);
        Response.ResponseBuilder cond = request.evaluatePreconditions(tag);
        if (cond != null) {
            return cond.tag(tag).build();                              // 304
        }
        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        cc.setMaxAge(60);
        cc.setNoTransform(true);
        return Response.ok(ProductDto.from(p)).tag(tag).cacheControl(cc).build();
    }

    // Version 2 is only chosen when the client asks for it; qs=0.5 keeps it behind v1 for "*/*".
    @GET
    @Path("/{id: \\d+}")
    @Produces("application/vnd.shop.v2+json; qs=0.5")
    public ProductV2 oneV2(@PathParam("id") long id) {
        return ProductV2.from(find(id));
    }

    @POST
    public Response create(@Valid @NotNull ProductDto dto, @Context UriInfo uriInfo) {
        repo.findBySku(dto.sku).ifPresent(p -> { throw new DuplicateSkuException(dto.sku); });
        Product saved = repo.save(dto.toProduct(0, 0));
        URI location = uriInfo.getAbsolutePathBuilder().path(Long.toString(saved.id())).build();
        return Response.created(location).entity(ProductDto.from(saved)).build();
    }

    @PUT
    @Path("/{id: \\d+}")
    public Response replace(@PathParam("id") long id,
                            @HeaderParam(HttpHeaders.IF_MATCH) String ifMatch,
                            @Context Request request,
                            @Valid @NotNull ProductDto dto) {
        if (ifMatch == null) {
            throw new ClientErrorException("If-Match header is required", 428);
        }
        Product existing = find(id);
        if (request.evaluatePreconditions(tagOf(existing)) != null) {
            throw new ClientErrorException("The product changed; reload it and retry", 412);
        }
        Product saved = repo.save(dto.toProduct(id, existing.version()));
        return Response.ok(ProductDto.from(saved)).tag(tagOf(saved)).build();
    }

    @DELETE
    @Path("/{id: \\d+}")
    public void delete(@PathParam("id") long id) {
        if (!repo.delete(id)) {
            throw new NotFoundException("Product " + id + " not found");
        }
    }

    @PATCH
    @Path("/{id: \\d+}/stock")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProductDto adjustStock(@PathParam("id") long id, @Valid @NotNull StockChange change) {
        Product updated = repo.adjustStock(id, change.delta());
        if (updated == null) {
            throw new NotFoundException("Product " + id + " not found");
        }
        return ProductDto.from(updated);
    }

    // Sub-resource locator: no HTTP method annotation. An unknown product fails here with 404.
    @Path("/{id: \\d+}/reviews")
    public ReviewResource reviews(@PathParam("id") long id) {
        return new ReviewResource(find(id), reviews);
    }

    private static EntityTag tagOf(Product p) {
        return new EntityTag(Integer.toHexString(p.version()));
    }

    private Product find(long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }
}
