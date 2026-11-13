package edu.itc.shop.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import edu.itc.shop.domain.Category;
import edu.itc.shop.domain.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>,
                                           JpaSpecificationExecutor<Product> {
    boolean existsBySku(String sku);
    List<Product> findTop3ByCategoryOrderByPriceAmountDesc(Category category);
    List<Product> findByTagsName(String name);
    @Query(value = "SELECT p.* FROM product p WHERE p.stock < :min ORDER BY p.stock",
        nativeQuery = true)
    List<Product> findLowStock(@Param("min") int min);
}