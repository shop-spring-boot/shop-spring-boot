package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryProductRepository implements ProductRepository {

    private final ConcurrentHashMap<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public InMemoryProductRepository() {
        save(new Product(0, "Kettle", "KT-01", new BigDecimal("24.90"), 12, 0));
        save(new Product(0, "Mug", "MG-01", new BigDecimal("6.50"), 40, 0));
        save(new Product(0, "Tea", "TE-01", new BigDecimal("4.20"), 0, 0));
        // 30 more products so that paging is visible (33 in total).
        for (int i = 1; i <= 30; i++) {
            save(new Product(0, String.format("Item %02d", i), String.format("IT-%02d", i),
                    new BigDecimal("10.00").add(BigDecimal.valueOf(i)), i % 7, 0));
        }
    }

    @Override
    public List<Product> findAll() {
        List<Product> all = new ArrayList<>(store.values());
        all.sort(Comparator.comparingLong(Product::id));
        return all;
    }

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return store.values().stream().filter(p -> p.sku().equals(sku)).findFirst();
    }

    @Override
    public Product save(Product p) {
        long id = p.id() == 0 ? ids.incrementAndGet() : p.id();
        Product saved = new Product(id, p.name(), p.sku(), p.price(), p.stock(), p.version() + 1);
        store.put(id, saved);
        return saved;
    }

    @Override
    public boolean delete(long id) {
        return store.remove(id) != null;
    }

    @Override
    public Product adjustStock(long id, int delta) {
        return store.compute(id, (k, p) -> {
            if (p == null) return null;
            if (p.stock() + delta < 0) throw new InsufficientStockException(p.sku());
            return new Product(p.id(), p.name(), p.sku(), p.price(), p.stock() + delta, p.version() + 1);
        });
    }
}
