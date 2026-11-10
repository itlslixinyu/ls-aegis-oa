package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.aegis.biz.psi.enums.BankDirectionEnum;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.MatchStatusEnum;
import com.ls.aegis.biz.psi.mapper.BankStatementMapper;
import com.ls.aegis.biz.psi.mapper.CompanyAccountMapper;
import com.ls.aegis.biz.psi.mapper.PsiPaymentMapper;
import com.ls.aegis.biz.psi.mapper.ReceiptMapper;
import com.ls.aegis.biz.psi.model.entity.BankStatementDO;
import com.ls.aegis.biz.psi.model.entity.CompanyAccountDO;
import com.ls.aegis.biz.psi.model.entity.PaymentDO;
import com.ls.aegis.biz.psi.model.entity.ReceiptDO;
import com.ls.aegis.biz.psi.model.query.BankStatementQuery;
import com.ls.aegis.biz.psi.model.req.BankImportReq;
import com.ls.aegis.biz.psi.model.req.BankMatchReq;
import com.ls.aegis.biz.psi.model.resp.BankStatementResp;
import com.ls.aegis.biz.psi.model.resp.LabelValueResp;
import com.ls.aegis.biz.psi.service.BankStatementService;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import com.ls.aegis.crypto.api.IGmCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankStatementServiceImpl implements BankStatementService {

    private final BankStatementMapper bankStatementMapper;
    private final CompanyAccountMapper companyAccountMapper;
    private final ReceiptMapper receiptMapper;
    private final PsiPaymentMapper paymentMapper;
    private final ObjectProvider<IGmCrypto> gmCryptoProvider;
    private final com.ls.aegis.biz.psi.mapper.CashLedgerMapper cashLedgerMapper;
    private final com.ls.aegis.starter.support.idempotent.IdempotentHelper idempotentHelper;

    @Override
    public PageResp<BankStatementResp> page(BankStatementQuery query, PageQuery pageQuery) {
        QueryWrapper<BankStatementDO> wrapper = QueryWrapperHelper.build(query);
        wrapper.orderByDesc("id");
        Page<BankStatementDO> page = bankStatementMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), wrapper);
        PageResp<BankStatementResp> resp = PageResp.build(page, BankStatementResp.class);
        fillAccountNames(resp.getList());
        return resp;
    }

    @Override
    public BankStatementResp get(Long id) {
        BankStatementDO entity = bankStatementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("银行流水不存在");
        }
        BankStatementResp resp = BeanUtil.copyProperties(entity, BankStatementResp.class);
        fillAccountNames(List.of(resp));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public int importStatements(BankImportReq req) {
        Long tenantId = TenantFillHelper.currentTenantId();
        CompanyAccountDO account = companyAccountMapper.selectByIdForUpdate(req.getAccountId());
        if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
            throw new BusinessException("银行账户不存在或无权访问");
        }
        int inserted = 0;
        for (BankImportReq.Item line : req.getItems()) {
            if (line.getAmount() == null || line.getAmount().signum() <= 0 || line.getAmount().scale() > 2) {
                throw new BusinessException("流水金额必须为正数且最多两位小数");
            }
            line.setBankSeqNo(StrUtil.trimToNull(line.getBankSeqNo()));
            line.setStmtDate(line.getStmtDate().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
            line.setAmount(line.getAmount().setScale(2));
            line.setCounterparty(StrUtil.trimToEmpty(line.getCounterparty()));
            line.setSummary(StrUtil.trimToEmpty(line.getSummary()));
            if (line.getBankSeqNo() == null && bankStatementMapper.countNoSequenceIdentity(tenantId,req.getAccountId(),
                line.getStmtDate(),line.getDirection().getValue(),line.getAmount(),line.getCounterparty(),line.getSummary()) > 0) {
                continue; // Historical raw-scale/hash variants keep their stored fingerprints and audit identity.
            }
            String fingerprint = fingerprint(line);
            if (StrUtil.isNotBlank(line.getBankSeqNo())) {
                Long cnt = bankStatementMapper.selectCount(Wrappers.lambdaQuery(BankStatementDO.class)
                    .eq(BankStatementDO::getTenantId, tenantId)
                    .eq(BankStatementDO::getAccountId, req.getAccountId())
                    .eq(BankStatementDO::getBankSeqNo, line.getBankSeqNo()));
                if (cnt != null && cnt > 0) {
                    continue;
                }
            }
            Long fpCnt = bankStatementMapper.selectCount(Wrappers.lambdaQuery(BankStatementDO.class)
                .eq(BankStatementDO::getTenantId, tenantId)
                .eq(BankStatementDO::getAccountId, req.getAccountId())
                .eq(BankStatementDO::getFingerprint, fingerprint));
            if (fpCnt != null && fpCnt > 0) {
                continue;
            }
            BankStatementDO row = new BankStatementDO();
            row.setTenantId(tenantId);
            row.setDeptId(TenantFillHelper.currentDeptId());
            row.setAccountId(req.getAccountId());
            row.setStmtDate(line.getStmtDate());
            row.setDirection(line.getDirection());
            row.setAmount(line.getAmount());
            row.setCounterparty(line.getCounterparty());
            row.setSummary(line.getSummary());
            row.setBankSeqNo(line.getBankSeqNo());
            row.setFingerprint(fingerprint);
            row.setMatchStatus(MatchStatusEnum.UNMATCHED);
            bankStatementMapper.insert(row);
            inserted++;
        }
        return inserted;
    }

    @Override
    public void downloadImportTemplate(jakarta.servlet.http.HttpServletResponse response) {
        com.ls.aegis.biz.psi.support.PsiImportHelper.downloadTemplate(response, com.ls.aegis.biz.psi.model.req.BankImportRowReq.class, "银行流水导入模板.xlsx");
    }

    @Override
    public com.ls.aegis.biz.psi.model.resp.BankImportParseResp parseImportExcel(org.springframework.web.multipart.MultipartFile file) {
        top.continew.starter.core.util.validation.CheckUtils.throwIf(file == null || file.isEmpty(), "文件不能为空");
        List<com.ls.aegis.biz.psi.model.req.BankImportRowReq> rows;
        try {
            rows = com.ls.aegis.biz.psi.support.PsiImportHelper.readExcel(file.getInputStream(), com.ls.aegis.biz.psi.model.req.BankImportRowReq.class);
        } catch (Exception e) {
            throw new BusinessException("数据文件解析异常，请检查模板格式");
        }
        com.ls.aegis.biz.psi.model.resp.BankImportParseResp resp = new com.ls.aegis.biz.psi.model.resp.BankImportParseResp();
        resp.setTotalRows(rows.size());
        List<com.ls.aegis.biz.psi.model.resp.BankImportParseResp.Item> items = new ArrayList<>();
        for (com.ls.aegis.biz.psi.model.req.BankImportRowReq row : rows) {
            if (StrUtil.isBlank(row.getStmtDate()) && StrUtil.isBlank(row.getAmount()) && StrUtil.isBlank(row.getDirection())) {
                continue;
            }
            com.ls.aegis.biz.psi.model.resp.BankImportParseResp.Item item = new com.ls.aegis.biz.psi.model.resp.BankImportParseResp.Item();
            item.setStmtDate(normalizeStmtDate(row.getStmtDate()));
            item.setDirection(parseDirection(row.getDirection()).getValue());
            item.setAmount(parseAmount(row.getAmount()));
            item.setCounterparty(StrUtil.trim(row.getCounterparty()));
            item.setSummary(StrUtil.trim(row.getSummary()));
            item.setBankSeqNo(StrUtil.trim(row.getBankSeqNo()));
            items.add(item);
        }
        top.continew.starter.core.util.validation.CheckUtils.throwIfEmpty(items, "未解析到有效流水（日期、方向、金额必填）");
        resp.setValidRows(items.size());
        resp.setItems(items);
        return resp;
    }

    private String normalizeStmtDate(String raw) {
        top.continew.starter.core.util.validation.CheckUtils.throwIfBlank(raw, "流水日期不能为空");
        String v = raw.trim().replace('/', '-');
        if (v.length() == 10) {
            return v + "T00:00:00";
        }
        if (v.contains(" ")) {
            return v.replace(' ', 'T');
        }
        return v;
    }

    private BankDirectionEnum parseDirection(String raw) {
        top.continew.starter.core.util.validation.CheckUtils.throwIfBlank(raw, "方向不能为空");
        String v = raw.trim();
        if ("IN".equalsIgnoreCase(v) || "收入".equals(v) || "贷".equals(v) || "收".equals(v)) {
            return BankDirectionEnum.IN;
        }
        if ("OUT".equalsIgnoreCase(v) || "支出".equals(v) || "借".equals(v) || "付".equals(v)) {
            return BankDirectionEnum.OUT;
        }
        throw new BusinessException("方向无法识别（请填 收入/支出 或 IN/OUT）：" + raw);
    }

    private BigDecimal parseAmount(String raw) {
        top.continew.starter.core.util.validation.CheckUtils.throwIfBlank(raw, "金额不能为空");
        String v = raw.trim().replace(",", "");
        try {
            BigDecimal amount = new BigDecimal(v);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("金额不能为负数：" + raw);
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new BusinessException("金额格式不正确：" + raw);
        }
    }

    @Override
    public List<LabelValueResp> matchCandidates(Long statementId, String matchBizType) {
        BankStatementDO stmt = bankStatementMapper.selectById(statementId);
        if (stmt == null) {
            throw new BusinessException("银行流水不存在");
        }
        String type = StrUtil.blankToDefault(matchBizType, "").trim().toUpperCase();
        if (StrUtil.isBlank(type)) {
            type = BankDirectionEnum.OUT.equals(stmt.getDirection()) ? "PAYMENT" : "RECEIPT";
        }
        List<LabelValueResp> options = findCandidates(stmt, type).stream()
            .map(candidate -> new LabelValueResp(candidate.docNo() + "｜¥" + candidate.amount().toPlainString(), candidate.id()))
            .collect(Collectors.toCollection(ArrayList::new));
        // 无严格候选时，回落为同账户已确认且未被占用的单据，便于人工强制匹配前浏览
        if (options.isEmpty()) {
            java.util.Set<Long> occupied = occupiedBizIds(type);
            if ("RECEIPT".equals(type)) {
                List<ReceiptDO> list = receiptMapper.selectList(Wrappers.lambdaQuery(ReceiptDO.class)
                    .eq(ReceiptDO::getAccountId, stmt.getAccountId())
                    .eq(ReceiptDO::getDocStatus, DocStatusEnum.CONFIRMED)
                    .orderByDesc(ReceiptDO::getId)
                    .last("LIMIT 50"));
                for (ReceiptDO r : list) {
                    if (occupied.contains(r.getId())) {
                        continue;
                    }
                    options.add(new LabelValueResp(r.getDocNo() + "｜¥" + r.getAmount().toPlainString() + "（需校验金额日期）", r.getId()));
                }
            } else if ("PAYMENT".equals(type)) {
                List<PaymentDO> list = paymentMapper.selectList(Wrappers.lambdaQuery(PaymentDO.class)
                    .eq(PaymentDO::getAccountId, stmt.getAccountId())
                    .eq(PaymentDO::getDocStatus, DocStatusEnum.CONFIRMED)
                    .orderByDesc(PaymentDO::getId)
                    .last("LIMIT 50"));
                for (PaymentDO p : list) {
                    if (occupied.contains(p.getId())) {
                        continue;
                    }
                    options.add(new LabelValueResp(p.getDocNo() + "｜¥" + p.getAmount().toPlainString() + "（需校验金额日期）", p.getId()));
                }
            }
        }
        return options;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String match(BankMatchReq req) {
        BankStatementDO stmt = bankStatementMapper.selectByIdForUpdate(req.getStatementId());
        if (stmt == null) {
            throw new BusinessException("银行流水不存在");
        }
        if (MatchStatusEnum.MATCHED.equals(stmt.getMatchStatus())) {
            throw new BusinessException("流水已匹配");
        }
        String type = req.getMatchBizType().trim().toUpperCase();
        List<Long> candidates = findCandidates(stmt, type).stream().map(MatchCandidate::id).toList();
        if (!candidates.isEmpty() && !candidates.contains(req.getMatchBizId())) {
            throw new BusinessException("指定业务不在匹配候选范围内（金额差≤0.01且日期差≤2天）");
        }
        // 1:1：即使人工强制，也禁止占用已匹配单据
        if (occupiedBizIds(type).contains(req.getMatchBizId())) {
            throw new BusinessException("该业务单已被其他银行流水匹配，禁止一对多");
        }
        // 允许：唯一候选、多候选中人工指定、或无候选时人工强制（仍校验金额日期）
        if ("RECEIPT".equals(type)) {
            ReceiptDO receipt = receiptMapper.selectByIdForUpdate(req.getMatchBizId());
            if (receipt == null || !DocStatusEnum.CONFIRMED.equals(receipt.getDocStatus())) {
                throw new BusinessException("回款单不存在或未确认");
            }
            if (!ObjectUtil.equal(receipt.getAccountId(), stmt.getAccountId())) {
                throw new BusinessException("账户不一致");
            }
            if (!BankDirectionEnum.IN.equals(stmt.getDirection())) {
                throw new BusinessException("回款仅可匹配收入流水");
            }
            assertAmountDate(stmt.getAmount(), receipt.getAmount(), stmt.getStmtDate().toLocalDate(), receipt.getReceiptDate().toLocalDate());
        } else if ("PAYMENT".equals(type)) {
            PaymentDO payment = paymentMapper.selectByIdForUpdate(req.getMatchBizId());
            if (payment == null || !DocStatusEnum.CONFIRMED.equals(payment.getDocStatus())) {
                throw new BusinessException("付款单不存在或未确认");
            }
            if (!ObjectUtil.equal(payment.getAccountId(), stmt.getAccountId())) {
                throw new BusinessException("账户不一致");
            }
            if (!BankDirectionEnum.OUT.equals(stmt.getDirection())) {
                throw new BusinessException("付款仅可匹配支出流水");
            }
            assertAmountDate(stmt.getAmount(), payment.getAmount(), stmt.getStmtDate().toLocalDate(), payment.getPaymentDate().toLocalDate());
        } else {
            throw new BusinessException("不支持的匹配业务类型");
        }
        // 同资金单的匹配/作废均锁同一行；锁后current read防止两条流水并发占用。
        long occupied = cashLedgerMapper.bankMatches(stmt.getTenantId(), type, req.getMatchBizId());
        if (occupied > 0) throw new BusinessException("该业务单已被其他银行流水匹配");
        stmt.setMatchStatus(MatchStatusEnum.MATCHED);
        stmt.setMatchBizType(type);
        stmt.setMatchBizId(req.getMatchBizId());
        bankStatementMapper.updateById(stmt);
        return "MATCHED";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unmatch(Long id, com.ls.aegis.biz.psi.model.req.CashActionReq req) {
        BankStatementDO stmt = bankStatementMapper.selectByIdForUpdate(id);
        if (stmt == null) throw new BusinessException("银行流水不存在或无权访问");
        idempotentHelper.execute(stmt.getTenantId(), "bank-unmatch-" + id, req.getIdempotentKey(), () -> {
            if (!MatchStatusEnum.UNMATCHED.equals(stmt.getMatchStatus())) {
                bankStatementMapper.update(null, Wrappers.lambdaUpdate(BankStatementDO.class)
                    .eq(BankStatementDO::getId, id).set(BankStatementDO::getMatchStatus, MatchStatusEnum.UNMATCHED)
                    .set(BankStatementDO::getMatchBizType, null).set(BankStatementDO::getMatchBizId, null)
                    .set(BankStatementDO::getUnmatchReason, req.getReason())
                    .set(BankStatementDO::getUnmatchUser, com.ls.aegis.common.context.UserContextHolder.getUserId())
                    .set(BankStatementDO::getUnmatchTime, java.time.LocalDateTime.now()));
            }
            return "UNMATCHED";
        });
    }

    private record MatchCandidate(Long id, String docNo, BigDecimal amount) {}

    private List<MatchCandidate> findCandidates(BankStatementDO stmt, String type) {
        if (stmt.getStmtDate() == null || stmt.getAmount() == null) {
            throw new BusinessException("银行流水日期或金额缺失，无法匹配");
        }
        List<MatchCandidate> candidates = new ArrayList<>();
        LocalDate stmtDay = stmt.getStmtDate().toLocalDate();
        BigDecimal minimumAmount = stmt.getAmount().subtract(new BigDecimal("0.01"));
        BigDecimal maximumAmount = stmt.getAmount().add(new BigDecimal("0.01"));
        // 与 within 的自然日语义相同，右侧采用开区间覆盖第二天末尾的全部时间精度。
        var firstDay = stmtDay.minusDays(2).atStartOfDay();
        var afterLastDay = stmtDay.plusDays(3).atStartOfDay();
        Set<Long> occupied = occupiedBizIds(type);
        if ("RECEIPT".equals(type) && BankDirectionEnum.IN.equals(stmt.getDirection())) {
            List<ReceiptDO> list = receiptMapper.selectList(Wrappers.lambdaQuery(ReceiptDO.class)
                .select(ReceiptDO::getId, ReceiptDO::getDocNo, ReceiptDO::getAmount, ReceiptDO::getReceiptDate)
                .eq(ReceiptDO::getAccountId, stmt.getAccountId())
                .eq(ReceiptDO::getDocStatus, DocStatusEnum.CONFIRMED)
                .between(ReceiptDO::getAmount, minimumAmount, maximumAmount)
                .ge(ReceiptDO::getReceiptDate, firstDay)
                .lt(ReceiptDO::getReceiptDate, afterLastDay));
            for (ReceiptDO row : list) {
                if (!occupied.contains(row.getId()) && row.getReceiptDate() != null
                    && within(stmt.getAmount(), row.getAmount(), stmtDay, row.getReceiptDate().toLocalDate())) {
                    candidates.add(new MatchCandidate(row.getId(), row.getDocNo(), row.getAmount()));
                }
            }
        } else if ("PAYMENT".equals(type) && BankDirectionEnum.OUT.equals(stmt.getDirection())) {
            List<PaymentDO> list = paymentMapper.selectList(Wrappers.lambdaQuery(PaymentDO.class)
                .select(PaymentDO::getId, PaymentDO::getDocNo, PaymentDO::getAmount, PaymentDO::getPaymentDate)
                .eq(PaymentDO::getAccountId, stmt.getAccountId())
                .eq(PaymentDO::getDocStatus, DocStatusEnum.CONFIRMED)
                .between(PaymentDO::getAmount, minimumAmount, maximumAmount)
                .ge(PaymentDO::getPaymentDate, firstDay)
                .lt(PaymentDO::getPaymentDate, afterLastDay));
            for (PaymentDO row : list) {
                if (!occupied.contains(row.getId()) && row.getPaymentDate() != null
                    && within(stmt.getAmount(), row.getAmount(), stmtDay, row.getPaymentDate().toLocalDate())) {
                    candidates.add(new MatchCandidate(row.getId(), row.getDocNo(), row.getAmount()));
                }
            }
        }
        return candidates;
    }

    /** 已被 MATCHED 流水占用的业务单 ID */
    private java.util.Set<Long> occupiedBizIds(String type) {
        List<BankStatementDO> matched = bankStatementMapper.selectList(Wrappers.lambdaQuery(BankStatementDO.class)
            .select(BankStatementDO::getMatchBizId)
            .eq(BankStatementDO::getMatchStatus, MatchStatusEnum.MATCHED)
            .eq(BankStatementDO::getMatchBizType, type)
            .isNotNull(BankStatementDO::getMatchBizId));
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (BankStatementDO s : matched) {
            if (s.getMatchBizId() != null) {
                ids.add(s.getMatchBizId());
            }
        }
        return ids;
    }

    private void assertAmountDate(BigDecimal a1, BigDecimal a2, LocalDate d1, LocalDate d2) {
        if (!within(a1, a2, d1, d2)) {
            throw new BusinessException("金额或日期不满足匹配规则（金额差≤0.01且日期差≤2天）");
        }
    }

    private boolean within(BigDecimal a1, BigDecimal a2, LocalDate d1, LocalDate d2) {
        if (a1 == null || a2 == null || d1 == null || d2 == null) {
            return false;
        }
        if (a1.subtract(a2).abs().compareTo(new BigDecimal("0.01")) > 0) {
            return false;
        }
        return Math.abs(ChronoUnit.DAYS.between(d1, d2)) <= 2;
    }

    private void fillAccountNames(List<BankStatementResp> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> ids = list.stream().map(BankStatementResp::getAccountId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> names = new HashMap<>();
        for (CompanyAccountDO a : companyAccountMapper.selectByIds(ids)) {
            names.put(a.getId(), a.getAccountName());
        }
        for (BankStatementResp row : list) {
            if (row.getAccountId() != null) {
                row.setAccountName(names.get(row.getAccountId()));
            }
        }
    }

    private String fingerprint(BankImportReq.Item line) {
        // A bank sequence identifies a transaction even when all economic fields coincide.
        // A structured tuple avoids delimiter collisions; semantic lookup retains legacy hash/scale compatibility.
        String raw = StrUtil.isNotBlank(line.getBankSeqNo()) ? "BANK_SEQ|" + line.getBankSeqNo().trim() : cn.hutool.json.JSONUtil.toJsonStr(java.util.List.of(
            String.valueOf(line.getStmtDate()),
            line.getDirection() == null ? "" : line.getDirection().getValue(),
            line.getAmount() == null ? "" : line.getAmount().toPlainString(),
            StrUtil.blankToDefault(line.getCounterparty(), ""),
            StrUtil.blankToDefault(line.getSummary(), "")));
        IGmCrypto gm = gmCryptoProvider.getIfAvailable();
        if (gm != null) {
            return gm.sm3Hex(raw);
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException("计算流水指纹失败");
        }
    }
}
