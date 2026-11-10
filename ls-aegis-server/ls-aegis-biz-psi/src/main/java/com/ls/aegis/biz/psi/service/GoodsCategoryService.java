package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.GoodsCategoryQuery;
import com.ls.aegis.biz.psi.model.req.GoodsCategoryReq;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.resp.GoodsCategoryResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.mybatis.base.service.BaseService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品分类
 */
public interface GoodsCategoryService extends BaseService<GoodsCategoryResp, GoodsCategoryResp, GoodsCategoryQuery, GoodsCategoryReq> {

    void export(GoodsCategoryQuery query, HttpServletResponse response);

    void downloadImportTemplate(HttpServletResponse response);

    PsiImportParseResp parseImport(MultipartFile file);

    PsiImportResp importData(PsiImportReq req);
}
