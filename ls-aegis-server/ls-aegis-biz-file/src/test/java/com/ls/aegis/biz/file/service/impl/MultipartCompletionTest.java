package com.ls.aegis.biz.file.service.impl;

import com.ls.aegis.biz.file.dao.MultipartUploadDao;
import com.ls.aegis.biz.file.enums.StorageTypeEnum;
import com.ls.aegis.biz.file.factory.StorageHandlerFactory;
import com.ls.aegis.biz.file.handler.StorageHandler;
import com.ls.aegis.biz.file.model.entity.StorageDO;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.file.model.resp.file.*;
import com.ls.aegis.biz.file.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MultipartCompletionTest {
    StorageService storage=mock(StorageService.class);StorageHandlerFactory factory=mock(StorageHandlerFactory.class);
    MultipartUploadDao dao=mock(MultipartUploadDao.class);FileService files=mock(FileService.class);StorageHandler handler=mock(StorageHandler.class);
    MultipartUploadServiceImpl service=new MultipartUploadServiceImpl(storage,factory,dao,files);
    @BeforeEach void setup(){var s=new StorageDO();s.setType(StorageTypeEnum.LOCAL);s.setId(1L);when(storage.getByCode(null)).thenReturn(s);when(factory.createHandler(StorageTypeEnum.LOCAL)).thenReturn(handler);}
    FilePartInfo part(int n,long size){var p=new FilePartInfo();p.setPartNumber(n);p.setPartSize(size);p.setStatus("SUCCESS");return p;}
    void init(){var i=new MultipartUploadInitResp();i.setFileName("synthetic.txt");i.setPath("synthetic.txt");i.setFileSize(6L);i.setPartSize(5L);i.setExtension("txt");when(dao.getMultipartUpload("own")).thenReturn(i);}
    @Test void failedPartIsNotRecordedAsSuccess(){when(handler.uploadPart(any(),any(),any(),any(),any())).thenReturn(new MultipartUploadResp());assertThrows(RuntimeException.class,()->service.uploadPart(new MockMultipartFile("file",new byte[]{1}),"own",1,"synthetic.txt"));verify(dao,never()).setFilePart(any(),any());}
    @Test void missingLastPartCannotComplete(){init();when(dao.getFileParts("own")).thenReturn(List.of(part(1,5)));assertThrows(RuntimeException.class,()->service.completeMultipartUpload("own"));verifyNoInteractions(files);verify(handler,never()).completeMultipartUpload(any(),any(),any(),any(),anyBoolean());}
    @Test void wrongPartSizeCannotComplete(){init();when(dao.getFileParts("own")).thenReturn(List.of(part(1,4),part(2,1)));assertThrows(RuntimeException.class,()->service.completeMultipartUpload("own"));verifyNoInteractions(files);}
    @Test void successfulCompletionClearsPartCacheAsWellAsSession(){init();when(dao.getFileParts("own")).thenReturn(List.of(part(2,1),part(1,5)));when(files.save(any(FileDO.class))).thenReturn(true);service.completeMultipartUpload("own");verify(files).save(any(FileDO.class));verify(dao).deleteMultipartUploadAll("own");}
    @Test void cleanupFailureKeepsSessionForRetry(){doThrow(new IllegalStateException("synthetic IO failure")).when(handler).cleanPart(any(),eq("own"));assertThrows(RuntimeException.class,()->service.cancelMultipartUpload("own"));verify(dao,never()).deleteMultipartUploadAll(any());}
}
