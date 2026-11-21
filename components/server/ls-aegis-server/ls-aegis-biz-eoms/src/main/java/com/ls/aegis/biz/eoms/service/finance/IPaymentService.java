package com.ls.aegis.biz.eoms.service.finance;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.Payment;
import com.ls.aegis.biz.eoms.domain.PaymentVerifyBatchDTO;
import com.ls.aegis.biz.eoms.domain.PaymentVerifyResultVO;

/**
 * Payment Service接口
 * 
 * @author EOMS Generator
 */
public interface IPaymentService
{
    /**
     * 查询Payment
     * 
     * @param id 主键
     * @return Payment
     */
    public Payment selectPaymentById(Long id);

    /**
     * 查询Payment列表
     * 
     * @param payment Payment
     * @return Payment集合
     */
    public List<Payment> selectPaymentList(Payment payment);

    /**
     * 新增Payment
     * 
     * @param payment Payment
     * @return 结果
     */
    public int insertPayment(Payment payment);

    /**
     * 修改Payment
     * 
     * @param payment Payment
     * @return 结果
     */
    public int updatePayment(Payment payment);

    /**
     * 批量删除Payment
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deletePaymentByIds(Long[] ids);

    /**
     * 删除Payment信息
     * 
     * @param id 主键
     * @return 结果
     */
    public int deletePaymentById(Long id);

    /**
     * 收款核销（payment → receivable 状态切换）
     * 1. 校验应收单存在且状态非PAID
     * 2. 校验付款金额合法（>0 且 ≤ 欠款金额）
     * 3. 插入收款记录（Payment）
     * 4. 原子更新应收账款：已收金额累加、欠款金额递减、状态自动切换
     *    - 欠款=0 → PAID（已收款）
     *    - 欠款>0 → PARTIAL（部分收款）
     * 5. 记录操作日志（PAYMENT + RECEIVABLE 两类）
     * 全部操作在一个事务中完成，任一失败则回滚
     *
     * @param payment 收款单（需包含 receivableId、amount、paymentMethod，可选 paymentTime、remark）
     * @return 结果
     */
    public int verifyReceivable(Payment payment);

    /**
     * 批量收款核销（同一客户一次付款核销多笔未结清应收单）
     * <p>
     * 统一付款方式、付款时间、备注；每笔应收单独立核销金额。
     * 全部操作在一个事务中完成，任一笔失败则全部回滚。
     * 整个批量操作共享一个幂等令牌，防并发重复提交。
     * </p>
     *
     * @param dto 批量核销请求（含统一付款信息 + 核销明细列表 + 幂等令牌）
     * @return 核销结果（每笔明细 + 汇总信息）
     */
    public PaymentVerifyResultVO verifyReceivableBatch(PaymentVerifyBatchDTO dto);
}
