package com.im.imcommunicationsystem.auth.repository;

import com.im.imcommunicationsystem.auth.entity.VerificationCode;
import com.im.imcommunicationsystem.auth.enums.VerificationCodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 验证码数据仓库
 * 验证码数据的持久化操作
 */
@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    /**
     * 查找有效验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @param codeType 验证码类型
     * @param currentTime 当前时间
     * @return 验证码信息
     */
    Optional<VerificationCode> findByEmailAndCodeAndCodeTypeAndIsUsedFalseAndExpiresAtAfter(
            String email, String code, VerificationCodeType codeType, LocalDateTime currentTime);

    /**
     * 删除指定邮箱和类型的验证码
     * @param email 邮箱地址
     * @param codeType 验证码类型
     */
    void deleteByEmailAndCodeType(String email, VerificationCodeType codeType);

    /**
     * 删除过期验证码
     * @param currentTime 当前时间
     */
    void deleteByExpiresAtBefore(LocalDateTime currentTime);

    /**
     * 根据邮箱和类型查找最新的验证码
     * @param email 邮箱地址
     * @param codeType 验证码类型
     * @return 验证码信息
     */
    Optional<VerificationCode> findTopByEmailAndCodeTypeOrderByCreatedAtDesc(String email, VerificationCodeType codeType);
}