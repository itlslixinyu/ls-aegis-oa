package com.ls.aegis.boot.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

/**
 * 全局 Jackson：时间格式 + 雪花 Long 以字符串下发（避免前端 JS Number 丢精度）。
 * <p>使用 {@code serializerByType}/{@code deserializerByType} 覆盖 JavaTimeModule 默认 ISO，
 * 避免 ContiNew / Spring Boot 已注册模块被整表替换。勿在业务模块单独声明 {@code ObjectMapper} Bean。</p>
 */
@Configuration
public class JacksonConfig {

    /** 全站统一日期时间格式（前后端约定） */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern(TIME_PATTERN);
    private static final ZoneId ZONE_CN = ZoneId.of("Asia/Shanghai");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer javaTimeModuleCustomizer() {
        return builder -> {
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.simpleDateFormat(DATETIME_PATTERN);
            builder.timeZone(TimeZone.getTimeZone(ZONE_CN));

            // 雪花 ID（Long）一律序列化为字符串，前端禁止 Number() 再转
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);

            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATETIME_FMT));
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(DATE_FMT));
            builder.serializerByType(LocalTime.class, new LocalTimeSerializer(TIME_FMT));

            builder.deserializerByType(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(DATE_FMT));
            builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(TIME_FMT));
        };
    }

    /**
     * 优先解析 {@code yyyy-MM-dd HH:mm:ss}，兼容 ISO（含 T）、仅日期、带时区字符串。
     */
    static final class FlexibleLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

        FlexibleLocalDateTimeDeserializer() {
            super(LocalDateTime.class);
        }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getValueAsString();
            if (!StringUtils.hasText(text)) {
                return null;
            }
            text = text.trim();
            try {
                return LocalDateTime.parse(text, DATETIME_FMT);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
            try {
                return OffsetDateTime.parse(text).atZoneSameInstant(ZONE_CN).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                // fall through
            }
            try {
                return LocalDate.parse(text, DATE_FMT).atStartOfDay();
            } catch (DateTimeParseException ex) {
                return (LocalDateTime) ctxt.handleWeirdStringValue(LocalDateTime.class, text,
                    "期望格式 " + DATETIME_PATTERN + "（兼容 ISO / yyyy-MM-dd）");
            }
        }
    }
}
