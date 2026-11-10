package com.ls.aegis.biz.eoms.mapper.finance;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.Income;

/**
 * Income Mapper接口
 * 
 * @author EOMS Generator
 */
public interface IncomeMapper
{
    /**
     * 查询Income
     * 
     * @param id 主键
     * @return Income
     */
    public Income selectIncomeById(Long id);

    /**
     * 查询Income列表
     * 
     * @param income Income
     * @return Income集合
     */
    public List<Income> selectIncomeList(Income income);

    /**
     * 新增Income
     * 
     * @param income Income
     * @return 结果
     */
    public int insertIncome(Income income);

    /**
     * 修改Income
     * 
     * @param income Income
     * @return 结果
     */
    public int updateIncome(Income income);

    /**
     * 删除Income（主键）
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteIncomeById(Long id);

    /**
     * 批量删除Income
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteIncomeByIds(Long[] ids);
    Income selectOriginalByPaymentId(Long paymentId);
}
