package edu.itc.shop.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// the next three imports moved packages in Spring Boot 4: use VS Code's auto-import
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductMappingTest {

    @Autowired
    TestEntityManager em;

    @Test
    void moneyAndCategoryRoundTrip() {
        Product p = new Product("SKU-1", "Laptop", Category.HARDWARE, Money.eur("899.99"), 10);
        em.persist(p);
        em.flush();   // send the INSERT to the database now
        em.clear();   // forget the object, so the next find really reads the row

        Product loaded = em.find(Product.class, p.getId());
        assertThat(loaded.getPrice()).isEqualTo(Money.eur("899.99"));
        assertThat(loaded.getCategory()).isEqualTo(Category.HARDWARE);

        Object raw = em.getEntityManager()
                .createNativeQuery("select category from product where id = :id")
                .setParameter("id", p.getId())
                .getSingleResult();
        assertThat(raw).isEqualTo("HARDWARE");   // stored as text, not a number
    }
}