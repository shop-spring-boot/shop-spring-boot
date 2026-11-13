package edu.itc.shop.orderfeed;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Order;
import edu.itc.shop.persistence.OrderJpaRepository;

@Service
public class OrderFeedService {

    private static final Sort FEED_SORT = Sort.by(
            Sort.Order.asc("placedAt"),
            Sort.Order.asc("id"));

    private final OrderJpaRepository orders;

    public OrderFeedService(OrderJpaRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public OrderFeedResponse<OrderSummary> slice(int size, String cursorValue) {
        validateSize(size);
        OrderCursor cursor = OrderCursor.parse(cursorValue);
        PageRequest request = PageRequest.of(0, size, FEED_SORT);
        Slice<OrderSummary> slice = cursor == null
                ? orders.findAllSummaries(request)
                : orders.findSummariesAfter(cursor.placedAt(), cursor.id(), request);
        String nextCursor = slice.hasNext() && !slice.isEmpty()
                ? slice.getContent().getLast().cursor()
                : null;
        return new OrderFeedResponse<>(slice.getContent(), nextCursor, slice.hasNext());
    }

    @Transactional(readOnly = true)
    public Window<Order> scroll(int size, String cursorValue) {
        validateSize(size);
        OrderCursor cursor = OrderCursor.parse(cursorValue);
        ScrollPosition position = cursor == null
                ? ScrollPosition.keyset()
                : cursor.toScrollPosition();
        return orders.findBy(
                (root, query, cb) -> cb.conjunction(),
                fluentQuery -> fluentQuery
                        .sortBy(FEED_SORT)
                        .limit(size)
                        .scroll(position));
    }

    public static void validateSize(int size) {
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
