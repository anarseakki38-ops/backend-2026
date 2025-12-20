package com.enterprise.reportgenerator.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ExcelGenerator {

    public static void generateExcel(List<Map<String, Object>> data, String filePath) throws IOException {
        if (data == null || data.isEmpty()) {
            log.warn("No data to write to Excel: {}", filePath);
            return;
        }

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) { // Keep 100 rows in memory
            Sheet sheet = workbook.createSheet("Report");

            // Create Header
            Row headerRow = sheet.createRow(0);
            Map<String, Object> firstRow = data.get(0);
            int colIndex = 0;
            for (String key : firstRow.keySet()) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(key);
            }

            // Populate Data
            AtomicInteger rowIndex = new AtomicInteger(1);
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowIndex.getAndIncrement());
                int cellIndex = 0;
                for (Object value : rowData.values()) {
                    Cell cell = row.createCell(cellIndex++);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value != null ? value.toString() : "");
                    }
                }
            }

            // Write file
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
            workbook.dispose(); // Dispose temporary files
            log.info("Excel generated successfully at {}", filePath);
        }
    }
}
