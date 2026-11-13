package edu.itc.shop.orderfeed;

import java.time.Instant;

import org.springframework.data.domain.ScrollPosition;

import edu.itc.shop.domain.Order;

public record OrderCursor(Instant placedAt, Long id) {

    public static OrderCursor parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Cursor must contain placedAt and id");
        }
        try {
            return new OrderCursor(Instant.parse(parts[0]), Long.valueOf(parts[1]));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid order cursor", ex);
        }
    }

    public ScrollPosition toScrollPosition() {
        return ScrollPosition.forward(java.util.Map.of("placedAt", placedAt, "id", id));
    }

    public static String from(Order order) {
        return order.getPlacedAt() + "," + order.getId();
    }
}
