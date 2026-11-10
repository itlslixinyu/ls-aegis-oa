package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import lombok.Data;

import java.io.Serial;

@Data
public class PsiTodoResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    private String todoType;
    private String title;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private Boolean done;
}
