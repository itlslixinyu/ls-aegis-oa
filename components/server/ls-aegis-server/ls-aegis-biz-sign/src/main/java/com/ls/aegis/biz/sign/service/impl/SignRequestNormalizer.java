package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.biz.sign.domain.vo.SignPosition;
import com.ls.aegis.biz.sign.domain.vo.SignRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
public class SignRequestNormalizer {

    public String normalizeMode(String signMode) {
        if (signMode == null || signMode.isBlank()) {
            return "single";
        }
        return signMode.trim().toLowerCase(Locale.ROOT);
    }

    public List<SignPosition> normalizePositions(SignRequest request, int totalPages) {
        List<SignPosition> positions = request.getPositions();
        if (positions == null || positions.isEmpty()) {
            return Collections.emptyList();
        }

        String requestMode = normalizeMode(request.getSignMode());
        List<SignPosition> normalized = new ArrayList<>();
        for (SignPosition position : positions) {
            SignPosition copy = copyOf(position);
            copy.setSignMode(normalizeMode(copy.getSignMode() != null ? copy.getSignMode() : requestMode));
            normalized.add(copy);
        }

        // fixed 模式：仅将 signMode 为 fixed 的印章展开为多页，其他模式的印章保持原样
        if ("fixed".equals(requestMode)) {
            List<SignPosition> result = new ArrayList<>();
            for (SignPosition pos : normalized) {
                if (!"fixed".equalsIgnoreCase(pos.getSignMode())) {
                    // 非 fixed 模式的印章，直接保留
                    result.add(pos);
                    continue;
                }
                // fixed 模式的印章：展开为多页
                if (totalPages <= 1) {
                    result.add(pos);
                    continue;
                }
                for (int page = 1; page <= totalPages; page++) {
                    SignPosition copy = copyOf(pos);
                    copy.setPage(page);
                    copy.setSignMode("fixed");
                    result.add(copy);
                }
            }
            // 如果没有 fixed 模式的印章，直接返回
            return result;
        }

        // span 模式：仅将 signMode 为 span 的印章均分为 totalPages 个切片，每页显示一个切片
        // 其他模式的印章保持原样（支持混合模式：single/fixed/custom + span）
        if ("span".equals(requestMode)) {
            List<SignPosition> result = new ArrayList<>();
            int spanGroupCounter = 0;
            for (SignPosition pos : normalized) {
                if (!"span".equalsIgnoreCase(pos.getSignMode())) {
                    // 非 span 模式的印章，直接保留
                    result.add(pos);
                    continue;
                }
                // span 模式的印章：展开为多个切片
                String groupId = "span-group-" + spanGroupCounter++;
                if (totalPages <= 1) {
                    pos.setSpanGroupId(groupId);
                    result.add(pos);
                    continue;
                }
                for (int page = 1; page <= totalPages; page++) {
                    SignPosition slice = copyOf(pos);
                    slice.setPage(page);
                    slice.setSpanTotal(totalPages);
                    slice.setSpanIndex(page - 1);
                    slice.setSpanGroupId(groupId);
                    slice.setSignMode("span");
                    result.add(slice);
                }
            }
            return result;
        }

        return normalized;
    }

    public boolean isTimestampOnlyRequest(SignRequest request) {
        if (request.getSealId() != null) {
            return false;
        }
        List<SignPosition> positions = request.getPositions();
        if (positions == null || positions.isEmpty()) {
            return "1".equals(request.getHasTimestamp());
        }
        for (SignPosition position : positions) {
            if (!Boolean.TRUE.equals(position.getIsTimestampOnly())) {
                return false;
            }
        }
        return true;
    }

    private SignPosition copyOf(SignPosition source) {
        SignPosition target = new SignPosition();
        target.setPage(source.getPage());
        target.setX(source.getX());
        target.setY(source.getY());
        target.setSize(source.getSize());
        target.setSignMode(source.getSignMode());
        target.setSealName(source.getSealName());
        target.setSealFilePath(source.getSealFilePath());
        target.setSpanTotal(source.getSpanTotal());
        target.setSpanIndex(source.getSpanIndex());
        target.setSpanGroupId(source.getSpanGroupId());
        target.setHasTimestamp(source.getHasTimestamp());
        target.setTimestamp(source.getTimestamp());
        target.setIsTimestampOnly(source.getIsTimestampOnly());
        target.setSpanPosition(source.getSpanPosition());
        return target;
    }
}
