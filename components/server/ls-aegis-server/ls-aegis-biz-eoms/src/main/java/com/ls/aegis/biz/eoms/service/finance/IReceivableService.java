package com.ls.aegis.biz.eoms.service.finance;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.Receivable;

/**
 * Receivable Service接口
 * 
 * @author EOMS Generator
 */
public interface IReceivableService
{
    /**
     * 查询Receivable
     * 
     * @param id 主键
     * @return Receivable
     */
    public Receivable selectReceivableById(Long id);

    /**
     * 查询Receivable列表
     *
     * @param receivable Receivable
     * @return Receivable集合
     */
    public List<Receivable> selectReceivableList(Receivable receivable);

    /**
     * 查询指定客户的未结清应收单（UNPAID 或 PARTIAL），用于收款登记加载待核销列表
     *
     * @param customerId 客户ID
     * @return 未结清应收单集合
     */
    public List<Receivable> selectUnsettledByCustomerId(Long customerId);

    /**
     * 新增Receivable
     * 
     * @param receivable Receivable
     * @return 结果
     */
    public int insertReceivable(Receivable receivable);

    /**
     * 修改Receivable
     * 
     * @param receivable Receivable
     * @return 结果
     */
    public int updateReceivable(Receivable receivable);

    /**
     * 批量删除Receivable
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteReceivableByIds(Long[] ids);

    /**
     * 删除Receivable信息
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteReceivableById(Long id);
}
