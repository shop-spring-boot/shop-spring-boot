package edu.itc.shop.reporting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Repository;

import edu.itc.shop.domain.Category;
import edu.itc.shop.domain.Money_;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.Product_;
import edu.itc.shop.domain.Tag;
import edu.itc.shop.domain.Tag_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;

@Repository
public class ProductReportRepositoryImpl implements ProductReportRepository {

    private static final Map<String, Function<ReportExpressions, Expression<?>>> SORT_FIELDS = Map.of(
            "sku", expressions -> expressions.product().get(Product_.sku),
            "name", expressions -> expressions.product().get(Product_.name),
            "amount", expressions -> expressions.product().get(Product_.price).get(Money_.amount),
            "stock", expressions -> expressions.product().get(Product_.stock),
            "tagCount", ReportExpressions::tagCount);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ProductReportRow> findProductReport(
            String q,
            Category category,
            BigDecimal maxPrice,
            Integer minStock,
            String tag,
            String sort) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProductReportRow> query = cb.createQuery(ProductReportRow.class);
        Root<Product> product = query.from(Product.class);
        List<Predicate> predicates = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(product.get(Product_.name)), pattern, '\\'),
                    cb.like(cb.lower(product.get(Product_.sku)), pattern, '\\')));
        }
        if (category != null) {
            predicates.add(cb.equal(product.get(Product_.category), category));
        }
        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(
                    product.get(Product_.price).get(Money_.amount), maxPrice));
        }
        if (minStock != null) {
            predicates.add(cb.greaterThanOrEqualTo(product.get(Product_.stock), minStock));
        }
        if (tag != null && !tag.isBlank()) {
            Join<Product, Tag> tagJoin = product.join(Product_.tags, JoinType.INNER);
            predicates.add(cb.equal(cb.lower(tagJoin.get(Tag_.name)), tag.trim().toLowerCase(Locale.ROOT)));
            query.distinct(true);
        }

        Subquery<Long> tagCountQuery = query.subquery(Long.class);
        Root<Product> correlatedProduct = tagCountQuery.correlate(product);
        SetJoin<Product, Tag> tags = correlatedProduct.join(Product_.tags, JoinType.LEFT);
        tagCountQuery.select(cb.count(tags.get(Tag_.id)));
        Expression<Long> tagCount = tagCountQuery;

        query.select(cb.construct(
                ProductReportRow.class,
                product.get(Product_.sku),
                product.get(Product_.name),
                product.get(Product_.price).get(Money_.amount),
                product.get(Product_.stock),
                tagCount));
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(Predicate[]::new)));
        }
        query.groupBy(
                product.get(Product_.id),
                product.get(Product_.sku),
                product.get(Product_.name),
                product.get(Product_.category),
                product.get(Product_.price).get(Money_.amount),
                product.get(Product_.price).get(Money_.currency),
                product.get(Product_.stock),
                tagCount);
        query.orderBy(orders(cb, new ReportExpressions(product, tagCount), sort));
        return entityManager.createQuery(query).getResultList();
    }

    private List<Order> orders(CriteriaBuilder cb, ReportExpressions expressions, String sort) {
        validateSort(sort);
        String value = sort == null || sort.isBlank() ? "sku,asc" : sort.trim();
        String[] parts = value.split(",", -1);
        String field = parts[0].trim();
        Function<ReportExpressions, Expression<?>> fieldExpression = SORT_FIELDS.get(field);
        String direction = parts.length == 1 ? "asc" : parts[1].trim().toLowerCase(Locale.ROOT);
        Expression<?> sortExpression = fieldExpression.apply(expressions);
        if ("asc".equals(direction)) {
            return List.of(cb.asc(sortExpression));
        }
        if ("desc".equals(direction)) {
            return List.of(cb.desc(sortExpression));
        }
        throw new IllegalStateException("Validated sort direction was not recognized");
    }

    static void validateSort(String sort) {
        String value = sort == null || sort.isBlank() ? "sku,asc" : sort.trim();
        String[] parts = value.split(",", -1);
        if (parts.length > 2) {
            throw new IllegalArgumentException("Sort must be a field and optional direction");
        }
        String field = parts[0].trim();
        if (!SORT_FIELDS.containsKey(field)) {
            throw new IllegalArgumentException("Unsupported product report sort field: " + field);
        }
        String direction = parts.length == 1 ? "asc" : parts[1].trim().toLowerCase(Locale.ROOT);
        if (!"asc".equals(direction) && !"desc".equals(direction)) {
            throw new IllegalArgumentException("Sort direction must be asc or desc");
        }
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private record ReportExpressions(Root<Product> product, Expression<Long> tagCount) {
    }
}
