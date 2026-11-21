package com.ls.aegis.biz.eoms.utils;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/** Reads only the bounded JPEG APP1/IFD0 orientation; no GPS or other metadata is retained. */
final class JpegOrientation {
    private JpegOrientation() { }

    static int read(byte[] data) {
        if (data == null || data.length < 4 || (data[0] & 255) != 255 || (data[1] & 255) != 216) return 1;
        int at = 2;
        while (at < data.length) {
            if ((data[at++] & 255) != 255) return 1;
            while (at < data.length && (data[at] & 255) == 255) at++;
            if (at >= data.length) return 1;
            int marker = data[at++] & 255;
            if (marker == 218 || marker == 217) break;
            if (marker == 1 || (marker >= 208 && marker <= 216)) continue;
            if (at + 2 > data.length) return 1;
            int length = (int) number(data, at, 2, false);
            if (length < 2 || length > data.length - at) return 1;
            int payload = at + 2, end = at + length;
            if (marker == 225 && end - payload >= 14 && data[payload] == 'E' && data[payload + 1] == 'x'
                    && data[payload + 2] == 'i' && data[payload + 3] == 'f' && data[payload + 4] == 0 && data[payload + 5] == 0) {
                return readTiff(data, payload + 6, end);
            }
            at = end;
        }
        return 1;
    }

    private static int readTiff(byte[] data, int base, int end) {
        boolean little = data[base] == 'I' && data[base + 1] == 'I';
        if (!little && !(data[base] == 'M' && data[base + 1] == 'M')) return 1;
        if (number(data, base + 2, 2, little) != 42) return 1;
        long offset = number(data, base + 4, 4, little);
        if (offset < 8 || offset > end - base - 2) return 1;
        int ifd = base + (int) offset;
        int count = (int) number(data, ifd, 2, little);
        if (count > (end - ifd - 2) / 12) return 1;
        for (int i = 0, entry = ifd + 2; i < count; i++, entry += 12) {
            if (number(data, entry, 2, little) == 274 && number(data, entry + 2, 2, little) == 3
                    && number(data, entry + 4, 4, little) == 1) {
                int value = (int) number(data, entry + 8, 2, little);
                return value >= 1 && value <= 8 ? value : 1;
            }
        }
        return 1;
    }

    private static long number(byte[] data, int at, int size, boolean little) {
        long value = 0;
        for (int i = 0; i < size; i++) value = value << 8 | data[at + (little ? size - i - 1 : i)] & 255L;
        return value;
    }

    static BufferedImage apply(BufferedImage source, int orientation) {
        int w = source.getWidth(), h = source.getHeight();
        AffineTransform transform = switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
            default -> null;
        };
        if (transform == null) return source;
        BufferedImage output = new BufferedImage(orientation >= 5 ? h : w, orientation >= 5 ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try { graphics.drawImage(source, transform, null); } finally { graphics.dispose(); }
        return output;
    }
}
