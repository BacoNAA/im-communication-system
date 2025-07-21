package com.im.imcommunicationsystem.admin.repository;

import com.im.imcommunicationsystem.admin.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 管理员用户仓库接口
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    
    /**
     * 根据用户名查找管理员用户
     * 
     * @param username 用户名
     * @return 可选的管理员用户
     */
    Optional<AdminUser> findByUsername(String username);
    
    /**
     * 根据邮箱查找管理员用户
     * 
     * @param email 邮箱
     * @return 可选的管理员用户
     */
    Optional<AdminUser> findByEmail(String email);
    
    /**
     * 根据角色查找管理员用户
     * 
     * @param role 角色
     * @return 管理员用户列表
     */
    java.util.List<AdminUser> findByRole(AdminUser.AdminRole role);
    
    /**
     * 检查用户名是否已存在
     * 
     * @param username 用户名
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否已存在
     * 
     * @param email 邮箱
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByEmail(String email);
} 