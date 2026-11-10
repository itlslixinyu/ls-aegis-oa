package com.ls.aegis.biz.eoms.service.impl.finance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.SecurityUtils;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.domain.Income;
import com.ls.aegis.biz.eoms.domain.Payment;
import com.ls.aegis.biz.eoms.domain.PaymentVerifyBatchDTO;
import com.ls.aegis.biz.eoms.domain.PaymentVerifyResultVO;
import com.ls.aegis.biz.eoms.domain.Receivable;
import com.ls.aegis.biz.eoms.mapper.finance.IncomeMapper;
import com.ls.aegis.biz.eoms.mapper.finance.PaymentMapper;
import com.ls.aegis.biz.eoms.mapper.finance.ReceivableMapper;
import com.ls.aegis.biz.eoms.service.finance.IPaymentService;
import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.starter.support.idempotent.IdempotentConstants;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import top.continew.starter.core.exception.BusinessException;

/**
 * Payment Service实现
 *
 * @author EOMS Generator
 */
@Service("eomsPaymentServiceImpl")
public class PaymentServiceImpl implements IPaymentService
{
    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private ReceivableMapper receivableMapper;


    @Autowired
    private IncomeMapper incomeMapper;

    @Autowired
    private com.ls.aegis.biz.eoms.mapper.finance.FinanceEventMapper financeEventMapper;

    @Autowired
    private EomsNoGenerator eomsNoGenerator;

    @Autowired
    private IdempotentHelper idempotentHelper;

    /** 幂等令牌缺失或冲突的错误码 */
    private static final int CODE_IDEMPOTENT_CONFLICT = 10009;

    @Override
    public Payment selectPaymentById(Long id)
    {
        if (EomsDataScopeHelper.shouldFilter())
        {
            Payment query = new Payment();
            query.setId(id);
            query.setStatus("ALL");
            EomsDataScopeHelper.apply(query, false, "p");
            List<Payment> list = paymentMapper.selectPaymentList(query);
            return list.isEmpty() ? null : list.get(0);
        }
        return paymentMapper.selectPaymentById(id);
    }

