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

package com.ls.aegis.biz.notice.service.impl;

import cn.crane4j.annotation.AutoOperate;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.exception.NotPermissionException;
import com.ls.aegis.common.context.UserContextHolder;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.biz.notice.enums.MessageTypeEnum;
import com.ls.aegis.biz.notice.enums.NoticeScopeEnum;
import com.ls.aegis.biz.notice.mapper.MessageMapper;
import com.ls.aegis.biz.notice.model.entity.MessageDO;
import com.ls.aegis.biz.notice.model.query.MessageQuery;
import com.ls.aegis.biz.notice.model.req.MessageReq;
import com.ls.aegis.biz.notice.model.resp.message.MessageDetailResp;
import com.ls.aegis.biz.notice.model.resp.message.MessageResp;
import com.ls.aegis.biz.notice.model.resp.message.MessageTypeUnreadResp;
import com.ls.aegis.biz.notice.model.resp.message.MessageTypeCount;
import com.ls.aegis.biz.notice.model.resp.message.MessageUnreadResp;
import com.ls.aegis.biz.notice.service.MessageLogService;
import com.ls.aegis.biz.notice.service.MessageService;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.messaging.websocket.util.WebSocketUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息业务实现
 *
 * @author Bull-BCLS
 * @author Charles7c
 * @since 2023/10/15 19:05
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper baseMapper;
    private final MessageLogService messageLogService;

    @Override
    @AutoOperate(type = MessageResp.class, on = "list")
    public PageResp<MessageResp> page(MessageQuery query, PageQuery pageQuery) {
        query.setUserId(currentUserId());
        IPage<MessageResp> page = baseMapper.selectMessagePage(new Page<>(pageQuery.getPage(), pageQuery
            .getSize()), query);
        return PageResp.build(page);
    }

    @Override
    public MessageDetailResp get(Long id) {
        return baseMapper.selectMessageById(id, currentUserId());
    }

    @Override
    public void readMessage(List<Long> ids, Long userId) {
        // 仅投影需要写入的ID，指定消息时不加载整个收件箱；计数交给数据库。
        List<Long> unreadIds = baseMapper.selectUnreadIdsByUserId(userId, ids);
        messageLogService.addWithUserId(unreadIds, userId);
        WebSocketUtils.sendMessage(StpUtil.getTokenValueByLoginId(userId),
            String.valueOf(baseMapper.selectUnreadCountByUserIdAndType(userId, null)));
    }

    @Override
    public MessageUnreadResp countUnreadByUserId(Long userId, Boolean isDetail) {
        MessageUnreadResp result = new MessageUnreadResp();
        if (Boolean.TRUE.equals(isDetail)) {
            var counts = new java.util.HashMap<Integer, Long>();
            for (MessageTypeCount row : baseMapper.selectUnreadCountsByType(userId)) {
                if (row.getType() != null) counts.put(row.getType(), row.getCount());
            }
            List<MessageTypeUnreadResp> details = new ArrayList<>();
            long total = 0;
            for (MessageTypeEnum type : MessageTypeEnum.values()) {
                MessageTypeUnreadResp row = new MessageTypeUnreadResp();
                row.setType(type);
                row.setCount(counts.getOrDefault(type.getValue(), 0L));
                details.add(row);
                total += row.getCount();
            }
            result.setDetails(details);
            result.setTotal(total);
        } else {
            result.setTotal(baseMapper.selectUnreadCountByUserIdAndType(userId, null));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(MessageReq req, List<String> userIdList) {
        MessageDO message = BeanUtil.copyProperties(req, MessageDO.class);
        message.setScope(CollUtil.isEmpty(userIdList) ? NoticeScopeEnum.ALL : NoticeScopeEnum.USER);
        message.setUsers(userIdList);
        baseMapper.insert(message);
        // 发送消息给指定在线用户
        if (CollUtil.isNotEmpty(userIdList)) {
            userIdList.parallelStream().forEach(userId -> {
                List<String> tokenList = StpUtil.getTokenValueListByLoginId(userId);
                tokenList.parallelStream().forEach(token -> WebSocketUtils.sendMessage(token, "1"));
            });
            return;
        }
        // 发送消息给所有在线用户
        WebSocketUtils.sendMessage("1");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        Long userId = currentUserId();
        if (CollUtil.isEmpty(ids)) return;
        if (ids.stream().anyMatch(java.util.Objects::isNull)) {
            throw new NotPermissionException("user:message:delete");
        }
        // 固定批量写入锁顺序，减少同用户交错批次的数据库死锁。
        List<Long> distinctIds = ids.stream().distinct().sorted().toList();
        List<Long> accessibleIds = baseMapper.selectAccessibleIds(distinctIds, userId);
        // 整批校验先于任何写入；个人墓碑不参与可见性校验，以支持重复删除幂等。
        if (!new java.util.HashSet<>(accessibleIds).equals(new java.util.HashSet<>(distinctIds))) {
            throw new NotPermissionException("user:message:delete");
        }
        messageLogService.markDeletedForUser(distinctIds, userId);
    }

    private Long currentUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) throw new NotPermissionException("user:message:access");
        return userId;
    }
}
