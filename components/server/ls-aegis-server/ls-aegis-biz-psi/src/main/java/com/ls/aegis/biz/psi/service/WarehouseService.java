package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.WarehouseQuery;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.req.WarehouseReq;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.model.resp.WarehouseDetailResp;
import com.ls.aegis.biz.psi.model.resp.WarehouseResp;
import com.ls.aegis.mybatis.base.service.BaseService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface WarehouseService extends BaseService<WarehouseResp, WarehouseDetailResp, WarehouseQuery, WarehouseReq> {

    void downloadImportTemplate(HttpServletResponse response);

    PsiImportParseResp parseImport(MultipartFile file);

    PsiImportResp importData(PsiImportReq req);
}
