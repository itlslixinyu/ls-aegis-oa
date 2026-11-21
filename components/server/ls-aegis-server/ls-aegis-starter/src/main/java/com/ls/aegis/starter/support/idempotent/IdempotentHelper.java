package com.ls.aegis.starter.support.idempotent;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.continew.starter.cache.redisson.util.RedisLockUtils;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.exception.BusinessException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 确认类接口幂等（Redis + 锁）
 * <p>
 * PSI 确认：相同 key 返回首次结果。EOMS 收款：相同令牌拒绝重复提交；失败不落缓存，允许原令牌重试。
 * 业务侧禁止再手写 Redis token。
 */
@Component
@Slf4j
public class IdempotentHelper {

    // 该资源有意随线程保留到所属操作完成（含事务挂起期间），只用于阻止分布式锁的同线程重入。
    // 结果回放仍按当前事务同步列表判断；每条成功/失败路径均由 LockLease 清理，不能串用事务结果。
    private static final Object LOCAL_LOCK_OWNERS = new Object();

    /**
     * PSI 确认：{@code psi:idem:{tenant}:{action}:{key}}，24 小时，命中缓存则回放首次结果
     */
    public String execute(Long tenantId, String action, String key, Supplier<String> actionFn) {
        return execute(IdempotentConstants.PSI_KEY_PREFIX, tenantId, action, key,
            Duration.ofHours(IdempotentConstants.DEFAULT_TTL_HOURS), true, actionFn);
    }

    /**
     * 令牌只成功一次：已成功过的 key 再次提交则拒绝；动作失败不缓存，可原令牌重试
     */
    public <T> T runOnce(String keyPrefix, Long tenantId, String action, String key, Duration ttl, Supplier<T> actionFn) {
        AtomicReference<T> holder = new AtomicReference<>();
        execute(keyPrefix, tenantId, action, key, ttl, false, () -> {
            holder.set(actionFn.get());
            return "1";
        });
        return holder.get();
    }

    private String execute(String keyPrefix,
                           Long tenantId,
                           String action,
                           String key,
                           Duration ttl,
                           boolean replayCached,
                           Supplier<String> actionFn) {
        if (StrUtil.isBlank(key)) {
            throw new BusinessException("幂等键不能为空");
        }
        if (StrUtil.isBlank(action)) {
            throw new BusinessException("幂等动作不能为空");
        }
        long tid = tenantId == null ? 0L : tenantId;
        String prefix = StrUtil.blankToDefault(keyPrefix, IdempotentConstants.PSI_KEY_PREFIX);
        Duration expire = ttl == null ? Duration.ofHours(IdempotentConstants.DEFAULT_TTL_HOURS) : ttl;
        String redisKey = prefix + tid + ":" + action + ":" + key;
        boolean transactional = TransactionSynchronizationManager.isActualTransactionActive();
        if (transactional && !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new BusinessException("当前事务未启用同步，无法保证幂等操作一致性");
        }
        LockLease owner = localOwners().get(redisKey);
        if (owner != null) {
            CommittedResult pending = owner.pending;
            if (transactional && pending != null && TransactionSynchronizationManager.getSynchronizations().contains(pending)) {
                return replayOrReject(replayCached, pending.result);
            }
            // Redisson 锁可同线程重入；嵌套动作或被 REQUIRES_NEW / NOT_SUPPORTED 挂起的事务仍持有该键。
            throw new BusinessException("业务处理中，请勿重复提交");
        }
        String cached = RedisUtils.get(redisKey);
        if (StrUtil.isNotBlank(cached)) {
            return replayOrReject(replayCached, cached);
        }
        String lockKey = redisKey + ":lock";
        // 默认 tryLock 的固定租期可能先于长事务结束；事务路径使用 watchdog 续租直至完成。
        try (LockLease lease = new LockLease(transactional
            ? RedisLockUtils.tryLockWithWatchdog(lockKey)
            : RedisLockUtils.tryLock(lockKey))) {
            if (!lease.lock.isLocked()) {
                throw new BusinessException("业务处理中，请勿重复提交");
            }
            cached = RedisUtils.get(redisKey);
            if (StrUtil.isNotBlank(cached)) {
                return replayOrReject(replayCached, cached);
            }
            if (transactional) {
                lease.redisKey = redisKey;
                Map<String, LockLease> owners = localOwners();
                if (!TransactionSynchronizationManager.hasResource(LOCAL_LOCK_OWNERS)) {
                    TransactionSynchronizationManager.bindResource(LOCAL_LOCK_OWNERS, owners);
                }
                owners.put(redisKey, lease);
            }
            String result = actionFn.get();
            if (transactional) {
                // actionFn 返回不等于外层 @Transactional 已提交。缓存及锁必须跟随真正的事务终点。
                lease.pending = new CommittedResult(redisKey, result, expire, lease);
                TransactionSynchronizationManager.registerSynchronization(lease.pending);
                lease.deferred = true;
            } else {
                RedisUtils.set(redisKey, result, expire);
            }
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, LockLease> localOwners() {
        Map<String, LockLease> owners = (Map<String, LockLease>)TransactionSynchronizationManager.getResource(LOCAL_LOCK_OWNERS);
        return owners == null ? new HashMap<>() : owners;
    }

    private static final class LockLease implements AutoCloseable {
        private final RedisLockUtils lock;
        private boolean deferred;
        private String redisKey;
        private CommittedResult pending;

        private LockLease(RedisLockUtils lock) {
            this.lock = lock;
        }

        @Override
        public void close() {
            if (!deferred) {
                complete();
            }
        }

        private void complete() {
            try {
                lock.close();
            } finally {
                if (redisKey != null) {
                    Map<String, LockLease> owners = localOwners();
                    owners.remove(redisKey, this);
                    if (owners.isEmpty()) {
                        TransactionSynchronizationManager.unbindResourceIfPossible(LOCAL_LOCK_OWNERS);
                    }
                }
            }
        }
    }

    private static final class CommittedResult implements TransactionSynchronization {
        private final String redisKey;
        private final String result;
        private final Duration ttl;
        private final LockLease lease;

        private CommittedResult(String redisKey, String result, Duration ttl, LockLease lease) {
            this.redisKey = redisKey;
            this.result = result;
            this.ttl = ttl;
            this.lease = lease;
        }

        @Override
        public void afterCommit() {
            try {
                RedisUtils.set(redisKey, result, ttl);
            } catch (RuntimeException e) {
                // 数据库已经提交，不可向调用方伪报回滚/失败。记录故障类型，避免异常信息泄露原幂等键。
                log.error("Committed idempotent result cache write failed; transaction remains committed [{}]", e.getClass().getSimpleName());
            }
        }

        @Override
        public void afterCompletion(int status) {
            try {
                lease.complete();
            } catch (RuntimeException e) {
                log.error("Idempotent lock release failed after transaction completion [{}]", e.getClass().getSimpleName());
            }
        }
    }

    private String replayOrReject(boolean replayCached, String cached) {
        if (replayCached) {
            return cached;
        }
        throw new BusinessException("请勿重复提交");
    }
}
