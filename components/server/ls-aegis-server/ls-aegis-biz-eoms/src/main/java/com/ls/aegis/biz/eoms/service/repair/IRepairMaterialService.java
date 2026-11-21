package com.ls.aegis.biz.eoms.service.repair;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.RepairMaterial;

/**
 * RepairMaterial Service接口
 * 
 * @author EOMS Generator
 */
public interface IRepairMaterialService
{
    /**
     * 查询RepairMaterial
     * 
     * @param id 主键
     * @return RepairMaterial
     */
    public RepairMaterial selectRepairMaterialById(Long id);

    /**
     * 查询RepairMaterial列表
     * 
     * @param repairMaterial RepairMaterial
     * @return RepairMaterial集合
     */
    public List<RepairMaterial> selectRepairMaterialList(RepairMaterial repairMaterial);

    /**
     * 新增RepairMaterial
     * 
     * @param repairMaterial RepairMaterial
     * @return 结果
     */
    public int insertRepairMaterial(RepairMaterial repairMaterial);

    /**
     * 修改RepairMaterial
     * 
     * @param repairMaterial RepairMaterial
     * @return 结果
     */
    public int updateRepairMaterial(RepairMaterial repairMaterial);

    /**
     * 批量删除RepairMaterial
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteRepairMaterialByIds(Long[] ids);

    /**
     * 删除RepairMaterial信息
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteRepairMaterialById(Long id);
}
