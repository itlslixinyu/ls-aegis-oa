package com.ls.aegis.biz.file.util;

import org.junit.jupiter.api.Test;
import com.ls.aegis.biz.file.config.file.FileRecorderImpl;
import com.ls.aegis.biz.file.mapper.FileMapper;
import com.ls.aegis.biz.file.mapper.StorageMapper;
import com.ls.aegis.biz.file.model.entity.StorageDO;
import org.dromara.x.file.storage.core.FileInfo;
import java.net.URI;
import cn.hutool.core.lang.Dict;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileUrlUtilsTest {
    @Test void sameUrlResolvesAgainstLocalAndPublicEntrypoints() {
        var path = FileUrlUtils.join("/api/file/", "/avatar/logo.png");
        assertEquals("http://127.0.0.1:5173/api/file/avatar/logo.png", URI.create("http://127.0.0.1:5173/").resolve(path).toString());
        assertEquals("https://oa.example.test/api/file/avatar/logo.png", URI.create("https://oa.example.test/").resolve(path).toString());
        assertEquals("/avatar/logo.png", FileUrlUtils.storagePath(path));
    }
    @Test void mapsToBackendResourceAndRetainsAbsoluteDomains() {
        assertEquals("/file/", FileUrlUtils.localHandlerPath("/api/file/"));
        assertEquals("/file/", FileUrlUtils.localHandlerPath("http://localhost:8080/api/file/"));
        assertEquals("https://files.example.test/base/avatar/logo.png", FileUrlUtils.join("https://files.example.test/base/", "/avatar/logo.png"));
        assertFalse(FileUrlUtils.isSameOrigin("//other.example.test/api/file/"));
        assertFalse(FileUrlUtils.isSameOrigin("/api/file/../other/"));
    }
    @Test void uploadReturnsSameOriginFileAndThumbnailUrlsWithoutChangingStoredPath() {
        var mapper = mock(FileMapper.class);
        var recorder = new FileRecorderImpl(mapper, mock(StorageMapper.class));
        var storage = new StorageDO(); storage.setId(1L); storage.setDomain("/api/file/");
        var info = new FileInfo();
        info.setUrl("avatar/original.png"); info.setFilename("original.png"); info.setExt("png");
        info.setThUrl("avatar/thumb.png"); info.setPath("avatar/");
        var attr = Dict.create(); attr.put(StorageDO.class.getName(), storage); info.setAttr(attr);
        assertTrue(recorder.save(info));
        assertEquals("/api/file/avatar/original.png", info.getUrl());
        assertEquals("/api/file/avatar/thumb.png", info.getThUrl());
        verify(mapper).insert(org.mockito.ArgumentMatchers.<com.ls.aegis.biz.file.model.entity.FileDO>argThat(f -> "/avatar/original.png".equals(f.getPath())));
    }
}
