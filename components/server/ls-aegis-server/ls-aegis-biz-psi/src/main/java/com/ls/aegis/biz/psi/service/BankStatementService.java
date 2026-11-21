package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.BankStatementQuery;
import com.ls.aegis.biz.psi.model.req.BankImportReq;
import com.ls.aegis.biz.psi.model.req.BankMatchReq;
import com.ls.aegis.biz.psi.model.resp.BankStatementResp;
import com.ls.aegis.biz.psi.model.resp.LabelValueResp;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.util.List;

public interface BankStatementService {
    void unmatch(Long id, com.ls.aegis.biz.psi.model.req.CashActionReq req);
    PageResp<BankStatementResp> page(BankStatementQuery query, PageQuery pageQuery);

    BankStatementResp get(Long id);

    int importStatements(BankImportReq req);

    void downloadImportTemplate(jakarta.servlet.http.HttpServletResponse response);

    com.ls.aegis.biz.psi.model.resp.BankImportParseResp parseImportExcel(org.springframework.web.multipart.MultipartFile file);

    String match(BankMatchReq req);

    /** 匹配候选业务单据下拉（按金额/日期规则过滤） */
    List<LabelValueResp> matchCandidates(Long statementId, String matchBizType);
}
