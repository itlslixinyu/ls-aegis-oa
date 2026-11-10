package com.ls.aegis.biz.psi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ls.aegis.biz.psi.mapper.UnitAddressMapper;
import com.ls.aegis.biz.psi.mapper.UnitContactMapper;
import com.ls.aegis.biz.psi.mapper.UnitInvoiceMapper;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.mapper.UnitPayMapper;
import com.ls.aegis.biz.psi.model.entity.UnitAddressDO;
import com.ls.aegis.biz.psi.model.entity.UnitContactDO;
import com.ls.aegis.biz.psi.model.entity.UnitDO;
import com.ls.aegis.biz.psi.model.entity.UnitInvoiceDO;
import com.ls.aegis.biz.psi.model.entity.UnitPayDO;
import com.ls.aegis.biz.psi.model.req.UnitAddressReq;
import com.ls.aegis.biz.psi.model.req.UnitContactReq;
import com.ls.aegis.biz.psi.model.req.UnitInvoiceReq;
import com.ls.aegis.biz.psi.model.req.UnitPayReq;
import com.ls.aegis.biz.psi.model.resp.UnitAddressResp;
import com.ls.aegis.biz.psi.model.resp.UnitContactResp;
import com.ls.aegis.biz.psi.model.resp.UnitInvoiceResp;
import com.ls.aegis.biz.psi.model.resp.UnitPayResp;
import com.ls.aegis.biz.psi.service.UnitSubService;
import com.ls.aegis.biz.psi.support.AccountNoMask;
import com.ls.aegis.biz.psi.support.TenantFillHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;

import java.util.List;

/**
 * 往来单位子表业务实现
 */
@Service
@RequiredArgsConstructor
public class UnitSubServiceImpl implements UnitSubService {

    private static final int DEFAULT_YES = 1;
    private static final int DEFAULT_NO = 0;

    private final UnitMapper unitMapper;
    private final UnitContactMapper unitContactMapper;
    private final UnitInvoiceMapper unitInvoiceMapper;
    private final UnitAddressMapper unitAddressMapper;
    private final UnitPayMapper unitPayMapper;

    // ---------- 联系人 ----------

    @Override
    public List<UnitContactResp> listContacts(Long unitId) {
        requireUnit(unitId);
        List<UnitContactDO> list = unitContactMapper.selectList(Wrappers.lambdaQuery(UnitContactDO.class)
            .eq(UnitContactDO::getUnitId, unitId)
            .orderByDesc(UnitContactDO::getId));
        return BeanUtil.copyToList(list, UnitContactResp.class);
    }

