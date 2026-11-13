package edu.itc.shop.reporting;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Category;
import edu.itc.shop.persistence.ProductRepository;

@Service
public class ProductReportService {

    private final ProductRepository products;

    public ProductReportService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<ProductReportRow> findProducts(
            String q,
            Category category,
            BigDecimal maxPrice,
            Integer minStock,
            String tag,
            String sort) {
        ProductReportRepositoryImpl.validateSort(sort);
        return products.findProductReport(q, category, maxPrice, minStock, tag, sort);
    }
}
