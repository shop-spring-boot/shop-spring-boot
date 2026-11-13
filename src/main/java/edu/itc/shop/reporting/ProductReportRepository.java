package edu.itc.shop.reporting;

import java.math.BigDecimal;
import java.util.List;

import edu.itc.shop.domain.Category;

public interface ProductReportRepository {
    List<ProductReportRow> findProductReport(
            String q,
            Category category,
            BigDecimal maxPrice,
            Integer minStock,
            String tag,
            String sort);
}
