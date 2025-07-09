package com.im.imcommunicationsystem.user.exception;

/**
 * 文件上传异常
 */
public class FileUploadException extends RuntimeException {

    private final String fileName;
    private final String fileType;
    private final long fileSize;

    public FileUploadException(String message) {
        super(message);
        this.fileName = null;
        this.fileType = null;
        this.fileSize = 0;
    }

    public FileUploadException(String message, String fileName) {
        super(message);
        this.fileName = fileName;
        this.fileType = null;
        this.fileSize = 0;
    }

    public FileUploadException(String message, String fileName, String fileType) {
        super(message);
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = 0;
    }

    public FileUploadException(String message, String fileName, String fileType, long fileSize) {
        super(message);
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
        this.fileName = null;
        this.fileType = null;
        this.fileSize = 0;
    }

    public FileUploadException(String message, Throwable cause, String fileName) {
        super(message, cause);
        this.fileName = fileName;
        this.fileType = null;
        this.fileSize = 0;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    /**
     * 创建文件大小超限异常
     */
    public static FileUploadException fileSizeExceeded(String fileName, long fileSize, long maxSize) {
        return new FileUploadException(
            String.format("File size exceeded. File: %s, Size: %d bytes, Max allowed: %d bytes", 
                fileName, fileSize, maxSize),
            fileName, null, fileSize
        );
    }

    /**
     * 创建文件类型不支持异常
     */
    public static FileUploadException unsupportedFileType(String fileName, String fileType) {
        return new FileUploadException(
            String.format("Unsupported file type. File: %s, Type: %s", fileName, fileType),
            fileName, fileType
        );
    }

    /**
     * 创建文件保存失败异常
     */
    public static FileUploadException saveFailed(String fileName, Throwable cause) {
        return new FileUploadException(
            String.format("Failed to save file: %s", fileName),
            cause, fileName
        );
    }
}