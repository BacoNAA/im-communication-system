package com.im.imcommunicationsystem.user.exception;

/**
 * 二维码处理异常
 */
public class QRCodeException extends RuntimeException {

    private final String operation;
    private final String qrCodeData;

    public QRCodeException(String message) {
        super(message);
        this.operation = null;
        this.qrCodeData = null;
    }

    public QRCodeException(String message, String operation) {
        super(message);
        this.operation = operation;
        this.qrCodeData = null;
    }

    public QRCodeException(String message, String operation, String qrCodeData) {
        super(message);
        this.operation = operation;
        this.qrCodeData = qrCodeData;
    }

    public QRCodeException(String message, Throwable cause) {
        super(message, cause);
        this.operation = null;
        this.qrCodeData = null;
    }

    public QRCodeException(String message, Throwable cause, String operation) {
        super(message, cause);
        this.operation = operation;
        this.qrCodeData = null;
    }

    public String getOperation() {
        return operation;
    }

    public String getQrCodeData() {
        return qrCodeData;
    }

    /**
     * 创建二维码生成失败异常
     */
    public static QRCodeException generationFailed(String content, Throwable cause) {
        return new QRCodeException(
            String.format("Failed to generate QR code for content: %s", content),
            cause, "GENERATION"
        );
    }

    /**
     * 创建二维码解析失败异常
     */
    public static QRCodeException parseFailed(Throwable cause) {
        return new QRCodeException(
            "Failed to parse QR code from image",
            cause, "PARSING"
        );
    }

    /**
     * 创建二维码内容无效异常
     */
    public static QRCodeException invalidContent(String content) {
        return new QRCodeException(
            String.format("Invalid QR code content format: %s", content),
            "VALIDATION", content
        );
    }

    /**
     * 创建二维码已过期异常
     */
    public static QRCodeException expired(String content) {
        return new QRCodeException(
            "QR code has expired",
            "VALIDATION", content
        );
    }

    /**
     * 创建图片格式不支持异常
     */
    public static QRCodeException unsupportedImageFormat(String format) {
        return new QRCodeException(
            String.format("Unsupported image format for QR code parsing: %s", format),
            "PARSING"
        );
    }
}