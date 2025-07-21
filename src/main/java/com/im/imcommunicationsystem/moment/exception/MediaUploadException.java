package com.im.imcommunicationsystem.moment.exception;

/**
 * 媒体上传异常
 */
public class MediaUploadException extends RuntimeException {
    
    private String fileName;
    private Long fileSize;
    private String errorCode;
    
    public MediaUploadException(String fileName, Long fileSize, String errorCode) {
        super("媒体文件上传失败: " + fileName + ", 大小: " + fileSize + ", 错误码: " + errorCode);
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.errorCode = errorCode;
    }
    
    public MediaUploadException(String message) {
        super(message);
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
} 