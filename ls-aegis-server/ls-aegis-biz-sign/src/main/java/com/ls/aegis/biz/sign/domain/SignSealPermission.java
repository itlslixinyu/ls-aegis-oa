package com.ls.aegis.biz.sign.domain;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

/**
 * 印章使用授权表 sign_seal_permission
 */
public class SignSealPermission extends XmlMapperDO {

    private static final long serialVersionUID = 1L;

    private Long sealId;
    private Long userId;

    // --- 扩展字段 ---
    private String userName;
    private String nickName;

    public Long getSealId() {
        return sealId;
    }

    public void setSealId(Long sealId) {
        this.sealId = sealId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
}
