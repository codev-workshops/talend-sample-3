package com.migration.etl.job;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.migration.etl.service.CsvLoaderService;

public class CustomerSetupJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(CustomerSetupJob.class);

    private final JdbcTemplate jdbcTemplate;
    private final String csvFilePath;

    public CustomerSetupJob(JdbcTemplate jdbcTemplate, String csvFilePath) {
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

        jdbcTemplate.execute("DROP TABLE IF EXISTS customer");
        jdbcTemplate.execute("CREATE TABLE customer (id INT, first_name VARCHAR(100), "
                + "last_name VARCHAR(100), city VARCHAR(100), state_cd VARCHAR(100), flavor INT)");

        for (String[] row : rows) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO customer (id, first_name, last_name, city, state_cd, flavor) VALUES (?, ?, ?, ?, ?, ?)",
                        Integer.parseInt(row[0]),
                        row[1],
                        row[2],
                        row[3],
                        row[4],
                        Integer.parseInt(row[5]));
            } catch (Exception e) {
                logger.error("Error inserting customer row: {}", e.getMessage());
            }
        }
    }
}
