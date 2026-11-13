package edu.itc.shop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderStatus;

public record OrderFullDto(Long id, OrderStatus status, Instant placedAt,
                           BigDecimal total, List<OrderLineDto> lines) {

    public static OrderFullDto from(Order o) {
        return new OrderFullDto(o.getId(), o.getStatus(), o.getPlacedAt(), o.total().amount(),
                o.getLines().stream()
                        .map(l -> new OrderLineDto(l.getProduct().getSku(),
                                l.getProduct().getName(), l.getQuantity(),
                                l.getUnitPrice().amount()))
                        .toList());
    }
}