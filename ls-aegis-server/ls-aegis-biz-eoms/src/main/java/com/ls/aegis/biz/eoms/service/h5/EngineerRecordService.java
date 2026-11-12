package com.ls.aegis.biz.eoms.service.h5;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.biz.eoms.domain.RepairMaterial;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.domain.h5.EngineerMaterialView;
import com.ls.aegis.biz.eoms.domain.h5.EngineerRecordRequest;
import com.ls.aegis.biz.eoms.mapper.repair.EngineerMaterialMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairMaterialMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper;
import com.ls.aegis.biz.eoms.service.repair.IRepairMaterialService;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.biz.eoms.service.repair.IRepairRecordService;

@Service @RequiredArgsConstructor
public class EngineerRecordService {
    private final IRepairOrderService orderService;
    private final RepairOrderMapper orderMapper;
    private final IRepairRecordService recordService;
    private final IRepairMaterialService materialService;
    private final RepairMaterialMapper materialMapper;
    private final EngineerMaterialMapper catalog;

    public RepairOrder ownedOrder(Long id) {
        var order = orderService.selectRepairOrderById(id);
        Long actor = UserContextHolder.getUserId();
        if (order == null || actor == null || !actor.equals(order.getEngineerId()))
            throw new ServiceException("工单不存在或您无权操作此工单");
        return order;
    }
    public Map<String,Object> options(Long id,String keyword) {
        var order=ownedOrder(id);
        String search=keyword==null ? "" : keyword.trim();
        if(search.length()>100) throw new ServiceException("搜索内容过长");
        return Map.of("goods",catalog.goods(order.getTenantId(),search),"warehouses",catalog.warehouses(order.getTenantId()));
    }
    public List<EngineerMaterialView> materials(Long id) {
        var order=ownedOrder(id);return catalog.materials(order.getTenantId(),id);
    }
    @Transactional(rollbackFor=Exception.class)
    public int submit(EngineerRecordRequest request) {
        var visible=ownedOrder(request.getRepairOrderId());
        var order=orderMapper.lockOrder(visible.getId());
        if(order==null || !Objects.equals(visible.getTenantId(),order.getTenantId())
           || !Objects.equals(visible.getEngineerId(),order.getEngineerId()) || !Objects.equals(visible.getDeptId(),order.getDeptId())
           || !"PROCESSING".equals(order.getStatus())) throw new ServiceException("工单状态已变更，请刷新重试");
        var lines=request.getMaterials()==null ? List.<EngineerRecordRequest.MaterialLine>of() : request.getMaterials();
        if(lines.size()>20) throw new ServiceException("每次最多登记20项配件");
        Set<String> keys=new HashSet<>();
        for(var old:materialMapper.lockByOrder(order.getId())) keys.add(old.getMaterialId()+":"+old.getWarehouseId());
        List<RepairMaterial> additions=new ArrayList<>();
        for(var line:lines) {
            if(line==null || line.getMaterialId()==null || line.getWarehouseId()==null || line.getQuantity()==null
               || line.getQuantity().signum()<=0 || line.getQuantity().stripTrailingZeros().scale()>2)
                throw new ServiceException("配件、仓库和有效数量不能为空");
            if(!keys.add(line.getMaterialId()+":"+line.getWarehouseId())) throw new ServiceException("配件和仓库重复，请核对已有用料");
            var good=catalog.good(order.getTenantId(),line.getMaterialId());
            if(good==null) throw new ServiceException("配件已停用或不存在，请重新选择");
            var material=new RepairMaterial();material.setRepairOrderId(order.getId());material.setMaterialId(line.getMaterialId());
            material.setWarehouseId(line.getWarehouseId());material.setMaterialName(good.getName());material.setQuantity(line.getQuantity());
            // Catalog references only; settlement remains a back-office action and uses PSI actual FIFO cost.
            material.setSalePrice(good.getSalePrice()==null?BigDecimal.ZERO:good.getSalePrice());
            material.setCostPrice(good.getPurchasePrice()==null?BigDecimal.ZERO:good.getPurchasePrice());
            material.setRemark(line.getRemark());additions.add(material);
        }
        request.setId(null);request.setCreateBy(UserContextHolder.getUsername());
        int changed=recordService.insertRepairRecord(request);
        if(changed!=1) throw new ServiceException("维修记录保存失败");
        for(var material:additions) if(materialService.insertRepairMaterial(material)!=1) throw new ServiceException("配件保存失败，维修记录未提交");
        return changed;
    }
}
