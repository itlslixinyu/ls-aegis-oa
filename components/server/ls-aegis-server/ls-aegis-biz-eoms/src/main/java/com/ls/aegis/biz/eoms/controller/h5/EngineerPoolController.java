package com.ls.aegis.biz.eoms.controller.h5;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.biz.eoms.domain.h5.EngineerPoolItem;
import com.ls.aegis.biz.eoms.service.h5.EngineerPoolService;
import com.ls.aegis.starter.support.page.PageQueryHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.web.model.R;
import top.continew.starter.log.annotation.Log;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/eoms/engineer/pool")
@RequiredArgsConstructor
public class EngineerPoolController {
    private final EngineerPoolService service;
    @GetMapping
    @SaCheckPermission("eoms:order:query")
    public R<PageResp<EngineerPoolItem>> list(@Valid PageQuery query) {
        Long tenant = service.authorizeEngineer();
        return R.ok(PageQueryHelper.page(query, () -> service.list(tenant)));
    }
    @PostMapping("/{applyId}/claim")
    @SaCheckPermission("eoms:order:update")
    @Log(module = "工程师-工单池领取")
    public R<Map<String, String>> claim(@PathVariable Long applyId) {
        return R.ok(Map.of("orderId", String.valueOf(service.claim(applyId))));
    }
}
