package com.ls.aegis.biz.psi.service;

import com.ls.aegis.biz.psi.model.query.PrintTemplateQuery;
import com.ls.aegis.biz.psi.model.req.PrintTemplateReq;
import com.ls.aegis.biz.psi.model.resp.PrintRenderResp;
import com.ls.aegis.biz.psi.model.resp.PrintTemplateDetailResp;
import com.ls.aegis.biz.psi.model.resp.PrintTemplateResp;
import com.ls.aegis.mybatis.base.service.BaseService;

/**
 * 打印模板服务
 */
public interface PrintTemplateService extends BaseService<PrintTemplateResp, PrintTemplateDetailResp, PrintTemplateQuery, PrintTemplateReq> {

    /**
     * 渲染打印 HTML
     *
     * @param bizType    业务类型
     * @param bizId      业务单据 ID
     * @param templateId 模板 ID（可选，缺省取默认或唯一启用）
     * @return 标题与 HTML
     */
    PrintRenderResp render(String bizType, Long bizId, Long templateId);

    /**
     * Word(.docx) 套打：有 fileId 则读附件模板并替换占位符；否则用内置最小 docx。
     *
     * @param bizType    业务类型
     * @param bizId      业务单据 ID
     * @param templateId 模板 ID（可选）
     * @return docx 二进制
     */
    byte[] renderDocx(String bizType, Long bizId, Long templateId);
}
