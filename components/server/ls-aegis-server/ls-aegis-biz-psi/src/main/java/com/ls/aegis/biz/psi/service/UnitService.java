package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.UnitQuery;
import com.ls.aegis.biz.psi.model.req.PsiImportReq;
import com.ls.aegis.biz.psi.model.req.UnitReq;
import com.ls.aegis.biz.psi.model.resp.PsiImportParseResp;
import com.ls.aegis.biz.psi.model.resp.PsiImportResp;
import com.ls.aegis.biz.psi.model.resp.UnitDetailResp;
import com.ls.aegis.biz.psi.model.resp.UnitResp;
import com.ls.aegis.mybatis.base.service.BaseService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 往来单位业务接口
 */
public interface UnitService extends BaseService<UnitResp, UnitDetailResp, UnitQuery, UnitReq> {

    void downloadImportTemplate(HttpServletResponse response);

    PsiImportParseResp parseImport(MultipartFile file);

    PsiImportResp importData(PsiImportReq req);
}
