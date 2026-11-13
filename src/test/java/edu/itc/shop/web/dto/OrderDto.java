package edu.itc.shop.web.dto;

import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.OrderStatus;

public record OrderDto(Long id, OrderStatus status, int lineCount, Money total) {
}
