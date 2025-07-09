package com.im.imcommunicationsystem.user.repository;

import com.im.imcommunicationsystem.auth.entity.LoginDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户设备数据访问接口
 * 继承自JpaRepository，提供设备相关的数据操作
 */
@Repository
public interface UserDeviceRepository extends JpaRepository<LoginDevice, Long> {

    /**
     * 根据用户ID查找所有设备
     * 
     * @param userId 用户ID
     * @return 设备列表
     */
    List<LoginDevice> findByUserId(Long userId);

    /**
     * 根据用户ID查找活跃设备
     * 
     * @param userId 用户ID
     * @return 活跃设备列表
     */
    List<LoginDevice> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * 根据用户ID和设备ID查找设备
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @return 设备信息
     */
    Optional<LoginDevice> findByUserIdAndId(Long userId, Long deviceId);

    /**
     * 统计用户设备数量
     * 
     * @param userId 用户ID
     * @return 设备数量
     */
    long countByUserId(Long userId);

    /**
     * 统计用户活跃设备数量
     * 
     * @param userId 用户ID
     * @return 活跃设备数量
     */
    long countByUserIdAndIsActiveTrue(Long userId);

    /**
     * 查找指定时间之前的非活跃设备
     * 
     * @param userId 用户ID
     * @param beforeTime 时间点
     * @return 非活跃设备列表
     */
    @Query("SELECT d FROM LoginDevice d WHERE d.userId = :userId AND d.lastLoginAt < :beforeTime AND d.isActive = false")
    List<LoginDevice> findInactiveDevicesBefore(@Param("userId") Long userId, @Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 根据设备类型统计用户设备
     * 
     * @param userId 用户ID
     * @return 设备类型统计
     */
    @Query("SELECT d.deviceType, COUNT(d) FROM LoginDevice d WHERE d.userId = :userId GROUP BY d.deviceType")
    List<Object[]> countDevicesByType(@Param("userId") Long userId);
}