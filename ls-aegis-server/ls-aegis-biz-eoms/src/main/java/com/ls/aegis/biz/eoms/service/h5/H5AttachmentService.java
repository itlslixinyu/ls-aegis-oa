package com.ls.aegis.biz.eoms.service.h5;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.ls.aegis.biz.eoms.domain.H5CustomerInfo;
import com.ls.aegis.biz.eoms.domain.RepairApply;
import com.ls.aegis.biz.eoms.mapper.file.H5AttachmentMapper;
import com.ls.aegis.biz.eoms.mapper.file.EomsFileAccessMapper;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.file.service.FileService;
import com.ls.aegis.compat.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashSet;
import java.util.Date;

/** Server metadata binds an anonymous platform upload to its authenticated H5 customer. */
@Service
@RequiredArgsConstructor
public class H5AttachmentService {
    static final String OWNER = "itlsEomsH5UploadOwner";
    private final FileService fileService;
    private final H5AttachmentMapper attachmentMapper;
    private final EomsFileAccessMapper accessMapper;

    private Long activeDept(H5CustomerInfo customer) {
        if (customer == null || customer.getTenantId() == null || customer.getCustomerId() == null || customer.getAccountId() == null) throw new ServiceException("客户会话无效");
        Long dept = attachmentMapper.selectActiveCustomerDept(customer.getTenantId(), customer.getCustomerId());
        if (dept == null) throw new ServiceException("客户不存在或已停用");
        return dept;
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindNewUpload(H5CustomerInfo customer, Long fileId) {
        activeDept(customer);
        FileDO file = fileService.getById(fileId);
        if (file == null || !"/eoms/repair".equals(file.getParentPath())) throw new ServiceException("上传文件不存在");
        JSONObject metadata = metadata(file);
        if (metadata.containsKey(OWNER)) throw new ServiceException("上传归属已绑定，不可覆盖");
        metadata.set(OWNER, new JSONObject().set("tenantId", customer.getTenantId().toString()).set("customerId", customer.getCustomerId().toString()).set("accountId", customer.getAccountId().toString()));
        if (attachmentMapper.bindUpload(fileId, file.getMetadata(), metadata.toString()) != 1) throw new ServiceException("上传归属写入失败，请重新上传");
    }

    private JSONObject metadata(FileDO file) {
        String raw = file.getMetadata();
        if (raw == null || raw.isBlank() || "null".equals(raw)) return new JSONObject();
        try { return JSONUtil.parseObj(raw); }
        catch (RuntimeException e) { throw new ServiceException("文件归属信息无效"); }
    }

    public boolean canAccess(H5CustomerInfo customer, FileDO file) {
        activeDept(customer);
        if (file == null || file.getId() == null || (file.getDeleted() != null && file.getDeleted() != 0L)) return false;
        JSONObject owner = metadata(file).getJSONObject(OWNER);
        if (owner != null) {
            if (!customer.getTenantId().toString().equals(owner.getStr("tenantId")) || !customer.getCustomerId().toString().equals(owner.getStr("customerId"))) return false;
            if (customer.isCompanyAdmin() || customer.getAccountId().toString().equals(owner.getStr("accountId"))) return true;
        }
        if (customer.isCompanyAdmin()) return accessMapper.countByOwnerAndFileId(customer.getTenantId(), customer.getCustomerId(), file.getId()) > 0;
        return accessMapper.countByMemberAndFileId(customer.getTenantId(), customer.getCustomerId(), customer.getAccountId(), file.getId()) > 0;
    }

    public RepairApply prepareNewApply(H5CustomerInfo customer, RepairApply supplied) {
        Long dept = activeDept(customer);
        if (supplied == null) throw new ServiceException("报修申请不能为空");
        String name = supplied.getContactName() == null ? "" : supplied.getContactName().trim();
        String phone = supplied.getContactPhone() == null ? "" : supplied.getContactPhone().trim();
        String faultType = supplied.getFaultType() == null ? "" : supplied.getFaultType().trim();
        String description = supplied.getFaultDesc() == null ? "" : supplied.getFaultDesc().trim();
        String urgent = supplied.getUrgentLevel() == null ? "" : supplied.getUrgentLevel().trim();
        if (name.isEmpty() || name.length() > 50) throw new ServiceException("联系人姓名不能为空且不超过50字");
        if (!phone.matches("^1[3-9]\\d{9}$")) throw new ServiceException("请输入正确的手机号码");
        // Values come from the dynamic business dictionary; only persisted field bounds are enforced.
        if (faultType.isEmpty() || faultType.length() > 32) throw new ServiceException("故障类型不能为空且不超过32字");
        if (description.length() < 5 || description.length() > 500) throw new ServiceException("故障描述须为5至500字");
        if (urgent.isEmpty()) urgent = "NORMAL";
        if (!java.util.Set.of("NORMAL", "URGENT", "VERY_URGENT").contains(urgent)) throw new ServiceException("紧急程度无效");
        if (supplied.getPhotos() == null || supplied.getPhotos().isBlank()) throw new ServiceException("请至少上传1张故障现场照片");
        if (supplied.getPhotos().split(",", -1).length > 9) throw new ServiceException("故障现场照片最多9张");
        var ids = new LinkedHashSet<String>();
        if (supplied.getPhotos() != null && !supplied.getPhotos().isBlank()) {
            if (supplied.getPhotos().length() > 1000) throw new ServiceException("照片引用过长");
            for (String token : supplied.getPhotos().split(",", -1)) {
                String id = token.trim();
                if (!id.matches("[1-9][0-9]{0,18}")) throw new ServiceException("照片必须为已授权上传文件ID");
                Long fileId;
                try { fileId = Long.valueOf(id); } catch (NumberFormatException e) { throw new ServiceException("照片ID无效"); }
                if (!canAccess(customer, fileService.getById(fileId))) throw new ServiceException("照片不存在或无权引用");
                ids.add(fileId.toString());
            }
        }
        var clean = new RepairApply();
        clean.setCustomerId(customer.getCustomerId()); clean.setTenantId(customer.getTenantId()); clean.setDeptId(dept); clean.setCreateUser(0L);
        clean.setReporterAccountId(customer.getAccountId()); clean.setReporterName(customer.getDisplayName());
        clean.setContactName(name); clean.setContactPhone(phone);
        String address = supplied.getReportAddress() == null ? "" : supplied.getReportAddress().trim();
        if (address.length() > 500) throw new ServiceException("报修地址最多500字");
        clean.setReportAddress(address);

        clean.setFaultType(faultType); clean.setFaultDesc(description); clean.setUrgentLevel(urgent);
        clean.setPhotos(String.join(",", ids)); clean.setRemark(supplied.getRemark());
        clean.setStatus("WAIT"); clean.setDelFlag("0"); clean.setSource("H5"); clean.setCreateBy(customer.getLoginName()); clean.setApplyTime(new Date());
        return clean;
    }

    /** A record cannot turn an arbitrary known file ID into a customer-visible attachment. */
    public void validateRecordPhotos(com.ls.aegis.biz.eoms.domain.RepairOrder order, String supplied, String unchanged) {
        if (supplied == null || supplied.isBlank() || supplied.equals(unchanged)) return;
        if (supplied.length() > 4000) throw new ServiceException("照片引用过长");
        for (String token : supplied.split(",", -1)) {
            if (!token.trim().matches("[1-9][0-9]{0,18}")) throw new ServiceException("照片必须为平台文件ID");
            Long id;
            try { id = Long.valueOf(token.trim()); } catch (NumberFormatException ex) { throw new ServiceException("照片ID无效"); }
            FileDO file = fileService.getById(id);
            if (file == null || (file.getDeleted() != null && file.getDeleted() != 0)) throw new ServiceException("照片不存在");
            JSONObject owner = metadata(file).getJSONObject(OWNER);
            if (owner != null && order.getTenantId().toString().equals(owner.getStr("tenantId"))
                && order.getCustomerId().toString().equals(owner.getStr("customerId"))) continue;
            Long userId = com.ls.aegis.common.context.UserContextHolder.getUserId();
            if (userId != null && userId.equals(file.getCreateUser())) continue;
            if (com.ls.aegis.common.context.UserContextHolder.isSuperAdmin()) continue;
            throw new ServiceException("照片不属于当前工单或上传人");
        }
    }
}
