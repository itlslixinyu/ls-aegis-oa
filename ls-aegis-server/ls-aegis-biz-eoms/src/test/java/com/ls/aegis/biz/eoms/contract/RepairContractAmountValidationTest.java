package com.ls.aegis.biz.eoms.contract;

import com.ls.aegis.biz.eoms.domain.Contract;
import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class RepairContractAmountValidationTest {
    Contract request(String amount){var c=new Contract();c.setCustomerId(1L);c.setContractName("synthetic contract");c.setSignDate(new Date());c.setAmount(new BigDecimal(amount));return c;}
    @ParameterizedTest @ValueSource(strings={"-1","-0.01","1.001","10000000000"})
    void invalidCurrencyIsRejectedBeforePersistence(String value){try(var f=Validation.buildDefaultValidatorFactory()){var errors=f.getValidator().validate(request(value));assertFalse(errors.isEmpty());assertTrue(errors.stream().allMatch(x->x.getPropertyPath().toString().equals("amount")));}}
    @ParameterizedTest @ValueSource(strings={"0","0.01","100.50","9999999999.99"})
    void amountsWithinDatabasePrecisionRemainValid(String value){try(var f=Validation.buildDefaultValidatorFactory()){assertTrue(f.getValidator().validate(request(value)).isEmpty());}}
}
