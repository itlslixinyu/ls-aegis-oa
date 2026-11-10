package com.ls.aegis.biz.eoms.mapper.finance;


import java.util.List;
import com.ls.aegis.biz.eoms.domain.Payment;

/**
 * Payment Mapper接口
 * 
 * @author EOMS Generator
 */
public interface PaymentMapper
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
     * 删除Payment（主键）
     * 
     * @param id 主键
     * @return 结果
     */
    public int deletePaymentById(Long id);

    /**
     * 批量删除Payment
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deletePaymentByIds(Long[] ids);
    Payment lockPayment(Long id);
    int reversePayment(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("operator") String operator);
    int countActiveByReceivable(Long id);
}
