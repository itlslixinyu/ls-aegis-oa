package com.ls.aegis.biz.sign.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ls.aegis.biz.sign.domain.SignRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SignSnapshotFingerprintHelper {

    private static final Logger log = LoggerFactory.getLogger(SignSnapshotFingerprintHelper.class);

    private final ObjectMapper objectMapper;

    public SignSnapshotFingerprintHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildPageFingerprint(Long sealId, String signMode, String positionsJson) {
        String normalizedPositions = normalizePositionsForFingerprint(positionsJson);
        if (normalizedPositions == null) {
            return null;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sealId", sealId);
        payload.put("signMode", normalize(signMode));
        payload.put("positions", normalizedPositions);
        return sha256(toStableJson(payload));
    }

    public String buildSignHash(SignRecord record) {
        if (record == null) {
            return null;
        }

        String normalizedPositions = normalizePositionsForFingerprint(record.getPositions());
        if (normalizedPositions == null) {
            return null;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recordId", normalize(record.getRecordId()));
        payload.put("projectId", record.getProjectId());
        payload.put("sealId", record.getSealId());
        payload.put("fileId", record.getFileId());
        payload.put("signedFileId", record.getSignedFileId());
        payload.put("certId", record.getCertId());
        payload.put("serialNo", normalize(record.getSerialNo()));
        payload.put("fingerprintSha256", normalize(record.getFingerprintSha256()));
        payload.put("signMode", normalize(record.getSignMode()));
        payload.put("hasTimestamp", normalize(record.getHasTimestamp()));
        payload.put("positions", normalizedPositions);
        payload.put("signTime", record.getSignTime() != null ? record.getSignTime().getTime() : "");
        return sha256(toStableJson(payload));
    }

    public String normalizePositionsForFingerprint(String positionsJson) {
        if (positionsJson == null || positionsJson.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(positionsJson);
            if (!root.isArray() || root.isEmpty()) {
                return null;
            }

            List<Map<String, Object>> normalized = new ArrayList<>();
            for (JsonNode node : root) {
                Map<String, Object> position = new LinkedHashMap<>();
                position.put("page", readInteger(node, "page"));
                position.put("x", readDecimal(node, "x"));
                position.put("y", readDecimal(node, "y"));
                position.put("width", readDecimal(node, "width", "size"));
                position.put("height", readDecimal(node, "height", "size"));
                normalized.add(position);
            }

            normalized.sort(Comparator
                    .comparing((Map<String, Object> item) -> (Integer) item.get("page"), Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(item -> (BigDecimal) item.get("y"), Comparator.nullsLast(BigDecimal::compareTo))
                    .thenComparing(item -> (BigDecimal) item.get("x"), Comparator.nullsLast(BigDecimal::compareTo))
                    .thenComparing(item -> (BigDecimal) item.get("width"), Comparator.nullsLast(BigDecimal::compareTo))
                    .thenComparing(item -> (BigDecimal) item.get("height"), Comparator.nullsLast(BigDecimal::compareTo)));

            return toStableJson(normalized);
        } catch (Exception e) {
            log.warn("Failed to normalize sign positions for fingerprint", e);
            return null;
        }
    }

    private Integer readInteger(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.isNumber() ? value.intValue() : Integer.valueOf(value.asText());
    }

    private BigDecimal readDecimal(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value == null || value.isNull() || value.asText().isBlank()) {
                continue;
            }
            BigDecimal decimal = value.isNumber() ? value.decimalValue() : new BigDecimal(value.asText());
            return decimal.setScale(4, RoundingMode.HALF_UP);
        }
        return null;
    }

    private String toStableJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize sign snapshot payload", e);
        }
    }

    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
