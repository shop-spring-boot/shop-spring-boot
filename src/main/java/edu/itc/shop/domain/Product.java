package edu.itc.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity // this class is a table, and Hibernate now manages it.
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_gen")
    @SequenceGenerator(name = "product_gen", sequenceName = "product_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Embedded
    private Money price;

    @Column(nullable = false)
    private int stock;

    protected Product() {
        // required by JPA
    }

    public Product(String sku, String name, Category category, Money price, int stock) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public Money getPrice() { return price; }
    public int getStock() { return stock; }

    public void setName(String name) { this.name = name; }
    public void setPrice(Money price) { this.price = price; }
    public void setStock(int stock) { this.stock = stock; }
}
