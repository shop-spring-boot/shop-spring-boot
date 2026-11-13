package edu.itc.shop.order;

import static org.assertj.core.api.Assertions.assertThat;

import edu.itc.shop.product.JdbcProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

@JdbcTest
@ActiveProfiles("test")
@Import({JdbcClientOrderRepository.class, JdbcProductRepository.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class RepositoryTest {

    private final JdbcClientOrderRepository orders;
    private final JdbcProductRepository products;
    private final JdbcClient jdbc;

    RepositoryTest(JdbcClientOrderRepository orders, JdbcProductRepository products, JdbcClient jdbc) {
        this.orders = orders;
        this.products = products;
        this.jdbc = jdbc;
    }

    private int stockOf(long productId) {
        return jdbc.sql("SELECT stock FROM product WHERE id = :id")
                .param("id", productId)
                .query(Integer.class)
                .single();
    }

    private long newOrder(long customerId, OrderStatus status) {
        Order order = new Order(null, customerId, status, LocalDateTime.now(), new BigDecimal("10.00"));
        return orders.insert(order, List.of(new OrderLine(1, 1, new BigDecimal("49.99"))));
    }

    @Test
    void updateStockDecrementsWhenEnoughStock() {
        assertThat(products.updateStock(1, -2)).isEqualTo(1);
        assertThat(stockOf(1)).isEqualTo(8);
    }

    @Test
    void updateStockRefusesWhenNotEnoughStock() {
        assertThat(products.updateStock(2, -999)).isZero();
        assertThat(stockOf(2)).isEqualTo(5);
    }

    @Test
    void findByIdReturnsPriceOrEmpty() {
        assertThat(products.findById(1).orElseThrow().price()).isEqualByComparingTo("49.99");
        assertThat(products.findById(999)).isEmpty();
    }

    @Test
    void insertThenFindByIdWithLines() {
        Order order = new Order(null, 1, OrderStatus.NEW, LocalDateTime.now(), new BigDecimal("131.88"));
        long id = orders.insert(order, List.of(
                new OrderLine(1, 2, new BigDecimal("49.99")),
                new OrderLine(3, 1, new BigDecimal("10.00"))));

        Order loaded = orders.findById(id).orElseThrow();
        assertThat(loaded.customerId()).isEqualTo(1);
        assertThat(loaded.status()).isEqualTo(OrderStatus.NEW);
        assertThat(loaded.total()).isEqualByComparingTo("131.88");
        assertThat(orders.findLines(id)).hasSize(2);
    }

    @Test
    void searchPaginatesAndFilters() {
        newOrder(1, OrderStatus.NEW);
        newOrder(1, OrderStatus.NEW);
        newOrder(1, OrderStatus.CANCELLED);
        newOrder(2, OrderStatus.NEW);

        assertThat(orders.count(1L, null)).isEqualTo(3);
        assertThat(orders.count(null, OrderStatus.CANCELLED)).isEqualTo(1);
        assertThat(orders.search(1L, null, 0, 2)).hasSize(2);
        assertThat(orders.search(1L, null, 1, 2)).hasSize(1);
    }
}