package com.ls.aegis.starter.support.docno;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import top.continew.starter.cache.redisson.util.RedisLockUtils;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.exception.BusinessException;

import java.time.Duration;
import java.util.Date;

/**
 * 平台统一编号服务（Redis 序列 + 分布式锁）
 * <p>
 * PSI / EOMS / Quote / 签章单据共用日流水：前缀 + yyMMdd + 两位序号。Redis Key 按域隔离。
 */
@Service
public class DocNoService {

    private static final Duration DOC_SEQ_TTL = Duration.ofDays(2);
    /** 主数据序号持久保留，避免 Redis 丢键后从 1 重号 */
    private static final Duration CODE_SEQ_TTL = Duration.ofDays(3650);

    /**
     * PSI 单据号：前缀 + yyMMdd + 两位日序号
     */
    public String nextPsiDaily(Long tenantId, String prefix) {
        return nextPsiDaily(tenantId, prefix, null);
    }

    /**
     * PSI 单据号（可选 DB 种子，用于 EOMS 式历史对齐）
     */
    public String nextPsiDaily(Long tenantId, String prefix, Long dbSeed) {
        long tid = tenantId == null ? 0L : tenantId;
        return nextDaily(DocNoConstants.PSI_DOC_SEQ_PREFIX + tid + ":", prefix, dbSeed);
    }

    /**
     * EOMS 业务编号：前缀 + yyMMdd + 两位日序号
     */
    public String nextEomsDaily(String prefix, Long dbSeed) {
        return nextDaily(DocNoConstants.EOMS_NO_PREFIX, prefix, dbSeed);
    }

    /**
     * 签章项目编号：前缀 + yyMMdd + 两位日序号
     */
    public String nextSignDaily(String prefix) {
        return nextSignDaily(prefix, null);
    }

    /**
     * 签章项目编号（可选 DB 种子，Redis 丢键时按库内当日最大序号续编）
     */
    public String nextSignDaily(String prefix, Long dbSeed) {
        return nextDaily(DocNoConstants.SIGN_DOC_SEQ_PREFIX, prefix, dbSeed);
    }

    /**
     * PSI 主数据编码：前缀 + 6 位全局序号（兼容旧调用）
     */
    public String nextPsiCode(Long tenantId, String scope, String prefix) {
        return nextPsiCode(tenantId, scope, prefix, 6);
    }

    /**
     * PSI 主数据编码：前缀 + {@code seqWidth} 位全局序号（如 WLDW0001、CK0001）
     */
    public String nextPsiCode(Long tenantId, String scope, String prefix, int seqWidth) {
        long tid = tenantId == null ? 0L : tenantId;
        String redisKey = DocNoConstants.PSI_CODE_SEQ_PREFIX + tid + ":" + scope;
        return nextSeq(redisKey, prefix, seqWidth, CODE_SEQ_TTL, null);
    }

    /**
     * EOMS 物料编号：分类编码 + 4 位序号
     */
    public String nextEomsMaterial(String categoryCode, Long dbSeed) {
        if (StrUtil.isBlank(categoryCode)) {
            throw new BusinessException("分类编码为空，无法生成物料编号");
        }
        String redisKey = DocNoConstants.EOMS_NO_PREFIX + "MATERIAL:" + categoryCode;
        return nextSeq(redisKey, categoryCode, 4, DOC_SEQ_TTL, dbSeed);
    }

