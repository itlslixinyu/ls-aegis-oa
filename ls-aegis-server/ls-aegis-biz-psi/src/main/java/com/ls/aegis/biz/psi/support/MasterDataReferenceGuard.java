package com.ls.aegis.biz.psi.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.entity.*;
import com.ls.aegis.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.continew.starter.core.exception.BusinessException;
import java.util.*;

/** Preserve all active document/ledger/detail references, including exhausted batches and VOID audit heads. */
@Component
@RequiredArgsConstructor
public class MasterDataReferenceGuard {
    private final MasterDataReferenceMapper references;
    private final UnitMapper units;
    private final GoodsMapper goods;
    private final PsiWarehouseMapper warehouses;

    public void requireEditable(String kind, Long id) {
        requireVisible(kind, Collections.singletonList(id));
    }

    public void requireDeletable(String kind, List<Long> ids) {
        List<Long> keys = requireVisible(kind, ids);
        Long tenant = TenantFillHelper.currentTenantId();
        for(Long id:keys) {
            if(references.lockMaster(kind,tenant,id)==null) throw new BusinessException("主数据不存在或已变更，请刷新后重试");
            if(references.hasReferences(kind,tenant,id)) throw new BusinessException("主数据存在单据、库存、明细或审计引用，不可删除；请使用禁用，或先清理可撤销的引用");
        }
        // New reference writers must share this master anchor to serialize create/delete races.
    }

    private List<Long> requireVisible(String kind, List<Long> ids) {
        var actor=UserContextHolder.getContext();
        if(actor==null || actor.getId()==null || (!actor.isSuperAdmin() && (actor.getDeptId()==null
            || actor.getRoles()==null || actor.getRoles().isEmpty() || actor.getRoles().stream()
            .anyMatch(role->role==null || role.getId()==null || role.getDataScope()==null)))) {
            throw new BusinessException("主数据操作身份或数据权限上下文无效");
        }
        if(ids==null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) throw new BusinessException("请选择有效主数据");
        List<Long> keys=ids.stream().distinct().sorted().toList(); Long tenant=TenantFillHelper.currentTenantId();
        // selectList is explicitly @DataPermission on the platform mapper; never replace it with selectById/maps.
        Set<Long> visible=new HashSet<>();
        switch(kind) {
            case "UNIT" -> units.selectList(Wrappers.<UnitDO>lambdaQuery().eq(UnitDO::getTenantId,tenant).in(UnitDO::getId,keys).select(UnitDO::getId)).forEach(row->visible.add(row.getId()));
            case "GOODS" -> goods.selectList(Wrappers.<GoodsDO>lambdaQuery().eq(GoodsDO::getTenantId,tenant).in(GoodsDO::getId,keys).select(GoodsDO::getId)).forEach(row->visible.add(row.getId()));
            case "WAREHOUSE" -> warehouses.selectList(Wrappers.<WarehouseDO>lambdaQuery().eq(WarehouseDO::getTenantId,tenant).in(WarehouseDO::getId,keys).select(WarehouseDO::getId)).forEach(row->visible.add(row.getId()));
            default -> throw new IllegalArgumentException("Unsupported master kind");
        }
        if(visible.size()!=keys.size()) throw new BusinessException("主数据不存在或无权操作");
        return keys;
    }
}
