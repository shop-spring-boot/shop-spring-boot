package edu.itc.shop.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EntityIdentityTest {

    @Autowired
    TestEntityManager em;

    private Product newProduct(String sku) {
        return new Product(sku, "Pen", Category.BOOKS, Money.eur("1.00"), 10);
    }

    @Test
    void hashSetStillContainsProductAfterPersist() {
        Product product = newProduct("SKU-9");
        Set<Product> set = new HashSet<>();
        set.add(product);                          // id is still null here

        em.persist(product);
        em.flush();                                // the id is assigned now

        assertThat(product.getId()).isNotNull();
        assertThat(set.contains(product)).isTrue();   // works because hashCode is constant
    }

    @Test
    void reloadedInstanceIsEqualButNotSame() {
        Product product = newProduct("SKU-10");
        em.persist(product);
        em.flush();
        Long id = product.getId();
        em.clear();                                // fresh context: find gives a new object

        Product reloaded = em.find(Product.class, id);

        assertThat(reloaded).isNotSameAs(product);    // == is false (two Java objects)
        assertThat(reloaded).isEqualTo(product);      // equals() is true (same id)
    }

    @Test
    void twoUnsavedProductsAreNotEqual() {
        Product a = newProduct("SKU-11");
        Product b = newProduct("SKU-12");

        assertThat(a).isNotEqualTo(b);             // null ids: only equal to itself
        assertThat(a).isEqualTo(a);
    }
}