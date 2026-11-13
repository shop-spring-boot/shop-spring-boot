package edu.itc.shop.bulkimport;

public record BulkImportResult(
        int importedRows,
        long wallTimeMillis,
        long peakHeapBytes,
        int batchCount) {

    public double rowsPerSecond() {
        return wallTimeMillis == 0 ? importedRows * 1000.0 : importedRows * 1000.0 / wallTimeMillis;
    }
}
