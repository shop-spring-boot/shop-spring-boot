package edu.itc.shop.api;

import java.net.URI;
import java.util.List;

import edu.itc.shop.api.dto.ProductDto;
import edu.itc.shop.api.dto.StockChange;
import edu.itc.shop.api.params.ProductFilter;
import edu.itc.shop.domain.DuplicateSkuException;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.ProductRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/products")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ProductResource {

    private final ProductRepository repo;

    @Inject
    public ProductResource(ProductRepository repo) {
        this.repo = repo;
    }

    @GET
    public List<ProductDto> list(@BeanParam @Valid ProductFilter f) {
        return f.apply(repo.findAll()).stream().map(ProductDto::from).toList();
    }

    @GET
    @Path("/{id: \\d+}")
    public ProductDto get(@PathParam("id") long id) {
        return ProductDto.from(find(id));
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
    public ProductDto replace(@PathParam("id") long id, @Valid @NotNull ProductDto dto) {
        Product existing = find(id);
        return ProductDto.from(repo.save(dto.toProduct(id, existing.version())));
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

    private Product find(long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }
}
