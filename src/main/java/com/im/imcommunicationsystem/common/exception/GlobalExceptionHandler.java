package com.im.imcommunicationsystem.common.exception;

import com.im.imcommunicationsystem.common.utils.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 * 捕获Controller层抛出的异常，并返回统一的响应格式
 */
@Slf4j // Lombok注解，方便打印日志
@RestControllerAdvice // 声明这是一个作用于所有RestController的切面
public class GlobalExceptionHandler {

    /**
     * 捕获并处理业务异常 (BusinessException)
     * @param e 捕获到的业务异常
     * @return 统一的错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 捕获并处理所有其他未被处理的异常
     * @param e 捕获到的未知异常
     * @return 统一的系统错误响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleGenericException(Exception e) {
        log.error("未捕获的系统异常: ", e); // 打印完整的异常堆栈信息
        return ApiResponse.error(500, "服务器内部错误，请联系管理员");
    }
}