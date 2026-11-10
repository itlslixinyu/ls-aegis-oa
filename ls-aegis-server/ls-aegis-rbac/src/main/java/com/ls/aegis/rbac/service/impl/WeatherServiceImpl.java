package com.ls.aegis.rbac.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.ls.aegis.rbac.enums.OptionCategoryEnum;
import com.ls.aegis.rbac.model.resp.WeatherNowResp;
import com.ls.aegis.rbac.service.OptionService;
import com.ls.aegis.rbac.service.WeatherService;
import com.ls.aegis.rbac.util.QWeatherJwtHelper;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.validation.ValidationUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.GZIPInputStream;

/**
 * 运营中枢天气：本地模拟 / 和风天气（API Key 或 JWT）
 * <p>城市一律经和风 GeoAPI 解析（坐标反查或城市名查询），不再使用本地 IP 库。</p>
 * <p>鉴权：控制台「API KEY」凭据填 Key；「JWT」凭据需凭据ID+项目ID+私钥。二者都配时优先 JWT。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private static final String PROVIDER_MOCK = "mock";
    private static final String PROVIDER_QWEATHER = "qweather";
    private static final String CITY_MODE_AUTO = "auto";
    private static final String DEFAULT_CITY = "北京";
    /** 连接超时（毫秒）；和风 TLS 偶发偏慢 */
    private static final int HTTP_CONNECT_TIMEOUT_MS = 8_000;
    /** 读取超时（毫秒）；单次请求含 Gzip 解压 */
    private static final int HTTP_READ_TIMEOUT_MS = 15_000;
    private static final String FALLBACK_LOCATION_ID = "101010100";

    private final OptionService optionService;

    private volatile String cachedJwt;
    private volatile long cachedJwtExpireAtEpoch;

    @Override
    public WeatherNowResp now(String clientIp, Double lat, Double lon) {
        Map<String, String> cfg = optionService.getByCategory(OptionCategoryEnum.WEATHER);
        String provider = normalizeProvider(cfg.get("WEATHER_PROVIDER"));
        String cityMode = StrUtil.blankToDefault(cfg.get("WEATHER_CITY_MODE"), CITY_MODE_AUTO).trim();
        String fallbackCity = resolveFallbackCity(cfg);

        ResolvedLocation location = resolveLocation(lat, lon, cityMode, fallbackCity, cfg);
        if (PROVIDER_QWEATHER.equalsIgnoreCase(provider)) {
            if (!hasAuthConfig(cfg)) {
                log.warn("和风天气鉴权未配置（需 API Key，或完整 JWT），回退本地模拟");
            } else {
                try {
                    WeatherNowResp remote = fetchQWeather(location, cfg);
                    if (remote != null) {
                        return remote;
                    }
                } catch (Exception e) {
                    log.warn("和风天气获取失败，回退本地模拟：{}", e.getMessage());
                }
            }
        }
        return mockWeather(location.city());
    }

    @Override
    public WeatherNowResp testConnection(String clientIp) {
        clearTokenCache();
        Map<String, String> cfg = optionService.getByCategory(OptionCategoryEnum.WEATHER);
        String provider = normalizeProvider(cfg.get("WEATHER_PROVIDER"));
        String cityMode = StrUtil.blankToDefault(cfg.get("WEATHER_CITY_MODE"), CITY_MODE_AUTO).trim();
        String fallbackCity = resolveFallbackCity(cfg);

        if (PROVIDER_MOCK.equalsIgnoreCase(provider)) {
            ResolvedLocation location = resolveLocation(null, null, cityMode, fallbackCity, cfg);
            WeatherNowResp mock = mockWeather(location.city());
            mock.setLabel(mock.getLabel() + "（本地模拟）");
            return mock;
        }

        ValidationUtils.throwIf(!PROVIDER_QWEATHER.equalsIgnoreCase(provider), "请先将数据来源保存为和风天气后再测试");
        ValidationUtils.throwIf(!hasAuthConfig(cfg), "请先配置并保存鉴权：API Key（推荐），或完整 JWT（凭据 ID / 项目 ID / 私钥）");
        ValidationUtils.throwIf(StrUtil.isBlank(normalizeApiHost(cfg.get("WEATHER_API_HOST"))),
            "请填写并保存和风 API Host（控制台「设置」中的专属域名，如 xxx.re.qweatherapi.com）");

        ResolvedLocation location = resolveLocation(null, null, cityMode, fallbackCity, cfg);
        try {
            WeatherNowResp remote = fetchQWeather(location, cfg);
            ValidationUtils.throwIfNull(remote, "和风接口未返回有效天气数据，请检查 API Host 与凭据");
            return remote;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            String msg = StrUtil.blankToDefault(e.getMessage(), "未知错误");
            if (StrUtil.containsIgnoreCase(msg, "timed out") || StrUtil.containsIgnoreCase(msg, "timeout")) {
                throw new BusinessException("和风连接超时，请确认 API Host 可访问（须 https:// 专属域名），并稍后重试");
            }
            throw new BusinessException("和风连接失败：" + msg);
        }
    }

    @Override
    public void clearTokenCache() {
        cachedJwt = null;
        cachedJwtExpireAtEpoch = 0;
    }

    private static String normalizeProvider(String raw) {
        String provider = StrUtil.blankToDefault(raw, PROVIDER_MOCK).trim();
        if ("open-meteo".equalsIgnoreCase(provider)) {
            return PROVIDER_QWEATHER;
        }
        return provider;
    }

    private static String resolveFallbackCity(Map<String, String> cfg) {
        String city = StrUtil.blankToDefault(cfg.get("WEATHER_CITY"), DEFAULT_CITY).trim();
        return StrUtil.isBlank(city) ? DEFAULT_CITY : city;
    }

    /**
     * 解析展示城市与和风 location（LocationID 或 lon,lat）。
     * <ul>
     *   <li>有浏览器坐标：和风坐标反查</li>
     *   <li>固定城市 / 自动无坐标：按配置城市名走和风城市搜索</li>
     * </ul>
     */
    private ResolvedLocation resolveLocation(Double lat,
                                             Double lon,
                                             String cityMode,
                                             String fallbackCity,
                                             Map<String, String> cfg) {
        boolean canLookup = hasAuthConfig(cfg);
        if (lat != null && lon != null && NumberUtil.isValidNumber(lat) && NumberUtil.isValidNumber(lon)) {
            String locationParam = formatLonLat(lon, lat);
            if (canLookup) {
                CityLookup looked = lookupCity(locationParam, cfg);
                if (looked != null) {
                    return new ResolvedLocation(looked.name(), looked.locationId(),
                        coalesce(looked.lat(), lat), coalesce(looked.lon(), lon));
                }
            }
            return new ResolvedLocation(fallbackCity, locationParam, lat, lon);
        }

        String queryCity = fallbackCity;
        if (!CITY_MODE_AUTO.equalsIgnoreCase(cityMode) && StrUtil.isNotBlank(fallbackCity)) {
            queryCity = fallbackCity;
        }
        if (canLookup) {
            CityLookup looked = lookupCity(queryCity, cfg);
            if (looked != null) {
                return new ResolvedLocation(looked.name(), looked.locationId(), looked.lat(), looked.lon());
            }
        }
        // 兜底 LocationID（北京）及中心点坐标，供空气质量 v1 使用
        return new ResolvedLocation(queryCity, FALLBACK_LOCATION_ID, 39.90, 116.40);
    }

    private static Double coalesce(Double preferred, Double fallback) {
        return preferred != null ? preferred : fallback;
    }

    /** 和风要求「经度,纬度」，坐标保留两位小数 */
    private static String formatLonLat(double lon, double lat) {
        return String.format(Locale.US, "%.2f,%.2f", lon, lat);
    }

    private static boolean hasJwtConfig(Map<String, String> cfg) {
        String privateKey = StrUtil.trim(cfg.get("WEATHER_JWT_PRIVATE_KEY"));
        return StrUtil.isNotBlank(cfg.get("WEATHER_JWT_KID"))
            && StrUtil.isNotBlank(cfg.get("WEATHER_JWT_PROJECT_ID"))
            && StrUtil.isNotBlank(privateKey)
            && !"********".equals(privateKey);
    }

    /** 控制台「API KEY」类型凭据的 Key 值（不是 JWT 凭据 ID） */
    private static boolean hasApiKeyConfig(Map<String, String> cfg) {
        String apiKey = StrUtil.trim(cfg.get("WEATHER_API_KEY"));
        return StrUtil.isNotBlank(apiKey) && !"********".equals(apiKey);
    }

    /** API Key 与 JWT 二选一即可 */
    private static boolean hasAuthConfig(Map<String, String> cfg) {
        return hasApiKeyConfig(cfg) || hasJwtConfig(cfg);
    }

    private String resolveBearerToken(Map<String, String> cfg) {
        long now = System.currentTimeMillis() / 1000;
        if (StrUtil.isNotBlank(cachedJwt) && cachedJwtExpireAtEpoch - 60 > now) {
            return cachedJwt;
        }
        synchronized (this) {
            now = System.currentTimeMillis() / 1000;
            if (StrUtil.isNotBlank(cachedJwt) && cachedJwtExpireAtEpoch - 60 > now) {
                return cachedJwt;
            }
            cachedJwt = QWeatherJwtHelper.createToken(
                StrUtil.trim(cfg.get("WEATHER_JWT_KID")),
                StrUtil.trim(cfg.get("WEATHER_JWT_PROJECT_ID")),
                cfg.get("WEATHER_JWT_PRIVATE_KEY"));
            cachedJwtExpireAtEpoch = now - 30 + QWeatherJwtHelper.tokenTtlSeconds();
            return cachedJwt;
        }
    }

    private static String normalizeCityName(String raw) {
        String city = StrUtil.trim(raw);
        city = StrUtil.removeSuffix(city, "市");
        city = StrUtil.removeSuffix(city, "地区");
        city = StrUtil.removeSuffix(city, "自治州");
        return StrUtil.isBlank(city) ? null : city;
    }

    /** 顶栏展示优先市级（adm2），避免只显示「东城」等区名 */
    static String pickCityDisplayName(JSONObject location, String fallback) {
        if (location == null) {
            return fallback;
        }
        String adm2 = normalizeCityName(location.getStr("adm2"));
        if (StrUtil.isNotBlank(adm2)) {
            return adm2;
        }
        String name = normalizeCityName(location.getStr("name"));
        if (StrUtil.isNotBlank(name)) {
            return name;
        }
        String adm1 = normalizeCityName(location.getStr("adm1"));
        if (StrUtil.isNotBlank(adm1)) {
            return adm1;
        }
        return fallback;
    }

    private CityLookup lookupCity(String location, Map<String, String> cfg) {
        if (StrUtil.isBlank(location)) {
            return null;
        }
        String url = buildCityLookupUrl(location, cfg);
        if (StrUtil.isBlank(url)) {
            return null;
        }
        try {
            String body = authorizedGet(url, cfg);
            JSONObject json = parseQWeatherJson(body, "城市查询");
            if (!"200".equals(json.getStr("code"))) {
                log.warn("和风城市查询失败 code={} location={} tip={}", json.getStr("code"), location, describeQWeatherCode(json
                    .getStr("code"), json));
                return null;
            }
            JSONArray locations = json.getJSONArray("location");
            if (locations == null || locations.isEmpty()) {
                return null;
            }
            JSONObject first = locations.getJSONObject(0);
            String id = first.getStr("id");
            if (StrUtil.isBlank(id)) {
                return null;
            }
            String name = pickCityDisplayName(first, location);
            Double cityLat = NumberUtil.parseDouble(first.getStr("lat"), Double.NaN);
            Double cityLon = NumberUtil.parseDouble(first.getStr("lon"), Double.NaN);
            return new CityLookup(name, id,
                NumberUtil.isValidNumber(cityLat) ? cityLat : null,
                NumberUtil.isValidNumber(cityLon) ? cityLon : null);
        } catch (Exception e) {
            log.warn("和风城市查询异常 location={}：{}", location, e.getMessage());
            return null;
        }
    }

    /**
     * 新版控制台 API Host 统一走 /geo/v2/city/lookup；
     * 兼容旧版独立 Geo 域名 geoapi.qweather.com 的 /v2/city/lookup。
     */
    private static String buildCityLookupUrl(String location, Map<String, String> cfg) {
        String geoHost = normalizeApiHost(cfg.get("WEATHER_GEO_HOST"));
        String apiHost = normalizeApiHost(cfg.get("WEATHER_API_HOST"));
        String host;
        String path;
        if (StrUtil.isNotBlank(geoHost)) {
            host = geoHost;
            path = host.contains("geoapi.qweather.com") ? "/v2/city/lookup" : "/geo/v2/city/lookup";
        } else if (StrUtil.isNotBlank(apiHost)) {
            host = apiHost;
            path = "/geo/v2/city/lookup";
        } else {
            return null;
        }
        return host + path + "?location=" + URLUtil.encode(location) + "&range=cn&number=1&lang=zh";
    }

    /** 统一补全 https，去掉尾斜杠；禁止回落到已淘汰的公共域名。 */
    static String normalizeApiHost(String raw) {
        String host = StrUtil.trim(raw);
        if (StrUtil.isBlank(host)) {
            return "";
        }
        host = host.replaceAll("/$", "");
        if (StrUtil.startWithIgnoreCase(host, "http://")) {
            host = "https://" + host.substring("http://".length());
        } else if (!StrUtil.startWithIgnoreCase(host, "https://")) {
            host = "https://" + host;
        }
        return host;
    }

    private WeatherNowResp fetchQWeather(ResolvedLocation location, Map<String, String> cfg) {
        String weatherHost = normalizeApiHost(cfg.get("WEATHER_API_HOST"));
        if (StrUtil.isBlank(weatherHost)) {
            throw new BusinessException("请配置和风 API Host（控制台「设置」专属域名，如 https://xxx.re.qweatherapi.com）");
        }
        String url = weatherHost + "/v7/weather/now?location=" + URLUtil.encode(location.locationParam());
        String body = authorizedGet(url, cfg);
        JSONObject json = parseQWeatherJson(body, "实时天气");
        String code = json.getStr("code");
        if (!"200".equals(code)) {
            throw new BusinessException("和风实时天气失败：" + describeQWeatherCode(code, json));
        }
        JSONObject now = json.getJSONObject("now");
        if (now == null) {
            throw new BusinessException("和风实时天气失败：响应缺少 now 数据，请检查 API Host 与凭据");
        }
        KindMeta kind = mapQWeatherIcon(now.getStr("icon"), now.getStr("text"));
        WeatherNowResp resp = new WeatherNowResp();
        resp.setCity(location.city());
        resp.setKind(kind.code());
        resp.setLabel(StrUtil.blankToDefault(now.getStr("text"), kind.label()));
        resp.setTemp(NumberUtil.parseInt(StrUtil.blankToDefault(now.getStr("temp"), "0"), 0));
        resp.setFeelsLike(parseOptionalInt(now.getStr("feelsLike")));
        resp.setWindLevel(parseWindScale(now.getStr("windScale")));
        resp.setWindDir(StrUtil.blankToDefault(now.getStr("windDir"), null));
        resp.setHumidity(parseOptionalInt(now.getStr("humidity")));
        resp.setVisibility(parseOptionalDouble(now.getStr("vis")));
        resp.setPressure(parseOptionalInt(now.getStr("pressure")));
        resp.setPrecip(parseOptionalDouble(now.getStr("precip")));
        resp.setProvider(PROVIDER_QWEATHER);
        fillAirQuality(resp, location, cfg, weatherHost);
        return resp;
    }

    /**
     * 空气质量：优先 API v1（经纬度），失败再回退 v7/air/now（LocationID，预计 2026-06 停服）。
     * 优先选用中国标准指数（code 以 cn- 开头），类别统一为国标中文。
     */
    private void fillAirQuality(WeatherNowResp resp, ResolvedLocation location, Map<String, String> cfg, String weatherHost) {
        Double lat = location.lat();
        Double lon = location.lon();
        if (lat == null || lon == null) {
            double[] parsed = parseLonLat(location.locationParam());
            if (parsed != null) {
                lon = parsed[0];
                lat = parsed[1];
            }
        }
        boolean filled = false;
        if (lat != null && lon != null && NumberUtil.isValidNumber(lat) && NumberUtil.isValidNumber(lon)) {
            filled = fillAirQualityV1(resp, cfg, weatherHost, lat, lon);
        } else {
            log.warn("和风空气质量 v1 跳过：缺少经纬度 city={}", location.city());
        }
        if (!filled) {
            filled = fillAirQualityV7(resp, location, cfg, weatherHost);
        }
        if (!filled) {
            resp.setAqi(null);
            resp.setAqiCategory("--");
        }
    }

    private boolean fillAirQualityV1(WeatherNowResp resp, Map<String, String> cfg, String weatherHost, double lat, double lon) {
        String url = String.format(Locale.US, "%s/airquality/v1/current/%.2f/%.2f?lang=zh", weatherHost, lat, lon);
        try {
            String body = authorizedGet(url, cfg);
            if (StrUtil.isBlank(body) || !(body.trim().startsWith("{"))) {
                throw new BusinessException("非 JSON 响应");
            }
            JSONObject json = JSONUtil.parseObj(body.trim());
            if (StrUtil.isNotBlank(json.getStr("code")) && !"200".equals(json.getStr("code"))) {
                throw new BusinessException(describeQWeatherCode(json.getStr("code"), json));
            }
            JSONObject index = pickAirIndex(json.getJSONArray("indexes"));
            if (index == null) {
                log.warn("和风空气质量 v1 无 indexes：{}", StrUtil.maxLength(body.replaceAll("\\s+", " "), 160));
                return false;
            }
            applyAirIndex(resp, index);
            return resp.getAqi() != null || StrUtil.isNotBlank(resp.getAqiCategory());
        } catch (Exception e) {
            log.warn("和风空气质量 v1 失败 lat={},lon={}：{}", lat, lon, e.getMessage());
            return false;
        }
    }

    /** 兼容旧版实时空气质量，location 支持 LocationID 或「经度,纬度」 */
    private boolean fillAirQualityV7(WeatherNowResp resp, ResolvedLocation location, Map<String, String> cfg, String weatherHost) {
        String loc = StrUtil.blankToDefault(location.locationParam(), FALLBACK_LOCATION_ID);
        String url = weatherHost + "/v7/air/now?location=" + URLUtil.encode(loc) + "&lang=zh";
        try {
            String body = authorizedGet(url, cfg);
            JSONObject json = parseQWeatherJson(body, "空气质量");
            if (!"200".equals(json.getStr("code"))) {
                log.warn("和风空气质量 v7 失败 code={} tip={}", json.getStr("code"), describeQWeatherCode(json.getStr("code"), json));
                return false;
            }
            JSONObject now = json.getJSONObject("now");
            if (now == null) {
                return false;
            }
            Integer aqi = parseAqiNumber(now.get("aqi"));
            String category = normalizeAqiCategory(now.getStr("category"), aqi);
            resp.setAqi(aqi);
            resp.setAqiCategory(category);
            return aqi != null || StrUtil.isNotBlank(category);
        } catch (Exception e) {
            log.warn("和风空气质量 v7 失败 location={}：{}", loc, e.getMessage());
            return false;
        }
    }

    private static void applyAirIndex(WeatherNowResp resp, JSONObject index) {
        Integer aqi = parseAqiNumber(index.get("aqi"));
        if (aqi == null) {
            aqi = parseAqiNumber(index.get("aqiDisplay"));
        }
        String category = normalizeAqiCategory(index.getStr("category"), aqi);
        resp.setAqi(aqi);
        resp.setAqiCategory(category);
    }

    /** 解析 AQI 数值；qaqi 等小数按四舍五入为整数便于展示与预警 */
    static Integer parseAqiNumber(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            double v = number.doubleValue();
            if (!NumberUtil.isValidNumber(v)) {
                return null;
            }
            return (int) Math.round(v);
        }
        String text = StrUtil.trim(String.valueOf(raw));
        if (StrUtil.isBlank(text) || !NumberUtil.isNumber(text)) {
            return null;
        }
        double v = NumberUtil.parseDouble(text, Double.NaN);
        if (!NumberUtil.isValidNumber(v)) {
            return null;
        }
        return (int) Math.round(v);
    }

    /** 将类别归一为国标中文；英文或空时按 AQI 数值推断 */
    static String normalizeAqiCategory(String raw, Integer aqi) {
        String cat = StrUtil.trim(raw);
        if (StrUtil.isNotBlank(cat)) {
            String lower = cat.toLowerCase(Locale.ROOT);
            if (cat.contains("优") || cat.contains("良") || cat.contains("污染")) {
                return cat;
            }
            if (lower.contains("excellent")) {
                return "优";
            }
            if (lower.contains("good")) {
                return aqi != null && aqi <= 50 ? "优" : "良";
            }
            if (lower.contains("moderate")) {
                return "轻度污染";
            }
            if (lower.contains("unhealthy for sensitive")) {
                return "中度污染";
            }
            if (lower.contains("unhealthy") && !lower.contains("very") && !lower.contains("hazardous")) {
                return "重度污染";
            }
            if (lower.contains("very unhealthy") || lower.contains("hazardous")) {
                return "严重污染";
            }
        }
        if (aqi == null) {
            return StrUtil.blankToDefault(cat, "--");
        }
        if (aqi <= 50) {
            return "优";
        }
        if (aqi <= 100) {
            return "良";
        }
        if (aqi <= 150) {
            return "轻度污染";
        }
        if (aqi <= 200) {
            return "中度污染";
        }
        if (aqi <= 300) {
            return "重度污染";
        }
        return "严重污染";
    }

    /** locationParam 为「经度,纬度」时解析；否则返回 null */
    private static double[] parseLonLat(String locationParam) {
        if (StrUtil.isBlank(locationParam) || !locationParam.contains(",")) {
            return null;
        }
        String[] parts = locationParam.split(",");
        if (parts.length != 2) {
            return null;
        }
        double lon = NumberUtil.parseDouble(StrUtil.trim(parts[0]), Double.NaN);
        double lat = NumberUtil.parseDouble(StrUtil.trim(parts[1]), Double.NaN);
        if (!NumberUtil.isValidNumber(lon) || !NumberUtil.isValidNumber(lat)) {
            return null;
        }
        return new double[] {lon, lat};
    }

    static JSONObject pickAirIndex(JSONArray indexes) {
        if (indexes == null || indexes.isEmpty()) {
            return null;
        }
        JSONObject fallback = null;
        JSONObject qaqi = null;
        for (int i = 0; i < indexes.size(); i++) {
            JSONObject item = indexes.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String code = StrUtil.blankToDefault(item.getStr("code"), "").toLowerCase(Locale.ROOT);
            if (code.startsWith("cn-") || "cn-mee".equals(code) || code.contains("china")) {
                return item;
            }
            if ("qaqi".equals(code)) {
                qaqi = item;
            }
            if (fallback == null) {
                fallback = item;
            }
        }
        return qaqi != null ? qaqi : fallback;
    }

    private String authorizedGet(String url, Map<String, String> cfg) {
        // 和风始终返回 Gzip（即使未声明 Accept-Encoding），需可靠解压后再解析 JSON
        var request = HttpRequest.get(url)
            .header(Header.ACCEPT_ENCODING, "gzip")
            .setConnectionTimeout(HTTP_CONNECT_TIMEOUT_MS)
            .setReadTimeout(HTTP_READ_TIMEOUT_MS);
        // 官方：JWT 与 API Key 勿同时使用；完整 JWT 优先，否则走 API Key
        if (hasJwtConfig(cfg)) {
            request.header(Header.AUTHORIZATION, "Bearer " + resolveBearerToken(cfg));
        } else if (hasApiKeyConfig(cfg)) {
            request.header("X-QW-Api-Key", StrUtil.trim(cfg.get("WEATHER_API_KEY")));
        } else {
            throw new BusinessException("和风鉴权未配置：请填写 API Key，或配置完整 JWT");
        }
        var response = request.execute();
        int status = response.getStatus();
        String body = decodeQWeatherBody(response.bodyBytes());
        if (status < 200 || status >= 300) {
            String preview = StrUtil.maxLength(StrUtil.blankToDefault(body, "").replaceAll("\\s+", " "), 120);
            throw new BusinessException("和风 HTTP " + status + (StrUtil.isBlank(preview) ? "" : "：" + preview));
        }
        return body;
    }

    /** 若响应仍是 Gzip 魔数，手动解压（兼容未自动解压的客户端行为）。 */
    static String decodeQWeatherBody(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return "";
        }
        byte[] data = raw;
        if (raw.length >= 2 && raw[0] == (byte) 0x1f && raw[1] == (byte) 0x8b) {
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(raw))) {
                data = IoUtil.readBytes(gzip);
            } catch (Exception e) {
                throw new BusinessException("和风响应 Gzip 解压失败");
            }
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    private static JSONObject parseQWeatherJson(String body, String scene) {
        if (StrUtil.isBlank(body)) {
            throw new BusinessException("和风" + scene + "失败：响应为空，请检查 API Host 是否可访问");
        }
        String trimmed = body.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            String preview = StrUtil.maxLength(trimmed.replaceAll("\\s+", " "), 120);
            throw new BusinessException("和风" + scene + "失败：非 JSON 响应，请确认 API Host（控制台项目地址）。预览：" + preview);
        }
        try {
            return JSONUtil.parseObj(trimmed);
        } catch (Exception e) {
            throw new BusinessException("和风" + scene + "失败：JSON 解析错误");
        }
    }

    private static String describeQWeatherCode(String code, JSONObject json) {
        if (StrUtil.isBlank(code)) {
            return "响应无 code，请填写控制台「API Host」，并确认 API Key 或 JWT 凭据正确";
        }
        String message = json.getStr("message");
        return switch (code) {
            case "401", "402", "403" -> "鉴权失败（" + code + "），请检查 API Key，或 JWT（凭据 ID / 项目 ID / 私钥）是否匹配";
            case "404" -> "查询的数据或路径不存在（404），请检查 API Host 与 Location";
            case "429" -> "请求过于频繁（429），请稍后再试";
            case "500" -> "和风服务异常（500）";
            default -> StrUtil.blankToDefault(message, "code=" + code);
        };
    }

    private WeatherNowResp mockWeather(String city) {
        String[] kinds = {"sunny", "cloudy", "rainy", "storm", "thunder", "hail", "snow", "fog", "sand"};
        String[] labels = {"晴", "多云", "雨", "暴雨", "雷电", "冰雹", "暴雪", "大雾", "沙尘"};
        String[] aqiCategories = {"优", "良", "轻度污染", "中度污染", "重度污染", "严重污染"};
        int[] aqiSamples = {35, 72, 120, 175, 250, 350};
        int idx = ThreadLocalRandom.current().nextInt(kinds.length);
        int aqiIdx = ThreadLocalRandom.current().nextInt(aqiCategories.length);
        WeatherNowResp resp = new WeatherNowResp();
        resp.setCity(StrUtil.blankToDefault(city, DEFAULT_CITY));
        resp.setKind(kinds[idx]);
        resp.setLabel(labels[idx]);
        resp.setTemp(-15 + ThreadLocalRandom.current().nextInt(58));
        resp.setFeelsLike(resp.getTemp() - 2 + ThreadLocalRandom.current().nextInt(5));
        resp.setAqi(aqiSamples[aqiIdx]);
        resp.setAqiCategory(aqiCategories[aqiIdx]);
        resp.setWindLevel(ThreadLocalRandom.current().nextInt(13));
        String[] windDirs = {"北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"};
        resp.setWindDir(windDirs[ThreadLocalRandom.current().nextInt(windDirs.length)]);
        resp.setHumidity(20 + ThreadLocalRandom.current().nextInt(71));
        resp.setVisibility(0.5 + ThreadLocalRandom.current().nextDouble() * 24.5);
        resp.setPressure(990 + ThreadLocalRandom.current().nextInt(40));
        resp.setPrecip(ThreadLocalRandom.current().nextDouble() * 12);
        resp.setProvider(PROVIDER_MOCK);
        return resp;
    }

    static KindMeta mapQWeatherIcon(String icon, String text) {
        int code = NumberUtil.parseInt(StrUtil.blankToDefault(icon, "-1"), -1);
        if (StrUtil.contains(text, "雹")) {
            return new KindMeta("hail", "冰雹");
        }
        if (StrUtil.containsAny(text, "雷")) {
            return new KindMeta("thunder", "雷电");
        }
        if (code == 100 || code == 150) {
            return new KindMeta("sunny", "晴");
        }
        if ((code >= 101 && code <= 104) || (code >= 151 && code <= 153)) {
            return new KindMeta("cloudy", "多云");
        }
        if (code == 304) {
            return new KindMeta("hail", "冰雹");
        }
        if (code == 302 || code == 303) {
            return new KindMeta("thunder", "雷电");
        }
        if (code == 308 || code == 310 || code == 311 || code == 312) {
            return new KindMeta("storm", "暴雨");
        }
        if (code >= 300 && code <= 399) {
            return new KindMeta("rainy", "雨");
        }
        if (code == 402 || code == 403 || code == 410) {
            return new KindMeta("snow", "暴雪");
        }
        if (code >= 400 && code <= 499) {
            return new KindMeta("snow", "雪");
        }
        if ((code >= 500 && code <= 502) || code == 509 || code == 510 || code == 511 || code == 514 || code == 515) {
            return new KindMeta("fog", "大雾");
        }
        if ((code >= 503 && code <= 508) || code == 512 || code == 513) {
            return new KindMeta("sand", "沙尘");
        }
        return new KindMeta("cloudy", StrUtil.blankToDefault(text, "多云"));
    }

    static Integer parseOptionalInt(String raw) {
        if (StrUtil.isBlank(raw) || "--".equals(raw.trim())) {
            return null;
        }
        String v = raw.trim();
        if (!NumberUtil.isInteger(v)) {
            return null;
        }
        return NumberUtil.parseInt(v);
    }

    static Double parseOptionalDouble(String raw) {
        if (StrUtil.isBlank(raw) || "--".equals(raw.trim())) {
            return null;
        }
        String v = raw.trim();
        if (!NumberUtil.isNumber(v)) {
            return null;
        }
        return NumberUtil.parseDouble(v);
    }

    static int parseWindScale(String windScale) {
        if (StrUtil.isBlank(windScale)) {
            return 0;
        }
        String first = StrUtil.subBefore(windScale, '-', false);
        return Math.max(0, Math.min(12, NumberUtil.parseInt(StrUtil.trim(first), 0)));
    }

    private record ResolvedLocation(String city, String locationParam, Double lat, Double lon) {
    }

    private record CityLookup(String name, String locationId, Double lat, Double lon) {
    }

    record KindMeta(String code, String label) {
    }
}
