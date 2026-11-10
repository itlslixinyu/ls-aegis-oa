package com.ls.aegis.biz.eoms.finance;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LockSqlRewriteTest {
    @Test void lockingQueriesRemainValidAfterRealTenantSqlRewrite() throws Exception {
        var interceptor=new TenantLineInnerInterceptor(new TenantLineHandler(){
            @Override public Expression getTenantId(){return new LongValue(0);}
        });
        for(String name:List.of("RepairMaterial","Receivable","Cost")){
            var factory=DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
            try(var in=getClass().getResourceAsStream("/mapper/eoms/"+name+"Mapper.xml")){
                assertNotNull(in);
                var selects=factory.newDocumentBuilder().parse(in).getElementsByTagName("select");
                boolean found=false;
                for(int i=0;i<selects.getLength();i++){
                    var node=(Element)selects.item(i);
                    if(!"lockByOrder".equals(node.getAttribute("id")))continue;
                    found=true;
                    var rewritten=interceptor.parserSingle(node.getTextContent().replace("#{orderId}","1"),null).trim().toUpperCase();
                    assertTrue(rewritten.endsWith("FOR UPDATE"),name+": "+rewritten);
                    assertFalse(rewritten.contains("FOR UPDATE ORDER BY"),name+": "+rewritten);
                }
                assertTrue(found,name);
            }
        }
    }
}
