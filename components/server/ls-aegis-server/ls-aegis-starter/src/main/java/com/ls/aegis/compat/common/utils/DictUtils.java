package com.ls.aegis.compat.common.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.alibaba.fastjson2.JSONArray;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ls.aegis.compat.common.constant.CacheConstants;
import com.ls.aegis.compat.common.core.domain.entity.SysDictData;
import com.ls.aegis.compat.common.core.redis.RedisCache;
import com.ls.aegis.compat.common.utils.spring.SpringUtils;

/**
 * 字典工具类
 * 
 * @author ItLs
 */
public class DictUtils
{
    /**
     * 分隔符
     */
    public static final String SEPARATOR = ",";

    /** 字典缓存 TTL（秒）：12 小时。【P0 修复】原永不过期会导致 Redis 内存无限增长。 */
    private static final int DICT_CACHE_TTL_SECONDS = 12 * 60 * 60;

    /** 【P2-13 优化】Caffeine JVM 本地一级缓存：比 Redis 省掉 ~2ms 网络 RTT，热点字典（订单状态等）命中直接纳秒级返回 */
    private static final Cache<String, List<SysDictData>> LOCAL_DICT_CACHE = Caffeine.newBuilder() //
            .maximumSize(100) // 最多 100 个字典类型（企业系统字典通常 20~50 个，足够）
            .expireAfterWrite(10, TimeUnit.MINUTES) // 10 分钟软失效，后台 admin 修改字典后 10min 前端可见
            .build();

    /**
     * 设置字典缓存
     * 
     * @param key 参数键
     * @param dictDatas 字典数据列表
     */
    public static void setDictCache(String key, List<SysDictData> dictDatas)
    {
        // 【P2-13 优化】先写 Caffeine 本地一级，再写 Redis 二级
        LOCAL_DICT_CACHE.put(key, dictDatas);
        // 【P0 修复】加 TTL，避免 Redis 内存泄漏；字典数据不常变，12h 足够，失效后下次查询自动回源 DB 刷新
        SpringUtils.getBean(RedisCache.class).setCacheObject(getCacheKey(key), dictDatas,
                DICT_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 获取字典缓存
     * 
     * @param key 参数键
     * @return dictDatas 字典数据列表
     */
    public static List<SysDictData> getDictCache(String key)
    {
        // 【P2-13 优化】先查 Caffeine 本地一级缓存（纳秒级），命不中再查 Redis（~2ms），再命不中回源 DB
        List<SysDictData> local = LOCAL_DICT_CACHE.getIfPresent(key);
        if (local != null)
        {
            return local;
        }
        JSONArray arrayCache = SpringUtils.getBean(RedisCache.class).getCacheObject(getCacheKey(key));
        if (StringUtils.isNotNull(arrayCache))
        {
            List<SysDictData> list = arrayCache.toList(SysDictData.class);
            LOCAL_DICT_CACHE.put(key, list);
            return list;
        }
        return null;
    }

    /**
     * 根据字典类型和字典值获取字典标签
     * 
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue)
    {
        if (StringUtils.isEmpty(dictValue))
        {
            return StringUtils.EMPTY;
        }
        return getDictLabel(dictType, dictValue, SEPARATOR);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     * 
     * @param dictType 字典类型
     * @param dictLabel 字典标签
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel)
    {
        if (StringUtils.isEmpty(dictLabel))
        {
            return StringUtils.EMPTY;
        }
        return getDictValue(dictType, dictLabel, SEPARATOR);
    }

    /**
     * 根据字典类型和字典值获取字典标签
     * 
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue, String separator)
    {
        List<SysDictData> datas = getDictCache(dictType);
        if (StringUtils.isNull(datas) || StringUtils.isEmpty(dictValue))
        {
            return StringUtils.EMPTY;
        }
        Map<String, String> dictMap = datas.stream().collect(HashMap::new, (map, dict) -> map.put(dict.getDictValue(), dict.getDictLabel()), Map::putAll);
        if (!StringUtils.contains(dictValue, separator))
        {
            return dictMap.getOrDefault(dictValue, StringUtils.EMPTY);
        }
        StringBuilder labelBuilder = new StringBuilder();
        for (String seperatedValue : dictValue.split(separator))
        {
            if (dictMap.containsKey(seperatedValue))
            {
                labelBuilder.append(dictMap.get(seperatedValue)).append(separator);
            }
        }
        return StringUtils.removeEnd(labelBuilder.toString(), separator);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     * 
     * @param dictType 字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel, String separator)
    {
        List<SysDictData> datas = getDictCache(dictType);
        if (StringUtils.isNull(datas) || StringUtils.isEmpty(dictLabel))
        {
            return StringUtils.EMPTY;
        }
        Map<String, String> dictMap = datas.stream().collect(HashMap::new, (map, dict) -> map.put(dict.getDictLabel(), dict.getDictValue()), Map::putAll);
        if (!StringUtils.contains(dictLabel, separator))
        {
            return dictMap.getOrDefault(dictLabel, StringUtils.EMPTY);
        }
        StringBuilder valueBuilder = new StringBuilder();
        for (String seperatedValue : dictLabel.split(separator))
        {
            if (dictMap.containsKey(seperatedValue))
            {
                valueBuilder.append(dictMap.get(seperatedValue)).append(separator);
            }
        }
        return StringUtils.removeEnd(valueBuilder.toString(), separator);
    }

    /**
     * 根据字典类型获取字典所有值
     *
     * @param dictType 字典类型
     * @return 字典值
     */
    public static String getDictValues(String dictType)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (StringUtils.isNull(datas))
        {
            return StringUtils.EMPTY;
        }
        for (SysDictData dict : datas)
        {
            propertyString.append(dict.getDictValue()).append(SEPARATOR);
        }
        return StringUtils.stripEnd(propertyString.toString(), SEPARATOR);
    }

    /**
     * 根据字典类型获取字典所有标签
     *
     * @param dictType 字典类型
     * @return 字典值
     */
    public static String getDictLabels(String dictType)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> datas = getDictCache(dictType);
        if (StringUtils.isNull(datas))
        {
            return StringUtils.EMPTY;
        }
        for (SysDictData dict : datas)
        {
            propertyString.append(dict.getDictLabel()).append(SEPARATOR);
        }
        return StringUtils.stripEnd(propertyString.toString(), SEPARATOR);
    }

    /**
     * 删除指定字典缓存
     * 
     * @param key 字典键
     */
    public static void removeDictCache(String key)
    {
        // 【P2-13 优化】同步清理 Caffeine 本地一级缓存
        LOCAL_DICT_CACHE.invalidate(key);
        SpringUtils.getBean(RedisCache.class).deleteObject(getCacheKey(key));
    }

    /**
     * 清空字典缓存
     */
    public static void clearDictCache()
    {
        // 【P2-13 优化】同步清理 Caffeine 本地一级缓存
        LOCAL_DICT_CACHE.invalidateAll();
        Collection<String> keys = SpringUtils.getBean(RedisCache.class).scanKeys(CacheConstants.SYS_DICT_KEY + "*");
        SpringUtils.getBean(RedisCache.class).deleteObject(keys);
    }

    /**
     * 设置cache key
     * 
     * @param configKey 参数键
     * @return 缓存键key
     */
    public static String getCacheKey(String configKey)
    {
        return CacheConstants.SYS_DICT_KEY + configKey;
    }
}


