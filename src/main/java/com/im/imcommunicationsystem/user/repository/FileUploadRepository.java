package com.im.imcommunicationsystem.user.repository;

import com.im.imcommunicationsystem.user.entity.FileUpload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件上传记录Repository接口
 * 提供文件上传记录的数据访问操作
 */
@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    /**
     * 根据用户ID查询文件列表（未删除）
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.userId = :userId AND f.isDeleted = false ORDER BY f.createdAt DESC")
    Page<FileUpload> findByUserIdAndIsDeletedFalse(@Param("userId") Long userId, Pageable pageable);

    /**
     * 根据用户ID查询所有文件列表（未删除，不分页）
     *
     * @param userId 用户ID
     * @return 文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.userId = :userId AND f.isDeleted = false ORDER BY f.createdAt DESC")
    List<FileUpload> findByUserIdAndIsDeletedFalse(@Param("userId") Long userId);

    /**
     * 根据用户ID和文件类型查询文件列表（未删除）
     *
     * @param userId 用户ID
     * @param fileType 文件类型
     * @param pageable 分页参数
     * @return 文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.userId = :userId AND f.fileType = :fileType AND f.isDeleted = false ORDER BY f.createdAt DESC")
    Page<FileUpload> findByUserIdAndFileTypeAndIsDeletedFalse(@Param("userId") Long userId, 
                                                              @Param("fileType") FileUpload.FileType fileType, 
                                                              Pageable pageable);

    /**
     * 根据ID查询文件（未删除）
     *
     * @param id 文件ID
     * @return 文件记录
     */
    Optional<FileUpload> findByIdAndIsDeletedFalse(Long id);

    /**
     * 根据MD5哈希值查询文件（用于去重）
     *
     * @param md5Hash MD5哈希值
     * @return 文件记录
     */
    Optional<FileUpload> findByMd5HashAndIsDeletedFalse(String md5Hash);

    /**
     * 根据文件名查询文件
     *
     * @param fileName 文件名
     * @return 文件记录
     */
    Optional<FileUpload> findByFileNameAndIsDeletedFalse(String fileName);

    /**
     * 根据对象存储键查询文件
     *
     * @param objectKey 对象存储键
     * @return 文件记录
     */
    Optional<FileUpload> findByObjectKeyAndIsDeletedFalse(String objectKey);

    /**
     * 统计用户上传的文件总数（未删除）
     *
     * @param userId 用户ID
     * @return 文件总数
     */
    @Query("SELECT COUNT(f) FROM FileUpload f WHERE f.userId = :userId AND f.isDeleted = false")
    Long countByUserIdAndIsDeletedFalse(@Param("userId") Long userId);

    /**
     * 统计用户上传的文件总大小（未删除）
     *
     * @param userId 用户ID
     * @return 文件总大小（字节）
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileUpload f WHERE f.userId = :userId AND f.isDeleted = false")
    Long sumFileSizeByUserIdAndIsDeletedFalse(@Param("userId") Long userId);

    /**
     * 根据文件类型统计用户上传的文件数量（未删除）
     *
     * @param userId 用户ID
     * @param fileType 文件类型
     * @return 文件数量
     */
    @Query("SELECT COUNT(f) FROM FileUpload f WHERE f.userId = :userId AND f.fileType = :fileType AND f.isDeleted = false")
    Long countByUserIdAndFileTypeAndIsDeletedFalse(@Param("userId") Long userId, 
                                                   @Param("fileType") FileUpload.FileType fileType);

    /**
     * 查询指定时间范围内上传的文件
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.userId = :userId AND f.createdAt BETWEEN :startTime AND :endTime AND f.isDeleted = false ORDER BY f.createdAt DESC")
    Page<FileUpload> findByUserIdAndCreatedAtBetweenAndIsDeletedFalse(@Param("userId") Long userId,
                                                                      @Param("startTime") LocalDateTime startTime,
                                                                      @Param("endTime") LocalDateTime endTime,
                                                                      Pageable pageable);

    /**
     * 根据文件名模糊搜索用户的文件
     *
     * @param userId 用户ID
     * @param fileName 文件名关键词
     * @param pageable 分页参数
     * @return 文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.userId = :userId AND (f.originalName LIKE %:fileName% OR f.fileName LIKE %:fileName%) AND f.isDeleted = false ORDER BY f.createdAt DESC")
    Page<FileUpload> findByUserIdAndFileNameContainingAndIsDeletedFalse(@Param("userId") Long userId,
                                                                        @Param("fileName") String fileName,
                                                                        Pageable pageable);

    /**
     * 软删除文件（标记为已删除）
     *
     * @param id 文件ID
     * @param userId 用户ID（确保只能删除自己的文件）
     * @param deletedAt 删除时间
     * @return 影响的行数
     */
    @Modifying
    @Query("UPDATE FileUpload f SET f.isDeleted = true, f.deletedAt = :deletedAt WHERE f.id = :id AND f.userId = :userId AND f.isDeleted = false")
    int softDeleteByIdAndUserId(@Param("id") Long id, 
                               @Param("userId") Long userId, 
                               @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 批量软删除文件
     *
     * @param ids 文件ID列表
     * @param userId 用户ID
     * @param deletedAt 删除时间
     * @return 影响的行数
     */
    @Modifying
    @Query("UPDATE FileUpload f SET f.isDeleted = true, f.deletedAt = :deletedAt WHERE f.id IN :ids AND f.userId = :userId AND f.isDeleted = false")
    int softDeleteByIdsAndUserId(@Param("ids") List<Long> ids, 
                                @Param("userId") Long userId, 
                                @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 查询已删除的文件（回收站）
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 已删除的文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.userId = :userId AND f.isDeleted = true ORDER BY f.deletedAt DESC")
    Page<FileUpload> findDeletedFilesByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 恢复已删除的文件
     *
     * @param id 文件ID
     * @param userId 用户ID
     * @return 影响的行数
     */
    @Modifying
    @Query("UPDATE FileUpload f SET f.isDeleted = false, f.deletedAt = null WHERE f.id = :id AND f.userId = :userId AND f.isDeleted = true")
    int restoreFileByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 物理删除指定时间之前的已删除文件
     *
     * @param beforeTime 指定时间
     * @return 删除的文件数量
     */
    @Modifying
    @Query("DELETE FROM FileUpload f WHERE f.isDeleted = true AND f.deletedAt < :beforeTime")
    int physicalDeleteExpiredFiles(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 根据文件ID和用户ID查询文件（未删除）
     *
     * @param id 文件ID
     * @param userId 用户ID
     * @return 文件信息
     */
    Optional<FileUpload> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);
    
    /**
     * 根据用户ID和访问级别查询文件列表（未删除）
     *
     * @param userId 用户ID
     * @param accessLevel 访问级别
     * @return 文件列表
     */
    List<FileUpload> findByUserIdAndAccessLevelAndIsDeletedFalse(Long userId, FileUpload.AccessLevel accessLevel);
    
    /**
     * 根据用户ID、文件类型和访问级别查询文件列表（未删除）
     *
     * @param userId 用户ID
     * @param fileType 文件类型
     * @param accessLevel 访问级别
     * @return 文件列表
     */
    List<FileUpload> findByUserIdAndFileTypeAndAccessLevelAndIsDeletedFalse(
        Long userId, FileUpload.FileType fileType, FileUpload.AccessLevel accessLevel);
    
    /**
     * 根据文件ID和访问级别查询文件（未删除）
     *
     * @param id 文件ID
     * @param accessLevel 访问级别
     * @return 文件信息
     */
    Optional<FileUpload> findByIdAndAccessLevelAndIsDeletedFalse(Long id, FileUpload.AccessLevel accessLevel);
    
    /**
     * 检查文件是否存在且符合访问级别（未删除）
     *
     * @param id 文件ID
     * @param accessLevel 访问级别
     * @return 是否存在
     */
    boolean existsByIdAndAccessLevelAndIsDeletedFalse(Long id, FileUpload.AccessLevel accessLevel);
    
    /**
     * 根据访问级别查询文件列表（未删除）
     *
     * @param accessLevel 访问级别
     * @param pageable 分页参数
     * @return 文件列表
     */
    Page<FileUpload> findByAccessLevelAndIsDeletedFalse(FileUpload.AccessLevel accessLevel, Pageable pageable);
    
    /**
     * 根据文件类型和访问级别查询文件列表（未删除）
     *
     * @param fileType 文件类型
     * @param accessLevel 访问级别
     * @param pageable 分页参数
     * @return 文件列表
     */
    Page<FileUpload> findByFileTypeAndAccessLevelAndIsDeletedFalse(
        FileUpload.FileType fileType, FileUpload.AccessLevel accessLevel, Pageable pageable);
    
    /**
     * 根据用户ID和文件标签查询文件列表（未删除）
     *
     * @param userId 用户ID
     * @param fileTag 文件标签
     * @return 文件列表
     */
    List<FileUpload> findByUserIdAndFileTagAndIsDeletedFalse(Long userId, FileUpload.FileTag fileTag);
    
    /**
     * 根据用户ID、文件类型和文件标签查询文件列表（未删除）
     *
     * @param userId 用户ID
     * @param fileType 文件类型
     * @param fileTag 文件标签
     * @return 文件列表
     */
    List<FileUpload> findByUserIdAndFileTypeAndFileTagAndIsDeletedFalse(
        Long userId, FileUpload.FileType fileType, FileUpload.FileTag fileTag);
    
    /**
     * 查询过期的临时文件（未删除）
     *
     * @param currentTime 当前时间
     * @return 过期的临时文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.fileTag = 'TEMPORARY' AND f.expiresAt < :currentTime AND f.isDeleted = false")
    List<FileUpload> findExpiredTemporaryFiles(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 查询指定桶中过期的临时文件（未删除）
     *
     * @param bucketName 桶名称
     * @param currentTime 当前时间
     * @return 过期的临时文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.bucketName = :bucketName AND f.fileTag = 'TEMPORARY' AND f.expiresAt < :currentTime AND f.isDeleted = false")
    List<FileUpload> findExpiredTemporaryFilesByBucket(@Param("bucketName") String bucketName, @Param("currentTime") LocalDateTime currentTime);

    // ==================== 消息模块特有查询方法 ====================

    /**
     * 根据会话ID查询媒体文件列表（未删除）
     *
     * @param conversationId 会话ID
     * @param pageable 分页参数
     * @return 文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.conversationId = :conversationId AND f.isDeleted = false ORDER BY f.createdAt DESC")
    Page<FileUpload> findByConversationIdAndIsDeletedFalse(@Param("conversationId") Long conversationId, Pageable pageable);

    /**
     * 根据消息ID查询媒体文件（未删除）
     *
     * @param messageId 消息ID
     * @return 文件记录
     */
    Optional<FileUpload> findByMessageIdAndIsDeletedFalse(Long messageId);

    /**
     * 根据上传者ID查询媒体文件列表（未删除）
     *
     * @param uploaderId 上传者ID
     * @param pageable 分页参数
     * @return 文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.userId = :uploaderId AND f.conversationId IS NOT NULL AND f.isDeleted = false ORDER BY f.createdAt DESC")
    Page<FileUpload> findMediaFilesByUploaderIdAndIsDeletedFalse(@Param("uploaderId") Long uploaderId, Pageable pageable);

    /**
     * 根据会话ID和文件类型查询媒体文件列表（未删除）
     *
     * @param conversationId 会话ID
     * @param fileType 文件类型
     * @param pageable 分页参数
     * @return 文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.conversationId = :conversationId AND f.fileType = :fileType AND f.isDeleted = false ORDER BY f.createdAt DESC")
    Page<FileUpload> findByConversationIdAndFileTypeAndIsDeletedFalse(@Param("conversationId") Long conversationId, 
                                                                      @Param("fileType") FileUpload.FileType fileType, 
                                                                      Pageable pageable);

    /**
     * 统计会话中的媒体文件数量（未删除）
     *
     * @param conversationId 会话ID
     * @return 文件数量
     */
    @Query("SELECT COUNT(f) FROM FileUpload f WHERE f.conversationId = :conversationId AND f.isDeleted = false")
    Long countByConversationIdAndIsDeletedFalse(@Param("conversationId") Long conversationId);

    /**
     * 统计会话中指定类型的媒体文件数量（未删除）
     *
     * @param conversationId 会话ID
     * @param fileType 文件类型
     * @return 文件数量
     */
    @Query("SELECT COUNT(f) FROM FileUpload f WHERE f.conversationId = :conversationId AND f.fileType = :fileType AND f.isDeleted = false")
    Long countByConversationIdAndFileTypeAndIsDeletedFalse(@Param("conversationId") Long conversationId, 
                                                           @Param("fileType") FileUpload.FileType fileType);

    /**
     * 更新文件的会话和消息关联
     *
     * @param fileId 文件ID
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @return 影响的行数
     */
    @Modifying
    @Query("UPDATE FileUpload f SET f.conversationId = :conversationId, f.messageId = :messageId WHERE f.id = :fileId AND f.isDeleted = false")
    int updateFileAssociation(@Param("fileId") Long fileId, 
                              @Param("conversationId") Long conversationId, 
                              @Param("messageId") Long messageId);

    /**
     * 软删除文件（不需要用户ID验证，用于系统级删除）
     *
     * @param fileId 文件ID
     * @param deletedAt 删除时间
     * @return 影响的行数
     */
    @Modifying
    @Query("UPDATE FileUpload f SET f.isDeleted = true, f.deletedAt = :deletedAt WHERE f.id = :fileId AND f.isDeleted = false")
    int softDeleteById(@Param("fileId") Long fileId, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 查询会话中的所有媒体文件（不分页，用于导出等场景）
     *
     * @param conversationId 会话ID
     * @return 文件列表
     */
    @Query("SELECT f FROM FileUpload f WHERE f.conversationId = :conversationId AND f.isDeleted = false ORDER BY f.createdAt ASC")
    List<FileUpload> findAllByConversationIdAndIsDeletedFalse(@Param("conversationId") Long conversationId);
}