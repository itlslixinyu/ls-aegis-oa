package com.ls.aegis.biz.eoms.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eoms.wechat")
public class WechatNoticeProperties {
    private String appId = "";
    private String appSecret = "";
    private String miniprogramState = "formal";
    private Template customer = new Template();
    private Template engineer = new Template();

    @Data
    public static class Template {
        private String id = "";
        private String numberField = "";
        private String statusField = "";
        private String timeField = "";
        private String titleField = "";

        public boolean configured() {
            return !id.isBlank() && numberField.matches("(character_string|thing)[0-9]+")
                && statusField.matches("(thing|phrase)[0-9]+") && !numberField.equals(statusField)
                && (timeField.isBlank() || timeField.matches("(time|date)[0-9]+"))
                && (titleField.isBlank() || (titleField.matches("thing[0-9]+")
                    && !titleField.equals(numberField) && !titleField.equals(statusField)));
        }
    }
}
