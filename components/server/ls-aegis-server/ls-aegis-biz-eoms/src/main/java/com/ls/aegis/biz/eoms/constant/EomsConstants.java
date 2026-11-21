package com.ls.aegis.biz.eoms.constant;

/**
 * EOMS 系统常量定义
 *
 * @author EOMS
 */
public class EomsConstants
{
    /** ==================== Redis Key 前缀 ==================== */
    /** 编号生成器 Redis key 前缀 */
    public static final String NO_GENERATOR_KEY = "eoms:no:";

    /** H5 客户令牌 Redis key 前缀 */
    public static final String H5_TOKEN_KEY = "eoms:h5:token:";

    /** H5 令牌默认有效期（小时）：审计要求缩短至 2 小时（120 分钟） */
    public static final int H5_TOKEN_EXPIRE_HOURS = 2;

    /** H5 令牌默认有效期（分钟，供 H5TokenService 等分钟级 TTL 使用） */
    public static final int H5_TOKEN_EXPIRE_MINUTES = 120;

    /** ==================== 报修申请状态 ==================== */
    /** 待处理 */
    public static final String APPLY_STATUS_WAIT = "WAIT";
    /** 已确认 */
    public static final String APPLY_STATUS_CONFIRM = "CONFIRM";
    /** 已转工单 */
    public static final String APPLY_STATUS_ORDER = "ORDER";
    /** 已取消 */
    public static final String APPLY_STATUS_CANCEL = "CANCEL";

    /** ==================== 维修工单状态 ==================== */
    /** 待接单 */
    public static final String ORDER_STATUS_WAIT_ACCEPT = "WAIT_ACCEPT";
    /** 已接单 */
    public static final String ORDER_STATUS_ACCEPTED = "ACCEPTED";
    /** 维修中（对外统一命名 IN_PROGRESS，DB 值为 PROCESSING 以兼容历史数据；两常量等价） */
    public static final String ORDER_STATUS_IN_PROGRESS = "PROCESSING";
    /** 维修中（DB 存储值，推荐对外使用 ORDER_STATUS_IN_PROGRESS） */
    public static final String ORDER_STATUS_PROCESSING = "PROCESSING";
    /** 待结算 */
    public static final String ORDER_STATUS_WAIT_SETTLEMENT = "WAIT_SETTLEMENT";
    /** 已完成 */
    public static final String ORDER_STATUS_FINISHED = "FINISHED";
    /** 已关闭 */
    public static final String ORDER_STATUS_CLOSED = "CLOSED";

    /** ==================== 结算状态 ==================== */
    public static final String SETTLEMENT_STATUS_DRAFT = "DRAFT";
    public static final String SETTLEMENT_STATUS_CONFIRMED = "CONFIRMED";

    /** ==================== 应收账款状态 ==================== */
    public static final String RECEIVABLE_STATUS_UNPAID = "UNPAID";
    public static final String RECEIVABLE_STATUS_PARTIAL = "PARTIAL";
    public static final String RECEIVABLE_STATUS_PAID = "PAID";

    /** ==================== 库存流水业务类型 ==================== */
    public static final String STOCK_BIZ_IN = "IN";
    public static final String STOCK_BIZ_OUT = "OUT";
    public static final String STOCK_BIZ_RETURN = "RETURN";

    /** ==================== 成本类型 ==================== */
    public static final String COST_TYPE_MATERIAL = "MATERIAL";
    public static final String COST_TYPE_LABOR = "LABOR";
    public static final String COST_TYPE_OTHER = "OTHER";

    /** ==================== 合同状态（对齐销售/采购草稿确认流） ==================== */
    public static final String CONTRACT_STATUS_DRAFT = "DRAFT";
    public static final String CONTRACT_STATUS_CONFIRMED = "CONFIRMED";
    /** 历史状态，存量已迁移为 CONFIRMED */
    @Deprecated
    public static final String CONTRACT_STATUS_EFFECTIVE = "EFFECTIVE";
    public static final String CONTRACT_STATUS_EXPIRED = "EXPIRED";
    public static final String CONTRACT_STATUS_TERMINATED = "TERMINATED";

    /** ==================== 采购单状态 ==================== */
    public static final String PURCHASE_STATUS_DRAFT = "DRAFT";
    public static final String PURCHASE_STATUS_CONFIRMED = "CONFIRMED";

    /** ==================== 入库单状态 ==================== */
    public static final String INBOUND_STATUS_DRAFT = "DRAFT";
    public static final String INBOUND_STATUS_SUBMIT = "SUBMIT";

    /** ==================== 报修来源 ==================== */
    public static final String APPLY_SOURCE_H5 = "H5";
    public static final String APPLY_SOURCE_ADMIN = "ADMIN";

