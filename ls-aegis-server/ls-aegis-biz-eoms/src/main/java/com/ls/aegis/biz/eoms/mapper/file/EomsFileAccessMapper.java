package com.ls.aegis.biz.eoms.mapper.file;

import org.apache.ibatis.annotations.Param;

/**
 * EOMS 业务文件访问校验（sys_file.id 是否被 EOMS 业务引用）。
 */
public interface EomsFileAccessMapper {
    int countVisibleApplyFile(com.ls.aegis.biz.eoms.model.query.EomsFileQuery query);
    int countVisibleOrderFile(com.ls.aegis.biz.eoms.model.query.EomsFileQuery query);
    int countVisibleRecordFile(com.ls.aegis.biz.eoms.model.query.EomsFileQuery query);
    int countVisibleContractFile(com.ls.aegis.biz.eoms.model.query.EomsFileQuery query);

    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    int countByOwnerAndFileId(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId, @Param("fileId") Long fileId);

    /**
     * 文件是否属于指定客户编号的业务数据。
     */
    int countByCustomerAndFileId(@Param("customerNo") String customerNo, @Param("fileId") Long fileId);

    /**
     * 文件是否被任意 EOMS 业务记录引用（平台已登录用户）。
     */
    int countByFileId(@Param("fileId") Long fileId);
}
