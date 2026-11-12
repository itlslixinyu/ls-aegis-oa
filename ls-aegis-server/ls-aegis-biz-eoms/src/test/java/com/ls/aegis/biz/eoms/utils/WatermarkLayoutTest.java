package com.ls.aegis.biz.eoms.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatermarkLayoutTest {
    private WatermarkInfo fullInfo(String title) {
        var info = new WatermarkInfo();
        info.setTitle(title);
        info.setCustomerName("示例客户公司");
        info.setTaskName("设备现场检修");
        info.setServiceAddress("示例市示例区服务中心三号楼二层设备管理办公室，东侧入口");
        info.setProjectName("设备维护与现场服务项目 · 示例资料");
        info.setExecutor("示例工程师");
        info.setWorkTime("2026-11-11 20:30");
        info.setWorkTimeLabel("报修资料".equals(title) ? "报修时间" : "完成资料".equals(title) ? "维修完毕时间" : "维修时间");
        info.setRemark("检查设备连接与运行状态，记录现场处理情况。");
        return info;
    }

    @ParameterizedTest
    @CsvSource({"1280,720", "720,1280", "640,480", "320,240", "1,1"})
    void measuredCardAndWrappedRowsStayWithinImage(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            var card = WatermarkUtils.cardLayout(graphics, width, height, WatermarkUtils.watermarkFields(fullInfo("维修资料")));
            assertTrue(card.x() >= 0 && card.y() >= 0);
            assertTrue(card.x() + card.width() <= width && card.y() + card.height() <= height);
            if (width >= 320) {
                int rows = card.lines().stream().mapToInt(line -> line.values().size()).sum();
                assertEquals(card.headerHeight() + card.padding() * 2 + rows * card.lineHeight(), card.height(), "wrapped text must fit measured card height");
                assertTrue(card.x() < (width - card.width()) / 2, "card should align towards the left");
                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, card.fontSize()));
                assertTrue(card.lines().stream().allMatch(line -> line.values().size() <= 2));
            }
        } finally { graphics.dispose(); }
    }

    @Test void wrappingRetainsTheStartOfLastLineAndDoesNotSplitSurrogates() {
        var graphics = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB).createGraphics();
        try {
            graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
            var fm = graphics.getFontMetrics();
            var rows = WatermarkUtils.wrapString(fm, "abcdefghijklmnopqrstuvwxyz", fm.stringWidth("abcdef"), 2);
            assertEquals("abcdef", rows.get(0));
            assertTrue(rows.get(1).startsWith("gh"), "second line must keep its beginning rather than skip to trailing text");
            var unicode = WatermarkUtils.wrapString(fm, "AB😀CD😀EF😀GH", fm.stringWidth("AB😀CD"), 2);
            for (String line : unicode) {
                assertTrue(fm.stringWidth(line) <= fm.stringWidth("AB😀CD"));
                for (int i = 0; i < line.length(); i++) {
                    if (Character.isHighSurrogate(line.charAt(i))) assertTrue(++i < line.length() && Character.isLowSurrogate(line.charAt(i)));
                    else assertFalse(Character.isLowSurrogate(line.charAt(i)));
                }
            }
        } finally { graphics.dispose(); }
    }

    @Test void emptyFieldsAreOmittedAndCoordinatesAreNotCalledAnAddress() {
        var info = new WatermarkInfo();
        info.setRemark("  ");
        info.setTaskName("\t");
        info.setServiceAddress("39.123456,116.123456");
        var fields = WatermarkUtils.watermarkFields(info);
        assertEquals(2, fields.size());
        assertEquals("定位坐标", fields.get(0)[0]);
        assertEquals("施工时间", fields.get(1)[0]);
    }

    @Test void maintenanceCardShowsCustomerAndEngineerIndependently() {
        var info = fullInfo("维修资料");
        info.setExecutorLabel("维修人员");
        var fields = WatermarkUtils.watermarkFields(info);
        assertTrue(fields.stream().anyMatch(f -> "报修客户".equals(f[0]) && "示例客户公司".equals(f[1])));
        assertTrue(fields.stream().anyMatch(f -> "维修人员".equals(f[0]) && "示例工程师".equals(f[1])));
        assertTrue(fields.stream().anyMatch(f -> "任务名称".equals(f[0]) && "设备现场检修".equals(f[1])));
    }

    @Test void customerCompanyIsNotDisplayedAsTaskName() {
        var info = new WatermarkInfo();
        info.setCustomerName("示例客户公司");
        var fields = WatermarkUtils.watermarkFields(info);
        assertTrue(fields.stream().anyMatch(field -> "报修客户".equals(field[0]) && "示例客户公司".equals(field[1])));
        assertFalse(fields.stream().anyMatch(field -> "任务名称".equals(field[0])));
        info.setTaskName("设备故障排查");
        assertTrue(WatermarkUtils.watermarkFields(info).stream().anyMatch(field -> "任务名称".equals(field[0]) && "设备故障排查".equals(field[1])));
    }

    @Test void reportAndServiceLocationsAndPeopleHaveSeparateLabels() {
        var report = new WatermarkInfo();
        report.setAddressLabel("报修地址"); report.setExecutorLabel("报修人");
        report.setServiceAddress("客户报修现场"); report.setExecutor("示例联系人"); report.setRemark("客户说明");
        var fields = WatermarkUtils.watermarkFields(report);
        assertTrue(fields.stream().anyMatch(f -> "报修地址".equals(f[0]) && "客户报修现场".equals(f[1])));
        assertTrue(fields.stream().anyMatch(f -> "报修人".equals(f[0]) && "示例联系人".equals(f[1])));
        assertTrue(fields.stream().anyMatch(f -> "备注".equals(f[0]) && "客户说明".equals(f[1])));
        report.setServiceAddress("39.1,116.1");
        assertTrue(WatermarkUtils.watermarkFields(report).stream().anyMatch(f -> "报修坐标".equals(f[0])));
        var service = new WatermarkInfo();
        service.setAddressLabel("服务地址"); service.setExecutorLabel("维修人员");
        service.setServiceAddress("工程师服务现场"); service.setExecutor("示例工程师");
        assertTrue(WatermarkUtils.watermarkFields(service).stream().anyMatch(f -> "服务地址".equals(f[0]) && "工程师服务现场".equals(f[1])));
        assertTrue(WatermarkUtils.watermarkFields(service).stream().anyMatch(f -> "维修人员".equals(f[0]) && "示例工程师".equals(f[1])));
        assertFalse(WatermarkUtils.watermarkFields(service).stream().anyMatch(f -> "备注".equals(f[0])));
    }

    @Test void stageCardsRenderAsJpegAndKeepPhotoDimensions() throws Exception {
        var source = new BufferedImage(900, 1200, BufferedImage.TYPE_INT_RGB);
        var g = source.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(202, 216, 226), 900, 1200, new Color(83, 111, 133)));
        g.fillRect(0, 0, 900, 1200);
        g.setColor(new Color(144, 162, 175)); g.fillRect(120, 210, 660, 600);
        g.setColor(new Color(59, 83, 105)); g.fillRect(180, 280, 540, 460);
        g.dispose();
        var original = new ByteArrayOutputStream(); ImageIO.write(source, "png", original);
        String previewDir = System.getProperty("watermark.preview.dir");
        for (String title : new String[]{"报修资料", "维修资料", "完成资料"}) {
            byte[] bytes = WatermarkUtils.addWatermark(original.toByteArray(), fullInfo(title));
            var rendered = ImageIO.read(new ByteArrayInputStream(bytes));
            assertEquals(900, rendered.getWidth()); assertEquals(1200, rendered.getHeight());
            assertEquals(0xff, bytes[0] & 255); assertEquals(0xd8, bytes[1] & 255);
            assertNotEquals(source.getRGB(60, 1050), rendered.getRGB(60, 1050));
            if (previewDir != null) { Path directory = Path.of(previewDir); Files.createDirectories(directory); Files.write(directory.resolve(title + ".jpg"), bytes); }
        }
    }
}
