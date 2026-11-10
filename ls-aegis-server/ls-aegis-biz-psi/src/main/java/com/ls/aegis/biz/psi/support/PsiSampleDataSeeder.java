package com.ls.aegis.biz.psi.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.biz.psi.enums.InvoiceTypeEnum;
import com.ls.aegis.biz.psi.enums.StockInTypeEnum;
import com.ls.aegis.biz.psi.enums.StockOutTypeEnum;
import com.ls.aegis.biz.psi.enums.UnitNatureEnum;
import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.biz.psi.mapper.PayableMapper;
import com.ls.aegis.biz.psi.mapper.PsiReceivableMapper;
import com.ls.aegis.biz.psi.model.entity.PayableDO;
import com.ls.aegis.biz.psi.model.entity.ReceivableDO;
import com.ls.aegis.biz.psi.model.req.CompanyAccountReq;
import com.ls.aegis.biz.psi.model.req.ContractReq;
import com.ls.aegis.biz.psi.model.req.DocConfirmReq;
import com.ls.aegis.biz.psi.model.req.GoodsCategoryReq;
import com.ls.aegis.biz.psi.model.req.GoodsReq;
import com.ls.aegis.biz.psi.model.req.InvoiceReq;
import com.ls.aegis.biz.psi.model.req.PaymentReq;
import com.ls.aegis.biz.psi.model.req.ReceiptReq;
import com.ls.aegis.biz.psi.model.req.StockInReq;
import com.ls.aegis.biz.psi.model.req.StockLineReq;
import com.ls.aegis.biz.psi.model.req.StockOutReq;
import com.ls.aegis.biz.psi.model.req.UnitReq;
import com.ls.aegis.biz.psi.model.req.WarehouseReq;
import com.ls.aegis.biz.psi.model.req.WriteOffReq;
import com.ls.aegis.biz.psi.service.CompanyAccountService;
import com.ls.aegis.biz.psi.service.ContractPurchaseService;
import com.ls.aegis.biz.psi.service.ContractSaleService;
import com.ls.aegis.biz.psi.service.GoodsCategoryService;
import com.ls.aegis.biz.psi.service.GoodsService;
import com.ls.aegis.biz.psi.service.InvoiceInService;
import com.ls.aegis.biz.psi.service.InvoiceOutService;
import com.ls.aegis.biz.psi.service.PaymentService;
import com.ls.aegis.biz.psi.service.ReceiptService;
import com.ls.aegis.biz.psi.service.StockInService;
import com.ls.aegis.biz.psi.service.StockOutService;
import com.ls.aegis.biz.psi.service.UnitService;
import com.ls.aegis.biz.psi.service.WarehouseService;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.common.enums.RoleCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import top.continew.starter.cache.redisson.util.RedisUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * PSI 示例数据种子：物理清库后走真实 Service（DocNoGenerator）造数，与生产编号规则一致。
 * <p>启用：{@code psi.sample-data.seed=true}（环境变量 {@code PSI_SAMPLE_DATA_SEED=true}）。造完后务必关闭，避免重启重复清库。</p>
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "psi.sample-data", name = "seed", havingValue = "true")
public class PsiSampleDataSeeder implements ApplicationRunner {

    private static final Long ADMIN_USER_ID = 1L;
    private static final Long ADMIN_DEPT_ID = 1L;
    private static final Long TENANT_ID = 0L;

    private static final String[] CLEAR_TABLES = {
        "psi_write_off_item",
        "psi_write_off",
        "psi_stock_out_batch",
        "psi_stock_out_item",
        "psi_stock_out",
        "psi_stock_in_item",
        "psi_stock_in",
        "psi_stock_return_item",
        "psi_stock_return",
        "psi_stock_transfer_item",
        "psi_stock_transfer",
        "psi_stock_check_item",
        "psi_stock_check",
        "psi_stock_adjust_item",
        "psi_stock_adjust",
        "psi_stock_flow",
        "psi_stock_batch",
        "psi_invoice_out_link",
        "psi_invoice_out_item",
        "psi_invoice_out",
        "psi_invoice_in_link",
        "psi_invoice_in_item",
        "psi_invoice_in",
        "psi_receipt",
        "psi_payment",
        "psi_receivable",
        "psi_payable",
        "psi_init_stock_item",
        "psi_init_stock",
        "psi_init_receivable_item",
        "psi_init_receivable",
        "psi_init_payable_item",
        "psi_init_payable",
        "psi_init_prepay_item",
        "psi_init_prepay",
        "psi_bank_statement",
        "psi_contract_sale",
        "psi_contract_purchase",
        "psi_company_account",
        "psi_unit_contact",
        "psi_unit_invoice",
        "psi_unit_address",
        "psi_unit_pay",
        "psi_unit",
        "psi_goods",
        "psi_goods_brand",
        "psi_goods_category",
        "psi_warehouse",
        "psi_todo",
        "psi_doc_seq"
    };

