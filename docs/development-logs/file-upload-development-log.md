# 文件上传功能开发日志

## 项目信息
- **项目名称**: IM通信系统
- **模块**: 文件上传与存储管理
- **开发时间**: 2024年1月 - 2025年1月
- **最后更新**: 2025年1月12日

## 开发概述

文件上传功能是IM通信系统的核心模块之一，支持用户上传各种类型的文件，包括图片、视频、音频、文档等。系统采用MinIO作为对象存储服务，实现了公开文件和私有文件的分离存储架构。

## 功能特性

### 核心功能
- ✅ 多文件类型支持（图片、视频、音频、文档）
- ✅ 文件大小限制和类型验证
- ✅ 图片自动压缩和缩略图生成
- ✅ MD5哈希值计算和重复文件检测
- ✅ 文件访问权限控制（公开/私有/受限）
- ✅ 文件生命周期管理（临时/永久文件）
- ✅ 软删除和物理删除
- ✅ 文件分享功能
- ✅ 下载统计和访问日志

### 存储架构
- ✅ MinIO对象存储集成
- ✅ 多存储桶分离架构
- ✅ 公开存储桶系列（im-public-*）
- ✅ 私有存储桶系列（im-private-*）
- ✅ 存储桶策略配置
- ✅ 文件URL签名机制

### 安全特性
- ✅ 用户身份验证
- ✅ 文件访问权限验证
- ✅ 防止越权访问
- ✅ 文件类型白名单
- ✅ 文件大小限制
- ✅ XSS和恶意文件防护

## 技术架构

### 技术栈
- **后端框架**: Spring Boot 3.x
- **数据库**: MySQL 8.0
- **对象存储**: MinIO
- **文件处理**: ImageIO, FFmpeg（视频处理）
- **数据库迁移**: Flyway
- **缓存**: Redis（计划中）

### 核心组件

#### 1. 实体类
- `FileUpload.java` - 文件上传记录实体
- `FileShare.java` - 文件分享记录实体
- `FileDownload.java` - 文件下载记录实体

#### 2. 服务层
- `FileUploadService` - 私有文件上传服务接口
- `MinioFileUploadServiceImpl` - MinIO文件上传服务实现
- `PublicFileUploadService` - 公开文件上传服务接口
- `PublicFileUploadServiceImpl` - 公开文件上传服务实现
- `MinioService` - MinIO基础服务
- `MinioLifecycleService` - MinIO生命周期管理服务

#### 3. 控制器层
- `FileUploadController` - 私有文件上传API
- `PublicFileUploadController` - 公开文件上传API

#### 4. 配置类
- `MinioConfig` - MinIO配置
- `FileUploadConfig` - 文件上传配置
- `GlobalConfig.FileUpload` - 全局文件上传配置

#### 5. 工具类
- `FileUtils` - 文件处理工具类
- `SecurityUtils` - 安全工具类

## 数据库设计

### 主要表结构

#### file_uploads 表
```sql
CREATE TABLE `file_uploads` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件唯一标识',
    `user_id` BIGINT NOT NULL COMMENT '上传用户ID',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_url` VARCHAR(500) NOT NULL COMMENT '文件访问URL',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `content_type` VARCHAR(100) NOT NULL COMMENT '文件MIME类型',
    `file_type` ENUM('image', 'video', 'audio', 'document', 'other') NOT NULL DEFAULT 'other',
    `md5_hash` VARCHAR(32) NULL COMMENT '文件MD5哈希值',
    `storage_type` ENUM('local', 'minio', 's3', 'oss') NOT NULL DEFAULT 'minio',
    `bucket_name` VARCHAR(100) NULL COMMENT '存储桶名称',
    `object_key` VARCHAR(500) NULL COMMENT '对象存储键',
    `width` INT NULL COMMENT '图片/视频宽度',
    `height` INT NULL COMMENT '图片/视频高度',
    `duration` INT NULL COMMENT '音频/视频时长（秒）',
    `thumbnail_url` VARCHAR(500) NULL COMMENT '缩略图URL',
    `access_level` VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' COMMENT '访问级别',
    `file_tag` VARCHAR(20) NOT NULL DEFAULT 'TEMPORARY' COMMENT '文件标签',
    `expires_at` DATETIME NULL COMMENT '文件过期时间',
    `is_public` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否公开访问（已废弃）',
    `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已删除',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` TIMESTAMP NULL COMMENT '删除时间',
    PRIMARY KEY (`id`)
);
```

#### 数据库迁移历史
1. **V2__Create_file_upload_tables.sql** - 创建基础文件上传表
2. **V20250110_001__add_access_level_to_file_uploads.sql** - 添加访问级别字段
3. **V20250112_001__add_file_tag_and_expires_at.sql** - 添加文件标签和过期时间字段

## API接口设计

### 私有文件API

#### 上传文件
```http
POST /api/files/upload
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (必需)
- directory: String (可选，默认为"files")
```

#### 上传图片
```http
POST /api/files/upload-image
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (必需)
- compress: Boolean (可选，是否压缩)
```

#### 获取文件列表
```http
GET /api/files/list

