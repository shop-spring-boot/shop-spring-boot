package edu.itc.shop.product;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.itc.shop.domain.Category;

@Service
public class ProductSearchService {

    private final ProductRepository products;

    public ProductSearchService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public Page<ProductView> search(String q, Category category, BigDecimal maxPrice,
                                    Boolean inStock, Pageable pageable) {
        return products.findAll(ProductSpecs.filter(q, category, maxPrice, inStock), pageable)
                .map(p -> new ProductView(p.getId(), p.getSku(), p.getName(),
                        p.getPrice().amount(), p.getStock()));
    }
}