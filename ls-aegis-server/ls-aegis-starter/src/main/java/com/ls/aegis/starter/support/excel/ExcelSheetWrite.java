package com.ls.aegis.starter.support.excel;

import java.util.List;

/**
 * 多 Sheet 导出描述
 */
public record ExcelSheetWrite(String sheetName, Class<?> headClass, List<?> rows) {

    public static <T> ExcelSheetWrite of(String sheetName, Class<T> headClass, List<T> rows) {
        return new ExcelSheetWrite(sheetName, headClass, rows);
    }
}
