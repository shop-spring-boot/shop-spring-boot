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
        return Specification.allOf(
                q == null || q.isBlank() ? null : nameContains(q),
                category == null ? null : inCategory(category),
                maxPrice == null ? null : cheaperThan(maxPrice),
                Boolean.TRUE.equals(inStock) ? inStock() : null);
    }
}