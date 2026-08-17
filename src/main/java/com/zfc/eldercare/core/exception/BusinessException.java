package com.zfc.eldercare.core.exception;

import lombok.Getter;

/**
 * 业务异常（详见详细设计文档 11.1）。
 * 默认错误码 400，可在构造时指定。
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
    }
}
