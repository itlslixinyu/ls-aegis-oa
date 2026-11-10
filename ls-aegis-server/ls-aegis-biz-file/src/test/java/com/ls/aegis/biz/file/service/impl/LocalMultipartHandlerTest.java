package com.ls.aegis.biz.file.service.impl;

import com.ls.aegis.biz.file.handler.impl.LocalStorageHandler;
import com.ls.aegis.biz.file.model.entity.StorageDO;
import com.ls.aegis.biz.file.model.req.MultipartUploadInitReq;
import com.ls.aegis.biz.file.model.resp.file.MultipartUploadResp;
import com.ls.aegis.biz.file.service.FileService;
import com.ls.aegis.crypto.api.IGmCrypto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import java.io.File;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalMultipartHandlerTest {
    @TempDir(factory = ProjectTempDirFactory.class) Path dir;

    static class ProjectTempDirFactory implements TempDirFactory {
        @Override
        public Path createTempDirectory(AnnotatedElementContext elementContext, ExtensionContext extensionContext)
                throws java.io.IOException {
            // 相对路径用例必须与工作目录同盘；Windows系统临时目录可能位于另一个盘。
            Path parent = Path.of("target", "multipart-test-temp").toAbsolutePath();
            Files.createDirectories(parent);
            return Files.createTempDirectory(parent, "case-");
        }
    }
    @SuppressWarnings("unchecked") LocalStorageHandler handler(){return new LocalStorageHandler(mock(FileService.class),(ObjectProvider<IGmCrypto>)mock(ObjectProvider.class));}
    StorageDO storage(boolean relative){var s=new StorageDO();s.setBucketName(relative?Path.of("").toAbsolutePath().relativize(dir).toString():dir.toString());return s;}
    MultipartUploadResp part(int n){var p=new MultipartUploadResp();p.setPartNumber(n);p.setSuccess(true);return p;}
    @Test void servletStyleTransferReceivesAbsoluteDestination() throws Exception {
        var h=handler();var s=storage(true);
        var f=new MockMultipartFile("file","own.txt","text/plain",new byte[]{1,2,3}){
            @Override public void transferTo(File destination) throws java.io.IOException {assertTrue(destination.isAbsolute(),"Servlet resolves relative targets under its own temp directory");super.transferTo(destination);}
        };
        assertTrue(h.uploadPart(s,"own.txt","own-upload",1,f).isSuccess());
        assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(dir.resolve("temp/own-upload/part_1")));
    }
    @Test void missingLaterPartDoesNotTruncatePublishedTarget() throws Exception {
        var h=handler();var s=storage(false);Files.writeString(dir.resolve("own.txt"),"existing stable content");
        assertTrue(h.uploadPart(s,"own.txt","own-upload",1,new MockMultipartFile("file",new byte[]{1})).isSuccess());
        assertThrows(RuntimeException.class,()->h.completeMultipartUpload(s,List.of(part(1),part(2)),"own.txt","own-upload",false));
        assertEquals("existing stable content",Files.readString(dir.resolve("own.txt")));
    }
    @Test void reversedPartsMergeInOrderAndTemporaryDirectoryIsRemoved() throws Exception {
        var h=handler();var s=storage(false);
        h.uploadPart(s,"own.txt","own-upload",2,new MockMultipartFile("file",new byte[]{3}));
        h.uploadPart(s,"own.txt","own-upload",1,new MockMultipartFile("file",new byte[]{1,2}));
        h.completeMultipartUpload(s,List.of(part(2),part(1)),"own.txt","own-upload",false);
        assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(dir.resolve("own.txt")));assertFalse(Files.exists(dir.resolve("temp/own-upload")));
    }
    @Test void omittedOptionalParentDefaultsToRoot() {
        var req=new MultipartUploadInitReq();req.setFileName("synthetic.txt");req.setFileSize(1L);req.setFileDigest("synthetic");
        assertEquals("/",handler().initMultipartUpload(storage(false),req).getParentPath());
    }
}
