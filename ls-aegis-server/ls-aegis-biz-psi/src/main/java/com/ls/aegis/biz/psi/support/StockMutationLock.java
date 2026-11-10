package com.ls.aegis.biz.psi.support;

import com.ls.aegis.biz.psi.mapper.StockMutationLockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.continew.starter.core.exception.BusinessException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * One persistent row per inventory dimension, including dimensions without batches.
 * Acquire the full document key set before any inventory mutation; rows remain locked until commit/rollback.
 */
@Component
@RequiredArgsConstructor
public class StockMutationLock {
    private final StockMutationLockMapper mapper;
    public record Key(Long tenantId, Long warehouseId, Long goodsId) {}

    public void lock(Long tenantId, Long warehouseId, Long goodsId) {
        lockAll(List.of(new Key(tenantId, warehouseId, goodsId)));
    }

    public void lockAll(Collection<Key> keys) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new BusinessException("库存变更必须在业务事务内持有库存锁");
        if (keys == null || keys.stream().anyMatch(k -> k == null || k.tenantId() == null || k.tenantId() < 0
                || k.warehouseId() == null || k.warehouseId() <= 0 || k.goodsId() == null || k.goodsId() <= 0))
            throw new BusinessException("库存锁的租户、仓库和商品维度不能为空或无效");
        for (Key key : keys.stream().distinct().sorted(Comparator.comparing(Key::tenantId)
                .thenComparing(Key::warehouseId).thenComparing(Key::goodsId)).toList()) {
            mapper.ensureAnchor(key.tenantId(), key.warehouseId(), key.goodsId());
            if (mapper.lockAnchor(key.tenantId(), key.warehouseId(), key.goodsId()) == null)
                throw new BusinessException("库存串行锚点创建失败");
        }
    }

}
