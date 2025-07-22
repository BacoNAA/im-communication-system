package com.im.imcommunicationsystem.admin.enums;

/**
 * 管理员操作类型枚举
 */
public enum OperationType {
    /**
     * 登录操作
     */
    LOGIN("登录系统"),
    
    /**
     * 登出操作
     */
    LOGOUT("登出系统"),
    
    /**
     * 创建操作
     */
    CREATE("创建"),
    
    /**
     * 更新操作
     */
    UPDATE("更新"),
    
    /**
     * 删除操作
     */
    DELETE("删除"),
    
    /**
     * 查询操作
     */
    QUERY("查询"),
    
    /**
     * 冻结用户
     */
    FREEZE_USER("冻结用户"),
    
    /**
     * 解冻用户
     */
    UNFREEZE_USER("解冻用户"),
    
    /**
     * 重置密码
     */
    RESET_PASSWORD("重置密码"),
    
    /**
     * 审核内容
     */
    MODERATE_CONTENT("审核内容"),
    
    /**
     * 系统配置
     */
    SYSTEM_CONFIG("系统配置"),
    
    /**
     * 导出数据
     */
    EXPORT_DATA("导出数据"),
    
    /**
     * 导入数据
     */
    IMPORT_DATA("导入数据"),
    
    /**
     * 创建系统通知
     */
    CREATE_NOTIFICATION("创建系统通知"),
    
    /**
     * 更新系统通知
     */
    UPDATE_NOTIFICATION("更新系统通知"),
    
    /**
     * 删除系统通知
     */
    DELETE_NOTIFICATION("删除系统通知"),
    
    /**
     * 发布系统通知
     */
    PUBLISH_NOTIFICATION("发布系统通知"),
    
    /**
     * 其他操作
     */
    OTHER("其他操作");
    
    /**
     * 操作描述
     */
    private final String description;
    
    /**
     * 构造函数
     * 
     * @param description 操作描述
     */
    OperationType(String description) {
        this.description = description;
    }
    
    /**
     * 获取操作描述
     * 
     * @return 操作描述
     */
    public String getDescription() {
        return this.description;
    }
} 