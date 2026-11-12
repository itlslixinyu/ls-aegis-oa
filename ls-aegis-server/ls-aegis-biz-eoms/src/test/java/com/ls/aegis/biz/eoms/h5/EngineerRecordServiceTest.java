package com.ls.aegis.biz.eoms.h5;

import java.util.List;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.ls.aegis.biz.eoms.domain.RepairMaterial;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.domain.h5.EngineerRecordRequest;
import com.ls.aegis.biz.eoms.domain.h5.MaterialCatalogItem;
import com.ls.aegis.biz.eoms.mapper.repair.EngineerMaterialMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairMaterialMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper;
import com.ls.aegis.biz.eoms.service.h5.EngineerRecordService;
import com.ls.aegis.biz.eoms.service.repair.IRepairMaterialService;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.biz.eoms.service.repair.IRepairRecordService;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.compat.common.exception.ServiceException;

class EngineerRecordServiceTest {
    EngineerRecordService service;IRepairOrderService orderService;RepairOrderMapper orders;IRepairRecordService records;
    IRepairMaterialService materials;RepairMaterialMapper materialMapper;EngineerMaterialMapper catalog;RepairOrder order;EngineerRecordRequest request;
    @BeforeEach void setup(){
        var user=new UserContext();user.setId(21L);user.setUsername("synthetic-worker");UserContextHolder.setContext(user,false);
        orderService=mock(IRepairOrderService.class);orders=mock(RepairOrderMapper.class);records=mock(IRepairRecordService.class);
        materials=mock(IRepairMaterialService.class);materialMapper=mock(RepairMaterialMapper.class);catalog=mock(EngineerMaterialMapper.class);
        service=new EngineerRecordService(orderService,orders,records,materials,materialMapper,catalog);
        order=new RepairOrder();order.setId(1L);order.setTenantId(0L);order.setDeptId(7L);order.setEngineerId(21L);order.setStatus("PROCESSING");
        when(orderService.selectRepairOrderById(1L)).thenReturn(order);when(orders.lockOrder(1L)).thenReturn(order);
        when(materialMapper.lockByOrder(1L)).thenReturn(List.of());when(records.insertRepairRecord(any())).thenReturn(1);when(materials.insertRepairMaterial(any())).thenReturn(1);
        var good=new MaterialCatalogItem();good.setId("11");good.setName("合成配件");good.setSalePrice(new BigDecimal("120.00"));good.setPurchasePrice(new BigDecimal("80.00"));when(catalog.good(0L,11L)).thenReturn(good);
        request=new EngineerRecordRequest();request.setRepairOrderId(1L);var line=new EngineerRecordRequest.MaterialLine();line.setMaterialId(11L);line.setWarehouseId(12L);line.setQuantity(BigDecimal.ONE);line.setRemark("合成备注");request.setMaterials(List.of(line));
    }
    @AfterEach void cleanup(){UserContextHolder.clearContext();}
    @Test void recordAndMaterialUseExistingServicesWithServerCatalogReferences(){
        assertEquals(1,service.submit(request));var c=ArgumentCaptor.forClass(RepairMaterial.class);var orderCheck=inOrder(records,materials);orderCheck.verify(records).insertRepairRecord(request);orderCheck.verify(materials).insertRepairMaterial(c.capture());
        assertEquals("合成配件",c.getValue().getMaterialName());assertEquals(new BigDecimal("120.00"),c.getValue().getSalePrice());assertEquals(new BigDecimal("80.00"),c.getValue().getCostPrice());assertEquals(1L,c.getValue().getRepairOrderId());
    }
    @Test void anotherEngineerCannotReadOptionsOrWrite(){order.setEngineerId(22L);assertThrows(ServiceException.class,()->service.options(1L,""));assertThrows(ServiceException.class,()->service.submit(request));verifyNoInteractions(catalog,records,materials);}
    @Test void changedStatusCannotWrite(){order.setStatus("WAIT_SETTLEMENT");assertThrows(ServiceException.class,()->service.submit(request));verifyNoInteractions(records,materials);}
    @Test void duplicateMaterialsAreRejectedBeforeRecordWrite(){request.setMaterials(List.of(request.getMaterials().get(0),request.getMaterials().get(0)));assertThrows(ServiceException.class,()->service.submit(request));verifyNoInteractions(records,materials);}
    @Test void existingBackOfficeMaterialIsNotDuplicated(){var old=new RepairMaterial();old.setMaterialId(11L);old.setWarehouseId(12L);when(materialMapper.lockByOrder(1L)).thenReturn(List.of(old));assertThrows(ServiceException.class,()->service.submit(request));verifyNoInteractions(records,materials);}
    @Test void disabledOrForeignCatalogEntryCannotBeUsed(){when(catalog.good(0L,11L)).thenReturn(null);assertThrows(ServiceException.class,()->service.submit(request));verifyNoInteractions(records,materials);}
    @Test void optionalNoPartsStillUsesExistingRecordService(){request.setMaterials(List.of());assertEquals(1,service.submit(request));verify(records).insertRepairRecord(request);verifyNoInteractions(materials);}
    @Test void childWriteFailurePropagatesForTransactionalRollback(){when(materials.insertRepairMaterial(any())).thenThrow(new ServiceException("warehouse invalid"));assertThrows(ServiceException.class,()->service.submit(request));}
    @Test void financialFieldsAreNeverSerializedToEngineer() throws Exception {var option=catalog.good(0L,11L);String json=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(option);assertFalse(json.contains("salePrice"));assertFalse(json.contains("purchasePrice"));}
}
