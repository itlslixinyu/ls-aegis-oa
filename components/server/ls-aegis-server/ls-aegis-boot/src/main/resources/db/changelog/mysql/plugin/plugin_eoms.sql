-- liquibase formatted sql

-- changeset aegis:plugin_eoms_schema
-- validCheckSum: ANY
-- comment P5：EOMS 业务表 DDL（自 eoms_init 提炼，无 DROP、无若依菜单/字典；已折叠 photos/签到字段）
SET NAMES utf8mb4;

-- =====================================================
-- ITLS-EOMS 维修服务管理系统 数据库初始化脚本
-- 版本：V1.1
-- 说明：包含 22 张业务表 DDL + 字典 + 菜单 + 角色初始化
-- 字符集：utf8mb4 / 排序：utf8mb4_general_ci
-- =====================================================


-- =====================================================
-- 【T2-1】22 张 eoms_ 业务表 DDL
-- 所有表含完整公共字段（日志/流水/附件表按规范省略 del_flag）
-- =====================================================

-- ---------- 5.1 客户模块（M2 已并入 psi_unit）----------
-- eoms_customer / eoms_customer_contact → psi_unit / psi_unit_contact
-- 业务表 customer_id 语义：psi_unit.id

-- ---------- 5.2 报修模块 ----------

-- 报修申请表
CREATE TABLE IF NOT EXISTS `eoms_repair_apply` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  repair_no       varchar(32)     NOT NULL                COMMENT '报修编号，唯一',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  contact_name    varchar(50)     DEFAULT NULL            COMMENT '联系人姓名',
  contact_phone   varchar(30)     DEFAULT NULL            COMMENT '联系电话',
  fault_type      varchar(32)     DEFAULT NULL            COMMENT '故障类型',
  fault_desc      text            DEFAULT NULL            COMMENT '故障描述',
  photos          varchar(1000)   DEFAULT NULL            COMMENT '现场照片URL（多个逗号分隔，带水印）',
  urgent_level    varchar(32)     DEFAULT 'NORMAL'        COMMENT '紧急程度',
  contract_id     bigint(20)      DEFAULT NULL            COMMENT '关联合同ID',
  status          varchar(32)     NOT NULL DEFAULT 'WAIT' COMMENT '状态：WAIT-待处理、CONFIRM-已确认、ORDER-已转工单、CANCEL-已取消',
  apply_time      datetime        DEFAULT NULL            COMMENT '报修时间',
  source          varchar(20)     NOT NULL DEFAULT 'H5'   COMMENT '报修来源：H5-客户H5、ADMIN-后台登记',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_repair_no (repair_no),
  KEY idx_customer_id (customer_id),
  KEY idx_status (status),
  KEY idx_urgent_level (urgent_level),
  KEY idx_apply_time (apply_time),
  KEY idx_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报修申请表';

-- ---------- 5.3 维修模块 ----------

