package com.ls.aegis.biz.eoms.mapper.finance;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.Cost;

/**
 * Cost Mapper接口
 * 
 * @author EOMS Generator
 */
public interface CostMapper
{
    List<Cost> lockByOrder(Long orderId);
    /**
     * 查询Cost
     * 
     * @param id 主键
     * @return Cost
     */
    public Cost selectCostById(Long id);

    /**
     * 查询Cost列表
     * 
     * @param cost Cost
     * @return Cost集合
     */
    public List<Cost> selectCostList(Cost cost);

    /**
     * 新增Cost
     * 
     * @param cost Cost
     * @return 结果
     */
    public int insertCost(Cost cost);

    /**
     * 修改Cost
     * 
     * @param cost Cost
     * @return 结果
     */
    public int updateCost(Cost cost);

    /**
     * 删除Cost（主键）
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteCostById(Long id);

    /**
     * 批量删除Cost
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteCostByIds(Long[] ids);
}
