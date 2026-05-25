package com.migration.etl.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(CsvLoaderService.class);
    private static final Charset ISO_8859_15 = Charset.forName("ISO-8859-15");
    private static final String DELIMITER = "\\|";

    private final String filePath;

    public CsvLoaderService(String filePath) {
        this.filePath = filePath;
    }

    public List<String[]> loadCsv() throws IOException {
        List<String[]> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), ISO_8859_15))) {

            // Skip 1 header row
            String headerLine = reader.readLine();
            if (headerLine == null) {
                logger.warn("Code 42: The Customer input file does not have any records / is mepty");
                return rows;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(DELIMITER, -1);
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim();
                }
                rows.add(fields);
            }
        }

        if (rows.isEmpty()) {
            logger.warn("Code 42: The Customer input file does not have any records / is mepty");
        }

        return rows;
    }
}
