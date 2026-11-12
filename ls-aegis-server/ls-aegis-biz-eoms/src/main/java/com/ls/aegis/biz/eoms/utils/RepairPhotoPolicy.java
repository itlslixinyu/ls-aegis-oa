package com.ls.aegis.biz.eoms.utils;

import com.ls.aegis.compat.common.exception.ServiceException;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/** The mobile repair contract is applied before reading or decoding the full image. */
public final class RepairPhotoPolicy {
    private RepairPhotoPolicy() { }

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ServiceException("请选择非空的现场照片");
        if (file.getSize() > 10L * 1024 * 1024) throw new ServiceException("图片大小不能超过10MB");
        try (var stream = file.getInputStream()) {
            byte[] header = stream.readNBytes(8);
            boolean jpeg = header.length >= 3 && (header[0] & 255) == 255 && (header[1] & 255) == 216 && (header[2] & 255) == 255;
            boolean png = java.util.Arrays.equals(header, new byte[]{(byte)137,80,78,71,13,10,26,10});
            if (!jpeg && !png) throw new ServiceException("仅支持真实的JPG/PNG图片");
        } catch (IOException ex) {
            throw new ServiceException("图片无法读取，请重新选择");
        }
    }
}
