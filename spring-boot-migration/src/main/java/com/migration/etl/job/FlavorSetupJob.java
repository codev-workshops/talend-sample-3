package com.migration.etl.job;

import java.io.IOException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import com.migration.etl.service.CsvLoaderService;

public class FlavorSetupJob implements Job {

    private final JdbcTemplate jdbcTemplate;
    private final String csvFilePath;

    public FlavorSetupJob(JdbcTemplate jdbcTemplate, String csvFilePath) {
        this.jdbcTemplate = jdbcTemplate;
        this.csvFilePath = csvFilePath;
    }

    @Override
    public void run() {
        CsvLoaderService loader = new CsvLoaderService(csvFilePath);
        List<String[]> rows;
        try {
            rows = loader.loadCsv();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + csvFilePath, e);
        }

        jdbcTemplate.execute("DROP TABLE IF EXISTS flavors");
        jdbcTemplate.execute("CREATE TABLE flavors (flavor_id INT, flavor_name VARCHAR(100))");

        for (String[] row : rows) {
            jdbcTemplate.update(
                    "INSERT INTO flavors (flavor_id, flavor_name) VALUES (?, ?)",
                    Integer.parseInt(row[0]),
                    row[1]);
        }
    }
}
