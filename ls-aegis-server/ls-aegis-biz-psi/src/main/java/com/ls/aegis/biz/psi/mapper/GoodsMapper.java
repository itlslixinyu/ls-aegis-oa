package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.GoodsDO;
import com.ls.aegis.biz.psi.model.resp.ImportGoodsMatch;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;
import java.util.List;

@Mapper
public interface GoodsMapper extends PsiScopedMapper<GoodsDO> {
    /** 固定大小批量匹配；g上的数据权限与原selectList一致，排序规则由数据库决定。 */
    @DataPermission(tableAlias = "g")
    @Select("""
        <script>
        SELECT r.importCode, g.id, g.goods_code, g.goods_name, g.default_tax_rate
        FROM psi_goods g
        JOIN (
          <choose>
            <when test="codes != null and !codes.isEmpty()">
              <foreach collection="codes" item="code" separator=" UNION ALL ">SELECT CONCAT(#{code}, '') AS importCode</foreach>
            </when>
            <otherwise>SELECT NULL AS importCode WHERE 1=0</otherwise>
          </choose>
        ) r ON g.goods_code=r.importCode
        WHERE g.tenant_id=#{tenantId} AND g.deleted=0
        </script>
        """)
    List<ImportGoodsMatch> selectImportGoods(@Param("tenantId") Long tenantId, @Param("codes") List<String> codes);

}
