package com.ls.aegis.biz.eoms.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JpegOrientationTest {
    static byte[] exif(int orientation, boolean little) {
        ByteBuffer tiff = ByteBuffer.allocate(26).order(little ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        tiff.put((byte) (little ? 'I' : 'M')).put((byte) (little ? 'I' : 'M')).putShort((short) 42).putInt(8);
        tiff.putShort((short) 1).putShort((short) 274).putShort((short) 3).putInt(1).putShort((short) orientation).putShort((short) 0).putInt(0);
        ByteBuffer jpeg = ByteBuffer.allocate(38).order(ByteOrder.BIG_ENDIAN);
        jpeg.putShort((short) 0xffd8).putShort((short) 0xffe1).putShort((short) 34).put(new byte[]{'E','x','i','f',0,0}).put(tiff.array());
        return jpeg.array();
    }

    @ParameterizedTest @ValueSource(ints={1,2,3,4,5,6,7,8})
    void allRotationsAndMirrorsPreservePixelPlacementAndParseBothByteOrders(int orientation) {
        for (boolean little : new boolean[]{false,true}) assertEquals(orientation, JpegOrientation.read(exif(orientation,little)));
        BufferedImage source = new BufferedImage(3,2,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<2;y++) for(int x=0;x<3;x++) source.setRGB(x,y,0xff000000 | (y*3+x+1));
        BufferedImage actual = JpegOrientation.apply(source,orientation);
        int[][] expected = switch(orientation) {
            case 2 -> new int[][]{{3,2,1},{6,5,4}};
            case 3 -> new int[][]{{6,5,4},{3,2,1}};
            case 4 -> new int[][]{{4,5,6},{1,2,3}};
            case 5 -> new int[][]{{1,4},{2,5},{3,6}};
            case 6 -> new int[][]{{4,1},{5,2},{6,3}};
            case 7 -> new int[][]{{6,3},{5,2},{4,1}};
            case 8 -> new int[][]{{3,6},{2,5},{1,4}};
            default -> new int[][]{{1,2,3},{4,5,6}};
        };
        assertEquals(expected.length,actual.getHeight());assertEquals(expected[0].length,actual.getWidth());
        for(int y=0;y<actual.getHeight();y++) for(int x=0;x<actual.getWidth();x++) assertEquals(expected[y][x],actual.getRGB(x,y)&0xffffff);
    }

    @Test void malformedMetadataNeverReadsOutsideItsOwnSegment() {
        byte[] good=exif(6,true);
        for(int length=0;length<good.length;length++) assertEquals(1,JpegOrientation.read(Arrays.copyOf(good,length)));
        byte[] bad=good.clone();Arrays.fill(bad,16,20,(byte)255);assertEquals(1,JpegOrientation.read(bad));
        assertEquals(1,JpegOrientation.read(exif(9,false)));assertEquals(1,JpegOrientation.read(new byte[]{1,2,3}));
    }

    @Test void watermarkOutputHonorsCameraOrientationBeforeDiscardingExif() throws Exception {
        BufferedImage source=new BufferedImage(480,320,BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream jpeg=new ByteArrayOutputStream();ImageIO.write(source,"jpg",jpeg);
        ByteArrayOutputStream rotated=new ByteArrayOutputStream();rotated.write(exif(6,true));rotated.write(jpeg.toByteArray(),2,jpeg.size()-2);
        // Reproduces why ImageIO dimensions alone cannot establish the displayed orientation.
        assertEquals(480,ImageIO.read(new ByteArrayInputStream(rotated.toByteArray())).getWidth());
        BufferedImage output=ImageIO.read(new ByteArrayInputStream(WatermarkUtils.addWatermark(rotated.toByteArray(),new WatermarkInfo())));
        assertEquals(320,output.getWidth());assertEquals(480,output.getHeight());
    }
}
