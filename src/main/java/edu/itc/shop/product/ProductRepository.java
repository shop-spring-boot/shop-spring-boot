package edu.itc.shop.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import edu.itc.shop.domain.Category;
import edu.itc.shop.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long>,
                                           JpaSpecificationExecutor<Product> {
    boolean existsBySku(String sku);
    List<Product> findTop3ByCategoryOrderByPriceAmountDesc(Category category);
    List<Product> findByTagsName(String name);
}