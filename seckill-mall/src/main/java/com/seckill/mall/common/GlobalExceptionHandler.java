package com.seckill.mall.common;

import com.seckill.mall.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * M-S6 修复：为不同异常设置对应 HTTP 状态码（400/401/403/404/429/500），
 * body 保留业务 code，恢复 REST 语义与网关监控能力。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：GlobalExceptionHandler.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：根据 ErrorCode 映射 HTTP 状态码。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        HttpStatus httpStatus = mapErrorCodeToHttpStatus(e.getErrorCode());
        return ResponseEntity.status(httpStatus).body(Result.error(e.getErrorCode().getCode(), e.getMessage()));
    }

    /**
     * 参数校验异常 → 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 约束校验异常 → 400 Bad Request
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("约束校验异常: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 权限不足 → 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问被拒绝: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(ErrorCode.FORBIDDEN));
    }

    /**
     * 认证异常 → 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 请求方法不支持 → 405 Method Not Allowed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.error(ErrorCode.METHOD_NOT_ALLOWED.getCode(), e.getMessage()));
    }

    /**
     * 缺少必需参数 → 400 Bad Request
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必需参数: {}", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(ErrorCode.PARAM_ERROR.getCode(), "缺少必需参数: " + e.getParameterName()));
    }

    /**
     * 请求体格式错误 → 400 Bad Request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(ErrorCode.PARAM_ERROR.getCode(), "请求体格式错误"));
    }

    /**
     * 文件大小超限 → 413 Payload Too Large
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("文件大小超限: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Result.error(ErrorCode.FILE_TOO_LARGE));
    }

    /**
     * 数据完整性冲突 → 409 Conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("数据完整性冲突: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.error(ErrorCode.DATA_CONFLICT));
    }

    /**
     * 请求超时 → 504 Gateway Timeout
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Result<Void>> handleTimeout(TimeoutException e) {
        log.warn("请求超时: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(Result.error(ErrorCode.REQUEST_TIMEOUT));
    }

    /**
     * 接口不存在 → 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("接口不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(ErrorCode.NOT_FOUND));
    }

    /**
     * 兜底异常 → 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ErrorCode.SYSTEM_ERROR));
    }

    /**
     * M-S6 修复：将业务 ErrorCode 映射为对应 HTTP 状态码。
     * - 429 → RATE_LIMIT_EXCEEDED
     * - 401 → UNAUTHORIZED / USERNAME_OR_PASSWORD_ERROR / ACCOUNT_DISABLED / LOGIN_LOCKED
     * - 403 → FORBIDDEN / REPLAY_DETECTED / SECKILL_TOKEN_INVALID
     * - 404 → *_NOT_FOUND 系列
     * - 400 → PARAM_ERROR / CAPTCHA_ERROR / VERIFICATION_CODE_* 系列
     * - 500 → SYSTEM_ERROR
     */
    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        int code = errorCode.getCode();
        // 限流 → 429
        if (code == 429) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        // 未授权 → 401
        if (errorCode == ErrorCode.UNAUTHORIZED) {
            return HttpStatus.UNAUTHORIZED;
        }
        // 登录失败 / 账号禁用 / 登录锁定 → 401 (凭证错误，非权限错误)
        if (errorCode == ErrorCode.USERNAME_OR_PASSWORD_ERROR
                || errorCode == ErrorCode.ACCOUNT_DISABLED
                || errorCode == ErrorCode.LOGIN_LOCKED) {
            return HttpStatus.UNAUTHORIZED;
        }
        // 禁止访问 / 防重放 / token 失效 → 403
        if (errorCode == ErrorCode.FORBIDDEN
                || errorCode == ErrorCode.REPLAY_DETECTED
                || errorCode == ErrorCode.SECKILL_TOKEN_INVALID) {
            return HttpStatus.FORBIDDEN;
        }
        // 不存在 → 404
        String name = errorCode.name();
        if (name.endsWith("_NOT_FOUND") || name.endsWith("_NOT_FOUND_BY_ACCOUNT")) {
            return HttpStatus.NOT_FOUND;
        }
        // 参数/验证码错误 → 400
        if (errorCode == ErrorCode.PARAM_ERROR
                || errorCode == ErrorCode.CAPTCHA_ERROR
                || errorCode == ErrorCode.VERIFICATION_CODE_INVALID
                || errorCode == ErrorCode.VERIFICATION_CODE_ERROR
                || errorCode == ErrorCode.VERIFICATION_CODE_EXPIRED
                || errorCode == ErrorCode.VERIFICATION_CODE_RATE_LIMIT
                || errorCode == ErrorCode.PASSWORD_NOT_MATCH
                || errorCode == ErrorCode.CART_QUANTITY_INVALID) {
            return HttpStatus.BAD_REQUEST;
        }
        // 系统错误 → 500
        if (errorCode == ErrorCode.SYSTEM_ERROR) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // HTTP 异常相关 → 对应状态码
        if (errorCode == ErrorCode.NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        }
        if (errorCode == ErrorCode.METHOD_NOT_ALLOWED) {
            return HttpStatus.METHOD_NOT_ALLOWED;
        }
        if (errorCode == ErrorCode.REQUEST_TIMEOUT) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (errorCode == ErrorCode.DATA_CONFLICT) {
            return HttpStatus.CONFLICT;
        }
        // 其他业务异常默认 400
        return HttpStatus.BAD_REQUEST;
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
