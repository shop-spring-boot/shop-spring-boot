package edu.itc.shop.orderfeed;

import java.time.Instant;

import edu.itc.shop.domain.OrderStatus;

public record OrderSummary(Long id, Instant placedAt, OrderStatus status) {
    public String cursor() {
        return placedAt + "," + id;
    }
}
