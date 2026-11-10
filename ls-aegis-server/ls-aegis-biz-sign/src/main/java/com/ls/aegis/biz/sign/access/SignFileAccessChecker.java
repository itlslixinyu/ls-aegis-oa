package com.ls.aegis.biz.sign.access;

import com.ls.aegis.biz.file.access.FileAccessChecker;
import com.ls.aegis.biz.file.access.FileAccessContext;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.sign.domain.SignFile;
import com.ls.aegis.biz.sign.mapper.SignFileMapper;
import com.ls.aegis.biz.sign.service.impl.SignFileServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 签章模块文件访问校验。
 * <p>仅依赖 Mapper，避免 FileService ↔ SignFileService 循环依赖。</p>
 */
@Component
public class SignFileAccessChecker implements FileAccessChecker {

    @Autowired
    private SignFileMapper signFileMapper;

    @Override
    public boolean canAccess(FileDO file, FileAccessContext context) {
        if (file == null || file.getId() == null || !context.isPlatformLogin()) {
            return false;
        }
        String pointer = SignFileServiceImpl.SYS_FILE_PREFIX + file.getId();
        SignFile signFile = signFileMapper.selectFileByPath(pointer);
        if (signFile == null) {
            return false;
        }
        if (context.isSuperAdmin()) {
            return true;
        }
        Long userId = context.getUserId();
        return userId != null && userId.equals(signFile.getOwnerId());
    }
}
