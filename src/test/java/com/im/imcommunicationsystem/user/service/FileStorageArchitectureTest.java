package com.im.imcommunicationsystem.user.service;

import com.im.imcommunicationsystem.user.config.MinioConfig;
import com.im.imcommunicationsystem.user.entity.FileUpload;
import com.im.imcommunicationsystem.user.repository.FileUploadRepository;
import com.im.imcommunicationsystem.user.service.impl.PublicFileUploadServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件存储架构测试类
 * 测试公开文件和私有文件的分离存储功能
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FileStorageArchitectureTest {

    @Autowired
    private PublicFileUploadService publicFileUploadService;
    
    @Autowired
    private FileUploadService privateFileUploadService;
    
    @Autowired
    private MinioConfig minioConfig;
    
    @Autowired
    private FileUploadRepository fileUploadRepository;
    
    private static final Long TEST_USER_ID = 1L;
    
    @Test
    public void testBucketNameGeneration() {
        // 测试公开存储桶名称生成
        assertEquals("im-public-images", minioConfig.getBucketName("image", false));
        assertEquals("im-public-videos", minioConfig.getBucketName("video", false));
        assertEquals("im-public-audios", minioConfig.getBucketName("audio", false));
        assertEquals("im-public-documents", minioConfig.getBucketName("document", false));
        assertEquals("im-public-others", minioConfig.getBucketName("other", false));
        assertEquals("im-public", minioConfig.getBucketName(null, false));
        
        // 测试私有存储桶名称生成
        assertEquals("im-private-images", minioConfig.getBucketName("image", true));
        assertEquals("im-private-videos", minioConfig.getBucketName("video", true));
        assertEquals("im-private-audios", minioConfig.getBucketName("audio", true));
        assertEquals("im-private-documents", minioConfig.getBucketName("document", true));
        assertEquals("im-private-others", minioConfig.getBucketName("other", true));
        assertEquals("im-private", minioConfig.getBucketName(null, true));
        
        // 测试兼容性方法（默认为私有）
        assertEquals("im-private-images", minioConfig.getBucketName("image", true));
    }
    
    @Test
    public void testPublicFileUpload() {
        // 创建测试图片文件
        MockMultipartFile imageFile = new MockMultipartFile(
            "file", 
            "test-avatar.jpg", 
            "image/jpeg", 
            "test image content".getBytes()
        );
        
        // 上传公开文件
        String fileUrl = publicFileUploadService.uploadFile(imageFile, TEST_USER_ID);
        assertNotNull(fileUrl);
        
        // 验证文件记录
        List<FileUpload> publicFiles = publicFileUploadService.getUserPublicFiles(TEST_USER_ID, null);
        assertFalse(publicFiles.isEmpty());
        
        FileUpload uploadedFile = publicFiles.get(0);
        assertEquals(FileUpload.AccessLevel.PUBLIC, uploadedFile.getAccessLevel());
        assertTrue(uploadedFile.getIsPublic()); // 兼容性字段
        assertEquals(FileUpload.FileType.image, uploadedFile.getFileType());
        assertTrue(uploadedFile.getBucketName().startsWith("im-public"));
    }
    
    @Test
    public void testAvatarUpload() {
        // 创建测试头像文件
        MockMultipartFile avatarFile = new MockMultipartFile(
            "avatar", 
            "avatar.png", 
            "image/png", 
            "test avatar content".getBytes()
        );
        
        // 上传头像
        String avatarUrl = publicFileUploadService.uploadAvatar(avatarFile, TEST_USER_ID);
        assertNotNull(avatarUrl);
        
        // 验证头像是公开文件
        List<FileUpload> publicImages = publicFileUploadService.getUserPublicFiles(
            TEST_USER_ID, FileUpload.FileType.image
        );
        assertFalse(publicImages.isEmpty());
        
        FileUpload avatar = publicImages.get(0);
        assertEquals(FileUpload.AccessLevel.PUBLIC, avatar.getAccessLevel());
        assertTrue(avatar.getBucketName().contains("public"));
    }
    
    @Test
    public void testPrivateFileUpload() {
        // 创建测试文档文件
        MockMultipartFile documentFile = new MockMultipartFile(
            "document", 
            "private-doc.pdf", 
            "application/pdf", 
            "test document content".getBytes()
        );
        
        // 上传私有文件
        String fileUrl = privateFileUploadService.uploadFile(documentFile, "documents");
        assertNotNull(fileUrl);
        
        // 验证文件记录
        List<FileUpload> userFiles = fileUploadRepository.findByUserIdAndIsDeletedFalse(TEST_USER_ID);
        assertFalse(userFiles.isEmpty());
        
        FileUpload uploadedFile = userFiles.stream()
            .filter(f -> f.getOriginalName().equals("private-doc.pdf"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(uploadedFile);
        assertEquals(FileUpload.AccessLevel.PRIVATE, uploadedFile.getAccessLevel());
        assertFalse(uploadedFile.getIsPublic()); // 兼容性字段
        assertEquals(FileUpload.FileType.document, uploadedFile.getFileType());
        assertTrue(uploadedFile.getBucketName().startsWith("im-private"));
    }
    
    @Test
    public void testAccessLevelSeparation() {
        // 上传公开文件
        MockMultipartFile publicFile = new MockMultipartFile(
            "public", 
            "public-image.jpg", 
            "image/jpeg", 
            "public image content".getBytes()
        );
        publicFileUploadService.uploadFile(publicFile, TEST_USER_ID);
        
        // 上传私有文件
        MockMultipartFile privateFile = new MockMultipartFile(
            "private", 
            "private-doc.txt", 
            "text/plain", 
            "private document content".getBytes()
        );
        privateFileUploadService.uploadFile(privateFile, "documents");
        
        // 验证公开文件查询
        List<FileUpload> publicFiles = fileUploadRepository.findByUserIdAndAccessLevelAndIsDeletedFalse(
            TEST_USER_ID, FileUpload.AccessLevel.PUBLIC
        );
        assertEquals(1, publicFiles.size());
        assertEquals("public-image.jpg", publicFiles.get(0).getOriginalName());
        
        // 验证私有文件查询
        List<FileUpload> privateFiles = fileUploadRepository.findByUserIdAndAccessLevelAndIsDeletedFalse(
            TEST_USER_ID, FileUpload.AccessLevel.PRIVATE
        );
        assertEquals(1, privateFiles.size());
        assertEquals("private-doc.txt", privateFiles.get(0).getOriginalName());
    }
    
    @Test
    public void testPublicFileAccess() {
        // 上传公开文件
        MockMultipartFile publicFile = new MockMultipartFile(
            "public", 
            "public-image.jpg", 
            "image/jpeg", 
            "public image content".getBytes()
        );
        String fileUrl = publicFileUploadService.uploadFile(publicFile, TEST_USER_ID);
        
        // 获取文件ID
        List<FileUpload> publicFiles = publicFileUploadService.getUserPublicFiles(TEST_USER_ID, null);
        Long fileId = publicFiles.get(0).getId();
        
        // 测试公开文件访问
        assertTrue(publicFileUploadService.isPublicFile(fileId));
        
        FileUpload publicFileInfo = publicFileUploadService.getPublicFileById(fileId);
        assertNotNull(publicFileInfo);
        assertEquals(FileUpload.AccessLevel.PUBLIC, publicFileInfo.getAccessLevel());
        
        String publicFileUrl = publicFileUploadService.getPublicFileUrl(fileId);
        assertNotNull(publicFileUrl);
        assertEquals(fileUrl, publicFileUrl);
    }
    
    @Test
    public void testFileTypeBucketMapping() {
        // 测试不同文件类型的存储桶映射
        String[] fileTypes = {"image", "video", "audio", "document", "other"};
        
        for (String fileType : fileTypes) {
            // 公开存储桶
            String publicBucket = minioConfig.getBucketName(fileType, false);
            assertTrue(publicBucket.startsWith("im-public"));
            assertTrue(publicBucket.contains(fileType) || fileType.equals("other"));
            
            // 私有存储桶
            String privateBucket = minioConfig.getBucketName(fileType, true);
            assertTrue(privateBucket.startsWith("im-private"));
            assertTrue(privateBucket.contains(fileType) || fileType.equals("other"));
        }
    }
    
    @Test
    public void testFileUploadRecordFields() {
        // 上传公开文件
        MockMultipartFile publicFile = new MockMultipartFile(
            "public", 
            "test-file.jpg", 
            "image/jpeg", 
            "test content".getBytes()
        );
        publicFileUploadService.uploadFile(publicFile, TEST_USER_ID);
        
        // 验证文件记录字段
        List<FileUpload> files = fileUploadRepository.findByUserIdAndIsDeletedFalse(TEST_USER_ID);
        FileUpload file = files.get(0);
        
        // 验证新字段
        assertNotNull(file.getAccessLevel());
        assertEquals(FileUpload.AccessLevel.PUBLIC, file.getAccessLevel());
        
        // 验证兼容性字段
        assertNotNull(file.getIsPublic());
        assertTrue(file.getIsPublic());
        
        // 验证其他字段
        assertNotNull(file.getBucketName());
        assertNotNull(file.getObjectKey());
        assertNotNull(file.getFileType());
        assertNotNull(file.getStorageType());
        assertEquals(FileUpload.StorageType.minio, file.getStorageType());
    }
}