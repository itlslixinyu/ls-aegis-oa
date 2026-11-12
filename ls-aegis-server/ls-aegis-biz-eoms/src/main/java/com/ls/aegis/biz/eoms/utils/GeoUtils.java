package com.ls.aegis.biz.eoms.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ls.aegis.compat.common.utils.StringUtils;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.math.BigDecimal;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.model.resp.BaiduAkTestResp;
import com.ls.aegis.common.api.system.SysOptionApi;

/**
 * 地理位置工具类
 * 当前外部地图调用已停用，Android负责系统地址解析，本类仅提供兼容坐标回退。
 * 历史供应商实现与配置保留用于兼容，不会读取密钥或发起地图请求。
 *
 * 配置项存储在 sys_option（category=EOMS，系统配置「运维报修」页签）：
 *   GEO_BAIDU_AK          百度地图 Web 服务 AK（必填，为空时降级为坐标字符串）
 *   GEO_BAIDU_REGEO_URL   逆地理编码接口地址（可选，默认 https://api.map.baidu.com/reverse_geocoding/v3/）
 *
 * 百度地图 AK 申请：https://lbsyun.baidu.com/apiconsole/key （选择"应用类型=服务端"）
 *
 * 坐标系说明：
 *   浏览器 navigator.geolocation 获取的为 WGS84 坐标（GPS 国际标准），
 *   百度 API 通过 coordtype=wgs84ll 参数自动转换为 bd09ll 进行解析，无需前端做坐标转换。
 *
 * 地址精度策略：
 *   1. 优先从 POI 列表中筛选住宅类（住宅区/内部楼栋），拼接为"省市区+小区名（距XX米）"
 *   2. 其次使用 formatted_address_poi（含 POI 的基础地址）
 *   3. 再次使用 formatted_address（街道级地址）
 *   4. 降级返回 "纬度,经度" 坐标字符串，不阻断主流程
 *
 * @author EOMS
 */
@Component
public class GeoUtils
{
    private static final Logger log = LoggerFactory.getLogger(GeoUtils.class);

