package edu.itc.shop.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ToStringPitfallTest {

    @Autowired
    TestEntityManager em;

    private Long saveOrder() {
        Address address = new Address("1 Main St", "Phnom Penh", "12000");
        Customer customer = em.persist(new Customer("Dara", "pitfall@example.com", address, address));
        Product book = em.persist(new Product("PIT-1", "Java Book", Category.BOOKS, Money.eur("10.00"), 100));
        Order order = new Order(customer, OrderStatus.NEW, Instant.now());
        order.addLine(book, 2);
        em.persist(order);
        em.flush();
        em.clear();
        return order.getId();
    }

    @Test
    void stackOverflowWhenSessionIsOpen() {
        Long id = saveOrder();
        Order order = em.find(Order.class, id);
        order.getLines().size();                       // load the lines

        assertThatThrownBy(order::toString)
                .isInstanceOf(StackOverflowError.class)
                .satisfies(e -> System.out.println("EXCEPTION: " + e.getClass().getName()));
    }

    @Test
    void lazyInitializationWhenDetached() {
        Long id = saveOrder();
        Order order = em.find(Order.class, id);
        em.clear();                                    // order is detached: no session any more

        assertThatThrownBy(order::toString)
                .isInstanceOf(LazyInitializationException.class)
                .satisfies(e -> System.out.println("EXCEPTION: " + e));
    }
}