package com.ls.aegis.biz.sign.domain.vo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * 灵活的 Boolean 反序列化器
 * 接受 "true"/"false"/"1"/"0"/"on"/"off" 等多种格式
 */
public class FlexibleBooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return parse(p.getValueAsString());
    }

    public static Boolean parse(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = String.valueOf(rawValue);
        if (value == null) return null;

        String lower = value.trim().toLowerCase();
        return switch (lower) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> null;
        };
    }
}