-- 维修订单表
CREATE TABLE IF NOT EXISTS `eoms_repair_order` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键，工单ID',
  order_no        varchar(32)     NOT NULL                COMMENT '维修单号，唯一',
  apply_id        bigint(20)      DEFAULT NULL            COMMENT '关联报修申请ID',
  quote_id        bigint(20)      DEFAULT NULL            COMMENT '关联报价单ID',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  engineer_id     bigint(20)      DEFAULT NULL            COMMENT '工程师ID，关联sys_user.user_id',
  status          varchar(32)     NOT NULL DEFAULT 'WAIT_ACCEPT' COMMENT '状态：WAIT_ACCEPT-待接单、ACCEPTED-已接单、PROCESSING-维修中、WAIT_SETTLEMENT-待结算、FINISHED-已完成、CLOSED-已关闭',
  accept_time     datetime        DEFAULT NULL            COMMENT '接单时间',
  start_time      datetime        DEFAULT NULL            COMMENT '开始维修时间',
  finish_time     datetime        DEFAULT NULL            COMMENT '完成时间',
  address         varchar(500)    DEFAULT NULL            COMMENT '维修地址',
  photos          varchar(1000)   DEFAULT NULL            COMMENT '现场照片URL（多个逗号分隔，带水印）',
  sign_in_longitude  varchar(32)  DEFAULT NULL            COMMENT '签到经度（开始维修时定位）',
  sign_in_latitude   varchar(32)  DEFAULT NULL            COMMENT '签到纬度（开始维修时定位）',
  sign_in_address    varchar(500) DEFAULT NULL            COMMENT '签到地址（逆地理编码中文地址）',
  sign_out_longitude varchar(32)  DEFAULT NULL            COMMENT '签退经度（完成维修时定位）',
  sign_out_latitude  varchar(32)  DEFAULT NULL            COMMENT '签退纬度（完成维修时定位）',
  sign_out_address   varchar(500) DEFAULT NULL            COMMENT '签退地址（逆地理编码中文地址）',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_apply_id (apply_id),
  KEY idx_customer_id (customer_id),
  KEY idx_engineer_id (engineer_id),
  KEY idx_status (status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修订单表';

-- 维修记录表
CREATE TABLE IF NOT EXISTS `eoms_repair_record` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  repair_order_id bigint(20)      NOT NULL                COMMENT '维修工单ID',
  fault_reason    text            DEFAULT NULL            COMMENT '故障原因',
  check_process   text            DEFAULT NULL            COMMENT '检测过程',
  solution        text            DEFAULT NULL            COMMENT '解决方案',
  result          text            DEFAULT NULL            COMMENT '维修结果',
  processing_photos text          DEFAULT NULL            COMMENT '维修中照片URL（逗号分隔，带水印）',
  completed_photos  text          DEFAULT NULL            COMMENT '维修完成照片URL（逗号分隔，带水印）',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  KEY idx_repair_order_id (repair_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修记录表';

-- 维修结算表
CREATE TABLE IF NOT EXISTS `eoms_repair_settlement` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  settlement_no   varchar(32)     DEFAULT NULL            COMMENT '结算单号（JS前缀）',
  repair_order_id bigint(20)      NOT NULL                COMMENT '维修工单ID',
  service_fee     decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '服务费',
  material_amount decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '材料收入总额',
  labor_amount    decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '人工收入总额',
  other_amount    decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '其它收入',
  total_income    decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '总收入',
  material_cost   decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '材料成本总额',
  labor_cost      decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '人工成本总额',
  other_cost      decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '其它成本',
  total_cost      decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '总成本',
  profit_amount   decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '利润额 = 总收入 - 总成本',
  status          varchar(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿、CONFIRMED-已确认',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_settlement_no (settlement_no),
  UNIQUE KEY uk_repair_order_id (repair_order_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修结算表';

-- 维修材料使用表
CREATE TABLE IF NOT EXISTS `eoms_repair_material` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  repair_order_id bigint(20)      NOT NULL                COMMENT '维修工单ID',
  material_id     bigint(20)      NOT NULL                COMMENT '物料ID，关联 psi_goods.id',
  warehouse_id    bigint(20)      DEFAULT NULL            COMMENT '出库仓库ID，关联 psi_warehouse.id',
  material_name   varchar(100)    NOT NULL                COMMENT '物料名称（快照）',
  quantity        decimal(10,2)   NOT NULL                COMMENT '使用数量',
  sale_price      decimal(12,2)   NOT NULL                COMMENT '销售单价（快照）',
  cost_price      decimal(12,2)   NOT NULL                COMMENT '成本单价（快照）',
  sale_amount     decimal(12,2)   NOT NULL                COMMENT '销售金额',
  cost_amount     decimal(12,2)   NOT NULL                COMMENT '成本金额',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  KEY idx_repair_order_id (repair_order_id),
  KEY idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修材料使用表';

-- ---------- 5.4 报价模块 ----------

-- 报价单表
CREATE TABLE IF NOT EXISTS `eoms_quote` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  quote_no        varchar(32)     NOT NULL                COMMENT '报价编号，唯一',
  customer_id     bigint(20)      DEFAULT NULL            COMMENT '客户ID，关联 psi_unit.id',
  warehouse_id    bigint(20)      DEFAULT NULL            COMMENT '仓库ID，关联 psi_warehouse.id',
  project_name    varchar(200)    DEFAULT NULL            COMMENT '项目名称',
  quote_unit      varchar(200)    DEFAULT NULL            COMMENT '报价单位',
  quote_date      datetime        DEFAULT NULL            COMMENT '报价时间',
  valid_until     datetime        DEFAULT NULL            COMMENT '报价有效期',
  tax_rate        decimal(5,2)    NOT NULL DEFAULT 4.00   COMMENT '增值税综合税率(%)',
  material_subtotal decimal(12,2) NOT NULL DEFAULT 0.00   COMMENT '材料小计',
  service_subtotal decimal(12,2)  NOT NULL DEFAULT 0.00   COMMENT '技术服务费小计',
  repair_order_id bigint(20)      DEFAULT NULL            COMMENT '关联维修工单ID',
  total_amount    decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '报价总金额(含税)',
  status          varchar(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿、SUBMIT-已提交、CONFIRM-已确认、CANCEL-已取消',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  terms           text            DEFAULT NULL            COMMENT '备注/条款',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_quote_no (quote_no),
  KEY idx_customer_id (customer_id),
  KEY idx_quote_warehouse (warehouse_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报价单表';

-- 报价明细表
CREATE TABLE IF NOT EXISTS `eoms_quote_detail` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  quote_id        bigint(20)      NOT NULL                COMMENT '报价单ID',
  line_type       varchar(16)     NOT NULL DEFAULT 'SERVICE' COMMENT 'MATERIAL材料 / SERVICE服务',
  goods_id        bigint(20)      DEFAULT NULL            COMMENT '材料行关联 psi_goods.id',
  item_name       varchar(200)    NOT NULL                COMMENT '材料名称/服务项目',
  brand_model     varchar(100)    DEFAULT NULL            COMMENT '品牌/型号',
  unit            varchar(20)     DEFAULT '台'            COMMENT '单位',
  description     varchar(200)    DEFAULT NULL            COMMENT '服务说明',
  sort_no         int             NOT NULL DEFAULT 0      COMMENT '行序号',
  quantity        decimal(10,2)   NOT NULL                COMMENT '数量/工作量',
  price           decimal(12,2)   NOT NULL                COMMENT '单价',
  amount          decimal(12,2)   NOT NULL                COMMENT '金额',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  KEY idx_quote_id (quote_id),
  KEY idx_qd_goods (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报价明细表';

-- ---------- 5.5 合同模块 ----------

-- 合同表
CREATE TABLE IF NOT EXISTS `eoms_contract` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  contract_no     varchar(32)     NOT NULL                COMMENT '合同编号，唯一',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  contract_name   varchar(200)    NOT NULL                COMMENT '合同名称',
  contract_type   varchar(20)     DEFAULT NULL            COMMENT '合同类型',
  amount          decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '合同金额',
  start_date      date            DEFAULT NULL            COMMENT '开始日期',
  end_date        date            DEFAULT NULL            COMMENT '结束日期',
  status          varchar(20)     NOT NULL DEFAULT 'EFFECTIVE' COMMENT '状态：EFFECTIVE-生效中、EXPIRED-已到期、TERMINATED-已终止',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  file_id         bigint(20)      DEFAULT NULL            COMMENT '平台 sys_file.id',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_contract_no (contract_no),
  KEY idx_customer_id (customer_id),
  KEY idx_ec_file (file_id),
  KEY idx_status (status),
  KEY idx_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合同表';

-- ---------- 5.6 物料库存模块（M3 已并入 psi_goods / psi_stock_*）----------
-- eoms_material / eoms_material_category / eoms_material_stock / eoms_material_record
-- eoms_purchase / eoms_inbound → 进销存商品档案与采购入库
-- 工单用料 eoms_repair_material.material_id 语义：psi_goods.id；结算出库走 PSI stock-out（REPAIR）

-- ---------- 5.7 财务模块 ----------

-- 收入记录表
CREATE TABLE IF NOT EXISTS `eoms_income` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  income_no       varchar(32)     NOT NULL                COMMENT '收入编号',
  repair_order_id bigint(20)      NOT NULL                COMMENT '关联维修工单ID',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  amount          decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '收入金额',
  income_type     varchar(20)     DEFAULT NULL            COMMENT '收入类型',
  income_date     date            DEFAULT NULL            COMMENT '收入日期',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_income_no (income_no),
  KEY idx_repair_order_id (repair_order_id),
  KEY idx_customer_id (customer_id),
  KEY idx_income_date (income_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收入记录表';

-- 成本记录表
CREATE TABLE IF NOT EXISTS `eoms_cost` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  repair_order_id bigint(20)      NOT NULL                COMMENT '关联维修工单ID',
  cost_type       varchar(20)     NOT NULL                COMMENT '成本类型：MATERIAL-材料、LABOR-人工、OTHER-其它',
  amount          decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '成本金额',
  description     varchar(500)    DEFAULT NULL            COMMENT '成本说明',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  KEY idx_repair_order_id (repair_order_id),
  KEY idx_cost_type (cost_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成本记录表';

-- 应收账款表
CREATE TABLE IF NOT EXISTS `eoms_receivable` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  receivable_no   varchar(32)     DEFAULT NULL            COMMENT '应收单号（YS前缀）',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  repair_order_id bigint(20)      NOT NULL                COMMENT '关联维修工单ID',
  amount          decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '应收金额',
  received_amount decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '已收金额',
  arrears_amount  decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '欠款金额 = 应收 - 已收',
  status          varchar(20)     NOT NULL DEFAULT 'UNPAID' COMMENT '状态：UNPAID-未收款、PARTIAL-部分收款、PAID-已收款',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_receivable_no (receivable_no),
  KEY idx_customer_id (customer_id),
  KEY idx_repair_order_id (repair_order_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修应收账款';

-- 收款记录表（维修收款，勿与进销存 psi_payment 付款混淆）
CREATE TABLE IF NOT EXISTS `eoms_payment` (
  id              bigint(20)      NOT NULL AUTO_INCREMENT COMMENT '主键',
  customer_id     bigint(20)      NOT NULL                COMMENT '客户ID，关联 psi_unit.id',
  receivable_id   bigint(20)      NOT NULL                COMMENT '关联应收单ID',
  payment_no      varchar(32)     DEFAULT NULL            COMMENT '收款单号（SK前缀）',
  batch_id        varchar(64)     DEFAULT NULL            COMMENT '批量核销批次ID',
  amount          decimal(12,2)   NOT NULL DEFAULT 0.00   COMMENT '收款金额',
  payment_method  varchar(20)     DEFAULT NULL            COMMENT '收款方式：现金、转账、微信、支付宝',
  payment_time    datetime        DEFAULT NULL            COMMENT '收款时间',
  create_by       varchar(64)     DEFAULT ''              COMMENT '创建者',
  create_time     datetime        DEFAULT NULL            COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''              COMMENT '更新者',
  update_time     datetime        DEFAULT NULL            COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL            COMMENT '备注',
  del_flag        char(1)         NOT NULL DEFAULT '0'    COMMENT '删除标志（0存在 1删除）',
  PRIMARY KEY (id),
  KEY idx_customer_id (customer_id),
  KEY idx_receivable_id (receivable_id),
  KEY idx_payment_time (payment_time),
  KEY idx_payment_no (payment_no),
  KEY idx_batch_id (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修收款记录';

-- ---------- 5.8 基础能力（M1 已并入平台）----------
-- eoms_config → sys_option（category=EOMS）
-- eoms_attachment → sys_file；合同附件走 eoms_contract.file_id
-- eoms_operation_log → sys_log（continew @Log）

-- =====================================================
