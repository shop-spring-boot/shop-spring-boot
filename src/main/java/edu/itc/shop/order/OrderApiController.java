package edu.itc.shop.order;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import edu.itc.shop.domain.OrderStatus;

@RestController
public class OrderApiController {

    private final OrderRepository orders;
    private final OrderQueryService queries;

    public OrderApiController(OrderRepository orders, OrderQueryService queries) {
        this.orders = orders;
        this.queries = queries;
    }

    @GetMapping("/api/orders")
    public Page<OrderSummary> list(@RequestParam OrderStatus status,
            @PageableDefault(size = 20, sort = "placedAt",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        return orders.findByStatus(status, pageable);
    }

    @GetMapping("/api/customers/{email}/orders")
    public List<OrderFullDto> customerOrders(@PathVariable String email) {
        return queries.ordersOf(email);
    }
}