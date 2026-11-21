package com.ls.aegis.biz.psi.access;

import com.ls.aegis.biz.file.access.FileAccessChecker;
import com.ls.aegis.biz.file.access.FileAccessContext;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.psi.mapper.file.PsiFileAccessMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.dev33.satoken.stp.StpUtil;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import java.util.Map;
import java.util.Objects;

/**
 * PSI 业务文件访问校验：平台用户仅能访问进销存业务引用的附件。
 */
@Component
public class PsiFileAccessChecker implements FileAccessChecker {

    @Autowired
    private PsiFileAccessMapper psiFileAccessMapper;

    @Override
    public boolean canAccess(FileDO file, FileAccessContext context) {
        if (file == null || file.getId() == null || context == null || !context.isPlatformLogin()) {
            return false;
        }
        var actor = UserContextHolder.getContext();
        if (actor == null || actor.getId() == null || !Objects.equals(context.getUserId(), actor.getId())) return false;
        if (!actor.isSuperAdmin() && (actor.getDeptId() == null || actor.getRoles() == null || actor.getRoles().isEmpty()
            || actor.getRoles().stream().anyMatch(role -> role == null || role.getId() == null || role.getDataScope() == null))) return false;
        try {
            StpUtil.checkLogin();
            if (!Objects.equals(String.valueOf(actor.getId()), StpUtil.getLoginIdAsString())) return false;
            for (var entry : Map.of("contractSale", "psi:contractSale:list", "contractPurchase", "psi:contractPurchase:list",
                "invoiceIn", "psi:invoice-in:list", "invoiceOut", "psi:invoice-out:list", "printTemplate", "psi:printTemplate:get").entrySet()) {
                if ((actor.isSuperAdmin() || StpUtil.hasPermission(entry.getValue()))
                    && psiFileAccessMapper.countAccessible(file.getId(), TenantFillHelper.currentTenantId(), entry.getKey()) > 0) return true;
            }
        } catch (cn.dev33.satoken.exception.NotLoginException ignored) {
            return false;
        }
        return false;
    }
}
