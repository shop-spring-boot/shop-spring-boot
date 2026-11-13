package edu.itc.shop.product;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import edu.itc.shop.domain.Category;

@RestController
public class ProductApiController {

    private final ProductSearchService service;

    public ProductApiController(ProductSearchService service) {
        this.service = service;
    }

    @GetMapping("/api/products")
    public Page<ProductView> search(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Category category,
                                    @RequestParam(required = false) BigDecimal maxPrice,
                                    @RequestParam(required = false) Boolean inStock,
                                    @PageableDefault(size = 20) Pageable pageable) {
        return service.search(q, category, maxPrice, inStock, pageable);
    }
}