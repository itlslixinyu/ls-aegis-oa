package com.ls.aegis.compat.common.utils.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;
import com.ls.aegis.compat.common.config.ItlsConfig;
import com.ls.aegis.compat.common.constant.Constants;
import com.ls.aegis.compat.common.exception.file.FileNameLengthLimitExceededException;
import com.ls.aegis.compat.common.exception.file.FileSizeLimitExceededException;
import com.ls.aegis.compat.common.exception.file.InvalidExtensionException;
import com.ls.aegis.compat.common.utils.DateUtils;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.compat.common.utils.uuid.IdUtils;
import com.ls.aegis.compat.common.utils.uuid.Seq;

/**
 * 文件上传工具类
 * 
 * @author ItLs
 */
public class FileUploadUtils
{
    /**
     * 默认大小 50M -> 【P1 修复】对齐 Spring Multipart 上限 5MB。
     * 2C1G 下上传大图再加水印处理极其容易 OOM，Spring 层面也有 max-file-size 拦截；
     * 这里保持一致，避免两边阈值不同导致异常类型不统一。
     */
    public static final long DEFAULT_MAX_SIZE = 5 * 1024 * 1024L;

    /**
     * 【P1 修复】磁盘空间预检缓冲区（100MB），上传前需保证磁盘可用空间 ≥ 文件大小 + 缓冲
     */
    private static final long DISK_SPACE_BUFFER = 100 * 1024 * 1024L;

    /**
     * 默认的文件名最大长度 100
     */
    public static final int DEFAULT_FILE_NAME_LENGTH = 100;

