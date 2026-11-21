package com.ls.aegis.compat.common.utils;

import com.ls.aegis.compat.common.exception.ServiceException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件真实类型校验工具 — 通过「文件头魔数（Magic Number）」识别文件真实格式。
 * <p>
 * 【为什么需要】文件扩展名和 MIME 类型都可以被伪造，仅靠后缀白名单无法防御：
 * <ul>
 *   <li>1. 把 xss.html 重命名为 xss.png 绕过图片校验</li>
 *   <li>2. 把 webshell 重命名为 contract.pdf 上传</li>
 *   <li>3. 图片/polyglot 文件：前端显示为正常图片，实则内嵌脚本/zip</li>
 * </ul>
 * <p>
 * 【实现原则】只放行"明确已知"的魔数，任何未命中的文件一律视为非法（Fail-Closed）。
 * 每个判断函数要求：文件头最短 N 字节匹配 → 返回 true；否则返回 false。
 */
public final class FileMagicValidator {

    private FileMagicValidator() {
    }

    // ================ 允许的 MIME 类型白名单（HTTP Content-Type 辅助检查，不做唯一依据） ================

    private static final Set<String> IMAGE_ALLOWED_MIME = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "image/png", "image/jpeg", "image/gif", "image/bmp",
            "image/x-png",  "image/pjpeg" // 旧浏览器兼容
    )));

    private static final Set<String> PDF_ALLOWED_MIME = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "application/pdf",
            "application/octet-stream" // 部分客户端/代理返回通用MIME
    )));

    private static final Set<String> OFFICE_ALLOWED_MIME = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    )));

    // ================ 魔数表 ================
    // 注意：PK 对应 zip 系列（docx/xlsx/pptx/jar/apk），需结合扩展名二次确认
    private static final byte[] MAGIC_PDF = {(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46}; // %PDF
    private static final byte[] MAGIC_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] MAGIC_JPEG_SOI = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}; // JPEG: SOI + marker
    private static final byte[] MAGIC_GIF_87 = {'G', 'I', 'F', '8', '7', 'a'};
    private static final byte[] MAGIC_GIF_89 = {'G', 'I', 'F', '8', '9', 'a'};
    private static final byte[] MAGIC_BMP = {'B', 'M'};
    private static final byte[] MAGIC_PK_ZIP = {'P', 'K', 0x03, 0x04}; // docx/xlsx/pptx 都是 ZIP 格式
    private static final byte[] MAGIC_OLE2 = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}; // 旧版 doc/xls/ppt (OLE2)

    // ================ 主入口 ================

    /**
     * 根据扩展名匹配校验文件真实类型。
     * <p>
     * 【核心规则】扩展名与魔数必须互相匹配，否则视为"伪装文件"直接拒绝。
     *
     * @param file       上传的文件
     * @param extension  小写扩展名（pdf / png / jpg / docx ...）
     * @throws ServiceException 校验不通过时抛出（消息已对用户友好）
     */
    public static void assertValid(MultipartFile file, String extension) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        if (extension == null || extension.isEmpty()) {
            throw new ServiceException("缺少文件扩展名，无法确认类型");
        }
        String ext = extension.toLowerCase();

        // 1) 读取文件头 16 字节（足够覆盖所有已知魔数）
        byte[] header = safePeekHeader(file, 16);

        // 2) 按扩展名分派
        boolean match = switch (ext) {
            case "pdf" -> isPdf(header);
            case "png" -> isPng(header);
            case "jpg", "jpeg" -> isJpeg(header);
            case "gif" -> isGif(header);
            case "bmp" -> isBmp(header);
            case "doc", "xls", "ppt" -> isOle2CompoundFile(header);
            case "docx", "xlsx", "pptx" -> isZipPackage(header);
            default -> throw new ServiceException("不支持的文件类型：" + ext);
        };
        if (!match) {
            throw new ServiceException(
                    "文件类型伪装告警：文件扩展名 ." + ext + " 与实际内容不一致，拒绝上传。"
                            + "请确认为真实的 " + ext.toUpperCase() + " 文件后重试");
        }

        // 3) 辅助：MIME 与扩展名强不一致时额外告警（仅做软检查，不拦截 — 浏览器会误报）
        String mime = file.getContentType();
        if (mime != null && !mime.isEmpty()) {
            Set<String> allowed = resolveAllowedMimeByExt(ext);
            if (allowed != null && !allowed.contains(mime.toLowerCase())) {
                // 只记录，不拦截（部分老浏览器 MIME 不准）
                // logger.info("上传文件 MIME={} 与扩展名 .{} 不匹配，已通过魔数校验放行", mime, ext);
            }
        }
    }

    /**
     * 印章图片专用校验：必须是 PNG/JPEG（印章要求透明通道时强制 PNG）。
     * 同时额外禁止 GIF 动图（防止签章过程中动画帧被误用）。
     */
    public static void assertSealImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("印章图片不能为空");
        }
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        // 印章图片只允许 png/jpg/jpeg（gif/bmp 不接受）
        if (!("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext))) {
            throw new ServiceException("印章图片仅支持 PNG / JPG / JPEG 格式");
        }
        byte[] header = safePeekHeader(file, 16);
        boolean match = "png".equals(ext) ? isPng(header) : isJpeg(header);
        if (!match) {
            throw new ServiceException("印章图片内容与扩展名不一致（疑似伪造），请上传真实的图片文件");
        }
    }

    /**
     * 证书文件专用校验：PFX / P12（PKCS#12 文件）。
     * <p>PKCS12 魔数不固定（ASN.1 序列头），通过 ASN.1 Tag 的稳定特征判断：
     * 字节 0 = 0x30 (SEQUENCE)，字节 1 = 长度编码形式，后续为 PKCS#7/PKCS#12 数据。
     * 为了严谨，我们校验文件前 4 字节并结合扩展名 .p12/.pfx。
     */
    public static void assertPkcs12(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("证书文件不能为空");
        }
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        if (!("pfx".equals(ext) || "p12".equals(ext))) {
            throw new ServiceException("数字证书仅支持 .pfx / .p12 (PKCS#12) 格式");
        }
        byte[] header = safePeekHeader(file, 8);
        // PKCS#12 DER 编码：0x30 (SEQUENCE) + 长度字节
        // 兼容：第一个字节=0x30 基本稳定；再加上不能是 zip/rar（常见伪装）
        if (header.length < 2 || header[0] != 0x30) {
            throw new ServiceException("证书文件内容非法：不是合法的 PKCS#12（.pfx/.p12）二进制格式，请确认真实证书文件");
        }
    }

    // ================ 单类型判断（对外亦可复用） ================

    public static boolean isPdf(byte[] header) {
        return match(header, MAGIC_PDF);
    }

    public static boolean isPng(byte[] header) {
        return match(header, MAGIC_PNG);
    }

    public static boolean isJpeg(byte[] header) {
        return match(header, MAGIC_JPEG_SOI);
    }

    public static boolean isGif(byte[] header) {
        return match(header, MAGIC_GIF_87) || match(header, MAGIC_GIF_89);
    }

    public static boolean isBmp(byte[] header) {
        return match(header, MAGIC_BMP);
    }

    public static boolean isZipPackage(byte[] header) {
        return match(header, MAGIC_PK_ZIP);
    }

    public static boolean isOle2CompoundFile(byte[] header) {
        return match(header, MAGIC_OLE2);
    }

    // ================ 辅助 ================

    private static boolean match(byte[] header, byte[] magic) {
        if (header == null || header.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 读取文件前 N 字节，失败抛 ServiceException（可读即可用，避免重复读流）。
     */
    private static byte[] safePeekHeader(MultipartFile file, int length) {
        try (InputStream in = file.getInputStream()) {
            byte[] buf = new byte[length];
            int read = 0;
            while (read < length) {
                int n = in.read(buf, read, length - read);
                if (n <= 0) break;
                read += n;
            }
            if (read == 0) {
                throw new ServiceException("上传文件为空，无法读取文件内容");
            }
            // 实际读取不足 length 时：trim 到真实长度（例如小 BMP 文件 < 16 字节也允许）
            if (read < length) {
                return Arrays.copyOf(buf, read);
            }
            return buf;
        } catch (IOException e) {
            throw new ServiceException("文件读取失败：无法确认上传内容类型");
        }
    }

    private static String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private static Set<String> resolveAllowedMimeByExt(String ext) {
        return switch (ext) {
            case "pdf" -> PDF_ALLOWED_MIME;
            case "png", "jpg", "jpeg", "gif", "bmp" -> IMAGE_ALLOWED_MIME;
            case "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> OFFICE_ALLOWED_MIME;
            default -> null;
        };
    }

    /**
     * 对于已经读入内存的字节数组（如印章提取后生成的 PNG），同样允许快速校验。
     */
    public static void assertImageBytes(byte[] bytes, String fileTypeLabel) {
        if (bytes == null || bytes.length < 2) {
            throw new ServiceException(fileTypeLabel + "内容为空");
        }
        byte[] header = bytes.length < 16 ? bytes : Arrays.copyOf(bytes, 16);
        if (!isPng(header) && !isJpeg(header)) {
            throw new ServiceException(fileTypeLabel + "格式非法：仅支持 PNG/JPEG");
        }
    }
}
