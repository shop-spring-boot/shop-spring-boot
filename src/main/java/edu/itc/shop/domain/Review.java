package edu.itc.shop.domain;

import jakarta.persistence.*;

@Entity
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long productId;
    private String author;
    private int stars;
    @Column(length = 1000)
    private String text;

    protected Review() {}
    public Review(Long productId, String author, int stars, String text) {
        this.productId = productId; this.author = author; this.stars = stars; this.text = text;
    }
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getAuthor() { return author; }
    public int getStars() { return stars; }
    public String getText() { return text; }
}