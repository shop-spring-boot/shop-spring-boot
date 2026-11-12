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

    @Test
    void persistingOrderInsertsItsLines() {
        Address address = new Address("1 Main St", "Phnom Penh", "12000");
        Customer customer = em.persist(new Customer("Dara", "dara@example.com", address, address));
        Product book = em.persist(new Product("SKU-1", "Java Book", Category.BOOKS, Money.eur("10.00"), 100));
        Product toy = em.persist(new Product("SKU-2", "Robot Toy", Category.TOYS, Money.eur("5.00"), 50));
        em.flush();

        // count only what happens from here on
        Statistics stats = em.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        Order order = new Order(customer, OrderStatus.NEW, Instant.now());
        order.addLine(book, 2);
        order.addLine(toy, 1);
        em.persist(order);      // only the order is persisted, the lines follow by cascade
        em.flush();             // SQL is sent here, read the log

        assertThat(stats.getEntityInsertCount()).isEqualTo(3);   // 1 order + 2 lines
        assertThat(stats.getEntityUpdateCount()).isZero();       // no extra update

        Long orderId = order.getId();
        em.clear();             // empty the session so the next find hits the database

        Order loaded = em.find(Order.class, orderId);
        assertThat(loaded.getLines()).hasSize(2);
        assertThat(loaded.total().amount()).isEqualByComparingTo("25.00");   // 2 x 10.00 + 1 x 5.00
        assertThat(loaded.total().currency()).isEqualTo("EUR");
    }
}