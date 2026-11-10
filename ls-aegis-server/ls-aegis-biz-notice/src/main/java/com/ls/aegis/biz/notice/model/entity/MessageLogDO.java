/*
 * Copyright (c) 2022-present Charles7c Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ls.aegis.biz.notice.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息日志实体
 *
 * @author Charles7c
 * @author Bull-BCLS
 * @since 2023/10/15 20:25
 */
@Data
@NoArgsConstructor
@TableName("sys_message_log")
public class MessageLogDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息 ID（联合主键之一；IdType.INPUT 表示业务写入，避免全局 ASSIGN_ID 覆盖）
     */
    @TableId(type = IdType.INPUT)
    private Long messageId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 读取时间
     */
    private LocalDateTime readTime;

    /** 个人收件箱删除墓碑，不改变其他接收者的可见性。 */
    private Boolean isDeleted;

    public MessageLogDO(Long messageId, Long userId, LocalDateTime readTime) {
        this.messageId = messageId;
        this.userId = userId;
        this.readTime = readTime;
    }
}
