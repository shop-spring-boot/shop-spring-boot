package edu.itc.shop.domain;

import jakarta.ws.rs.ClientErrorException;

public class InsufficientStockException extends ClientErrorException {
    public InsufficientStockException(String sku) {
        super("Not enough stock for " + sku, 409);
    }
}
