package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.GoodsCategoryDO;

import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类 Mapper
 */
@Mapper
public interface GoodsCategoryMapper extends PsiScopedMapper<GoodsCategoryDO> {
}
