package edu.itc.shop.order;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Repository
public class JdbcClientOrderRepository {

    private final JdbcClient jdbc;

    public JdbcClientOrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(Order order, List<OrderLine> lines) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO orders(customer_id, status, placed_at, total)
                VALUES (:customerId, :status, :placedAt, :total)
                """)
                .param("customerId", order.customerId())
                .param("status", order.status().name())
                .param("placedAt", order.placedAt())
                .param("total", order.total())
                .update(keyHolder, "id");
        long orderId = keyHolder.getKey().longValue();

        for (OrderLine line : lines) {
            jdbc.sql("""
                    INSERT INTO order_line(order_id, product_id, qty, unit_price)
                    VALUES (:orderId, :productId, :qty, :unitPrice)
                    """)
                    .param("orderId", orderId)
                    .param("productId", line.productId())
                    .param("qty", line.qty())
                    .param("unitPrice", line.unitPrice())
                    .update();
        }
        return orderId;
    }

    public Optional<Order> findById(long id) {
        return jdbc.sql("""
                SELECT id, customer_id, status, placed_at, total
                FROM orders WHERE id = :id
                """)
                .param("id", id)
                .query(this::mapOrder)
                .optional();
    }

    public List<OrderLine> findLines(long orderId) {
        return jdbc.sql("""
                SELECT product_id, qty, unit_price
                FROM order_line WHERE order_id = :orderId
                """)
                .param("orderId", orderId)
                .query((rs, rowNum) -> new OrderLine(
                        rs.getLong("product_id"),
                        rs.getInt("qty"),
                        rs.getBigDecimal("unit_price")))
                .list();
    }

    public int updateStatus(long id, OrderStatus status) {
        return jdbc.sql("UPDATE orders SET status = :status WHERE id = :id")
                .param("status", status.name())
                .param("id", id)
                .update();
    }

    private Order mapOrder(ResultSet rs, int rowNum) throws SQLException {
        return new Order(
            rs.getLong("id"),
            rs.getLong("customer_id"),
            OrderStatus.valueOf(rs.getString("status")),
            rs.getObject("placed_at", java.time.LocalDateTime.class),
            rs.getBigDecimal("total")
        );
    }

    public List<Order> search(Long customerId, OrderStatus status, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT id, customer_id, status, placed_at, total FROM orders"
                + where(customerId, status, params)
                + " ORDER BY id DESC LIMIT :limit OFFSET :offset";
        params.put("limit", size);
        params.put("offset", page * size);
        return jdbc.sql(sql).params(params).query(this::mapOrder).list();
    }

    public long count(Long customerId, OrderStatus status) {
        Map<String, Object> params = new HashMap<>();
        String sql = "SELECT COUNT(*) FROM orders" + where(customerId, status, params);
        return jdbc.sql(sql).params(params).query(Long.class).single();
    }

    private String where(Long customerId, OrderStatus status, Map<String, Object> params) {
        List<String> conditions = new ArrayList<>();
        if (customerId != null) {
            conditions.add("customer_id = :customerId");
            params.put("customerId", customerId);
        }
        if (status != null) {
            conditions.add("status = :status");
            params.put("status", status.name());
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }
}