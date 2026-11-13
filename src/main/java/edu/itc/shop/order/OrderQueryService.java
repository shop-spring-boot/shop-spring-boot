package edu.itc.shop.order;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {

    private final OrderRepository orders;   // the new JPA repository

    public OrderQueryService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<OrderFullDto> ordersOf(String email) {
        return orders.findWithLinesByCustomerEmail(email).stream()
                .map(OrderFullDto::from)
                .toList();
    }
}