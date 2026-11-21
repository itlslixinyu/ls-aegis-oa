package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.OpenItemQuery;
import com.ls.aegis.biz.psi.model.resp.OpenItemOptionResp;
import com.ls.aegis.biz.psi.model.resp.OpenItemResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.util.List;

public interface OpenItemService {
    PageResp<OpenItemResp> pageReceivable(OpenItemQuery query, PageQuery pageQuery);

    PageResp<OpenItemResp> pagePayable(OpenItemQuery query, PageQuery pageQuery);

    List<OpenItemOptionResp> openReceivables(Long unitId);

    List<OpenItemOptionResp> openPayables(Long unitId);
}
