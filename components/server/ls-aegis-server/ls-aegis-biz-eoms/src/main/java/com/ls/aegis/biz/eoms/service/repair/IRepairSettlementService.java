package com.ls.aegis.biz.eoms.service.repair;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.RepairSettlement;

/**
 * RepairSettlement Service接口
 * 
 * @author EOMS Generator
 */
public interface IRepairSettlementService
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
     * 批量删除RepairSettlement
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteRepairSettlementByIds(Long[] ids);

    /**
     * 删除RepairSettlement信息
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteRepairSettlementById(Long id);

    /**
     * 根据工单ID查询结算单
     */
    public RepairSettlement selectSettlementByOrderId(Long repairOrderId);

    /**
     * 结算确认（8动作原子事务）
     * 1. 校验结算单状态为DRAFT
     * 2. 自动计算金额（材料收入/成本、总收入/总成本、利润）
     * 3. 更新结算单状态为CONFIRMED
     * 4. 更新工单状态为FINISHED
     * 5. 扣减物料库存（乐观锁）+ 生成库存出库流水
     * 6. 生成收入记录
     * 7. 生成分项成本记录（MATERIAL/LABOR/OTHER）
     * 8. 生成应收账款记录
     * 全部操作在一个事务中完成，任一失败则回滚
     *
     * @param settlementId 结算单ID
     * @param remark       备注（可选）
     * @return 结果
     */
    public int confirmSettlement(Long settlementId, String remark);
    int reverseSettlement(Long settlementId, String reason);
}
