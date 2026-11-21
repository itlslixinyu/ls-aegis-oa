package com.ls.aegis.biz.file.access;

import com.ls.aegis.biz.file.model.entity.FileDO;

/**
 * 业务模块文件访问扩展校验（由 EOMS、签章等模块实现并注册为 Spring Bean）。
 */
public interface FileAccessChecker {

    /**
     * 是否允许访问指定文件。
     */
    boolean canAccess(FileDO file, FileAccessContext context);
}
