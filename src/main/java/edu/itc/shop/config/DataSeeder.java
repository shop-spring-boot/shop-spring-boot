package edu.itc.shop.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import edu.itc.shop.domain.*;
import edu.itc.shop.order.CustomerRepository;
import edu.itc.shop.order.OrderRepository;
import edu.itc.shop.product.ProductRepository;
import edu.itc.shop.product.TagRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final TagRepository tags;
    private final OrderRepository orders;

    public DataSeeder(CustomerRepository customers, ProductRepository products,
                      TagRepository tags, OrderRepository orders) {
        this.customers = customers;
        this.products = products;
        this.tags = tags;
        this.orders = orders;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (customers.count() > 0) return;          // do nothing if data exists
        var rnd = new Random(42);                   // fixed seed: same data every run

        var cs = customers.saveAll(List.of(
                new Customer("Ada Lovelace", "ada@example.com",
                        addr("1 Analytical St"), addr("1 Analytical St")),
                new Customer("Grace Hopper", "grace@example.com",
                        addr("2 Cobol Ave"), addr("9 Navy Rd")),
                new Customer("Alan Turing", "alan@example.com",
                        addr("3 Enigma Ln"), addr("3 Enigma Ln"))));

        var ts = tags.saveAll(List.of(new Tag("new"), new Tag("sale"), new Tag("popular")));

        Object[][] data = {
                {"KB-1", "Mechanical Keyboard", Category.HARDWARE, "49.90"},
                {"KB-7", "Compact Keyboard",    Category.HARDWARE, "39.00"},
                {"MS-1", "Gaming Mouse",        Category.HARDWARE, "25.50"},
                {"HB-1", "USB-C Hub",           Category.HARDWARE, "34.90"},
                {"BK-1", "Java Handbook",       Category.BOOKS,    "29.00"},
                {"BK-2", "SQL Cookbook",        Category.BOOKS,    "32.00"},
                {"BK-3", "Hibernate in Depth",  Category.BOOKS,    "44.00"},
                {"TY-1", "Wooden Train",        Category.TOYS,     "19.90"},
                {"TY-2", "Puzzle 1000",         Category.TOYS,     "14.50"},
                {"FD-1", "Coffee Beans",        Category.FOOD,     "9.90"},
                {"FD-2", "Dark Chocolate",      Category.FOOD,     "3.50"},
                {"FD-3", "Green Tea",           Category.FOOD,     "6.80"}};

        var ps = new ArrayList<Product>();
        for (int i = 0; i < data.length; i++) {
            var p = new Product((String) data[i][0], (String) data[i][1],
                    (Category) data[i][2], Money.eur((String) data[i][3]),
                    5 + rnd.nextInt(40));
            p.tag(ts.get(i % ts.size()));
            ps.add(p);
        }
        products.saveAll(ps);

        var statuses = OrderStatus.values();
        for (int i = 0; i < 30; i++) {
            var status = statuses[rnd.nextInt(statuses.length)];
            var placedAt = Instant.now().minus(rnd.nextInt(60), ChronoUnit.DAYS);
            var o = new Order(cs.get(i % cs.size()), status, placedAt);
            var pool = new ArrayList<>(ps);
            Collections.shuffle(pool, rnd);
            int lines = 2 + rnd.nextInt(3);          
            for (int j = 0; j < lines; j++) {
                o.addLine(pool.get(j), 1 + rnd.nextInt(3));
            }
            orders.save(o);
        }
    }

    private static Address addr(String street) {
        return new Address(street, "Phnom Penh", "12000");
    }
}