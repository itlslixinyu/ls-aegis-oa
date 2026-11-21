package com.ls.aegis.biz.psi.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.*;
import java.util.*;

/** Internal global integrity checks: callers MUST authorize master rows through ordinary PSI DP first. */
@Mapper
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface MasterDataReferenceMapper {
    @SelectProvider(type = Sql.class, method = "referenceSql")
    boolean hasReferences(@Param("kind") String kind, @Param("tenantId") Long tenantId, @Param("id") Long id);

    @SelectProvider(type = Sql.class, method = "lockSql")
    Long lockMaster(@Param("kind") String kind, @Param("tenantId") Long tenantId, @Param("id") Long id);

    final class Sql {
        public record Ref(String table, String column, boolean tenant, String soft) {}
        public static String master(String kind) { return switch (kind) {
            case "WAREHOUSE" -> "psi_warehouse"; case "GOODS" -> "psi_goods"; case "UNIT" -> "psi_unit";
            default -> throw new IllegalArgumentException("Unsupported master kind"); }; }
        public static List<Ref> refs(String kind) { return switch (kind) {
            case "WAREHOUSE" -> java.util.List.of(
                new Ref("psi_stock_batch", "warehouse_id", true, "deleted"),
                new Ref("psi_stock_flow", "warehouse_id", true, "deleted"),
                new Ref("psi_stock_in", "warehouse_id", true, "deleted"),
                new Ref("psi_stock_out", "warehouse_id", true, "deleted"),
                new Ref("psi_stock_return", "warehouse_id", true, "deleted"),
                new Ref("psi_stock_transfer", "from_warehouse_id", true, "deleted"),
                new Ref("psi_stock_transfer", "to_warehouse_id", true, "deleted"),
                new Ref("psi_init_stock", "warehouse_id", true, "deleted"),
                new Ref("psi_stock_check", "warehouse_id", true, "deleted"),
                new Ref("psi_stock_adjust", "warehouse_id", true, "deleted"),
                new Ref("eoms_repair_material", "warehouse_id", false, "del_flag"),
                new Ref("eoms_quote", "warehouse_id", false, "del_flag"),
                new Ref("eoms_m2_warehouse_map", "psi_warehouse_id", false, "")
            );
            case "GOODS" -> java.util.List.of(
                new Ref("psi_stock_batch", "goods_id", true, "deleted"),
                new Ref("psi_stock_flow", "goods_id", true, "deleted"),
                new Ref("psi_stock_in_item", "goods_id", true, "deleted"),
                new Ref("psi_stock_out_item", "goods_id", true, "deleted"),
                new Ref("psi_stock_return_item", "goods_id", true, "deleted"),
                new Ref("psi_stock_transfer_item", "goods_id", true, "deleted"),
                new Ref("psi_init_stock_item", "goods_id", true, "deleted"),
                new Ref("psi_stock_check_item", "goods_id", true, "deleted"),
                new Ref("psi_stock_adjust_item", "goods_id", true, "deleted"),
                new Ref("eoms_repair_material", "material_id", false, "del_flag"),
                new Ref("eoms_quote_detail", "goods_id", false, "del_flag"),
                new Ref("eoms_m3_goods_map", "psi_goods_id", false, "")
            );
            case "UNIT" -> java.util.List.of(
                new Ref("psi_stock_in", "unit_id", true, "deleted"),
                new Ref("psi_stock_out", "unit_id", true, "deleted"),
                new Ref("psi_receivable", "unit_id", true, "deleted"),
                new Ref("psi_payable", "unit_id", true, "deleted"),
                new Ref("psi_stock_return", "unit_id", true, "deleted"),
                new Ref("psi_init_receivable_item", "unit_id", true, "deleted"),
                new Ref("psi_init_payable_item", "unit_id", true, "deleted"),
                new Ref("psi_init_prepay_item", "unit_id", true, "deleted"),
                new Ref("psi_receipt", "unit_id", true, "deleted"),
                new Ref("psi_payment", "unit_id", true, "deleted"),
                new Ref("psi_invoice_out", "unit_id", true, "deleted"),
                new Ref("psi_invoice_in", "unit_id", true, "deleted"),
                new Ref("psi_contract_sale", "unit_id", true, "deleted"),
                new Ref("psi_contract_purchase", "unit_id", true, "deleted"),
                new Ref("psi_unit_contact", "unit_id", true, "deleted"),
                new Ref("psi_unit_invoice", "unit_id", true, "deleted"),
                new Ref("psi_unit_address", "unit_id", true, "deleted"),
                new Ref("psi_unit_pay", "unit_id", true, "deleted"),
                new Ref("eoms_repair_apply", "customer_id", true, "del_flag"),
                new Ref("eoms_repair_order", "customer_id", true, "del_flag"),
                new Ref("eoms_contract", "customer_id", true, "del_flag"),
                new Ref("eoms_income", "customer_id", false, "del_flag"),
                new Ref("eoms_receivable", "customer_id", true, "del_flag"),
                new Ref("eoms_payment", "customer_id", true, "del_flag"),
                new Ref("eoms_quote", "customer_id", false, "del_flag"),
                new Ref("eoms_m2_unit_map", "psi_unit_id", false, "")
            );
            default -> throw new IllegalArgumentException("Unsupported master kind"); }; }
        public static String lockSql(Map<String,Object> args) {
            return "SELECT id FROM " + master((String)args.get("kind")) + " WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted=0 FOR UPDATE";
        }
        public static String referenceSql(Map<String,Object> args) {
            String kind=(String)args.get("kind"); List<String> queries=new ArrayList<>();
            for (Ref r:refs(kind)) {
                String query="SELECT 1 FROM "+r.table()+" r";
                // Legacy no-tenant tables are bound back to the already authorized master identity.
                if(!r.tenant()) query+=" JOIN "+master(kind)+" owner ON owner.id=r."+r.column()+" AND owner.tenant_id=#{tenantId}";
                query+=" WHERE r."+r.column()+"=#{id}";
                if(r.tenant()) query+=" AND r.tenant_id=#{tenantId}";
                if(!r.soft().isEmpty()) query+=" AND r."+r.soft()+(r.soft().equals("deleted")?"=0":"='0'");
                queries.add(query);
            }
            return "SELECT CASE WHEN EXISTS ("+String.join(" UNION ALL ",queries)+") THEN 1 ELSE 0 END";
        }
    }
}
