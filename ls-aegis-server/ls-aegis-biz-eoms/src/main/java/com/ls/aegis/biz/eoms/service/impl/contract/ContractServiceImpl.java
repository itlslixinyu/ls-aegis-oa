package com.ls.aegis.biz.eoms.service.impl.contract;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.domain.Contract;
import com.ls.aegis.biz.eoms.mapper.contract.ContractMapper;
import com.ls.aegis.biz.eoms.service.contract.IContractService;
import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;
import com.ls.aegis.common.context.UserContextHolder;

/**
 * Contract Service实现
 *
 * @author EOMS Generator
 */
@Service
public class ContractServiceImpl implements IContractService
{
    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private EomsNoGenerator eomsNoGenerator;

    @Override
    public Contract selectContractById(Long id)
    {
        Contract contract;
        if (EomsDataScopeHelper.shouldFilter())
        {
            Contract query = new Contract();
            query.setId(id);
            EomsDataScopeHelper.apply(query, false, "ct");
            List<Contract> list = contractMapper.selectContractList(query);
            contract = list.isEmpty() ? null : list.get(0);
        }
        else
        {
            contract = contractMapper.selectContractById(id);
        }
        fillProgress(contract);
        return contract;
    }

    @Override
    public List<Contract> selectContractList(Contract contract)
    {
        EomsDataScopeHelper.apply(contract, false, "ct");
        List<Contract> list = contractMapper.selectContractList(contract);
        if (list != null)
        {
            list.forEach(this::fillProgress);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertContract(Contract contract)
    {
        if (StringUtils.isEmpty(contract.getContractNo()))
        {
            contract.setContractNo(eomsNoGenerator.generateContractNo());
        }
        // 对齐销售/采购：新建默认草稿
        if (StringUtils.isEmpty(contract.getStatus()))
        {
            contract.setStatus(EomsConstants.CONTRACT_STATUS_DRAFT);
        }
        if (StringUtils.isEmpty(contract.getDelFlag()))
        {
            contract.setDelFlag("0");
        }
        EomsDataScopeHelper.fillOnInsert(contract);
        return contractMapper.insertContract(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateContract(Contract contract)
    {
        Contract exist = require(contract.getId());
        assertDraft(exist);
        return contractMapper.updateContract(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteContractByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            assertDraft(require(id));
        }
        return contractMapper.deleteContractByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteContractById(Long id)
    {
        assertDraft(require(id));
        return contractMapper.deleteContractById(id);
    }

    @Override
    public int bindProject(Long id, Long projectId)
    {
        Contract exist = selectContractById(id);
        if (exist == null)
        {
            return 0;
        }
        return contractMapper.bindProject(id, projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String confirm(Long id)
    {
        Contract exist = require(id);
        if (EomsConstants.CONTRACT_STATUS_CONFIRMED.equals(exist.getStatus()))
        {
            return exist.getContractNo();
        }
        assertDraft(exist);
        Contract patch = new Contract();
        patch.setId(id);
        patch.setStatus(EomsConstants.CONTRACT_STATUS_CONFIRMED);
        patch.setUpdateBy(UserContextHolder.getUsername());
        contractMapper.updateContract(patch);
        return exist.getContractNo();
    }

    private Contract require(Long id)
    {
        Contract exist = selectContractById(id);
        if (exist == null)
        {
            throw new ServiceException("维修合同不存在");
        }
        return exist;
    }

    private void assertDraft(Contract entity)
    {
        if (!EomsConstants.CONTRACT_STATUS_DRAFT.equals(entity.getStatus()))
        {
            throw new ServiceException("仅草稿状态允许此操作");
        }
    }

    /** 暂无关联执行单据时进度为 0，列表展示与销售/采购一致 */
    private void fillProgress(Contract contract)
    {
        if (contract != null && contract.getProgress() == null)
        {
            contract.setProgress(BigDecimal.ZERO);
        }
    }
}
