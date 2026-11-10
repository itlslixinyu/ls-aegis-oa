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

package com.ls.aegis.biz.tenant.service;

import com.ls.aegis.mybatis.base.service.BaseService;
import com.ls.aegis.biz.tenant.model.entity.PackageDO;
import com.ls.aegis.biz.tenant.model.query.PackageQuery;
import com.ls.aegis.biz.tenant.model.req.PackageReq;
import com.ls.aegis.biz.tenant.model.resp.PackageDetailResp;
import com.ls.aegis.biz.tenant.model.resp.PackageResp;
import top.continew.starter.data.service.IService;

/**
 * 套餐业务接口
 *
 * @author 小熊
 * @since 2024/11/26 11:25
 */
public interface PackageService extends BaseService<PackageResp, PackageDetailResp, PackageQuery, PackageReq>, IService<PackageDO> {

    /**
     * 检查套餐状态
     *
     * @param id ID
     */
    void checkStatus(Long id);
}