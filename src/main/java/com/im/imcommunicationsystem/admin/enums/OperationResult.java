package com.im.imcommunicationsystem.admin.enums;

/**
 * 管理员操作结果枚举
 * 
 * @deprecated 此枚举类已不再使用，admin_operation_logs表不再需要result字段
 */
@Deprecated
public enum OperationResult {
    /**
     * 操作成功
     */
    SUCCESS("成功"),
    
    /**
     * 操作失败
     */
    FAILURE("失败"),
    
    /**
     * 操作被拒绝
     */
    REJECTED("被拒绝"),
    
    /**
     * 操作被取消
     */
    CANCELLED("已取消"),
    
    /**
     * 操作超时
     */
    TIMEOUT("超时"),
    
    /**
     * 操作部分成功
     */
    PARTIAL_SUCCESS("部分成功"),
    
    /**
     * 操作进行中
     */
    IN_PROGRESS("进行中"),
    
    /**
     * 操作等待中
     */
    PENDING("等待中");
    
    /**
     * 结果描述
     */
    private final String description;
    
    /**
     * 构造函数
     * 
     * @param description 结果描述
     */
    OperationResult(String description) {
        this.description = description;
    }
    
    /**
     * 获取结果描述
     * 
     * @return 结果描述
     */
    public String getDescription() {
        return this.description;
    }
} 