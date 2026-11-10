package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.biz.sign.domain.SignContractOption;
import com.ls.aegis.biz.sign.domain.SignProject;
import com.ls.aegis.biz.sign.mapper.SignProjectMapper;
import com.ls.aegis.biz.sign.service.ISignProjectService;
import com.ls.aegis.biz.sign.utils.SignDataScopeHelper;
import com.ls.aegis.compat.common.core.domain.entity.SysUser;
import com.ls.aegis.compat.system.service.ISysUserService;
import com.ls.aegis.starter.support.docno.DocNoConstants;
import com.ls.aegis.starter.support.docno.DocNoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 签章项目管理服务实现
 */
@Service
public class SignProjectServiceImpl implements ISignProjectService {

    private static final String PROJECT_NO_PREFIX = "XM";
    private static final DateTimeFormatter PROJECT_NO_DATE_FMT =
        DateTimeFormatter.ofPattern(DocNoConstants.DATE_PATTERN);

    @Autowired
    private SignProjectMapper projectMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private DocNoService docNoService;

    @Override
    public List<SignProject> selectProjectList(SignProject project) {
        // 列表查询前先按开始/结束时间刷新，避免仅依赖定时任务导致状态滞后
        refreshExpiredProjects();
        SignDataScopeHelper.apply(project, true, false, "p");
        return projectMapper.selectProjectList(project);
    }

    @Override
    public SignProject selectProjectDetail(Long projectId) {
        refreshExpiredProjects();
        return selectScopedById(projectId, true);
    }

    @Override
    @Transactional
    public int insertProject(SignProject project) {
        LocalDateTime createTime = LocalDateTime.now();
        fillManagerNames(project);

        project.setProjectCode(nextProjectCode());
        project.setCreateBy(UserContextHolder.getUsername());
        project.setCreateTime(createTime);
        project.setDelFlag("0");
        project.setStatus(SignProject.STATUS_PENDING_REVIEW);
        fillContractSnapshots(project, null);
        assertHasContractOrOtherFiles(project);
        SignDataScopeHelper.fillOnInsert(project);

        int rows = projectMapper.insertProject(project);
        syncReverseProjectIds(null, project);

        if (project.getSealIds() != null && !project.getSealIds().isEmpty()) {
            projectMapper.batchInsertProjectSeal(project.getProjectId(), project.getSealIds());
        }

        if (project.getUserIds() != null && project.getUserIds().length > 0) {
            projectMapper.batchInsertProjectUser(project.getProjectId(), project.getUserIds());
        }

        return rows;
    }

    @Override
    @Transactional
    public int updateProject(SignProject project) {
        SignProject previous = selectScopedById(project.getProjectId(), false);
        if (previous == null) {
            throw new ServiceException("项目不存在");
        }
        fillManagerNames(project);
        fillContractSnapshots(project, project.getProjectId());
        assertHasContractOrOtherFiles(project);
        syncReverseProjectIds(previous, project);

        project.setUpdateBy(UserContextHolder.getUsername());
        project.setUpdateTime(LocalDateTime.now());

        if (project.getSealIds() != null) {
            projectMapper.deleteProjectSeal(project.getProjectId());
            if (!project.getSealIds().isEmpty()) {
                projectMapper.batchInsertProjectSeal(project.getProjectId(), project.getSealIds());
            }
        }

        if (project.getUserIds() != null) {
            projectMapper.deleteProjectUser(project.getProjectId());
            if (project.getUserIds().length > 0) {
                projectMapper.batchInsertProjectUser(project.getProjectId(), project.getUserIds());
            }
        }

        return projectMapper.updateProject(project);
    }

