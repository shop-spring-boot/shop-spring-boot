package edu.itc.shop.product;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ProductRow(long id, String name, BigDecimal price, BigDecimal gross, int stock, boolean low) {

    public static ProductRow of(ProductDto d, BigDecimal taxRate, int lowStockThreshold) {
        BigDecimal gross = d.price()
                .multiply(BigDecimal.ONE.add(taxRate))
                .setScale(2, RoundingMode.HALF_UP);
        return new ProductRow(d.id(), d.name(), d.price(), gross, d.stock(), d.stock() < lowStockThreshold);
    }
}