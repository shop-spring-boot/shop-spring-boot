package edu.itc.shop.api.params;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Product;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

/** Query parameters of GET /products, grouped with @BeanParam. */
public class ProductFilter {
    @QueryParam("q") public String q;
    @QueryParam("minPrice") public Money minPrice;                  // "5 EUR"
    @QueryParam("maxPrice") public Money maxPrice;
    @QueryParam("inStock") @DefaultValue("false") public boolean inStock;
    @QueryParam("sort") @DefaultValue("name,asc")
    @Pattern(regexp = "[a-z]+,(asc|desc)", message = "use field,asc or field,desc")
    public String sort;

    // Whitelist: only these fields can be sorted on.
    private static final Map<String, Comparator<Product>> SORTS = Map.of(
            "name", Comparator.comparing(Product::name),
            "price", Comparator.comparing(Product::price),
            "stock", Comparator.comparingInt(Product::stock));

    public String sortField() { return sort.split(",")[0]; }
    public boolean descending() { return sort.endsWith(",desc"); }

    public List<Product> apply(Collection<Product> all) {
        requireEur(minPrice);
        requireEur(maxPrice);
        Comparator<Product> base = SORTS.get(sortField());
        if (base == null) {
            throw new BadRequestException("Unknown sort field " + sortField());
        }
        Comparator<Product> order = descending() ? base.reversed() : base;
        return all.stream()
                .filter(p -> q == null || q.isBlank() || p.name().toLowerCase().contains(q.toLowerCase()))
                .filter(p -> minPrice == null || p.price().compareTo(minPrice.amount()) >= 0)
                .filter(p -> maxPrice == null || p.price().compareTo(maxPrice.amount()) <= 0)
                .filter(p -> !inStock || p.stock() > 0)
                .sorted(order)
                .toList();
    }

    private static void requireEur(Money m) {
        if (m != null && !"EUR".equals(m.currency())) {
            throw new BadRequestException("Only EUR is supported, got " + m.currency());
        }
    }
}
