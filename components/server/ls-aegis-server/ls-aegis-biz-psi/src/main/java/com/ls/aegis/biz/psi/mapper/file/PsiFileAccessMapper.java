package com.ls.aegis.biz.psi.mapper.file;

import org.apache.ibatis.annotations.Param;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

/**
 * PSI 业务文件访问校验（sys_file.id 是否被进销存业务引用）。
 */
public interface PsiFileAccessMapper {

    /**
     * 文件是否被任意 PSI 业务记录引用（平台已登录用户）。
     */
    @DataPermission(tableAlias = "ref")
    int countAccessible(@Param("fileId") Long fileId, @Param("tenantId") Long tenantId,
                        @Param("kind") String kind);
}
