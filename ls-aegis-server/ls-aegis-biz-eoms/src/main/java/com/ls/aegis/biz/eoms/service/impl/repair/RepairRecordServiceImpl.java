package com.ls.aegis.biz.eoms.service.impl.repair;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.domain.RepairRecord;
import com.ls.aegis.biz.eoms.mapper.repair.RepairRecordMapper;
import com.ls.aegis.biz.eoms.service.repair.IRepairRecordService;
import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.biz.eoms.utils.EomsPlatformFileHelper;

/**
 * RepairRecord Service实现
 * 
 * @author EOMS Generator
 */
@Service
public class RepairRecordServiceImpl implements IRepairRecordService
{
    @Autowired
    private RepairRecordMapper repairRecordMapper;

    @Autowired
    private EomsPlatformFileHelper platformFileHelper;

    @Autowired
    private com.ls.aegis.biz.eoms.service.repair.IRepairOrderService repairOrderService;
    @Autowired
    private com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper repairOrderMapper;
    @Autowired
    private com.ls.aegis.biz.eoms.service.h5.H5AttachmentService h5AttachmentService;

    @Override
    public RepairRecord selectRepairRecordById(Long id)
    {
        RepairRecord record;
        if (EomsDataScopeHelper.shouldFilter())
        {
            RepairRecord query = new RepairRecord();
            query.setId(id);
            EomsDataScopeHelper.apply(query, false, "r");
            List<RepairRecord> list = repairRecordMapper.selectRepairRecordList(query);
            record = list.isEmpty() ? null : list.get(0);
        }
        else
        {
            record = repairRecordMapper.selectRepairRecordById(id);
        }
        platformFileHelper.fillRecord(record);
        return record;
    }

    @Override
    public List<RepairRecord> selectRepairRecordList(RepairRecord repairRecord)
    {
        EomsDataScopeHelper.apply(repairRecord, false, "r");
        List<RepairRecord> list = repairRecordMapper.selectRepairRecordList(repairRecord);
        platformFileHelper.fillRecords(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRepairRecord(RepairRecord repairRecord)
    {
        // 一单一条：Service 层判重（软删除场景不宜对 repair_order_id 建硬唯一索引）
        if (repairRecord == null || repairRecord.getRepairOrderId() == null)
        {
            throw new ServiceException("工单ID不能为空", EomsConstants.CODE_STATUS_ILLEGAL);
        }
        var order = lockWritableOrder(repairRecord.getRepairOrderId());
        h5AttachmentService.validateRecordPhotos(order, repairRecord.getProcessingPhotos(), null);
        h5AttachmentService.validateRecordPhotos(order, repairRecord.getCompletedPhotos(), null);
        if (!repairRecordMapper.lockActiveRecordIds(order.getId(), order.getTenantId()).isEmpty())
        {
            throw new ServiceException("该工单已提交过维修记录，如需修改请联系管理员删除旧记录后重新提交",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }
        // 删除标志默认 0-存在
        repairRecord.setId(null);
        repairRecord.setTenantId(order.getTenantId()); repairRecord.setDeptId(order.getDeptId());
        repairRecord.setCreateUser(com.ls.aegis.common.context.UserContextHolder.getUserId());
        repairRecord.setDelFlag("0");
        EomsDataScopeHelper.fillOnInsert(repairRecord);
        int changed = repairRecordMapper.insertRepairRecord(repairRecord);
        if (changed != 1) throw new ServiceException("维修记录写入失败");
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRepairRecord(RepairRecord repairRecord)
    {
        if (repairRecord == null || repairRecord.getId() == null) throw new ServiceException("记录ID不能为空");
        RepairRecord visible = selectRepairRecordById(repairRecord.getId());
        if (visible == null) throw new ServiceException("维修记录不存在或无权访问");
        var order = lockWritableOrder(visible.getRepairOrderId());
        RepairRecord current = repairRecordMapper.lockRecord(visible.getId(), order.getTenantId());
        if (current == null || !order.getId().equals(current.getRepairOrderId())) throw new ServiceException("维修记录已变更，请刷新重试");
        if (repairRecord.getRepairOrderId() != null && !order.getId().equals(repairRecord.getRepairOrderId())) throw new ServiceException("维修记录不可迁移工单");
        h5AttachmentService.validateRecordPhotos(order, repairRecord.getProcessingPhotos(), current.getProcessingPhotos());
        h5AttachmentService.validateRecordPhotos(order, repairRecord.getCompletedPhotos(), current.getCompletedPhotos());
        repairRecord.setRepairOrderId(order.getId()); repairRecord.setTenantId(order.getTenantId()); repairRecord.setDeptId(order.getDeptId());
        repairRecord.setDelFlag("0");
        int changed = repairRecordMapper.updateRepairRecord(repairRecord);
        if (changed != 1) throw new ServiceException("维修记录更新失败");
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRepairRecordByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) return 0;
        if (java.util.Arrays.stream(ids).anyMatch(java.util.Objects::isNull)) throw new ServiceException("记录ID不能为空");
        var records = new java.util.ArrayList<RepairRecord>();
        for (Long id : java.util.Arrays.stream(ids).distinct().sorted().toList()) {
            RepairRecord record = selectRepairRecordById(id);
            if (record == null) throw new ServiceException("维修记录不存在或无权访问");
            records.add(record);
        }
        var orders = new java.util.HashMap<Long, com.ls.aegis.biz.eoms.domain.RepairOrder>();
        for (Long orderId : records.stream().map(RepairRecord::getRepairOrderId).distinct().sorted().toList()) orders.put(orderId, lockWritableOrder(orderId));
        for (RepairRecord visible : records) {
            var order = orders.get(visible.getRepairOrderId());
            RepairRecord locked = repairRecordMapper.lockRecord(visible.getId(), order.getTenantId());
            if (locked == null || !order.getId().equals(locked.getRepairOrderId())) throw new ServiceException("维修记录已变更，请刷新重试");
        }
        // Preserve referenced photo evidence; file lifecycle is owned by the platform file service.
        Long[] distinct = records.stream().map(RepairRecord::getId).toArray(Long[]::new);
        int changed = repairRecordMapper.deleteRepairRecordByIds(distinct);
        if (changed != distinct.length) throw new ServiceException("维修记录删除失败");
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRepairRecordById(Long id)
    {
        return deleteRepairRecordByIds(new Long[]{id});
    }

    private com.ls.aegis.biz.eoms.domain.RepairOrder lockWritableOrder(Long id) {
        var visible = repairOrderService.selectRepairOrderById(id);
        if (visible == null) throw new ServiceException("工单不存在或无权访问");
        var current = repairOrderMapper.lockOrder(id);
        if (current == null || !java.util.Objects.equals(visible.getTenantId(), current.getTenantId())
            || !java.util.Objects.equals(visible.getDeptId(), current.getDeptId())
            || !java.util.Objects.equals(visible.getEngineerId(), current.getEngineerId())
            || !java.util.Objects.equals(visible.getCreateUser(), current.getCreateUser())) throw new ServiceException("工单已变更，请刷新重试");
        if (!"PROCESSING".equals(current.getStatus())) throw new ServiceException("仅维修中的工单可维护维修记录");
        return current;
    }

}
