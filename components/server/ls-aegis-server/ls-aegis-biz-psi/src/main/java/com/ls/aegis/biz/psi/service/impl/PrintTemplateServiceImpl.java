package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.mapper.PrintTemplateMapper;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.mapper.PsiWarehouseMapper;
import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.entity.PrintTemplateDO;
import com.ls.aegis.biz.psi.model.entity.UnitDO;
import com.ls.aegis.biz.psi.model.entity.WarehouseDO;
import com.ls.aegis.biz.psi.model.query.PrintTemplateQuery;
import com.ls.aegis.biz.psi.model.req.PrintTemplateReq;
import com.ls.aegis.biz.psi.model.resp.PrintRenderResp;
import com.ls.aegis.biz.psi.model.resp.PrintTemplateDetailResp;
import com.ls.aegis.biz.psi.model.resp.PrintTemplateResp;
import com.ls.aegis.biz.psi.model.resp.StockLineResp;
import com.ls.aegis.biz.psi.model.resp.StockInResp;
import com.ls.aegis.biz.psi.model.resp.StockOutResp;
import com.ls.aegis.biz.psi.service.PrintTemplateService;
import com.ls.aegis.biz.psi.service.StockInService;
import com.ls.aegis.biz.psi.service.StockOutService;
import com.ls.aegis.biz.psi.support.DocxPlaceholderFiller;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.mybatis.base.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 打印模板服务实现
 */
@Service
@RequiredArgsConstructor
public class PrintTemplateServiceImpl extends BaseServiceImpl<PrintTemplateMapper, PrintTemplateDO, PrintTemplateResp, PrintTemplateDetailResp, PrintTemplateQuery, PrintTemplateReq> implements PrintTemplateService {