    @Override
    @Transactional
    public int deleteProjectByIds(Long[] projectIds) {
        if (projectIds != null) {
            for (Long projectId : projectIds) {
                SignProject previous = selectScopedById(projectId, false);
                if (previous == null) {
                    continue;
                }
                clearIfPresent(SignProject.CONTRACT_SALE, previous.getSaleContractId());
                clearIfPresent(SignProject.CONTRACT_PURCHASE, previous.getPurchaseContractId());
                clearIfPresent(SignProject.CONTRACT_REPAIR, previous.getRepairContractId());
                // 兼容未迁移的旧单指针
                if (previous.getSaleContractId() == null
                    && previous.getPurchaseContractId() == null
                    && previous.getRepairContractId() == null
                    && previous.getContractId() != null
                    && StringUtils.isNotEmpty(previous.getContractType())) {
                    clearIfPresent(previous.getContractType(), previous.getContractId());
                }
            }
        }
        return projectMapper.deleteProjectByIds(projectIds);
    }

    @Override
    public int refreshExpiredProjects() {
        return projectMapper.refreshExpiredProjects();
    }

    @Override
    @Transactional
    public int reviewProject(Long projectId, String status) {
        SignProject project = selectScopedById(projectId, false);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (!SignProject.STATUS_PENDING_REVIEW.equals(project.getStatus())) {
            throw new ServiceException("项目状态不允许审核");
        }
        if (!SignProject.STATUS_NOT_STARTED.equals(status) && !SignProject.STATUS_CANCELLED.equals(status)) {
            throw new ServiceException("审核状态不合法");
        }
        // 通过：按开始/结束时间即时落态（未开始 / 进行中 / 已完成）；驳回：已取消
        if (SignProject.STATUS_NOT_STARTED.equals(status)) {
            project.setStatus(resolveStatusBySchedule(project.getStartDate(), project.getEndDate()));
        } else {
            project.setStatus(status);
        }
        project.setUpdateBy(UserContextHolder.getUsername());
        project.setUpdateTime(LocalDateTime.now());
        return projectMapper.updateProject(project);
    }

    @Override
    public List<SignProject> selectProjectListByUserId(Long userId) {
        refreshExpiredProjects();
        return projectMapper.selectProjectListByUserId(userId);
    }

    /**
     * 审核通过后按项目时间窗计算状态：
     * 未到开始→未开始；已到开始未到结束→进行中；已到结束→已完成。
     */
    private String resolveStatusBySchedule(Date startDate, Date endDate) {
        Date now = new Date();
        if (endDate != null && !endDate.after(now)) {
            return SignProject.STATUS_COMPLETED;
        }
        if (startDate != null && !startDate.after(now)) {
            return SignProject.STATUS_IN_PROGRESS;
        }
        return SignProject.STATUS_NOT_STARTED;
    }

    /**
     * 管理端按 DataScope 取项目；详情带印章集合。
     */
    private SignProject selectScopedById(Long projectId, boolean withSeals) {
        if (projectId == null) {
            return null;
        }
        if (!SignDataScopeHelper.shouldFilter()) {
            return withSeals ? projectMapper.selectProjectDetail(projectId) : projectMapper.selectProjectById(projectId);
        }
        SignProject query = new SignProject();
        query.setProjectId(projectId);
        SignDataScopeHelper.apply(query, true, false, "p");
        List<SignProject> list = projectMapper.selectProjectList(query);
        if (list.isEmpty()) {
            return null;
        }
        return withSeals ? projectMapper.selectProjectDetail(projectId) : list.get(0);
    }

    @Override
    public List<SignContractOption> listContractOptions(String contractType, String keyword, Long excludeProjectId) {
        if (StringUtils.isEmpty(contractType)) {
            List<SignContractOption> all = new ArrayList<>();
            all.addAll(stampType(projectMapper.selectContractOptions(SignProject.CONTRACT_SALE, keyword, excludeProjectId),
                SignProject.CONTRACT_SALE));
            all.addAll(stampType(projectMapper.selectContractOptions(SignProject.CONTRACT_PURCHASE, keyword, excludeProjectId),
                SignProject.CONTRACT_PURCHASE));
            all.addAll(stampType(projectMapper.selectContractOptions(SignProject.CONTRACT_REPAIR, keyword, excludeProjectId),
                SignProject.CONTRACT_REPAIR));
            return all;
        }
        if (!isSupportedContractType(contractType)) {
            throw new ServiceException("合同类型不合法");
        }
        return stampType(projectMapper.selectContractOptions(contractType, keyword, excludeProjectId), contractType);
    }

