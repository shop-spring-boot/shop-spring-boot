package edu.itc.shop.api.dto;

import java.util.List;

import edu.itc.shop.domain.OrderLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/** Body of POST /orders. */
public record PlaceOrder(@NotEmpty @Valid List<Line> lines) {

    public record Line(long productId, @Min(1) int quantity) { }

    public List<OrderLine> toLines() {
        return lines.stream().map(l -> new OrderLine(l.productId(), l.quantity())).toList();
    }
}
