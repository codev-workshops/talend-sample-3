package com.migration.etl.service;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvLoaderServiceTest {

    private String resourcePath(String fileName) {
        return getClass().getClassLoader().getResource(fileName).getPath();
    }

    @Test
    void testParseCustCsv_returns15Rows() throws IOException {
        CsvLoaderService service = new CsvLoaderService(resourcePath("ps_cust.csv"));
        List<String[]> rows = service.loadCsv();
        assertEquals(15, rows.size());
    }

    @Test
    void testParseFlavorCsv_returns6Rows() throws IOException {
        CsvLoaderService service = new CsvLoaderService(resourcePath("ps_flavor.csv"));
        List<String[]> rows = service.loadCsv();
        assertEquals(6, rows.size());
    }

    @Test
    void testParseCustEmptyCsv_returns0RowsAndWarns() throws IOException {
        CsvLoaderService service = new CsvLoaderService(resourcePath("ps_cust_empty.csv"));
        List<String[]> rows = service.loadCsv();
        assertEquals(0, rows.size());
    }

    @Test
    void testParseFlavorEmptyCsv_returns0Rows() throws IOException {
        CsvLoaderService service = new CsvLoaderService(resourcePath("ps_flavor_empty.csv"));
        List<String[]> rows = service.loadCsv();
        assertEquals(0, rows.size());
    }
}
