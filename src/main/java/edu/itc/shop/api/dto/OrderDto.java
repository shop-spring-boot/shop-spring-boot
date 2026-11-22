package edu.itc.shop.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import edu.itc.shop.domain.OrderLine;
import edu.itc.shop.domain.OrderStatus;

/** An order as the client sees it: the data plus "_links" (HATEOAS). */
public record OrderDto(String id, OrderStatus status, BigDecimal total, List<OrderLine> lines,
                       Map<String, String> _links) { }
