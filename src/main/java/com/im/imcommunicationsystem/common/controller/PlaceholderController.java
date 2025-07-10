package com.im.imcommunicationsystem.common.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * 占位符图片控制器
 * 提供动态生成的占位符图片
 */
@RestController
@RequestMapping("/api/placeholder")
public class PlaceholderController {

    /**
     * 生成占位符图片
     * @param width 图片宽度
     * @param height 图片高度
     * @return 占位符图片
     */
    @GetMapping("/{width}/{height}")
    public ResponseEntity<byte[]> generatePlaceholder(
            @PathVariable int width, 
            @PathVariable int height) {
        
        try {
            // 限制图片尺寸范围
            width = Math.max(1, Math.min(width, 1000));
            height = Math.max(1, Math.min(height, 1000));
            
            // 创建图片
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            // 设置抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 设置背景色（浅灰色）
            g2d.setColor(new Color(240, 240, 240));
            g2d.fillRect(0, 0, width, height);
            
            // 绘制边框
            g2d.setColor(new Color(200, 200, 200));
            g2d.drawRect(0, 0, width - 1, height - 1);
            
            // 绘制文字
            g2d.setColor(new Color(150, 150, 150));
            String text = width + "×" + height;
            Font font = new Font("Arial", Font.PLAIN, Math.min(width, height) / 8);
            g2d.setFont(font);
            
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            
            int x = (width - textWidth) / 2;
            int y = (height - textHeight) / 2 + fm.getAscent();
            
            g2d.drawString(text, x, y);
            
            // 绘制用户图标（简单的圆形头像占位符）
            if (width >= 50 && height >= 50) {
                int iconSize = Math.min(width, height) / 3;
                int iconX = (width - iconSize) / 2;
                int iconY = (height - iconSize) / 2 - iconSize / 4;
                
                // 绘制头部圆形
                g2d.setColor(new Color(180, 180, 180));
                g2d.fillOval(iconX + iconSize / 4, iconY, iconSize / 2, iconSize / 2);
                
                // 绘制身体部分
                g2d.fillOval(iconX, iconY + iconSize / 3, iconSize, iconSize * 2 / 3);
            }
            
            g2d.dispose();
            
            // 转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageBytes.length);
            headers.setCacheControl("public, max-age=3600"); // 缓存1小时
            
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 生成正方形占位符图片
     * @param size 图片尺寸
     * @return 占位符图片
     */
    @GetMapping("/{size}")
    public ResponseEntity<byte[]> generateSquarePlaceholder(@PathVariable int size) {
        return generatePlaceholder(size, size);
    }
}