    private final JdbcTemplate jdbcTemplate;
    private final UnitService unitService;
    private final GoodsCategoryService goodsCategoryService;
    private final GoodsService goodsService;
    private final WarehouseService warehouseService;
    private final CompanyAccountService companyAccountService;
    private final ContractPurchaseService contractPurchaseService;
    private final ContractSaleService contractSaleService;
    private final StockInService stockInService;
    private final StockOutService stockOutService;
    private final ReceiptService receiptService;
    private final PaymentService paymentService;
    private final InvoiceOutService invoiceOutService;
    private final InvoiceInService invoiceInService;
    private final PsiReceivableMapper receivableMapper;
    private final PayableMapper payableMapper;

    @Override
    public void run(ApplicationArguments args) {
        log.warn("PSI 示例数据种子启动：将清空业务表并用 DocNoGenerator 重建（psi.sample-data.seed=true）");
        bindAdminContext();
        try {
            clearBusinessData();
            seed();
            log.warn("PSI 示例数据种子完成。请关闭 psi.sample-data.seed / PSI_SAMPLE_DATA_SEED 后重启，避免再次清库。");
        } catch (Exception e) {
            // 造数失败不阻断应用启动，便于修复后再次开启开关重跑
            log.error("PSI 示例数据种子失败（应用继续启动）", e);
        } finally {
            UserContextHolder.clearContext();
        }
    }

    private void bindAdminContext() {
        UserContext ctx = new UserContext();
        ctx.setId(ADMIN_USER_ID);
        ctx.setUsername("admin");
        ctx.setDeptId(ADMIN_DEPT_ID);
        ctx.setTenantId(TENANT_ID);
        ctx.setRoleCodes(Set.of(RoleCodeEnum.SUPER_ADMIN.getCode()));
        UserContextHolder.setContext(ctx, false);
    }

    private void clearBusinessData() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        for (String table : CLEAR_TABLES) {
            try {
                Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?", Integer.class, table);
                if (cnt != null && cnt > 0) {
                    jdbcTemplate.update("DELETE FROM `" + table + "`");
                }
            } catch (Exception ex) {
                log.warn("清表跳过 {}: {}", table, ex.getMessage());
            }
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        try {
            RedisUtils.deleteByPattern(PsiConstants.DOC_SEQ_KEY_PREFIX + "*");
            RedisUtils.deleteByPattern(PsiConstants.CODE_SEQ_KEY_PREFIX + "*");
            RedisUtils.deleteByPattern(PsiConstants.IDEMPOTENT_KEY_PREFIX + "*");
        } catch (Exception ex) {
            log.warn("清理 Redis 序号/幂等键失败（将继续造数）: {}", ex.getMessage());
        }
        log.info("PSI 业务表与 Redis 序号已清空（保留 psi_print_template）");
    }

