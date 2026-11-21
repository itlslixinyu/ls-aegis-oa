package com.ls.aegis.biz.eoms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.ls.aegis.biz.eoms.interceptor.H5TokenInterceptor;

/**
 * EOMS Web MVC 配置
 * 注册 H5 客户身份令牌拦截器，拦截 /eoms/h5/** 排除 /eoms/h5/auth/**
 *
 * @author EOMS
 */
@Configuration
public class EomsWebMvcConfig implements WebMvcConfigurer
{
    @Autowired
    private H5TokenInterceptor h5TokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(h5TokenInterceptor)
                .addPathPatterns("/eoms/h5/**")
                .excludePathPatterns("/eoms/h5/auth/**");
    }
}
