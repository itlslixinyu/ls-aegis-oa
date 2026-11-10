package com.ls.aegis.biz.sign.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.crypto.SmUtil;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.biz.sign.domain.SignVerifyReceipt;
import com.ls.aegis.biz.sign.domain.vo.SignRequest;
import com.ls.aegis.biz.sign.domain.vo.SignResult;
import com.ls.aegis.biz.sign.domain.vo.UnifiedSignRequest;
import com.ls.aegis.biz.sign.service.IReceiptService;
import com.ls.aegis.biz.sign.service.ISignExecuteService;
import com.ls.aegis.biz.sign.service.IUnifiedSignService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.web.model.R;
import top.continew.starter.json.jackson.util.JSONUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * 签章执行
 * <p>
 * 类级别登录校验；签章写操作使用平台 Redis 锁及成功令牌，阻止并发重复签章。
 */
@RestController
@RequestMapping("/sign/execute")
@SaCheckLogin
public class SignExecuteController {

    private static final Logger log = LoggerFactory.getLogger(SignExecuteController.class);

    @Autowired
    private ISignExecuteService executeService;

    @Autowired
    private IReceiptService receiptService;

    @Autowired
    private IUnifiedSignService unifiedSignService;

    @Autowired
    private IdempotentHelper idempotentHelper;

    /**
     * 获取可签章项目列表
     */
    @SaCheckPermission("sign:execute:run")
    @GetMapping("/projects")
    public R<Object> getProjects() {
        Long userId = UserContextHolder.getUserId();
        return R.ok(executeService.getSignableProjects(userId));
    }

    /**
     * 签章准备 - 校验文件/项目/印章权限
     */
    @Log(module = "签章执行-准备")
    @SaCheckPermission("sign:execute:run")
    @PostMapping("/prepare")
    public R<Boolean> prepare(@Valid @RequestBody SignRequest request) {
        return R.ok(executeService.prepareSign(request));
    }

    /**
     * 执行签章
     */
    @Log(module = "签章执行")
    @SaCheckPermission("sign:execute:run")
    @PostMapping("/run")
    public R<SignResult> run(@Valid @RequestBody SignRequest request) {
        request.setUseManagedKey("1");
        return R.ok(runSignOnce("run", request, () -> executeService.executeSign(request)));
    }

    /**
     * 从已签章PDF提取证书信息
     */
    @SaCheckPermission("sign:log:query")
    @PostMapping("/cert-info")
    public R<?> certInfo(@RequestBody(required = false) Object body) {
        Long fileId = parseFileId(body);
        if (fileId == null) {
            throw new ServiceException("缺少文件ID");
        }
        return R.ok(executeService.extractCertInfo(fileId));
    }

    /**
     * 查询签章回单
     */
    @SaCheckPermission("sign:log:query")
    @GetMapping("/receipt/{recordId}")
    public R<SignVerifyReceipt> receipt(@PathVariable String recordId) {
        return R.ok(executeService.selectReceiptByRecordId(recordId));
    }

    /**
     * 下载签章回单 PDF
     */
    @SaCheckPermission("sign:log:query")
    @GetMapping("/receipt/{recordId}/download")
    public void receiptPdf(@PathVariable String recordId, jakarta.servlet.http.HttpServletResponse response) {
        try {
            SignVerifyReceipt receipt = executeService.selectReceiptByRecordId(recordId);
            if (receipt == null) {
                response.setStatus(404);
                return;
            }
            byte[] data = receiptService.renderReceiptPdf(receipt);
            response.setContentType("application/pdf");
            response.setContentLength(data.length);
            String baseName = StringUtils.isNotEmpty(receipt.getReceiptNo()) ? receipt.getReceiptNo() : ("receipt-" + recordId);
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" +
                URLEncoder.encode(baseName + ".pdf", StandardCharsets.UTF_8));
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("签章回单下载失败: {}", recordId, e);
            try {
                response.setStatus(500);
            } catch (Exception ignored) {
            }
        }
    }

    private Long parseFileId(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof Number number) {
            return number.longValue();
        }
        if (body instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (body instanceof Map<?, ?> map) {
            Object value = map.get("fileId");
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text) {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 合并签章（一次性完成普通章+骑缝章+单次签名）
     */
    @Log(module = "合并签章")
    @SaCheckPermission("sign:execute:run")
    @PostMapping("/unified")
    public R<SignResult> unifiedSign(@Valid @RequestBody UnifiedSignRequest request) {
        return R.ok(runSignOnce("unified", request, () -> unifiedSignService.executeUnifiedSign(request)));
    }

    private <T> T runSignOnce(String action, Object request, Supplier<T> operation) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new ServiceException("登录状态无效，请重新登录");
        }
        // 原兼容 RepeatSubmit 只是注解，仓库中没有执行它的切面。复用统一幂等组件，
        // 按租户、用户、接口与规范化请求指纹隔离；失败不占用成功令牌，允许重试。
        String key = userId + ":" + SmUtil.sm3(JSONUtils.toJsonStr(request));
        return idempotentHelper.runOnce("sign:idem:", UserContextHolder.getTenantId(), action, key,
            Duration.ofSeconds(5), operation);
    }
}
