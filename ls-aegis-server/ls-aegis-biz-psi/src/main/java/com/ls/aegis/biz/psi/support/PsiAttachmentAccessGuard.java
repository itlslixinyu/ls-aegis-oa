package com.ls.aegis.biz.psi.support;

import cn.dev33.satoken.stp.StpUtil;
import com.ls.aegis.biz.file.access.FileAccessContext;
import com.ls.aegis.biz.file.access.FileAccessService;
import com.ls.aegis.biz.file.mapper.FileMapper;
import com.ls.aegis.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.continew.starter.core.exception.BusinessException;
import java.util.*;

/** Validate BEFORE writing a new reference: a guessed file ID must not grant itself access. */
@Component
@RequiredArgsConstructor
public class PsiAttachmentAccessGuard {
    private final FileMapper files;
    private final FileAccessService access;

    public void require(Long id) {
        if (id == null) return;
        StpUtil.checkLogin();
        var actor=UserContextHolder.getContext();
        if (actor==null || actor.getId()==null || !Objects.equals(StpUtil.getLoginIdAsString(),String.valueOf(actor.getId())))
            throw new BusinessException("附件访问身份无效，请重新登录");
        var file=files.selectById(id);
        if (id<=0 || file==null || file.getType()==com.ls.aegis.biz.file.enums.FileTypeEnum.DIR || (file.getDeleted()!=null && file.getDeleted()!=0L)
            || !access.canAccess(file,FileAccessContext.forPlatformUser()))
            throw new BusinessException("附件不存在或无权引用，请先上传或选择已授权文件");
    }

    public void requireCsv(String csv) {
        if(csv==null || csv.isBlank()) return;
        Set<Long> ids=new HashSet<>();
        for(String part:csv.split(",",-1)) {
            String value=part.trim();
            if(!value.matches("[1-9][0-9]{0,18}")) throw new BusinessException("附件必须为有效文件编号");
            try { ids.add(Long.valueOf(value)); }
            catch(NumberFormatException e){throw new BusinessException("附件编号超出范围");}
        }
        for(Long id:ids) require(id);
    }
}
