package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;

/** Order rules: reserving stock, the NEW -> PAID / CANCELLED life cycle. Shared by all requests. */
public class OrderService {

    private final ProductRepository products;
    private final ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong(1000);

    public OrderService(ProductRepository products) {
        this.products = products;
    }

    /** 404 for an unknown product, 409 when the stock is not enough; nothing stays reserved on failure. */
    public Order place(List<OrderLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLine line : lines) {
            Product p = products.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Product " + line.productId() + " not found"));
            total = total.add(p.price().multiply(BigDecimal.valueOf(line.quantity())));
        }
        List<OrderLine> reserved = new ArrayList<>();
        try {
            for (OrderLine line : lines) {
                if (products.adjustStock(line.productId(), -line.quantity()) == null) {
                    throw new NotFoundException("Product " + line.productId() + " not found");
                }
                reserved.add(line);
            }
        } catch (RuntimeException e) {
            reserved.forEach(this::release);
            throw e;
        }
        Order order = new Order("o-" + ids.incrementAndGet(), List.copyOf(lines), total, OrderStatus.NEW);
        orders.put(order.id(), order);
        return order;
    }

    public Order find(String id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new NotFoundException("Order " + id + " not found");
        }
        return order;
    }

    /** NEW -> PAID, 409 otherwise. */
    public Order pay(String id) {
        find(id);
        return orders.compute(id, (k, o) -> {
            if (o.status() != OrderStatus.NEW) {
                throw new ClientErrorException("Order " + id + " is " + o.status() + "; only a NEW order can be paid", 409);
            }
            return o.withStatus(OrderStatus.PAID);
        });
    }

    /** NEW -> CANCELLED and the stock goes back, 409 otherwise. */
    public Order cancel(String id) {
        find(id);
        return orders.compute(id, (k, o) -> {
            if (o.status() != OrderStatus.NEW) {
                throw new ClientErrorException("Order " + id + " is " + o.status() + "; only a NEW order can be cancelled", 409);
            }
            o.lines().forEach(this::release);
            return o.withStatus(OrderStatus.CANCELLED);
        });
    }

    private void release(OrderLine line) {
        products.adjustStock(line.productId(), line.quantity());
    }
}
