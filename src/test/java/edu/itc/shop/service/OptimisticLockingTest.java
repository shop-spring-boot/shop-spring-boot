package edu.itc.shop.service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderStatus;
import edu.itc.shop.order.OrderRepository;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class OptimisticLockingTest {

    @Autowired OrderRepository orders;
    @Autowired TransactionTemplate tx;
    @Autowired TestData data;
    @Autowired OrderFacade facade;

    @Test
    void secondWriterFailsWithOptimisticLock() throws Exception {
        Long orderId = data.newOrder();
        long versionBefore = orders.findById(orderId).orElseThrow().getVersion();
        CountDownLatch bothLoaded = new CountDownLatch(2);
        Function<OrderStatus, Callable<Throwable>> writer = target -> () -> {
            try {
                tx.executeWithoutResult(status -> {
                    Order o = orders.findById(orderId).orElseThrow();   // both read the same version
                    bothLoaded.countDown();
                    await(bothLoaded);
                    o.setStatus(target);                                // dirty: UPDATE at commit
                });
                return null;
            } catch (RuntimeException e) {
                return e;
            }
        };
        List<OrderStatus> targets = List.of(OrderStatus.PAID, OrderStatus.CANCELLED);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Throwable>> results = pool.invokeAll(targets.stream().map(writer).toList());
            List<Throwable> failures = results.stream().map(this::get).filter(Objects::nonNull).toList();
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0)).isInstanceOf(ObjectOptimisticLockingFailureException.class);

            OrderStatus winner = null;                                  // invokeAll keeps the list order
            for (int i = 0; i < results.size(); i++) {
                if (get(results.get(i)) == null) {
                    winner = targets.get(i);
                }
            }
            Order after = orders.findById(orderId).orElseThrow();
            assertThat(after.getVersion()).isEqualTo(versionBefore + 1);
            assertThat(after.getStatus()).isEqualTo(winner);
        }
    }

    @Test
    void facadeRetriesSoBothWritersSucceed() throws Exception {
        Long orderId = data.newOrder();
        long versionBefore = orders.findById(orderId).orElseThrow().getVersion();
        CountDownLatch holding = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            // Writer 1 changes the row and keeps its transaction open for a moment (row is locked).
            Future<?> first = pool.submit(() -> tx.executeWithoutResult(status -> {
                Order o = orders.findById(orderId).orElseThrow();
                o.setStatus(OrderStatus.PAID);
                orders.flush();                                         // UPDATE sent, version+1 uncommitted
                holding.countDown();
                sleep(300);                                             // facade loads the old version meanwhile
            }));
            // Writer 2 goes through the facade: its first commit conflicts, the retry succeeds.
            Future<?> second = pool.submit(() -> {
                await(holding);
                facade.changeStatusWithRetry(orderId, OrderStatus.CANCELLED);
            });
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        Order after = orders.findById(orderId).orElseThrow();
        assertThat(after.getVersion()).isEqualTo(versionBefore + 2);
        assertThat(after.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    private static void await(CountDownLatch l) {
        try {
            if (!l.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("latch timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Throwable get(Future<Throwable> f) {
        try {
            return f.get();
        } catch (Exception e) {
            return e;
        }
    }
}