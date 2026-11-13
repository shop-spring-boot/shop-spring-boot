package edu.itc.shop.order;

import edu.itc.shop.audit.Audited;
import edu.itc.shop.config.ShopProperties;
import edu.itc.shop.product.JdbcProductRepository;
import edu.itc.shop.notification.Notifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final JdbcClientOrderRepository orders;
    private final JdbcProductRepository products;
    private final ShopProperties props;
    private final Notifier notifier;

    public OrderService(JdbcClientOrderRepository orders, JdbcProductRepository products,
                        ShopProperties props, @Qualifier("emailNotifier") Notifier notifier) {
        this.orders = orders;
        this.products = products;
        this.props = props;
        this.notifier = notifier;
    }

    @Audited
    @Transactional
    public OrderDto place(PlaceOrder cmd) {
        // 1. no duplicate productId
        Set<Long> seen = new HashSet<>();
        for (PlaceOrder.Line line : cmd.lines()) {
            if (!seen.add(line.productId())) {
                throw new IllegalArgumentException("Duplicate product " + line.productId());
            }
        }

        // 2 + 3. reserve stock for every line, snapshot prices, sum the net
        List<OrderLine> lines = new ArrayList<>();
        BigDecimal net = BigDecimal.ZERO;
        for (PlaceOrder.Line line : cmd.lines()) {
            if (products.updateStock(line.productId(), -line.qty()) == 0) {
                throw new InsufficientStockException(line.productId());
            }
            BigDecimal price = products.findById(line.productId()).orElseThrow().price();
            lines.add(new OrderLine(line.productId(), line.qty(), price));
            net = net.add(price.multiply(BigDecimal.valueOf(line.qty())));
        }

        // total = net x (1 + taxRate), rounded HALF_UP to 2 decimals
        BigDecimal total = net.multiply(BigDecimal.ONE.add(props.taxRate()))
                .setScale(2, RoundingMode.HALF_UP);

        // 4 + 5. save and return
        Order order = new Order(null, cmd.customerId(), OrderStatus.NEW, LocalDateTime.now(), total);
        long id = orders.insert(order, lines);
        return new OrderDto(id, OrderStatus.NEW, total, lines);
    }

    @Audited
    @Transactional
    public OrderDto cancel(long id) {
        Order order = orders.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        if (order.status() != OrderStatus.NEW) {
            throw new OrderNotCancellableException(id, order.status());
        }
        List<OrderLine> lines = orders.findLines(id);
        for (OrderLine line : lines) {
            products.updateStock(line.productId(), line.qty());
        }
        orders.updateStatus(id, OrderStatus.CANCELLED);
        notifier.send("customer-" + order.customerId(), "Order " + id + " cancelled");
        return new OrderDto(id, OrderStatus.CANCELLED, order.total(), lines);
    }

    @Transactional(readOnly = true)
    public OrderDto get(long id) {
        Order order = orders.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> list(Long customerId, OrderStatus status, int page, int size) {
        List<OrderDto> content = orders.search(customerId, status, page, size).stream()
                .map(this::toDto)
                .toList();
        return new Page<>(content, page, size, orders.count(customerId, status));
    }

    private OrderDto toDto(Order order) {
        return new OrderDto(order.id(), order.status(), order.total(),
                orders.findLines(order.id()));
    }
}