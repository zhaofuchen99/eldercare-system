package com.zfc.eldercare.core.common;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 统一响应封装（详见详细设计文档 7.1 / 11.3）。
 * code 与 HTTP 状态码对齐：200 成功，400/401/403/404/409/429/500 对应各类错误。
 */
@Getter
@Setter
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 响应数据 */
    private T data;
    /** 追踪 ID（错误响应时生成，便于排查） */
    private String traceId;

    public Result() {
    }

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
