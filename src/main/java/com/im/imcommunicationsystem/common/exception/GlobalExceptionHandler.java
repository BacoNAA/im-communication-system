package com.im.imcommunicationsystem.common.exception;

import com.im.imcommunicationsystem.common.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import javax.security.sasl.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理应用程序中的各种异常
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * 
     * @param e 业务异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtils.error(e.getCode(), e.getMessage()));
    }

    /**
     * 处理参数验证异常
     * 
     * @param e 参数验证异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("参数验证失败: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtils.error(400, "参数验证失败", errors));
    }

    /**
     * 处理绑定异常
     * 
     * @param e 绑定异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<?> handleBindException(BindException e, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("数据绑定失败: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtils.error(400, "数据绑定失败", errors));
    }

    /**
     * 处理认证异常
     * 
     * @param e 认证异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleAuthenticationException(BadCredentialsException e, HttpServletRequest request) {
        log.warn("认证失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseUtils.error(401, "用户名或密码错误"));
    }

    /**
     * 处理认证异常
     *
     * @param e 认证异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("登录认证失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseUtils.error(401, e.getMessage()));
    }

    /**
     * 处理自定义认证异常
     * 
     * @param e 自定义认证异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(com.im.imcommunicationsystem.auth.exception.AuthenticationException.class)
    public ResponseEntity<?> handleCustomAuthenticationException(com.im.imcommunicationsystem.auth.exception.AuthenticationException e, HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        if (e.hasErrorCode()) {
            log.warn("登录认证失败 [{}]: {} - 来源IP: {}, User-Agent: {}", 
                    e.getErrorCode(), e.getMessage(), clientIp, userAgent);
        } else {
            log.warn("登录认证失败: {} - 来源IP: {}, User-Agent: {}", 
                    e.getMessage(), clientIp, userAgent);
        }
        
        // 构建详细的错误响应
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", java.time.LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        
        if (e.hasErrorCode()) {
            errorDetails.put("errorCode", e.getErrorCode());
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseUtils.error(401, e.getMessage(), errorDetails));
    }
    
    /**
     * 获取客户端真实IP地址
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 处理权限不足异常
     * 
     * @param e 权限异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限不足: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseUtils.error(403, "权限不足，拒绝访问"));
    }

    /**
     * 处理文件上传大小超限异常
     * 
     * @param e 文件上传异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件上传大小超限: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtils.error(400, "文件大小超出限制"));
    }

    /**
     * 处理非法参数异常
     * 
     * @param e 非法参数异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtils.error(400, "参数错误: " + e.getMessage()));
    }

    /**
     * 处理空指针异常
     * 
     * @param e 空指针异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<?> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseUtils.error(500, "系统内部错误"));
    }

    /**
     * 处理用户ID冲突异常
     * 
     * @param e 用户ID冲突异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(com.im.imcommunicationsystem.user.exception.UserIdConflictException.class)
    public ResponseEntity<?> handleUserIdConflictException(com.im.imcommunicationsystem.user.exception.UserIdConflictException e, HttpServletRequest request) {
        log.warn("用户ID冲突: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtils.error(400, e.getMessage()));
    }

    /**
     * 处理个人ID验证异常
     * 
     * @param e 个人ID验证异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(com.im.imcommunicationsystem.user.exception.UserIdValidationException.class)
    public ResponseEntity<?> handleUserIdValidationException(com.im.imcommunicationsystem.user.exception.UserIdValidationException e, HttpServletRequest request) {
        log.warn("个人ID验证失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtils.error(400, e.getMessage()));
    }

    /**
     * 处理个人ID修改过于频繁异常
     * 
     * @param e 个人ID修改频率异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(com.im.imcommunicationsystem.user.exception.UserIdUpdateTooFrequentException.class)
    public ResponseEntity<?> handleUserIdUpdateTooFrequentException(com.im.imcommunicationsystem.user.exception.UserIdUpdateTooFrequentException e, HttpServletRequest request) {
        log.warn("个人ID修改过于频繁: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ResponseUtils.error(429, e.getMessage()));
    }

    /**
     * 处理运行时异常
     * 
     * @param e 运行时异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseUtils.error(500, "系统运行异常"));
    }

    /**
     * 处理所有未捕获的异常
     * 
     * @param e 异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseUtils.error(500, "系统异常，请联系管理员"));
    }

    /**
     * 异常处理说明：
     * 
     * 1. 业务异常（BusinessException）：
     *    - 用户操作导致的可预期异常
     *    - 返回具体的错误码和错误信息
     *    - 日志级别：WARN
     * 
     * 2. 参数验证异常：
     *    - @Valid注解验证失败
     *    - 返回详细的字段验证错误信息
     *    - 日志级别：WARN
     * 
     * 3. 认证授权异常：
     *    - 登录失败、权限不足等
     *    - 返回401或403状态码
     *    - 日志级别：WARN
     * 
     * 4. 系统异常：
     *    - 空指针、运行时异常等
     *    - 返回500状态码和通用错误信息
     *    - 日志级别：ERROR
     * 
     * 5. 文件上传异常：
     *    - 文件大小超限等
     *    - 返回400状态码
     *    - 日志级别：WARN
     * 
     * 6. 响应格式统一：
     *    - 使用ResponseUtils.error()方法
     *    - 包含错误码、错误信息和详细数据
     *    - 便于前端统一处理
     */

}