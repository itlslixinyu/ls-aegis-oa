package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.StockBatchDO;
import com.ls.aegis.biz.psi.model.resp.StockQueryResp;
import com.ls.aegis.biz.psi.model.resp.StockAlertQuantityResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Options;
import top.continew.starter.data.mapper.BaseMapper;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存批次 Mapper
 */
@Mapper
public interface StockBatchMapper extends BaseMapper<StockBatchDO> {

    /**
     * Goods IDs have already passed GoodsMapper data permission. Batches have no dept_id;
     * authorize their warehouse via the same platform department/self/custom scope instead.
     * Keep aggregation in SQL so result transfer is bounded by watched goods, not batch count.
     */
    @DataPermission(tableAlias = "w")
    @Select("""
        <script>
        SELECT b.goods_id AS goodsId, SUM(b.remain_quantity) AS remainQuantity
        FROM psi_stock_batch b
        INNER JOIN psi_warehouse w ON w.id = b.warehouse_id AND w.tenant_id = b.tenant_id AND w.deleted = 0
        WHERE b.tenant_id = #{tenantId} AND b.deleted = 0 AND b.remain_quantity &gt; 0
        <choose>
          <when test="goodsIds != null and !goodsIds.isEmpty()">
            AND b.goods_id IN
            <foreach collection="goodsIds" item="goodsId" open="(" separator="," close=")">#{goodsId}</foreach>
          </when>
          <otherwise>AND 1 = 0</otherwise>
        </choose>
        GROUP BY b.goods_id
        </script>
        """)
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    List<StockAlertQuantityResp> selectAlertQuantities(@Param("tenantId") Long tenantId,
                                                      @Param("goodsIds") List<Long> goodsIds);

    /** Current read of all batches (including exhausted batches), after the dimension anchor is locked. */
    @Select("""
        SELECT * FROM psi_stock_batch
        WHERE tenant_id=#{tenantId} AND warehouse_id=#{warehouseId} AND goods_id=#{goodsId} AND deleted=0
        FOR UPDATE
        """)
    List<StockBatchDO> selectAllForUpdate(@Param("tenantId") Long tenantId,
                                         @Param("warehouseId") Long warehouseId,
                                         @Param("goodsId") Long goodsId);

    /**
     * 锁定可用批次。
     * <p>
     * 不在 SQL 里写 {@code ORDER BY ... FOR UPDATE}：JSQLParser 会重排成
     * {@code FOR UPDATE ORDER BY}，MySQL 语法错误。FIFO 顺序在 Java 侧排序。
     */
    @Select("""
        SELECT * FROM psi_stock_batch
        WHERE tenant_id = #{tenantId}
          AND warehouse_id = #{warehouseId}
          AND goods_id = #{goodsId}
          AND remain_quantity > 0
          AND deleted = 0
        FOR UPDATE
        """)
    List<StockBatchDO> selectAvailableForUpdate(@Param("tenantId") Long tenantId,
                                                @Param("warehouseId") Long warehouseId,
                                                @Param("goodsId") Long goodsId);

    /**
     * 锁定指定来源单据产生的可用批次（FIFO 顺序在 Java 侧）
     */
    @Select("""
        SELECT * FROM psi_stock_batch
        WHERE tenant_id = #{tenantId}
          AND warehouse_id = #{warehouseId}
          AND goods_id = #{goodsId}
          AND source_type = #{sourceType}
          AND source_id = #{sourceId}
          AND remain_quantity > 0
          AND deleted = 0
        FOR UPDATE
        """)
    List<StockBatchDO> selectAvailableBySourceForUpdate(@Param("tenantId") Long tenantId,
                                                        @Param("warehouseId") Long warehouseId,
                                                        @Param("goodsId") Long goodsId,
                                                        @Param("sourceType") String sourceType,
                                                        @Param("sourceId") Long sourceId);

    /**
     * 按主键锁定批次（强制租户，防跨租户旁路）
     */
    @Select("SELECT * FROM psi_stock_batch WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 FOR UPDATE")
    StockBatchDO selectByIdForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);

    /**
     * 汇总当前可用数量
     */
    @Select("""
        SELECT COALESCE(SUM(remain_quantity), 0) FROM psi_stock_batch
        WHERE tenant_id = #{tenantId}
          AND warehouse_id = #{warehouseId}
          AND goods_id = #{goodsId}
          AND deleted = 0
        """)
    BigDecimal sumRemainQuantity(@Param("tenantId") Long tenantId,
                                 @Param("warehouseId") Long warehouseId,
                                 @Param("goodsId") Long goodsId);

    /**
     * 汇总当前可用金额
     */
    @Select("""
        SELECT COALESCE(SUM(remain_amount), 0) FROM psi_stock_batch
        WHERE tenant_id = #{tenantId}
          AND warehouse_id = #{warehouseId}
          AND goods_id = #{goodsId}
          AND deleted = 0
        """)
    BigDecimal sumRemainAmount(@Param("tenantId") Long tenantId,
                               @Param("warehouseId") Long warehouseId,
                               @Param("goodsId") Long goodsId);

    /**
     * 按仓+商品汇总剩余库存（分页）
     */
    @Select("""
        <script>
        SELECT b.warehouse_id AS warehouseId,
               b.goods_id AS goodsId,
               SUM(b.remain_quantity) AS remainQuantity,
               SUM(b.remain_amount) AS remainAmount
        FROM psi_stock_batch b
        WHERE b.deleted = 0
          AND b.tenant_id = #{tenantId}
          <if test="onlyPositive">
            AND b.remain_quantity &gt; 0
          </if>
          AND b.warehouse_id IN
          <foreach collection="warehouseIds" item="wid" open="(" separator="," close=")">
            #{wid}
          </foreach>
          <if test="goodsId != null">
            AND b.goods_id = #{goodsId}
          </if>
        GROUP BY b.warehouse_id, b.goods_id
        <if test="onlyPositive">
          HAVING SUM(b.remain_quantity) &gt; 0
        </if>
        ORDER BY b.warehouse_id ASC, b.goods_id ASC
        LIMIT #{offset}, #{limit}
        </script>
        """)
    List<StockQueryResp> selectBalancePage(@Param("tenantId") Long tenantId,
                                           @Param("warehouseIds") List<Long> warehouseIds,
                                           @Param("goodsId") Long goodsId,
                                           @Param("onlyPositive") boolean onlyPositive,
                                           @Param("offset") long offset,
                                           @Param("limit") long limit);

    /**
     * 按仓+商品汇总剩余库存（总数）
     */
    @Select("""
        <script>
        SELECT COUNT(1) FROM (
          SELECT 1
          FROM psi_stock_batch b
          WHERE b.deleted = 0
            AND b.tenant_id = #{tenantId}
            <if test="onlyPositive">
              AND b.remain_quantity &gt; 0
            </if>
            AND b.warehouse_id IN
            <foreach collection="warehouseIds" item="wid" open="(" separator="," close=")">
              #{wid}
            </foreach>
            <if test="goodsId != null">
              AND b.goods_id = #{goodsId}
            </if>
          GROUP BY b.warehouse_id, b.goods_id
          <if test="onlyPositive">
            HAVING SUM(b.remain_quantity) &gt; 0
          </if>
        ) t
        </script>
        """)
    long countBalance(@Param("tenantId") Long tenantId,
                      @Param("warehouseIds") List<Long> warehouseIds,
                      @Param("goodsId") Long goodsId,
                      @Param("onlyPositive") boolean onlyPositive);
}
