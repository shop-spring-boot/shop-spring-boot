package edu.itc.shop.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import edu.itc.shop.domain.Category;
import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Product;
import edu.itc.shop.persistence.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "logging.level.org.hibernate.SQL=DEBUG",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF"
})
class StockReservationTest {

    @Autowired
    StockService stockService;

    @Autowired
    ProductRepository products;

    @Test
    void threeReservationStrategiesNeverOversellUnderTenConcurrentThreads() throws Exception {
        measure("pessimistic", stockService::reserve);
        measure("optimistic", stockService::reserveOptimistically);
        measure("atomic", stockService::reserveAtomically);
    }

    private void measure(String strategy, BiConsumer<Long, Integer> reservation) throws Exception {
        String sku = "STOCK-" + UUID.randomUUID().toString().replace("-", "");
        Product product = products.saveAndFlush(new Product(
                sku, "Concurrency test product", Category.HARDWARE, Money.eur("1.00"), 5));
        long productId = product.getId();
        stockService.resetOptimisticRetryCount();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch ready = new CountDownLatch(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        long startedAt;
        try {
            for (int i = 0; i < 10; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Reservation start barrier timed out");
                    }
                    try {
                        reservation.accept(productId, 1);
                        return true;
                    } catch (OutOfStockException ex) {
                        return false;
                    }
                }));
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            startedAt = System.nanoTime();
            start.countDown();

            int successes = 0;
            int failures = 0;
            for (Future<Boolean> result : results) {
                if (result.get(60, TimeUnit.SECONDS)) {
                    successes++;
                } else {
                    failures++;
                }
            }
            long elapsedNanos = System.nanoTime() - startedAt;
            Product saved = products.findById(productId).orElseThrow();

            assertThat(successes).isEqualTo(5);
            assertThat(failures).isEqualTo(5);
            assertThat(saved.getStock()).isZero();
            assertThat(saved.getStock()).isGreaterThanOrEqualTo(0);
            System.out.printf("Stock benchmark %-11s elapsed=%d ms throughput=%.2f reservations/s retries=%d%n",
                    strategy, TimeUnit.NANOSECONDS.toMillis(elapsedNanos),
                    10_000_000_000.0 / elapsedNanos, stockService.optimisticRetryCount());
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            products.deleteById(productId);
        }
    }
}
