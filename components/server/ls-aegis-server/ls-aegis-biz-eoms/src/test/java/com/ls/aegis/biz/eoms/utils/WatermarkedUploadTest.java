package com.ls.aegis.biz.eoms.utils;

import com.ls.aegis.biz.file.service.FileService;
import com.ls.aegis.compat.common.utils.FileMagicValidator;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.dromara.x.file.storage.core.FileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WatermarkedUploadTest {
 byte[] image(String format) throws Exception {var im=new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB);var g=im.createGraphics();g.setColor(Color.BLUE);g.fillRect(0,0,640,480);g.dispose();var b=new ByteArrayOutputStream();assertTrue(ImageIO.write(im,format,b));return b.toByteArray();}
 @ParameterizedTest @ValueSource(strings={"png","jpg","jpeg"})
 void actualEncodingMagicStorageNameAndPayloadAgree(String ext) throws Exception {
  byte[] original=image(ext.equals("png")?"png":"jpg");
  var input=new MockMultipartFile("file","现场."+ext,"image/"+ext,original);assertTrue(WatermarkUtils.isSupportedImage(input));FileMagicValidator.assertValid(input,ext);
  var info=new WatermarkInfo();info.setTitle("合成现场测试");info.setRemark("水印不可省略");byte[] jpeg=WatermarkUtils.addWatermark(original,info);
  assertEquals(0xff,jpeg[0]&255);assertEquals(0xd8,jpeg[1]&255);assertEquals(0xff,jpeg[2]&255);assertNotNull(ImageIO.read(new ByteArrayInputStream(jpeg)));assertFalse(java.util.Arrays.equals(original,jpeg));
  if(ext.equals("png"))assertThrows(ServiceException.class,()->FileMagicValidator.assertValid(new MockMultipartFile("file","现场.png","image/png",jpeg),"png"),"old upload contract must reproduce the strict magic rejection");
  var helper=new EomsPlatformFileHelper();var fs=mock(FileService.class);ReflectionTestUtils.setField(helper,"fileService",fs);
  when(fs.upload(any(MultipartFile.class),eq("/eoms/repair"))).thenAnswer(call->{MultipartFile uploaded=call.getArgument(0);assertEquals("现场.jpg",uploaded.getOriginalFilename());assertEquals("image/jpeg",uploaded.getContentType());FileMagicValidator.assertValid(uploaded,"jpg");assertArrayEquals(jpeg,uploaded.getBytes());var out=new FileInfo();out.setId("123");return out;});
  var stored=helper.uploadWatermarkedJpeg(jpeg,input.getOriginalFilename(),"/eoms/repair");var payload=helper.h5UploadPayload(stored,input.getOriginalFilename());assertEquals("现场.jpg",payload.get("fileName"));assertEquals(payload.get("fileName"),payload.get("originalFilename"));verify(fs,times(1)).upload(any(MultipartFile.class),eq("/eoms/repair"));
 }
 @ParameterizedTest @CsvSource({"'../../photo.PNG',photo.jpg","'C:\\fakepath\\photo.JPEG',photo.jpg","'.png',photo.jpg","'..',photo.jpg","'  ',photo.jpg","'a:b?.png',a_b_.jpg","'photo',photo.jpg"})
 void onlySafeJpegBasenames(String input,String expected){assertEquals(expected,new EomsPlatformFileHelper().ensureJpegName(input));}
 @Test void missingAndControlNamesAreSafe(){var h=new EomsPlatformFileHelper();assertEquals("photo.jpg",h.ensureJpegName(null));assertEquals("a_b.jpg",h.ensureJpegName("a\nb.png"));assertTrue(h.ensureJpegName("x".repeat(400)+".png").length()<=104);}
 @Test void ordinaryPngIsStillStrictlyPng() throws Exception {var png=new MockMultipartFile("file","photo.png","image/png",image("png"));FileMagicValidator.assertValid(png,"png");assertThrows(ServiceException.class,()->FileMagicValidator.assertValid(png,"jpg"));}
}
