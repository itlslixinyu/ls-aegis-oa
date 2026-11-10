package com.ls.aegis.biz.eoms.mapper.contract;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ls.aegis.biz.eoms.domain.Contract;

/**
 * Contract Mapper接口
 * 
 * @author EOMS Generator
 */
public interface ContractMapper
{
    /**
     * 查询Contract
     * 
     * @param id 主键
     * @return Contract
     */
    public Contract selectContractById(Long id);

    /**
     * 查询Contract列表
     * 
     * @param contract Contract
     * @return Contract集合
     */
    public List<Contract> selectContractList(Contract contract);

    /**
     * 新增Contract
     * 
     * @param contract Contract
     * @return 结果
     */
    public int insertContract(Contract contract);

    /**
     * 修改Contract
     * 
     * @param contract Contract
     * @return 结果
     */
    public int updateContract(Contract contract);

    /**
     * 删除Contract（主键）
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteContractById(Long id);

    /**
     * 批量删除Contract
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteContractByIds(Long[] ids);

    /**
     * 绑定/解绑所属项目（projectId 为空则解绑）
     */
    int bindProject(@Param("id") Long id, @Param("projectId") Long projectId);
}
