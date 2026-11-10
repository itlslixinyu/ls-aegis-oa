package com.ls.aegis.biz.psi.mapper;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import org.apache.ibatis.annotations.*;

/**
 * 内部不变量读取：调用前必须先授权并锁定源资金/退货。
 * 显式tenant+精确业务ID覆盖全部部门，不能让操作人的列表权限隐藏已发生的退款或银行占用。
 * 这些方法不直接暴露为HTTP查询，不改变平台页面的数据权限。
 */
@Mapper
@com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface CashLedgerMapper {
    @Select("SELECT * FROM psi_receivable WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    com.ls.aegis.biz.psi.model.entity.ReceivableDO lockReceivableForCash(@Param("tenantId") Long tenantId,@Param("id") Long id);
    @Select("SELECT * FROM psi_payable WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    com.ls.aegis.biz.psi.model.entity.PayableDO lockPayableForCash(@Param("tenantId") Long tenantId,@Param("id") Long id);
    @Select("SELECT * FROM psi_receivable WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0")
    com.ls.aegis.biz.psi.model.entity.ReceivableDO receivableForCash(@Param("tenantId") Long tenantId,@Param("id") Long id);
    @Select("SELECT * FROM psi_payable WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0")
    com.ls.aegis.biz.psi.model.entity.PayableDO payableForCash(@Param("tenantId") Long tenantId,@Param("id") Long id);
    @Select("SELECT id FROM psi_stock_out WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    Long lockCashStockOut(@Param("tenantId") Long tenantId,@Param("id") Long id);
    @Select("SELECT id FROM psi_stock_in WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    Long lockCashStockIn(@Param("tenantId") Long tenantId,@Param("id") Long id);
    @Update("UPDATE psi_receivable SET written_off_amount=#{written},unwritten_amount=#{unwritten},doc_status=#{status},update_user=#{actor},update_time=CURRENT_TIMESTAMP WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 AND written_off_amount=#{oldWritten} AND unwritten_amount=#{oldUnwritten}")
    int applyCashReceivable(@Param("tenantId")Long tenantId,@Param("id")Long id,@Param("written")BigDecimal written,@Param("unwritten")BigDecimal unwritten,@Param("status")String status,@Param("actor")Long actor,@Param("oldWritten")BigDecimal oldWritten,@Param("oldUnwritten")BigDecimal oldUnwritten);
    @Update("UPDATE psi_payable SET written_off_amount=#{written},unwritten_amount=#{unwritten},doc_status=#{status},update_user=#{actor},update_time=CURRENT_TIMESTAMP WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 AND written_off_amount=#{oldWritten} AND unwritten_amount=#{oldUnwritten}")
    int applyCashPayable(@Param("tenantId")Long tenantId,@Param("id")Long id,@Param("written")BigDecimal written,@Param("unwritten")BigDecimal unwritten,@Param("status")String status,@Param("actor")Long actor,@Param("oldWritten")BigDecimal oldWritten,@Param("oldUnwritten")BigDecimal oldUnwritten);
    @Update("UPDATE psi_stock_out SET pay_status=#{status},update_user=#{actor},update_time=CURRENT_TIMESTAMP WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0")
    int cashStockOutPayStatus(@Param("tenantId")Long tenantId,@Param("id")Long id,@Param("status")String status,@Param("actor")Long actor);
    @Update("UPDATE psi_stock_in SET pay_status=#{status},update_user=#{actor},update_time=CURRENT_TIMESTAMP WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0")
    int cashStockInPayStatus(@Param("tenantId")Long tenantId,@Param("id")Long id,@Param("status")String status,@Param("actor")Long actor);
    @Select("SELECT * FROM psi_receivable WHERE tenant_id=#{tenantId} AND id=#{id} AND source_type='STOCK_OUT' AND deleted=0")
    com.ls.aegis.biz.psi.model.entity.ReceivableDO receivableForInvariant(@Param("tenantId") Long tenantId, @Param("id") Long id);
    @Select("SELECT * FROM psi_payable WHERE tenant_id=#{tenantId} AND id=#{id} AND source_type='STOCK_IN' AND deleted=0")
    com.ls.aegis.biz.psi.model.entity.PayableDO payableForInvariant(@Param("tenantId") Long tenantId, @Param("id") Long id);
    @Select("SELECT * FROM psi_receivable WHERE tenant_id=#{tenantId} AND source_type='STOCK_OUT' AND source_id=#{originId} AND deleted=0")
    com.ls.aegis.biz.psi.model.entity.ReceivableDO receivableForOrigin(@Param("tenantId") Long tenantId, @Param("originId") Long originId);
    @Select("SELECT * FROM psi_payable WHERE tenant_id=#{tenantId} AND source_type='STOCK_IN' AND source_id=#{originId} AND deleted=0")
    com.ls.aegis.biz.psi.model.entity.PayableDO payableForOrigin(@Param("tenantId") Long tenantId, @Param("originId") Long originId);
    @Select("""
        <script>
        SELECT * FROM psi_write_off_item WHERE tenant_id=#{tenantId} AND deleted=0
        <choose><when test="receipt">AND open_type='AR'
          <if test="sourceId != null">AND receipt_id=#{sourceId}</if>
        </when><otherwise>AND open_type='AP'
          <if test="sourceId != null">AND payment_id=#{sourceId}</if>
        </otherwise></choose>
        <if test="openId != null">AND open_id=#{openId}</if>
        </script>
        """)
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    List<com.ls.aegis.biz.psi.model.entity.WriteOffItemDO> allocationItems(@Param("tenantId") Long tenantId,
        @Param("receipt") boolean receipt, @Param("sourceId") Long sourceId, @Param("openId") Long openId);
    @Select("""
        <script>
        SELECT * FROM psi_stock_return WHERE tenant_id=#{tenantId} AND deleted=0
        <choose><when test="receipt">AND return_type='SALE_RETURN' AND origin_stock_out_id=#{originId}</when>
        <otherwise>AND return_type='PURCHASE_RETURN' AND origin_stock_in_id=#{originId}</otherwise></choose>
        </script>
        """)
    List<com.ls.aegis.biz.psi.model.entity.StockReturnDO> returnsForOrigin(@Param("tenantId") Long tenantId,
        @Param("receipt") boolean receipt, @Param("originId") Long originId);
    @Select("SELECT COUNT(*) FROM psi_todo WHERE tenant_id=#{tenantId} AND biz_id=#{returnId} AND todo_type=#{kind} AND deleted=0")
    long refundTodoCount(@Param("tenantId") Long tenantId, @Param("returnId") Long returnId, @Param("kind") String kind);
    @Insert("""
        INSERT INTO psi_todo(id,tenant_id,dept_id,todo_type,title,biz_type,biz_id,biz_no,done,create_user,create_time,deleted)
        VALUES(#{id},#{tenantId},#{deptId},#{kind},#{title},#{bizType},#{returnId},#{bizNo},FALSE,#{actor},CURRENT_TIMESTAMP,0)
        """)
    int createRefundTodo(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("deptId") Long deptId,
        @Param("kind") String kind, @Param("title") String title, @Param("bizType") String bizType,
        @Param("returnId") Long returnId, @Param("bizNo") String bizNo, @Param("actor") Long actor);
    @Select("SELECT * FROM psi_receivable WHERE tenant_id=#{tenantId} AND source_type='STOCK_OUT' AND source_id=#{originId} AND deleted=0 FOR UPDATE")
    com.ls.aegis.biz.psi.model.entity.ReceivableDO lockReturnReceivable(@Param("tenantId") Long tenantId, @Param("originId") Long originId);
    @Select("SELECT * FROM psi_payable WHERE tenant_id=#{tenantId} AND source_type='STOCK_IN' AND source_id=#{originId} AND deleted=0 FOR UPDATE")
    com.ls.aegis.biz.psi.model.entity.PayableDO lockReturnPayable(@Param("tenantId") Long tenantId, @Param("originId") Long originId);
    @Update("UPDATE psi_receivable SET tax_inclusive_amount=#{amount},unwritten_amount=#{unwritten},doc_status=#{status},update_user=#{actor},update_time=CURRENT_TIMESTAMP WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0")
    int applyReturnReceivable(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("amount") BigDecimal amount,
        @Param("unwritten") BigDecimal unwritten, @Param("status") String status, @Param("actor") Long actor);
    @Update("UPDATE psi_payable SET tax_inclusive_amount=#{amount},unwritten_amount=#{unwritten},doc_status=#{status},update_user=#{actor},update_time=CURRENT_TIMESTAMP WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0")
    int applyReturnPayable(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("amount") BigDecimal amount,
        @Param("unwritten") BigDecimal unwritten, @Param("status") String status, @Param("actor") Long actor);
    @Select("SELECT * FROM psi_receipt WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    com.ls.aegis.biz.psi.model.entity.ReceiptDO receiptForInvariant(@Param("tenantId") Long tenantId, @Param("id") Long id);
    @Select("SELECT * FROM psi_payment WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    com.ls.aegis.biz.psi.model.entity.PaymentDO paymentForInvariant(@Param("tenantId") Long tenantId, @Param("id") Long id);
    @Select("SELECT COALESCE(SUM(amount),0) FROM psi_payment WHERE tenant_id=#{tenantId} AND refund_return_id=#{returnId} AND doc_status='CONFIRMED' AND deleted=0")
    BigDecimal sumRefundPayments(@Param("tenantId") Long tenantId, @Param("returnId") Long returnId);
    @Select("SELECT COALESCE(SUM(amount),0) FROM psi_receipt WHERE tenant_id=#{tenantId} AND refund_return_id=#{returnId} AND doc_status='CONFIRMED' AND deleted=0")
    BigDecimal sumRefundReceipts(@Param("tenantId") Long tenantId, @Param("returnId") Long returnId);
    @Select("""
        SELECT c.id,c.amount,c.source_sequence,c.void_sequence,c.doc_status,c.refund_return_id FROM psi_payment c
        JOIN psi_stock_return r ON r.id=c.refund_return_id AND r.tenant_id=c.tenant_id AND r.deleted=0
        WHERE c.tenant_id=#{tenantId} AND c.refund_source_id=#{sourceId} AND r.origin_stock_out_id=#{originId} AND c.deleted=0
        """)
    List<RefundHistory> refundPayments(@Param("tenantId") Long tenantId, @Param("sourceId") Long sourceId, @Param("originId") Long originId);
    @Select("""
        SELECT c.id,c.amount,c.source_sequence,c.void_sequence,c.doc_status,c.refund_return_id FROM psi_receipt c
        JOIN psi_stock_return r ON r.id=c.refund_return_id AND r.tenant_id=c.tenant_id AND r.deleted=0
        WHERE c.tenant_id=#{tenantId} AND c.refund_source_id=#{sourceId} AND r.origin_stock_in_id=#{originId} AND c.deleted=0
        """)
    List<RefundHistory> refundReceipts(@Param("tenantId") Long tenantId, @Param("sourceId") Long sourceId, @Param("originId") Long originId);
    @Select("SELECT COUNT(*) FROM psi_bank_statement WHERE tenant_id=#{tenantId} AND match_biz_type=#{kind} AND match_biz_id=#{id} AND match_status='MATCHED' AND deleted=0")
    long bankMatches(@Param("tenantId") Long tenantId, @Param("kind") String kind, @Param("id") Long id);
    @Update("UPDATE psi_todo SET done=#{done},update_time=CURRENT_TIMESTAMP,update_user=#{actor} WHERE tenant_id=#{tenantId} AND biz_id=#{returnId} AND todo_type=#{kind} AND deleted=0")
    int setRefundTodo(@Param("tenantId") Long tenantId, @Param("returnId") Long returnId, @Param("kind") String kind,
        @Param("done") boolean done, @Param("actor") Long actor);

    @Data
    class RefundHistory {
        private Long id, sourceSequence, voidSequence, refundReturnId;
        private BigDecimal amount;
        private String docStatus;
    }
}
