package com.ls.aegis.biz.psi.api;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ls.aegis.biz.psi.mapper.UnitMapper;
import com.ls.aegis.biz.psi.model.entity.UnitDO;
import com.ls.aegis.common.context.*;
import net.sf.jsqlparser.expression.*;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import top.continew.starter.extension.datapermission.enums.DataScope;
import top.continew.starter.extension.datapermission.handler.DefaultDataPermissionHandler;
import top.continew.starter.extension.datapermission.model.*;
import top.continew.starter.extension.datapermission.provider.DataPermissionUserDataProvider;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real MyBatis tenant + platform DEPT chain; capability lookup bypass is explicitly bounded. */
class H5CustomerAccessBoundaryTest {
    JdbcTemplate jdbc; H5CustomerAccessApiImpl api;
    @BeforeEach void setup() throws Exception {
        UserContext user=new UserContext();user.setId(99L);user.setDeptId(12L);user.setTenantId(7L);UserContextHolder.setContext(user,false);
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");ds.setDriverClassName("org.h2.Driver");jdbc=new JdbcTemplate(ds);
        var config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);
        config.setDefaultEnumTypeHandler(top.continew.starter.data.handler.CompositeBaseEnumTypeHandler.class);
        GlobalConfigUtils.getGlobalConfig(config).getDbConfig().setLogicDeleteField("deleted").setLogicNotDeleteValue("0").setLogicDeleteValue("id");
        var chain=new MybatisPlusInterceptor();chain.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){public Expression getTenantId(){return new LongValue(UserContextHolder.getTenantId());}}));
        chain.addInnerInterceptor(new DataPermissionInterceptor(new DefaultDataPermissionHandler(new DataPermissionUserDataProvider(){
            public boolean isFilter(){return true;}
            public UserData getUserData(){return new UserData(99L,12L,Set.of(new RoleData(1L,DataScope.DEPT)));}
        })));
        config.addInterceptor(chain);config.addMapper(UnitMapper.class);
        var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(config);
        api=new H5CustomerAccessApiImpl(new SqlSessionTemplate(factory.getObject()).getMapper(UnitMapper.class));
        var table=TableInfoHelper.getTableInfo(UnitDO.class);var ddl=new StringBuilder("create table psi_unit(id bigint primary key");
        for(var field:table.getFieldList()){
            String type=field.getPropertyType()==Long.class?"bigint":field.getPropertyType()==LocalDateTime.class?"timestamp":field.getProperty().equals("status")?"int":"varchar(512)";
            ddl.append(',').append(field.getColumn()).append(' ').append(type);if(field.getProperty().equals("deleted"))ddl.append(" default 0");
        }
        jdbc.execute(ddl.append(')').toString());
        seed(1,7,12,"CUSTOMER",1,0);seed(2,7,20,"BOTH",1,0);seed(3,8,12,"CUSTOMER",1,0);
        seed(4,7,12,"SUPPLIER",1,0);seed(5,7,12,"CUSTOMER",2,0);seed(6,7,12,"CUSTOMER",1,6);
    }
    @AfterEach void cleanup(){UserContextHolder.clearContext();jdbc.execute("SHUTDOWN");}
    @Test void staffIssuanceUsesRealDepartmentAndTenantPermissionChain(){
        assertEquals(1L,api.requireManagedCustomer(1L).customerId());
        assertThrows(RuntimeException.class,()->api.requireManagedCustomer(2L));
        assertThrows(RuntimeException.class,()->api.requireManagedCustomer(3L));
    }
    @Test void capabilityResolutionAcceptsOnlyExactBoundTenantAndId(){
        // Customer session is independent of a staff user's DEPT, but never tenant/identity scope.
        assertEquals(2L,api.requireAvailableCustomer(7L,2L).customerId());
        assertThrows(RuntimeException.class,()->api.requireAvailableCustomer(8L,2L));
        assertThrows(RuntimeException.class,()->api.requireAvailableCustomer(null,2L));
        assertThrows(RuntimeException.class,()->api.requireAvailableCustomer(7L,null));
        assertEquals(8L,api.requireAvailableCustomer(8L,3L).tenantId());
    }
    @Test void disabledDeletedAndSupplierNeverReceiveCustomerIdentity(){
        for(long id:new long[]{4,5,6}) {
            assertThrows(RuntimeException.class,()->api.requireManagedCustomer(id));
            assertThrows(RuntimeException.class,()->api.requireAvailableCustomer(7L,id));
        }
        jdbc.update("update psi_unit set status=2 where id=1");
        assertThrows(RuntimeException.class,()->api.requireAvailableCustomer(7L,1L));
    }
    void seed(long id,long tenant,long dept,String type,int status,long deleted){jdbc.update("insert into psi_unit(id,tenant_id,dept_id,unit_code,unit_name,unit_type,status,deleted) values(?,?,?,?,?,?,?,?)",id,tenant,dept,"WLDW"+id,"synthetic",type,status,deleted);}
}
