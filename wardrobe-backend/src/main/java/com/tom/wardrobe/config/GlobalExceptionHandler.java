package com.tom.wardrobe.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回友好提示，不暴露技术细节
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Map<String, Object>> handleNotLoginException(NotLoginException e) {
        log.warn("未登录访问: {}", e.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "请先登录");
    }

    /**
     * 处理无权限异常
     */
    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<Map<String, Object>> handleNotRoleException(NotRoleException e) {
        log.warn("权限不足: {}", e.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "没有权限访问此资源");
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传过大: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "文件大小超过限制");
    }

    /**
     * 处理 IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "参数错误");
    }

    /**
     * 处理 RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("未知异常", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
    }

    /**
     * 构建统一响应
     */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", status.value());
        response.put("message", message);
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            response.put("traceId", traceId);
        }
        return ResponseEntity.status(status).body(response);
    }
}
