package com.ls.aegis.biz.eoms.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import com.ls.aegis.biz.eoms.model.query.H5OnlineTokenQuery;
import com.ls.aegis.biz.eoms.model.resp.H5OnlineTokenResp;
import com.ls.aegis.biz.eoms.service.h5.H5TokenService;

/**
 * H5 客户授权管理（后台）
 * 在线令牌查询与强退；配置项见系统配置「H5 客户授权」页签（sys_option/EOMS）。
 */
@RestController
@RequestMapping("/eoms/admin/h5/auth")
public class H5AuthAdminController
{
    @Autowired
    private H5TokenService h5TokenService;

    public record InvitationReq(@jakarta.validation.constraints.NotNull Long customerId, String accountRole, Long resetAccountId) {}
    public record MemberUpdateReq(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=50) String displayName,
        @jakarta.validation.constraints.NotBlank String accountRole, @jakarta.validation.constraints.NotNull Integer status) {}

    /** The response contains a one-time bearer secret: never use request/response audit logging. */
    @Log(ignore = true)
    @SaCheckPermission("psi:unit:update")
    @org.springframework.web.bind.annotation.PostMapping("/invitations")
    public R<H5TokenService.Invitation> invitation(@Valid @org.springframework.web.bind.annotation.RequestBody InvitationReq req) {
        return R.ok(req.accountRole() == null && req.resetAccountId() == null ? h5TokenService.createInvitation(req.customerId()) :
            h5TokenService.createMemberInvitation(req.customerId(), req.accountRole() == null ? "MEMBER" : req.accountRole(), req.resetAccountId()));
    }

    @SaCheckPermission("psi:unit:update")
    @GetMapping("/customers/{customerId}/members")
    public R<java.util.List<com.ls.aegis.biz.eoms.domain.H5CustomerCredential>> members(@PathVariable Long customerId) {
        return R.ok(h5TokenService.listMembers(customerId));
    }

    @SaCheckPermission("psi:unit:update")
    @org.springframework.web.bind.annotation.PutMapping("/customers/{customerId}/members/{accountId}")
    public R<Void> updateMember(@PathVariable Long customerId, @PathVariable Long accountId, @Valid @org.springframework.web.bind.annotation.RequestBody MemberUpdateReq req) {
        h5TokenService.updateMember(customerId, accountId, req.displayName(), req.accountRole(), req.status()); return R.ok();
    }

    /**
     * 分页查询在线 H5 客户令牌
     */
    @SaCheckPermission("system:h5Auth:list")
    @GetMapping("/online")
    public R<PageResp<H5OnlineTokenResp>> pageOnline(@Valid H5OnlineTokenQuery query, @Valid PageQuery pageQuery)
    {
        return R.ok(h5TokenService.pageOnline(query, pageQuery));
    }

    /**
     * 强退 H5 客户令牌
     */
    @SaCheckPermission("system:h5Auth:kickout")
    @Log(module = "H5客户授权-强退")
    @DeleteMapping("/online/{sessionId}")
    public R<Void> kickout(@PathVariable String sessionId)
    {
        h5TokenService.kickoutSession(sessionId);
        return R.ok();
    }
}
