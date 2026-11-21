package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.biz.sign.domain.SignSeal;
import com.ls.aegis.biz.sign.domain.SignSealExtract;
import com.ls.aegis.biz.sign.mapper.SignSealExtractMapper;
import com.ls.aegis.biz.sign.mapper.SignSealMapper;
import com.ls.aegis.biz.sign.service.ISealExtractService;
import com.ls.aegis.biz.sign.service.ISignStorageService;
import com.ls.aegis.biz.sign.util.SealImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 印章提取服务实现类
 */
@Service
public class SealExtractServiceImpl implements ISealExtractService {

    private static final Logger log = LoggerFactory.getLogger(SealExtractServiceImpl.class);

    /** 图像处理工具类 */
    private final SealImageProcessor imageProcessor = new SealImageProcessor();

    @Autowired
    private ISignStorageService signStorage;

    @Autowired
    private SignSealExtractMapper extractMapper;

    @Autowired
    private SignSealMapper sealMapper;

    @Override
    public Map<String, Object> extractSeal(byte[] imageBytes, Long userId, String userName) throws Exception {
        byte[] extractBytes = imageProcessor.extractSealCircle(imageBytes);

        Map<String, Object> result = new HashMap<>();
        result.put("extractBytes", extractBytes);
        result.put("extractImageBase64", java.util.Base64.getEncoder().encodeToString(extractBytes));

        return result;
    }

    @Override
    public Map<String, Object> extractSealCircle(byte[] imageBytes, Long userId, String userName, String sealCategory) throws Exception {
        byte[] extractBytes = imageProcessor.extractSealCircle(imageBytes, sealCategory);

        Map<String, Object> result = new HashMap<>();
        result.put("extractBytes", extractBytes);
        result.put("extractImageBase64", java.util.Base64.getEncoder().encodeToString(extractBytes));

        int targetSizeMm = SealImageProcessor.getSealSizeByCategory(sealCategory);
        result.put("targetSizeMm", targetSizeMm);

        return result;
    }

    @Override
    public Long saveExtractedSeal(Long extractId, String sealName, Integer width, Integer height,
                                   Long userId, String userName) throws Exception {
        // 获取提取记录
        SignSealExtract extract = extractMapper.selectExtractById(extractId);
        if (extract == null) {
            throw new Exception("提取记录不存在");
        }

        // 生成印章编号（优先使用序列方法，高并发安全）
        String sealCode = null;
        try {
            sealCode = sealMapper.selectNextSealCodeWithSequence();
        } catch (Exception e) {
            sealCode = sealMapper.selectNextSealCode();
        }
        if (sealCode == null || sealCode.isEmpty()) {
            sealCode = "YZ001";
        }

        // 构建印章实体
        SignSeal seal = new SignSeal();
        seal.setSealCode(sealCode);
        seal.setSealName(sealName != null ? sealName : extract.getExtractFileName());
        seal.setSealFilePath(extract.getExtractFilePath());
        seal.setDefaultSizeMm(width != null ? width : 42); // 默认 42mm
        seal.setSealType("0"); // 使用 String 类型
        seal.setCreateBy(userName);
        seal.setDeptId(userId); // 使用 deptId 存储用户ID（临时方案）

        // 插入数据库
        sealMapper.insertSeal(seal);
        return seal.getSealId(); // 返回生成的 ID
    }

    @Override
    public List<Map<String, Object>> listExtractRecords(Map<String, Object> params) {
        Long userId = (Long) params.get("userId");
        if (userId != null) {
            return extractMapper.selectExtractListByUserId(userId);
        }
        return extractMapper.selectExtractListMap();
    }

    @Override
    public String previewExtractedSeal(byte[] imageBytes) throws Exception {
        byte[] extractBytes = imageProcessor.extractSealCircle(imageBytes);

        String objectName = signStorage.upload(extractBytes, "preview-" + System.currentTimeMillis() + ".png", "seal-extract-preview");

        return "/sign/seal/preview/image/" + objectName;
    }

    @Override
    public String previewExtractedSealCircle(byte[] imageBytes, String sealCategory) throws Exception {
        byte[] extractBytes = imageProcessor.extractSealCircle(imageBytes, sealCategory);

        String objectName = signStorage.upload(extractBytes, "preview-" + System.currentTimeMillis() + ".png", "seal-extract-preview");

        return "/sign/seal/preview/image/" + objectName;
    }

    /**
     * 获取图像处理器实例
     * 供 Controller 调用提取算法
     *
     * @return 图像处理器
     */
    public SealImageProcessor getImageProcessor() {
        return imageProcessor;
    }
}