    @Override
    public UnitContactResp getContact(Long unitId, Long id) {
        return BeanUtil.copyProperties(requireContact(unitId, id), UnitContactResp.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContact(Long unitId, UnitContactReq req) {
        requireUnit(unitId);
        UnitContactDO entity = BeanUtil.copyProperties(req, UnitContactDO.class);
        entity.setUnitId(unitId);
        entity.setIsDefault(normalizeDefault(entity.getIsDefault()));
        TenantFillHelper.fill(entity);
        unitContactMapper.insert(entity);
        clearOtherContactDefault(unitId, entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContact(Long unitId, Long id, UnitContactReq req) {
        UnitContactDO entity = requireContact(unitId, id);
        BeanUtil.copyProperties(req, entity);
        entity.setId(id);
        entity.setUnitId(unitId);
        entity.setIsDefault(normalizeDefault(entity.getIsDefault()));
        unitContactMapper.updateById(entity);
        clearOtherContactDefault(unitId, entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContact(Long unitId, Long id) {
        requireContact(unitId, id);
        unitContactMapper.deleteById(id);
    }

    // ---------- 开票信息 ----------

    @Override
    public List<UnitInvoiceResp> listInvoices(Long unitId) {
        requireUnit(unitId);
        List<UnitInvoiceDO> list = unitInvoiceMapper.selectList(Wrappers.lambdaQuery(UnitInvoiceDO.class)
            .eq(UnitInvoiceDO::getUnitId, unitId)
            .orderByDesc(UnitInvoiceDO::getId));
        List<UnitInvoiceResp> respList = BeanUtil.copyToList(list, UnitInvoiceResp.class);
        respList.forEach(r -> r.setAccountNo(AccountNoMask.mask(r.getAccountNo())));
        return respList;
    }

    @Override
    public UnitInvoiceResp getInvoice(Long unitId, Long id) {
        return BeanUtil.copyProperties(requireInvoice(unitId, id), UnitInvoiceResp.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInvoice(Long unitId, UnitInvoiceReq req) {
        requireUnit(unitId);
        UnitInvoiceDO entity = BeanUtil.copyProperties(req, UnitInvoiceDO.class);
        entity.setUnitId(unitId);
        entity.setIsDefault(normalizeDefault(entity.getIsDefault()));
        TenantFillHelper.fill(entity);
        unitInvoiceMapper.insert(entity);
        clearOtherInvoiceDefault(unitId, entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInvoice(Long unitId, Long id, UnitInvoiceReq req) {
        UnitInvoiceDO entity = requireInvoice(unitId, id);
        BeanUtil.copyProperties(req, entity);
        entity.setId(id);
        entity.setUnitId(unitId);
        entity.setIsDefault(normalizeDefault(entity.getIsDefault()));
        unitInvoiceMapper.updateById(entity);
        clearOtherInvoiceDefault(unitId, entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInvoice(Long unitId, Long id) {
        requireInvoice(unitId, id);
        unitInvoiceMapper.deleteById(id);
    }

    // ---------- 收货地址 ----------

    @Override
    public List<UnitAddressResp> listAddresses(Long unitId) {
        requireUnit(unitId);
        List<UnitAddressDO> list = unitAddressMapper.selectList(Wrappers.lambdaQuery(UnitAddressDO.class)
            .eq(UnitAddressDO::getUnitId, unitId)
            .orderByDesc(UnitAddressDO::getId));
        return BeanUtil.copyToList(list, UnitAddressResp.class);
    }

    @Override
    public UnitAddressResp getAddress(Long unitId, Long id) {
        return BeanUtil.copyProperties(requireAddress(unitId, id), UnitAddressResp.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAddress(Long unitId, UnitAddressReq req) {
        requireUnit(unitId);
        UnitAddressDO entity = BeanUtil.copyProperties(req, UnitAddressDO.class);
        entity.setUnitId(unitId);
        entity.setIsDefault(normalizeDefault(entity.getIsDefault()));
        TenantFillHelper.fill(entity);
        unitAddressMapper.insert(entity);
        clearOtherAddressDefault(unitId, entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long unitId, Long id, UnitAddressReq req) {
        UnitAddressDO entity = requireAddress(unitId, id);
        BeanUtil.copyProperties(req, entity);
        entity.setId(id);
        entity.setUnitId(unitId);
        entity.setIsDefault(normalizeDefault(entity.getIsDefault()));
        unitAddressMapper.updateById(entity);
        clearOtherAddressDefault(unitId, entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long unitId, Long id) {
        requireAddress(unitId, id);
        unitAddressMapper.deleteById(id);
    }

    // ---------- 付款账户 ----------

    @Override
    public List<UnitPayResp> listPays(Long unitId) {
        requireUnit(unitId);
        List<UnitPayDO> list = unitPayMapper.selectList(Wrappers.lambdaQuery(UnitPayDO.class)
            .eq(UnitPayDO::getUnitId, unitId)
            .orderByDesc(UnitPayDO::getId));
        List<UnitPayResp> respList = BeanUtil.copyToList(list, UnitPayResp.class);
        respList.forEach(r -> r.setAccountNo(AccountNoMask.mask(r.getAccountNo())));
        return respList;
    }

    @Override
    public UnitPayResp getPay(Long unitId, Long id) {
        return BeanUtil.copyProperties(requirePay(unitId, id), UnitPayResp.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPay(Long unitId, UnitPayReq req) {
        requireUnit(unitId);
        UnitPayDO entity = BeanUtil.copyProperties(req, UnitPayDO.class);
        entity.setUnitId(unitId);
        entity.setIsDefault(normalizeDefault(entity.getIsDefault()));
        TenantFillHelper.fill(entity);
        unitPayMapper.insert(entity);
        clearOtherPayDefault(unitId, entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePay(Long unitId, Long id, UnitPayReq req) {
        UnitPayDO entity = requirePay(unitId, id);
        BeanUtil.copyProperties(req, entity);
        entity.setId(id);
        entity.setUnitId(unitId);
        entity.setIsDefault(normalizeDefault(entity.getIsDefault()));
        unitPayMapper.updateById(entity);
        clearOtherPayDefault(unitId, entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePay(Long unitId, Long id) {
        requirePay(unitId, id);
        unitPayMapper.deleteById(id);
    }

    // ---------- 公共 ----------

    private void requireUnit(Long unitId) {
        UnitDO unit = unitMapper.selectById(unitId);
        if (unit == null) {
            throw new BusinessException("往来单位不存在");
        }
    }

    private UnitContactDO requireContact(Long unitId, Long id) {
        requireUnit(unitId);
        UnitContactDO entity = unitContactMapper.selectById(id);
        if (entity == null || !unitId.equals(entity.getUnitId())) {
            throw new BusinessException("联系人不存在或不属于该往来单位");
        }
        return entity;
    }

    private UnitInvoiceDO requireInvoice(Long unitId, Long id) {
        requireUnit(unitId);
        UnitInvoiceDO entity = unitInvoiceMapper.selectById(id);
        if (entity == null || !unitId.equals(entity.getUnitId())) {
            throw new BusinessException("开票信息不存在或不属于该往来单位");
        }
        return entity;
    }

    private UnitAddressDO requireAddress(Long unitId, Long id) {
        requireUnit(unitId);
        UnitAddressDO entity = unitAddressMapper.selectById(id);
        if (entity == null || !unitId.equals(entity.getUnitId())) {
            throw new BusinessException("收货地址不存在或不属于该往来单位");
        }
        return entity;
    }

    private UnitPayDO requirePay(Long unitId, Long id) {
        requireUnit(unitId);
        UnitPayDO entity = unitPayMapper.selectById(id);
        if (entity == null || !unitId.equals(entity.getUnitId())) {
            throw new BusinessException("付款账户不存在或不属于该往来单位");
        }
        return entity;
    }

    private int normalizeDefault(Integer isDefault) {
        return ObjectUtil.equal(isDefault, DEFAULT_YES) ? DEFAULT_YES : DEFAULT_NO;
    }

    private void clearOtherContactDefault(Long unitId, UnitContactDO entity) {
        if (!ObjectUtil.equal(entity.getIsDefault(), DEFAULT_YES)) {
            return;
        }
        unitContactMapper.update(null, Wrappers.lambdaUpdate(UnitContactDO.class)
            .set(UnitContactDO::getIsDefault, DEFAULT_NO)
            .eq(UnitContactDO::getUnitId, unitId)
            .ne(UnitContactDO::getId, entity.getId())
            .eq(UnitContactDO::getIsDefault, DEFAULT_YES));
    }

    private void clearOtherInvoiceDefault(Long unitId, UnitInvoiceDO entity) {
        if (!ObjectUtil.equal(entity.getIsDefault(), DEFAULT_YES)) {
            return;
        }
        unitInvoiceMapper.update(null, Wrappers.lambdaUpdate(UnitInvoiceDO.class)
            .set(UnitInvoiceDO::getIsDefault, DEFAULT_NO)
            .eq(UnitInvoiceDO::getUnitId, unitId)
            .ne(UnitInvoiceDO::getId, entity.getId())
            .eq(UnitInvoiceDO::getIsDefault, DEFAULT_YES));
    }

    private void clearOtherAddressDefault(Long unitId, UnitAddressDO entity) {
        if (!ObjectUtil.equal(entity.getIsDefault(), DEFAULT_YES)) {
            return;
        }
        unitAddressMapper.update(null, Wrappers.lambdaUpdate(UnitAddressDO.class)
            .set(UnitAddressDO::getIsDefault, DEFAULT_NO)
            .eq(UnitAddressDO::getUnitId, unitId)
            .ne(UnitAddressDO::getId, entity.getId())
            .eq(UnitAddressDO::getIsDefault, DEFAULT_YES));
    }

    private void clearOtherPayDefault(Long unitId, UnitPayDO entity) {
        if (!ObjectUtil.equal(entity.getIsDefault(), DEFAULT_YES)) {
            return;
        }
        unitPayMapper.update(null, Wrappers.lambdaUpdate(UnitPayDO.class)
            .set(UnitPayDO::getIsDefault, DEFAULT_NO)
            .eq(UnitPayDO::getUnitId, unitId)
            .ne(UnitPayDO::getId, entity.getId())
            .eq(UnitPayDO::getIsDefault, DEFAULT_YES));
    }
}
