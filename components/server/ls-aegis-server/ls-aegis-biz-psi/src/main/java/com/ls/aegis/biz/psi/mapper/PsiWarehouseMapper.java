package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.WarehouseDO;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PsiWarehouseMapper extends PsiScopedMapper<WarehouseDO> {
}
