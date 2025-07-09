package com.im.imcommunicationsystem.user.service;

import com.im.imcommunicationsystem.user.dto.response.QRCodeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 二维码服务接口
 * 实现二维码生成和解析的业务逻辑
 */
public interface UserQRCodeService {

    /**
     * 生成用户二维码
     * 
     * @param userId 用户ID
     * @return 二维码响应
     */
    QRCodeResponse generateUserQRCode(Long userId);

    /**
     * 解析用户二维码
     * 
     * @param file 二维码图片文件
     * @return 用户信息
     */
    Map<String, Object> parseUserQRCode(MultipartFile file);

    /**
     * 编码用户信息
     * 
     * @param userId 用户ID
     * @return 编码后的用户信息
     */
    String encodeUserInfo(Long userId);

    /**
     * 解码用户信息
     * 
     * @param qrCodeData 二维码数据
     * @return 解码后的用户信息
     */
    Map<String, Object> decodeUserInfo(String qrCodeData);
}