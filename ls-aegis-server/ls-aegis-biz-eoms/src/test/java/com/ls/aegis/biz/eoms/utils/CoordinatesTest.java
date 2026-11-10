package com.ls.aegis.biz.eoms.utils;
import com.ls.aegis.biz.eoms.controller.h5.EngineerController;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
class CoordinatesTest {
 @ParameterizedTest @CsvSource({"0,0","-180,-90","180,90","120.123456,30.123456"})
 void validPairsIncludingZero(String lon,String lat){assertDoesNotThrow(()->Coordinates.require(lon,lat));assertDoesNotThrow(()->Coordinates.optional(lon,lat));}
 @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ","NaN","Infinity","-Infinity","181","-181","abc","1&key=x"," 0"})
 void invalidLongitudeBeforeAnyControllerWork(String lon){assertThrows(ServiceException.class,()->Coordinates.optional(lon,"0"));var c=new EngineerController();assertThrows(ServiceException.class,()->c.start(1L,null,lon,"0"));assertThrows(ServiceException.class,()->c.finish(1L,null,lon,"0"));}
 @ParameterizedTest @ValueSource(strings={"91","-91","NaN","Infinity"," "})
 void invalidLatitude(String lat){assertThrows(ServiceException.class,()->Coordinates.require("0",lat));}
 @Test void optionalMeansBothAbsentNotHalfProvided(){assertDoesNotThrow(()->Coordinates.optional(null,null));assertThrows(ServiceException.class,()->Coordinates.optional("0",null));assertThrows(ServiceException.class,()->Coordinates.optional(null,"0"));assertThrows(ServiceException.class,()->Coordinates.require(null,null));}
}
