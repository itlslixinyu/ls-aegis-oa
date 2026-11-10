package com.ls.aegis.biz.notice.model.resp.message;

import lombok.Data;

/** 内部计数投影；保留原始类型值，不让历史未知类型影响已知分类。 */
@Data
public class MessageTypeCount {
    private Integer type;
    private Long count;
}
