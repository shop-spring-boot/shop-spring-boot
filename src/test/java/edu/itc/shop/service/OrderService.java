package edu.itc.shop.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderStatus;
import edu.itc.shop.order.OrderRepository;
import edu.itc.shop.web.dto.OrderDto;

@Service
public class OrderService {

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    /** Managed entity: dirty checking issues the UPDATE at commit, no save() needed. */
    @Transactional
    public void markPaid(Long orderId) {
        Order o = orders.findById(orderId).orElseThrow();
        o.setStatus(OrderStatus.PAID);
    }

    @Transactional
    public void changeStatus(Long id, OrderStatus next) {
        Order o = orders.findById(id).orElseThrow();
        o.setStatus(next);
    }

    @Transactional(readOnly = true)
    public OrderDto view(Long id) {
        Order o = orders.findById(id).orElseThrow();
        return new OrderDto(o.getId(), o.getStatus(), o.getLines().size(), o.total());
    }

    /** THE TRAP: modifies inside a read-only transaction, so Hibernate never flushes it. Kept as documentation. */
    @Transactional(readOnly = true)
    public void wronglyModifyInReadOnly(Long id) {
        Order o = orders.findById(id).orElseThrow();
        o.setStatus(OrderStatus.CANCELLED);
    }
}
