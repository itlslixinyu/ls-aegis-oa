package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.InvoiceOutDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface InvoiceOutMapper extends PsiScopedMapper<InvoiceOutDO> {

    @DataPermission
    @Select("SELECT * FROM psi_invoice_out WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    InvoiceOutDO selectByIdForUpdate(@Param("id") Long id);
}
