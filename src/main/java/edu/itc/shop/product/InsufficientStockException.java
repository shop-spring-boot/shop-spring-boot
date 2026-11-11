package edu.itc.shop.product;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(long id, int currentStock, int requestedDelta) {
        super("Product " + id + " has " + currentStock + " units, cannot remove " + Math.abs(requestedDelta));
    }
}