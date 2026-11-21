package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.InvoiceInDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface InvoiceInMapper extends PsiScopedMapper<InvoiceInDO> {

    @DataPermission
    @Select("SELECT * FROM psi_invoice_in WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    InvoiceInDO selectByIdForUpdate(@Param("id") Long id);
}
