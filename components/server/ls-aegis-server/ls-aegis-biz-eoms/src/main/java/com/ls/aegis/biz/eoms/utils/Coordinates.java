package com.ls.aegis.biz.eoms.utils;

import java.math.BigDecimal;
import com.ls.aegis.compat.common.exception.ServiceException;

/** Validate before any geocoding, file write or work-order transition. Zero is a valid coordinate. */
public final class Coordinates {
    private Coordinates() {}
    public static void require(String longitude, String latitude) { validate(longitude, latitude, true); }
    public static void optional(String longitude, String latitude) { validate(longitude, latitude, false); }
    private static void validate(String longitude, String latitude, boolean required) {
        if (!required && longitude == null && latitude == null) return;
        if (!valid(longitude, 180) || !valid(latitude, 90)) {
            throw new ServiceException("定位坐标无效：经纬度必须成对提供有效数字，经度[-180,180]、纬度[-90,90]");
        }
    }
    private static boolean valid(String value, int bound) {
        if (value == null || value.isBlank() || value.length() > 64 || !value.equals(value.trim())) return false;
        try { return new BigDecimal(value).abs().compareTo(BigDecimal.valueOf(bound)) <= 0; }
        catch (NumberFormatException invalid) { return false; }
    }
}
