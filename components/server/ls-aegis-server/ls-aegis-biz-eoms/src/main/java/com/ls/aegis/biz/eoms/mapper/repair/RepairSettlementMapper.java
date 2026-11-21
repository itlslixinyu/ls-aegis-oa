package com.ls.aegis.biz.eoms.mapper.repair;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.RepairSettlement;
import org.apache.ibatis.annotations.Param;

/**
 * RepairSettlement Mapper接口
 * 
 * @author EOMS Generator
 */
public interface RepairSettlementMapper
{
    /**
     * 查询RepairSettlement
     * 
     * @param id 主键
     * @return RepairSettlement
     */
    public RepairSettlement selectRepairSettlementById(Long id);

    /**
     * 查询RepairSettlement列表
     * 
     * @param repairSettlement RepairSettlement
     * @return RepairSettlement集合
     */
    public List<RepairSettlement> selectRepairSettlementList(RepairSettlement repairSettlement);

    /**
     * 新增RepairSettlement
     * 
     * @param repairSettlement RepairSettlement
     * @return 结果
     */
    public int insertRepairSettlement(RepairSettlement repairSettlement);

    /**
     * 修改RepairSettlement
     * 
     * @param repairSettlement RepairSettlement
     * @return 结果
     */
    public int updateRepairSettlement(RepairSettlement repairSettlement);

    /**
     * CAS原子确认结算单（防并发双重确认）
     * 仅当状态为期望旧状态时才更新为新状态，返回受影响行数（0表示已被其他事务修改）
     *
     * @param id 结算单ID
     * @param oldStatus 期望的旧状态
     * @param newStatus 新状态
     * @param settlementNo 结算单号
     * @param serviceFee 服务费
     * @param laborAmount 人工费
     * @param otherAmount 其它费用
     * @param materialAmount 材料收入
     * @param materialCost 材料成本
     * @param totalIncome 总收入
     * @param totalCost 总成本
     * @param profitAmount 利润
     * @param updateBy 更新人
     * @param remark 备注
     * @return 受影响行数
     */
    public int casConfirmSettlement(@Param("id") Long id,
                                    @Param("oldStatus") String oldStatus,
                                    @Param("newStatus") String newStatus,
                                    @Param("settlementNo") String settlementNo,
                                    @Param("serviceFee") java.math.BigDecimal serviceFee,
                                    @Param("laborAmount") java.math.BigDecimal laborAmount,
                                    @Param("otherAmount") java.math.BigDecimal otherAmount,
                                    @Param("materialAmount") java.math.BigDecimal materialAmount,
                                    @Param("materialCost") java.math.BigDecimal materialCost,
                                    @Param("totalIncome") java.math.BigDecimal totalIncome,
                                    @Param("totalCost") java.math.BigDecimal totalCost,
                                    @Param("profitAmount") java.math.BigDecimal profitAmount,
                                    @Param("updateBy") String updateBy,
                                    @Param("remark") String remark);

    /**
     * 删除RepairSettlement（主键）
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteRepairSettlementById(Long id);

    /**
     * 批量删除RepairSettlement
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteRepairSettlementByIds(Long[] ids);

    /**
     * 根据工单ID查询结算单
     */
    public RepairSettlement selectSettlementByOrderId(Long repairOrderId);
    RepairSettlement lockSettlement(Long id);
    int reverseSettlement(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("operator") String operator, @org.apache.ibatis.annotations.Param("remark") String remark);
}
