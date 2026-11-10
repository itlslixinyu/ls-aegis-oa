package com.ls.aegis.biz.eoms.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 百度地图 AK 测试请求（浏览器 WGS84 坐标）
 */
@Data
@Schema(description = "百度地图 AK 测试请求")
public class BaiduAkTestReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "经度（WGS84）", example = "116.397128")
    @NotBlank(message = "经度不能为空")
    private String longitude;

    @Schema(description = "纬度（WGS84）", example = "39.916527")
    @NotBlank(message = "纬度不能为空")
    private String latitude;
}
