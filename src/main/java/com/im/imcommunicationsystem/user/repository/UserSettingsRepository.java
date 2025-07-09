package com.im.imcommunicationsystem.user.repository;

import com.im.imcommunicationsystem.user.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户设置数据访问层
 * 处理用户设置相关的数据库操作
 */
@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    /**
     * 根据用户ID查找用户设置
     * 
     * @param userId 用户ID
     * @return 用户设置信息
     */
    Optional<UserSettings> findByUserId(Long userId);

    /**
     * 根据用户ID删除用户设置
     * 
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);

    /**
     * 检查用户是否存在设置
     * 
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByUserId(Long userId);
}