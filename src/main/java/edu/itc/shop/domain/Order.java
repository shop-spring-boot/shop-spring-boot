package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.List;

public record Order(String id, List<OrderLine> lines, BigDecimal total, OrderStatus status) {

    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, lines, total, newStatus);
    }
}
