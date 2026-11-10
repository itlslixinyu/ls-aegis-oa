package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.biz.sign.domain.vo.SealVisibleBounds;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class SealVisibleBoundsHelper {

    public SealVisibleBounds detectVisibleBounds(byte[] pngBytes) throws IOException {
        if (pngBytes == null || pngBytes.length == 0) {
            return null;
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return null;
        }

        int left = image.getWidth();
        int top = image.getHeight();
        int right = -1;
        int bottom = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if (isVisibleSealPixel(argb)) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }

        if (right < left || bottom < top) {
            return null;
        }
        return SealVisibleBounds.of(image.getWidth(), image.getHeight(), left, top, right, bottom);
    }

    private boolean isVisibleSealPixel(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;

        if (alpha <= 16) {
            return false;
        }

        return (red > 120 && red > green + 20 && red > blue + 20)
                || (red > 100 && green < 120 && blue < 120);
    }
}