    private List<SignContractOption> stampType(List<SignContractOption> list, String type) {
        if (list == null || list.isEmpty()) {
            return list == null ? List.of() : list;
        }
        for (SignContractOption opt : list) {
            if (opt != null && StringUtils.isEmpty(opt.getContractType())) {
                opt.setContractType(type);
            }
        }
        return list;
    }

    private void fillManagerNames(SignProject project) {
        if (project.getManagerIds() == null || project.getManagerIds().isEmpty()) {
            project.setManagerNames(null);
            return;
        }
        StringBuilder names = new StringBuilder();
        String[] ids = project.getManagerIds().split(",");
        for (String raw : ids) {
            String id = raw.trim();
            if (id.isEmpty()) {
                continue;
            }
            SysUser user = userService.selectUserById(Long.parseLong(id));
            if (user == null) {
                continue;
            }
            // nickName 为空时 StringBuilder.append(null) 会写成字面量 "null"；回退到 userName
            String displayName = StringUtils.isNotEmpty(user.getNickName())
                ? user.getNickName()
                : user.getUserName();
            if (StringUtils.isEmpty(displayName)) {
                continue;
            }
            if (names.length() > 0) {
                names.append(",");
            }
            names.append(displayName);
        }
        project.setManagerNames(names.length() > 0 ? names.toString() : null);
    }

    /**
     * 项目仅绑定一份合同；填充快照，并写汇总列便于列表展示。
     */
    private void fillContractSnapshots(SignProject project, Long excludeProjectId) {
        // 前端单选：contractType + contractId → 落到对应类型列，并清空其余类型
        migrateLegacySingleBind(project);
        enforceSingleBind(project);

        applyTypedBind(project, SignProject.CONTRACT_SALE, project.getSaleContractId(), excludeProjectId);
        applyTypedBind(project, SignProject.CONTRACT_PURCHASE, project.getPurchaseContractId(), excludeProjectId);
        applyTypedBind(project, SignProject.CONTRACT_REPAIR, project.getRepairContractId(), excludeProjectId);
        refreshSummary(project);
    }

    /** 关联合同与其他文件至少一项有数据 */
    private void assertHasContractOrOtherFiles(SignProject project) {
        boolean hasContract = project.getSaleContractId() != null
            || project.getPurchaseContractId() != null
            || project.getRepairContractId() != null;
        boolean hasOtherFiles = StringUtils.isNotEmpty(project.getOtherFileIds());
        if (!hasContract && !hasOtherFiles) {
            throw new ServiceException("请关联合同或上传其他文件（至少一项）");
        }
    }

    private void migrateLegacySingleBind(SignProject project) {
        if (project.getContractId() == null || StringUtils.isEmpty(project.getContractType())) {
            return;
        }
        // 单选提交时以汇总字段为准，清空三类后再写入目标类型
        project.setSaleContractId(null);
        project.setPurchaseContractId(null);
        project.setRepairContractId(null);
        if (SignProject.CONTRACT_SALE.equals(project.getContractType())) {
            project.setSaleContractId(project.getContractId());
        } else if (SignProject.CONTRACT_PURCHASE.equals(project.getContractType())) {
            project.setPurchaseContractId(project.getContractId());
        } else if (SignProject.CONTRACT_REPAIR.equals(project.getContractType())) {
            project.setRepairContractId(project.getContractId());
        } else {
            throw new ServiceException("合同类型不合法");
        }
    }

    /** 业务约定：项目仅绑定一份合同 */
    private void enforceSingleBind(SignProject project) {
        int bound = 0;
        if (project.getSaleContractId() != null) {
            bound++;
        }
        if (project.getPurchaseContractId() != null) {
            bound++;
        }
        if (project.getRepairContractId() != null) {
            bound++;
        }
        if (bound > 1) {
            throw new ServiceException("项目仅能绑定一份合同");
        }
    }