    // Map requests contain credentials and location. Never route through legacy HttpUtils URL/body logging.
    private static final HttpClient MAP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(1)).followRedirects(HttpClient.Redirect.NEVER).build();
    @Autowired
    private SysOptionApi sysOptionApi;

    /** 外部地图已按产品要求停用，仅保留兼容接口与本地坐标回退。 */
    public static boolean externalGeocodingEnabled() { return false; }

    /**
     * 测试已保存的百度地图 AK 是否可用
     *
     * @param longitude 经度（WGS84）
     * @param latitude  纬度（WGS84）
     */
    public BaiduAkTestResp testBaiduAk(String longitude, String latitude)
    {
        BaiduAkTestResp resp = new BaiduAkTestResp();
        if (!externalGeocodingEnabled()) {
            resp.setSuccess(false);
            resp.setMessage("外部地图API已停用，使用Android系统地址解析");
            return resp;
        }
        String ak = getConfigValue(EomsConstants.OPTION_CODE_GEO_BAIDU_AK);
        if (StringUtils.isBlank(ak))
        {
            resp.setSuccess(false);
            resp.setMessage("请先配置并保存百度地图 AK");
            return resp;
        }
        if (StringUtils.isEmpty(longitude) || StringUtils.isEmpty(latitude))
        {
            resp.setSuccess(false);
            resp.setMessage("定位坐标无效，请重新获取浏览器位置");
            return resp;
        }

        try { Coordinates.require(longitude, latitude); }
        catch (RuntimeException invalid) {
            resp.setSuccess(false);
            resp.setMessage("定位坐标无效，请重新获取浏览器位置");
            return resp;
        }
        String address = reverseGeocode(longitude, latitude);
        String coordFallback = latitude + "," + longitude;
        if (StringUtils.isEmpty(address) || coordFallback.equals(address))
        {
            resp.setSuccess(false);
            resp.setMessage("逆地理编码失败，请检查 AK 类型、配额与 IP 白名单");
            return resp;
        }

        resp.setSuccess(true);
        resp.setMessage("AK 可用");
        resp.setSampleAddress(address);
        return resp;
    }

    /**
     * 逆地理编码：经纬度转中文地址
     *
     * @param longitude 经度（字符串，如 "116.481488"）
     * @param latitude  纬度（字符串，如 "39.990464"）
     * @return 中文地址；未配置 AK 或调用失败时返回 "纬度,经度" 坐标字符串
     */
    public String reverseGeocode(String longitude, String latitude)
    {
        if (StringUtils.isEmpty(longitude) || StringUtils.isEmpty(latitude))
        {
            return StringUtils.EMPTY;
        }

        Coordinates.require(longitude, latitude);
        if (!externalGeocodingEnabled()) return latitude + "," + longitude;
        String ak = getConfigValue(EomsConstants.OPTION_CODE_GEO_BAIDU_AK);
        // 未配置 AK，降级为坐标字符串
        if (StringUtils.isBlank(ak))
        {
            log.debug("百度地图 AK 未配置，降级返回坐标字符串。请在系统配置「运维报修」填写 GEO_BAIDU_AK");
            return latitude + "," + longitude;
        }

        String regeoUrl = getConfigValue(EomsConstants.OPTION_CODE_GEO_BAIDU_REGEO_URL);
        if (StringUtils.isBlank(regeoUrl))
        {
            regeoUrl = EomsConstants.DEFAULT_GEO_BAIDU_REGEO_URL;
        }

        try
        {
            // 百度逆地理编码 v3 接口：location=纬度,经度（注意与高德的"经度,纬度"相反）
            // coordtype=wgs84ll 表示输入为 WGS84 坐标，百度自动转 bd09ll 解析
            // extensions_poi=2 启用 POI 返回，可精确到小区/楼栋级别
            // radius=500 搜索半径500米，覆盖较大范围的住宅区
            String param = "location=" + encode(latitude + "," + longitude)
                    + "&ak=" + encode(ak)
                    + "&output=json"
                    + "&coordtype=wgs84ll"
                    + "&extensions_poi=2"
                    + "&radius=500";
            URI endpoint = URI.create(regeoUrl);
            if (!("https".equalsIgnoreCase(endpoint.getScheme()) || "http".equalsIgnoreCase(endpoint.getScheme()))
                    || endpoint.getRawQuery() != null || endpoint.getRawFragment() != null || endpoint.getUserInfo() != null) {
                throw new IllegalArgumentException("Invalid map endpoint");
            }
            HttpRequest request = HttpRequest.newBuilder(URI.create(regeoUrl + "?" + param))
                    .timeout(Duration.ofSeconds(3)).header("Accept", "application/json").GET().build();
            var pending = MAP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            HttpResponse<String> response;
            try { response = pending.get(3, java.util.concurrent.TimeUnit.SECONDS); }
            finally { if (!pending.isDone()) pending.cancel(true); }
            if (response.statusCode() != 200) {
                log.warn("地图逆编码HTTP失败，status={}", response.statusCode());
                return latitude + "," + longitude;
            }
            String result = response.body();
            JSONObject json = JSON.parseObject(result);
            if (json != null && json.getInteger("status") != null && json.getIntValue("status") == 0)
            {
                JSONObject resObj = json.getJSONObject("result");
                if (resObj != null)
                {
                    String address = buildAddressFromResult(resObj);
                    if (StringUtils.isNotEmpty(address))
                    {
                        return address;
                    }
                }
            }
            log.warn("地图逆编码业务失败或地址为空");
        }
        catch (Exception e)
        {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("地图逆编码异常，type={}", e.getClass().getSimpleName());
        }
        // 调用失败降级为坐标字符串
        return latitude + "," + longitude;
    }

    /**
     * 从百度逆地理编码 result 对象构建精确地址
     * 优先级：住宅类POI > formatted_address_poi > formatted_address
     *
     * @param resObj 百度 API 返回的 result 对象
     * @return 中文地址字符串
     */
    private String buildAddressFromResult(JSONObject resObj)
    {
        JSONObject addrComponent = resObj.getJSONObject("addressComponent");
        // 构建基础地址前缀：省+市+区+街道
        String basePrefix = buildBasePrefix(addrComponent);

        // 1. 优先从 POI 列表中筛选住宅类 POI（住宅区/内部楼栋）
        String residentialAddr = findResidentialPoiAddress(resObj, basePrefix);
        if (StringUtils.isNotEmpty(residentialAddr))
        {
            return residentialAddr;
        }

        // 2. 其次使用 formatted_address_poi（含 POI 的基础地址）
        String addrPoi = resObj.getString("formatted_address_poi");
        if (StringUtils.isNotEmpty(addrPoi))
        {
            return addrPoi;
        }

        // 3. 再次使用 formatted_address（街道级地址）
        String addr = resObj.getString("formatted_address");
        if (StringUtils.isNotEmpty(addr))
        {
            return addr;
        }

        return StringUtils.EMPTY;
    }

    /**
     * 构建地址前缀：省+市+区+街道
     */
    private String buildBasePrefix(JSONObject addrComponent)
    {
        if (addrComponent == null)
        {
            return StringUtils.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        appendIfNotEmpty(sb, addrComponent.getString("province"));
        appendIfNotEmpty(sb, addrComponent.getString("city"));
        appendIfNotEmpty(sb, addrComponent.getString("district"));
        appendIfNotEmpty(sb, addrComponent.getString("town"));
        return sb.toString();
    }

    private void appendIfNotEmpty(StringBuilder sb, String value)
    {
        if (StringUtils.isNotEmpty(value))
        {
            sb.append(value);
        }
    }

    /**
     * 从 POI 列表中筛选最近的住宅类 POI，拼接为"省市区+小区名（距XX米）"格式
     * 住宅类标签：房地产;住宅区、房地产;内部楼栋
     *
     * @param resObj     百度 API 返回的 result 对象
     * @param basePrefix 地址前缀（省市区街道）
     * @return 住宅类地址，如 "河北省廊坊市广阳区南尖塔镇华夏奥韵（西北199米）"
     */
    private String findResidentialPoiAddress(JSONObject resObj, String basePrefix)
    {
        JSONArray pois = resObj.getJSONArray("pois");
        if (pois == null || pois.isEmpty())
        {
            return StringUtils.EMPTY;
        }

        String nearest = StringUtils.EMPTY;
        BigDecimal nearestDistance = null;
        // Do not assume provider POIs are already sorted. Ignore invalid distances.
        for (int i = 0; i < pois.size(); i++)
        {
            JSONObject poi = pois.getJSONObject(i);
            if (poi == null)
            {
                continue;
            }
            String tag = poi.getString("tag");
            if (tag == null)
            {
                continue;
            }
            // 筛选住宅类 POI：住宅区或内部楼栋
            if (tag.contains("住宅区") || tag.contains("内部楼栋"))
            {
                String name = poi.getString("name");
                String distance = poi.getString("distance");
                if (StringUtils.isNotEmpty(name))
                {
                    // 构建地址：省市区+小区名（方向+距离）
                    String direction = poi.getString("direction");
                    String suffix = buildDirectionSuffix(direction, distance);
                    BigDecimal meters;
                    try { meters = new BigDecimal(distance); }
                    catch (Exception invalid) { continue; }
                    if (meters.signum() < 0) continue;
                    if (nearestDistance == null || meters.compareTo(nearestDistance) < 0) {
                        nearestDistance = meters;
                        nearest = basePrefix + name + suffix;
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * 构建方向距离后缀，如"（西北199米）"
     * distance=0 且 direction=内 表示定位点就在小区内，不加后缀
     */
    private String buildDirectionSuffix(String direction, String distance)
    {
        if (StringUtils.isEmpty(distance))
        {
            return StringUtils.EMPTY;
        }
        // 就在小区内，不加后缀
        if ("0".equals(distance) && "内".equals(direction))
        {
            return StringUtils.EMPTY;
        }
        if (StringUtils.isNotEmpty(direction) && !"内".equals(direction))
        {
            return "（" + direction + distance + "米）";
        }
        // direction 为 "内" 但有距离偏移
        return "（附近" + distance + "米）";
    }

    /**
     * 从 sys_option（EOMS）读取配置值
     */
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private String getConfigValue(String optionCode)
    {
        try
        {
            return sysOptionApi.getValue(EomsConstants.OPTION_CATEGORY, optionCode);
        }
        catch (Exception e)
        {
            log.warn("读取地图配置失败，type={}", e.getClass().getSimpleName());
            return null;
        }
    }
}
