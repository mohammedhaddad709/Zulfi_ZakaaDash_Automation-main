package com.taf.core.utils.dataReader;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.file.*;

/** Low-level Excel cells reader kept from TAF (no Test Cases imported yet). */
public final class ExcelReader {
    private ExcelReader() { }
    public static String getExcelData(String excelFilename, String sheetName, int rowNum, int colNum) {
        Path root = Path.of("src", "test", "resources", "test-data").toAbsolutePath().normalize();
        Path file = root.resolve(excelFilename).normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Invalid Excel data path");
        try (InputStream data = Files.newInputStream(file); Workbook workbook = new XSSFWorkbook(data)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new IllegalArgumentException("Missing sheet: " + sheetName);
            Row row = sheet.getRow(rowNum);
            if (row == null) return "";
            Cell cell = row.getCell(colNum);
            if (cell == null) return "";
            return new DataFormatter().formatCellValue(cell);
        } catch (IOException e) { throw new IllegalStateException("Unable to read Excel input: " + file.getFileName(), e); }
    }
}
