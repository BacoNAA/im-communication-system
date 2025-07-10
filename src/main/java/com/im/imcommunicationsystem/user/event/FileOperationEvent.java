package com.im.imcommunicationsystem.user.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 文件操作事件
 * 用于触发实时数据一致性检查
 */
@Getter
public class FileOperationEvent extends ApplicationEvent {

    /**
     * 操作类型
     */
    public enum OperationType {
        UPLOAD,    // 文件上传
        DELETE,    // 软删除
        PHYSICAL_DELETE,  // 物理删除
        RESTORE    // 恢复文件
    }

    private final String fileId;
    private final Long userId;
    private final OperationType operationType;
    private final String objectKey;
    private final String fileName;
    private final long operationTimestamp;

    public FileOperationEvent(Object source, String fileId, Long userId, 
                             OperationType operationType, String objectKey, String fileName) {
        super(source);
        this.fileId = fileId;
        this.userId = userId;
        this.operationType = operationType;
        this.objectKey = objectKey;
        this.fileName = fileName;
        this.operationTimestamp = System.currentTimeMillis();
    }

    // toString() 方法由 Lombok @Getter 和父类提供
}