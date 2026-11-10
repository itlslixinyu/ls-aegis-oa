package com.ls.aegis.starter.support.excel;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.URLUtil;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.idev.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import jakarta.servlet.http.HttpServletResponse;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.excel.converter.ExcelBigNumberConverter;
import top.continew.starter.excel.util.ExcelUtils;

import java.util.Date;
import java.util.List;

/**
 * 平台统一 Excel 导出（EasyExcel + {@link ExcelUtils}）
 * <p>
 * 业务侧禁止 {@code new ExcelUtil}，导出走 DTO。
 */
public final class ExcelExportHelper {

    /** 单次导出硬性行数上限，避免 2C1G 环境 OOM */
    public static final int EXPORT_MAX_ROWS = 10000;

    private ExcelExportHelper() {
    }

    /**
     * 将源列表拷贝为导出 DTO 后写出单 Sheet
     *
     * @param source      源数据（实体或已是 DTO）
     * @param exportClass 导出 DTO
     * @param fileName    下载文件名（不含扩展名）
     * @param response    HTTP 响应
     */
    public static <S, T> void export(List<S> source,
                                     Class<T> exportClass,
                                     String fileName,
                                     HttpServletResponse response) {
        List<S> list = source == null ? List.of() : source;
        assertMaxRows(list.size());
        List<T> rows = BeanUtil.copyToList(list, exportClass);
        ExcelUtils.export(rows, fileName, exportClass, response);
    }

    /**
     * 多 Sheet 写出同一工作簿（报表用）
     */
    public static void exportSheets(String fileName, HttpServletResponse response, ExcelSheetWrite... sheets) {
        if (sheets == null || sheets.length == 0) {
            throw new BusinessException("导出 Sheet 不能为空");
        }
        int total = 0;
        for (ExcelSheetWrite sheet : sheets) {
            if (sheet.rows() != null) {
                total += sheet.rows().size();
            }
        }
        assertMaxRows(total);
        try {
            String exportFileName = URLUtil.encode("%s_%s.xlsx".formatted(fileName,
                DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN)));
            response.setHeader("Content-disposition", "attachment;filename=" + exportFileName);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            try (ExcelWriter writer = EasyExcel.write(response.getOutputStream())
                .autoCloseStream(false)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .registerConverter(new ExcelBigNumberConverter())
                .build()) {
                int index = 0;
                for (ExcelSheetWrite sheet : sheets) {
                    WriteSheet writeSheet = EasyExcel.writerSheet(index++, sheet.sheetName())
                        .head(sheet.headClass())
                        .build();
                    writer.write(sheet.rows() == null ? List.of() : sheet.rows(), writeSheet);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("导出 Excel 失败");
        }
    }

    private static void assertMaxRows(int size) {
        if (size > EXPORT_MAX_ROWS) {
            throw new BusinessException("导出数据量过大（当前 " + size + " 行），请缩小筛选条件后分批次导出（单次最多 "
                + EXPORT_MAX_ROWS + " 行）");
        }
    }
}
