package com.ls.aegis.biz.eoms.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.core.util.StrUtil;
import com.ls.aegis.biz.eoms.domain.RepairApply;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.domain.RepairRecord;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.file.model.entity.StorageDO;
import com.ls.aegis.biz.file.service.FileService;
import com.ls.aegis.biz.file.service.StorageService;
import com.ls.aegis.compat.common.config.ItlsConfig;
import com.ls.aegis.compat.common.utils.file.FileUtils;

/**
 * EOMS 业务文件写入平台 sys_file。
 */
@Component
public class EomsPlatformFileHelper {

    @Autowired
    private FileService fileService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private FileStorageService fileStorageService;

    /** 带鉴权预览接口路径前缀（相对路径，前端 axios 携带 token） */
    public static final String PREVIEW_PATH_PREFIX = "/eoms/file/preview/";

    /**
     * 将已处理好的字节（如水印图）写入平台文件中心。
     *
     * @param data             文件内容
     * @param originalFilename 原始文件名
     * @param parentPath       上级目录，如 /eoms/repair
     */
    public FileInfo uploadWatermarkedJpeg(byte[] data, String originalFilename, String parentPath) throws IOException {
        String name = ensureJpegName(originalFilename);
        // This entry is only for WatermarkUtils JPEG output; FileService still verifies actual magic.
        MultipartFile file = new BytesMultipartFile("file", name, guessContentType(name), data);
        return fileService.upload(file, parentPath);
    }

    /**
     * H5 水印上传响应：只回 sys_file.id 与平台可预览 URL，不再写本地盘。
     */
    public Map<String, Object> h5UploadPayload(FileInfo uploaded, String originalFilename) {
        String fileId = uploaded.getId();
        Map<String, Object> data = new HashMap<>();
        // 必须回鉴权预览路径；/api/file/** 直链 img/axios 都带不上 h5-token
        data.put("url", previewUrl(fileId));
        data.put("fileName", ensureJpegName(originalFilename));
        data.put("fileId", fileId);
        data.put("newFileName", "图片");
        data.put("originalFilename", ensureJpegName(originalFilename));
        return data;
    }

    /** Only for re-encoded watermark output, never rename arbitrary unprocessed PNG bytes. */
    public String ensureJpegName(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        int dot = name.lastIndexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        // Keep a display basename, removing controls, path and reserved filesystem characters.
        base = base.replaceAll("[\\p{Cntrl}<>:\"/\\\\|?*]", "_").replaceAll("^[. ]+|[. ]+$", "");
        if (base.isBlank()) base = "photo";
        if (base.length() > 100) base = base.substring(0, base.offsetByCodePoints(0, Math.min(80, base.codePointCount(0, base.length()))));
        return base + ".jpg";
    }

    /** 是否为平台 sys_file.id（纯数字）。 */
    public boolean isFileIdToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将业务字段中的单个 token 转为带鉴权的预览路径。
     * 纯数字按 sys_file.id → {@link #PREVIEW_PATH_PREFIX}{id}；存量 /profile 路径原样返回。
     */
    public String previewUrl(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }
        String trimmed = token.trim();
        if (!isFileIdToken(trimmed)) {
            return trimmed;
        }
        return PREVIEW_PATH_PREFIX + trimmed;
    }

    /**
     * 读取 sys_file 二进制内容与类型（整文件进内存，仅适合小文件或兼容场景）。
     */
    public FilePreviewContent readPreviewContent(Long fileId) {
        if (fileId == null) {
            return null;
        }
        FileDO file = fileService.getById(fileId);
        if (file == null || file.getPath() == null) {
            return null;
        }
        StorageDO storage = storageService.getById(file.getStorageId());
        if (storage == null) {
            return null;
        }
        FileInfo fileInfo = file.toFileInfo(storage);
        byte[] data = fileStorageService.download(fileInfo).bytes();
        String contentType = StrUtil.blankToDefault(file.getContentType(), guessContentType(file.getOriginalName()));
        return new FilePreviewContent(data, contentType);
    }

    /**
     * 流式输出预览文件，避免整文件加载到堆内存。
     *
     * @return true 已写出；false 文件不存在
     */
    public boolean streamPreviewContent(Long fileId, HttpServletResponse response) throws IOException {
        if (fileId == null) {
            return false;
        }
        FileDO file = fileService.getById(fileId);
        if (file == null || file.getPath() == null) {
            return false;
        }
        StorageDO storage = storageService.getById(file.getStorageId());
        if (storage == null) {
            return false;
        }
        String contentType = StrUtil.blankToDefault(file.getContentType(), guessContentType(file.getOriginalName()));
        response.setContentType(contentType);
        if (file.getSize() != null && file.getSize() > 0) {
            response.setContentLengthLong(file.getSize());
        }
        response.setHeader("Cache-Control", "private, max-age=300");
        FileInfo fileInfo = file.toFileInfo(storage);
        fileStorageService.download(fileInfo).outputStream(response.getOutputStream());
        response.getOutputStream().flush();
        return true;
    }

    /** 预览文件内容与 MIME */
    public record FilePreviewContent(byte[] data, String contentType) {
    }

    /** 把存储绝对地址收成 H5/PC 都可走反代的相对路径。 */
    public String toRelativeFileUrl(String full) {
        if (full == null || full.isBlank()) {
            return full;
        }
        int apiIdx = full.indexOf("/api/file/");
        if (apiIdx >= 0) {
            return full.substring(apiIdx);
        }
        int idx = full.indexOf("/file/");
        if (idx >= 0) {
            return "/api" + full.substring(idx);
        }
        return full;
    }

    /** 逗号分隔字段批量转预览 URL。 */
    public String previewCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return csv;
        }
        StringBuilder sb = new StringBuilder();
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(previewUrl(trimmed));
        }
        return sb.toString();
    }

    public void fillApply(RepairApply apply) {
        if (apply != null) {
            apply.setPhotoUrls(previewCsv(apply.getPhotos()));
        }
    }

    public void fillOrder(RepairOrder order) {
        if (order != null) {
            order.setPhotoUrls(previewCsv(order.getPhotos()));
        }
    }

    public void fillRecord(RepairRecord record) {
        if (record != null) {
            record.setProcessingPhotoUrls(previewCsv(record.getProcessingPhotos()));
            record.setCompletedPhotoUrls(previewCsv(record.getCompletedPhotos()));
        }
    }

    public void fillApplies(List<RepairApply> list) {
        if (list == null) {
            return;
        }
        for (RepairApply item : list) {
            fillApply(item);
        }
    }

    public void fillOrders(List<RepairOrder> list) {
        if (list == null) {
            return;
        }
        for (RepairOrder item : list) {
            fillOrder(item);
        }
    }

    public void fillRecords(List<RepairRecord> list) {
        if (list == null) {
            return;
        }
        for (RepairRecord item : list) {
            fillRecord(item);
        }
    }

    /**
     * 仅删除存量本地盘路径；纯数字 sys_file.id 留给 M1.2，避免误删工作目录。
     */
    public void deleteLocalIfPath(String csv) {
        if (csv == null || csv.isEmpty()) {
            return;
        }
        for (String photo : csv.split(",")) {
            String trimmed = photo.trim();
            if (trimmed.isEmpty() || isFileIdToken(trimmed) || trimmed.startsWith("sys_file:")) {
                continue;
            }
            try {
                String filePath = ItlsConfig.getProfile() + FileUtils.stripPrefix(trimmed);
                FileUtils.deleteFile(filePath);
            } catch (Exception ignored) {
                // 存量文件缺失不影响删除主流程
            }
        }
    }

    private static String guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }
}
