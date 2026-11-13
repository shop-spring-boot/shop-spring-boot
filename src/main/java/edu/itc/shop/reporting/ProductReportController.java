package edu.itc.shop.reporting;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.shop.domain.Category;

@RestController
@RequestMapping("/api/reports/products")
public class ProductReportController {

    private final ProductReportService service;

    public ProductReportController(ProductReportService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductReportRow> report(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "sku,asc") String sort) {
        return service.findProducts(q, category, maxPrice, minStock, tag, sort);
    }
}
