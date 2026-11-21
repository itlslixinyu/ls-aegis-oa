package com.ls.aegis.biz.file.access;

import cn.hutool.core.util.StrUtil;
import com.ls.aegis.biz.file.model.entity.FileDO;
import org.springframework.stereotype.Service;
import top.continew.starter.core.constant.StringConstants;

import java.util.List;

/**
 * 平台 sys_file 统一访问控制。
 */
@Service
public class FileAccessService {

    private static final String AVATAR_PATH_SEGMENT = "user/avatar/";

    private final List<FileAccessChecker> checkers;

    public FileAccessService(List<FileAccessChecker> checkers) {
        this.checkers = checkers != null ? checkers : List.of();
    }

    public boolean canAccess(FileDO file, FileAccessContext context) {
        if (file == null || context == null) {
            return false;
        }
        if (isPublicAvatar(file)) {
            return true;
        }
        if (context.isSuperAdmin()) {
            return true;
        }
        Long userId = context.getUserId();
        if (userId != null && userId.equals(file.getCreateUser())) {
            return true;
        }
        for (FileAccessChecker checker : checkers) {
            if (checker.canAccess(file, context)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublicAvatar(FileDO file) {
        String parentPath = StrUtil.blankToDefault(file.getParentPath(), StringConstants.EMPTY);
        String path = StrUtil.blankToDefault(file.getPath(), StringConstants.EMPTY);
        return parentPath.contains(AVATAR_PATH_SEGMENT) || path.contains(AVATAR_PATH_SEGMENT);
    }
}
