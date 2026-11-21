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

package com.ls.aegis.rbac.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.ls.aegis.crypto.api.IGmCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.ls.aegis.rbac.auth.model.query.OnlineUserQuery;
import com.ls.aegis.rbac.auth.service.OnlineUserService;
import com.ls.aegis.mybatis.base.service.BaseServiceImpl;
import com.ls.aegis.rbac.mapper.ClientMapper;
import com.ls.aegis.rbac.model.entity.ClientDO;
import com.ls.aegis.rbac.model.query.ClientQuery;
import com.ls.aegis.rbac.model.req.ClientReq;
import com.ls.aegis.rbac.model.resp.ClientResp;
import com.ls.aegis.rbac.service.ClientService;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.validation.CheckUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 客户端业务实现
 *
 * @author KAI
 * @author Charles7c
 * @since 2024/12/03 16:04
 */
@Service
@RequiredArgsConstructor
public class ClientServiceImpl extends BaseServiceImpl<ClientMapper, ClientDO, ClientResp, ClientResp, ClientQuery, ClientReq> implements ClientService {

    private final OnlineUserService onlineUserService;
    private final ObjectProvider<IGmCrypto> gmCryptoProvider;

    @Override
    public void beforeCreate(ClientReq req) {
        String raw = Base64.encode(IdUtil.fastSimpleUUID())
            .replace(StringConstants.SLASH, StringConstants.EMPTY)
            .replace(StringConstants.PLUS, StringConstants.EMPTY);
        // client_id 字段 varchar(50)：SM3 取前 32 位十六进制，与历史 MD5 长度兼容
        IGmCrypto gmCrypto = gmCryptoProvider.getIfAvailable();
        String clientId;
        if (gmCrypto != null && gmCrypto.isEnabled()) {
            clientId = gmCrypto.sm3Hex(raw.getBytes(StandardCharsets.UTF_8)).substring(0, 32);
        } else {
            clientId = SecureUtil.md5(raw);
        }
        req.setClientId(clientId);
    }

    @Override
    public void beforeDelete(List<Long> ids) {
        // 如果还存在在线用户，则不能删除
        OnlineUserQuery query = new OnlineUserQuery();
        for (Long id : ids) {
            ClientDO client = this.getById(id);
            query.setClientId(client.getClientId());
            CheckUtils.throwIfNotEmpty(onlineUserService.list(query), "客户端 [{}] 还存在在线用户，不允许删除", client.getClientId());
        }
    }

    @Override
    public ClientResp getByClientId(String clientId) {
        return baseMapper.lambdaQuery()
            .eq(ClientDO::getClientId, clientId)
            .oneOpt()
            .map(client -> BeanUtil.copyProperties(client, ClientResp.class))
            .orElse(null);
    }
}