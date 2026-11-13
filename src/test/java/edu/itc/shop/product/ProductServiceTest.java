package edu.itc.shop.product;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductServiceTest {

    @Autowired ProductService service;
    @Autowired ProductRepository repo;

    @Test
    void createPersistsProductUsingCurrentSchema() {
        ProductDto created = service.create(
                new CreateProduct("Test product", new java.math.BigDecimal("12.34"), 8));

        Product saved = repo.findById(created.id()).orElseThrow();
        assertEquals("Test product", saved.name());
        assertEquals(new java.math.BigDecimal("12.34"), saved.price());
        assertEquals(8, saved.stock());
    }

    @Test
    void restockRollsBackWhenSourceHasTooLittleStock() {
        int before1 = repo.findById(1).orElseThrow().stock();
        int before2 = repo.findById(2).orElseThrow().stock();

        assertThrows(IllegalStateException.class, () -> service.restock(1, 2, 1_000));

        assertEquals(before1, repo.findById(1).orElseThrow().stock());
        assertEquals(before2, repo.findById(2).orElseThrow().stock());
    }

    // This test really proves the rollback: the FIRST update succeeds,
    // the SECOND fails (product 999 does not exist), and the first must be undone.
    @Test
    void restockRollsBackWhenSecondUpdateFails() {
        int before1 = repo.findById(1).orElseThrow().stock();

        assertThrows(IllegalStateException.class, () -> service.restock(1, 999, 1));

        assertEquals(before1, repo.findById(1).orElseThrow().stock());
    }

    @Test
    void restockMovesStockWhenEverythingIsFine() {
        int before1 = repo.findById(1).orElseThrow().stock();
        int before2 = repo.findById(2).orElseThrow().stock();

        service.restock(1, 2, 1);

        assertEquals(before1 - 1, repo.findById(1).orElseThrow().stock());
        assertEquals(before2 + 1, repo.findById(2).orElseThrow().stock());
    }
}