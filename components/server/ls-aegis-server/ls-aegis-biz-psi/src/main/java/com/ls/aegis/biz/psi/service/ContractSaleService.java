package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.ContractQuery;
import com.ls.aegis.biz.psi.model.req.ContractReq;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.resp.ContractResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

public interface ContractSaleService {
    PageResp<ContractResp> page(ContractQuery query, PageQuery pageQuery);
    ContractResp get(Long id);
    Long create(ContractReq req);
    void update(ContractReq req, Long id);
    void delete(Long id);
    String confirm(Long id, DocConfirmReq req);
    ContractResp progress(Long id);
    void bindProject(Long id, Long projectId);
}
