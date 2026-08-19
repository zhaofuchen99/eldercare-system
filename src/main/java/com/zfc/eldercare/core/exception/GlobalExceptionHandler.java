package com.zfc.eldercare.core.exception;

import com.zfc.eldercare.core.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * 全局异常处理器（详见详细设计文档 11.2）。
 * 认证(401)、权限(403) 等异常在引入 Spring Security 后追加处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 → 400 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验异常 → 400，返回具体字段错误 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : "参数校验失败";
        log.warn("参数校验异常: {}", message);
        return Result.error(400, message);
    }

    /** 未知异常 → 500，记录完整日志并生成 traceId */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.error("系统异常 traceId={}", traceId, e);
        Result<Void> result = Result.error("系统繁忙，请稍后重试");
        result.setTraceId(traceId);
        return result;
    }
}
