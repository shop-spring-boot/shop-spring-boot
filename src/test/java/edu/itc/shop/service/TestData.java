package edu.itc.shop.service;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import edu.itc.shop.domain.*;
import edu.itc.shop.repository.*;

@Component
public class TestData {

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;

    public TestData(CustomerRepository customers, ProductRepository products, OrderRepository orders) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
    }

    @Transactional
    public Long newProduct() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return products.save(new Product("SKU-" + id, "Item " + id, Money.eur("10.00"), Category.BOOKS)).getId();
    }

    /** One customer, one product (price 10.00), one order with one line of quantity 2. */
    @Transactional
    public Long newOrder() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Address a = new Address("Street 1", "Phnom Penh", "12000");
        Customer c = customers.save(new Customer("Test " + id, id + "@test.io", a, a));
        Product p = products.save(new Product("SKU-" + id, "Item " + id, Money.eur("10.00"), Category.BOOKS));
        Order o = new Order(c);
        o.addLine(p, 2);
        return orders.save(o).getId();
    }
}