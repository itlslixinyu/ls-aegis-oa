package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.PsiTodoQuery;
import com.ls.aegis.biz.psi.model.resp.PsiTodoResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

/**
 * 业务待办
 */
public interface PsiTodoService {

    PageResp<PsiTodoResp> page(PsiTodoQuery query, PageQuery pageQuery);

    /** 标记完成 */
    void done(Long id);

    /** 重新打开 */
    void reopen(Long id);
}