    private void seed() {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Long whMain = createWarehouse("主仓", true);
        Long whSpare = createWarehouse("备用仓", false);

        Long supplier = createUnit("华东智造科技有限公司", UnitTypeEnum.SUPPLIER, UnitNatureEnum.ENTERPRISE);
        Long customer = createUnit("深圳星河供应链管理股份有限公司", UnitTypeEnum.CUSTOMER, UnitNatureEnum.ENTERPRISE);
        Long both = createUnit("北京云启信息技术有限公司", UnitTypeEnum.BOTH, UnitNatureEnum.ENTERPRISE);
        createUnit("张三（个人客户）", UnitTypeEnum.CUSTOMER, UnitNatureEnum.PERSON);

        Long catNetwork = createCategory(0L, "网络设备", 1);
        Long catSwitch = createCategory(catNetwork, "交换机", 1);
        Long catCable = createCategory(catNetwork, "线缆辅材", 2);
        Long catCabinet = createCategory(0L, "机柜配件", 3);
        Long catCabinetStd = createCategory(catCabinet, "标准机柜", 1);

        Long gSwitch = createGoods("工业交换机千兆24口", "台", "13", "980.00", "1280.00", catSwitch);
        Long gCable = createGoods("超五类网线箱装", "箱", "13", "220.00", "320.00", catCable);
        Long gCabinet = createGoods("机柜42U标准", "台", "13", "1800.00", "2400.00", catCabinetStd);

        Long accountId = createAccount();

        Long purchaseContractId = createAndConfirmPurchaseContract(supplier, new BigDecimal("50000.00"), now);
        Long saleContractId = createAndConfirmSaleContract(customer, new BigDecimal("80000.00"), now);

        // 入库 1：交换机 20 + 网线 50
        Long stockIn1 = createAndConfirmStockIn(whMain, supplier, purchaseContractId, now.minusDays(2), List.of(
            line(gSwitch, "20", "980.00", "13"),
            line(gCable, "50", "220.00", "13")));
        // 入库 2：机柜 10（备用仓）
        Long stockIn2 = createAndConfirmStockIn(whSpare, both, null, now.minusDays(1), List.of(
            line(gCabinet, "10", "1800.00", "13")));

        // 出库 1：交换机 5 + 网线 10
        Long stockOut1 = createAndConfirmStockOut(whMain, customer, saleContractId, now, List.of(
            line(gSwitch, "5", "1280.00", "13"),
            line(gCable, "10", "320.00", "13")));
        // 出库 2：机柜 2
        Long stockOut2 = createAndConfirmStockOut(whSpare, both, null, now, List.of(
            line(gCabinet, "2", "2400.00", "13")));

        BigDecimal out1Amount = stockOutService.get(stockOut1).getTaxInclusiveAmount();
        BigDecimal in1Amount = stockInService.get(stockIn1).getTaxInclusiveAmount();

        // 回款 / 付款并即时核销部分开口
        createAndConfirmReceiptWithWriteOff(customer, accountId, out1Amount.multiply(new BigDecimal("0.5"))
            .setScale(2, RoundingMode.HALF_UP), now);
        createAndConfirmPaymentWithWriteOff(supplier, accountId, in1Amount.multiply(new BigDecimal("0.4"))
            .setScale(2, RoundingMode.HALF_UP), now);

        // 发票关联已确认出入库
        createAndConfirmInvoiceOut(customer, stockOut1, out1Amount, now);
        createAndConfirmInvoiceIn(supplier, stockIn1, in1Amount, now);

        // 再留一笔草稿入库，便于测草稿流
        StockInReq draftIn = new StockInReq();
        draftIn.setInType(StockInTypeEnum.PURCHASE);
        draftIn.setInDate(now);
        draftIn.setWarehouseId(whMain);
        draftIn.setUnitId(supplier);
        draftIn.setItems(List.of(line(gCable, "5", "220.00", "13")));
        draftIn.setRemark("示例草稿入库（未确认）");
        stockInService.create(draftIn);

        log.info("示例数据已生成：仓={} / {}，入库含草稿，出库2，收付与发票已确认（stockIn2={}, stockOut2={}）", whMain, whSpare, stockIn2, stockOut2);
    }

    private Long createWarehouse(String name, boolean isDefault) {
        WarehouseReq req = new WarehouseReq();
        req.setWarehouseName(name);
        req.setManager("仓储员");
        req.setAddress("示例地址-" + name);
        req.setIsDefault(isDefault);
        req.setStatus(DisEnableStatusEnum.ENABLE);
        req.setRemark("示例数据");
        return warehouseService.create(req);
    }

    private Long createUnit(String name, UnitTypeEnum type, UnitNatureEnum nature) {
        UnitReq req = new UnitReq();
        req.setUnitName(name);
        req.setUnitType(type);
        req.setUnitNature(nature);
        req.setStatus(DisEnableStatusEnum.ENABLE);
        req.setRemark("示例数据");
        return unitService.create(req);
    }

    private Long createCategory(Long parentId, String name, int sort) {
        GoodsCategoryReq req = new GoodsCategoryReq();
        req.setParentId(parentId == null ? 0L : parentId);
        req.setName(name);
        req.setSort(sort);
        req.setStatus(DisEnableStatusEnum.ENABLE);
        return goodsCategoryService.create(req);
    }

    private Long createGoods(String name, String unit, String taxRate, String purchase, String sale, Long categoryId) {
        GoodsReq req = new GoodsReq();
        req.setGoodsName(name);
        req.setUnit(unit);
        req.setDefaultTaxRate(new BigDecimal(taxRate));
        req.setPurchasePrice(new BigDecimal(purchase));
        req.setSalePrice(new BigDecimal(sale));
        req.setCategoryId(categoryId);
        req.setStatus(DisEnableStatusEnum.ENABLE);
        req.setRemark("示例数据");
        return goodsService.create(req);
    }

