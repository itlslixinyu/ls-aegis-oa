package com.ls.aegis.biz.eoms.service.contract;

import java.util.List;
import com.ls.aegis.biz.eoms.domain.Contract;

/**
 * Contract Service接口
 * 
 * @author EOMS Generator
 */
public interface IContractService
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
     * 批量删除Contract
     * 
     * @param ids 需要删除的主键集合
     * @return 结果
     */
    public int deleteContractByIds(Long[] ids);

    /**
     * 删除Contract信息
     * 
     * @param id 主键
     * @return 结果
     */
    public int deleteContractById(Long id);
    /**
     * 绑定/解绑所属项目
     */
    int bindProject(Long id, Long projectId);

    /**
     * 确认生效（草稿 → 已确认）
     */
    String confirm(Long id);
}
