package com.ls.aegis.common.api.psi;

/** PSI owns customer identity. Public H5 must supply a server-issued tenant/customer capability. */
public interface H5CustomerAccessApi {
    /** Staff call: ordinary PSI tenant and data permission chain must authorize this exact customer. */
    Customer requireManagedCustomer(Long customerId);
    /** Internal capability/session call only: explicit tenant and ID, never a customer number. */
    Customer requireAvailableCustomer(Long tenantId, Long customerId);
    record Customer(Long customerId, Long tenantId, String customerNo, String customerName) {}
}
