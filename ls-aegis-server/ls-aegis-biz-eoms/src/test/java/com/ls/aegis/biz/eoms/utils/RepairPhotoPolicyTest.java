package com.ls.aegis.biz.eoms.utils;

import com.ls.aegis.compat.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepairPhotoPolicyTest {
    @Test void validatesBytesRatherThanOnlyExtensionOrMime() {
        assertDoesNotThrow(() -> RepairPhotoPolicy.validate(new MockMultipartFile("file","camera","",new byte[]{(byte)137,80,78,71,13,10,26,10})));
        assertDoesNotThrow(() -> RepairPhotoPolicy.validate(new MockMultipartFile("file",new byte[]{(byte)255,(byte)216,(byte)255,1})));
        assertThrows(ServiceException.class, () -> RepairPhotoPolicy.validate(new MockMultipartFile("file","photo.jpg","image/jpeg","not an image".getBytes())));
    }
    @Test void rejectsMissingEmptyOversizedAndUnreadableBeforeDecoding() throws Exception {
        assertThrows(ServiceException.class, () -> RepairPhotoPolicy.validate(null));
        assertThrows(ServiceException.class, () -> RepairPhotoPolicy.validate(new MockMultipartFile("file",new byte[0])));
        var large = mock(MultipartFile.class); when(large.getSize()).thenReturn(10L*1024*1024+1);
        assertThrows(ServiceException.class, () -> RepairPhotoPolicy.validate(large)); verify(large,never()).getInputStream();
        var unreadable = mock(MultipartFile.class); when(unreadable.getInputStream()).thenThrow(new IOException());
        assertThrows(ServiceException.class, () -> RepairPhotoPolicy.validate(unreadable));
    }
}
