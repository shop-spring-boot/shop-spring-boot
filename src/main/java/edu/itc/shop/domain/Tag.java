package edu.itc.shop.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.HashSet;
import java.util.Set;


@Entity
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tag_gen")
    @SequenceGenerator(name = "tag_gen", sequenceName = "tag_seq", allocationSize = 50)
    private Long id;

    @ManyToMany(mappedBy = "tags")
    private Set<Product> products = new HashSet<>();

    public Set<Product> getProducts() { return products; }

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    // NEW (Task 2, step 2): inverse side, Product.tags is the owner
    @ManyToMany(mappedBy = "tags")
    private Set<Product> products = new HashSet<>();

    protected Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }

    // NEW (Task 2, step 2)
    void addProduct(Product product) { products.add(product); }

    public Set<Product> getProducts() { return Collections.unmodifiableSet(products); }

    // NEW (Task 2, step 3)
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Tag other && id != null && id.equals(other.getId()));
    }

    @Override
    public int hashCode() {
        return Tag.class.hashCode();
    }

    @Override
    public String toString() {
        return "Tag[id=%d, name=%s]".formatted(id, name);
    }
}