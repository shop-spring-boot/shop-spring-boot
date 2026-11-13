package edu.itc.shop.stock;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Product;
import edu.itc.shop.persistence.ProductRepository;
import jakarta.persistence.OptimisticLockException;

@Service
public class StockService {

    private static final int OPTIMISTIC_RETRY_LIMIT = 10;

    private final AtomicLong optimisticRetryCount = new AtomicLong();
    private final ProductRepository products;
    private final OptimisticReservationAttempt optimisticAttempt;

    public StockService(ProductRepository products, OptimisticReservationAttempt optimisticAttempt) {
        this.products = products;
        this.optimisticAttempt = optimisticAttempt;
    }

    @Transactional
    public void reserve(Long productId, int quantity) {
        validateQuantity(quantity);
        Product product = products.findForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product " + productId + " not found"));
        reserveFromProduct(product, quantity);
    }

    public void reserveOptimistically(Long productId, int quantity) {
        validateQuantity(quantity);
        for (int attempt = 1; attempt <= OPTIMISTIC_RETRY_LIMIT; attempt++) {
            try {
                optimisticAttempt.reserve(productId, quantity);
                return;
            } catch (OptimisticLockingFailureException | OptimisticLockException ex) {
                if (attempt == OPTIMISTIC_RETRY_LIMIT) {
                    throw ex;
                }
                optimisticRetryCount.incrementAndGet();
                Thread.yield();
            }
        }
        throw new IllegalStateException("Optimistic reservation retry loop ended unexpectedly");
    }

    long optimisticRetryCount() {
        return optimisticRetryCount.get();
    }

    void resetOptimisticRetryCount() {
        optimisticRetryCount.set(0);
    }

    @Transactional
    public void reserveAtomically(Long productId, int quantity) {
        validateQuantity(quantity);
        if (products.reserveIfAvailable(productId, quantity) == 1) {
            return;
        }
        if (!products.existsById(productId)) {
            throw new IllegalArgumentException("Product " + productId + " not found");
        }
        throw new OutOfStockException(productId, quantity);
    }

    private void reserveFromProduct(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new OutOfStockException(product.getId(), quantity);
        }
        product.setStock(product.getStock() - quantity);
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }
    }
}
