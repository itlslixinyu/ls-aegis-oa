package com.ls.aegis.biz.eoms.repair;

import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairRecordMapper;
import com.ls.aegis.biz.eoms.service.impl.repair.RepairOrderServiceImpl;
import com.ls.aegis.biz.eoms.service.repair.IRepairSettlementService;
import com.ls.aegis.biz.eoms.utils.EomsNoGenerator;
import com.ls.aegis.biz.eoms.utils.GeoUtils;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AttendanceAddressServiceTest {
    private RepairOrderServiceImpl service;
    private RepairOrderMapper orders;
    private IRepairSettlementService settlements;
    private RepairOrder before;
    private GeoUtils geo;
    private final String longitude = "116.3975", latitude = "39.9087";

    @BeforeEach void setup() {
        var actor = new UserContext(); actor.setId(21L); actor.setTenantId(0L);
        UserContextHolder.setContext(actor, false);
        orders = mock(RepairOrderMapper.class);
        settlements = mock(IRepairSettlementService.class);
        geo = spy(new GeoUtils());
        service = spy(new RepairOrderServiceImpl());
        before = new RepairOrder(); before.setId(1L); before.setTenantId(0L);
        before.setEngineerId(21L); before.setDeptId(1L); before.setStatus("ACCEPTED");
        before.setAddress("工单原服务地点");
        doReturn(before).when(service).selectRepairOrderById(1L);
        when(orders.lockOrder(1L)).thenReturn(before);
        when(orders.updateRepairOrder(any())).thenReturn(1);
        var records = mock(RepairRecordMapper.class); when(records.countByRepairOrderId(1L)).thenReturn(1);
        var numbers = mock(EomsNoGenerator.class); when(numbers.generateSettlementNo()).thenReturn("TEST-SETTLEMENT");
        ReflectionTestUtils.setField(service, "repairOrderMapper", orders);
        ReflectionTestUtils.setField(service, "repairRecordMapper", records);
        ReflectionTestUtils.setField(service, "repairSettlementService", settlements);
        ReflectionTestUtils.setField(service, "eomsNoGenerator", numbers);
        ReflectionTestUtils.setField(service, "geoUtils", geo);
    }
    @AfterEach void cleanup() { UserContextHolder.clearContext(); }

    private RepairOrder written() {
        var capture = ArgumentCaptor.forClass(RepairOrder.class);
        verify(orders).updateRepairOrder(capture.capture());
        return capture.getValue();
    }

    @Test void signInPersistsSystemAddressAndRemarkWithoutReplacingJobAddress() {
        assertEquals(1, service.startProcess(1L, "签到备注", longitude, latitude, "示例签到现场"));
        var after = written();
        assertEquals("示例签到现场", after.getSignInAddress());
        assertEquals(longitude, after.getSignInLongitude());
        assertEquals("签到备注", after.getRemark());
        assertNull(after.getAddress()); assertNull(after.getSignOutAddress());
        assertEquals("工单原服务地点", before.getAddress());
        verifyNoInteractions(geo, settlements);
    }

    @Test void signOutHasItsOwnSystemAddressAndKeepsTheSignInLocation() {
        before.setStatus("PROCESSING"); before.setSignInAddress("先前签到位置");
        assertEquals(1, service.submitSettlement(1L, "签退备注", longitude, latitude, "示例签退现场"));
        var after = written();
        assertEquals("示例签退现场", after.getSignOutAddress());
        assertEquals("先前签到位置", before.getSignInAddress());
        assertNull(after.getSignInAddress());
        assertEquals("WAIT_SETTLEMENT", after.getStatus());
        assertEquals("签退备注", after.getRemark());
        verify(settlements).insertRepairSettlement(any());
        verifyNoInteractions(geo);
    }

    @Test void oldCoordinateOnlyEntryRemainsAValidLocalFallback() {
        assertEquals(1, service.startProcess(1L, null, longitude, latitude));
        assertEquals(latitude + "," + longitude, written().getSignInAddress());
        assertFalse(GeoUtils.externalGeocodingEnabled());
    }

    @Test void addressesFitTheExistingFiveHundredCharacterColumn() {
        String value = "路😀".repeat(300);
        service.startProcess(1L, null, longitude, latitude, value);
        String actual = written().getSignInAddress();
        assertEquals(500, actual.codePointCount(0, actual.length()));
        assertFalse(Character.isHighSurrogate(actual.charAt(actual.length() - 1)));
    }

    @Test void invalidStateCannotBeChangedBySupplyingAnAddress() {
        before.setStatus("WAIT_ACCEPT");
        assertThrows(RuntimeException.class, () -> service.startProcess(1L, "备注", longitude, latitude, "地址"));
        verify(orders, never()).updateRepairOrder(any());
        verifyNoInteractions(geo, settlements);
    }
}
