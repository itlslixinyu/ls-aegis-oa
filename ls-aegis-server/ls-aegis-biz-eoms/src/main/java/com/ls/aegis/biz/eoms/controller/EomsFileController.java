package com.ls.aegis.biz.eoms.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.ls.aegis.biz.eoms.service.h5.H5TokenService;
import com.ls.aegis.biz.eoms.utils.EomsPlatformFileHelper;
import com.ls.aegis.biz.file.access.FileAccessContext;
import com.ls.aegis.biz.file.access.FileAccessService;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.file.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EOMS 业务文件预览（带鉴权）。
 * <p>
 * H5 客户：请求头 {@code h5-token}；PC / 工程师：{@code Authorization: Bearer}（Sa-Token）。
 * 供前端 axios 拉 blob 后 {@code URL.createObjectURL} 展示，避免 img 直链无法带凭证。
 * </p>
 */
@SaIgnore
@RestController
@RequestMapping("/eoms/file")
public class EomsFileController {

    private static final Logger log = LoggerFactory.getLogger(EomsFileController.class);

    private static final String HEADER_H5_TOKEN = "h5-token";

    @Autowired
    private EomsPlatformFileHelper platformFileHelper;

    @Autowired
    private H5TokenService h5TokenService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileAccessService fileAccessService;

    @Autowired
    private com.ls.aegis.biz.eoms.service.h5.H5AttachmentService h5AttachmentService;

    /**
     * 预览平台 sys_file 文件（二进制流）。
     */
    @GetMapping("/preview/{id}")
    public void preview(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        String customerToken = request.getHeader(HEADER_H5_TOKEN);
        if (cn.hutool.core.util.StrUtil.isBlank(customerToken)) customerToken = request.getParameter(HEADER_H5_TOKEN);
        if (cn.hutool.core.util.StrUtil.isNotBlank(customerToken)) {
            var customer = h5TokenService.validateToken(customerToken);
            if (customer == null) { response.setStatus(401); return; }
            FileDO file = fileService.getById(id);
            if (!h5AttachmentService.canAccess(customer, file)) { response.setStatus(403); return; }
            stream(id, response);
            return;
        }
        FileAccessContext accessContext = resolveAccessContext(request);
        if (accessContext == null) {
            response.setStatus(401);
            return;
        }
        FileDO file = fileService.getById(id);
        if (file == null || !fileAccessService.canAccess(file, accessContext)) {
            response.setStatus(403);
            return;
        }
        stream(id, response);
    }

    private void stream(Long id, HttpServletResponse response) {
        try {
            if (!platformFileHelper.streamPreviewContent(id, response)) {
                response.setStatus(404);
            }
        } catch (Exception e) {
            log.error("EOMS 文件预览失败: {}", id, e);
            response.setStatus(500);
        }
    }

    private FileAccessContext resolveAccessContext(HttpServletRequest request) {
        String h5Token = request.getHeader(HEADER_H5_TOKEN);
        if (cn.hutool.core.util.StrUtil.isBlank(h5Token)) {
            h5Token = request.getParameter(HEADER_H5_TOKEN);
        }
        if (cn.hutool.core.util.StrUtil.isNotBlank(h5Token)) {
            var customer = h5TokenService.validateToken(h5Token);
            if (customer != null && cn.hutool.core.util.StrUtil.isNotBlank(customer.getCustomerNo())) {
                return FileAccessContext.forH5Member(customer.getTenantId(), customer.getCustomerId(), customer.getCustomerNo(), customer.getAccountId(), customer.isCompanyAdmin());
            }
            return null;
        }
        try {
            if (StpUtil.isLogin()) {
                return FileAccessContext.forPlatformUser();
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
