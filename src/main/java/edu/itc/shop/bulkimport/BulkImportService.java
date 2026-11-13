package edu.itc.shop.bulkimport;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManagerFactory;

@Service
public class BulkImportService {

    private final EntityManagerFactory entityManagerFactory;
    private final JpaBulkImportWorker jpaWorker;

    public BulkImportService(EntityManagerFactory entityManagerFactory, JpaBulkImportWorker jpaWorker) {
        this.entityManagerFactory = entityManagerFactory;
        this.jpaWorker = jpaWorker;
    }

    public BulkImportResult importCsv(Path csv) {
        return measure(() -> importStateless(csv));
    }

    public BulkImportResult importWithSaveAll(Path csv) {
        return measure(() -> jpaWorker.saveAllWithoutClearing(csv));
    }

    public BulkImportResult importWithSaveAllAndFlushClear(Path csv) {
        return measure(() -> jpaWorker.saveAllWithFlushAndClear(csv));
    }

    private ProductCsvReader.ImportCount importStateless(Path csv) {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                ProductCsvReader.ImportCount count =
                        ProductCsvReader.forEachChunk(csv, session::insertMultiple);
                transaction.commit();
                return count;
            } catch (RuntimeException ex) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw ex;
            }
        }
    }

    private BulkImportResult measure(Supplier<ProductCsvReader.ImportCount> operation) {
        long startedAt = System.nanoTime();
        try (PeakHeapSampler sampler = new PeakHeapSampler()) {
            ProductCsvReader.ImportCount count = operation.get();
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            return new BulkImportResult(count.rows(), elapsedMillis, sampler.finish(), count.batches());
        }
    }

    private static final class PeakHeapSampler implements AutoCloseable {
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final AtomicLong peakBytes = new AtomicLong(usedHeap());
        private final Thread sampler;
        private long result = -1;

        private PeakHeapSampler() {
            sampler = new Thread(() -> {
                while (running.get()) {
                    peakBytes.accumulateAndGet(usedHeap(), Math::max);
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, "bulk-import-heap-sampler");
            sampler.setDaemon(true);
            sampler.start();
        }

        private long finish() {
            close();
            return result;
        }

        @Override
        public void close() {
            if (result >= 0) {
                return;
            }
            running.set(false);
            try {
                sampler.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while measuring peak heap", ex);
            }
            peakBytes.accumulateAndGet(usedHeap(), Math::max);
            result = peakBytes.get();
        }

        private static long usedHeap() {
            return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }
    }
}
