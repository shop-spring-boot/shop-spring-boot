package edu.itc.shop.bulkimport;

import java.nio.file.Path;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.persistence.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class JpaBulkImportWorker {

    private final ProductRepository products;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaBulkImportWorker(ProductRepository products) {
        this.products = products;
    }

    @Transactional
    public ProductCsvReader.ImportCount saveAllWithoutClearing(Path csv) {
        return ProductCsvReader.forEachChunk(csv, products::saveAll);
    }

    @Transactional
    public ProductCsvReader.ImportCount saveAllWithFlushAndClear(Path csv) {
        return ProductCsvReader.forEachChunk(csv, chunk -> {
            products.saveAll(chunk);
            entityManager.flush();
            entityManager.clear();
        });
    }
}
