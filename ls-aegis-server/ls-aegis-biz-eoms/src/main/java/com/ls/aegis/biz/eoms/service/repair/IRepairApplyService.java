package com.ls.aegis.biz.eoms.service.repair;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.RepairApply;

/**
 * RepairApply Service接口
 * 
 * @author EOMS Generator
 */
public interface IRepairApplyService
{
    /**
     * 查询RepairApply
     * 
     * @param id 主键
     * @return RepairApply
     */
    public RepairApply selectRepairApplyById(Long id);

    /**
     * 查询RepairApply列表
     * 
     * @param repairApply RepairApply
     * @return RepairApply集合
     */
    public List<RepairApply> selectRepairApplyList(RepairApply repairApply);

    /**
     * 新增RepairApply
     * 
     * @param repairApply RepairApply
     * @return 结果
     */
    public int insertRepairApply(RepairApply repairApply);

    /**
     * 修改RepairApply
     * 
     * @param repairApply RepairApply
     * @return 结果
     */
    public int updateRepairApply(RepairApply repairApply);

    /**
     * 批量删除RepairApply
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteRepairApplyByIds(Long[] ids);

    /**
     * 删除RepairApply信息
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteRepairApplyById(Long id);

    /**
     * 确认报修（WAIT → CONFIRM）
     *
     * @param id     报修申请ID
     * @param remark 备注（可选）
     * @return 结果
     */
    public int confirmApply(Long id, String remark);

    /**
     * 取消报修（WAIT → CANCEL）
     * 仅待处理状态的报修可直接取消
     *
     * @param id     报修申请ID
     * @param remark 备注（可选）
     * @return 结果
     */
    public int cancelApply(Long id, String remark);

    /**
     * 关闭报修（CONFIRM → CANCEL）
     * 已确认的报修因故关闭/取消
     *
     * @param id     报修申请ID
     * @param remark 备注（可选，建议填写关闭原因）
     * @return 结果
     */
    public int closeApply(Long id, String remark);

    /**
     * 转工单（CONFIRM → ORDER）
     * 将已确认的报修申请转为维修工单，自动创建工单（状态待接单）
     *
     * @param applyId 报修申请ID
     * @param remark  备注（可选）
     * @return 创建的工单ID
     */
    public Long toOrder(Long applyId, String remark);
}