    private static final Map<String, byte[]> FILE_MAGIC_NUMBERS = new HashMap<>();
    static {
        FILE_MAGIC_NUMBERS.put("jpg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
        FILE_MAGIC_NUMBERS.put("jpeg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
        FILE_MAGIC_NUMBERS.put("png", new byte[]{(byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47});
        FILE_MAGIC_NUMBERS.put("gif", new byte[]{(byte)0x47, (byte)0x49, (byte)0x46, (byte)0x38});
        FILE_MAGIC_NUMBERS.put("bmp", new byte[]{(byte)0x42, (byte)0x4D});
        FILE_MAGIC_NUMBERS.put("doc", new byte[]{(byte)0xD0, (byte)0xCF, (byte)0x11, (byte)0xE0});
        FILE_MAGIC_NUMBERS.put("docx", new byte[]{(byte)0x50, (byte)0x4B, (byte)0x03, (byte)0x04});
        FILE_MAGIC_NUMBERS.put("xls", new byte[]{(byte)0xD0, (byte)0xCF, (byte)0x11, (byte)0xE0});
        FILE_MAGIC_NUMBERS.put("xlsx", new byte[]{(byte)0x50, (byte)0x4B, (byte)0x03, (byte)0x04});
        FILE_MAGIC_NUMBERS.put("pdf", new byte[]{(byte)0x25, (byte)0x50, (byte)0x44, (byte)0x46});
        FILE_MAGIC_NUMBERS.put("zip", new byte[]{(byte)0x50, (byte)0x4B, (byte)0x03, (byte)0x04});
        FILE_MAGIC_NUMBERS.put("rar", new byte[]{(byte)0x52, (byte)0x61, (byte)0x72, (byte)0x21});
    }

    /**
     * 默认上传的地址
     */
    private static String defaultBaseDir = ItlsConfig.getProfile();

    public static void setDefaultBaseDir(String defaultBaseDir)
    {
        FileUploadUtils.defaultBaseDir = defaultBaseDir;
    }

    public static String getDefaultBaseDir()
    {
        return defaultBaseDir;
    }

    /**
     * 以默认配置进行文件上传
     *
     * @param file 上传的文件
     * @return 文件名称
     * @throws Exception
     */
    public static final String upload(MultipartFile file) throws IOException
    {
        try
        {
            return upload(getDefaultBaseDir(), file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 根据文件路径上传
     *
     * @param baseDir 相对应用的基目录
     * @param file 上传的文件
     * @return 文件名称
     * @throws IOException
     */
    public static final String upload(String baseDir, MultipartFile file) throws IOException
    {
        try
        {
            return upload(baseDir, file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
        }
        catch (Exception e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * 文件上传
     *
     * @param baseDir 相对应用的基目录
     * @param file 上传的文件
     * @param allowedExtension 上传文件类型
     * @return 返回上传成功的文件名
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws FileNameLengthLimitExceededException 文件名太长
     * @throws IOException 比如读写文件出错时
     * @throws InvalidExtensionException 文件校验异常
     */
    public static final String upload(String baseDir, MultipartFile file, String[] allowedExtension)
            throws FileSizeLimitExceededException, IOException, FileNameLengthLimitExceededException,
            InvalidExtensionException
    {
        return upload(baseDir, file, allowedExtension, false);
    }
    
    /**
     * 文件上传
     *
     * @param baseDir 相对应用的基目录
     * @param file 上传的文件
     * @param useCustomNaming 系统自定义文件名
     * @param allowedExtension 上传文件类型
     * @return 返回上传成功的文件名
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws FileNameLengthLimitExceededException 文件名太长
     * @throws IOException 比如读写文件出错时
     * @throws InvalidExtensionException 文件校验异常
     */
    public static final String upload(String baseDir, MultipartFile file, String[] allowedExtension, boolean useCustomNaming)
            throws FileSizeLimitExceededException, IOException, FileNameLengthLimitExceededException,
            InvalidExtensionException
    {
        int fileNameLength = Objects.requireNonNull(file.getOriginalFilename()).length();
        if (fileNameLength > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH)
        {
            throw new FileNameLengthLimitExceededException(FileUploadUtils.DEFAULT_FILE_NAME_LENGTH);
        }

        // 【P1 修复】磁盘空间预检，防止磁盘写满导致服务器崩溃（2C1G 低配服务器尤其敏感）
        checkDiskSpace(baseDir, file.getSize());

        assertAllowed(file, allowedExtension);

        String fileName = useCustomNaming ? uuidFilename(file) : extractFilename(file);

        String absPath = getAbsoluteFile(baseDir, fileName).getAbsolutePath();
        file.transferTo(Paths.get(absPath));
        return getPathFileName(baseDir, fileName);
    }

    /**
     * 编码文件名(日期格式目录 + 原文件名 + 序列值 + 后缀)
     */
    public static final String extractFilename(MultipartFile file)
    {
        return StringUtils.format("{}/{}_{}.{}", DateUtils.datePath(), FilenameUtils.getBaseName(file.getOriginalFilename()), Seq.getId(Seq.uploadSeqType), getExtension(file));
    }

    /**
     * 编编码文件名(日期格式目录 + UUID + 后缀)
     */
    public static final String uuidFilename(MultipartFile file)
    {
        return StringUtils.format("{}/{}.{}", DateUtils.datePath(), IdUtils.fastSimpleUUID(), getExtension(file));
    }

    public static final File getAbsoluteFile(String uploadDir, String fileName) throws IOException
    {
        File desc = new File(uploadDir + File.separator + fileName);

        if (!desc.exists())
        {
            if (!desc.getParentFile().exists())
            {
                desc.getParentFile().mkdirs();
            }
        }
        return desc;
    }

    public static final String getPathFileName(String uploadDir, String fileName) throws IOException
    {
        int dirLastIndex = ItlsConfig.getProfile().length() + 1;
        String currentDir = StringUtils.substring(uploadDir, dirLastIndex);
        return Constants.RESOURCE_PREFIX + "/" + currentDir + "/" + fileName;
    }

    /**
     * 【P1 修复】磁盘可用空间预检
     * <p>
     * 2C1G 低配服务器磁盘空间有限，若放任上传导致磁盘写满，会引发：
     * 1. 后续所有写入操作失败（日志、数据库、上传全部瘫痪）
     * 2. 系统关键服务崩溃，业务无法运行
     * 这里在上传前预检，留出 100MB 缓冲，磁盘不足时直接拒绝上传。
     *
     * @param baseDir  上传目标目录
     * @param fileSize 待写入文件大小（字节）
     * @throws IOException 磁盘空间不足时抛出
     */
    public static final void checkDiskSpace(String baseDir, long fileSize) throws IOException
    {
        File dir = new File(baseDir);
        // 目录不存在时取其父目录作为判定基准
        if (!dir.exists())
        {
            dir = dir.getParentFile() != null ? dir.getParentFile() : new File(".");
        }
        long freeSpace = dir.getUsableSpace();
        long required = fileSize + DISK_SPACE_BUFFER;
        if (freeSpace < required)
        {
            throw new IOException(
                    StringUtils.format("磁盘空间不足，可用 {}MB，至少需要 {}MB（含100MB缓冲）",
                            freeSpace / 1024 / 1024, required / 1024 / 1024));
        }
    }

    /**
     * 文件大小校验
     *
     * @param file 上传的文件
     * @return
     * @throws FileSizeLimitExceededException 如果超出最大大小
     * @throws InvalidExtensionException
     */
    public static final void assertAllowed(MultipartFile file, String[] allowedExtension)
            throws FileSizeLimitExceededException, InvalidExtensionException
    {
        long size = file.getSize();
        if (size > DEFAULT_MAX_SIZE)
        {
            throw new FileSizeLimitExceededException(DEFAULT_MAX_SIZE / 1024 / 1024);
        }

        String fileName = file.getOriginalFilename();
        String extension = getExtension(file);
        if (allowedExtension != null && !isAllowedExtension(extension, allowedExtension))
        {
            if (allowedExtension == MimeTypeUtils.IMAGE_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidImageExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.FLASH_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidFlashExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.MEDIA_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidMediaExtensionException(allowedExtension, extension,
                        fileName);
            }
            else if (allowedExtension == MimeTypeUtils.VIDEO_EXTENSION)
            {
                throw new InvalidExtensionException.InvalidVideoExtensionException(allowedExtension, extension,
                        fileName);
            }
            else
            {
                throw new InvalidExtensionException(allowedExtension, extension, fileName);
            }
        }

        if (!verifyFileMagicNumber(file, extension))
        {
            throw new InvalidExtensionException(allowedExtension, extension, fileName);
        }
    }

    private static boolean verifyFileMagicNumber(MultipartFile file, String extension)
    {
        if (StringUtils.isEmpty(extension))
        {
            return true;
        }
        byte[] expectedMagic = FILE_MAGIC_NUMBERS.get(extension.toLowerCase());
        if (expectedMagic == null)
        {
            return true;
        }
        try (InputStream is = file.getInputStream())
        {
            byte[] actualMagic = new byte[expectedMagic.length];
            if (is.read(actualMagic) < expectedMagic.length)
            {
                return false;
            }
            return Arrays.equals(actualMagic, expectedMagic);
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /**
     * 判断MIME类型是否是允许的MIME类型
     *
     * @param extension
     * @param allowedExtension
     * @return
     */
    public static final boolean isAllowedExtension(String extension, String[] allowedExtension)
    {
        for (String str : allowedExtension)
        {
            if (str.equalsIgnoreCase(extension))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文件名的后缀
     * 
     * @param file 表单文件
     * @return 后缀名
     */
    public static final String getExtension(MultipartFile file)
    {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (StringUtils.isEmpty(extension))
        {
            extension = MimeTypeUtils.getExtension(Objects.requireNonNull(file.getContentType()));
        }
        return extension;
    }
}


