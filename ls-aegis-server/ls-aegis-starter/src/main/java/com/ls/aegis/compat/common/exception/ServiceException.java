package com.ls.aegis.compat.common.exception;

import com.feiniaojin.gracefulresponse.api.ExceptionMapper;

/**
 * 业务异常。
 * <p>
 * 标注 {@code msgReplaceable=true}，让 graceful-response 使用实例文案，
 * 避免被包成「服务器异常，请联系管理员」。
 */
@ExceptionMapper(code = "1", msg = "业务处理失败", msgReplaceable = true)
public final class ServiceException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误提示
     */
    private String message;

    /**
     * 错误明细，内部调试错误
     *
     * 和 {@link CommonResult#getDetailMessage()} 一致的设计
     */
    private String detailMessage;

    /**
     * 空构造方法，避免反序列化问题
     */
    public ServiceException()
    {
    }

    public ServiceException(String message)
    {
        super(message);
        this.message = message;
    }

    public ServiceException(String message, Integer code)
    {
        super(message);
        this.message = message;
        this.code = code;
    }

    public String getDetailMessage()
    {
        return detailMessage;
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    public Integer getCode()
    {
        return code;
    }

    public ServiceException setMessage(String message)
    {
        this.message = message;
        return this;
    }

    public ServiceException setDetailMessage(String detailMessage)
    {
        this.detailMessage = detailMessage;
        return this;
    }
}

