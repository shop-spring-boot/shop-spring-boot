package edu.itc.shop.support;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Address;
import edu.itc.shop.domain.Category;
import edu.itc.shop.domain.Customer;
import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderStatus;
import edu.itc.shop.domain.Product;
import edu.itc.shop.order.CustomerRepository;
import edu.itc.shop.order.OrderRepository;
import edu.itc.shop.product.ProductRepository;

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
    public Long newOrder() {
        Customer c = new Customer("Test User", "test" + System.nanoTime() + "@test.com", 
                new Address("St", "City", "Zip"), new Address("St", "City", "Zip"));
        customers.save(c);
        Order o = new Order(c, OrderStatus.NEW, Instant.now());
        orders.save(o);
        return o.getId();
    }

    @Transactional
    public Long newProduct() {
        Product p = new Product("SKU-" + System.nanoTime(), "Test Product", Category.HARDWARE, Money.eur("10"), 10);
        products.save(p);
        return p.getId();
    }

    @Transactional
    public void paidOrders(int orderCount, int linesPerOrder) {
        if (orders.count() > 0) return; // Prevent duplicate seeding

        Customer c = customers.save(new Customer("Test User", "test@test.com", 
            new Address("St", "City", "Zip"), new Address("St", "City", "Zip")));
        Product p = products.save(new Product("SKU-TEST", "Test Product", Category.HARDWARE, Money.eur("10"), 999));

        for (int i = 0; i < orderCount; i++) {
            Order o = new Order(c, OrderStatus.PAID, Instant.now());
            for (int j = 0; j < linesPerOrder; j++) {
                o.addLine(p, 1);
            }
            orders.save(o);
        }
    }
}