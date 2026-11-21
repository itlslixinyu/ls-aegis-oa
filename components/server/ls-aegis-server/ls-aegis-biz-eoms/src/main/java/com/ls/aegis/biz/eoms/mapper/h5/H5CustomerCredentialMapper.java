package com.ls.aegis.biz.eoms.mapper.h5;

import com.ls.aegis.biz.eoms.domain.H5CustomerCredential;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
@com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface H5CustomerCredentialMapper {
    String COLUMNS = "id,tenant_id,customer_id,login_name,display_name,account_role,status,auth_version,password_hash";
    @Select("SELECT " + COLUMNS + " FROM eoms_h5_customer_credential WHERE login_name=#{loginName}")
    @Results(id="member",value={@Result(column="id",property="id"),@Result(column="tenant_id",property="tenantId"),
        @Result(column="customer_id",property="customerId"),@Result(column="login_name",property="loginName"),
        @Result(column="display_name",property="displayName"),@Result(column="account_role",property="accountRole"),
        @Result(column="auth_version",property="authVersion"),@Result(column="password_hash",property="passwordHash")})
    H5CustomerCredential selectByLoginName(@Param("loginName") String loginName);
    @Select("SELECT " + COLUMNS + " FROM eoms_h5_customer_credential WHERE id=#{id}")
    @ResultMap("member")
    H5CustomerCredential selectById(@Param("id") Long id);
    @Select("SELECT " + COLUMNS + " FROM eoms_h5_customer_credential WHERE tenant_id=#{tenantId} AND customer_id=#{customerId} ORDER BY id")
    @ResultMap("member")
    List<H5CustomerCredential> list(@Param("tenantId") Long tenantId,@Param("customerId") Long customerId);
    @Select("SELECT " + COLUMNS + " FROM eoms_h5_customer_credential WHERE tenant_id=#{tenantId} AND customer_id=#{customerId} ORDER BY id FOR UPDATE")
    @ResultMap("member")
    List<H5CustomerCredential> lockMembers(@Param("tenantId") Long tenantId,@Param("customerId") Long customerId);
    @Insert("INSERT INTO eoms_h5_customer_credential(tenant_id,customer_id,login_name,display_name,account_role,status,auth_version,password_hash) VALUES(#{tenantId},#{customerId},#{loginName},#{displayName},#{accountRole},1,1,#{passwordHash})")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insert(H5CustomerCredential credential);
    @Update("UPDATE eoms_h5_customer_credential SET password_hash=#{passwordHash},auth_version=auth_version+1,update_time=CURRENT_TIMESTAMP WHERE id=#{id} AND tenant_id=#{tenantId} AND customer_id=#{customerId} AND auth_version=#{authVersion}")
    int updatePassword(H5CustomerCredential credential);
    @Update("UPDATE eoms_h5_customer_credential SET display_name=#{displayName},account_role=#{accountRole},status=#{status},auth_version=auth_version+1,update_time=CURRENT_TIMESTAMP WHERE id=#{id} AND tenant_id=#{tenantId} AND customer_id=#{customerId} AND auth_version=#{authVersion}")
    int updateMember(H5CustomerCredential credential);
}
