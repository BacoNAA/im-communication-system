package com.im.imcommunicationsystem.auth.repository;

import com.im.imcommunicationsystem.auth.entity.LoginDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 登录设备数据仓库
 * 登录设备数据的持久化操作
 */
@Repository
public interface LoginDeviceRepository extends JpaRepository<LoginDevice, Long> {

    /**
     * 查找用户的活跃设备
     * @param userId 用户ID
     * @return 活跃设备列表
     */
    List<LoginDevice> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * 查找特定类型的活跃设备
     * @param userId 用户ID
     * @param deviceType 设备类型
     * @return 活跃设备列表
     */
    List<LoginDevice> findByUserIdAndDeviceTypeAndIsActiveTrue(Long userId, String deviceType);

    /**
     * 删除指定用户的特定设备
     * @param userId 用户ID
     * @param deviceType 设备类型
     */
    void deleteByUserIdAndDeviceType(Long userId, String deviceType);

    /**
     * 根据用户ID和设备类型查找设备
     * @param userId 用户ID
     * @param deviceType 设备类型
     * @return 设备信息
     */
    Optional<LoginDevice> findByUserIdAndDeviceType(Long userId, String deviceType);

    /**
     * 根据用户ID查找所有设备
     * @param userId 用户ID
     * @return 设备列表
     */
    List<LoginDevice> findByUserId(Long userId);

    /**
     * 根据用户ID和设备信息查找设备
     * @param userId 用户ID
     * @param deviceInfo 设备信息
     * @return 设备信息
     */
    Optional<LoginDevice> findByUserIdAndDeviceInfo(Long userId, String deviceInfo);

    /**
     * 删除指定用户的特定设备（根据设备信息）
     * @param userId 用户ID
     * @param deviceInfo 设备信息
     */
    void deleteByUserIdAndDeviceInfo(Long userId, String deviceInfo);

    /**
     * 将所有活跃设备标记为非活跃状态
     * 用于应用启动时重置设备状态
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE LoginDevice d SET d.isActive = false WHERE d.isActive = true")
    int updateAllActiveDevicesToInactive();

    /**
     * 删除指定时间之前的非活跃设备记录
     * @param cutoffTime 截止时间
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM LoginDevice d WHERE d.isActive = false AND d.lastLoginAt < :cutoffTime")
    int deleteInactiveDevicesOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 将超过指定时间未更新的活跃设备标记为非活跃
     * @param staleTime 过期时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE LoginDevice d SET d.isActive = false WHERE d.isActive = true AND d.lastLoginAt < :staleTime")
    int updateStaleDevicesToInactive(@Param("staleTime") LocalDateTime staleTime);
}