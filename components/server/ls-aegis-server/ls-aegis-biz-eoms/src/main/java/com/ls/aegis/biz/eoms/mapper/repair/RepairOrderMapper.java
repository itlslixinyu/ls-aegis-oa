package com.ls.aegis.biz.eoms.mapper.repair;

import java.util.List;
import java.util.Map;
import com.ls.aegis.biz.eoms.domain.RepairOrder;

/**
 * RepairOrder Mapper接口
 *
 * @author EOMS Generator
 */
public interface RepairOrderMapper
{
    /**
     * 查询RepairOrder
     *
     * @param id 主键
     * @return RepairOrder
     */
    public RepairOrder selectRepairOrderById(Long id);

    /**
     * 查询RepairOrder列表
     *
     * @param repairOrder RepairOrder
     * @return RepairOrder集合
     */
    public List<RepairOrder> selectRepairOrderList(RepairOrder repairOrder);

    /**
     * 查询工程师角色用户选项（用于工单派单下拉选择）
     * 仅返回具有 eoms_engineer 角色且状态正常的用户
     *
     * @return List&lt;Map&lt;id, nickName&gt;&gt;
     */
    public List<Map<String, Object>> selectEngineerOptions();

    /**
     * 新增RepairOrder
     * 
     * @param repairOrder RepairOrder
     * @return 结果
     */
    public int insertRepairOrder(RepairOrder repairOrder);

    /**
     * 修改RepairOrder
     * 
     * @param repairOrder RepairOrder
     * @return 结果
     */
    public int updateRepairOrder(RepairOrder repairOrder);

    /**
     * 删除RepairOrder（主键）
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteRepairOrderById(Long id);

    /**
     * 批量删除RepairOrder
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteRepairOrderByIds(Long[] ids);

    /**
     * 解除报修申请关联（将指定 apply_id 的工单的 apply_id 置 NULL）
     *
     * @param applyId 报修申请ID
     * @return 影响行数
     */
    public int clearApplyId(Long applyId);

    /**
     * CAS 更新工单状态（WHERE 当前状态 = expectedStatus 时才更新，防并发覆盖）
     *
     * @param id              工单ID
     * @param newStatus       新状态
     * @param updateBy        更新人
     * @param finishTime      办结时间（可为 null）
     * @param remark          备注（可为 null）
     * @param expectedStatus  期望的当前状态（不匹配时不更新，返回 0）
     * @return 影响行数，0 表示并发状态冲突
     */
    public int updateOrderStatusWithCheck(Long id, String newStatus, String updateBy,
                                          java.util.Date finishTime, String remark,
                                          String expectedStatus);
    int countEnabledEngineer(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId, @org.apache.ibatis.annotations.Param("engineerId") Long engineerId);
    RepairOrder lockOrder(Long id);
    int countActiveByApply(Long applyId);
    int reopenAfterSettlement(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("operator") String operator, @org.apache.ibatis.annotations.Param("remark") String remark);
    // Internal exact-order invariants, only after the service authorizes this order.
    java.util.List<com.ls.aegis.biz.eoms.domain.RepairSettlement> cancelSettlementHistory(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId, @org.apache.ibatis.annotations.Param("orderId") Long orderId);
    java.util.List<com.ls.aegis.biz.eoms.domain.Receivable> lockCancelReceivables(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId, @org.apache.ibatis.annotations.Param("orderId") Long orderId);
    int countCancelPayments(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId, @org.apache.ibatis.annotations.Param("orderId") Long orderId);
    java.math.BigDecimal cancelIncomeNet(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId, @org.apache.ibatis.annotations.Param("orderId") Long orderId);
    java.math.BigDecimal cancelCostNet(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId, @org.apache.ibatis.annotations.Param("orderId") Long orderId);
    int countCancelEvent(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId, @org.apache.ibatis.annotations.Param("orderId") Long orderId);
    int cancelClosed(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId, @org.apache.ibatis.annotations.Param("orderId") Long orderId,
                     @org.apache.ibatis.annotations.Param("operator") String operator, @org.apache.ibatis.annotations.Param("remark") String remark);

}
