package com.ls.aegis.biz.quote.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.ls.aegis.biz.quote.constant.QuoteConstants;
import com.ls.aegis.biz.quote.domain.Quote;
import com.ls.aegis.biz.quote.domain.QuoteDetail;
import com.ls.aegis.biz.quote.mapper.QuoteDetailMapper;
import com.ls.aegis.biz.quote.mapper.QuoteMapper;
import com.ls.aegis.biz.quote.model.resp.QuoteSummaryResp;
import com.ls.aegis.biz.quote.port.QuoteNoAllocator;
import com.ls.aegis.biz.quote.port.QuoteToRepairBridge;
import com.ls.aegis.biz.quote.support.QuoteAmountCalculator;
import com.ls.aegis.biz.quote.support.QuoteAccessContext;
import com.ls.aegis.biz.quote.service.IQuoteService;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.starter.support.docno.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.continew.starter.core.exception.BusinessException;

/**
 * 报价 Service 实现
 */
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements IQuoteService {

    private final QuoteMapper quoteMapper;
    private final QuoteDetailMapper quoteDetailMapper;
    private final ObjectProvider<QuoteNoAllocator> quoteNoAllocator;
    private final ObjectProvider<QuoteToRepairBridge> quoteToRepairBridge;
    private final DocNoService docNoService;

    @Override
    public Quote selectQuoteById(Long id) {
        Quote quote = quoteMapper.selectQuoteById(id);
        if (quote != null) {
            QuoteDetail query = new QuoteDetail();
            query.setQuoteId(id);
            quote.setDetails(quoteDetailMapper.selectQuoteDetailList(query));
        }
        return quote;
    }

    @Override
    public List<Quote> selectQuoteList(Quote quote) {
        return quoteMapper.selectQuoteList(quote);
    }

    @Override
    public QuoteSummaryResp selectQuoteSummary() {
        String monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        QuoteSummaryResp resp = quoteMapper.selectQuoteSummary(monthStart);
        if (resp == null) {
            resp = new QuoteSummaryResp();
        }
        if (resp.getMonthAmount() == null) {
            resp.setMonthAmount(BigDecimal.ZERO);
        }
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertQuote(Quote quote) {
        var scope = QuoteAccessContext.current();
        if (scope.deptId() == null) throw new BusinessException("创建报价需要有效部门");
        quote.setTenantId(scope.tenantId());
        quote.setCreateUser(scope.userId());
        quote.setDeptId(scope.deptId());
        quote.setCreateBy(UserContextHolder.getUsername());
        if (!StringUtils.hasText(quote.getQuoteNo())) {
            quote.setQuoteNo(allocateQuoteNo());
        }
        // 新建一律草稿，避免直接落确认/取消绕过流转
        quote.setStatus(QuoteConstants.QUOTE_STATUS_DRAFT);
        normalizeQuoteHeader(quote, true);
        quote.setDetails(normalizeDetails(quote.getDetails()));
        QuoteAmountCalculator.applyTotals(quote, quote.getDetails());
        // 工单关联和删除标志只能由对应业务操作维护。
        quote.setRepairOrderId(null);
        quote.setDelFlag("0");
        int rows = quoteMapper.insertQuote(quote);
        persistDetails(quote.getId(), quote.getDetails(), quote.getCreateBy());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateQuote(Quote quote) {
        if (quote.getId() == null) {
            throw new BusinessException("报价单ID不能为空");
        }
        Quote existing = quoteMapper.selectQuoteByIdForUpdate(quote.getId());
        if (existing == null) {
            throw new BusinessException("报价单不存在");
        }
        String oldStatus = normalizeStatus(existing.getStatus());
        if (isTerminalStatus(oldStatus)) {
            throw new BusinessException("已确认或已取消的报价单不可修改，仅可查看与导出");
        }
        String newStatus = StringUtils.hasText(quote.getStatus())
            ? normalizeStatus(quote.getStatus())
            : oldStatus;
        assertStatusTransition(oldStatus, newStatus);
        quote.setStatus(newStatus);

        // PATCH 语义：未传税率/明细时使用持久化值，而非默认税率或客户端合计。
        if (quote.getTaxRate() == null) {
            quote.setTaxRate(existing.getTaxRate());
        }
        normalizeQuoteHeader(quote, false);
        boolean replaceLines = quote.getDetails() != null;
        List<QuoteDetail> finalDetails;
        if (replaceLines) {
            finalDetails = normalizeDetails(quote.getDetails());
        } else {
            QuoteDetail query = new QuoteDetail();
            query.setQuoteId(quote.getId());
            finalDetails = quoteDetailMapper.selectQuoteDetailList(query);
        }
        QuoteAmountCalculator.applyTotals(quote, finalDetails);
        quote.setRepairOrderId(null);
        quote.setDelFlag(null);
        if (replaceLines) {
            quoteDetailMapper.deleteQuoteDetailByQuoteId(quote.getId());
            persistDetails(quote.getId(), finalDetails, quote.getUpdateBy());
        }
        return quoteMapper.updateQuote(quote);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteQuoteByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        // 固定锁顺序，避免两个批量删除以相反顺序争锁。
        Long[] validIds = Arrays.stream(ids).filter(java.util.Objects::nonNull).distinct().sorted().toArray(Long[]::new);
        if (validIds.length == 0) {
            return 0;
        }
        for (Long id : validIds) {
            assertDeletable(id);
        }
        quoteDetailMapper.deleteQuoteDetailByQuoteIds(validIds);
        return quoteMapper.deleteQuoteByIds(validIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteQuoteById(Long id) {
        assertDeletable(id);
        quoteDetailMapper.deleteQuoteDetailByQuoteId(id);
        return quoteMapper.deleteQuoteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToOrder(Long quoteId, String address, String remark) {
        if (quoteId == null) {
            throw new BusinessException("报价单ID不能为空");
        }
        Quote existing = quoteMapper.selectQuoteByIdForUpdate(quoteId);
        if (existing == null) {
            throw new BusinessException("报价单不存在");
        }
        if (existing.getRepairOrderId() != null) {
            return existing.getRepairOrderId();
        }
        String oldStatus = normalizeStatus(existing.getStatus());
        if (!QuoteConstants.QUOTE_STATUS_DRAFT.equals(oldStatus)
            && !QuoteConstants.QUOTE_STATUS_SUBMIT.equals(oldStatus)) {
            throw new BusinessException("当前状态[" + oldStatus + "]不可转工单，仅草稿/已提交状态可转");
        }
        if (existing.getCustomerId() == null) {
            throw new BusinessException("报价单缺少客户信息，无法转工单");
        }

        QuoteToRepairBridge bridge = quoteToRepairBridge.getIfAvailable();
        if (bridge == null) {
            throw new BusinessException("报价转工单能力未就绪，请确认运维报修模块已启用");
        }

        String operator = resolveOperator();
        Long orderId = bridge.createOrderFromQuote(quoteId, existing.getCustomerId(), existing.getQuoteNo(), address,
            remark, operator);

        Quote update = new Quote();
        update.setId(quoteId);
        update.setRepairOrderId(orderId);
        update.setStatus(QuoteConstants.QUOTE_STATUS_CONFIRM);
        update.setUpdateBy(operator);
        if (orderId == null || quoteMapper.updateQuote(update) != 1) {
            throw new BusinessException("报价转工单失败，请重试");
        }
        return orderId;
    }

    /** 规范化后再计算与落库，空列表明确清空明细并将合计归零。 */
    private List<QuoteDetail> normalizeDetails(List<QuoteDetail> details) {
        List<QuoteDetail> normalized = new ArrayList<>();
        if (details == null) {
            return normalized;
        }
        int sort = 0;
        for (QuoteDetail line : details) {
            if (line == null || !StringUtils.hasText(line.getItemName())) {
                throw new BusinessException("明细项目名称不能为空");
            }
            String lineType = StringUtils.hasText(line.getLineType())
                ? line.getLineType().trim().toUpperCase(java.util.Locale.ROOT)
                : QuoteConstants.LINE_TYPE_SERVICE;
            if (!QuoteConstants.LINE_TYPE_MATERIAL.equals(lineType)
                && !QuoteConstants.LINE_TYPE_SERVICE.equals(lineType)) {
                throw new BusinessException("明细行类型必须是材料或服务");
            }
            if (line.getQuantity() == null || line.getQuantity().signum() <= 0) {
                throw new BusinessException("明细数量必须大于 0：" + line.getItemName());
            }
            if (line.getPrice() == null || line.getPrice().signum() < 0) {
                throw new BusinessException("明细单价不能为空或为负：" + line.getItemName());
            }
            // 与现有 DECIMAL(10,2)/(12,2) 字段保持一致，避免数据库隐式舍入后明细与总额不符。
            BigDecimal quantity = line.getQuantity().setScale(2, RoundingMode.HALF_UP);
            BigDecimal price = line.getPrice().setScale(2, RoundingMode.HALF_UP);
            if (quantity.signum() <= 0 || quantity.precision() > 10 || price.precision() > 12) {
                throw new BusinessException("明细数量或单价超出允许精度：" + line.getItemName());
            }
            line.setQuantity(quantity);
            line.setPrice(price);
            line.setAmount(quantity.multiply(price).setScale(2, RoundingMode.HALF_UP));
            if (line.getAmount().precision() > 12) {
                throw new BusinessException("明细金额超出允许范围：" + line.getItemName());
            }
            line.setLineType(lineType);
            if (QuoteConstants.LINE_TYPE_SERVICE.equals(lineType)) {
                line.setGoodsId(null);
            }
            if (!StringUtils.hasText(line.getUnit())) {
                line.setUnit(QuoteConstants.LINE_TYPE_MATERIAL.equals(lineType) ? "台" : "项");
            }
            if (line.getSortNo() == null) {
                line.setSortNo(++sort);
            } else {
                sort++;
            }
            normalized.add(line);
        }
        return normalized;
    }

    private void persistDetails(Long quoteId, List<QuoteDetail> details, String operator) {
        for (QuoteDetail line : details) {
            line.setId(null);
            line.setQuoteId(quoteId);
            line.setDelFlag("0");
            if (StringUtils.hasText(operator)) {
                line.setCreateBy(operator);
            }
            quoteDetailMapper.insertQuoteDetail(line);
        }
    }

    private void normalizeQuoteHeader(Quote quote, boolean isCreate) {
        if (quote.getTaxRate() == null) {
            quote.setTaxRate(new BigDecimal("4.00"));
        }
        if (quote.getTaxRate().signum() < 0 || quote.getTaxRate().compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("税率必须在 0 到 100 之间");
        }
        quote.setTaxRate(quote.getTaxRate().setScale(2, RoundingMode.HALF_UP));
        if (quote.getQuoteDate() == null && isCreate) {
            quote.setQuoteDate(new Date());
        }
        if (!StringUtils.hasText(quote.getTerms())) {
            quote.setTerms(null);
        }
    }

    private static String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : QuoteConstants.QUOTE_STATUS_DRAFT;
    }

    /** 已确认 / 已取消：终态，禁止再改内容与状态（轻量留存） */
    private static boolean isTerminalStatus(String status) {
        return QuoteConstants.QUOTE_STATUS_CONFIRM.equals(status)
            || QuoteConstants.QUOTE_STATUS_CANCEL.equals(status);
    }

    /**
     * 纯报价状态机（与工单无关）：
     * 草稿 → 已提交 / 已确认 / 已取消；
     * 已提交 → 草稿 / 已确认 / 已取消。
     */
    private static void assertStatusTransition(String from, String to) {
        if (from.equals(to)) {
            return;
        }
        boolean allowed = switch (from) {
            case QuoteConstants.QUOTE_STATUS_DRAFT -> QuoteConstants.QUOTE_STATUS_SUBMIT.equals(to)
                || QuoteConstants.QUOTE_STATUS_CONFIRM.equals(to)
                || QuoteConstants.QUOTE_STATUS_CANCEL.equals(to);
            case QuoteConstants.QUOTE_STATUS_SUBMIT -> QuoteConstants.QUOTE_STATUS_DRAFT.equals(to)
                || QuoteConstants.QUOTE_STATUS_CONFIRM.equals(to)
                || QuoteConstants.QUOTE_STATUS_CANCEL.equals(to);
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException("不允许从状态[" + from + "]变更为[" + to + "]");
        }
    }

    private void assertDeletable(Long id) {
        if (id == null) {
            return;
        }
        Quote existing = quoteMapper.selectQuoteByIdForUpdate(id);
        if (existing == null) {
            throw new BusinessException("报价单不存在或无权操作");
        }
        if (isTerminalStatus(normalizeStatus(existing.getStatus()))) {
            throw new BusinessException("已确认或已取消的报价单不可删除");
        }
    }

    private String allocateQuoteNo() {
        QuoteNoAllocator allocator = quoteNoAllocator.getIfAvailable();
        if (allocator != null) {
            return allocator.nextQuoteNo();
        }
        return docNoService.nextEomsDaily("BJ", null);
    }

    private String resolveOperator() {
        try {
            String username = UserContextHolder.getUsername();
            return StringUtils.hasText(username) ? username : "system";
        } catch (Exception e) {
            return "system";
        }
    }
}