    /**
     * PSI 分类编码：一级 10、11、12…；下级为父编码 + 两位序号（如 10 → 1001）
     */
    public String nextPsiCategoryCode(Long tenantId,
                                      Long parentId,
                                      String parentCode,
                                      long rootStart,
                                      long rootStep,
                                      long rootMax,
                                      int childSeqWidth,
                                      long childSeqMax) {
        long tid = tenantId == null ? 0L : tenantId;
        long pid = parentId == null ? 0L : parentId;
        String redisKey = DocNoConstants.PSI_CODE_SEQ_PREFIX + tid + ":CATSEG:" + pid;
        String lockKey = redisKey + ":lock";
        try (RedisLockUtils lock = RedisLockUtils.tryLock(lockKey)) {
            if (!lock.isLocked()) {
                throw new BusinessException("编号生成繁忙，请稍后重试");
            }
            String current = RedisUtils.get(redisKey);
            if (pid == 0L) {
                long seq = StrUtil.isBlank(current) ? rootStart : Long.parseLong(current) + rootStep;
                if (seq > rootMax) {
                    throw new BusinessException("一级分类已超过上限（10~99），无法继续自动编码");
                }
                RedisUtils.set(redisKey, String.valueOf(seq), CODE_SEQ_TTL);
                return String.valueOf(seq);
            }
            if (StrUtil.isBlank(parentCode)) {
                throw new BusinessException("上级分类编码不存在，请先完善上级分类编码");
            }
            long seq = StrUtil.isBlank(current) ? 1L : Long.parseLong(current) + 1L;
            if (seq > childSeqMax) {
                throw new BusinessException("同级下级分类已超过上限（01~99），无法继续自动编码");
            }
            RedisUtils.set(redisKey, String.valueOf(seq), CODE_SEQ_TTL);
            return parentCode + String.format("%0" + childSeqWidth + "d", seq);
        }
    }

    /**
     * PSI 商品编码：分类编码 + 序号
     */
    public String nextPsiGoodsCode(Long tenantId, String categoryCode, int seqWidth) {
        if (StrUtil.isBlank(categoryCode)) {
            throw new BusinessException("分类编码为空，无法生成商品编号");
        }
        long tid = tenantId == null ? 0L : tenantId;
        String redisKey = DocNoConstants.PSI_CODE_SEQ_PREFIX + tid + ":GOODS:" + categoryCode;
        return nextSeq(redisKey, categoryCode, seqWidth, CODE_SEQ_TTL, null);
    }

    /**
     * 日流水：{@code redisKeyPrefix}{prefix}:{yyMMdd} → 前缀 + yyMMdd + 01…99
     */
    private String nextDaily(String redisKeyPrefix, String prefix, Long dbSeed) {
        String day = DateUtil.format(new Date(), DocNoConstants.DATE_PATTERN);
        String redisKey = redisKeyPrefix + prefix + ":" + day;
        return nextSeq(redisKey, prefix + day, DocNoConstants.DAILY_SEQ_WIDTH, DOC_SEQ_TTL, dbSeed);
    }

    private String nextSeq(String redisKey, String numberPrefix, int width, Duration ttl, Long dbSeed) {
        long seq = nextRawSeq(redisKey, ttl, dbSeed);
        long max = (long) Math.pow(10, width) - 1;
        if (seq > max) {
            if (width == DocNoConstants.DAILY_SEQ_WIDTH) {
                throw new BusinessException("当日编号已用尽（最多 99 张），请联系管理员");
            }
            throw new BusinessException("编号序列已用尽，请联系管理员");
        }
        return numberPrefix + String.format("%0" + width + "d", seq);
    }

    private long nextRawSeq(String redisKey, Duration ttl, Long dbSeed) {
        String lockKey = redisKey + ":lock";
        try (RedisLockUtils lock = RedisLockUtils.tryLock(lockKey)) {
            if (!lock.isLocked()) {
                throw new BusinessException("编号生成繁忙，请稍后重试");
            }
            String current = RedisUtils.get(redisKey);
            long seq;
            if (StrUtil.isBlank(current)) {
                seq = (dbSeed != null && dbSeed > 0) ? dbSeed + 1 : 1L;
            } else {
                seq = Long.parseLong(current) + 1L;
                if (dbSeed != null && dbSeed > 0 && seq <= dbSeed) {
                    seq = dbSeed + 1;
                }
            }
            RedisUtils.set(redisKey, String.valueOf(seq), ttl);
            return seq;
        }
    }
}
