package com.ls.aegis.biz.psi.support;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.enums.InvoiceTypeEnum;
import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.model.entity.UnitDO;
import com.ls.aegis.biz.psi.model.resp.InvoiceImportParseResp;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.validation.CheckUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 税务发票文件解析（XML / PDF / OFD → 表单回填；可手改）
 */
public final class InvoiceImportSupport {

    private static final Pattern INVOICE_NO_PATTERN = Pattern.compile(
        "(?:发票号码|数电票号码|电子发票号码|发票号)[：:\\s]*([0-9][0-9\\s\\-]{6,40}[0-9])");
    private static final Pattern INVOICE_NO_TAG_PATTERN = Pattern.compile(
        "(?:InvoiceNumber|EIid|EIID|fphm|FPHM)[\"'\\s:=<>/]*([0-9]{8,30})", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVOICE_NO_NEAR_PATTERN = Pattern.compile(
        "(?:发票号码|数电票号码|电子发票号码|发票号).{0,48}?([0-9](?:[\\s\\-]*[0-9]){7,29})", Pattern.DOTALL);
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(?:开票日期|开具日期)[：:\\s]*([0-9]{4}[-年./][0-9]{1,2}[-月./][0-9]{1,2})");
    private static final Pattern BUYER_TAX_PATTERN = Pattern.compile(
        "(?:购买方|购方)[^\\n]{0,40}(?:统一社会信用代码|纳税人识别号|税号)[：:\\s]*([0-9A-Z]{15,20})");
    private static final Pattern SELLER_TAX_PATTERN = Pattern.compile(
        "(?:销售方|销方)[^\\n]{0,40}(?:统一社会信用代码|纳税人识别号|税号)[：:\\s]*([0-9A-Z]{15,20})");
    private static final Pattern AMOUNT_INCLUSIVE_PATTERN = Pattern.compile(
        "(?:价税合计)[（(]?小写[）)]?[：:\\s]*[￥¥]?\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern AMOUNT_UNTAX_PATTERN = Pattern.compile(
        "(?:合计金额|金额合计)[：:\\s]*[￥¥]?\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern AMOUNT_TAX_PATTERN = Pattern.compile(
        "(?:合计税额|税额合计)[：:\\s]*[￥¥]?\\s*([0-9]+(?:\\.[0-9]+)?)");

    private InvoiceImportSupport() {
    }

    /**
     * @param expectedUnitType 进项=供应商；销项=客户；null 不校验类型
     */
    public static InvoiceImportParseResp parse(MultipartFile file,
                                               UnitMapper unitMapper,
                                               Long tenantId,
                                               UnitTypeEnum expectedUnitType) {
        CheckUtils.throwIf(file == null || file.isEmpty(), "文件不能为空");
        String filename = StrUtil.blankToDefault(file.getOriginalFilename(), "invoice");
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException("读取上传文件失败");
        }
        String kind = detectKind(filename, bytes);
        RawInvoice raw = switch (kind) {
            case "xml" -> parseXmlBytes(bytes);
            case "ofd" -> parseOfdBytes(bytes);
            case "pdf" -> parsePdfBytes(bytes);
            default -> throw new BusinessException("仅支持上传税务发票 XML / PDF / OFD 文件（当前无法识别文件类型：" + filename + "）");
        };
        return toResp(raw, unitMapper, tenantId, expectedUnitType);
    }

    /** 优先扩展名，其次文件头魔数（上传丢失文件名时仍可识别） */
    private static String detectKind(String filename, byte[] bytes) {
        String ext = extension(filename);
        if ("xml".equals(ext) || "pdf".equals(ext) || "ofd".equals(ext)) {
            return ext;
        }
        if (bytes == null || bytes.length < 5) {
            return ext;
        }
        // PDF
        if (bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F') {
            return "pdf";
        }
        // OFD / ZIP
        if (isZip(bytes)) {
            return "ofd";
        }
        // XML（含 BOM）
        String head = peekText(bytes.length > 256 ? java.util.Arrays.copyOf(bytes, 256) : bytes).trim();
        if (head.startsWith("<?xml") || head.startsWith("<EInvoice") || head.startsWith("<einvoice")
            || head.contains("<EInvoice") || head.contains("<TaxSupervisionInfo")) {
            return "xml";
        }
        return ext;
    }

