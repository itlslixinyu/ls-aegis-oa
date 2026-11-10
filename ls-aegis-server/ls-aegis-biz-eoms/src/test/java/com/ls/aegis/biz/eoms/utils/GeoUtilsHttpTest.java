package com.ls.aegis.biz.eoms.utils;
import com.sun.net.httpserver.HttpServer;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.common.api.system.SysOptionApi;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.test.util.ReflectionTestUtils;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Loopback HTTP only; no provider credential, business database or paid external API. */
class GeoUtilsHttpTest {
 HttpServer server;ExecutorService executor;GeoUtils geo;SysOptionApi options;
 AtomicInteger calls=new AtomicInteger();AtomicReference<String> query=new AtomicReference<>();
 volatile int status=200;volatile String body="{}";volatile boolean delayedBody=false;volatile long delay=0;
 ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> logs;Logger logger;
 @BeforeEach void setup() throws Exception {
  server=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);executor=Executors.newFixedThreadPool(2);server.setExecutor(executor);
  server.createContext("/map",exchange->{calls.incrementAndGet();query.set(exchange.getRequestURI().getRawQuery());try{if(!delayedBody&&delay>0)Thread.sleep(delay);byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,bytes.length);if(delayedBody&&delay>0)Thread.sleep(delay);exchange.getResponseBody().write(bytes);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}finally{exchange.close();}});server.start();
  options=mock(SysOptionApi.class);when(options.getValue(EomsConstants.OPTION_CATEGORY,EomsConstants.OPTION_CODE_GEO_BAIDU_AK)).thenReturn("SYNTHETIC&secret=marker");when(options.getValue(EomsConstants.OPTION_CATEGORY,EomsConstants.OPTION_CODE_GEO_BAIDU_REGEO_URL)).thenReturn("http://127.0.0.1:"+server.getAddress().getPort()+"/map");geo=new GeoUtils();ReflectionTestUtils.setField(geo,"sysOptionApi",options);
  logger=(Logger)LoggerFactory.getLogger(GeoUtils.class);logs=new ListAppender<>();logs.start();logger.addAppender(logs);
 }
 @AfterEach void cleanup(){server.stop(0);executor.shutdownNow();logger.detachAppender(logs);logs.stop();}
 String call(){return geo.reverseGeocode("116.481488","39.990464");}
 void result(String json){body="{\"status\":0,\"result\":"+json+"}";}
 @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" "}) void noAkDoesNotMakeHttpRequest(String absent){when(options.getValue(EomsConstants.OPTION_CATEGORY,EomsConstants.OPTION_CODE_GEO_BAIDU_AK)).thenReturn(absent);assertEquals("39.990464,116.481488",call());assertEquals(0,calls.get());assertFalse(geo.testBaiduAk("0","0").isSuccess());}
 @Test void validAddressAndEncodedParameters(){result("{\"formatted_address\":\"合成地址\"}");assertEquals("合成地址",call());String decoded=URLDecoder.decode(query.get(),StandardCharsets.UTF_8);assertTrue(decoded.contains("location=39.990464,116.481488"));assertTrue(query.get().contains("ak=SYNTHETIC%26secret%3Dmarker"));assertTrue(decoded.contains("coordtype=wgs84ll"));assertTrue(geo.testBaiduAk("0","0").isSuccess());}
 @ParameterizedTest @ValueSource(strings={"not-json","{}","{\"status\":1,\"message\":\"SENSITIVE_PROVIDER_BODY\"}","{\"status\":0,\"result\":{}}"})
 void malformedOrBusinessFailureFallsBackWithoutSensitiveLog(String response){body=response;assertEquals("39.990464,116.481488",call());assertFalse(geo.testBaiduAk("0","0").isSuccess());for(var entry:logs.list){String text=entry.getFormattedMessage();assertFalse(text.contains("SYNTHETIC"));assertFalse(text.contains("116.481488"));assertFalse(text.contains("39.990464"));assertFalse(text.contains("SENSITIVE_PROVIDER_BODY"));}}
 @ParameterizedTest @ValueSource(ints={302,401,429,500})
 void unsuccessfulHttpNeverBecomesAnAddress(int http){status=http;result("{\"formatted_address\":\"不应接受\"}");assertEquals("39.990464,116.481488",call());assertEquals(1,calls.get());}
 @ParameterizedTest @ValueSource(booleans={false,true})
 void timeoutBoundsBothHeadersAndBody(boolean bodyDelay){delayedBody=bodyDelay;delay=4500;result("{\"formatted_address\":\"迟到地址\"}");assertTimeoutPreemptively(Duration.ofSeconds(4),()->assertEquals("39.990464,116.481488",call()));}
 @Test void nearestResidentialPoiWinsUnsortedAndInvalidDistancesAreIgnored(){result("{\"addressComponent\":{\"province\":\"省\",\"city\":\"市\"},\"pois\":[{\"tag\":\"住宅区\",\"name\":\"远\",\"distance\":100},{\"tag\":\"住宅区\",\"name\":\"非法\",\"distance\":-1},{\"tag\":\"内部楼栋\",\"name\":\"近\",\"distance\":0,\"direction\":\"内\"}]}");assertEquals("省市近",call());}
 @Test void addressPriorityAndEmptyPoiFallback(){result("{\"pois\":[null,{\"tag\":\"住宅区\",\"name\":\"无距离\"}],\"formatted_address_poi\":\"兴趣点地址\",\"formatted_address\":\"街道地址\"}");assertEquals("兴趣点地址",call());}
 @Test void invalidCoordinatesNeverReachHttp(){assertThrows(RuntimeException.class,()->geo.reverseGeocode("NaN","0"));assertFalse(geo.testBaiduAk("181","0").isSuccess());assertEquals(0,calls.get());}
}
