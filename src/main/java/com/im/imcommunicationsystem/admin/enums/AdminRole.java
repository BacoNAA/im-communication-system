package com.im.imcommunicationsystem.admin.enums;

/**
 * 管理员角色枚举
 */
public enum AdminRole {
    /**
     * 超级管理员
     * 拥有所有权限
     */
    super_admin("超级管理员"),
    
    /**
     * 普通管理员
     * 拥有大部分管理权限，但无法管理其他管理员
     */
    admin("管理员"),
    
    /**
     * 内容审核员
     * 主要负责内容审核相关工作
     */
    moderator("内容审核员");
    
    /**
     * 角色的中文描述
     */
    private final String description;
    
    /**
     * 构造函数
     * 
     * @param description 角色的中文描述
     */
    AdminRole(String description) {
        this.description = description;
    }
    
    /**
     * 获取角色的中文描述
     * 
     * @return 角色的中文描述
     */
    public String getDescription() {
        return this.description;
    }
} 