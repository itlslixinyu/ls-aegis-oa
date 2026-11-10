package com.ls.aegis.biz.psi.service.impl;

import com.ls.aegis.biz.psi.model.req.CompanyAccountReq;
import org.junit.jupiter.api.*;
import top.continew.starter.core.exception.BusinessException;
import static org.junit.jupiter.api.Assertions.*;

class CompanyAccountValidationTest {
    private final CompanyAccountServiceImpl service=new CompanyAccountServiceImpl();
    private CompanyAccountReq request(){
        var req=new CompanyAccountReq();req.setAccountName("synthetic");req.setBankName("synthetic bank");req.setAccountNo("NOT-A-REAL-ACCOUNT");return req;
    }
    @Test void importedRowsMustHonorUiRequiredAccountFields(){
        for(int missing=0;missing<3;missing++){
            var req=request();if(missing==0)req.setAccountName(null);if(missing==1)req.setBankName(" ");if(missing==2)req.setAccountNo(null);
            assertThrows(BusinessException.class,()->service.beforeCreate(req));
            assertThrows(BusinessException.class,()->service.beforeUpdate(req,1L));
        }
    }
    @Test void updateCannotMoveAccountIntoClientSuppliedTenantOrDepartment(){
        var req=request();req.setTenantId(999L);req.setDeptId(888L);service.beforeUpdate(req,1L);
        assertNull(req.getTenantId());assertNull(req.getDeptId());
    }
}
