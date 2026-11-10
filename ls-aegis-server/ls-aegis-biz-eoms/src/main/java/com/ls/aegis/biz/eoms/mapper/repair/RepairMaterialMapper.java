package com.ls.aegis.biz.eoms.mapper.repair;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.RepairMaterial;

/**
 * RepairMaterial Mapper接口
 * 
 * @author EOMS Generator
 */
public interface RepairMaterialMapper
{
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    int countUsableReferences(@org.apache.ibatis.annotations.Param("tenantId") Long tenantId,
        @org.apache.ibatis.annotations.Param("goodsId") Long goodsId,
        @org.apache.ibatis.annotations.Param("warehouseId") Long warehouseId);
    // Material has no tenant column: the service authorizes/locks its exact tenant-scoped parent.
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    RepairMaterial lockMaterial(@org.apache.ibatis.annotations.Param("id") Long id,
        @org.apache.ibatis.annotations.Param("orderId") Long orderId);
    java.util.List<com.ls.aegis.biz.eoms.domain.RepairMaterial> lockByOrder(Long orderId);
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
    // Material has no tenant column: the service authorizes/locks its exact tenant-scoped parent.
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    public List<RepairMaterial> selectRepairMaterialList(RepairMaterial repairMaterial);

    /**
     * 新增RepairMaterial
     * 
     * @param repairMaterial RepairMaterial
     * @return 结果
     */
    // Material has no tenant column: the service authorizes/locks its exact tenant-scoped parent.
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    public int insertRepairMaterial(RepairMaterial repairMaterial);

    /**
     * 修改RepairMaterial
     * 
     * @param repairMaterial RepairMaterial
     * @return 结果
     */
    // Material has no tenant column: the service authorizes/locks its exact tenant-scoped parent.
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    public int updateRepairMaterial(RepairMaterial repairMaterial);

    /**
     * 删除RepairMaterial（主键）
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteRepairMaterialById(Long id);

    /**
     * 批量删除RepairMaterial
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    // Material has no tenant column: the service authorizes/locks its exact tenant-scoped parent.
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    public int deleteRepairMaterialByIds(Long[] ids);
}
