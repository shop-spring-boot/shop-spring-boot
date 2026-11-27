package edu.itc.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customer;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.NEW;

    protected Order() {
    }

    public Order(String customer, Long productId, int quantity) {
        this.customer = customer;
        this.productId = productId;
        this.quantity = quantity;
        this.status = OrderStatus.NEW;
    }

    public Long getId() {
        return id;
    }

    public String getCustomer() {
        return customer;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void ship() {
        this.status = OrderStatus.SHIPPED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }
}