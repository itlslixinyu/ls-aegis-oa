package com.ls.aegis.biz.psi.support;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ls.aegis.biz.psi.mapper.GoodsMapper;
import com.ls.aegis.biz.psi.model.resp.ImportGoodsMatch;
import com.ls.aegis.biz.psi.model.req.StockLineImportRowReq;
import com.ls.aegis.biz.psi.model.resp.StockLineImportParseResp;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.validation.CheckUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * 出入库明细 Excel 导入（模板下载 / 解析填表）
 */
public final class StockLineImportSupport {

    private static final int GOODS_BATCH_SIZE = 200;

    private StockLineImportSupport() {
    }

    public static void downloadTemplate(HttpServletResponse response) {
        PsiImportHelper.downloadTemplate(response, StockLineImportRowReq.class, "出入库明细导入模板.xlsx");
    }

    public static StockLineImportParseResp parse(MultipartFile file, GoodsMapper goodsMapper, Long tenantId) {
        CheckUtils.throwIf(file == null || file.isEmpty(), "文件不能为空");
        List<StockLineImportRowReq> rows;
        try (var input = file.getInputStream()) {
            rows = PsiImportHelper.readExcel(input, StockLineImportRowReq.class);
        } catch (Exception e) {
            throw new BusinessException("数据文件解析异常，请检查模板格式");
        }
        // 分批只读取有效行的编号，避免逐行网络往返或单条SQL无限增长。
        List<String> codes = rows.stream()
            .filter(row -> StrUtil.isNotBlank(row.getGoodsCode()))
            .map(row -> row.getGoodsCode().trim()).distinct().toList();
        Map<String, ImportGoodsMatch> goodsByCode = new HashMap<>();
        for (int from = 0; from < codes.size(); from += GOODS_BATCH_SIZE) {
            for (ImportGoodsMatch match : goodsMapper.selectImportGoods(tenantId,
                    codes.subList(from, Math.min(from + GOODS_BATCH_SIZE, codes.size())))) {
                goodsByCode.putIfAbsent(match.getImportCode(), match);
            }
        }
        StockLineImportParseResp resp = new StockLineImportParseResp();
        resp.setTotalRows(rows.size());
        List<StockLineImportParseResp.Item> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (StockLineImportRowReq row : rows) {
            if (StrUtil.isBlank(row.getGoodsCode()) && StrUtil.isBlank(row.getQuantity())) {
                continue;
            }
            CheckUtils.throwIfBlank(row.getGoodsCode(), "商品编号不能为空");
            CheckUtils.throwIfBlank(row.getQuantity(), "数量不能为空：" + row.getGoodsCode());
            String code = row.getGoodsCode().trim();
            if (!seen.add(code)) {
                throw new BusinessException("商品编号重复：" + code);
            }
            ImportGoodsMatch goods = goodsByCode.get(code);
            if (goods == null) {
                throw new BusinessException("商品编号不存在：" + code);
            }
            StockLineImportParseResp.Item item = new StockLineImportParseResp.Item();
            item.setGoodsId(goods.getId());
            item.setGoodsCode(goods.getGoodsCode());
            item.setGoodsName(goods.getGoodsName());
            item.setQuantity(parsePositive(row.getQuantity(), "数量"));
            if (StrUtil.isNotBlank(row.getTaxInclusivePrice())) {
                item.setTaxInclusivePrice(parseNonNegative(row.getTaxInclusivePrice(), "含税单价"));
            } else {
                item.setTaxInclusivePrice(BigDecimal.ZERO);
            }
            if (StrUtil.isNotBlank(row.getTaxRate())) {
                item.setTaxRate(parseNonNegative(row.getTaxRate(), "税率"));
            } else if (goods.getDefaultTaxRate() != null) {
                item.setTaxRate(goods.getDefaultTaxRate());
            } else {
                item.setTaxRate(new BigDecimal("13"));
            }
            items.add(item);
        }
        CheckUtils.throwIfEmpty(items, "未解析到有效明细（商品编号、数量必填）");
        resp.setValidRows(items.size());
        resp.setItems(items);
        return resp;
    }

    private static BigDecimal parsePositive(String raw, String label) {
        BigDecimal n = parseNonNegative(raw, label);
        if (n.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(label + "必须大于 0：" + raw);
        }
        return n;
    }

    private static BigDecimal parseNonNegative(String raw, String label) {
        String v = raw.trim().replace(",", "");
        if (!NumberUtil.isNumber(v)) {
            throw new BusinessException(label + "格式不正确：" + raw);
        }
        BigDecimal n = new BigDecimal(v);
        if (n.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(label + "不能为负：" + raw);
        }
        return n;
    }
}
