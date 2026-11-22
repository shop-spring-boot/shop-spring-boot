package edu.itc.shop.domain;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<Product> findAll();
    Optional<Product> findById(long id);
    Optional<Product> findBySku(String sku);
    Product save(Product product);   // assigns an id when id == 0, version goes up on every save
    boolean delete(long id);
}
