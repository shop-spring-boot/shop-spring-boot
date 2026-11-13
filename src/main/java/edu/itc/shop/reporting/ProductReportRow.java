package edu.itc.shop.reporting;

import java.math.BigDecimal;

public record ProductReportRow(String sku, String name, BigDecimal amount, int stock, long tagCount) {
}
