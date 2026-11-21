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

package com.ls.aegis.biz.notice.service;

import com.ls.aegis.mybatis.base.service.BaseService;
import com.ls.aegis.biz.notice.enums.NoticeMethodEnum;
import com.ls.aegis.biz.notice.model.entity.NoticeDO;
import com.ls.aegis.biz.notice.model.query.NoticeQuery;
import com.ls.aegis.biz.notice.model.req.NoticeReq;
import com.ls.aegis.biz.notice.model.resp.dashboard.DashboardNoticeResp;
import com.ls.aegis.biz.notice.model.resp.notice.NoticeDetailResp;
import com.ls.aegis.biz.notice.model.resp.notice.NoticeResp;
import top.continew.starter.data.service.IService;

import java.util.List;

/**
 * 公告业务接口
 *
 * @author Charles7c
 * @since 2023/8/20 10:55
 */
public interface NoticeService extends BaseService<NoticeResp, NoticeDetailResp, NoticeQuery, NoticeReq>, IService<NoticeDO> {

    /**
     * 发布公告
     *
     * @param notice 公告信息
     */
    void publish(NoticeDO notice);

    /** 当前接收者可见且已发布的未读公告数量。 */
    Long countUnreadByUserId(Long userId);

    /**
     * 查询未读公告 ID 列表
     *
     * @param method 通知方式
     * @param userId 用户 ID
     * @return 未读公告 ID 响应参数
     */
    List<Long> listUnreadIdsByUserId(NoticeMethodEnum method, Long userId);

    /**
     * 阅读公告
     *
     * @param id     公告 ID
     * @param userId 用户 ID
     */
    void readNotice(Long id, Long userId);

    /**
     * 查询仪表盘公告列表
     *
     * @return 仪表盘公告列表
     */
    List<DashboardNoticeResp> listDashboard();
}
