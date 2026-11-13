package edu.itc.shop.stock;

public class OutOfStockException extends RuntimeException {

    public OutOfStockException(Long productId, int requestedQuantity) {
        super("Product " + productId + " does not have " + requestedQuantity + " units available");
    }
}
