package com.ls.aegis.biz.eoms.service.impl.repair;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.SecurityUtils;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.domain.RepairApply;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.mapper.repair.RepairApplyMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper;
import com.ls.aegis.biz.eoms.service.repair.IRepairApplyService;
import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;
import com.ls.aegis.biz.eoms.utils.EomsPlatformFileHelper;

/**
 * RepairApply Service实现
 *
 * @author EOMS Generator
 */
@Service
public class RepairApplyServiceImpl implements IRepairApplyService
{
    @Autowired
    private RepairApplyMapper repairApplyMapper;

    @Autowired
    private RepairOrderMapper repairOrderMapper;


    @Autowired
    private EomsNoGenerator eomsNoGenerator;

    @Autowired
    private EomsPlatformFileHelper platformFileHelper;

    @Override
    public RepairApply selectRepairApplyById(Long id)
    {
        RepairApply apply;
        if (EomsDataScopeHelper.shouldFilter())
        {
            RepairApply query = new RepairApply();
            query.setId(id);
            EomsDataScopeHelper.apply(query, false, "a");
            List<RepairApply> list = repairApplyMapper.selectRepairApplyList(query);
            apply = list.isEmpty() ? null : list.get(0);
        }
        else
        {
            apply = repairApplyMapper.selectRepairApplyById(id);
        }
        platformFileHelper.fillApply(apply);
        return apply;
    }

