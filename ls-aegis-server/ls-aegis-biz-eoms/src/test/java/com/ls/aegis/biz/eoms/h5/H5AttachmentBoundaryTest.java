package com.ls.aegis.biz.eoms.h5;

import com.ls.aegis.biz.eoms.domain.*;
import com.ls.aegis.biz.eoms.mapper.file.*;
import com.ls.aegis.biz.eoms.service.h5.H5AttachmentService;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.file.service.FileService;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.util.StringJoiner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class H5AttachmentBoundaryTest {
    FileService files = mock(FileService.class);
    H5AttachmentMapper mapper = mock(H5AttachmentMapper.class);
    EomsFileAccessMapper access = mock(EomsFileAccessMapper.class);
    H5AttachmentService service = new H5AttachmentService(files, mapper, access);
    H5CustomerInfo customer = customer(7L, 10L);
    static H5CustomerInfo customer(Long tenant, Long id) { var c = new H5CustomerInfo(id,"FIXTURE","fixture");c.setTenantId(tenant);return c; }
    @BeforeEach void setup() { when(mapper.selectActiveCustomerDept(7L,10L)).thenReturn(20L); }
    FileDO file(Long id, String metadata) {var f=new FileDO();f.setId(id);f.setParentPath("/eoms/repair");f.setCreateUser(1L);f.setDeleted(0L);f.setMetadata(metadata);when(files.getById(id)).thenReturn(f);return f;}
    static String owned(Long tenant,Long id){return "{\"storageTag\":\"preserved\",\"itlsEomsH5UploadOwner\":{\"tenantId\":\""+tenant+"\",\"customerId\":\""+id+"\"}}";}
    RepairApply valid(){file(100L,owned(7L,10L));var input=new RepairApply();input.setContactName("fixture");input.setContactPhone("13800000000");input.setFaultType("DYNAMIC_CUSTOM_CODE");input.setFaultDesc("fixture description");input.setPhotos("100");return input;}
    @Test void forgedFlowAndAuditFieldsAreDiscardedEvenWhenBodyLooksLikeConfirmedAdminApply(){
        var input=valid();input.setId(999L);input.setStatus("CONFIRM");input.setDelFlag("2");input.setRepairNo("CLIENT-CHOSEN");input.setDeptId(999L);input.setCreateUser(999L);input.setTenantId(999L);input.setCustomerId(999L);input.setContractId(999L);input.setFaultDesc("fixture");
        var clean=service.prepareNewApply(customer,input);
        assertNotSame(input,clean);assertNull(clean.getId());assertNull(clean.getRepairNo());assertNull(clean.getContractId());assertEquals("WAIT",clean.getStatus());assertEquals("0",clean.getDelFlag());assertEquals("H5",clean.getSource());assertEquals(7L,clean.getTenantId());assertEquals(10L,clean.getCustomerId());assertEquals(20L,clean.getDeptId());assertEquals(0L,clean.getCreateUser());assertEquals("fixture",clean.getFaultDesc());
    }
    @Test void freshH5UploadIsAccessibleBeforeApplyWithoutTreatingAnonymousCreateUserAsOwner(){
        var f=file(100L,owned(7L,10L));assertTrue(service.canAccess(customer,f));verifyNoInteractions(access);
        when(mapper.selectActiveCustomerDept(7L,11L)).thenReturn(20L);assertFalse(service.canAccess(customer(7L,11L),f));
        when(mapper.selectActiveCustomerDept(8L,10L)).thenReturn(20L);assertFalse(service.canAccess(customer(8L,10L),f));
    }
    @Test void guessedFileIdAndLegacyPathCannotLaunderAttachments(){
        var input=valid();file(100L,"{}");input.setPhotos("100");assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));
        input.setPhotos("/profile/../../private.txt");assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));
        input.setPhotos("sys_file:100");assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));
    }
    @Test void correctOwnedReferencesCanonicalizeAndDeduplicate(){
        var input=valid();input.setPhotos("100, 100");assertEquals("100",service.prepareNewApply(customer,input).getPhotos());
    }
    @Test void legacyReferenceUsesExactTenantAndCustomerRatherThanGlobalCustomerNumber(){
        var f=file(100L,"{}");when(access.countByOwnerAndFileId(7L,10L,100L)).thenReturn(1);assertTrue(service.canAccess(customer,f));verify(access).countByOwnerAndFileId(7L,10L,100L);
    }
    @Test void disabledCustomerAndDeletedFilesCannotBeReused(){
        var f=file(100L,owned(7L,10L));f.setDeleted(1L);assertFalse(service.canAccess(customer,f));when(mapper.selectActiveCustomerDept(7L,10L)).thenReturn(null);assertThrows(ServiceException.class,()->service.canAccess(customer,f));
    }
    @Test void newBindingPreservesStorageMetadataAndCasFailureOrExistingOwnerCannotBeOverwritten(){
        var f=file(100L,"{\"storageTag\":\"preserved\"}");when(mapper.bindUpload(eq(100L),eq(f.getMetadata()),anyString())).thenReturn(1);
        service.bindNewUpload(customer,100L);var written=ArgumentCaptor.forClass(String.class);verify(mapper).bindUpload(eq(100L),eq(f.getMetadata()),written.capture());
        var json=cn.hutool.json.JSONUtil.parseObj(written.getValue());assertEquals("preserved",json.getStr("storageTag"));assertEquals("7",json.getJSONObject("itlsEomsH5UploadOwner").getStr("tenantId"));assertEquals("10",json.getJSONObject("itlsEomsH5UploadOwner").getStr("customerId"));
        f.setMetadata(owned(7L,11L));assertThrows(ServiceException.class,()->service.bindNewUpload(customer,100L));
        f.setMetadata("{}");assertThrows(ServiceException.class,()->service.bindNewUpload(customer,100L));
    }
    @Test void emptyApplyAndMissingPhotosCannotBypassTheCustomerFormContract(){
        assertThrows(ServiceException.class,()->service.prepareNewApply(customer,null));assertThrows(ServiceException.class,()->service.prepareNewApply(customer,new RepairApply()));
        var input=valid();input.setPhotos(null);assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));input.setPhotos("  ");assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));
        input.setPhotos("9999999999999999999");assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));
    }
    @Test void contactNameAndPhoneAreRequiredAndBoundedRatherThanOnlyClientValidated(){
        var input=valid();input.setContactName(" ");assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));input.setContactName("n".repeat(51));assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));input.setContactName("n".repeat(50));assertEquals(50,service.prepareNewApply(customer,input).getContactName().length());
        for(String phone:new String[]{""," ","12800000000","1380000000","138000000000","1380000000x"}){input.setContactPhone(phone);assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));}
        input.setContactPhone(" 16600000000 ");assertEquals("16600000000",service.prepareNewApply(customer,input).getContactPhone());
    }
    @Test void descriptionTrimBoundsFiveToFiveHundredAndFaultTypeRemainsDynamic(){
        var input=valid();for(String value:new String[]{" ","1234","x".repeat(501)}){input.setFaultDesc(value);assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));}
        input.setFaultDesc(" 12345 ");assertEquals("12345",service.prepareNewApply(customer,input).getFaultDesc());input.setFaultDesc("x".repeat(500));assertEquals(500,service.prepareNewApply(customer,input).getFaultDesc().length());
        input.setFaultType(" ");assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));input.setFaultType("d".repeat(33));assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));input.setFaultType("d".repeat(32));assertEquals(32,service.prepareNewApply(customer,input).getFaultType().length());
    }
    @Test void urgentDefaultsToNormalAndRejectsUnknownValuesWithoutChangingPcApplyRules(){
        var input=valid();assertEquals("NORMAL",service.prepareNewApply(customer,input).getUrgentLevel());input.setUrgentLevel(" ");assertEquals("NORMAL",service.prepareNewApply(customer,input).getUrgentLevel());
        for(String value:new String[]{"NORMAL","URGENT","VERY_URGENT"}){input.setUrgentLevel(value);assertEquals(value,service.prepareNewApply(customer,input).getUrgentLevel());}input.setUrgentLevel("CRITICAL");assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));
    }
    @Test void oneThroughNineAuthorizedPhotosAreAllowedButTenAndEmptyCsvMembersAreRejected(){
        var input=valid();StringJoiner photos=new StringJoiner(",");for(long id=100;id<109;id++){file(id,owned(7L,10L));photos.add(Long.toString(id));}input.setPhotos(photos.toString());assertEquals(9,service.prepareNewApply(customer,input).getPhotos().split(",").length);
        file(109L,owned(7L,10L));input.setPhotos(photos+",109");assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));input.setPhotos("100,");assertThrows(ServiceException.class,()->service.prepareNewApply(customer,input));
    }
}
