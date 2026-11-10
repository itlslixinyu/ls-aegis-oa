package com.ls.aegis.biz.psi.support;

import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.starter.support.docno.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PSI 编号门面（委托平台 {@link DocNoService}）
 */
@Component
@RequiredArgsConstructor
public class DocNoGenerator {

    private final DocNoService docNoService;

    /**
     * 生成单据号：前缀 + yyMMdd + 两位日序号
     */
    public String next(Long tenantId, String prefix) {
        return docNoService.nextPsiDaily(tenantId, prefix);
    }

    /**
     * 生成主数据编码：前缀 + 4 位全局序号（往来单位 WLDW0001、仓库 CK0001）
     */
    public String nextCode(Long tenantId, String prefix) {
        return docNoService.nextPsiCode(tenantId, prefix, prefix, PsiConstants.MASTER_CODE_SEQ_WIDTH);
    }

    /**
     * 生成分类编码：一级 10、11、12…；下级为父编码 + 两位序号（如 10 → 1001、11 → 1101）。
     */
    public String nextCategoryCode(Long tenantId, Long parentId, String parentCode) {
        return docNoService.nextPsiCategoryCode(tenantId, parentId, parentCode,
            PsiConstants.CATEGORY_ROOT_START,
            PsiConstants.CATEGORY_ROOT_STEP,
            PsiConstants.CATEGORY_ROOT_MAX,
            PsiConstants.CATEGORY_CHILD_SEQ_WIDTH,
            PsiConstants.CATEGORY_CHILD_SEQ_MAX);
    }

    /**
     * 生成商品编码：分类编码 + 4 位序号，如 10010001、11010001
     */
    public String nextGoodsCode(Long tenantId, String categoryCode) {
        return docNoService.nextPsiGoodsCode(tenantId, categoryCode, PsiConstants.GOODS_CODE_SEQ_WIDTH);
    }
}