    private static final String BIZ_STOCK_OUT = "STOCK_OUT";
    private static final String BIZ_STOCK_IN = "STOCK_IN";
    private static final int DEFAULT_YES = 1;
    private static final int DEFAULT_NO = 0;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String DEFAULT_STOCK_OUT_HTML = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
        <meta charset="UTF-8"/>
        <title>出库单打印</title>
        <style>
          body{font-family:SimSun,serif;font-size:12px;color:#000;margin:16px}
          h1{text-align:center;font-size:18px;margin:0 0 12px}
          .meta{margin-bottom:12px;line-height:1.8}
          .meta span{display:inline-block;min-width:46%;margin-right:2%}
          table{width:100%;border-collapse:collapse}
          th,td{border:1px solid #333;padding:4px 6px;text-align:center}
          th{background:#f5f5f5}
          .remark{margin-top:12px}
          @media print{body{margin:0}}
        </style>
        </head>
        <body>
        <h1>出库单</h1>
        <div class="meta">
          <span>单号：{{docNo}}</span>
          <span>出库日期：{{outDate}}</span>
          <span>仓库：{{warehouseName}}</span>
          <span>往来单位：{{unitName}}</span>
          <span>合计金额：{{totalAmount}}</span>
        </div>
        <table>
          <thead>
            <tr><th>品名</th><th>数量</th><th>含税单价</th><th>税率</th><th>含税金额</th></tr>
          </thead>
          <tbody>
            {{itemsHtml}}
          </tbody>
        </table>
        <div class="remark">备注：{{remark}}</div>
        </body>
        </html>
        """;

    private static final String DEFAULT_STOCK_IN_HTML = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
        <meta charset="UTF-8"/>
        <title>入库单打印</title>
        <style>
          body{font-family:SimSun,serif;font-size:12px;color:#000;margin:16px}
          h1{text-align:center;font-size:18px;margin:0 0 12px}
          .meta{margin-bottom:12px;line-height:1.8}
          .meta span{display:inline-block;min-width:46%;margin-right:2%}
          table{width:100%;border-collapse:collapse}
          th,td{border:1px solid #333;padding:4px 6px;text-align:center}
          th{background:#f5f5f5}
          .remark{margin-top:12px}
          @media print{body{margin:0}}
        </style>
        </head>
        <body>
        <h1>入库单</h1>
        <div class="meta">
          <span>单号：{{docNo}}</span>
          <span>入库日期：{{inDate}}</span>
          <span>仓库：{{warehouseName}}</span>
          <span>往来单位：{{unitName}}</span>
          <span>合计金额：{{totalAmount}}</span>
        </div>
        <table>
          <thead>
            <tr><th>品名</th><th>数量</th><th>含税单价</th><th>税率</th><th>含税金额</th></tr>
          </thead>
          <tbody>
            {{itemsHtml}}
          </tbody>
        </table>
        <div class="remark">备注：{{remark}}</div>
        </body>
        </html>
        """;

    private final StockOutService stockOutService;
    private final StockInService stockInService;
    private final PsiWarehouseMapper warehouseMapper;
    private final UnitMapper unitMapper;
    private final GoodsMapper goodsMapper;
    private final JdbcTemplate jdbcTemplate;
    private final com.ls.aegis.biz.psi.support.PsiAttachmentAccessGuard attachments;

    @Override
    public void beforeCreate(PrintTemplateReq req) {
        attachments.require(req.getFileId());
        req.setTenantId(TenantFillHelper.currentTenantId());
        req.setDeptId(TenantFillHelper.currentDeptId());
        req.setIsDefault(normalizeDefault(req.getIsDefault()));
    }

    @Override
    public void beforeUpdate(PrintTemplateReq req, Long id) {
        attachments.require(req.getFileId());
        req.setTenantId(null);
        req.setDeptId(null);
        if (req.getIsDefault() != null) {
            req.setIsDefault(normalizeDefault(req.getIsDefault()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void afterCreate(PrintTemplateReq req, PrintTemplateDO entity) {
        clearOtherDefault(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void afterUpdate(PrintTemplateReq req, PrintTemplateDO entity) {
        clearOtherDefault(entity);
    }

    @Override
    public PrintRenderResp render(String bizType, Long bizId, Long templateId) {
        if (StrUtil.isBlank(bizType)) {
            throw new BusinessException("业务类型不能为空");
        }
        if (bizId == null) {
            throw new BusinessException("业务单据ID不能为空");
        }
        PrintTemplateDO template = resolveTemplate(bizType, templateId);
        if (BIZ_STOCK_OUT.equals(bizType)) {
            return renderStockOut(template, bizId);
        }
        if (BIZ_STOCK_IN.equals(bizType)) {
            return renderStockIn(template, bizId);
        }
        throw new BusinessException("暂不支持该业务类型打印");
    }

    @Override
    public byte[] renderDocx(String bizType, Long bizId, Long templateId) {
        if (StrUtil.isBlank(bizType)) {
            throw new BusinessException("业务类型不能为空");
        }
        if (bizId == null) {
            throw new BusinessException("业务单据ID不能为空");
        }
        PrintTemplateDO template = resolveTemplate(bizType, templateId);
        Map<String, String> placeholders = buildDocxPlaceholders(bizType, bizId);
        if (template.getFileId() != null) {
            byte[] templateBytes = loadLocalFileBytes(template.getFileId());
            return DocxPlaceholderFiller.fill(templateBytes, placeholders);
        }
        return DocxPlaceholderFiller.fillFromBuiltin(placeholders);
    }

    private Map<String, String> buildDocxPlaceholders(String bizType, Long bizId) {
        Map<String, String> map = new LinkedHashMap<>();
        if (BIZ_STOCK_OUT.equals(bizType)) {
            StockOutResp doc = stockOutService.get(bizId);
            if (doc == null) {
                throw new BusinessException("出库单不存在");
            }
            map.put("docNo", nullToEmpty(doc.getDocNo()));
            map.put("outDate", formatDateTime(doc.getOutDate()));
            map.put("inDate", formatDateTime(doc.getOutDate()));
            map.put("warehouseName", resolveWarehouseName(doc.getWarehouseId()));
            map.put("unitName", resolveUnitName(doc.getUnitId()));
            map.put("totalAmount", formatDecimal(doc.getTaxInclusiveAmount()));
            map.put("remark", nullToEmpty(doc.getRemark()));
            map.put("itemsText", buildItemsPlainText(doc.getItems()));
            return map;
        }
        if (BIZ_STOCK_IN.equals(bizType)) {
            StockInResp doc = stockInService.get(bizId);
            if (doc == null) {
                throw new BusinessException("入库单不存在");
            }
            map.put("docNo", nullToEmpty(doc.getDocNo()));
            map.put("inDate", formatDateTime(doc.getInDate()));
            map.put("outDate", formatDateTime(doc.getInDate()));
            map.put("warehouseName", resolveWarehouseName(doc.getWarehouseId()));
            map.put("unitName", resolveUnitName(doc.getUnitId()));
            map.put("totalAmount", formatDecimal(doc.getTaxInclusiveAmount()));
            map.put("remark", nullToEmpty(doc.getRemark()));
            map.put("itemsText", buildItemsPlainText(doc.getItems()));
            return map;
        }
        throw new BusinessException("暂不支持该业务类型 Word 套打");
    }

    private String buildItemsPlainText(List<StockLineResp> items) {
        if (CollUtil.isEmpty(items)) {
            return "";
        }
        Map<Long, String> goodsNameMap = loadGoodsNames(items);
        List<String> lines = new ArrayList<>();
        for (StockLineResp item : items) {
            String name = goodsNameMap.getOrDefault(item.getGoodsId(), "");
            lines.add(name + " qty=" + formatDecimal(item.getQuantity()) + " amt=" + formatDecimal(item
                .getTaxInclusiveAmount()));
        }
        return String.join("; ", lines);
    }

    private byte[] loadLocalFileBytes(Long fileId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
            SELECT f.path AS file_path, f.parent_path AS parent_path, s.bucket_name AS bucket_name
            FROM sys_file f
            INNER JOIN sys_storage s ON s.id = f.storage_id AND s.deleted = 0
            WHERE f.id = ? AND f.deleted = 0
            """, fileId);
        if (CollUtil.isEmpty(rows)) {
            throw new BusinessException("Word 模板文件不存在：" + fileId);
        }
        Map<String, Object> row = rows.get(0);
        String bucket = String.valueOf(row.get("bucket_name"));
        String filePath = String.valueOf(row.get("file_path"));
        Path path = Path.of(bucket, filePath).normalize();
        if (!Files.isRegularFile(path)) {
            Path alt = Path.of("/app", bucket, filePath).normalize();
            if (Files.isRegularFile(alt)) {
                path = alt;
            }
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new BusinessException("读取 Word 模板失败：" + path + "，" + ex.getMessage());
        }
    }

    private PrintRenderResp renderStockOut(PrintTemplateDO template, Long bizId) {
        StockOutResp doc = stockOutService.get(bizId);
        if (doc == null) {
            throw new BusinessException("出库单不存在");
        }
        String warehouseName = resolveWarehouseName(doc.getWarehouseId());
        String unitName = resolveUnitName(doc.getUnitId());
        Map<Long, String> goodsNameMap = loadGoodsNames(doc.getItems());
        String itemsHtml = buildItemsHtml(doc.getItems(), goodsNameMap);
        String content = StrUtil.isNotBlank(template.getContentHtml()) ? template.getContentHtml() : DEFAULT_STOCK_OUT_HTML;
        String html = applyCommonPlaceholders(content, nullToEmpty(doc.getDocNo()), formatDateTime(doc.getOutDate()), warehouseName, unitName, nullToEmpty(doc.getRemark()), formatDecimal(doc.getTaxInclusiveAmount()), itemsHtml);
        String title = StrUtil.blankToDefault(template.getTemplateName(), "出库单") + " - " + nullToEmpty(doc.getDocNo());
        return buildRenderResp(title, html, template.getFileId());
    }

    private PrintRenderResp renderStockIn(PrintTemplateDO template, Long bizId) {
        StockInResp doc = stockInService.get(bizId);
        if (doc == null) {
            throw new BusinessException("入库单不存在");
        }
        String warehouseName = resolveWarehouseName(doc.getWarehouseId());
        String unitName = resolveUnitName(doc.getUnitId());
        Map<Long, String> goodsNameMap = loadGoodsNames(doc.getItems());
        String itemsHtml = buildItemsHtml(doc.getItems(), goodsNameMap);
        String inDate = formatDateTime(doc.getInDate());
        String content = StrUtil.isNotBlank(template.getContentHtml()) ? template.getContentHtml() : DEFAULT_STOCK_IN_HTML;
        String html = applyCommonPlaceholders(content, nullToEmpty(doc.getDocNo()), inDate, warehouseName, unitName, nullToEmpty(doc.getRemark()), formatDecimal(doc.getTaxInclusiveAmount()), itemsHtml);
        String title = StrUtil.blankToDefault(template.getTemplateName(), "入库单") + " - " + nullToEmpty(doc.getDocNo());
        return buildRenderResp(title, html, template.getFileId());
    }

    private PrintRenderResp buildRenderResp(String title, String html, Long fileId) {
        PrintRenderResp resp = new PrintRenderResp();
        resp.setTitle(title);
        resp.setHtml(html);
        resp.setFileId(fileId);
        return resp;
    }

    private String applyCommonPlaceholders(String content, String docNo, String docDate, String warehouseName, String unitName, String remark, String totalAmount, String itemsHtml) {
        return content
            .replace("{{docNo}}", docNo)
            .replace("{{inDate}}", docDate)
            .replace("{{outDate}}", docDate)
            .replace("{{warehouseName}}", warehouseName)
            .replace("{{unitName}}", unitName)
            .replace("{{remark}}", remark)
            .replace("{{totalAmount}}", totalAmount)
            .replace("{{itemsHtml}}", itemsHtml);
    }

    private String resolveWarehouseName(Long warehouseId) {
        if (warehouseId == null) {
            return "";
        }
        WarehouseDO warehouse = warehouseMapper.selectById(warehouseId);
        return warehouse == null ? "" : StrUtil.blankToDefault(warehouse.getWarehouseName(), "");
    }

    private String resolveUnitName(Long unitId) {
        if (unitId == null) {
            return "";
        }
        UnitDO unit = unitMapper.selectById(unitId);
        return unit == null ? "" : StrUtil.blankToDefault(unit.getUnitName(), "");
    }

    private PrintTemplateDO resolveTemplate(String bizType, Long templateId) {
        Long tenantId = TenantFillHelper.currentTenantId();
        if (templateId != null) {
            PrintTemplateDO template = baseMapper.selectById(templateId);
            if (template == null || !Objects.equals(template.getTenantId(), tenantId) || !bizType.equals(template
                .getBizType())) {
                throw new BusinessException("打印模板不存在或不匹配业务类型");
            }
            return template;
        }
        List<PrintTemplateDO> enabled = baseMapper.selectList(Wrappers.lambdaQuery(PrintTemplateDO.class)
            .eq(PrintTemplateDO::getTenantId, tenantId)
            .eq(PrintTemplateDO::getBizType, bizType)
            .eq(PrintTemplateDO::getStatus, DisEnableStatusEnum.ENABLE)
            .orderByDesc(PrintTemplateDO::getIsDefault)
            .orderByDesc(PrintTemplateDO::getId));
        if (CollUtil.isEmpty(enabled)) {
            // 无库内模板时，对入/出库走内置 HTML / 内置 docx
            if (BIZ_STOCK_IN.equals(bizType) || BIZ_STOCK_OUT.equals(bizType)) {
                PrintTemplateDO fallback = new PrintTemplateDO();
                fallback.setBizType(bizType);
                fallback.setTenantId(tenantId);
                fallback.setTemplateName(BIZ_STOCK_IN.equals(bizType) ? "入库单内置模板" : "出库单内置模板");
                fallback.setIsDefault(DEFAULT_YES);
                return fallback;
            }
            throw new BusinessException("未找到可用的打印模板");
        }
        PrintTemplateDO defaultOne = enabled.stream()
            .filter(t -> ObjectUtil.equal(t.getIsDefault(), DEFAULT_YES))
            .findFirst()
            .orElse(null);
        if (defaultOne != null) {
            return defaultOne;
        }
        if (enabled.size() == 1) {
            return enabled.get(0);
        }
        throw new BusinessException("未设置默认打印模板，请指定 templateId");
    }

    private Map<Long, String> loadGoodsNames(List<StockLineResp> items) {
        Map<Long, String> map = new HashMap<>();
        if (CollUtil.isEmpty(items)) {
            return map;
        }
        List<Long> goodsIds = items.stream()
            .map(StockLineResp::getGoodsId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (CollUtil.isEmpty(goodsIds)) {
            return map;
        }
        List<GoodsDO> goodsList = goodsMapper.selectByIds(goodsIds);
        for (GoodsDO goods : goodsList) {
            map.put(goods.getId(), StrUtil.blankToDefault(goods.getGoodsName(), ""));
        }
        return map;
    }

    private String buildItemsHtml(List<StockLineResp> items, Map<Long, String> goodsNameMap) {
        if (CollUtil.isEmpty(items)) {
            return "";
        }
        List<String> rows = new ArrayList<>(items.size());
        for (StockLineResp item : items) {
            String name = goodsNameMap.getOrDefault(item.getGoodsId(), "");
            rows.add("<tr><td>" + name + "</td><td>" + formatDecimal(item.getQuantity()) + "</td><td>" + formatDecimal(item
                .getTaxInclusivePrice()) + "</td><td>" + formatDecimal(item.getTaxRate()) + "</td><td>" + formatDecimal(item
                    .getTaxInclusiveAmount()) + "</td></tr>");
        }
        return String.join("", rows);
    }

    private void clearOtherDefault(PrintTemplateDO entity) {
        if (!ObjectUtil.equal(entity.getIsDefault(), DEFAULT_YES)) {
            return;
        }
        baseMapper.update(null, Wrappers.lambdaUpdate(PrintTemplateDO.class)
            .set(PrintTemplateDO::getIsDefault, DEFAULT_NO)
            .eq(PrintTemplateDO::getTenantId, entity.getTenantId())
            .eq(PrintTemplateDO::getBizType, entity.getBizType())
            .ne(PrintTemplateDO::getId, entity.getId())
            .eq(PrintTemplateDO::getIsDefault, DEFAULT_YES));
    }

    private int normalizeDefault(Integer isDefault) {
        return ObjectUtil.equal(isDefault, DEFAULT_YES) ? DEFAULT_YES : DEFAULT_NO;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FMT.format(value);
    }

    private static String formatDecimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
