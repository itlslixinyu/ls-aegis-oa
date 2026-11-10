package com.ls.aegis.biz.quote.support;

import com.ls.aegis.biz.quote.constant.QuoteConstants;
import com.ls.aegis.biz.quote.domain.Quote;
import com.ls.aegis.biz.quote.domain.QuoteDetail;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 报价单导出 HTML 渲染（与前端表单、Word/PDF 导出共用版式）。
 */
public final class QuoteDocumentRenderer {

    private static final int MATERIAL_ROW_MIN = 10;
    private static final int SERVICE_ROW_MIN = 1;

    private QuoteDocumentRenderer() {
    }

    public static String renderHtml(Quote quote, String logoUrl) {
        List<QuoteDetail> materials = filterLines(quote.getDetails(), QuoteConstants.LINE_TYPE_MATERIAL);
        List<QuoteDetail> services = filterLines(quote.getDetails(), QuoteConstants.LINE_TYPE_SERVICE);
        padRows(materials, MATERIAL_ROW_MIN, QuoteConstants.LINE_TYPE_MATERIAL);
        padRows(services, SERVICE_ROW_MIN, QuoteConstants.LINE_TYPE_SERVICE);

        String quoteNo = esc(nullToEmpty(quote.getQuoteNo()));
        String quoteUnit = esc(nullToEmpty(quote.getQuoteUnit()));
        String quoteDate = esc(formatQuoteDate(quote));
        String projectName = esc(nullToEmpty(quote.getProjectName()).trim());
        String docSubtitle = "设备采购及技术服务报价单";
        String logoSrc = escAttr(QuoteLogoHelper.embeddableLogoSrc(logoUrl));
        String logoHtml = StringUtils.hasText(logoSrc)
            ? "<img src=\"" + logoSrc + "\" alt=\"logo\" width=\"80\" height=\"80\" style=\"width:80px;height:80px;object-fit:contain;\"/>"
            : "";

        BigDecimal materialSub = nvl(quote.getMaterialSubtotal());
        BigDecimal serviceSub = nvl(quote.getServiceSubtotal());
        BigDecimal preTax = QuoteAmountCalculator.preTaxTotal(quote);
        BigDecimal tax = QuoteAmountCalculator.taxAmount(quote);
        BigDecimal total = nvl(quote.getTotalAmount());
        String rate = quote.getTaxRate() == null ? "4" : quote.getTaxRate().stripTrailingZeros().toPlainString();
        String upper = esc(QuoteAmountCalculator.amountUpper(quote));
        String terms = esc(nullToEmpty(quote.getTerms())).replace("\n", "<br/>");

        StringBuilder sb = new StringBuilder(8192);
        sb.append("""
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml" lang="zh-CN">
            <head>
            <meta charset="UTF-8"/>
            <title>报价单</title>
            <style>
              @page { size: A4; margin: 12mm; }
              html { background: #e8e8e8; }
              body {
                font-family: SimSun, "Microsoft YaHei", "Songti SC", serif;
                font-size: 12px;
                color: #000;
                margin: 0 auto;
                box-sizing: border-box;
                background: #fff;
              }
              @media screen {
                body {
                  width: 210mm;
                  min-height: 297mm;
                  margin: 12px auto;
                  padding: 12mm;
                  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.12);
                }
              }
              @media print {
                html { background: #fff; }
                body { width: auto; min-height: auto; margin: 0; padding: 0; box-shadow: none; }
              }
              .header { position: relative; margin-bottom: 10px; min-height: 80px; }
              .logo { position: absolute; left: 0; top: 0; width: 80px; }
              .logo img { display: block; width: 80px; height: 80px; object-fit: contain; }
              .title-wrap { text-align: center; padding: 0 80px; }
              .title-wrap h1 { margin: 0; font-size: 18px; font-weight: 700; line-height: 1.5; }
              .title-wrap h2 { margin: 4px 0 0; font-size: 14px; font-weight: 400; line-height: 1.5; }
              .meta { text-align: right; margin-top: 8px; font-size: 12px; line-height: 1.7; }
              .section { margin-top: 8px; font-weight: 700; font-size: 12px; }
              table.data-table { width: 100%; border-collapse: collapse; margin-top: 3px; table-layout: fixed; border-spacing: 0; }
              table.data-table th, table.data-table td {
                border: 1px solid #333;
                padding: 5px 4px;
                text-align: center !important;
                vertical-align: middle !important;
                word-break: break-word;
              }
              table.data-table th {
                background: #f5f5f5;
                font-weight: 700;
                padding: 7px 4px;
                white-space: nowrap;
                line-height: 1.35;
              }
              table.data-table td { height: 22px; line-height: 1.3; }
              table.data-table col.col-idx { width: 7%; }
              table.data-table col.col-name { width: 28%; }
              table.data-table col.col-spec { width: 18%; }
              table.data-table col.col-unit { width: 8%; }
              table.data-table col.col-qty { width: 11%; }
              table.data-table col.col-price { width: 14%; }
              table.data-table col.col-amount { width: 14%; }
              table.summary { width: 100%; border-collapse: collapse; margin-top: 3px; table-layout: fixed; border-spacing: 0; }
              table.summary td {
                height: 26px;
                border: 1px solid #333;
                padding: 4px 3px;
                text-align: center !important;
                vertical-align: middle !important;
              }
              .summary-note { margin-top: 3px; font-size: 11px; line-height: 1.5; }
              .terms { margin-top: 8px; line-height: 1.7; font-size: 11px; }
              .terms p { margin: 0 0 3px; }
              @media print { body { margin: 0; } }
            </style>
            </head>
            <body>
            """);
        sb.append("<div class=\"header\">");
        sb.append("<div class=\"logo\">").append(logoHtml).append("</div>");
        sb.append("<div class=\"title-wrap\"><h1>").append(projectName).append("</h1><h2>")
            .append(docSubtitle).append("</h2></div>");
        sb.append("</div>");
        sb.append("<div class=\"meta\">");
        sb.append("报价单位：").append(quoteUnit).append("<br/>");
        sb.append("报价单号：").append(quoteNo).append("<br/>");
        sb.append("报价时间：").append(quoteDate);
        sb.append("</div>");

        sb.append("<div class=\"section\">一、材料部分</div>");
        sb.append("<table class=\"data-table\">");
        sb.append("<colgroup>");
        sb.append("<col class=\"col-idx\"/><col class=\"col-name\"/><col class=\"col-spec\"/>");
        sb.append("<col class=\"col-unit\"/><col class=\"col-qty\"/><col class=\"col-price\"/><col class=\"col-amount\"/>");
        sb.append("</colgroup>");
        sb.append("<thead><tr>");
        sb.append("<th>序号</th><th>材料名称/规格</th><th>品牌/型号</th>");
        sb.append("<th>单位</th><th>数量</th><th>单价(¥)</th><th>金额(¥)</th>");
        sb.append("</tr></thead><tbody>");
        for (int i = 0; i < materials.size(); i++) {
            QuoteDetail row = materials.get(i);
            sb.append("<tr>");
            sb.append("<td>").append(i + 1).append("</td>");
            sb.append("<td>").append(cell(esc(nullToEmpty(row.getItemName())))).append("</td>");
            sb.append("<td>").append(cell(esc(nullToEmpty(row.getBrandModel())))).append("</td>");
            sb.append("<td>").append(cell(esc(nullToEmpty(row.getUnit())))).append("</td>");
            sb.append("<td>").append(cell(formatQty(row.getQuantity()))).append("</td>");
            sb.append("<td>").append(cell(formatMoney(row.getPrice()))).append("</td>");
            sb.append("<td>").append(cell(formatMoney(row.getAmount()))).append("</td>");
            sb.append("</tr>");
        }
        sb.append("<tr><td colspan=\"6\">材料小计</td><td>")
            .append(formatMoney(materialSub)).append("</td></tr>");
        sb.append("</tbody></table>");

        sb.append("<div class=\"section\">二、技术服务费部分</div>");
        sb.append("<table class=\"data-table\">");
        sb.append("<colgroup>");
        sb.append("<col class=\"col-idx\"/><col class=\"col-name\"/><col class=\"col-spec\"/>");
        sb.append("<col class=\"col-unit\"/><col class=\"col-qty\"/><col class=\"col-price\"/><col class=\"col-amount\"/>");
        sb.append("</colgroup>");
        sb.append("<thead><tr>");
        sb.append("<th>序号</th><th>服务项目</th><th>说明</th>");
        sb.append("<th>单位</th><th>工作量</th><th>单价(¥)</th><th>金额(¥)</th>");
        sb.append("</tr></thead><tbody>");
        for (int i = 0; i < services.size(); i++) {
            QuoteDetail row = services.get(i);
            sb.append("<tr>");
            sb.append("<td>").append(i + 1).append("</td>");
            sb.append("<td>").append(cell(esc(nullToEmpty(row.getItemName())))).append("</td>");
            sb.append("<td>").append(cell(esc(nullToEmpty(row.getDescription())))).append("</td>");
            sb.append("<td>").append(cell(esc(nullToEmpty(row.getUnit())))).append("</td>");
            sb.append("<td>").append(cell(formatQty(row.getQuantity()))).append("</td>");
            sb.append("<td>").append(cell(formatMoney(row.getPrice()))).append("</td>");
            sb.append("<td>").append(cell(formatMoney(row.getAmount()))).append("</td>");
            sb.append("</tr>");
        }
        sb.append("<tr><td colspan=\"6\">服务小计</td><td>")
            .append(formatMoney(serviceSub)).append("</td></tr>");
        sb.append("</tbody></table>");

        sb.append("<div class=\"section\">三、费用合计</div>");
        sb.append("<table class=\"summary\"><tbody>");
        sb.append("<tr><td>材料小计</td><td>").append(formatMoney(materialSub))
            .append("</td><td>税前合计</td><td>").append(formatMoney(preTax))
            .append("</td><td>总价(含税)</td><td>").append(formatMoney(total)).append("</td></tr>");
        sb.append("<tr><td>技术服务费小计</td><td>").append(formatMoney(serviceSub))
            .append("</td><td>增值税综合税率*").append(rate).append("%</td><td>")
            .append(formatMoney(tax)).append("</td><td>人民币大写</td><td>").append(upper).append("</td></tr>");
        sb.append("</tbody></table>");
        sb.append("<div class=\"summary-note\">").append(summaryNote(rate)).append("</div>");

        sb.append("<div class=\"section\">四、备注 / 条款</div>");
        sb.append("<div class=\"terms\">").append(StringUtils.hasText(terms) ? terms : defaultTerms()).append("</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static List<QuoteDetail> filterLines(List<QuoteDetail> details, String lineType) {
        if (details == null) {
            return new ArrayList<>();
        }
        return details.stream()
            .filter(d -> d != null && lineType.equals(d.getLineType()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static void padRows(List<QuoteDetail> rows, int min, String lineType) {
        while (rows.size() < min) {
            QuoteDetail blank = new QuoteDetail();
            blank.setLineType(lineType);
            rows.add(blank);
        }
    }

    private static String cell(String content) {
        // openhtmltopdf 按 XHTML 解析，未声明实体时 &nbsp; 会报错，改用数字实体
        return StringUtils.hasText(content) ? content : "&#160;";
    }

    private static String summaryNote(String rate) {
        return "本报价为含税总价。增值税综合税率" + rate
            + "%(含增值税及附加等);开票增值税税率以实际票面为准(小规模纳税人常见为1%)，无需另行支付税款。";
    }

    private static String defaultTerms() {
        return """
            <p>1. 本报价单自出具之日起 7 个自然日内有效，有效期届满本报价自动失效，如需沿用本报价，需经供需双方重新书面确认。</p>
            <p>2. 报价为含税价，包含本市范围内运费，无其他额外费用。</p>
            <p>3. 付款方式：合同签订后预付 30%，设备到货后支付 50%，验收合格后支付余款 20%。</p>
            """;
    }

    private static String formatQuoteDate(Quote quote) {
        if (quote.getQuoteDate() != null) {
            return formatDate(quote.getQuoteDate());
        }
        LocalDateTime createTime = quote.getCreateTime();
        if (createTime != null) {
            return createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return "";
    }

    private static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private static String formatMoney(BigDecimal val) {
        if (val == null) {
            return "";
        }
        return val.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatQty(BigDecimal val) {
        if (val == null) {
            return "";
        }
        return val.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal nvl(BigDecimal val) {
        return val == null ? BigDecimal.ZERO.setScale(2) : val.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String esc(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static String escAttr(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replace("&", "&amp;").replace("\"", "&quot;");
    }
}
