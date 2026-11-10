package com.ls.aegis.biz.eoms.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.domain.H5CustomerInfo;
import com.ls.aegis.biz.eoms.service.h5.H5TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * H5 客户身份令牌拦截器
 * 拦截 /eoms/h5/** 接口（排除 /eoms/h5/auth/** 授权接口），
 * 从请求头 h5-token 获取令牌并校验，校验通过则注入 customerId 到 request attribute。
 *
 * @author EOMS
 */
@Component
public class H5TokenInterceptor implements HandlerInterceptor
{
    @Autowired
    private H5TokenService h5TokenService;

    /** 请求头令牌字段名 */
    public static final String HEADER_H5_TOKEN = "h5-token";

    /** request attribute 中客户ID的key */
    public static final String ATTR_TENANT_ID = "h5TenantId";

    public static final String ATTR_CUSTOMER_ID = "h5CustomerId";

    /** request attribute 中客户信息的key */
    public static final String ATTR_CUSTOMER_INFO = "h5CustomerInfo";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        String token = request.getHeader(HEADER_H5_TOKEN);
        if (StringUtils.isEmpty(token))
        {
            token = request.getParameter("h5-token");
        }
        H5CustomerInfo customerInfo = h5TokenService.validateToken(token);
        if (customerInfo == null)
        {
            // 与平台 R 形态对齐（success/code/msg），业务码仍用 10007 供前端识别
            response.setStatus(200);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"success\":false,\"code\":" + EomsConstants.CODE_H5_TOKEN_INVALID
                    + ",\"msg\":\"H5身份令牌无效或已过期\",\"data\":null}");
            response.getWriter().flush();
            return false;
        }
        // 注入客户上下文到 request，供后续 Controller 使用
        request.setAttribute(ATTR_CUSTOMER_ID, customerInfo.getCustomerId());
        request.setAttribute(ATTR_TENANT_ID, customerInfo.getTenantId());
        request.setAttribute(ATTR_CUSTOMER_INFO, customerInfo);
        // 续期令牌（活跃用户保持登录）
        h5TokenService.refreshToken(token);
        return true;
    }
}
