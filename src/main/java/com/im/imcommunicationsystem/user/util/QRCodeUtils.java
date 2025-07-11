package com.im.imcommunicationsystem.user.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * 二维码工具类
 * 提供二维码生成和解析功能
 */
@Slf4j
public class QRCodeUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成二维码图片
     * 
     * @param content 二维码内容
     * @param width 宽度
     * @param height 高度
     * @return 二维码图片
     */
    public static BufferedImage generateQRCode(String content, int width, int height) {
        try {
            // 设置二维码参数
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);

            // 生成二维码矩阵
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            // 转换为BufferedImage
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            log.error("生成二维码失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }

    /**
     * 生成二维码Base64字符串
     * 
     * @param content 二维码内容
     * @param width 宽度
     * @param height 高度
     * @return Base64字符串
     */
    public static String generateQRCodeBase64(String content, int width, int height) {
        BufferedImage image = generateQRCode(content, width, height);
        return imageToBase64(image, "PNG");
    }

    /**
     * 解析二维码图片
     * 
     * @param image 二维码图片
     * @return 解析结果
     */
    public static String parseQRCode(BufferedImage image) {
        try {
            BinaryBitmap binaryBitmap = new BinaryBitmap(
                new HybridBinarizer(
                    new BufferedImageLuminanceSource(image)
                )
            );
            
            Hashtable<DecodeHintType, Object> hints = new Hashtable<>();
            hints.put(DecodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            
            Result result = new MultiFormatReader().decode(binaryBitmap, hints);
            return result.getText();
        } catch (Exception e) {
            log.error("解析二维码失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析二维码失败", e);
        }
    }

    /**
     * 解析上传的二维码文件
     * 
     * @param file 二维码文件
     * @return 解析结果
     */
    public static String parseQRCodeFile(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            return parseQRCode(image);
        } catch (IOException e) {
            log.error("读取二维码图片失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取二维码图片失败", e);
        }
    }

    /**
     * 验证二维码内容格式
     * 
     * @param content 二维码内容
     * @return 是否有效
     */
    public static boolean validateQRCodeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否是用户信息二维码
        if (content.startsWith("IM_USER:")) {
            try {
                decodeUserInfo(content);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        // 其他格式的二维码也认为是有效的
        return true;
    }

    /**
     * 编码用户信息为二维码内容
     * 
     * @param userInfo 用户信息
     * @return 编码后的内容
     */
    public static String encodeUserInfo(Map<String, Object> userInfo) {
        try {
            String jsonString = objectMapper.writeValueAsString(userInfo);
            String encodedUserInfo = Base64.getEncoder().encodeToString(jsonString.getBytes(StandardCharsets.UTF_8));
            return "IM_USER:" + encodedUserInfo;
        } catch (JsonProcessingException e) {
            log.error("编码用户信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("编码用户信息失败", e);
        }
    }

    /**
     * 解码二维码内容为用户信息
     * 
     * @param qrCodeContent 二维码内容
     * @return 用户信息
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> decodeUserInfo(String qrCodeContent) {
        try {
            // 检查是否是用户信息二维码
            if (!qrCodeContent.startsWith("IM_USER:")) {
                throw new IllegalArgumentException("不是有效的用户信息二维码");
            }
            
            // 提取Base64编码的用户信息
            String encodedUserInfo = qrCodeContent.substring(8); // 去掉"IM_USER:"前缀
            byte[] decodedBytes = Base64.getDecoder().decode(encodedUserInfo);
            String jsonString = new String(decodedBytes, StandardCharsets.UTF_8);
            
            return objectMapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
            log.error("解码用户信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("解码用户信息失败", e);
        }
    }

    /**
     * 将BufferedImage转换为Base64字符串
     * 
     * @param image 图片
     * @param format 图片格式
     * @return Base64字符串
     */
    public static String imageToBase64(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, format, baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/" + format.toLowerCase() + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            log.error("图片转Base64失败: {}", e.getMessage(), e);
            throw new RuntimeException("图片转Base64失败", e);
        }
    }
}