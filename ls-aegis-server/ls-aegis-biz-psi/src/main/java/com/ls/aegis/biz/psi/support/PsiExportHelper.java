package com.ls.aegis.biz.psi.support;

import com.ls.aegis.starter.support.excel.ExcelExportHelper;
import jakarta.servlet.http.HttpServletResponse;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.util.function.BiFunction;

/**
 * PSI 列表导出辅助（按当前筛选条件导出，上限 {@link ExcelExportHelper#EXPORT_MAX_ROWS}）
 */
public final class PsiExportHelper {

    private PsiExportHelper() {
    }

    public static PageQuery maxPage() {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(1);
        pageQuery.setSize(ExcelExportHelper.EXPORT_MAX_ROWS);
        return pageQuery;
    }

    public static <Q, R, E> void export(Q query,
                                        BiFunction<Q, PageQuery, PageResp<R>> pageFn,
                                        Class<E> exportClass,
                                        String sheetName,
                                        HttpServletResponse response) {
        PageResp<R> page = pageFn.apply(query, maxPage());
        ExcelExportHelper.export(page.getList(), exportClass, sheetName, response);
    }
}
