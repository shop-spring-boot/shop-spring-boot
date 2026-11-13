package edu.itc.shop.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import edu.itc.shop.domain.Address;
import edu.itc.shop.domain.Category;
import edu.itc.shop.domain.Customer;
import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderStatus;
import edu.itc.shop.domain.Product;
import edu.itc.shop.product.ProductRepository;
// Spring Boot 4: let VS Code auto-import these three from the new packages
// (...test.autoconfigure...), do not copy package names from old tutorials.
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;

    Order order;

    @BeforeEach
    void setUp() {
        var addr = new Address("1 Main St", "Phnom Penh", "12000");
        var ada = em.persist(new Customer("Ada", "ada@test.com", addr, addr));
        var kb = em.persist(new Product("T-1", "Keyboard", Category.HARDWARE, Money.eur("40.00"), 50));
        var mouse = em.persist(new Product("T-2", "Mouse", Category.HARDWARE, Money.eur("20.00"), 2));
        order = new Order(ada, OrderStatus.PAID, Instant.now());
        order.addLine(kb, 1);
        order.addLine(mouse, 2);
        em.persist(order);
        em.flush();
        em.clear();
    }

    @Test
    void derivedQuery() {
        assertThat(orders.findByCustomerEmailAndStatusOrderByPlacedAtDesc("ada@test.com", OrderStatus.PAID))
                .hasSize(1);
        assertThat(orders.countByStatus(OrderStatus.PAID)).isEqualTo(1);
    }

    @Test
    void fetchJoinInitialisesLines() {
        var loaded = orders.findWithLinesByCustomerEmail("ada@test.com").get(0);
        assertThat(isLoaded(loaded)).isTrue();
        assertThat(loaded.getLines()).hasSize(2);
    }

    @Test
    void plainFindByIdLeavesLinesLazy() {
        var loaded = orders.findById(order.getId()).orElseThrow();
        assertThat(isLoaded(loaded)).isFalse();
    }

    @Test
    void nativeQuery() {
        assertThat(products.findLowStock(5)).extracting(Product::getSku).containsExactly("T-2");
    }

    @Test
    void interfaceProjection() {
        var page = orders.findByStatus(OrderStatus.PAID, PageRequest.of(0, 5));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCustomer().getName()).isEqualTo("Ada");
    }

    private boolean isLoaded(Order o) {
        return em.getEntityManager().getEntityManagerFactory()
                .getPersistenceUnitUtil().isLoaded(o, "lines");
    }
}