# 邮箱配置指南

本指南将帮助您配置真实的邮件发送功能，实现从配置的邮箱服务器发送验证码到用户注册邮箱。

## 配置步骤

### 1. 选择邮箱服务商

系统支持多种邮箱服务商，推荐使用以下几种：

#### QQ邮箱（推荐）
- **优点**：稳定性好，发送速度快，国内用户使用广泛
- **SMTP服务器**：smtp.qq.com
- **端口**：587（STARTTLS）或 465（SSL）

#### 163邮箱
- **优点**：免费，配置简单
- **SMTP服务器**：smtp.163.com
- **端口**：465（SSL）或 25（非加密，不推荐）

#### Gmail
- **优点**：国际通用，功能强大
- **SMTP服务器**：smtp.gmail.com
- **端口**：587（STARTTLS）或 465（SSL）
- **注意**：需要开启"允许不够安全的应用"或使用应用专用密码

### 2. 获取邮箱授权码

#### QQ邮箱授权码获取步骤：
1. 登录QQ邮箱网页版
2. 点击"设置" → "账户"
3. 找到"POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务"
4. 开启"POP3/SMTP服务"或"IMAP/SMTP服务"
5. 按照提示发送短信，获取授权码
6. **重要**：授权码不是QQ密码，是一串16位的字母数字组合

#### 163邮箱授权码获取步骤：
1. 登录163邮箱网页版
2. 点击"设置" → "POP3/SMTP/IMAP"
3. 开启"POP3/SMTP服务"和"IMAP/SMTP服务"
4. 设置客户端授权密码（即授权码）

#### Gmail应用专用密码获取步骤：
1. 登录Google账户
2. 开启两步验证
3. 生成应用专用密码
4. 使用生成的16位密码作为授权码

### 3. 修改配置文件

编辑 `src/main/resources/application.yml` 文件：

#### QQ邮箱配置示例：
```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: your-email@qq.com        # 替换为您的QQ邮箱
    password: your-authorization-code   # 替换为您的QQ邮箱授权码
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
    default-encoding: UTF-8
```

#### 163邮箱配置示例：
```yaml
spring:
  mail:
    host: smtp.163.com
    port: 465
    username: your-email@163.com
    password: your-authorization-code
    properties:
      mail:
        smtp:
          auth: true
          ssl:
            enable: true
            trust: smtp.163.com
    default-encoding: UTF-8
```

#### Gmail配置示例：
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
    default-encoding: UTF-8
```

### 4. 测试邮件发送

配置完成后，重启应用程序并测试：

1. 启动应用：`mvn spring-boot:run`
2. 访问注册页面：`http://localhost:8080/register.html`
3. 输入邮箱地址，点击"发送验证码"
4. 检查目标邮箱是否收到验证码

### 5. 常见问题排查

#### 问题1：535 Authentication failed
**原因**：用户名或授权码错误
**解决**：
- 确认邮箱地址正确
- 确认使用的是授权码，不是登录密码
- 重新生成授权码

#### 问题2：连接超时
**原因**：网络问题或端口被封
**解决**：
- 检查网络连接
- 尝试不同的端口（587/465）
- 检查防火墙设置

#### 问题3：SSL连接失败
**原因**：SSL配置问题
**解决**：
- 对于端口465，启用SSL配置
- 对于端口587，使用STARTTLS配置

#### 问题4：邮件被拒绝
**原因**：邮箱服务商限制
**解决**：
- 确认SMTP服务已开启
- 检查发送频率限制
- 联系邮箱服务商客服

### 6. 安全建议

1. **不要将授权码提交到版本控制系统**
2. **使用环境变量存储敏感信息**：
   ```yaml
   spring:
     mail:
       username: ${MAIL_USERNAME:your-email@qq.com}
       password: ${MAIL_PASSWORD:your-authorization-code}
   ```
3. **定期更换授权码**
4. **监控邮件发送日志**

### 7. 生产环境部署

在生产环境中，建议：

1. 使用专用的邮箱账户
2. 配置邮件发送限流
3. 添加邮件发送监控
4. 准备备用邮箱服务商

## 业务流程说明

### 邮箱注册完整流程：

1. **用户输入邮箱** → 前端验证邮箱格式
2. **点击发送验证码** → 调用 `/api/auth/verification/send/register`
3. **后端生成验证码** → 6位随机数字，有效期5分钟
4. **保存验证码到数据库** → 包含邮箱、验证码、过期时间、使用状态
5. **发送邮件** → 通过配置的SMTP服务器发送
6. **用户收到邮件** → 包含验证码和有效期说明
7. **用户输入验证码和密码** → 前端验证密码强度和一致性
8. **提交注册** → 调用 `/api/auth/register/email`
9. **后端验证** → 验证码有效性、邮箱唯一性、密码强度
10. **创建用户账户** → 保存用户信息，标记验证码已使用
11. **注册成功** → 返回成功信息，用户可以登录

### 验证码安全机制：

- **有效期限制**：验证码5分钟后自动过期
- **一次性使用**：验证码使用后立即失效
- **频率限制**：同一邮箱60秒内只能发送一次
- **格式验证**：6位数字验证码
- **数据库存储**：加密存储，定期清理过期数据

配置完成后，您的应用就具备了完整的邮箱注册功能！