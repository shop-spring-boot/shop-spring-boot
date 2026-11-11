package edu.itc.shop.product;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import java.math.BigDecimal;

@JdbcTest
@Import(ProductRepository.class)
class ProductRepositoryTest {

    @Autowired ProductRepository repo;

    @Test
    void findAllReturnsFiveProducts() {
        assertEquals(5, repo.findAll().size());
    }

    @Test
    void insertReturnsGeneratedId() {
        long id = repo.insert(new Product(null, "Webcam", new BigDecimal("39.90"), 8));
        assertTrue(id > 5);
    }

    @Test
    void duplicateNameThrowsDuplicateKeyException() {
        Product dup = new Product(null, "Laptop", new BigDecimal("1.00"), 1);
        var ex = assertThrows(Exception.class, () -> repo.insert(dup));
        System.out.println("EXCEPTION CLASS = " + ex.getClass().getName());
        assertInstanceOf(DuplicateKeyException.class, ex);
    }
}