package com.ls.aegis.rbac.auth.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 国密公钥（仅公钥，不下发私钥）。
 */
@Data
@Schema(description = "国密公钥")
public class GmPublicKeyResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否启用国密", example = "true")
    private boolean enable;

    @Schema(description = "算法", example = "SM2")
    private String algorithm;

    @Schema(description = "SM2 公钥十六进制（含 04 前缀，供前端 sm-crypto）")
    private String publicKeyHex;

    @Schema(description = "SM2 公钥 Base64（X.509）")
    private String publicKeyBase64;
}