Parameters:
- page: Integer (页码，默认0)
- size: Integer (每页大小，默认10)
- fileType: String (文件类型过滤)
```

### 公开文件API

#### 上传公开文件
```http
POST /api/public/files/upload
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (必需)
```

#### 上传头像
```http
POST /api/public/files/upload-avatar
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (必需)
```

## 开发历程

### 第一阶段：基础功能实现（2024年1月）
- [x] 创建基础的文件上传实体和表结构
- [x] 实现本地文件存储功能
- [x] 添加基本的文件类型验证
- [x] 实现文件上传API接口

### 第二阶段：MinIO集成（2024年2月）
- [x] 集成MinIO对象存储服务
- [x] 实现MinIO配置和连接管理
- [x] 迁移文件存储到MinIO
- [x] 添加存储桶管理功能

### 第三阶段：功能增强（2024年3-6月）
- [x] 添加图片压缩和缩略图生成
- [x] 实现MD5哈希值计算
- [x] 添加文件重复检测
- [x] 实现软删除功能
- [x] 添加文件分享功能

### 第四阶段：架构优化（2024年7-12月）
- [x] 实现公开/私有文件分离架构
- [x] 添加多存储桶支持
- [x] 实现文件访问权限控制
- [x] 优化数据库查询性能
- [x] 添加文件下载统计

### 第五阶段：生命周期管理（2025年1月）
- [x] 添加文件生命周期管理
- [x] 实现临时文件和永久文件区分
- [x] 添加文件过期时间支持
- [x] 实现定时清理任务
- [x] 完善MinIO生命周期规则

## 配置说明

### MinIO配置
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  public-default-bucket: im-public
  private-default-bucket: im-private
  public-image-bucket: im-public-images
  private-image-bucket: im-private-images
  # ... 其他存储桶配置
```

### 文件上传配置
```yaml
app:
  file-upload:
    upload-path: /uploads
    base-url: http://localhost:8080
    max-file-size: 104857600  # 100MB
    max-request-size: 524288000  # 500MB
    allowed-image-types:
      - image/jpeg
      - image/png
      - image/gif
      - image/webp
    image-compression:
      enabled: true
      quality: 0.8
      max-width: 1920
      max-height: 1080
```

## 性能优化

### 已实现的优化
1. **数据库索引优化**
   - 为常用查询字段添加索引
   - 创建复合索引提高查询性能

2. **文件处理优化**
   - 图片压缩减少存储空间
   - 缩略图生成提高加载速度
   - MD5哈希值避免重复存储

3. **存储桶分离**
   - 按文件类型分离存储桶
   - 便于实施不同的缓存策略

### 计划中的优化
1. **CDN集成**
   - 为公开文件配置CDN加速
   - 提高全球访问性能

2. **缓存机制**
   - Redis缓存文件元数据
   - 减少数据库查询压力

3. **异步处理**
   - 文件上传异步处理
   - 大文件分片上传

## 安全措施

### 已实现的安全措施
1. **文件类型验证**
   - 白名单机制限制允许的文件类型
   - MIME类型检查防止伪造

2. **文件大小限制**
   - 全局文件大小限制
   - 按文件类型设置不同限制

3. **访问权限控制**
   - 用户身份验证
   - 文件所有权验证
   - 防止越权访问

4. **URL签名**
   - 私有文件使用签名URL
   - 设置URL过期时间

### 安全最佳实践
1. **输入验证**
   - 严格验证文件名和路径
   - 防止路径遍历攻击

2. **病毒扫描**
   - 计划集成病毒扫描服务
   - 检测恶意文件

3. **审计日志**
   - 记录所有文件操作
   - 便于安全审计

## 测试策略

### 单元测试
- [x] 文件上传服务测试
- [x] 文件验证逻辑测试
- [x] MinIO服务集成测试
- [x] 权限控制测试

### 集成测试
- [x] API接口测试
- [x] 数据库操作测试
- [x] 文件存储测试
- [x] 端到端功能测试

### 性能测试
- [ ] 大文件上传测试
- [ ] 并发上传测试
- [ ] 存储性能测试
- [ ] 网络传输测试

## 监控和运维

### 监控指标
1. **业务指标**
   - 文件上传成功率
   - 文件下载次数
   - 存储空间使用情况
   - 用户活跃度

2. **技术指标**
   - API响应时间
   - 错误率统计
   - 系统资源使用
   - MinIO服务状态

### 日志管理
- 结构化日志记录
- 错误日志告警
- 访问日志分析
- 性能日志监控

## 已知问题和解决方案

### 问题1：大文件上传超时
**问题描述**: 上传大文件时可能出现超时
**解决方案**: 
- 增加HTTP请求超时时间
- 实现分片上传功能
- 添加上传进度显示

### 问题2：图片压缩质量
**问题描述**: 图片压缩后质量下降明显
**解决方案**:
- 调整压缩参数
- 根据图片大小动态调整压缩比
- 提供压缩质量选项

### 问题3：MinIO连接池
**问题描述**: 高并发时MinIO连接不足
**解决方案**:
- 优化MinIO客户端配置
- 增加连接池大小
- 实现连接重试机制

## 未来规划

### 短期目标（1-3个月）
- [ ] 实现文件分片上传
- [ ] 添加上传进度显示
- [ ] 集成CDN服务
- [ ] 完善监控告警

### 中期目标（3-6个月）
- [ ] 实现文件版本管理
- [ ] 添加文件预览功能
- [ ] 支持文件在线编辑
- [ ] 实现文件同步功能

### 长期目标（6-12个月）
- [ ] 支持多云存储
- [ ] 实现智能文件分类
- [ ] 添加AI文件内容分析
- [ ] 支持区块链文件验证

## 参考文档

- [FILE_STORAGE_ARCHITECTURE.md](../FILE_STORAGE_ARCHITECTURE.md) - 文件存储架构说明
- [DATA_CONSISTENCY_GUIDE.md](../DATA_CONSISTENCY_GUIDE.md) - 数据一致性保障指南
- [MinIO官方文档](https://docs.min.io/)
- [Spring Boot文件上传文档](https://spring.io/guides/gs/uploading-files/)

## 开发团队

- **主要开发者**: 系统架构师
- **代码审查**: 技术负责人
- **测试负责人**: QA工程师
- **运维负责人**: DevOps工程师

---

**最后更新**: 2025年1月12日  
**文档版本**: v2.1  
**状态**: 持续维护中