    @Override
    public List<Payment> selectPaymentList(Payment payment)
    {
        EomsDataScopeHelper.apply(payment, false, "p");
        return paymentMapper.selectPaymentList(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertPayment(Payment payment)
    {
        return verifyReceivable(payment);
    }

    @Override
    public int updatePayment(Payment payment)
    {
        throw new ServiceException("已入账收款不可编辑，请撤销后重新收款");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePaymentByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) return 0;
        if (java.util.Arrays.stream(ids).anyMatch(java.util.Objects::isNull)) throw new ServiceException("收款ID不能为空");
        List<Payment> payments = new ArrayList<>();
        for (Long id : java.util.Arrays.stream(ids).distinct().sorted().toList())
        {
            Payment visible = selectPaymentById(id);
            if (visible == null) throw new ServiceException("收款不存在或无权访问");
            payments.add(visible);
        }
        // All finance paths lock receivables before payments; sorted locks avoid batch deadlock.
        for (Long receivableId : payments.stream().map(Payment::getReceivableId).distinct().sorted().toList())
        {
            if (receivableMapper.lockReceivable(receivableId) == null) throw new ServiceException("应收不存在，不能撤销收款");
        }
        int changed = 0;
        String operator = getCurrentUsername();
        for (Payment visible : payments)
        {
            Payment payment = paymentMapper.lockPayment(visible.getId());
            if (payment == null) throw new ServiceException("收款已变更，请刷新重试");
            if ("REVERSED".equals(payment.getStatus())) continue;
            Income original = incomeMapper.selectOriginalByPaymentId(payment.getId());
            if (original == null || original.getAmount() == null || payment.getAmount() == null
                    || original.getAmount().compareTo(payment.getAmount()) != 0)
                throw new ServiceException("收款收入关联缺失或金额不一致，请先对账建立明确关联后撤销");
            if (receivableMapper.rollbackReceivableOnPaymentDelete(payment.getReceivableId(), payment.getAmount(), operator) != 1)
                throw new ServiceException("应收已收金额不足或状态已变更，撤销失败");
            Income reversal = new Income();
            reversal.setIncomeNo(eomsNoGenerator.generateIncomeNo());
            reversal.setRepairOrderId(original.getRepairOrderId());
            reversal.setCustomerId(original.getCustomerId());
            reversal.setAmount(original.getAmount().negate());
            reversal.setIncomeType(original.getIncomeType());
            reversal.setIncomeDate(new Date());
            reversal.setReversalOfId(original.getId());
            reversal.setCreateBy(operator);
            reversal.setDelFlag("0");
            reversal.setRemark("撤销收款#" + payment.getId() + "，冲销收入#" + original.getId());
            incomeMapper.insertIncome(reversal);
            if (paymentMapper.reversePayment(payment.getId(), operator) != 1) throw new ServiceException("收款状态已变更");
            financeEventMapper.insertEvent("PAYMENT", payment.getId(), "REVERSE", original.getId(),
                    payment.getAmount().negate(), operator, "撤销收款并冲销收入，恢复应收");
            changed++;
        }
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePaymentById(Long id)
    {
        return deletePaymentByIds(new Long[]{id});
    }

    /**
     * 收款核销（payment → receivable 状态切换）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int verifyReceivable(Payment payment)
    {
        return withPaymentToken(payment == null ? null : payment.getIdempotentToken(), "payment-verify", () -> {
            doVerifyReceivable(payment);
            return 1;
        });
    }

    /**
     * 收款核销实际业务逻辑（被 verifyReceivable 包装用于幂等控制）
     * 返回核销结果明细，供单笔和批量核销共用
     */
    private PaymentVerifyResultVO.ItemResult doVerifyReceivable(Payment payment)
    {
        // ===== 1. 参数与应收单校验 =====
        if (payment == null || payment.getReceivableId() == null)
        {
            throw new ServiceException("收款参数或应收单ID不能为空", EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
        }
        BigDecimal amount = payment.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("收款金额必须大于0", EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
        }
        if (amount.stripTrailingZeros().scale() > 2 || amount.precision() - amount.scale() > 10)
            throw new ServiceException("收款金额最多10位整数、2位小数", EomsConstants.CODE_RECEIVABLE_DATA_ERROR);

        Receivable scoped = selectReceivableScoped(payment.getReceivableId());
        Receivable receivable = scoped == null ? null : receivableMapper.lockReceivable(scoped.getId());
        if (receivable == null || "2".equals(receivable.getDelFlag()))
        {
            throw new ServiceException("应收账款不存在", EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
        }
        if (!java.util.Set.of("UNPAID", "PARTIAL").contains(receivable.getStatus()))
        {
            throw new ServiceException("应收账款已结清，不可再次收款", EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
        }

        BigDecimal arrears = receivable.getArrearsAmount() == null
                ? BigDecimal.ZERO : receivable.getArrearsAmount();
        if (amount.compareTo(arrears) > 0)
        {
            throw new ServiceException("收款金额[" + amount + "]超过欠款金额[" + arrears + "]",
                    EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
        }

        String operator = getCurrentUsername();
        Date now = new Date();

        // ===== 2. 补全并插入收款记录 =====
        // 单笔核销时 paymentNo 为空，此处生成；批量核销时已由外层生成共享 SK 单号，此处复用
        if (StringUtils.isEmpty(payment.getPaymentNo()))
        {
            payment.setPaymentNo(eomsNoGenerator.generatePaymentNo());
        }
        payment.setStatus("ACTIVE");
        payment.setCustomerId(receivable.getCustomerId());
        if (payment.getPaymentTime() == null)
        {
            payment.setPaymentTime(now);
        }
        payment.setDelFlag("0");
        payment.setReversedBy(null);
        payment.setReversedTime(null);
        if (StringUtils.isNotEmpty(operator))
        {
            payment.setCreateBy(operator);
        }
        EomsDataScopeHelper.fillOnInsert(payment);
        paymentMapper.insertPayment(payment);

        // ===== 3. 计算新状态并原子更新应收账款 =====
        BigDecimal newArrears = arrears.subtract(amount);
        String newStatus = newArrears.compareTo(BigDecimal.ZERO) == 0
                ? EomsConstants.RECEIVABLE_STATUS_PAID
                : EomsConstants.RECEIVABLE_STATUS_PARTIAL;

        int rows = receivableMapper.updateReceivableOnPayment(
                receivable.getId(), amount, newStatus, operator);
        if (rows == 0)
        {
            // 并发场景下可能被其他线程先行收款导致欠款不足或状态变更
            throw new ServiceException("收款核销失败：应收单状态已变更或欠款不足，请刷新后重试",
                    EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
        }

        // ===== P0-1 修复：收款核销成功后生成实收收入（Income）记录 =====
        // 财务逻辑：结算确认只产生应收（Receivable），实际收款时才产生实收收入（Income）。
        // 每次收款创建一条 Income 记录，金额=本次收款金额，确保收入与收款一一对应。
        Income income = new Income();
        income.setIncomeNo(eomsNoGenerator.generateIncomeNo());
        income.setPaymentId(payment.getId());
        income.setRepairOrderId(receivable.getRepairOrderId());
        income.setCustomerId(receivable.getCustomerId());
        income.setAmount(amount);
        income.setIncomeType("REPAIR");
        income.setIncomeDate(payment.getPaymentTime() != null ? payment.getPaymentTime() : now);
        if (StringUtils.isNotEmpty(operator))
        {
            income.setCreateBy(operator);
        }
        income.setDelFlag("0");
        income.setRemark("收款核销生成收入(应收单#" + receivable.getId() + "," + (payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "") + ")");
        incomeMapper.insertIncome(income);

        // ===== 5. 组装核销结果明细 =====
        PaymentVerifyResultVO.ItemResult result = new PaymentVerifyResultVO.ItemResult();
        result.setReceivableId(receivable.getId());
        result.setReceivableNo(receivable.getReceivableNo());
        result.setCustomerId(receivable.getCustomerId());
        result.setCustomerName(receivable.getCustomerName());
        result.setOrderNo(receivable.getOrderNo());
        result.setVerifyAmount(amount);
        result.setArrearsAmount(newArrears);
        result.setNewStatus(newStatus);
        result.setPaymentId(payment.getId());
        return result;
    }

    /**
     * 批量收款核销（同一客户一次付款核销多笔未结清应收单）
     * <p>
     * 外层走平台 {@code IdempotentHelper.runOnce}（同一令牌只成功一次）；
     * 任一笔核销失败则全部回滚，且不缓存令牌，允许原令牌重试。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVerifyResultVO verifyReceivableBatch(PaymentVerifyBatchDTO dto)
    {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty())
        {
            throw new ServiceException("批量核销明细不能为空", EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
        }

        return withPaymentToken(dto.getIdempotentToken(), "payment-verify-batch", () -> doVerifyReceivableBatch(dto));
    }

    /**
     * 批量核销实际业务逻辑（单事务内循环核销，任一笔失败全回滚）
     */
    private PaymentVerifyResultVO doVerifyReceivableBatch(PaymentVerifyBatchDTO dto)
    {
        List<PaymentVerifyBatchDTO.VerifyItem> items = new ArrayList<>(dto.getItems());
        items.sort(java.util.Comparator.comparing(PaymentVerifyBatchDTO.VerifyItem::getReceivableId, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())));
        // 校验同一客户：所有应收单必须属于同一客户（批量核销的业务约束）
        Long firstCustomerId = null;
        // 检查 receivableId 不重复（防止同一应收单在批量请求中重复核销）
        Map<Long, PaymentVerifyBatchDTO.VerifyItem> receivableMap = new LinkedHashMap<>();
        for (PaymentVerifyBatchDTO.VerifyItem item : items)
        {
            if (item.getReceivableId() == null)
            {
                throw new ServiceException("应收单ID不能为空", EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
            }
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            {
                throw new ServiceException("核销金额必须大于0（应收单#" + item.getReceivableId() + "）",
                        EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
            }
            if (receivableMap.containsKey(item.getReceivableId()))
            {
                throw new ServiceException("应收单#" + item.getReceivableId() + "在批量请求中重复",
                        EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
            }
            receivableMap.put(item.getReceivableId(), item);
        }

        // 预加载所有应收单并校验同一客户、未结清状态
        for (PaymentVerifyBatchDTO.VerifyItem item : items)
        {
            Receivable receivable = selectReceivableScoped(item.getReceivableId());
            if (receivable == null || "2".equals(receivable.getDelFlag()))
            {
                throw new ServiceException("应收单#" + item.getReceivableId() + "不存在",
                        EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
            }
            if (EomsConstants.RECEIVABLE_STATUS_PAID.equals(receivable.getStatus()))
            {
                throw new ServiceException("应收单#" + item.getReceivableId() + "已结清，不可再次收款",
                        EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
            }
            if (firstCustomerId == null)
            {
                firstCustomerId = receivable.getCustomerId();
            }
            else if (!firstCustomerId.equals(receivable.getCustomerId()))
            {
                throw new ServiceException("批量核销的应收单必须属于同一客户",
                        EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
            }
            BigDecimal arrears = receivable.getArrearsAmount() == null
                    ? BigDecimal.ZERO : receivable.getArrearsAmount();
            if (item.getAmount().compareTo(arrears) > 0)
            {
                throw new ServiceException("应收单#" + item.getReceivableId()
                        + "核销金额[" + item.getAmount() + "]超过欠款金额[" + arrears + "]",
                        EomsConstants.CODE_RECEIVABLE_DATA_ERROR);
            }
        }

        // ===== 批量核销：共享1个SK单号 + 1个batch_id =====
        // 业务规则：同一客户一次付款 = 一张实收单 = 一个SK单号
        // 多笔应收单核销明细通过 batch_id 关联，每条 payment 记录仍对应一个 receivable_id
        String sharedPaymentNo = eomsNoGenerator.generatePaymentNo();
        String batchId = java.util.UUID.randomUUID().toString().replace("-", "");

        // 循环核销（复用单笔核心逻辑），收集结果
        List<PaymentVerifyResultVO.ItemResult> resultList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PaymentVerifyBatchDTO.VerifyItem item : items)
        {
            Payment payment = new Payment();
            payment.setReceivableId(item.getReceivableId());
            payment.setAmount(item.getAmount());
            payment.setPaymentMethod(dto.getPaymentMethod());
            payment.setPaymentTime(dto.getPaymentTime());
            payment.setRemark(dto.getRemark());
            // 共享同一 SK 单号和批次ID
            payment.setPaymentNo(sharedPaymentNo);
            payment.setBatchId(batchId);
            PaymentVerifyResultVO.ItemResult result = doVerifyReceivable(payment);
            resultList.add(result);
            totalAmount = totalAmount.add(item.getAmount());
        }

        // 组装汇总结果
        PaymentVerifyResultVO vo = new PaymentVerifyResultVO();
        vo.setItems(resultList);
        vo.setTotalCount(resultList.size());
        vo.setTotalAmount(totalAmount);
        vo.setPaymentNo(sharedPaymentNo);
        vo.setBatchId(batchId);
        return vo;
    }

    /**
     * 收款核销幂等：同一令牌只成功一次；业务失败不缓存，允许原令牌重试
     */
    private <T> T withPaymentToken(String token, String action, Supplier<T> actionFn)
    {
        if (StringUtils.isEmpty(token))
        {
            throw new ServiceException("缺少幂等令牌 idempotentToken，请刷新页面后重新提交收款", 400);
        }
        try
        {
            return idempotentHelper.runOnce(IdempotentConstants.EOMS_KEY_PREFIX, UserContextHolder.getTenantId(),
                action, token, IdempotentConstants.EOMS_PAYMENT_TTL, actionFn);
        }
        catch (BusinessException e)
        {
            String msg = e.getMessage();
            if ("请勿重复提交".equals(msg) || "业务处理中，请勿重复提交".equals(msg))
            {
                throw new ServiceException(msg, CODE_IDEMPOTENT_CONFLICT);
            }
            throw e;
        }
    }

    /**
     * 按 DataScope 加载应收单（核销入口防越权）
     */
    private Receivable selectReceivableScoped(Long id)
    {
        if (id == null)
        {
            return null;
        }
        if (!EomsDataScopeHelper.shouldFilter())
        {
            return receivableMapper.selectReceivableById(id);
        }
        Receivable query = new Receivable();
        query.setId(id);
        EomsDataScopeHelper.apply(query, false, "r");
        List<Receivable> list = receivableMapper.selectReceivableList(query);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 获取当前操作用户名（兼容未登录场景）
     */
    private String getCurrentUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return null;
        }
    }

}
