package com.ls.aegis.biz.eoms.h5;

import com.ls.aegis.biz.eoms.controller.h5.EngineerController;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.biz.eoms.utils.EomsPlatformFileHelper;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EngineerPhotoWorkflowTest {
    private org.springframework.context.support.GenericApplicationContext spring;
    @AfterEach void cleanup() { UserContextHolder.clearContext(); if (spring != null) spring.close(); }
    @Test void rejectsUnknownPhotoCategoryAndNonWritableOrderBeforeStorage() {
        spring = new org.springframework.context.support.GenericApplicationContext();
        var factory = mock(com.feiniaojin.gracefulresponse.api.ResponseStatusFactory.class);
        var success = mock(com.feiniaojin.gracefulresponse.data.ResponseStatus.class); when(success.getCode()).thenReturn("0");
        when(factory.defaultSuccess()).thenReturn(success);
        spring.registerBean(com.feiniaojin.gracefulresponse.api.ResponseStatusFactory.class, () -> factory);
        spring.refresh(); new cn.hutool.extra.spring.SpringUtil().setApplicationContext(spring);
        var user = new UserContext(); user.setId(21L); UserContextHolder.setContext(user,false);
        var service = mock(IRepairOrderService.class); var files = mock(EomsPlatformFileHelper.class);
        var controller = new EngineerController(); ReflectionTestUtils.setField(controller,"repairOrderService",service); ReflectionTestUtils.setField(controller,"platformFileHelper",files);
        var image = new MockMultipartFile("file","photo.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10});
        assertFalse(controller.uploadRepairPhoto(image,1L,"other",null,null,null,null).isSuccess());
        for (String state : new String[]{"WAIT_ACCEPT","ACCEPTED","WAIT_SETTLEMENT","FINISHED","CLOSED"}) {
            var order = new RepairOrder(); order.setId(1L); order.setEngineerId(21L); order.setStatus(state); when(service.selectRepairOrderById(1L)).thenReturn(order);
            assertFalse(controller.uploadRepairPhoto(image,1L,"processing",null,null,null,null).isSuccess());
        }
        verifyNoInteractions(files);
    }
}
