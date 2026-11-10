package com.ls.aegis.biz.psi.support;

import cn.dev33.satoken.stp.StpUtil;
import com.ls.aegis.biz.file.access.*;
import com.ls.aegis.biz.file.mapper.FileMapper;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.file.enums.FileTypeEnum;
import com.ls.aegis.biz.psi.mapper.*;
import com.ls.aegis.biz.psi.model.req.ContractReq;
import com.ls.aegis.biz.psi.service.impl.ContractSaleServiceImpl;
import com.ls.aegis.common.context.*;
import com.ls.aegis.starter.support.idempotent.IdempotentHelper;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PsiAttachmentAccessGuardTest {
    FileMapper files; FileDO file; PsiAttachmentAccessGuard guard; MockedStatic<StpUtil> auth;
    @BeforeEach void setup(){
        files=mock(FileMapper.class);file=new FileDO();file.setId(51L);file.setCreateUser(200L);file.setDeleted(0L);
        when(files.selectById(51L)).thenReturn(file);guard=new PsiAttachmentAccessGuard(files,new FileAccessService(List.of()));
        var actor=new UserContext();actor.setId(100L);actor.setTenantId(1L);UserContextHolder.setContext(actor);
        auth=mockStatic(StpUtil.class);auth.when(StpUtil::getLoginIdAsString).thenReturn("100");
    }
    @AfterEach void cleanup(){auth.close();UserContextHolder.clearContext();}
    @Test void foreignUnreferencedFileCannotBeLaunderedThroughNewContract(){
        var contracts=mock(ContractSaleMapper.class);
        var service=new ContractSaleServiceImpl(contracts,mock(StockOutMapper.class),mock(DocNoGenerator.class),mock(IdempotentHelper.class),guard);
        var request=new ContractReq();request.setFileId("51");request.setName("synthetic");
        assertThrows(RuntimeException.class,()->service.create(request));verify(contracts,never()).insert(any(com.ls.aegis.biz.psi.model.entity.ContractSaleDO.class));
    }
    @Test void ownFileOrPreviouslyAuthorizedBusinessFileCanBeReferenced(){
        file.setCreateUser(100L);assertDoesNotThrow(()->guard.require(51L));
        file.setCreateUser(200L);guard=new PsiAttachmentAccessGuard(files,new FileAccessService(List.of((f,c)->f.getId().equals(51L))));
        assertDoesNotThrow(()->guard.require(51L));
    }
    @Test void deletedMissingAndDirectoryFilesRemainRejectedEvenForOwner(){
        file.setCreateUser(100L);file.setDeleted(51L);assertThrows(RuntimeException.class,()->guard.require(51L));
        file.setDeleted(0L);file.setType(FileTypeEnum.DIR);assertThrows(RuntimeException.class,()->guard.require(51L));
        assertThrows(RuntimeException.class,()->guard.require(99L));
    }
    @Test void csvRejectsPathsOverflowAndMalformedIdsAndChecksEachFile(){
        file.setCreateUser(100L);assertDoesNotThrow(()->guard.requireCsv("51,51"));
        verify(files,times(1)).selectById(51L);
        for(String invalid:List.of("../51","51,","0","-1","9999999999999999999","51,99"))assertThrows(RuntimeException.class,()->guard.requireCsv(invalid));
    }
    @Test void replacementLoginCannotUseStaleThreadOwnership(){
        file.setCreateUser(100L);auth.when(StpUtil::getLoginIdAsString).thenReturn("200");
        assertThrows(RuntimeException.class,()->guard.require(51L));verifyNoInteractions(files);
    }
}
