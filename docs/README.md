# IM通信系统文档中心

## 文档概述

本目录包含IM通信系统的完整技术文档，涵盖系统架构、开发指南、API文档和运维手册等内容。

## 文档结构

### 📋 核心文档

#### 系统架构文档
- **[FILE_STORAGE_ARCHITECTURE.md](FILE_STORAGE_ARCHITECTURE.md)** - 文件存储架构说明
  - 存储桶架构设计
  - 访问权限控制
  - 安全策略配置
  - 性能优化方案

- **[DATA_CONSISTENCY_GUIDE.md](DATA_CONSISTENCY_GUIDE.md)** - 数据一致性保障指南
  - 数据一致性检查机制
  - 自动修复策略
  - 监控告警配置
  - 故障排查指南

#### 功能设计文档
- **[auth-module-design-comparison.md](auth-module-design-comparison.md)** - 认证模块设计对比
- **[common-module-development.md](common-module-development.md)** - 通用模块开发指南
- **[verification-code-system.md](verification-code-system.md)** - 验证码系统设计

#### 项目管理文档
- **[project-modification-log.md](project-modification-log.md)** - 项目修改记录
  - 功能实现汇总
  - 技术架构说明
  - 安全机制实现
  - 性能优化记录

### 📝 开发日志

#### [development-logs/](development-logs/) 目录
包含各个功能模块的详细开发日志：

- **[file-upload-development-log.md](development-logs/file-upload-development-log.md)** - 文件上传功能开发日志
  - 功能特性详述
  - 技术架构设计
  - 开发历程记录
  - 性能优化方案
  - 安全措施实现
  - 未来规划路线

- **[profile-management-development.md](development-logs/profile-management-development.md)** - 用户资料管理开发日志

#### 功能模块开发日志
- **[email-registration-development-log.md](email-registration-development-log.md)** - 邮箱注册功能开发
- **[password-login-development-log.md](password-login-development-log.md)** - 密码登录功能开发
- **[verification-code-login-development-log.md](verification-code-login-development-log.md)** - 验证码登录功能开发

## 文档使用指南

### 🚀 快速开始

1. **新开发者入门**
   - 首先阅读 [project-modification-log.md](project-modification-log.md) 了解项目整体架构
   - 查看 [common-module-development.md](common-module-development.md) 了解开发规范
   - 根据需要查看具体功能模块的开发日志

2. **功能开发参考**
   - 查看对应功能的开发日志了解实现细节
   - 参考架构文档了解系统设计原则
   - 遵循现有的代码规范和最佳实践

3. **问题排查**
   - 查看 [DATA_CONSISTENCY_GUIDE.md](DATA_CONSISTENCY_GUIDE.md) 了解数据一致性问题
   - 参考开发日志中的"已知问题和解决方案"部分
   - 查看项目修改记录了解历史变更

### 📖 文档阅读建议

#### 按角色阅读

**系统架构师**
- FILE_STORAGE_ARCHITECTURE.md
- auth-module-design-comparison.md
- project-modification-log.md

**后端开发工程师**
- development-logs/file-upload-development-log.md
- common-module-development.md
- verification-code-system.md

**运维工程师**
- DATA_CONSISTENCY_GUIDE.md
- FILE_STORAGE_ARCHITECTURE.md（部署配置部分）
- project-modification-log.md（部署配置实现部分）

**测试工程师**
- 各功能模块开发日志中的测试策略部分
- project-modification-log.md（测试覆盖实现部分）

#### 按功能阅读

**文件上传功能**
1. FILE_STORAGE_ARCHITECTURE.md - 了解整体架构
2. development-logs/file-upload-development-log.md - 了解实现细节
3. project-modification-log.md - 了解在项目中的位置

**用户认证功能**
1. auth-module-design-comparison.md - 了解设计思路
2. verification-code-system.md - 了解验证码机制
3. email-registration-development-log.md - 了解注册流程
4. password-login-development-log.md - 了解登录流程

## 文档维护规范

### 📝 文档更新原则

1. **及时性**：功能开发完成后及时更新相关文档
2. **准确性**：确保文档内容与代码实现保持一致
3. **完整性**：包含设计思路、实现细节、测试方案等完整信息
4. **可读性**：使用清晰的结构和简洁的语言

### 🔄 文档更新流程

1. **功能开发阶段**
   - 在development-logs目录下创建对应的开发日志
   - 记录设计思路、技术选型、实现过程

2. **功能完成阶段**
   - 更新project-modification-log.md中的功能汇总
   - 完善开发日志中的测试和部署信息

3. **架构变更阶段**
   - 更新相关的架构文档
   - 在项目修改记录中说明变更原因和影响

### 📋 文档模板

#### 开发日志模板
```markdown
# [功能名称]开发日志

## 项目信息
- 项目名称
- 模块名称
- 开发时间
- 最后更新

## 开发概述
## 功能特性
## 技术架构
## 数据库设计
## API接口设计
## 开发历程
## 配置说明
## 性能优化
## 安全措施
## 测试策略
## 监控和运维
## 已知问题和解决方案
## 未来规划
## 参考文档
```

## 相关资源

### 🔗 外部链接
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [MinIO官方文档](https://docs.min.io/)
- [MySQL官方文档](https://dev.mysql.com/doc/)
- [Redis官方文档](https://redis.io/documentation)

### 📚 技术栈文档
- Spring Boot 3.x
- Spring Security 6.x
- MySQL 8.0
- Redis 7.x
- MinIO
- Flyway

## 联系方式

### 📧 文档维护团队
- **技术负责人**：系统架构师
- **文档管理员**：技术文档工程师
- **代码审查员**：高级开发工程师

### 🐛 问题反馈
如果发现文档中的错误或需要补充的内容，请通过以下方式反馈：
1. 创建Issue描述问题
2. 提交Pull Request修复问题
3. 联系文档维护团队

---

**最后更新**: 2025年1月12日  
**文档版本**: v1.0  
**维护状态**: 持续更新中