package edu.itc.shop.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository("jdbcProductRepository")
public class ProductRepository {
    private final JdbcClient jdbc;

    @Value("${shop.currency:USD}")
    private String currency;

    public ProductRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<Product> findAll() {
        return jdbc.sql("SELECT id, name, amount AS price, stock FROM product ORDER BY id")
                   .query(Product.class).list();
    }

    public Optional<Product> findById(long id) {
        return jdbc.sql("SELECT id, name, amount AS price, stock FROM product WHERE id = :id")
                   .param("id", id)
                   .query(Product.class).optional();
    }

    public List<Product> findByNameContaining(String text) {
        return jdbc.sql("SELECT id, name, amount AS price, stock FROM product " +
                        "WHERE LOWER(name) LIKE :text ORDER BY id")
                   .param("text", "%" + text.toLowerCase() + "%")
                   .query(Product.class).list();
    }

    public long insert(Product p) {
        long id = jdbc.sql("SELECT nextval('product_seq')")
                .query(Long.class).single();
        jdbc.sql("INSERT INTO product(id, sku, name, category, amount, currency, stock) " +
                 "VALUES (:id, :sku, :name, :category, :price, :currency, :stock)")
            .param("id", id)
            .param("sku", "SKU-" + UUID.randomUUID())
            .param("name", p.name())
            .param("category", "HARDWARE")
            .param("price", p.price())
            .param("currency", currency)
            .param("stock", p.stock())
            .update();
        return id;
    }

    public int updateStock(long id, int delta) {
        return jdbc.sql("UPDATE product SET stock = stock + :delta " +
                        "WHERE id = :id AND stock + :delta >= 0")
                   .param("delta", delta)
                   .param("id", id)
                   .update();
    }

    public int delete(long id) {
        return jdbc.sql("DELETE FROM product WHERE id = :id")
                .param("id", id)
                .update();
    }
}