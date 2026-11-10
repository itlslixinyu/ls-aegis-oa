package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.ContractPurchaseDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

@Mapper
public interface ContractPurchaseMapper extends PsiScopedMapper<ContractPurchaseDO> {

    @DataPermission
    @Select("SELECT * FROM psi_contract_purchase WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    ContractPurchaseDO selectByIdForUpdate(@Param("id") Long id);
}
