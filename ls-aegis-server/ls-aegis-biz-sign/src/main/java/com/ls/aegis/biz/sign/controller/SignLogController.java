package com.ls.aegis.biz.sign.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.biz.sign.domain.SignLog;
import com.ls.aegis.biz.sign.domain.SignRecord;
import com.ls.aegis.biz.sign.mapper.SignLogMapper;
import com.ls.aegis.biz.sign.mapper.SignRecordMapper;
import com.ls.aegis.biz.sign.model.resp.SignLogExportResp;
import com.ls.aegis.biz.sign.model.resp.SignRecordExportResp;
import com.ls.aegis.biz.sign.service.ISignExecuteService;
import com.ls.aegis.biz.sign.utils.SignDataScopeHelper;
import com.ls.aegis.starter.support.excel.ExcelExportHelper;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import cn.hutool.core.bean.BeanUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.web.model.R;

import java.util.List;
import java.util.Map;

/**
 * 签章记录与日志
 */
@RestController
@RequestMapping("/sign")
public class SignLogController {

    @Autowired
    private SignRecordMapper recordMapper;

    @Autowired
    private SignLogMapper logMapper;

    @Autowired
    private ISignExecuteService executeService;

    /**
     * 签章记录列表
     */
    @SaCheckPermission("sign:log:list")
    @GetMapping("/record/list")
    public R<PageResp<SignRecord>> recordList(SignRecord record, @Valid PageQuery pageQuery) {
        SignDataScopeHelper.apply(record, false, true, "r");
        return R.ok(PageQueryHelper.page(pageQuery, () -> recordMapper.selectRecordList(record)));
    }

    /**
     * 签章记录详情
     */
    @SaCheckPermission("sign:log:query")
    @GetMapping("/record/{recordId}")
    public R<SignRecord> getRecordInfo(@PathVariable String recordId) {
        SignRecord query = new SignRecord();
        query.setRecordId(recordId);
        SignDataScopeHelper.apply(query, false, true, "r");
        List<SignRecord> list = recordMapper.selectRecordList(query);
        return R.ok(list.isEmpty() ? null : list.get(0));
    }

    /**
     * 签章日志列表
     */
    @SaCheckPermission("sign:log:list")
    @GetMapping("/log/list")
    public R<PageResp<SignLog>> logList(SignLog log, @Valid PageQuery pageQuery) {
        SignDataScopeHelper.apply(log, false, true, "l");
        return R.ok(PageQueryHelper.page(pageQuery, () -> logMapper.selectLogList(log)));
    }

    /**
     * 审计日志列表（签章日志）
     */
    @SaCheckPermission("sign:audit:list")
    @GetMapping("/audit/log/list")
    public R<PageResp<SignLog>> auditLogList(SignLog log, @Valid PageQuery pageQuery) {
        SignDataScopeHelper.apply(log, false, true, "l");
        return R.ok(PageQueryHelper.page(pageQuery, () -> logMapper.selectLogList(log)));
    }

    /**
     * 导出签章日志
     */
    @Log(module = "签章日志-导出")
    @SaCheckPermission("sign:log:export")
    @GetMapping(value = "/log/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void export(SignLog log, HttpServletResponse response) {
        PageQueryHelper.startExportLimit();
        SignDataScopeHelper.apply(log, false, true, "l");
        List<SignLog> list = logMapper.selectLogList(log);
        ExcelExportHelper.export(list, SignLogExportResp.class, "签章日志", response);
    }

    /**
     * 导出签章记录
     */
    @Log(module = "签章记录-导出")
    @SaCheckPermission("sign:log:export")
    @GetMapping(value = "/record/export", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void recordExport(SignRecord record, HttpServletResponse response) {
        PageQueryHelper.startExportLimit();
        SignDataScopeHelper.apply(record, false, true, "r");
        List<SignRecord> list = recordMapper.selectRecordList(record);
        List<SignRecordExportResp> rows = BeanUtil.copyToList(list, SignRecordExportResp.class);
        for (SignRecordExportResp row : rows) {
            row.setSignMode(convertMode(row.getSignMode()));
            row.setStatus(convertStatus(row.getStatus()));
        }
        ExcelExportHelper.export(rows, SignRecordExportResp.class, "签章记录", response);
    }

    private String convertMode(String mode) {
        if (mode == null) {
            return "";
        }
        return switch (mode) {
            case "single" -> "单页签章";
            case "multi", "continuous", "fixed" -> "逐页签章";
            case "custom" -> "定点签章";
            case "span" -> "骑缝签章";
            default -> mode;
        };
    }

    private String convertStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "completed" -> "成功";
            case "failed" -> "失败";
            default -> status;
        };
    }

    /**
     * 指纹校验
     */
    @Log(module = "指纹校验")
    @SaCheckPermission("sign:verify:run")
    @PostMapping("/verify/fingerprint")
    public R<Boolean> fingerprint(@RequestBody Long fileId) {
        return R.ok(executeService.verifyFingerprint(fileId));
    }

    /**
     * 上传签章后文件并执行指纹+CA校验
     */
    @Log(module = "指纹校验-上传")
    @SaCheckPermission("sign:verify:run")
    @PostMapping("/verify/fingerprint/upload")
    public R<Map<String, Object>> fingerprintUpload(@RequestParam("file") MultipartFile file) {
        return R.ok(executeService.verifyFingerprintUpload(file));
    }

}
