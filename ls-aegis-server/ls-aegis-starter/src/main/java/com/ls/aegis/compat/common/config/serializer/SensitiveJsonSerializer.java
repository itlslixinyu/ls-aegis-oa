package com.ls.aegis.compat.common.config.serializer;

import java.io.IOException;
import java.util.Objects;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.ls.aegis.compat.common.annotation.Sensitive;
import com.ls.aegis.compat.common.enums.DesensitizedType;
import com.ls.aegis.common.context.UserContextHolder;

/**
 * 数据脱敏序列化过滤（Jackson 2 / com.fasterxml）
 *
 * @author ItLs
 */
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer
{
    private DesensitizedType desensitizedType;

    public SensitiveJsonSerializer()
    {
    }

    public SensitiveJsonSerializer(DesensitizedType desensitizedType)
    {
        this.desensitizedType = desensitizedType;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        if (desensitizedType != null && desensitization())
        {
            gen.writeString(desensitizedType.desensitizer().apply(value));
        }
        else
        {
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException
    {
        if (property != null)
        {
            Sensitive annotation = property.getAnnotation(Sensitive.class);
            if (annotation == null)
            {
                annotation = property.getContextAnnotation(Sensitive.class);
            }
            if (Objects.nonNull(annotation) && Objects.equals(String.class, property.getType().getRawClass()))
            {
                return new SensitiveJsonSerializer(annotation.desensitizedType());
            }
            return prov.findValueSerializer(property.getType(), property);
        }
        return prov.findNullValueSerializer(null);
    }

    /**
     * 是否需要脱敏处理（超级管理员不脱敏）
     */
    private boolean desensitization()
    {
        try
        {
            return !UserContextHolder.isSuperAdmin();
        }
        catch (Exception e)
        {
            return true;
        }
    }
}
