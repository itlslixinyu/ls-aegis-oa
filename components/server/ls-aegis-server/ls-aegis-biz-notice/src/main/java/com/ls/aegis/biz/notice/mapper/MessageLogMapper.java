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

package com.ls.aegis.biz.notice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.time.LocalDateTime;
import com.ls.aegis.biz.notice.model.entity.MessageLogDO;
import top.continew.starter.data.mapper.BaseMapper;

/**
 * 消息日志 Mapper
 *
 * @author Bull-BCLS
 * @author Charles7c
 * @since 2023/10/15 20:25
 */
@Mapper
public interface MessageLogMapper extends BaseMapper<MessageLogDO> {
    int markDeleted(@Param("ids") List<Long> ids, @Param("userId") Long userId);
    int markRead(@Param("ids") List<Long> ids, @Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);
}
