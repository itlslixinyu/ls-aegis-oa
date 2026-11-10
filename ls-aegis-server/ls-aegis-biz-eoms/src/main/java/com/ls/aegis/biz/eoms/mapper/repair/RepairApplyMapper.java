package com.ls.aegis.biz.eoms.mapper.repair;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.RepairApply;

/**
 * RepairApply Mapper接口
 * 
 * @author EOMS Generator
 */
public interface RepairApplyMapper
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
     * 删除RepairApply（主键）
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteRepairApplyById(Long id);

    /**
     * 批量删除RepairApply
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteRepairApplyByIds(Long[] ids);
    com.ls.aegis.biz.eoms.domain.RepairApply lockApply(Long id);
    int restoreAfterOrderDelete(Long id);
}
