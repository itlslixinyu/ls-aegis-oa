package com.ls.aegis.biz.psi.mapper;

import com.ls.aegis.biz.psi.model.entity.UnitInvoiceDO;
import org.apache.ibatis.annotations.Mapper;
import top.continew.starter.data.mapper.BaseMapper;

/**
 * 往来单位开票信息 Mapper
 */
@Mapper
public interface UnitInvoiceMapper extends BaseMapper<UnitInvoiceDO> {
}
