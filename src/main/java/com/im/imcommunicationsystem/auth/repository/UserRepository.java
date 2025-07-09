package com.im.imcommunicationsystem.auth.repository;

import com.im.imcommunicationsystem.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据仓库
 * 用户数据的持久化操作
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据邮箱查找用户
     * @param email 邮箱地址
     * @return 用户信息
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户ID字符串查找用户
     * @param userIdStr 用户ID字符串
     * @return 用户信息
     */
    Optional<User> findByUserIdStr(String userIdStr);

    /**
     * 检查邮箱是否存在
     * @param email 邮箱地址
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查用户ID是否存在
     * @param userIdStr 用户ID字符串
     * @return 是否存在
     */
    boolean existsByUserIdStr(String userIdStr);

    /**
     * 检查用户ID字符串是否存在（方法别名）
     * @param userIdString 用户ID字符串
     * @return 是否存在
     */
    default boolean existsByUserIdString(String userIdString) {
        return existsByUserIdStr(userIdString);
    }
}