    private Long createAccount() {
        CompanyAccountReq req = new CompanyAccountReq();
        req.setAccountName("工行基本户");
        req.setBankName("中国工商银行示例支行");
        req.setAccountNo("6222021000000001234");
        req.setStatus(DisEnableStatusEnum.ENABLE);
        req.setRemark("示例数据");
        return companyAccountService.create(req);
    }

    private Long createAndConfirmPurchaseContract(Long unitId, BigDecimal amount, LocalDateTime now) {
        ContractReq req = new ContractReq();
        req.setName("2026年度采购框架合同");
        req.setSignDate(now.minusDays(10));
        req.setUnitId(unitId);
        req.setTaxInclusiveAmount(amount);
        req.setStartDate(now.minusDays(10));
        req.setEndDate(now.plusYears(1));
        req.setRemark("示例数据");
        Long id = contractPurchaseService.create(req);
        contractPurchaseService.confirm(id, confirmReq("seed-htp-" + id));
        return id;
    }

    private Long createAndConfirmSaleContract(Long unitId, BigDecimal amount, LocalDateTime now) {
        ContractReq req = new ContractReq();
        req.setName("2026年度销售框架合同");
        req.setSignDate(now.minusDays(8));
        req.setUnitId(unitId);
        req.setTaxInclusiveAmount(amount);
        req.setStartDate(now.minusDays(8));
        req.setEndDate(now.plusYears(1));
        req.setRemark("示例数据");
        Long id = contractSaleService.create(req);
        contractSaleService.confirm(id, confirmReq("seed-hts-" + id));
        return id;
    }

    private Long createAndConfirmStockIn(Long warehouseId, Long unitId, Long contractId, LocalDateTime date, List<StockLineReq> items) {
        StockInReq req = new StockInReq();
        req.setInType(StockInTypeEnum.PURCHASE);
        req.setInDate(date);
        req.setWarehouseId(warehouseId);
        req.setUnitId(unitId);
        req.setContractId(contractId);
        req.setItems(items);
        req.setRemark("示例数据-采购入库");
        Long id = stockInService.create(req);
        stockInService.confirm(id, confirmReq("seed-rk-" + id));
        return id;
    }

    private Long createAndConfirmStockOut(Long warehouseId, Long unitId, Long contractId, LocalDateTime date, List<StockLineReq> items) {
        StockOutReq req = new StockOutReq();
        req.setOutType(StockOutTypeEnum.SALE);
        req.setOutDate(date);
        req.setWarehouseId(warehouseId);
        req.setUnitId(unitId);
        req.setContractId(contractId);
        req.setItems(items);
        req.setRemark("示例数据-销售出库");
        Long id = stockOutService.create(req);
        stockOutService.confirm(id, confirmReq("seed-ckd-" + id));
        return id;
    }

    private void createAndConfirmReceiptWithWriteOff(Long unitId, Long accountId, BigDecimal amount, LocalDateTime now) {
        ReceiptReq req = new ReceiptReq();
        req.setReceiptDate(now);
        req.setUnitId(unitId);
        req.setAccountId(accountId);
        req.setPayMethod("BANK");
        req.setAmount(amount);
        req.setRemark("示例数据-回款");
        Long id = receiptService.create(req);

        ReceivableDO open = receivableMapper.selectList(Wrappers.lambdaQuery(ReceivableDO.class)
            .eq(ReceivableDO::getUnitId, unitId)
            .orderByAsc(ReceivableDO::getId)
            .last("LIMIT 1")).stream().findFirst().orElse(null);

        DocConfirmReq confirm = confirmReq("seed-hk-" + id);
        if (open != null && open.getUnwrittenAmount() != null && open.getUnwrittenAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal writeAmt = amount.min(open.getUnwrittenAmount());
            WriteOffReq.Item item = new WriteOffReq.Item();
            item.setOpenId(open.getId());
            item.setAmount(writeAmt);
            confirm.setWriteOffItems(List.of(item));
            confirm.setWriteOffReason("示例即时核销");
        }
        receiptService.confirm(id, confirm);
    }