    /** ==================== 编号前缀（默认值，DB 配置读取失败时兜底） ==================== */
    /** 客户编号前缀（拼音首字母 KH=KeHu） */
    public static final String PREFIX_CUSTOMER = "KH";
    /** 报修编号前缀（拼音首字母 BX=BaoXiu） */
    public static final String PREFIX_REPAIR = "BX";
    /** 工单编号前缀（拼音首字母 GD=GongDan） */
    public static final String PREFIX_ORDER = "GD";
    /** 报价单编号前缀（拼音首字母 BJ=BaoJia） */
    public static final String PREFIX_QUOTE = "BJ";
    /** 维修合同编号前缀（合同维修 HTWX） */
    public static final String PREFIX_CONTRACT = "HTWX";
    /** 采购单编号前缀（拼音首字母 CG=CaiGou） */
    public static final String PREFIX_PURCHASE = "CG";
    /** 收入单编号前缀（拼音首字母 SR=ShouRu） */
    public static final String PREFIX_INCOME = "SR";
    /** 收款单编号前缀（拼音首字母 SK=ShouKuan） */
    public static final String PREFIX_PAYMENT = "SK";
    /** 应收单编号前缀（拼音首字母 YS=YingShou） */
    public static final String PREFIX_RECEIVABLE = "YS";
    /** 物料编号前缀（拼音首字母 WL=WuLiao） */
    public static final String PREFIX_MATERIAL = "WL";
    /** 物料分类编号前缀（拼音首字母 WL=WuLiaoFenLei） */
    public static final String PREFIX_CATEGORY = "WL";
    /** 结算单编号前缀（拼音首字母 JS=JieSuan） */
    public static final String PREFIX_SETTLEMENT = "JS";
    /** 仓库编号前缀（拼音首字母 CK=CangKu） */
    public static final String PREFIX_WAREHOUSE = "CK";
    /** 供应商编号前缀（拼音首字母 GYS=GongYingShang） */
    public static final String PREFIX_SUPPLIER = "GYS";
    /** 入库单编号前缀（拼音首字母 RK=RuKu） */
    public static final String PREFIX_INBOUND = "RK";

    /** ==================== 编号前缀 sys_option.code（EOMS 类别） ==================== */
    public static final String OPTION_CODE_PREFIX_CUSTOMER = "CUSTOMER_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_REPAIR = "REPAIR_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_ORDER = "ORDER_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_QUOTE = "QUOTE_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_CONTRACT = "CONTRACT_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_PURCHASE = "PURCHASE_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_INCOME = "INCOME_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_PAYMENT = "PAYMENT_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_RECEIVABLE = "RECEIVABLE_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_MATERIAL = "MATERIAL_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_CATEGORY = "CATEGORY_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_SETTLEMENT = "SETTLEMENT_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_WAREHOUSE = "WAREHOUSE_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_SUPPLIER = "SUPPLIER_NO_PREFIX";
    public static final String OPTION_CODE_PREFIX_INBOUND = "INBOUND_NO_PREFIX";

    /** ==================== sys_option 类别 ==================== */
    public static final String OPTION_CATEGORY = "EOMS";

    /** 百度地图逆地理编码 AK */
    public static final String OPTION_CODE_GEO_BAIDU_AK = "GEO_BAIDU_AK";
    /** 百度地图逆地理编码接口地址 */
    public static final String OPTION_CODE_GEO_BAIDU_REGEO_URL = "GEO_BAIDU_REGEO_URL";
    /** 现场水印缩放最长边 */
    public static final String OPTION_CODE_WATERMARK_MAX_SIDE = "WATERMARK_MAX_SIDE";
    /** H5 客户令牌有效期（分钟） */
    public static final String OPTION_CODE_H5_TOKEN_EXPIRE_MINUTES = "H5_TOKEN_EXPIRE_MINUTES";
    /** H5 令牌活跃续期（1 开启 / 0 关闭） */
    public static final String OPTION_CODE_H5_TOKEN_AUTO_REFRESH = "H5_TOKEN_AUTO_REFRESH";

    /** H5 令牌有效期下限（分钟） */
    public static final int H5_TOKEN_EXPIRE_MIN = 30;
    /** H5 令牌有效期上限（分钟） */
    public static final int H5_TOKEN_EXPIRE_MAX = 480;
    /** 百度地图逆地理编码默认接口地址 */
    public static final String DEFAULT_GEO_BAIDU_REGEO_URL = "https://api.map.baidu.com/reverse_geocoding/v3/";
    /** 水印图默认最长边 */
    public static final int DEFAULT_WATERMARK_MAX_SIDE = 1280;

    /** ==================== 错误码 ==================== */
    public static final int CODE_CUSTOMER_NOT_EXISTS = 10001;
    public static final int CODE_ORDER_NOT_EXISTS = 10002;
    public static final int CODE_STOCK_NOT_ENOUGH = 10003;
    public static final int CODE_SETTLEMENT_CONFIRMED = 10004;
    public static final int CODE_RECEIVABLE_DATA_ERROR = 10005;
    public static final int CODE_STATUS_ILLEGAL = 10006;
    public static final int CODE_H5_TOKEN_INVALID = 10007;
    public static final int CODE_STOCK_LOCK_CONFLICT = 10008;
    public static final int CODE_PURCHASE_NO_EXISTS = 10009;
    /** 报修申请不存在 */
    public static final int CODE_APPLY_NOT_EXISTS = 10010;
}
