package com.ls.aegis.biz.eoms.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.core.util.validation.ValidationUtils;
import top.continew.starter.web.model.R;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.model.req.BaiduAkTestReq;
import com.ls.aegis.biz.eoms.model.resp.BaiduAkTestResp;
import com.ls.aegis.biz.eoms.utils.GeoUtils;
import com.ls.aegis.common.api.system.SysOptionApi;

/**
 * 运维报修系统配置辅助接口（AK 测试等；参数读写见系统配置「运维报修」页签）
 */
@Tag(name = "运维报修配置 API")
@RestController
@RequestMapping("/eoms/config")
public class EomsConfigController {

    @Autowired
    private GeoUtils geoUtils;

    @Autowired
    private SysOptionApi sysOptionApi;

    @Operation(summary = "查看百度地图 AK 明文", description = "配置页点击「显示」时使用；仅返回已保存 AK")
    @SaCheckPermission(value = {"system:eomsConfig:get", "system:eomsConfig:update"}, mode = SaMode.OR)
    @GetMapping("/baidu-ak/plain")
    public R<String> getBaiduAkPlain() {
        String ak = sysOptionApi.getValue(EomsConstants.OPTION_CATEGORY, EomsConstants.OPTION_CODE_GEO_BAIDU_AK);
        ValidationUtils.throwIfBlank(ak, "百度地图 AK 尚未配置");
        return R.ok(ak);
    }

    @Operation(summary = "测试百度地图 AK", description = "使用浏览器当前位置坐标与已保存 AK 做逆地理编码验证")
    @SaCheckPermission(value = {"system:eomsConfig:get", "system:eomsConfig:update"}, mode = SaMode.OR)
    @PostMapping("/baidu-ak/test")
    public R<BaiduAkTestResp> testBaiduAk(@RequestBody @Valid BaiduAkTestReq req) {
        return R.ok(geoUtils.testBaiduAk(req.getLongitude(), req.getLatitude()));
    }
}