    @Override
    public List<RepairApply> selectRepairApplyList(RepairApply repairApply)
    {
        EomsDataScopeHelper.apply(repairApply, false, "a");
        List<RepairApply> list = repairApplyMapper.selectRepairApplyList(repairApply);
        platformFileHelper.fillApplies(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRepairApply(RepairApply repairApply)
    {
        if (repairApply.getReportAddress() != null && repairApply.getReportAddress().length() > 500)
            throw new ServiceException("报修地址最多500字");
        // 自动生成报修编号
        if (StringUtils.isEmpty(repairApply.getRepairNo()))
        {
            repairApply.setRepairNo(eomsNoGenerator.generateRepairNo());
        }
        // 默认状态：待处理
        if (StringUtils.isEmpty(repairApply.getStatus()))
        {
            repairApply.setStatus(EomsConstants.APPLY_STATUS_WAIT);
        }
        // 默认报修时间
        if (repairApply.getApplyTime() == null)
        {
            repairApply.setApplyTime(new Date());
        }
        // 默认删除标志
        if (StringUtils.isEmpty(repairApply.getDelFlag()))
        {
            repairApply.setDelFlag("0");
        }
        EomsDataScopeHelper.fillOnInsert(repairApply);
        return repairApplyMapper.insertRepairApply(repairApply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRepairApply(RepairApply repairApply)
    {
        if (repairApply != null && repairApply.getReportAddress() != null && repairApply.getReportAddress().length() > 500)
            throw new ServiceException("报修地址最多500字");
        if (repairApply == null || selectRepairApplyById(repairApply.getId()) == null)
            throw new ServiceException("报修申请不存在或无权访问");
        RepairApply current = repairApplyMapper.lockApply(repairApply.getId());
        if (current == null || "ORDER".equals(current.getStatus()))
            throw new ServiceException("已转工单的申请不可直接编辑，请先处理关联工单");
        if (repairApply.getStatus() != null && !repairApply.getStatus().equals(current.getStatus()))
            throw new ServiceException("状态变更请使用专用业务接口");
        repairApply.setStatus(current.getStatus());
        repairApply.setDelFlag("0");
        return repairApplyMapper.updateRepairApply(repairApply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRepairApplyByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) return 0;
        for (Long applyId : java.util.Arrays.stream(ids).distinct().sorted().toList())
        {
            RepairApply visible = selectRepairApplyById(applyId);
            RepairApply existing = visible == null ? null : repairApplyMapper.lockApply(applyId);
            if (existing == null) throw new ServiceException("报修申请不存在或无权访问");
            // ORDER with only historical deleted orders is safe to archive; retain original apply_id.
            if (repairOrderMapper.countActiveByApply(applyId) != 0)
                throw new ServiceException("报修申请仍有活动工单，不可删除", EomsConstants.CODE_STATUS_ILLEGAL);
        }
        // 删除关联的图片文件
        for (Long applyId : ids)
        {
            RepairApply existing = repairApplyMapper.selectRepairApplyById(applyId);
            if (existing != null && existing.getPhotos() != null && !existing.getPhotos().isEmpty())
            {
                deletePhotoFiles(existing.getPhotos());
            }
        }
        return repairApplyMapper.deleteRepairApplyByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRepairApplyById(Long id)
    {
        return deleteRepairApplyByIds(new Long[]{ id });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmApply(Long id, String remark)
    {
        RepairApply before = mustExist(id);
        String oldStatus = before.getStatus();
        // 状态校验：仅待处理可确认
        if (!EomsConstants.APPLY_STATUS_WAIT.equals(oldStatus))
        {
            throw new ServiceException("当前状态[" + oldStatus + "]不可确认，仅待处理状态可确认",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }
        // 构造变更后对象
        RepairApply after = new RepairApply();
        after.setId(id);
        after.setStatus(EomsConstants.APPLY_STATUS_CONFIRM);
        after.setUpdateBy(getCurrentUsername());
        after.setRemark(remark);
        int rows = repairApplyMapper.transitionStatus(after, oldStatus);
        if (rows != 1) throw new ServiceException("报修状态已变更，请刷新后重试");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelApply(Long id, String remark)
    {
        RepairApply before = mustExist(id);
        String oldStatus = before.getStatus();
        // 状态校验：仅待处理可直接取消
        if (!EomsConstants.APPLY_STATUS_WAIT.equals(oldStatus))
        {
            throw new ServiceException("当前状态[" + oldStatus + "]不可取消，仅待处理状态可取消",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }
        RepairApply after = new RepairApply();
        after.setId(id);
        after.setStatus(EomsConstants.APPLY_STATUS_CANCEL);
        after.setUpdateBy(getCurrentUsername());
        after.setRemark(remark);
        int rows = repairApplyMapper.transitionStatus(after, oldStatus);
        if (rows != 1) throw new ServiceException("报修状态已变更，请刷新后重试");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeApply(Long id, String remark)
    {
        if (!org.springframework.util.StringUtils.hasText(remark))
        {
            throw new ServiceException("关闭原因不能为空");
        }
        String reason = remark.trim();
        if (reason.length() > 500)
        {
            throw new ServiceException("关闭原因长度不能超过500个字符");
        }
        RepairApply before = mustExist(id);
        String oldStatus = before.getStatus();
        // 状态校验：仅已确认可关闭（已转工单的报修由工单流程驱动，不允许此处关闭）
        if (!EomsConstants.APPLY_STATUS_CONFIRM.equals(oldStatus))
        {
            throw new ServiceException("当前状态[" + oldStatus + "]不可关闭，仅已确认状态可关闭",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }
        RepairApply after = new RepairApply();
        after.setId(id);
        after.setStatus(EomsConstants.APPLY_STATUS_CANCEL);
        after.setUpdateBy(getCurrentUsername());
        after.setRemark(reason);
        int rows = repairApplyMapper.transitionStatus(after, oldStatus);
        if (rows != 1) throw new ServiceException("报修状态已变更，请刷新后重试");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long toOrder(Long applyId, String remark)
    {
        RepairApply visible = mustExist(applyId);
        RepairApply apply = repairApplyMapper.lockApply(visible.getId());
        if (apply == null) throw new ServiceException("申请已变更");
        String oldStatus = apply.getStatus();
        // 状态校验：仅已确认可转工单
        if (!EomsConstants.APPLY_STATUS_CONFIRM.equals(oldStatus))
        {
            throw new ServiceException("当前状态[" + oldStatus + "]不可转工单，仅已确认状态可转工单",
                    EomsConstants.CODE_STATUS_ILLEGAL);
        }

        // 创建工单
        RepairOrder order = new RepairOrder();
        order.setOrderNo(eomsNoGenerator.generateOrderNo());
        order.setApplyId(applyId);
        order.setCustomerId(apply.getCustomerId());
        order.setStatus(EomsConstants.ORDER_STATUS_WAIT_ACCEPT);
        order.setCreateBy(getCurrentUsername());
        order.setDelFlag("0");
        // 现场照片来源于报修申请
        order.setPhotos(apply.getPhotos());
        if (StringUtils.isNotEmpty(remark))
        {
            order.setRemark(remark);
        }
        // 转工单继承申请部门；无则按当前用户填 DataScope 字段
        if (apply.getDeptId() != null && apply.getDeptId() > 0)
        {
            order.setDeptId(apply.getDeptId());
        }
        if (apply.getTenantId() != null)
        {
            order.setTenantId(apply.getTenantId());
        }
        EomsDataScopeHelper.fillOnInsert(order);
        repairOrderMapper.insertRepairOrder(order);

        // 更新报修申请状态为已转工单
        RepairApply after = new RepairApply();
        after.setId(applyId);
        after.setStatus(EomsConstants.APPLY_STATUS_ORDER);
        after.setUpdateBy(getCurrentUsername());
        if (StringUtils.isNotEmpty(remark))
        {
            after.setRemark(remark);
        }
        repairApplyMapper.updateRepairApply(after);
        return order.getId();
    }

    /**
     * 校验报修申请存在并返回（受 DataScope 约束，防越权操作）
     */
    private RepairApply mustExist(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("报修申请ID不能为空", EomsConstants.CODE_APPLY_NOT_EXISTS);
        }
        RepairApply apply = selectRepairApplyById(id);
        if (apply == null)
        {
            throw new ServiceException("报修申请不存在", EomsConstants.CODE_APPLY_NOT_EXISTS);
        }
        return apply;
    }

    /**
     * 获取当前操作用户名（兼容未登录场景）
     */
    private String getCurrentUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return null;
        }
    }


    /**
     * 删除 photos 字段关联的物理文件（逗号分隔；纯数字 fileId 不删本地盘）
     */
    private void deletePhotoFiles(String photos)
    {
        platformFileHelper.deleteLocalIfPath(photos);
    }
}
