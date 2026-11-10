package com.ls.aegis.biz.sign.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Iterator;

/**
 * 数组转逗号分隔字符串的反序列化器
 * 用于兼容前端传数组、后端存字符串的场景
 */
public class ArrayToStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) {
                JsonNode item = it.next();
                if (sb.length() > 0) {
                    sb.append(",");
                }
                if (item.isTextual()) {
                    sb.append(item.asText());
                } else if (item.isNumber()) {
                    sb.append(item.asLong());
                } else {
                    sb.append(item.toString());
                }
            }
            return sb.toString();
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isNull()) {
            return null;
        } else {
            return node.toString();
        }
    }
}