    private void applyTypedBind(SignProject project, String contractType, Long contractId, Long excludeProjectId) {
        if (contractId == null) {
            clearTypedSnapshot(project, contractType);
            return;
        }
        if (projectMapper.countBoundContract(contractType, contractId, excludeProjectId) > 0) {
            throw new ServiceException(typeLabel(contractType) + "已绑定其他项目");
        }
        SignContractOption option = projectMapper.selectContractById(contractType, contractId);
        if (option == null || option.getId() == null) {
            throw new ServiceException(typeLabel(contractType) + "不存在或已删除");
        }
        setTypedSnapshot(project, contractType, contractId, option);
    }

    private void clearTypedSnapshot(SignProject project, String contractType) {
        if (SignProject.CONTRACT_SALE.equals(contractType)) {
            project.setSaleContractId(null);
            project.setSaleContractNo(null);
            project.setSaleContractName(null);
            project.setSalePartnerName(null);
        } else if (SignProject.CONTRACT_PURCHASE.equals(contractType)) {
            project.setPurchaseContractId(null);
            project.setPurchaseContractNo(null);
            project.setPurchaseContractName(null);
            project.setPurchasePartnerName(null);
        } else if (SignProject.CONTRACT_REPAIR.equals(contractType)) {
            project.setRepairContractId(null);
            project.setRepairContractNo(null);
            project.setRepairContractName(null);
            project.setRepairPartnerName(null);
        }
    }

    private void setTypedSnapshot(SignProject project, String contractType, Long contractId, SignContractOption option) {
        if (SignProject.CONTRACT_SALE.equals(contractType)) {
            project.setSaleContractId(contractId);
            project.setSaleContractNo(option.getContractNo());
            project.setSaleContractName(option.getContractName());
            project.setSalePartnerName(option.getPartnerName());
        } else if (SignProject.CONTRACT_PURCHASE.equals(contractType)) {
            project.setPurchaseContractId(contractId);
            project.setPurchaseContractNo(option.getContractNo());
            project.setPurchaseContractName(option.getContractName());
            project.setPurchasePartnerName(option.getPartnerName());
        } else if (SignProject.CONTRACT_REPAIR.equals(contractType)) {
            project.setRepairContractId(contractId);
            project.setRepairContractNo(option.getContractNo());
            project.setRepairContractName(option.getContractName());
            project.setRepairPartnerName(option.getPartnerName());
        }
    }

    private void refreshSummary(SignProject project) {
        int bound = 0;
        String onlyType = null;
        Long onlyId = null;
        String onlyNo = null;
        String onlyName = null;
        String onlyPartner = null;
        if (project.getSaleContractId() != null) {
            bound++;
            onlyType = SignProject.CONTRACT_SALE;
            onlyId = project.getSaleContractId();
            onlyNo = project.getSaleContractNo();
            onlyName = project.getSaleContractName();
            onlyPartner = project.getSalePartnerName();
        }
        if (project.getPurchaseContractId() != null) {
            bound++;
            onlyType = SignProject.CONTRACT_PURCHASE;
            onlyId = project.getPurchaseContractId();
            onlyNo = project.getPurchaseContractNo();
            onlyName = project.getPurchaseContractName();
            onlyPartner = project.getPurchasePartnerName();
        }
        if (project.getRepairContractId() != null) {
            bound++;
            onlyType = SignProject.CONTRACT_REPAIR;
            onlyId = project.getRepairContractId();
            onlyNo = project.getRepairContractNo();
            onlyName = project.getRepairContractName();
            onlyPartner = project.getRepairPartnerName();
        }
        // 业务约定：项目仅绑定一份合同
        if (bound == 1) {
            project.setContractType(onlyType);
            project.setContractId(onlyId);
            project.setContractNo(onlyNo);
            project.setContractName(onlyName);
            project.setPartnerName(onlyPartner);
        } else if (bound == 0) {
            project.setContractType(null);
            project.setContractId(null);
            project.setContractNo(null);
            project.setContractName(null);
            project.setPartnerName(null);
        } else {
            // 历史多绑数据：汇总展示
            List<String> nos = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<String> partners = new ArrayList<>();
            if (StringUtils.isNotEmpty(project.getSaleContractNo())) {
                nos.add("销:" + project.getSaleContractNo());
            }
            if (StringUtils.isNotEmpty(project.getSaleContractName())) {
                names.add("销:" + project.getSaleContractName());
            }
            if (StringUtils.isNotEmpty(project.getSalePartnerName())) {
                partners.add("销:" + project.getSalePartnerName());
            }
            if (StringUtils.isNotEmpty(project.getPurchaseContractNo())) {
                nos.add("采:" + project.getPurchaseContractNo());
            }
            if (StringUtils.isNotEmpty(project.getPurchaseContractName())) {
                names.add("采:" + project.getPurchaseContractName());
            }
            if (StringUtils.isNotEmpty(project.getPurchasePartnerName())) {
                partners.add("采:" + project.getPurchasePartnerName());
            }
            if (StringUtils.isNotEmpty(project.getRepairContractNo())) {
                nos.add("维:" + project.getRepairContractNo());
            }
            if (StringUtils.isNotEmpty(project.getRepairContractName())) {
                names.add("维:" + project.getRepairContractName());
            }
            if (StringUtils.isNotEmpty(project.getRepairPartnerName())) {
                partners.add("维:" + project.getRepairPartnerName());
            }
            project.setContractType(null);
            project.setContractId(null);
            project.setContractNo(nos.isEmpty() ? null : String.join(" / ", nos));
            project.setContractName(names.isEmpty() ? null : String.join(" / ", names));
            project.setPartnerName(partners.isEmpty() ? null : String.join(" / ", partners));
        }
    }

