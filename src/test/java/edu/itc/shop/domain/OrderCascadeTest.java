package edu.itc.shop.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderCascadeTest {

    @Autowired
    TestEntityManager em;

    private Statistics stats() {
        return em.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
    }

    private long countLines() {
        return em.getEntityManager()
                .createQuery("select count(l) from OrderLine l", Long.class)
                .getSingleResult();
    }

    /** Saves a customer, two products and an order with two lines. Returns the order id. */
    private Long saveOrderWithTwoLines() {
        Address address = new Address("1 Main St", "Phnom Penh", "12000");
        Customer customer = em.persist(new Customer("Dara", "dara@example.com", address, address));
        Product book = em.persist(new Product("SKU-1", "Java Book", Category.BOOKS, Money.eur("10.00"), 100));
        Product toy = em.persist(new Product("SKU-2", "Robot Toy", Category.TOYS, Money.eur("5.00"), 50));
        em.flush();

        Order order = new Order(customer, OrderStatus.NEW, Instant.now());
        order.addLine(book, 2);
        order.addLine(toy, 1);
        em.persist(order);
        em.flush();
        return order.getId();
    }

    // ---------- Step 4 ----------
    @Test
    void persistingOrderInsertsItsLines() {
        Address address = new Address("1 Main St", "Phnom Penh", "12000");
        Customer customer = em.persist(new Customer("Dara", "dara@example.com", address, address));
        Product book = em.persist(new Product("SKU-1", "Java Book", Category.BOOKS, Money.eur("10.00"), 100));
        Product toy = em.persist(new Product("SKU-2", "Robot Toy", Category.TOYS, Money.eur("5.00"), 50));
        em.flush();

        Statistics stats = stats();
        stats.clear();

        Order order = new Order(customer, OrderStatus.NEW, Instant.now());
        order.addLine(book, 2);
        order.addLine(toy, 1);
        em.persist(order);
        em.flush();

        assertThat(stats.getEntityInsertCount()).isEqualTo(3);   // 1 order + 2 lines
        assertThat(stats.getEntityUpdateCount()).isZero();

        Long orderId = order.getId();
        em.clear();

        Order loaded = em.find(Order.class, orderId);
        assertThat(loaded.getLines()).hasSize(2);
        assertThat(loaded.total().amount()).isEqualByComparingTo("25.00");
        assertThat(loaded.total().currency()).isEqualTo("EUR");
    }

    // ---------- Step 5 ----------
    @Test
    void orphanRemovalAndCascadeRemove() {
        Long orderId = saveOrderWithTwoLines();
        em.clear();                                   // start from a clean session

        Order order = em.find(Order.class, orderId);
        assertThat(order.getLines()).hasSize(2);
        long before = countLines();
        assertThat(before).isEqualTo(2);

        // 1) orphan removal: take one line out of the order
        Statistics stats = stats();
        stats.clear();
        order.removeLine(order.getLines().get(0));
        em.flush();                                   // log: delete from order_line where id=?

        assertThat(countLines()).isEqualTo(before - 1);
        assertThat(stats.getEntityDeleteCount()).isEqualTo(1);

        // 2) cascade REMOVE: remove the whole order, its remaining line goes too
        stats.clear();
        em.remove(order);
        em.flush();                                   // log: delete order_line, then delete orders

        assertThat(countLines()).isZero();
        assertThat(em.find(Order.class, orderId)).isNull();
        assertThat(stats.getEntityDeleteCount()).isEqualTo(2);   // 1 remaining line + the order
    }
}