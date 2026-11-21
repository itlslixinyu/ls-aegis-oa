package com.ls.aegis.biz.sign.chain.task;

import com.ls.aegis.biz.sign.chain.SignChainContext;
import com.ls.aegis.biz.sign.chain.SignTask;
import com.ls.aegis.biz.sign.util.SealImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@Component
public class SealImageProcessTask implements SignTask {

    private static final Logger log = LoggerFactory.getLogger(SealImageProcessTask.class);

    @Override
    public void execute(SignChainContext context) throws Exception {
        Map<Long, BufferedImage> cache = context.getSealImageCache();

        log.info("开始处理印章图片，共{}个", cache.size());

        for (Map.Entry<Long, BufferedImage> entry : cache.entrySet()) {
            Long sealId = entry.getKey();
            BufferedImage original = entry.getValue();

            BufferedImage processed = processSealImage(original);
            cache.put(sealId, processed);

            log.debug("处理印章图片完成: sealId={}, size={}x{}", sealId, processed.getWidth(), processed.getHeight());
        }

        log.info("印章图片处理完成，共{}个", cache.size());
    }

    private BufferedImage processSealImage(BufferedImage source) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(source, "PNG", baos);
        SealImageProcessor processor = new SealImageProcessor();
        byte[] sanitizedBytes = processor.sanitizeImage(baos.toByteArray());
        return ImageIO.read(new ByteArrayInputStream(sanitizedBytes));
    }

    @Override
    public String getTaskName() {
        return "印章图片处理";
    }

    @Override
    public boolean canSkip(SignChainContext context) {
        return context.getSealImageCache() == null || context.getSealImageCache().isEmpty();
    }
}
