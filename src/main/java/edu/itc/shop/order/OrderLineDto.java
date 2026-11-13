package edu.itc.shop.order;

import java.math.BigDecimal;

public record OrderLineDto(String sku, String productName, int quantity, BigDecimal unitPrice) { }