    private static InvoiceImportParseResp toResp(RawInvoice raw,
                                                 UnitMapper unitMapper,
                                                 Long tenantId,
                                                 UnitTypeEnum expectedUnitType) {
        CheckUtils.throwIfBlank(raw.invoiceNo, "未能解析出发票号码，请检查文件或手工填写");
        InvoiceImportParseResp resp = new InvoiceImportParseResp();
        resp.setTotalRows(1);
        resp.setInvoiceNo(raw.invoiceNo.trim());
        resp.setInvoiceType(raw.invoiceType != null ? raw.invoiceType.getValue() : InvoiceTypeEnum.SPECIAL.getValue());
        if (StrUtil.isNotBlank(raw.invoiceDate)) {
            resp.setInvoiceDate(normalizeDate(raw.invoiceDate));
        }
        resp.setBuyerTaxNo(StrUtil.trim(raw.buyerTaxNo));
        resp.setSellerTaxNo(StrUtil.trim(raw.sellerTaxNo));
        resp.setRemark(StrUtil.trim(raw.remark));

        List<InvoiceImportParseResp.Item> items = new ArrayList<>();
        BigDecimal itemInclusiveSum = BigDecimal.ZERO;
        for (RawItem line : raw.items) {
            if (StrUtil.isBlank(line.goodsName) && line.taxInclusiveAmount == null && line.untaxAmount == null) {
                continue;
            }
            InvoiceImportParseResp.Item item = new InvoiceImportParseResp.Item();
            item.setGoodsName(StrUtil.blankToDefault(StrUtil.trim(line.goodsName), "未命名明细"));
            item.setQuantity(line.quantity != null ? line.quantity : BigDecimal.ONE);
            BigDecimal rate = normalizeTaxRate(line.taxRate);
            item.setTaxRate(rate);
            BigDecimal inclusive = line.taxInclusiveAmount;
            BigDecimal untax = line.untaxAmount;
            BigDecimal tax = line.taxAmount;
            if (inclusive == null && untax != null && tax != null) {
                inclusive = untax.add(tax).setScale(2, RoundingMode.HALF_UP);
            }
            if (inclusive == null && untax != null) {
                inclusive = untax.multiply(BigDecimal.ONE.add(rate.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)))
                    .setScale(2, RoundingMode.HALF_UP);
            }
            if (inclusive == null) {
                throw new BusinessException("明细金额无法解析：" + item.getGoodsName());
            }
            if (untax == null || tax == null) {
                untax = inclusive.divide(BigDecimal.ONE.add(rate.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)),
                    2, RoundingMode.HALF_UP);
                tax = inclusive.subtract(untax).setScale(2, RoundingMode.HALF_UP);
            } else {
                untax = untax.setScale(2, RoundingMode.HALF_UP);
                tax = tax.setScale(2, RoundingMode.HALF_UP);
            }
            item.setTaxInclusiveAmount(inclusive.setScale(2, RoundingMode.HALF_UP));
            item.setUntaxAmount(untax);
            item.setTaxAmount(tax);
            items.add(item);
            itemInclusiveSum = itemInclusiveSum.add(item.getTaxInclusiveAmount());
        }

        if (raw.taxInclusiveAmount != null) {
            resp.setTaxInclusiveAmount(raw.taxInclusiveAmount.setScale(2, RoundingMode.HALF_UP));
        } else if (!items.isEmpty()) {
            resp.setTaxInclusiveAmount(itemInclusiveSum);
        } else {
            throw new BusinessException("未能解析价税合计，请手工填写金额");
        }
        if (raw.untaxAmount != null && raw.taxAmount != null) {
            resp.setUntaxAmount(raw.untaxAmount.setScale(2, RoundingMode.HALF_UP));
            resp.setTaxAmount(raw.taxAmount.setScale(2, RoundingMode.HALF_UP));
        } else {
            BigDecimal rate = items.isEmpty() ? new BigDecimal("13") : items.get(0).getTaxRate();
            BigDecimal untax = resp.getTaxInclusiveAmount()
                .divide(BigDecimal.ONE.add(rate.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)), 2, RoundingMode.HALF_UP);
            resp.setUntaxAmount(untax);
            resp.setTaxAmount(resp.getTaxInclusiveAmount().subtract(untax).setScale(2, RoundingMode.HALF_UP));
        }
        resp.setItems(items);
        resp.setValidRows(Math.max(1, items.size()));

        // 进项匹配销方；销项匹配购方；匹配不到则留空，允许手选
        String matchTaxNo = UnitTypeEnum.SUPPLIER.equals(expectedUnitType)
            ? resp.getSellerTaxNo()
            : UnitTypeEnum.CUSTOMER.equals(expectedUnitType) ? resp.getBuyerTaxNo() : null;
        if (StrUtil.isBlank(matchTaxNo)) {
            matchTaxNo = StrUtil.blankToDefault(resp.getSellerTaxNo(), resp.getBuyerTaxNo());
        }
        fillUnitByCreditCode(resp, unitMapper, tenantId, expectedUnitType, matchTaxNo);
        return resp;
    }

    private static void fillUnitByCreditCode(InvoiceImportParseResp resp,
                                             UnitMapper unitMapper,
                                             Long tenantId,
                                             UnitTypeEnum expectedUnitType,
                                             String creditCode) {
        if (StrUtil.isBlank(creditCode)) {
            return;
        }
        UnitDO unit = unitMapper.selectOne(Wrappers.lambdaQuery(UnitDO.class)
            .eq(UnitDO::getTenantId, tenantId)
            .eq(UnitDO::getCreditCode, creditCode.trim())
            .last("LIMIT 1"));
        if (unit == null) {
            return;
        }
        if (expectedUnitType != null && unit.getUnitType() != null
            && !expectedUnitType.equals(unit.getUnitType())
            && !UnitTypeEnum.BOTH.equals(unit.getUnitType())) {
            return;
        }
        resp.setUnitId(unit.getId());
        resp.setUnitCode(unit.getUnitCode());
        resp.setUnitName(unit.getUnitName());
    }

    private static RawInvoice parseXmlBytes(byte[] bytes) {
        try {
            Document doc = InvoiceImportDomHelper.parseSecureXml(stripBom(bytes));
            RawInvoice raw = mapEInvoiceDom(doc);
            if (StrUtil.isBlank(raw.invoiceNo)) {
                // 再尝试整文正则（部分厂商自定义节点名）
                raw = mergePlain(raw, parseFromPlainText(peekText(bytes)));
            }
            if (StrUtil.isBlank(raw.invoiceNo)) {
                throw new BusinessException("XML 中未找到发票号码，请确认是税务局数电发票 XML");
            }
            return raw;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 编码失败时尝试按文本正则兜底
            RawInvoice raw = parseFromPlainText(peekText(bytes));
            if (StrUtil.isNotBlank(raw.invoiceNo)) {
                return raw;
            }
            throw new BusinessException("XML 发票解析失败：" + e.getMessage());
        }
    }

    private static RawInvoice mergePlain(RawInvoice base, RawInvoice plain) {
        if (base == null) {
            return plain;
        }
        if (StrUtil.isBlank(base.invoiceNo)) {
            base.invoiceNo = plain.invoiceNo;
        }
        if (StrUtil.isBlank(base.invoiceDate)) {
            base.invoiceDate = plain.invoiceDate;
        }
        if (StrUtil.isBlank(base.buyerTaxNo)) {
            base.buyerTaxNo = plain.buyerTaxNo;
        }
        if (StrUtil.isBlank(base.sellerTaxNo)) {
            base.sellerTaxNo = plain.sellerTaxNo;
        }
        if (base.taxInclusiveAmount == null) {
            base.taxInclusiveAmount = plain.taxInclusiveAmount;
        }
        if (base.untaxAmount == null) {
            base.untaxAmount = plain.untaxAmount;
        }
        if (base.taxAmount == null) {
            base.taxAmount = plain.taxAmount;
        }
        if (base.invoiceType == null) {
            base.invoiceType = plain.invoiceType;
        }
        return base;
    }

    private static byte[] stripBom(byte[] bytes) {
        if (bytes != null && bytes.length >= 3
            && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return java.util.Arrays.copyOfRange(bytes, 3, bytes.length);
        }
        return bytes;
    }

    private static RawInvoice parseOfdBytes(byte[] bytes) {
        if (!isZip(bytes)) {
            throw new BusinessException("OFD 文件格式不正确");
        }
        List<byte[]> xmlParts = extractZipXmlParts(bytes);
        for (byte[] part : xmlParts) {
            String text = peekText(part);
            if (text.contains("EInvoice") || text.contains("InvoiceNumber") || text.contains("TaxSupervisionInfo")) {
                try {
                    return parseXmlBytes(part);
                } catch (BusinessException ignored) {
                    // 继续尝试其它 XML 片段
                }
            }
        }
        // 回退：拼接 OFD 内文本再正则提取
        StringBuilder all = new StringBuilder();
        for (byte[] part : xmlParts) {
            all.append(peekText(part)).append('\n');
        }
        RawInvoice raw = parseFromPlainText(all.toString());
        if (StrUtil.isBlank(raw.invoiceNo)) {
            throw new BusinessException("OFD 中未解析到发票结构化数据，请改用税务局 XML 文件");
        }
        return raw;
    }

    private static RawInvoice parsePdfBytes(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            // 优先附件中的 XML（数电票 PDF 常内嵌）
            byte[] embeddedXml = extractEmbeddedXml(document);
            if (embeddedXml != null) {
                return parseXmlBytes(embeddedXml);
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            RawInvoice raw = parseFromPlainText(text);
            if (StrUtil.isBlank(raw.invoiceNo)) {
                throw new BusinessException("PDF 未能解析出发票号码，请上传税务局 XML / OFD，或手工填写");
            }
            return raw;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("PDF 发票解析失败：" + e.getMessage());
        }
    }

    private static byte[] extractEmbeddedXml(PDDocument document) throws Exception {
        PDDocumentNameDictionary names = document.getDocumentCatalog().getNames();
        if (names == null || names.getEmbeddedFiles() == null) {
            return null;
        }
        PDEmbeddedFilesNameTreeNode efTree = names.getEmbeddedFiles();
        Map<String, PDComplexFileSpecification> files = new LinkedHashMap<>();
        Map<String, PDComplexFileSpecification> rootNames = efTree.getNames();
        if (rootNames != null) {
            files.putAll(rootNames);
        }
        if (efTree.getKids() != null) {
            for (var kid : efTree.getKids()) {
                Map<String, PDComplexFileSpecification> kn = kid.getNames();
                if (kn != null) {
                    files.putAll(kn);
                }
            }
        }
        for (Map.Entry<String, PDComplexFileSpecification> e : files.entrySet()) {
            String name = StrUtil.blankToDefault(e.getKey(), "").toLowerCase(Locale.ROOT);
            PDComplexFileSpecification spec = e.getValue();
            if (spec == null) {
                continue;
            }
            PDEmbeddedFile ef = spec.getEmbeddedFile();
            if (ef == null) {
                continue;
            }
            byte[] data;
            try (InputStream in = ef.createInputStream()) {
                data = in.readAllBytes();
            }
            String peek = peekText(data);
            if (name.endsWith(".xml") || peek.contains("EInvoice") || peek.contains("InvoiceNumber")) {
                return data;
            }
        }
        return null;
    }

    private static RawInvoice mapEInvoiceDom(Document doc) {
        RawInvoice raw = new RawInvoice();
        // 数电票号码：InvoiceNumber；部分文件仅有 Header/EIid
        raw.invoiceNo = digitsOnly(InvoiceImportDomHelper.firstTextByLocalName(doc,
            "InvoiceNumber", "invoiceNumber", "EIid", "EIID", "fphm", "Fphm", "FP_HM", "InvoiceNo", "fp_hm"));
        if (StrUtil.isBlank(raw.invoiceNo)) {
            // 文本节点偶发带空格/换行
            raw.invoiceNo = digitsOnly(InvoiceImportDomHelper.firstTextByLocalName(doc, "InvoiceNo", "invoice_no", "发票号码"));
        }
        raw.invoiceDate = InvoiceImportDomHelper.firstTextByLocalName(doc,
            "IssueTime", "RequestTime", "kprq", "Kprq", "InvoiceDate", "Date", "fp_kprq");
        raw.buyerTaxNo = InvoiceImportDomHelper.firstTextByLocalName(doc,
            "BuyerIdNum", "BuyerTaxID", "gmf_nsrsbh", "GmfNsrsbh", "PurchaserTaxNo", "gmfnsrsbh");
        raw.sellerTaxNo = InvoiceImportDomHelper.firstTextByLocalName(doc,
            "SellerIdNum", "SellerTaxID", "xsf_nsrsbh", "XsfNsrsbh", "SellerTaxNo", "xsfnsrsbh");
        raw.untaxAmount = parseDecimal(InvoiceImportDomHelper.firstTextByLocalName(doc,
            "TotalAmWithoutTax", "TotalAmountWithoutTax", "hjje", "Hjje"));
        raw.taxAmount = parseDecimal(InvoiceImportDomHelper.firstTextByLocalName(doc,
            "TotalTaxAm", "TotalTaxAmount", "hjse", "Hjse"));
        raw.taxInclusiveAmount = parseDecimal(InvoiceImportDomHelper.firstTextByLocalName(doc,
            "TotalTax-includedAmount", "TotalTaxIncludedAmount", "jshj", "Jshj", "TotalAmount"));
        raw.remark = InvoiceImportDomHelper.firstTextByLocalName(doc, "Remark", "bz", "Bz");

        String typeLabel = InvoiceImportDomHelper.firstTextByLocalName(doc, "LabelName");
        String typeCode = InvoiceImportDomHelper.firstChildTextUnder(doc, "GeneralOrSpecialVAT", "LabelCode");
        raw.invoiceType = resolveInvoiceType(typeCode, typeLabel, InvoiceImportDomHelper.firstTextByLocalName(doc, "InvoiceType", "fplx", "Fplx"));

        List<Element> itemNodes = InvoiceImportDomHelper.elementsByLocalName(doc, "IssuItemInformation", "ItemInformation", "GoodInfo", "GoodsInfo");
        for (Element el : itemNodes) {
            RawItem item = new RawItem();
            item.goodsName = InvoiceImportDomHelper.childTextByLocalName(el, "ItemName", "itemName", "mc", "Mc", "GoodsName", "xmmc");
            item.quantity = parseDecimal(InvoiceImportDomHelper.childTextByLocalName(el, "Quantity", "quantity", "sl", "Sl"));
            item.untaxAmount = parseDecimal(InvoiceImportDomHelper.childTextByLocalName(el, "Amount", "amount", "je", "Je"));
            item.taxAmount = parseDecimal(InvoiceImportDomHelper.childTextByLocalName(el, "ComTaxAm", "TaxAmount", "se", "Se"));
            item.taxInclusiveAmount = parseDecimal(InvoiceImportDomHelper.childTextByLocalName(el,
                "TotaltaxIncludedAmount", "TotalTaxIncludedAmount", "TaxInclusiveAmount"));
            item.taxRate = parseDecimal(InvoiceImportDomHelper.childTextByLocalName(el, "TaxRate", "taxRate", "slv", "Slv"));
            raw.items.add(item);
        }
        return raw;
    }

    private static RawInvoice parseFromPlainText(String text) {
        RawInvoice raw = new RawInvoice();
        if (StrUtil.isBlank(text)) {
            return raw;
        }
        String normalized = text.replace('\u00A0', ' ');
        // PDF 常把汉字拆成「发 票 号 码」，先压紧再匹配
        String compactCjk = normalized.replaceAll("(?<=[\\u4e00-\\u9fff])\\s+(?=[\\u4e00-\\u9fff])", "");
        raw.invoiceNo = digitsOnly(matchFirst(INVOICE_NO_PATTERN, compactCjk));
        if (StrUtil.isBlank(raw.invoiceNo)) {
            raw.invoiceNo = digitsOnly(matchFirst(INVOICE_NO_NEAR_PATTERN, compactCjk));
        }
        if (StrUtil.isBlank(raw.invoiceNo)) {
            raw.invoiceNo = digitsOnly(matchFirst(INVOICE_NO_PATTERN, normalized));
        }
        if (StrUtil.isBlank(raw.invoiceNo)) {
            raw.invoiceNo = digitsOnly(matchFirst(INVOICE_NO_TAG_PATTERN, normalized));
        }
        raw.invoiceDate = matchFirst(DATE_PATTERN, compactCjk);
        if (StrUtil.isBlank(raw.invoiceDate)) {
            raw.invoiceDate = matchFirst(DATE_PATTERN, normalized);
        }
        raw.buyerTaxNo = matchFirst(BUYER_TAX_PATTERN, compactCjk);
        if (StrUtil.isBlank(raw.buyerTaxNo)) {
            raw.buyerTaxNo = matchFirst(BUYER_TAX_PATTERN, normalized);
        }
        raw.sellerTaxNo = matchFirst(SELLER_TAX_PATTERN, compactCjk);
        if (StrUtil.isBlank(raw.sellerTaxNo)) {
            raw.sellerTaxNo = matchFirst(SELLER_TAX_PATTERN, normalized);
        }
        raw.taxInclusiveAmount = parseDecimal(matchFirst(AMOUNT_INCLUSIVE_PATTERN, compactCjk));
        if (raw.taxInclusiveAmount == null) {
            raw.taxInclusiveAmount = parseDecimal(matchFirst(AMOUNT_INCLUSIVE_PATTERN, normalized));
        }
        raw.untaxAmount = parseDecimal(matchFirst(AMOUNT_UNTAX_PATTERN, compactCjk));
        if (raw.untaxAmount == null) {
            raw.untaxAmount = parseDecimal(matchFirst(AMOUNT_UNTAX_PATTERN, normalized));
        }
        raw.taxAmount = parseDecimal(matchFirst(AMOUNT_TAX_PATTERN, compactCjk));
        if (raw.taxAmount == null) {
            raw.taxAmount = parseDecimal(matchFirst(AMOUNT_TAX_PATTERN, normalized));
        }
        String typeProbe = compactCjk + normalized;
        if (typeProbe.contains("专用发票") || typeProbe.contains("专票")) {
            raw.invoiceType = InvoiceTypeEnum.SPECIAL;
        } else if (typeProbe.contains("普通发票") || typeProbe.contains("普票")) {
            raw.invoiceType = InvoiceTypeEnum.NORMAL;
        }
        return raw;
    }

    private static String digitsOnly(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        return StrUtil.isBlank(digits) ? null : digits;
    }

    private static InvoiceTypeEnum resolveInvoiceType(String code, String label, String fallback) {
        String joined = StrUtil.blankToDefault(code, "") + " " + StrUtil.blankToDefault(label, "") + " " + StrUtil
            .blankToDefault(fallback, "");
        if (joined.contains("普通") || joined.contains("普票") || "02".equals(StrUtil.trim(code))
            || "NORMAL".equalsIgnoreCase(StrUtil.trim(fallback))) {
            return InvoiceTypeEnum.NORMAL;
        }
        if (joined.contains("专用") || joined.contains("专票") || "01".equals(StrUtil.trim(code))
            || "SPECIAL".equalsIgnoreCase(StrUtil.trim(fallback))) {
            return InvoiceTypeEnum.SPECIAL;
        }
        return InvoiceTypeEnum.SPECIAL;
    }

    private static BigDecimal normalizeTaxRate(BigDecimal rate) {
        if (rate == null) {
            return new BigDecimal("13");
        }
        // XML 常见 0.06 / 0.13；表单用百分数
        if (rate.compareTo(BigDecimal.ONE) <= 0) {
            return rate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }
        return rate.setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalizeDate(String raw) {
        CheckUtils.throwIfBlank(raw, "开票日期不能为空");
        String v = raw.trim()
            .replace('年', '-')
            .replace('月', '-')
            .replace('日', ' ')
            .replace('/', '-')
            .replace('.', '-');
        v = v.replaceAll("\\s+", " ").trim();
        // 数电票常见紧凑格式：yyyyMMdd / yyyyMMddHHmmss
        if (v.matches("^\\d{8}$") || v.matches("^\\d{14}$")) {
            v = v.substring(0, 4) + "-" + v.substring(4, 6) + "-" + v.substring(6, 8);
        } else if (v.length() >= 10) {
            // yyyy-M-d → pad
            String[] parts = v.substring(0, Math.min(v.length(), 10)).split("-");
            if (parts.length >= 3) {
                v = String.format("%s-%02d-%02d", parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]
                    .replaceAll("\\D.*", "")));
            } else {
                v = v.substring(0, 10);
            }
        }
        try {
            LocalDate d = LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
            // 须带秒：LocalDateTime.toString() 在 00:00:00 会省略秒，变成 2026-08-12T00:00，Jackson 无法反序列化
            return LocalDateTime.of(d, LocalTime.MIN).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            throw new BusinessException("开票日期格式不正确：" + raw);
        }
    }

    private static String matchFirst(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static BigDecimal parseDecimal(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String v = raw.trim().replace(",", "").replace("￥", "").replace("¥", "").replace("%", "");
        if (!NumberUtil.isNumber(v)) {
            return null;
        }
        return new BigDecimal(v);
    }

    private static List<byte[]> extractZipXmlParts(byte[] bytes) {
        List<byte[]> parts = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes), Charset.forName("GBK"))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = StrUtil.blankToDefault(entry.getName(), "").toLowerCase(Locale.ROOT);
                byte[] data = readAll(zis);
                if (name.endsWith(".xml") || looksLikeXml(data)) {
                    parts.add(data);
                }
            }
        } catch (Exception e) {
            // 再试 UTF-8
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String name = StrUtil.blankToDefault(entry.getName(), "").toLowerCase(Locale.ROOT);
                    byte[] data = readAll(zis);
                    if (name.endsWith(".xml") || looksLikeXml(data)) {
                        parts.add(data);
                    }
                }
            } catch (Exception ex) {
                throw new BusinessException("OFD 解压失败");
            }
        }
        return parts;
    }

    private static boolean looksLikeXml(byte[] data) {
        String t = peekText(data).trim();
        return t.startsWith("<?xml") || t.startsWith("<");
    }

    private static boolean isZip(byte[] bytes) {
        return bytes != null && bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K';
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        in.transferTo(out);
        return out.toByteArray();
    }

    private static String peekText(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        int len = Math.min(data.length, 512 * 1024);
        String utf8 = new String(data, 0, len, StandardCharsets.UTF_8);
        long bad = utf8.chars().filter(c -> c == '\uFFFD').count();
        if (bad > 5) {
            return new String(data, 0, len, Charset.forName("GBK"));
        }
        return utf8;
    }

    private static String extension(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0) {
            return "";
        }
        return filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static final class RawInvoice {
        private String invoiceNo;
        private InvoiceTypeEnum invoiceType;
        private String invoiceDate;
        private String buyerTaxNo;
        private String sellerTaxNo;
        private BigDecimal taxInclusiveAmount;
        private BigDecimal untaxAmount;
        private BigDecimal taxAmount;
        private String remark;
        private final List<RawItem> items = new ArrayList<>();
    }

    private static final class RawItem {
        private String goodsName;
        private BigDecimal quantity;
        private BigDecimal taxInclusiveAmount;
        private BigDecimal untaxAmount;
        private BigDecimal taxAmount;
        private BigDecimal taxRate;
    }
}
