package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.ReceiptDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface ReceiptMapper extends PsiScopedMapper<ReceiptDO> {

    @DataPermission
    @Select("SELECT * FROM psi_receipt WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    ReceiptDO selectByIdForUpdate(@Param("id") Long id);
}
