package com.ls.aegis.biz.psi.support;

import com.ls.aegis.biz.psi.mapper.PsiTodoMapper;
import com.ls.aegis.biz.psi.model.entity.PsiTodoDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PSI 业务待办写入助手
 */
@Component
@RequiredArgsConstructor
public class PsiTodoHelper {

    private final PsiTodoMapper psiTodoMapper;

    public void create(Long tenantId, String todoType, String title, String bizType, Long bizId, String bizNo) {
        PsiTodoDO todo = new PsiTodoDO();
        todo.setTenantId(tenantId);
        todo.setDeptId(TenantFillHelper.currentDeptId());
        todo.setTodoType(todoType);
        todo.setTitle(title);
        todo.setBizType(bizType);
        todo.setBizId(bizId);
        todo.setBizNo(bizNo);
        todo.setDone(false);
        psiTodoMapper.insert(todo);
    }

    /**
     * 业务动作完成后自动关闭匹配的未完成待办（按类型 + 业务单号）。
     */
    public void completeOpen(String todoType, String bizNo) {
        if (bizNo == null || bizNo.isBlank()) {
            return;
        }
        var list = psiTodoMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(PsiTodoDO.class)
            .eq(PsiTodoDO::getTodoType, todoType)
            .eq(PsiTodoDO::getBizNo, bizNo)
            .eq(PsiTodoDO::getDone, false));
        for (PsiTodoDO todo : list) {
            todo.setDone(true);
            psiTodoMapper.updateById(todo);
        }
    }
}
