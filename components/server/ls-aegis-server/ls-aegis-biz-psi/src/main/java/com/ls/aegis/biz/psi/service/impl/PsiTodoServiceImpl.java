package com.ls.aegis.biz.psi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.mapper.PsiTodoMapper;
import com.ls.aegis.biz.psi.model.entity.PsiTodoDO;
import com.ls.aegis.biz.psi.model.query.PsiTodoQuery;
import com.ls.aegis.biz.psi.model.resp.PsiTodoResp;
import com.ls.aegis.biz.psi.service.PsiTodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

@Service
@RequiredArgsConstructor
public class PsiTodoServiceImpl implements PsiTodoService {

    private final PsiTodoMapper psiTodoMapper;

    @Override
    public PageResp<PsiTodoResp> page(PsiTodoQuery query, PageQuery pageQuery) {
        QueryWrapper<PsiTodoDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByAsc("done").orderByDesc("id");
        Page<PsiTodoDO> page = psiTodoMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        return PageResp.build(page, PsiTodoResp.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void done(Long id) {
        PsiTodoDO entity = require(id);
        if (Boolean.TRUE.equals(entity.getDone())) {
            return;
        }
        entity.setDone(true);
        psiTodoMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopen(Long id) {
        PsiTodoDO entity = require(id);
        if (!Boolean.TRUE.equals(entity.getDone())) {
            return;
        }
        entity.setDone(false);
        psiTodoMapper.updateById(entity);
    }

    private PsiTodoDO require(Long id) {
        PsiTodoDO entity = psiTodoMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("待办不存在");
        }
        return entity;
    }
}
