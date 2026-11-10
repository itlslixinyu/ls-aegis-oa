package com.ls.aegis.biz.eoms.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 百度地图 AK 连通性测试结果
 */
@Data
@Schema(description = "百度地图 AK 测试结果")
public class BaiduAkTestResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否可用")
    private boolean success;

    @Schema(description = "说明")
    private String message;

    @Schema(description = "样例逆地理地址（成功时）")
    private String sampleAddress;
}
