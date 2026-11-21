package com.ls.aegis.biz.psi.constant;

import com.ls.aegis.starter.support.excel.ExcelExportHelper;
import com.ls.aegis.starter.support.idempotent.IdempotentConstants;

/**
 * PSI 业务常量
 */
public final class PsiConstants {

    private PsiConstants() {
    }

    public static final String IDEMPOTENT_KEY_PREFIX = IdempotentConstants.PSI_KEY_PREFIX;
    /** 单据日序号 Redis 前缀：psi:seq:{tenant}:{prefix}:{yyMMdd} */
    public static final String DOC_SEQ_KEY_PREFIX = "psi:seq:";
    /** 主数据全局序号 Redis 前缀：psi:code:{tenant}:{prefix} */
    public static final String CODE_SEQ_KEY_PREFIX = "psi:code:";
    /** Excel 导入会话 Redis 前缀：psi:import:{key} */
    public static final String IMPORT_KEY_PREFIX = "psi:import:";
    public static final long IDEMPOTENT_TTL_HOURS = IdempotentConstants.DEFAULT_TTL_HOURS;
    public static final int EXPORT_MAX_ROWS = ExcelExportHelper.EXPORT_MAX_ROWS;

    /** 商品分类最大层级（仅一级、二级） */
    public static final int CATEGORY_MAX_LEVEL = 2;
    /** 一级分类起始编码（10、11、12…） */
    public static final long CATEGORY_ROOT_START = 10L;
    /** 一级分类步长 */
    public static final long CATEGORY_ROOT_STEP = 1L;
    /** 一级分类最大值 */
    public static final long CATEGORY_ROOT_MAX = 99L;
    /** 下级分类序号位数（挂在父编码后，如 10 → 1001、11 → 1101） */
    public static final int CATEGORY_CHILD_SEQ_WIDTH = 2;
    /** 下级分类序号最大值 */
    public static final long CATEGORY_CHILD_SEQ_MAX = 99L;
    /** 商品编码在分类码后的序号位数（0001~9999），如 10010001 */
    public static final int GOODS_CODE_SEQ_WIDTH = 4;
    /** 往来单位 / 仓库主数据全局序号位数（0001~9999） */
    public static final int MASTER_CODE_SEQ_WIDTH = 4;

    /** 主数据编码前缀（不含日期，见 DocNoGenerator#nextCode） */
    public static final String DOC_PREFIX_UNIT = "WLDW";
    public static final String DOC_PREFIX_GOODS = "SP";
    public static final String DOC_PREFIX_WAREHOUSE = "CK";
    public static final String DOC_PREFIX_STOCK_IN = "RK";
    public static final String DOC_PREFIX_STOCK_OUT = "CKD";
    public static final String DOC_PREFIX_STOCK_RETURN = "TH";
    public static final String DOC_PREFIX_STOCK_TRANSFER = "DB";
    /** 期初库存 */
    public static final String DOC_PREFIX_INIT_STOCK = "QC";
    /** 应收期初 */
    public static final String DOC_PREFIX_INIT_AR = "YS";
    /** 应付期初 */
    public static final String DOC_PREFIX_INIT_AP = "YF";
    /** 预付期初 */
    public static final String DOC_PREFIX_INIT_PREPAY = "YF2";
    /** 盘点 */
    public static final String DOC_PREFIX_STOCK_CHECK = "PD";
    /** 调整 */
    public static final String DOC_PREFIX_STOCK_ADJUST = "TZ";
    /** 回款 */
    public static final String DOC_PREFIX_RECEIPT = "HK";
    /** 付款 */
    public static final String DOC_PREFIX_PAYMENT = "FK";
    /** 销项发票 */
    public static final String DOC_PREFIX_INVOICE_OUT = "XX";
    /** 进项发票 */
    public static final String DOC_PREFIX_INVOICE_IN = "JX";
    /** 销售合同（合同销售） */
    public static final String DOC_PREFIX_CONTRACT_SALE = "HTXS";
    /** 采购合同（合同采购） */
    public static final String DOC_PREFIX_CONTRACT_PURCHASE = "HTCG";
    /** 核销 */
    public static final String DOC_PREFIX_WRITE_OFF = "ZH";
}
