package com.ls.aegis.biz.file.service.impl;

import com.ls.aegis.biz.file.access.FileAccessService;
import com.ls.aegis.biz.file.mapper.FileMapper;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.file.model.req.FileReq;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileRenameStorageTest {
    @SuppressWarnings("unchecked") FileServiceImpl service(){return new FileServiceImpl(new FileAccessService(List.of()),mock(FileStorageService.class),mock(org.springframework.beans.factory.ObjectProvider.class));}
    @Test void displayRenameDoesNotMoveStorageParent(){
        var s=service();var mapper=mock(FileMapper.class);ReflectionTestUtils.setField(s,"baseMapper",mapper);
        var file=new FileDO();file.setParentPath("/synthetic/");when(mapper.selectById(1L)).thenReturn(file);
        var req=new FileReq();req.setOriginalName("renamed.txt");s.beforeUpdate(req,1L);
        assertNull(req.getParentPath());assertEquals("renamed.txt",req.getOriginalName());assertEquals("/synthetic/",file.getParentPath());
    }
    @Test void suppliedDifferentParentIsRejectedBeforeMetadataUpdate(){
        var s=service();var mapper=mock(FileMapper.class);ReflectionTestUtils.setField(s,"baseMapper",mapper);
        var file=new FileDO();file.setParentPath("/synthetic/");when(mapper.selectById(1L)).thenReturn(file);
        var req=new FileReq();req.setOriginalName("renamed.txt");req.setParentPath("/");
        assertThrows(RuntimeException.class,()->s.beforeUpdate(req,1L));assertEquals("/synthetic/",file.getParentPath());
    }
    @Test void sameParentIsAcceptedWithoutWritingAStorageField(){
        var s=service();var mapper=mock(FileMapper.class);ReflectionTestUtils.setField(s,"baseMapper",mapper);
        var file=new FileDO();file.setParentPath("/");when(mapper.selectById(1L)).thenReturn(file);
        var req=new FileReq();req.setOriginalName("renamed.txt");req.setParentPath("/");s.beforeUpdate(req,1L);assertNull(req.getParentPath());
    }
}