    private void syncReverseProjectIds(SignProject previous, SignProject current) {
        syncOne(SignProject.CONTRACT_SALE,
            previous == null ? null : previous.getSaleContractId(),
            current.getSaleContractId(),
            current.getProjectId());
        syncOne(SignProject.CONTRACT_PURCHASE,
            previous == null ? null : previous.getPurchaseContractId(),
            current.getPurchaseContractId(),
            current.getProjectId());
        syncOne(SignProject.CONTRACT_REPAIR,
            previous == null ? null : previous.getRepairContractId(),
            current.getRepairContractId(),
            current.getProjectId());
    }

    private void syncOne(String contractType, Long previousId, Long currentId, Long projectId) {
        if (Objects.equals(previousId, currentId)) {
            if (currentId != null && projectId != null) {
                projectMapper.setContractProject(contractType, currentId, projectId);
            }
            return;
        }
        if (previousId != null) {
            projectMapper.clearContractProject(contractType, previousId);
        }
        if (currentId != null && projectId != null) {
            projectMapper.setContractProject(contractType, currentId, projectId);
        }
    }

    private void clearIfPresent(String contractType, Long contractId) {
        if (contractId != null && StringUtils.isNotEmpty(contractType)) {
            projectMapper.clearContractProject(contractType, contractId);
        }
    }

    private String nextProjectCode() {
        String day = LocalDate.now().format(PROJECT_NO_DATE_FMT);
        String likePattern = PROJECT_NO_PREFIX + day + "%";
        int substringPos = PROJECT_NO_PREFIX.length() + day.length() + 1;
        Long dbSeed = projectMapper.selectMaxDailySeq(likePattern, substringPos);
        return docNoService.nextSignDaily(PROJECT_NO_PREFIX, dbSeed);
    }

    private String typeLabel(String contractType) {
        if (SignProject.CONTRACT_SALE.equals(contractType)) {
            return "销售合同";
        }
        if (SignProject.CONTRACT_PURCHASE.equals(contractType)) {
            return "采购合同";
        }
        if (SignProject.CONTRACT_REPAIR.equals(contractType)) {
            return "维修合同";
        }
        return "合同";
    }

    private boolean isSupportedContractType(String contractType) {
        return SignProject.CONTRACT_SALE.equals(contractType)
            || SignProject.CONTRACT_PURCHASE.equals(contractType)
            || SignProject.CONTRACT_REPAIR.equals(contractType);
    }
}
