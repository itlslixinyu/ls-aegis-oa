package com.ls.aegis.biz.psi.service.impl;

import com.ls.aegis.biz.psi.model.req.ContractReq;
import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ContractAmountValidationTest {
    ContractReq request(String value) {
        var req=new ContractReq();req.setName("synthetic contract");req.setUnitId(1L);
        req.setSignDate(LocalDateTime.of(2026,1,1,0,0));req.setTaxInclusiveAmount(new BigDecimal(value));return req;
    }
    @ParameterizedTest @ValueSource(strings={"-1","-0.01","1.001","10000000000000000"})
    void invalidCurrencyCannotEnterEitherContractApi(String value) {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var errors=factory.getValidator().validate(request(value));
            assertFalse(errors.isEmpty());assertTrue(errors.stream().allMatch(x->x.getPropertyPath().toString().equals("taxInclusiveAmount")));
        }
    }
    @ParameterizedTest @ValueSource(strings={"0","0.01","100.50","9999999999999999.99"})
    void validIncludingZeroContractRemainsAllowed(String value) {
        try(var factory=Validation.buildDefaultValidatorFactory()) {assertTrue(factory.getValidator().validate(request(value)).isEmpty());}
    }
}
