package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.req.UnitAddressReq;
import com.ls.aegis.biz.psi.model.req.UnitContactReq;
import com.ls.aegis.biz.psi.model.req.UnitInvoiceReq;
import com.ls.aegis.biz.psi.model.req.UnitPayReq;
import com.ls.aegis.biz.psi.model.resp.UnitAddressResp;
import com.ls.aegis.biz.psi.model.resp.UnitContactResp;
import com.ls.aegis.biz.psi.model.resp.UnitInvoiceResp;
import com.ls.aegis.biz.psi.model.resp.UnitPayResp;

import java.util.List;

/**
 * 往来单位子表（联系人 / 开票 / 地址 / 付款账户）
 */
public interface UnitSubService {

    List<UnitContactResp> listContacts(Long unitId);

    UnitContactResp getContact(Long unitId, Long id);

    Long createContact(Long unitId, UnitContactReq req);

    void updateContact(Long unitId, Long id, UnitContactReq req);

    void deleteContact(Long unitId, Long id);

    List<UnitInvoiceResp> listInvoices(Long unitId);

    UnitInvoiceResp getInvoice(Long unitId, Long id);

    Long createInvoice(Long unitId, UnitInvoiceReq req);

    void updateInvoice(Long unitId, Long id, UnitInvoiceReq req);

    void deleteInvoice(Long unitId, Long id);

    List<UnitAddressResp> listAddresses(Long unitId);

    UnitAddressResp getAddress(Long unitId, Long id);

    Long createAddress(Long unitId, UnitAddressReq req);

    void updateAddress(Long unitId, Long id, UnitAddressReq req);

    void deleteAddress(Long unitId, Long id);

    List<UnitPayResp> listPays(Long unitId);

    UnitPayResp getPay(Long unitId, Long id);

    Long createPay(Long unitId, UnitPayReq req);

    void updatePay(Long unitId, Long id, UnitPayReq req);

    void deletePay(Long unitId, Long id);
}
