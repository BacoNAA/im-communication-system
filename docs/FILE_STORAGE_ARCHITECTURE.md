# 文件存储架构说明

## 概述

本系统实现了公开文件和私有文件的分离存储架构，通过不同的存储桶（Bucket）来管理不同访问级别和类型的文件，提供差异化的文件服务。

## 存储桶架构

### 公开存储桶系列（im-public-*）

公开存储桶用于存储可以被任何人访问的文件，如用户头像、公开图片等。

- `im-public` - 公开文件默认存储桶
- `im-public-images` - 公开图片文件
- `im-public-videos` - 公开视频文件
- `im-public-audios` - 公开音频文件
- `im-public-documents` - 公开文档文件
- `im-public-others` - 其他类型的公开文件

### 私有存储桶系列（im-private-*）

私有存储桶用于存储只有文件所有者才能访问的文件，如个人文档、私人照片等。

- `im-private` - 私有文件默认存储桶
- `im-private-images` - 私有图片文件
- `im-private-videos` - 私有视频文件
- `im-private-audios` - 私有音频文件
- `im-private-documents` - 私有文档文件
- `im-private-others` - 其他类型的私有文件



## 访问级别

系统定义了三种文件访问级别：

### PUBLIC（公开访问）
- 任何人都可以访问
- 不需要身份验证
- 适用于头像、公开图片等
- 存储在 `im-public-*` 系列存储桶中

### PRIVATE（私有访问）
- 只有文件所有者可以访问
- 需要身份验证
- 适用于个人文档、私人照片等
- 存储在 `im-private-*` 系列存储桶中

### RESTRICTED（受限访问）
- 特定用户或角色可以访问
- 需要特殊权限验证
- 预留用于未来扩展

## 服务架构

### 公开文件服务

#### PublicFileUploadService
负责处理公开文件的上传、管理和访问：

- `uploadFile()` - 上传公开文件
- `uploadImage()` - 上传公开图片（支持压缩）
- `uploadAvatar()` - 上传用户头像
- `deleteFile()` - 软删除公开文件
- `physicalDeleteFile()` - 物理删除公开文件
- `getUserPublicFiles()` - 获取用户的公开文件列表
- `getPublicFileById()` - 根据ID获取公开文件信息
- `isPublicFile()` - 检查文件是否为公开文件
- `getPublicFileUrl()` - 获取公开文件的访问URL

#### PublicFileUploadController
提供公开文件的REST API接口：

- `POST /api/public/files/upload` - 上传公开文件
- `POST /api/public/files/upload-image` - 上传公开图片
- `POST /api/public/files/upload-avatar` - 上传头像
- `DELETE /api/public/files/{fileId}` - 删除公开文件
- `GET /api/public/files/user/{userId}` - 获取用户公开文件
- `GET /api/public/files/{fileId}` - 获取公开文件信息
- `GET /api/public/files/{fileId}/url` - 获取公开文件URL

### 私有文件服务

#### FileUploadService（现有服务）
继续负责处理私有文件的上传、管理和访问，现在专门处理私有文件。

#### FileUploadController（现有控制器）
继续提供私有文件的REST API接口。

## 数据库变更

### FileUpload实体变更

1. **新增字段**：
   - `accessLevel` - 文件访问级别枚举（PUBLIC/PRIVATE/RESTRICTED）

2. **废弃字段**：
   - `isPublic` - 布尔类型的公开标识（保留用于兼容性）

3. **数据迁移**：
   - 根据现有的 `isPublic` 字段值设置 `accessLevel`
   - 创建相关索引以提高查询性能
   - 创建视图以便于查询公开和私有文件

### 新增查询方法

在 `FileUploadRepository` 中新增了基于 `accessLevel` 的查询方法：

- `findByUserIdAndAccessLevel()` - 按用户ID和访问级别查询
- `findByUserIdAndFileTypeAndAccessLevel()` - 按用户ID、文件类型和访问级别查询
- `findByIdAndAccessLevel()` - 按文件ID和访问级别查询
- `existsByIdAndAccessLevel()` - 检查文件是否存在且符合访问级别
- `findByAccessLevelAndFileType()` - 按访问级别和文件类型分页查询

