package com.ls.aegis.biz.psi.mapper;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.ls.aegis.mybatis.base.mapper.DataPermissionMapper;
import org.apache.ibatis.annotations.Param;
import top.continew.starter.extension.datapermission.annotation.DataPermission;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/** PSI headers have stable dept/create-user ownership; detail IDs must obey the same scope as lists. */
public interface PsiScopedMapper<T> extends DataPermissionMapper<T> {
    @Override
    @DataPermission
    T selectById(Serializable id);

    @Override
    @DataPermission
    List<T> selectByIds(@Param(Constants.COLL) Collection<? extends Serializable> ids);

    @Override
    @DataPermission
    int updateById(@Param(Constants.ENTITY) T entity);
}
