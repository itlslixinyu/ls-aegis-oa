package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.CompanyAccountQuery;
import com.ls.aegis.biz.psi.model.req.CompanyAccountReq;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.resp.CompanyAccountDetailResp;
import com.ls.aegis.biz.psi.model.resp.CompanyAccountResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.mybatis.base.service.BaseService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CompanyAccountService extends BaseService<CompanyAccountResp, CompanyAccountDetailResp, CompanyAccountQuery, CompanyAccountReq> {

    void downloadImportTemplate(HttpServletResponse response);

    PsiImportParseResp parseImport(MultipartFile file);

    PsiImportResp importData(PsiImportReq req);
}
