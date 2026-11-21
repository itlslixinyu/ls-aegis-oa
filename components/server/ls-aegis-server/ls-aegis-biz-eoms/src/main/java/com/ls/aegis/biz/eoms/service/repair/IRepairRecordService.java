package com.ls.aegis.biz.eoms.service.repair;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.RepairRecord;

/**
 * RepairRecord Service接口
 * 
 * @author EOMS Generator
 */
public interface IRepairRecordService
{
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
     * 批量删除RepairRecord
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteRepairRecordByIds(Long[] ids);

    /**
     * 删除RepairRecord信息
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteRepairRecordById(Long id);
}