    private void createAndConfirmPaymentWithWriteOff(Long unitId, Long accountId, BigDecimal amount, LocalDateTime now) {
        PaymentReq req = new PaymentReq();
        req.setPaymentDate(now);
        req.setUnitId(unitId);
        req.setAccountId(accountId);
        req.setPayMethod("BANK");
        req.setAmount(amount);
        req.setRemark("示例数据-付款");
        Long id = paymentService.create(req);

        PayableDO open = payableMapper.selectList(Wrappers.lambdaQuery(PayableDO.class)
            .eq(PayableDO::getUnitId, unitId)
            .orderByAsc(PayableDO::getId)
            .last("LIMIT 1")).stream().findFirst().orElse(null);

        DocConfirmReq confirm = confirmReq("seed-fk-" + id);
        if (open != null && open.getUnwrittenAmount() != null && open.getUnwrittenAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal writeAmt = amount.min(open.getUnwrittenAmount());
            WriteOffReq.Item item = new WriteOffReq.Item();
            item.setOpenId(open.getId());
            item.setAmount(writeAmt);
            confirm.setWriteOffItems(List.of(item));
            confirm.setWriteOffReason("示例即时核销");
        }
        paymentService.confirm(id, confirm);
    }

    private void createAndConfirmInvoiceOut(Long unitId, Long stockOutId, BigDecimal amount, LocalDateTime now) {
        BigDecimal untax = amount.divide(new BigDecimal("1.13"), 2, RoundingMode.HALF_UP);
        BigDecimal tax = amount.subtract(untax);
        InvoiceReq req = new InvoiceReq();
        req.setInvoiceNo("OUT-" + System.currentTimeMillis());
        req.setInvoiceType(InvoiceTypeEnum.SPECIAL);
        req.setInvoiceDate(now);
        req.setUnitId(unitId);
        req.setBuyerTaxNo("91440300MA5SAMPLE1");
        req.setSellerTaxNo("91310000MA5SAMPLE0");
        req.setTaxInclusiveAmount(amount);
        req.setUntaxAmount(untax);
        req.setTaxAmount(tax);
        req.setRemark("示例销项发票");
        InvoiceReq.Item item = new InvoiceReq.Item();
        item.setGoodsName("销售出库开票");
        item.setQuantity(BigDecimal.ONE);
        item.setTaxInclusiveAmount(amount);
        item.setUntaxAmount(untax);
        item.setTaxAmount(tax);
        item.setTaxRate(new BigDecimal("13"));
        req.setItems(List.of(item));
        InvoiceReq.Link link = new InvoiceReq.Link();
        link.setStockDocId(stockOutId);
        link.setAmount(amount);
        req.setLinks(List.of(link));
        Long id = invoiceOutService.create(req);
        invoiceOutService.confirm(id, confirmReq("seed-xx-" + id));
    }

    private void createAndConfirmInvoiceIn(Long unitId, Long stockInId, BigDecimal amount, LocalDateTime now) {
        BigDecimal untax = amount.divide(new BigDecimal("1.13"), 2, RoundingMode.HALF_UP);
        BigDecimal tax = amount.subtract(untax);
        InvoiceReq req = new InvoiceReq();
        req.setInvoiceNo("IN-" + System.currentTimeMillis());
        req.setInvoiceType(InvoiceTypeEnum.SPECIAL);
        req.setInvoiceDate(now);
        req.setUnitId(unitId);
        req.setBuyerTaxNo("91310000MA5SAMPLE0");
        req.setSellerTaxNo("91310115MA1SAMPLE2");
        req.setTaxInclusiveAmount(amount);
        req.setUntaxAmount(untax);
        req.setTaxAmount(tax);
        req.setRemark("示例进项发票");
        InvoiceReq.Item item = new InvoiceReq.Item();
        item.setGoodsName("采购入库收票");
        item.setQuantity(BigDecimal.ONE);
        item.setTaxInclusiveAmount(amount);
        item.setUntaxAmount(untax);
        item.setTaxAmount(tax);
        item.setTaxRate(new BigDecimal("13"));
        req.setItems(List.of(item));
        InvoiceReq.Link link = new InvoiceReq.Link();
        link.setStockDocId(stockInId);
        link.setAmount(amount);
        req.setLinks(List.of(link));
        Long id = invoiceInService.create(req);
        invoiceInService.confirm(id, confirmReq("seed-jx-" + id));
    }

    private static StockLineReq line(Long goodsId, String qty, String price, String taxRate) {
        StockLineReq line = new StockLineReq();
        line.setGoodsId(goodsId);
        line.setQuantity(new BigDecimal(qty));
        line.setTaxInclusivePrice(new BigDecimal(price));
        line.setTaxRate(new BigDecimal(taxRate));
        return line;
    }

    private static DocConfirmReq confirmReq(String key) {
        DocConfirmReq req = new DocConfirmReq();
        req.setIdempotentKey(key + "-" + UUID.randomUUID());
        return req;
    }

}
