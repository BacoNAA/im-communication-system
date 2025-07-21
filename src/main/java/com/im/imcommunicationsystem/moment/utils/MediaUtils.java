package com.im.imcommunicationsystem.moment.utils;

import com.im.imcommunicationsystem.moment.exception.MediaUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 媒体工具类
 */
@Slf4j
public class MediaUtils {

    /**
     * 支持的图片格式
     */
    private static final List<String> SUPPORTED_IMAGE_FORMATS = Arrays.asList("image/jpeg", "image/png", "image/gif");
    
    /**
     * 支持的视频格式
     */
    private static final List<String> SUPPORTED_VIDEO_FORMATS = Arrays.asList("video/mp4", "video/mpeg", "video/quicktime");
    
    /**
     * 最大图片大小（10MB）
     */
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    
    /**
     * 最大视频大小（100MB）
     */
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024;
    
    /**
     * 压缩图片
     *
     * @param file 原始图片
     * @param quality 压缩质量（0-1）
     * @return 压缩后的图片字节数组
     * @throws IOException 如果压缩失败
     */
    public static byte[] compressImage(MultipartFile file, float quality) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("图片文件不能为空");
        }
        
        // 验证文件格式
        if (!isImageFile(file)) {
            throw new MediaUploadException("不支持的图片格式: " + file.getContentType(), file.getSize(), "UNSUPPORTED_FORMAT");
        }
        
        // 如果小于1MB，不压缩
        if (file.getSize() < 1024 * 1024) {
            return file.getBytes();
        }
        
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        if (originalImage == null) {
            throw new IOException("无法读取图片数据");
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // 获取图片格式
        String format = getImageFormat(file);
        
        // 压缩并写入输出流
        try {
            if ("png".equals(format)) {
                // PNG不支持压缩质量，只能通过降低尺寸压缩
                BufferedImage compressedImage = resizeImage(originalImage, originalImage.getWidth() / 2, originalImage.getHeight() / 2);
                ImageIO.write(compressedImage, format, outputStream);
            } else {
                // JPEG和其他格式支持质量压缩
                ImageIO.write(originalImage, format, outputStream);
            }
        } catch (IOException e) {
            log.error("压缩图片失败: {}", e.getMessage());
            throw new MediaUploadException("压缩图片失败", file.getSize(), "COMPRESSION_FAILED");
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * 生成唯一文件名
     *
     * @param originalFilename 原始文件名
     * @return 唯一文件名
     */
    public static String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }
    
    /**
     * 验证媒体文件格式和大小
     *
     * @param file 媒体文件
     * @return 是否验证通过
     */
    public static boolean validateMediaFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        long size = file.getSize();
        
        // 验证图片
        if (isImageFile(file)) {
            return size <= MAX_IMAGE_SIZE;
        }
        
        // 验证视频
        if (isVideoFile(file)) {
            return size <= MAX_VIDEO_SIZE;
        }
        
        return false;
    }
    
    /**
     * 判断是否为图片文件
     *
     * @param file 媒体文件
     * @return 是否为图片
     */
    public static boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && SUPPORTED_IMAGE_FORMATS.contains(contentType.toLowerCase());
    }
    
    /**
     * 判断是否为视频文件
     *
     * @param file 媒体文件
     * @return 是否为视频
     */
    public static boolean isVideoFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && SUPPORTED_VIDEO_FORMATS.contains(contentType.toLowerCase());
    }
    
    /**
     * 调整图片大小
     *
     * @param originalImage 原始图片
     * @param width 目标宽度
     * @param height 目标高度
     * @return 调整后的图片
     */
    private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }
    
    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名
     */
    private static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty() || !filename.contains(".")) {
            return "tmp";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 获取图片格式
     *
     * @param file 图片文件
     * @return 图片格式
     */
    private static String getImageFormat(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                return "jpeg";
            } else if (contentType.contains("png")) {
                return "png";
            } else if (contentType.contains("gif")) {
                return "gif";
            }
        }
        // 默认jpeg格式
        return "jpeg";
    }
} 