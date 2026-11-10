package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.CompanyAccountDO;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CompanyAccountMapper extends PsiScopedMapper<CompanyAccountDO> {
    @top.continew.starter.extension.datapermission.annotation.DataPermission
    @org.apache.ibatis.annotations.Select("SELECT * FROM psi_company_account WHERE id=#{id} AND deleted=0 FOR UPDATE")
    CompanyAccountDO selectByIdForUpdate(@org.apache.ibatis.annotations.Param("id") Long id);
}
