package com.ls.aegis.biz.sign.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.biz.sign.domain.SignFile;
import com.ls.aegis.biz.sign.service.ISignFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.web.model.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件管理
 * 【安全强制】类级别要求登录态；业务方法再叠加更细粒度的权限注解。
 */
@RestController
@RequestMapping("/sign/file")
@SaCheckLogin
public class SignFileController {

    private static final Logger log = LoggerFactory.getLogger(SignFileController.class);

    @Autowired
    private ISignFileService fileService;

    /**
     * 上传文件（写入平台 sys_file，sign_file 记指针；SHA256 去重）
     * 【安全】需要 sign:file:upload 权限 + 已登录态；不允许匿名上传
     */
    @Log(module = "文件上传")
    @SaCheckPermission("sign:file:upload")
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
        @RequestParam(value = "bizType", defaultValue = "sign") String bizType) {
        SignFile result = fileService.uploadFile(file, bizType);
        Map<String, Object> response = new HashMap<>();
        response.put("fileId", result.getFileId());
        response.put("fileName", result.getFileName());
        response.put("filePath", result.getFilePath());
        response.put("fileSize", result.getFileSize());
        response.put("fileMime", result.getFileMime());
        response.put("previewUrl", "/sign/file/download/" + result.getFileId());
        response.put("totalPages", getPdfPageCount(file));
        return R.ok(response);
    }

    private int getPdfPageCount(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            try {
                byte[] data = file.getBytes();
                String content = new String(data, "ISO-8859-1");
                int count = 0;
                int idx = 0;
                while ((idx = content.indexOf("/Type /Page", idx)) != -1) {
                    count++;
                    idx += 12;
                }
                return count > 0 ? count : 1;
            } catch (Exception e) {
                return 1;
            }
        }
        return 1;
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/{fileId}")
    public R<SignFile> getInfo(@PathVariable Long fileId) {
        if (!fileService.canAccessFile(fileId, UserContextHolder.getUserId())) {
            return R.fail("403", "无权访问该文件");
        }
        return R.ok(fileService.getFileInfo(fileId));
    }

    /**
     * 下载/预览文件
     */
    @GetMapping("/download/{fileId}")
    public void download(@PathVariable Long fileId, jakarta.servlet.http.HttpServletResponse response) {
        try {
            SignFile file = fileService.getFileInfo(fileId);
            if (file == null) {
                response.setStatus(404);
                return;
            }
            if (!fileService.canAccessFile(fileId, UserContextHolder.getUserId())) {
                response.setStatus(403);
                return;
            }
            byte[] data = fileService.getFileBytes(fileId);
            String contentType = file.getFileMime();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }
            response.setContentType(contentType);
            response.setContentLength(data.length);
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("文件下载失败: {}", fileId, e);
            try {
                response.setStatus(500);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 获取允许上传的文件类型
     */
    @GetMapping("/allowed")
    public R<List<String>> allowed() {
        return R.ok(fileService.getAllowedExtensions());
    }
}
