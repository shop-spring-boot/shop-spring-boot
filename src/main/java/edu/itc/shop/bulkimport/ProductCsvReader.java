package edu.itc.shop.bulkimport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import edu.itc.shop.domain.Category;
import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Product;

final class ProductCsvReader {

    private static final String HEADER = "sku;name;amount;currency;category;stock";
    private static final int CHUNK_SIZE = 500;

    private ProductCsvReader() {
    }

    static ImportCount forEachChunk(Path csv, Consumer<List<Product>> consumer) {
        int rows = 0;
        int batches = 0;
        int lineNumber = 1;
        List<Product> chunk = new ArrayList<>(CHUNK_SIZE);

        try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (!HEADER.equals(header)) {
                throw new IllegalArgumentException("CSV header must be " + HEADER);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    chunk.add(parse(line));
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("Invalid product CSV at line " + lineNumber, ex);
                }
                if (chunk.size() == CHUNK_SIZE) {
                    consumer.accept(chunk);
                    rows += chunk.size();
                    batches++;
                    chunk.clear();
                }
            }
            if (!chunk.isEmpty()) {
                consumer.accept(chunk);
                rows += chunk.size();
                batches++;
            }
            return new ImportCount(rows, batches);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read product CSV: " + csv, ex);
        }
    }

    private static Product parse(String line) {
        List<String> fields = fields(line);
        if (fields.size() != 6) {
            throw new IllegalArgumentException("Expected 6 columns but found " + fields.size());
        }
        String sku = fields.get(0).trim();
        String name = fields.get(1).trim();
        String amountText = fields.get(2).trim();
        String currency = fields.get(3).trim().toUpperCase(java.util.Locale.ROOT);
        Category category = Category.valueOf(fields.get(4).trim().toUpperCase(java.util.Locale.ROOT));
        int stock = Integer.parseInt(fields.get(5).trim());
        BigDecimal amount = new BigDecimal(amountText);

        if (sku.isEmpty() || sku.length() > 40 || name.isEmpty() || name.length() > 80) {
            throw new IllegalArgumentException("SKU and name must fit their database columns");
        }
        if (amount.signum() < 0 || amount.precision() > 12 || amount.scale() > 2) {
            throw new IllegalArgumentException("Amount must be non-negative with at most 2 decimal places");
        }
        if (currency.length() != 3 || stock < 0) {
            throw new IllegalArgumentException("Currency must have 3 letters and stock cannot be negative");
        }
        return new Product(sku, name, category, new Money(amount, currency), stock);
    }

    private static List<String> fields(String line) {
        List<String> result = new ArrayList<>(6);
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closedQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (quoted) {
                if (current == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closedQuote = true;
                    }
                } else {
                    field.append(current);
                }
            } else if (current == ';') {
                result.add(field.toString());
                field.setLength(0);
                closedQuote = false;
            } else if (current == '"' && field.isEmpty() && !closedQuote) {
                quoted = true;
            } else if (closedQuote) {
                throw new IllegalArgumentException("Unexpected character after quoted field");
            } else if (current == '"') {
                throw new IllegalArgumentException("Quote must begin a field");
            } else {
                field.append(current);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed quoted field");
        }
        result.add(field.toString());
        return result;
    }

    record ImportCount(int rows, int batches) {
    }
}
