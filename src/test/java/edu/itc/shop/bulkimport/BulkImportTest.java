package edu.itc.shop.bulkimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import edu.itc.shop.domain.Category;
import edu.itc.shop.domain.Product;
import edu.itc.shop.persistence.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(properties = {
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF"
})
class BulkImportTest {

    private static final int ROW_COUNT = 50_000;

    @Autowired
    BulkImportService imports;

    @Autowired
    ProductRepository products;

    @Autowired
    JdbcClient jdbc;

    @TempDir
    Path tempDir;

    @Test
    void benchmarksAllStrategiesAndVerifiesAllFiftyThousandRows() throws Exception {
        List<ImportStrategy> strategies = List.of(
                new ImportStrategy("StatelessSession", imports::importCsv),
                new ImportStrategy("saveAll without clear", imports::importWithSaveAll),
                new ImportStrategy("saveAll with flush/clear", imports::importWithSaveAllAndFlushClear));

        for (ImportStrategy strategy : strategies) {
            String prefix = "BULK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            Path csv = writeCsv(prefix, ROW_COUNT);
            System.gc();
            Thread.sleep(100);
            BulkImportResult result = strategy.importer().apply(csv);

            assertThat(result.importedRows()).isEqualTo(ROW_COUNT);
            assertThat(result.batchCount()).isEqualTo(100);
            assertThat(products.countBySkuStartingWith(prefix + "-")).isEqualTo(ROW_COUNT);
            Product sample = products.findBySku(prefix + "-00000").orElseThrow();
            assertThat(sample.getName()).isEqualTo("Product 00000");
            assertThat(sample.getPrice().amount()).isEqualByComparingTo("12.34");
            assertThat(sample.getPrice().currency()).isEqualTo("USD");
            assertThat(sample.getCategory()).isEqualTo(Category.HARDWARE);
            assertThat(sample.getStock()).isEqualTo(20);

            System.out.printf("Bulk import %-24s rows=%d batches=%d wall=%d ms peakHeap=%d bytes throughput=%.2f rows/s%n",
                    strategy.name(), result.importedRows(), result.batchCount(),
                    result.wallTimeMillis(), result.peakHeapBytes(), result.rowsPerSecond());
            jdbc.sql("delete from product where sku like :prefix")
                    .param("prefix", prefix + "-%")
                    .update();
        }
    }

    @Test
    void malformedCsvRollsBackPreviouslyInsertedChunks() throws Exception {
        String prefix = "BAD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Path csv = tempDir.resolve("malformed.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
            writer.write("sku;name;amount;currency;category;stock\n");
            for (int i = 0; i < 500; i++) {
                writer.write(row(prefix + "-" + i, "Product " + i));
                writer.newLine();
            }
            writer.write(prefix + "-bad;Broken;not-money;USD;HARDWARE;1");
            writer.newLine();
        }

        assertThatThrownBy(() -> imports.importCsv(csv)).isInstanceOf(IllegalArgumentException.class);
        assertThat(products.countBySkuStartingWith(prefix + "-")).isZero();
    }

    @Test
    void duplicateSkuFailsAndRollsBackTheWholeImport() throws Exception {
        String prefix = "DUP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Path csv = tempDir.resolve("duplicate.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
            writer.write("sku;name;amount;currency;category;stock\n");
            writer.write(row(prefix + "-same", "First"));
            writer.newLine();
            writer.write(row(prefix + "-same", "Second"));
            writer.newLine();
        }

        assertThatThrownBy(() -> imports.importCsv(csv)).isInstanceOf(RuntimeException.class);
        assertThat(products.countBySkuStartingWith(prefix + "-")).isZero();
    }

    private Path writeCsv(String prefix, int count) throws Exception {
        Path csv = tempDir.resolve(prefix + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
            writer.write("sku;name;amount;currency;category;stock");
            writer.newLine();
            for (int i = 0; i < count; i++) {
                writer.write(row(prefix + "-" + "%05d".formatted(i), "Product %05d".formatted(i)));
                writer.newLine();
            }
        }
        return csv;
    }

    private String row(String sku, String name) {
        return sku + ";" + name + ";12.34;USD;HARDWARE;20";
    }

    private record ImportStrategy(String name, Function<Path, BulkImportResult> importer) {
    }
}
