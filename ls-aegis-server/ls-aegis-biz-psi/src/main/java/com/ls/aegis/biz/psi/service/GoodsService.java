package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.GoodsQuery;
import com.ls.aegis.biz.psi.model.req.GoodsReq;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.resp.GoodsDetailResp;
import com.ls.aegis.biz.psi.model.resp.GoodsResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.mybatis.base.service.BaseService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface GoodsService extends BaseService<GoodsResp, GoodsDetailResp, GoodsQuery, GoodsReq> {

    void downloadImportTemplate(HttpServletResponse response);

    PsiImportParseResp parseImport(MultipartFile file);

    PsiImportResp importData(PsiImportReq req);
}
