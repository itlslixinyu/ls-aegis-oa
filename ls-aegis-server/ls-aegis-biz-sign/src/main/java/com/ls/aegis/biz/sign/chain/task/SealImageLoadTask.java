package com.ls.aegis.biz.sign.chain.task;

import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.biz.sign.chain.SignChainContext;
import com.ls.aegis.biz.sign.chain.SignTask;
import com.ls.aegis.biz.sign.domain.SignSeal;
import com.ls.aegis.biz.sign.domain.vo.GeneralSealConfig;
import com.ls.aegis.biz.sign.domain.vo.SpanSealConfig;
import com.ls.aegis.biz.sign.mapper.SignSealMapper;
import com.ls.aegis.biz.sign.service.ISignStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class SealImageLoadTask implements SignTask {

    private static final Logger log = LoggerFactory.getLogger(SealImageLoadTask.class);

    @Autowired
    private ISignStorageService signStorage;

    @Autowired
    private SignSealMapper sealMapper;

    @Override
    public void execute(SignChainContext context) throws Exception {
        Map<Long, BufferedImage> cache = context.getSealImageCache();
        if (cache == null) {
            cache = new java.util.HashMap<>();
            context.setSealImageCache(cache);
        }

        Set<Long> sealIds = new HashSet<>();
        if (context.getGeneralSeals() != null) {
            for (GeneralSealConfig seal : context.getGeneralSeals()) {
                sealIds.add(seal.getSealId());
            }
        }
        if (context.getSpanSeals() != null) {
            for (SpanSealConfig span : context.getSpanSeals()) {
                sealIds.add(span.getSealId());
            }
        }

        log.info("加载印章图片，共{}个印章", sealIds.size());

        for (Long sealId : sealIds) {
            SignSeal seal = sealMapper.selectSealById(sealId);
            if (seal == null) {
                throw new ServiceException("印章不存在: " + sealId);
            }
            if (seal.getSealFilePath() == null || seal.getSealFilePath().isEmpty()) {
                throw new ServiceException("印章文件路径为空: sealId=" + sealId);
            }

            byte[] imgBytes = signStorage.download(seal.getSealFilePath());
            if (imgBytes == null || imgBytes.length == 0) {
                throw new ServiceException("印章图片下载失败: " + sealId);
            }
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
            if (img == null) {
                throw new ServiceException("印章图片加载失败: " + sealId);
            }

            cache.put(sealId, img);
            log.debug("加载印章图片成功: sealId={}, size={}x{}", sealId, img.getWidth(), img.getHeight());
        }

        log.info("印章图片加载完成，共{}个", cache.size());
    }

    @Override
    public String getTaskName() {
        return "印章图片加载";
    }

    @Override
    public boolean canSkip(SignChainContext context) {
        return (context.getGeneralSeals() == null || context.getGeneralSeals().isEmpty())
            && (context.getSpanSeals() == null || context.getSpanSeals().isEmpty());
    }
}
