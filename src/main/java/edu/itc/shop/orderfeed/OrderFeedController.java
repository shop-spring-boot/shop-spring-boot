package edu.itc.shop.orderfeed;

import java.util.List;

import org.springframework.data.domain.Window;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.shop.domain.Order;

@RestController
@RequestMapping("/api/orders/feed")
public class OrderFeedController {

    private final OrderFeedService service;

    public OrderFeedController(OrderFeedService service) {
        this.service = service;
    }

    @GetMapping
    public OrderFeedResponse<OrderSummary> slice(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String after) {
        return service.slice(size, after);
    }

    @GetMapping("/keyset")
    public OrderFeedResponse<OrderSummary> keyset(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String after) {
        Window<Order> window = service.scroll(size, after);
        List<OrderSummary> items = window.getContent().stream()
                .map(order -> new OrderSummary(order.getId(), order.getPlacedAt(), order.getStatus()))
                .toList();
        String nextCursor = window.hasNext() && !window.isEmpty()
                ? OrderCursor.from(window.getContent().getLast())
                : null;
        return new OrderFeedResponse<>(items, nextCursor, window.hasNext());
    }
}
