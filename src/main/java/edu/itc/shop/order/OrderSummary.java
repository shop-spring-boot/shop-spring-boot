package edu.itc.shop.order;

import java.time.Instant;
import edu.itc.shop.domain.OrderStatus;

public interface OrderSummary {
    Long getId();
    OrderStatus getStatus();
    Instant getPlacedAt();
    CustomerName getCustomer();

    interface CustomerName {
        String getName();
    }
}