package com.ls.aegis.biz.eoms.mapper.finance;


import java.util.List;
import com.ls.aegis.biz.eoms.domain.Receivable;
import org.apache.ibatis.annotations.Param;

/**
 * Receivable Mapper接口
 * 
 * @author EOMS Generator
 */
public interface ReceivableMapper
{
    /**
     * 查询Receivable
     * 
     * @param id 主键
     * @return Receivable
     */
    public Receivable selectReceivableById(Long id);

    /**
     * 查询Receivable列表
     *
     * @param receivable Receivable
     * @return Receivable集合
     */
    public List<Receivable> selectReceivableList(Receivable receivable);

    /**
     * 查询指定客户的未结清应收单（状态为 UNPAID 或 PARTIAL）
     * 用于收款登记时按客户加载待核销的应收单；支持 DataScope（params 由 Service 注入）
     *
     * @param query 须含 customerId；可含 DataScope params
     * @return 未结清应收单集合
     */
    public List<Receivable> selectUnsettledByCustomerId(Receivable query);

    /**
     * 新增Receivable
     * 
     * @param receivable Receivable
     * @return 结果
     */
    public int insertReceivable(Receivable receivable);

    /**
     * 修改Receivable
     * 
     * @param receivable Receivable
     * @return 结果
     */
    public int updateReceivable(Receivable receivable);

    /**
     * 收款核销：原子更新应收账款（已收金额累加、欠款金额递减、状态自动切换）
     * SQL条件：id匹配 + 未删除 + 状态非PAID + 当前欠款 >= 本次收款
     * 状态切换：欠款=0 → PAID，否则 → PARTIAL
     *
     * @param id            应收单ID
     * @param amount        本次收款金额（正数）
     * @param newStatus     新状态（PAID 或 PARTIAL）
     * @param updateBy      更新人
     * @return 影响行数（0表示应收单不存在、已结清或欠款不足）
     */
    public int updateReceivableOnPayment(@Param("id") Long id,
                                         @Param("amount") java.math.BigDecimal amount,
                                         @Param("newStatus") String newStatus,
                                         @Param("updateBy") String updateBy);

    /**
     * 删除收款时原子回滚应收账款（相对值更新 + 欠额充足校验，避免并发覆盖）
     * SQL条件：id匹配 + 未删除 + 当前已收金额 >= 本次回滚金额
     * 状态回滚：回滚后已收<=0 → UNPAID，否则 → PARTIAL
     *
     * @param id            应收单ID
     * @param amount        本次回滚金额（正数，=被删除收款的金额）
     * @param updateBy      更新人
     * @return 影响行数（0表示应收单不存在、已删除或已收不足回滚）
     */
    public int rollbackReceivableOnPaymentDelete(@Param("id") Long id,
                                                 @Param("amount") java.math.BigDecimal amount,
                                                 @Param("updateBy") String updateBy);

    /**
     * 删除Receivable（主键）
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteReceivableById(Long id);

    /**
     * 批量删除Receivable
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteReceivableByIds(Long[] ids);
    Receivable lockReceivable(Long id);
    List<Receivable> lockByOrder(Long orderId);
    int voidReceivable(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("operator") String operator);
}
