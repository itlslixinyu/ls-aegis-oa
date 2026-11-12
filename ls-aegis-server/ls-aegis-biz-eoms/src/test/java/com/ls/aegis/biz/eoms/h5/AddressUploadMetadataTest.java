package com.ls.aegis.biz.eoms.h5;

import com.ls.aegis.biz.eoms.controller.h5.EngineerController;
import com.ls.aegis.biz.eoms.controller.h5.H5RepairController;
import com.ls.aegis.biz.eoms.domain.H5CustomerInfo;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.interceptor.H5TokenInterceptor;
import com.ls.aegis.biz.eoms.service.h5.H5AttachmentService;
import com.ls.aegis.biz.eoms.service.repair.IRepairOrderService;
import com.ls.aegis.biz.eoms.utils.EomsPlatformFileHelper;
import com.ls.aegis.biz.eoms.utils.GeoUtils;
import com.ls.aegis.biz.eoms.utils.WatermarkInfo;
import com.ls.aegis.biz.eoms.utils.WatermarkUtils;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.dromara.x.file.storage.core.FileInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AddressUploadMetadataTest {
    private GenericApplicationContext spring;
    private EomsPlatformFileHelper files;
    private MockMultipartFile image;

    @BeforeEach void setup() throws Exception {
        spring = new GenericApplicationContext();
        var factory = mock(com.feiniaojin.gracefulresponse.api.ResponseStatusFactory.class);
        var success = mock(com.feiniaojin.gracefulresponse.data.ResponseStatus.class);
        when(success.getCode()).thenReturn("0"); when(factory.defaultSuccess()).thenReturn(success);
        spring.registerBean(com.feiniaojin.gracefulresponse.api.ResponseStatusFactory.class, () -> factory);
        spring.refresh(); new cn.hutool.extra.spring.SpringUtil().setApplicationContext(spring);
        files = mock(EomsPlatformFileHelper.class);
        var stored = new FileInfo(); stored.setId("123");
        when(files.uploadWatermarkedJpeg(any(), anyString(), anyString())).thenReturn(stored);
        when(files.h5UploadPayload(any(), anyString())).thenAnswer(call -> new HashMap<String, Object>());
        var bytes = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB), "jpg", bytes);
        image = new MockMultipartFile("file", "sample.jpg", "image/jpeg", bytes.toByteArray());
    }
    @AfterEach void cleanup() { UserContextHolder.clearContext(); spring.close(); }

    @Test void authenticatedCompanyIsCustomerAndNativeAddressReachesCustomerWatermark() {
        var request = new MockHttpServletRequest();
        var customer = new H5CustomerInfo(1L, "sample", "示例公司"); customer.setTenantId(10L);
        request.setAttribute(H5TokenInterceptor.ATTR_CUSTOMER_INFO, customer);
        request.setAttribute(H5TokenInterceptor.ATTR_CUSTOMER_ID, 1L);
        request.setAttribute(H5TokenInterceptor.ATTR_TENANT_ID, 10L);
        var controller = new H5RepairController();
        var attachments = mock(H5AttachmentService.class); var geo = mock(GeoUtils.class);
        ReflectionTestUtils.setField(controller, "platformFileHelper", files);
        ReflectionTestUtils.setField(controller, "h5AttachmentService", attachments);
        ReflectionTestUtils.setField(controller, "geoUtils", geo);
        var captured = new AtomicReference<WatermarkInfo>();
        try (var renderer = mockStatic(WatermarkUtils.class)) {
            renderer.when(() -> WatermarkUtils.addWatermark(any(byte[].class), any(WatermarkInfo.class)))
                .thenAnswer(call -> { captured.set(call.getArgument(1)); return new byte[]{1}; });
            assertTrue(controller.uploadWithWatermark(request, image, null, null, "示例报修人", "示例报修备注", null, null, null,
                "示例中文地址", "116.3975", "39.9087").isSuccess());
        }
        assertEquals("示例公司", captured.get().getCustomerName());
        assertNull(captured.get().getTaskName());
        assertEquals("报修资料", captured.get().getTitle());
        assertEquals("报修地址", captured.get().getAddressLabel());
        assertEquals("报修人", captured.get().getExecutorLabel());
        assertEquals("示例报修人", captured.get().getExecutor());
        assertEquals("示例报修备注", captured.get().getRemark());
        assertEquals("示例中文地址", captured.get().getServiceAddress());
        verify(attachments).bindNewUpload(customer, 123L);
        verifyNoInteractions(geo);
    }

    @ParameterizedTest @CsvSource({"processing,维修资料", "completed,完成资料"})
    void engineerUsesNativeAddressAndRetainsStageAndTask(String type, String title) {
        var user = new UserContext(); user.setId(21L); UserContextHolder.setContext(user, false);
        var controller = new EngineerController();
        var service = mock(IRepairOrderService.class); var geo = mock(GeoUtils.class);
        var order = new RepairOrder(); order.setId(1L); order.setEngineerId(21L); order.setStatus("PROCESSING"); order.setOrderNo("REPAIR-SAMPLE");
        order.setCustomerName("示例工单客户公司");
        when(service.selectRepairOrderById(1L)).thenReturn(order);
        ReflectionTestUtils.setField(controller, "repairOrderService", service);
        ReflectionTestUtils.setField(controller, "platformFileHelper", files);
        ReflectionTestUtils.setField(controller, "geoUtils", geo);
        var captured = new AtomicReference<WatermarkInfo>();
        try (var renderer = mockStatic(WatermarkUtils.class)) {
            renderer.when(() -> WatermarkUtils.addWatermark(any(byte[].class), any(WatermarkInfo.class)))
                .thenAnswer(call -> { captured.set(call.getArgument(1)); return new byte[]{1}; });
            assertTrue(controller.uploadRepairPhoto(image, 1L, type, "116.3975", "39.9087", "示例中文地址", "示例维修备注").isSuccess());
        }
        assertEquals(title, captured.get().getTitle());
        assertEquals("示例工单客户公司", captured.get().getCustomerName());
        assertEquals("服务地址", captured.get().getAddressLabel());
        assertEquals("维修人员", captured.get().getExecutorLabel());
        assertEquals("示例维修备注", captured.get().getRemark());
        assertEquals("REPAIR-SAMPLE", captured.get().getTaskName());
        assertEquals("示例中文地址", captured.get().getServiceAddress());
        verifyNoInteractions(geo);
    }
}
