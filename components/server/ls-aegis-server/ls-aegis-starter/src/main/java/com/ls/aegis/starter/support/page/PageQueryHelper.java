package com.ls.aegis.starter.support.page;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.util.List;
import java.util.function.Supplier;

/**
 * XML Mapper + PageHelper 分页封装（对齐平台 {@link PageQuery} / {@link PageResp}）。
 * <p>
 * 业务 Controller 禁止直接 {@code PageHelper.startPage}；列表用 {@link #page}，导出用 {@link #startExportLimit}。
 * MyBatis-Plus 域（如 PSI）继续用 {@code PageResp.build(page)}，不要走本类。
 */
public final class PageQueryHelper {

    private PageQueryHelper() {
    }

    /**
     * 按平台分页参数开启 PageHelper（下一句查询生效）
     */
    private static void startPage(PageQuery pageQuery) {
        int page = pageQuery != null && pageQuery.getPage() != null ? pageQuery.getPage() : 1;
        int size = pageQuery != null && pageQuery.getSize() != null ? pageQuery.getSize() : 10;
        PageHelper.startPage(page, size);
    }

    /**
     * 导出前限制行数（不计总数）
     */
    public static void startExportLimit() {
        PageHelper.startPage(1, ExcelExportHelper.EXPORT_MAX_ROWS, false);
    }

    /**
     * 将 PageHelper 结果转为平台 {@link PageResp}
     */
    public static <T> PageResp<T> toPageResp(List<T> list) {
        PageInfo<T> pageInfo = new PageInfo<>(list);
        PageResp<T> resp = new PageResp<>();
        resp.setList(list);
        resp.setTotal(pageInfo.getTotal());
        return resp;
    }

    /**
     * 开启分页并执行查询，返回平台分页体（推荐列表接口使用）
     */
    public static <T> PageResp<T> page(PageQuery pageQuery, Supplier<List<T>> query) {
        startPage(pageQuery);
        return toPageResp(query.get());
    }
}
