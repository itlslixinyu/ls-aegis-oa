package com.ls.aegis.compat.common.exception;

import com.feiniaojin.gracefulresponse.api.ExcludeFromGracefulResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.web.model.R;

/**
 * 业务异常转为平台 R 失败，避免被 graceful-response 包成「服务器异常」。
 * <p>
 * 须高于 graceful-response 默认 {@code Exception} 处理（否则 {@link BusinessException}
 * 文案会被换成配置项 default-error-msg）。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ExcludeFromGracefulResponse
@RestControllerAdvice
public class ServiceExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ServiceExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public R<Void> handle(ServiceException e, HttpServletRequest request) {
        log.warn("[{}] {} {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        String code = e.getCode() != null ? String.valueOf(e.getCode()) : "1";
        return R.fail(code, e.getMessage() != null ? e.getMessage() : "业务处理失败");
    }

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("[{}] {} {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return R.fail("1", e.getMessage() != null ? e.getMessage() : "业务处理失败");
    }

    /**
     * 数据库约束等未包装异常（如 NOT NULL）否则会被包成「服务器异常，请联系管理员」。
     */
    @ExceptionHandler(DataAccessException.class)
    public R<Void> handleDataAccess(DataAccessException e, HttpServletRequest request) {
        Throwable root = e.getMostSpecificCause() != null ? e.getMostSpecificCause() : e;
        log.error("[{}] {} data access: {}", request.getMethod(), request.getRequestURI(), root.getMessage(), e);
        String detail = root.getMessage();
        if (detail != null && detail.length() > 180) {
            detail = detail.substring(0, 180) + "...";
        }
        return R.fail("1", detail != null ? ("数据保存失败: " + detail) : "数据保存失败");
    }
}
