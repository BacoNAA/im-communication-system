package com.im.imcommunicationsystem.auth.repository;

import com.im.imcommunicationsystem.auth.entity.LoginDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}