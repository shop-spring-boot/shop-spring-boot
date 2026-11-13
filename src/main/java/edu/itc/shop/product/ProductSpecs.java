package edu.itc.shop.product;

import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;
import edu.itc.shop.domain.Category;
import edu.itc.shop.domain.Product;

public final class ProductSpecs {

    private ProductSpecs() { }

    public static Specification<Product> nameContains(String q) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<Product> inCategory(Category category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Product> cheaperThan(BigDecimal max) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("price").<BigDecimal>get("amount"), max);
    }

    public static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.<Integer>get("stock"), 0);
    }

    public static Specification<Product> filter(String q, Category category,
                                                BigDecimal maxPrice, Boolean inStock) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank())      spec = spec.and(nameContains(q));
        if (category != null)               spec = spec.and(inCategory(category));
        if (maxPrice != null)               spec = spec.and(cheaperThan(maxPrice));
        if (Boolean.TRUE.equals(inStock))   spec = spec.and(inStock());
        return spec;
    }
}