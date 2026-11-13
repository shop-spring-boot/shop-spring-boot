package edu.itc.shop.stock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Product;
import edu.itc.shop.persistence.ProductRepository;

@Service
public class OptimisticReservationAttempt {

    private final ProductRepository products;

    public OptimisticReservationAttempt(ProductRepository products) {
        this.products = products;
    }

    @Transactional
    public void reserve(Long productId, int quantity) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product " + productId + " not found"));
        if (product.getStock() < quantity) {
            throw new OutOfStockException(productId, quantity);
        }
        product.setStock(product.getStock() - quantity);
        products.flush();
    }
}