## 配置说明

### MinIO配置

在 `MinioConfig.java` 中配置了新的存储桶：

```java
// 公开存储桶配置
private String publicDefaultBucket = "im-public";
private String publicImageBucket = "im-public-images";
// ... 其他公开存储桶

// 私有存储桶配置
private String privateDefaultBucket = "im-private";
private String privateImageBucket = "im-private-images";
// ... 其他私有存储桶
```

### 存储桶策略

- **公开存储桶**：配置为允许匿名读取访问
- **私有存储桶**：配置为仅允许认证用户访问

## 使用示例

### 上传头像（公开文件）

```java
@Autowired
private PublicFileUploadService publicFileUploadService;

public void updateUserAvatar(Long userId, MultipartFile avatarFile) {
    FileUpload avatar = publicFileUploadService.uploadAvatar(avatarFile, userId);
    // 头像会自动存储到公开存储桶，任何人都可以访问
}
```

### 上传个人文档（私有文件）

```java
@Autowired
private FileUploadService fileUploadService;

public void uploadPersonalDocument(Long userId, MultipartFile document) {
    FileUpload file = fileUploadService.uploadFile(document, userId, "documents");
    // 文档会存储到私有存储桶，只有用户本人可以访问
}
```

### 获取文件URL

```java
// 公开文件 - 直接访问URL
String publicUrl = publicFileUploadService.getPublicFileUrl(fileId);

// 私有文件 - 需要签名URL
String privateUrl = fileUploadService.getSignedUrl(fileId, userId);
```

## 安全考虑

1. **访问控制**：
   - 公开文件可以被任何人访问，但只有文件所有者可以删除
   - 私有文件只有所有者可以访问和管理

2. **URL签名**：
   - 私有文件使用带有过期时间的签名URL
   - 公开文件使用永久性的直接访问URL

3. **权限验证**：
   - 所有文件操作都会验证用户权限
   - 防止越权访问其他用户的文件

## 性能优化

1. **存储桶分离**：
   - 不同类型的文件存储在不同的存储桶中
   - 便于实施不同的缓存和CDN策略

2. **索引优化**：
   - 为 `accessLevel` 字段创建了索引
   - 组合索引提高复合查询性能

3. **缓存策略**：
   - 公开文件启用长期缓存
   - 私有文件禁用缓存以保护隐私

## 迁移指南

### 从旧架构迁移

1. **运行数据库迁移脚本**：
   ```sql
   -- 执行 V20250110_001__add_access_level_to_file_uploads.sql
   ```

2. **更新应用配置**：
   - 确保新的存储桶配置正确
   - 验证MinIO连接和权限

3. **代码更新**：
   - 使用新的 `PublicFileUploadService` 处理公开文件
   - 继续使用现有的 `FileUploadService` 处理私有文件

4. **测试验证**：
   - 验证文件上传和访问功能
   - 确认权限控制正常工作

### 兼容性说明

- 现有的API接口保持不变
- 旧的存储桶继续可用（但建议迁移到新架构）
- `isPublic` 字段保留用于向后兼容

## 监控和维护

1. **存储使用监控**：
   - 监控各存储桶的使用情况
   - 设置存储配额和告警

2. **访问日志**：
   - 记录文件访问日志
   - 分析访问模式和性能

3. **定期清理**：
   - 清理已删除的文件
   - 归档长期未访问的文件

## 未来扩展

1. **CDN集成**：
   - 为公开文件配置CDN加速
   - 提高全球访问性能

2. **文件处理**：
   - 图片自动压缩和格式转换
   - 视频转码和缩略图生成

3. **高级权限控制**：
   - 实现 RESTRICTED 访问级别
   - 支持文件共享和协作

4. **备份和恢复**：
   - 自动备份重要文件
   - 提供文件恢复功能