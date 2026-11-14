package com.ls.aegis.biz.eoms.access;

import cn.hutool.core.util.StrUtil;
import com.ls.aegis.biz.eoms.mapper.file.EomsFileAccessMapper;
import com.ls.aegis.biz.file.access.FileAccessChecker;
import com.ls.aegis.biz.file.access.FileAccessContext;
import com.ls.aegis.biz.file.model.entity.FileDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ls.aegis.biz.eoms.model.query.EomsFileQuery;
import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.common.context.UserContextHolder;
import cn.dev33.satoken.stp.StpUtil;

/**
 * EOMS 业务文件访问校验：H5 客户仅能访问本客户业务附件；平台用户可访问 EOMS 引用的文件。
 */
@Component
public class EomsFileAccessChecker implements FileAccessChecker {

    @Autowired
    private EomsFileAccessMapper eomsFileAccessMapper;

    @Override
    public boolean canAccess(FileDO file, FileAccessContext context) {
        if (file == null || file.getId() == null || context == null || (file.getDeleted() != null && file.getDeleted() != 0)) {
            return false;
        }
        Long fileId = file.getId();
        if (StrUtil.isNotBlank(context.getH5CustomerNo())) {
            if (context.getH5TenantId() == null || context.getH5CustomerId() == null || context.getH5AccountId() == null) return false;
            return context.isH5CompanyAdmin() ? eomsFileAccessMapper.countByOwnerAndFileId(context.getH5TenantId(), context.getH5CustomerId(), fileId) > 0 :
                eomsFileAccessMapper.countByMemberAndFileId(context.getH5TenantId(),context.getH5CustomerId(),context.getH5AccountId(),fileId) > 0;
        }
        if (context.isPlatformLogin()) {
            try {
                if (!StpUtil.isLogin()) return false;
                var user = UserContextHolder.getContext();
                if (user == null || !java.util.Objects.equals(user.getId(), context.getUserId())) return false;
                Long tenant = top.continew.starter.extension.tenant.context.TenantContextHolder.isTenantEnabled()
                    ? user.getTenantId() : cn.hutool.extra.spring.SpringUtil.getBean(com.ls.aegis.common.config.TenantExtensionProperties.class).getDefaultTenantId();
                if (tenant == null) return false;
                if (StpUtil.hasPermission("eoms:repair:query") && eomsFileAccessMapper.countVisibleApplyFile(query(tenant,fileId,"a",false)) > 0) return true;
                if (StpUtil.hasPermission("eoms:order:query") && eomsFileAccessMapper.countVisibleOrderFile(query(tenant,fileId,"o",true)) > 0) return true;
                if (StpUtil.hasPermission("eoms:record:query") && eomsFileAccessMapper.countVisibleRecordFile(query(tenant,fileId,"o",true)) > 0) return true;
                return StpUtil.hasPermission("eoms:contract:query") && eomsFileAccessMapper.countVisibleContractFile(query(tenant,fileId,"ct",false)) > 0;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    private EomsFileQuery query(Long tenant, Long file, String alias, boolean engineer) {
        var query = new EomsFileQuery();query.setTenantId(tenant);query.setFileId(file);
        EomsDataScopeHelper.apply(query,engineer,alias);
        return query;
    }
}
