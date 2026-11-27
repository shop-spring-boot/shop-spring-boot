package edu.itc.shop.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.shop.domain.Order;
import edu.itc.shop.service.OrderService;
import edu.itc.shop.service.PlaceOrder;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    public ResponseEntity<Order> place(@Valid @RequestBody PlaceOrder cmd, Authentication auth) {
        Order created = orders.place(auth.getName(), cmd);
        return ResponseEntity
                .created(URI.create("/api/orders/" + created.getId()))
                .body(created);
    }

    @GetMapping
    public List<Order> history(@RequestParam(required = false) String customer, Authentication auth) {
        String target = (customer != null && !customer.isBlank()) ? customer : auth.getName();
        return orders.history(target);
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable long id) {
        return orders.get(id);
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<Void> ship(@PathVariable long id) {
        orders.ship(id);
        return ResponseEntity.noContent().build();
    }
}