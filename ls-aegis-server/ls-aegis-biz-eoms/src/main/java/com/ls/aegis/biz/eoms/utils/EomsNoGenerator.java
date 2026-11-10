package com.ls.aegis.biz.eoms.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.ls.aegis.common.api.docno.DocNoPrefixApi;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.starter.support.docno.DocNoConstants;
import com.ls.aegis.starter.support.docno.DocNoService;

/**
 * EOMS 编号门面（委托平台 {@link DocNoService}；前缀只读 {@code sys_option}）
 *
 * @author EOMS
 */
@Component
public class EomsNoGenerator {

    private static final Logger log = LoggerFactory.getLogger(EomsNoGenerator.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern(DocNoConstants.DATE_PATTERN);

    private final DocNoService docNoService;
    private final DocNoPrefixApi docNoPrefixApi;
    private final JdbcTemplate jdbcTemplate;

    public EomsNoGenerator(DocNoService docNoService, DocNoPrefixApi docNoPrefixApi, JdbcTemplate jdbcTemplate) {
        this.docNoService = docNoService;
        this.docNoPrefixApi = docNoPrefixApi;
        this.jdbcTemplate = jdbcTemplate;
    }

    private String generate(String prefix, String tableName, String columnName) {
        String dateStr = LocalDate.now().format(DATE_FMT);
        Long dbSeed = queryMaxSeqFromDb(prefix, dateStr, tableName, columnName);
        return docNoService.nextEomsDaily(prefix, dbSeed);
    }

    private Long queryMaxSeqFromDb(String prefix, String dateStr, String tableName, String columnName) {
        if (StringUtils.isEmpty(tableName) || StringUtils.isEmpty(columnName)) {
            return null;
        }
        try {
            String likePattern = prefix + dateStr + "%";
            int substringPos = prefix.length() + dateStr.length() + 1;
            String sql = "SELECT MAX(CAST(SUBSTRING(" + columnName + ", " + substringPos + ") AS UNSIGNED)) "
                + "FROM " + tableName + " WHERE " + columnName + " LIKE ?";
            Long result = jdbcTemplate.queryForObject(sql, Long.class, likePattern);
            if (result != null && result > 0) {
                log.info("从数据库初始化计数器: prefix={}, date={}, maxSeq={}", prefix, dateStr, result);
                return result;
            }
            return null;
        } catch (Exception e) {
            log.warn("查询数据库最大编号失败: prefix={}, error={}", prefix, e.getMessage());
            return null;
        }
    }

    private String resolvePrefix(String optionCode, String defaultPrefix) {
        return docNoPrefixApi.resolvePrefix(optionCode, defaultPrefix);
    }

    public String generateRepairNo() {
        return generate(resolvePrefix(EomsConstants.OPTION_CODE_PREFIX_REPAIR, EomsConstants.PREFIX_REPAIR),
            "eoms_repair_apply", "repair_no");
    }

    public String generateOrderNo() {
        return generate(resolvePrefix(EomsConstants.OPTION_CODE_PREFIX_ORDER, EomsConstants.PREFIX_ORDER),
            "eoms_repair_order", "order_no");
    }

    public String generateQuoteNo() {
        return generate(resolvePrefix(EomsConstants.OPTION_CODE_PREFIX_QUOTE, EomsConstants.PREFIX_QUOTE),
            "eoms_quote", "quote_no");
    }

    public String generateContractNo() {
        return generate(resolvePrefix(EomsConstants.OPTION_CODE_PREFIX_CONTRACT, EomsConstants.PREFIX_CONTRACT),
            "eoms_contract", "contract_no");
    }

    public String generateIncomeNo() {
        return generate(resolvePrefix(EomsConstants.OPTION_CODE_PREFIX_INCOME, EomsConstants.PREFIX_INCOME),
            "eoms_income", "income_no");
    }

    public String generatePaymentNo() {
        return generate(resolvePrefix(EomsConstants.OPTION_CODE_PREFIX_PAYMENT, EomsConstants.PREFIX_PAYMENT),
            "eoms_payment", "payment_no");
    }

    public String generateReceivableNo() {
        return generate(resolvePrefix(EomsConstants.OPTION_CODE_PREFIX_RECEIVABLE, EomsConstants.PREFIX_RECEIVABLE),
            "eoms_receivable", "receivable_no");
    }

    public String generateSettlementNo() {
        return generate(resolvePrefix(EomsConstants.OPTION_CODE_PREFIX_SETTLEMENT, EomsConstants.PREFIX_SETTLEMENT),
            "eoms_repair_settlement", "settlement_no");
    }
}
