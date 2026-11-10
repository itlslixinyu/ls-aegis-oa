package com.ls.aegis.biz.eoms.mapper.customer;

import com.ls.aegis.biz.eoms.domain.Customer;

/**
 * H5 客户身份只读：按往来单位编码查 {@code psi_unit}。
 */
public interface CustomerMapper {

    /**
     * 按单位编码查询客户（H5 令牌换取）
     *
     * @param customerNo 往来单位编码 {@code psi_unit.unit_code}
     * @return 客户投影；不存在返回 null
     */
    Customer selectCustomerByCustomerNo(String customerNo);
}
