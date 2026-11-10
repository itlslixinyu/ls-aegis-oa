package com.ls.aegis.biz.eoms.mapper.repair;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.RepairRecord;

/**
 * RepairRecord Mapper接口
 * 
 * @author EOMS Generator
 */
public interface RepairRecordMapper
{
    List<Long> lockActiveRecordIds(@org.apache.ibatis.annotations.Param("orderId") Long orderId,
                                  @org.apache.ibatis.annotations.Param("tenantId") Long tenantId);
    RepairRecord lockRecord(@org.apache.ibatis.annotations.Param("id") Long id,
                            @org.apache.ibatis.annotations.Param("tenantId") Long tenantId);
    /**
     * 查询RepairRecord
     * 
     * @param id 主键
     * @return RepairRecord
     */
    public RepairRecord selectRepairRecordById(Long id);

    /**
     * 查询RepairRecord列表
     * 
     * @param repairRecord RepairRecord
     * @return RepairRecord集合
     */
    public List<RepairRecord> selectRepairRecordList(RepairRecord repairRecord);

    /**
     * 新增RepairRecord
     * 
     * @param repairRecord RepairRecord
     * @return 结果
     */
    public int insertRepairRecord(RepairRecord repairRecord);

    /**
     * 修改RepairRecord
     * 
     * @param repairRecord RepairRecord
     * @return 结果
     */
    public int updateRepairRecord(RepairRecord repairRecord);

    /**
     * 删除RepairRecord（主键）
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteRepairRecordById(Long id);

    /**
     * 批量删除RepairRecord
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteRepairRecordByIds(Long[] ids);

    /**
     * 按工单ID统计存在的未删除维修记录（用于校验"一个工单只能提交一次维修记录"）
     *
     * @param repairOrderId 工单ID
     * @return 存在返回 >=1，不存在返回 0
     */
    public int countByRepairOrderId(Long repairOrderId);
}
