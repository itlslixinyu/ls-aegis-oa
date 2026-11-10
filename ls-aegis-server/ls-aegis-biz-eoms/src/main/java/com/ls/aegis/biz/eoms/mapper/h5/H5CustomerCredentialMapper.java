package com.ls.aegis.biz.eoms.mapper.h5;

import com.ls.aegis.biz.eoms.domain.H5CustomerCredential;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface H5CustomerCredentialMapper {

    @Select("SELECT tenant_id, customer_id, login_name, password_hash FROM eoms_h5_customer_credential WHERE login_name = #{loginName}")
    @Results({
        @Result(property = "tenantId", column = "tenant_id"),
        @Result(property = "customerId", column = "customer_id"),
        @Result(property = "loginName", column = "login_name"),
        @Result(property = "passwordHash", column = "password_hash")
    })
    H5CustomerCredential selectByLoginName(@Param("loginName") String loginName);

    @Select("SELECT tenant_id, customer_id, login_name, password_hash FROM eoms_h5_customer_credential WHERE tenant_id = #{tenantId} AND customer_id = #{customerId}")
    @Results({
        @Result(property = "tenantId", column = "tenant_id"),
        @Result(property = "customerId", column = "customer_id"),
        @Result(property = "loginName", column = "login_name"),
        @Result(property = "passwordHash", column = "password_hash")
    })
    H5CustomerCredential selectByCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);

    @Insert("INSERT INTO eoms_h5_customer_credential (tenant_id, customer_id, login_name, password_hash, create_time, update_time) VALUES (#{tenantId}, #{customerId}, #{loginName}, #{passwordHash}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
    int insert(H5CustomerCredential credential);

    @Update("UPDATE eoms_h5_customer_credential SET password_hash = #{passwordHash}, update_time = CURRENT_TIMESTAMP WHERE tenant_id = #{tenantId} AND customer_id = #{customerId} AND login_name = #{loginName}")
    int updatePassword(H5CustomerCredential credential);
}
