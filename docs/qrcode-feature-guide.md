# 二维码名片功能技术文档

## 概述

二维码名片功能为IM通信系统提供了便捷的用户信息分享和识别能力。用户可以生成包含个人信息的二维码名片，其他用户通过扫描二维码可以快速获取用户信息并发起好友申请。

## 功能特性

### 核心功能
- **二维码生成**：基于用户信息生成个性化二维码名片
- **二维码解析**：解析二维码获取用户信息
- **信息编码**：安全的用户信息编码和解码
- **图片处理**：支持多种图片格式的二维码识别
- **界面集成**：完整的前端界面和交互体验

### 扩展特性
- **下载功能**：支持二维码图片下载
- **分享功能**：支持系统原生分享
- **响应式设计**：适配不同设备屏幕
- **动画效果**：流畅的界面动画
- **错误处理**：完善的错误提示和处理

## API接口文档

### 1. 生成用户二维码

**接口地址**：`GET /api/user/qrcode`

**请求头**：
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**响应格式**：
```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {
    "qrCodeData": "IM_USER:eyJ1c2VySWQiOjEsIm5pY2tuYW1lIjoi5byg5LiJIiwidGltZXN0YW1wIjoxNjQwOTk1MjAwMDAwfQ==",
    "qrCodeBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51...",
    "userId": 1,
    "userNickname": "张三",
    "userAvatarUrl": "https://example.com/avatar.jpg",
    "generatedAt": "2025-01-01T12:00:00",
    "expiresAt": "2025-01-02T12:00:00"
  }
}
```

**字段说明**：
- `qrCodeData`：原始二维码数据内容
- `qrCodeBase64`：Base64编码的PNG图片，可直接用于img标签
- `userId`：用户ID
- `userNickname`：用户昵称
- `userAvatarUrl`：用户头像URL
- `generatedAt`：生成时间
- `expiresAt`：过期时间

**错误响应**：
```json
{
  "success": false,
  "code": 500,
  "message": "生成二维码失败: 用户不存在",
  "data": null
}
```

### 2. 解析二维码

**接口地址**：`POST /api/user/qrcode/parse`

**请求头**：
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: multipart/form-data
```

**请求参数**：
- `file`：二维码图片文件（支持PNG、JPG、JPEG等格式）

**响应格式**：
```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "nickname": "张三",
    "avatarUrl": "https://example.com/avatar.jpg",
    "timestamp": 1640995200000,
    "userExists": true,
    "currentNickname": "张三",
    "currentAvatarUrl": "https://example.com/avatar.jpg"
  }
}
```

**字段说明**：
- `userId`：二维码中的用户ID
- `nickname`：二维码中的用户昵称
- `avatarUrl`：二维码中的用户头像
- `timestamp`：二维码生成时间戳
- `userExists`：用户是否仍存在于系统中
- `currentNickname`：用户当前昵称
- `currentAvatarUrl`：用户当前头像

## 前端集成指南

### 1. HTML结构

```html
<!-- 触发按钮 -->
<button onclick="showQRCode()" class="qr-code-btn">
    <i class="icon-qrcode"></i>
    我的二维码
</button>

<!-- 二维码模态框 -->
<div id="qrCodeModal" class="modal">
    <div class="modal-content qr-modal-content">
        <div class="qr-modal-header">
            <h3>我的二维码名片</h3>
            <span class="close" onclick="closeQRCodeModal()">&times;</span>
        </div>
        <div class="qr-modal-body">
            <div class="qr-code-container">
                <img id="qrCodeImage" alt="二维码" />
            </div>
            <div class="qr-user-info">
                <h4 id="qrUserName">用户昵称</h4>
                <p id="qrUserInfo">用户信息</p>
            </div>
            <div class="qr-actions">
                <button onclick="downloadQRCode()" class="btn-download">
                    <i class="icon-download"></i> 下载
                </button>
                <button onclick="shareQRCode()" class="btn-share">
                    <i class="icon-share"></i> 分享
                </button>
            </div>
        </div>
    </div>
</div>
```

### 2. JavaScript实现

```javascript
// 显示二维码
async function showQRCode() {
    const token = getAuthToken();
    if (!token) {
        alert('请先登录');
        return;
    }

    try {
        const response = await fetch('/api/user/qrcode', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('生成二维码失败');
        }

        const data = await response.json();
        if (data.success) {
            showQRCodeModal(data.data);
        } else {
            throw new Error(data.message || '生成二维码失败');
        }
    } catch (error) {
        console.error('生成二维码失败:', error);
        alert('生成二维码失败: ' + error.message);
    }
}

// 显示模态框
function showQRCodeModal(qrCodeData) {
    const modal = document.getElementById('qrCodeModal');
    const qrImage = document.getElementById('qrCodeImage');
    const userName = document.getElementById('qrUserName');
    const userInfo = document.getElementById('qrUserInfo');
    
    // 设置二维码图片
    qrImage.src = qrCodeData.qrCodeBase64;
    
    // 设置用户信息
    userName.textContent = qrCodeData.userNickname || '用户';
    userInfo.textContent = qrCodeData.userId ? `ID: ${qrCodeData.userId}` : '暂无个人ID';
    
    modal.style.display = 'flex';
}

// 下载二维码
function downloadQRCode() {
    const qrImage = document.getElementById('qrCodeImage');
    const link = document.createElement('a');
    link.download = '我的二维码名片.png';
    link.href = qrImage.src;
    link.click();
}

// 分享二维码
function shareQRCode() {
    if (navigator.share) {
        const qrImage = document.getElementById('qrCodeImage');
        fetch(qrImage.src)
            .then(res => res.blob())
            .then(blob => {
                const file = new File([blob], '我的二维码名片.png', { type: 'image/png' });
                navigator.share({
                    title: '我的二维码名片',
                    text: '扫描二维码添加我为好友',
                    files: [file]
                });
            })
            .catch(err => {
                console.error('分享失败:', err);
                alert('分享功能暂不支持，请使用下载功能');
            });
    } else {
        alert('您的浏览器不支持分享功能，请使用下载功能');
    }
}

// 解析二维码
async function parseQRCode(file) {
    const token = getAuthToken();
    if (!token) {
        alert('请先登录');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch('/api/user/qrcode/parse', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token
            },
            body: formData
        });

        if (!response.ok) {
            throw new Error('解析二维码失败');
        }

        const data = await response.json();
        if (data.success) {
            return data.data;
        } else {
            throw new Error(data.message || '解析二维码失败');
        }
    } catch (error) {
        console.error('解析二维码失败:', error);
        throw error;
    }
}
```

### 3. CSS样式

```css
/* 二维码模态框样式 */
.qr-modal-content {
    background: white;
    border-radius: 12px;
    padding: 0;
    max-width: 400px;
    width: 90%;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
    animation: modalSlideIn 0.3s ease-out;
}

.qr-modal-header {
    padding: 20px 24px 16px;
    border-bottom: 1px solid #eee;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.qr-modal-body {
    padding: 24px;
    text-align: center;
}

.qr-code-container {
    margin-bottom: 20px;
    padding: 16px;
    background: #f8f9fa;
    border-radius: 8px;
    display: inline-block;
}

.qr-code-container img {
    width: 200px;
    height: 200px;
    border-radius: 4px;
}

.qr-user-info {
    margin-bottom: 24px;
}

.qr-user-info h4 {
    margin: 0 0 8px 0;
    font-size: 18px;
    color: #333;
}

.qr-user-info p {
    margin: 0;
    color: #666;
    font-size: 14px;
}

.qr-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
}

.btn-download, .btn-share {
    padding: 10px 20px;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 6px;
    transition: all 0.2s;
}

.btn-download {
    background: #007bff;
    color: white;
}

.btn-download:hover {
    background: #0056b3;
}

.btn-share {
    background: #28a745;
    color: white;
}

.btn-share:hover {
    background: #1e7e34;
}

@keyframes modalSlideIn {
    from {
        opacity: 0;
        transform: translateY(-50px) scale(0.9);
    }
    to {
        opacity: 1;
        transform: translateY(0) scale(1);
    }
}

/* 响应式设计 */
@media (max-width: 480px) {
    .qr-modal-content {
        width: 95%;
        margin: 20px auto;
    }
    
    .qr-code-container img {
        width: 160px;
        height: 160px;
    }
    
    .qr-actions {
        flex-direction: column;
    }
}
```

## 后端扩展指南

### 1. 自定义配置

```java
@Configuration
@ConfigurationProperties(prefix = "app.qrcode")
public class QRCodeConfig {
    private int width = 300;
    private int height = 300;
    private String format = "PNG";
    private long expirationHours = 24;
    
    // 用户信息配置
    private UserInfoConfig userInfo = new UserInfoConfig();
    
    @Data
    public static class UserInfoConfig {
        private boolean includeAvatar = true;
        private boolean includeSignature = true;
        private String prefix = "IM_USER:";
    }
    
    // getter和setter方法...
}
```

### 2. 扩展用户信息

```java
// 在UserQRCodeServiceImpl中扩展用户信息
private Map<String, Object> buildUserInfo(User user) {
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put("userId", user.getId());
    userInfo.put("nickname", user.getNickname());
    userInfo.put("timestamp", System.currentTimeMillis());
    
    // 可选信息
    if (qrCodeConfig.getUserInfo().isIncludeAvatar() && user.getAvatarUrl() != null) {
        userInfo.put("avatarUrl", user.getAvatarUrl());
    }
    
    if (qrCodeConfig.getUserInfo().isIncludeSignature() && user.getSignature() != null) {
        userInfo.put("signature", user.getSignature());
    }
    
    // 扩展字段
    if (user.getLocation() != null) {
        userInfo.put("location", user.getLocation());
    }
    
    if (user.getOccupation() != null) {
        userInfo.put("occupation", user.getOccupation());
    }
    
    return userInfo;
}
```

### 3. 添加缓存支持

```java
@Service
@RequiredArgsConstructor
public class UserQRCodeServiceImpl implements UserQRCodeService {
    
    @Cacheable(value = "qrcode", key = "#userId", unless = "#result == null")
    @Override
    public QRCodeResponse generateUserQRCode(Long userId) {
        // 现有实现...
    }
    
    @CacheEvict(value = "qrcode", key = "#userId")
    public void evictQRCodeCache(Long userId) {
        // 清除缓存，当用户信息更新时调用
    }
}
```

## 安全考虑

### 1. 数据保护
- **最小化原则**：只包含必要的用户信息
- **时效性**：设置合理的过期时间
- **加密传输**：使用HTTPS传输
- **访问控制**：基于JWT的身份验证

### 2. 防护措施
- **输入验证**：验证上传文件的格式和大小
- **异常处理**：避免敏感信息泄露
- **日志记录**：记录关键操作用于审计
- **频率限制**：防止恶意请求

### 3. 隐私保护
```java
// 敏感信息过滤
private Map<String, Object> filterSensitiveInfo(Map<String, Object> userInfo) {
    // 移除敏感字段
    userInfo.remove("email");
    userInfo.remove("phoneNumber");
    userInfo.remove("realName");
    
    return userInfo;
}
```

## 性能优化

### 1. 图片处理优化
```java
// 使用线程池处理图片生成
@Async
public CompletableFuture<String> generateQRCodeAsync(String content) {
    BufferedImage image = QRCodeUtils.generateQRCode(content, 300, 300);
    String base64 = QRCodeUtils.imageToBase64(image, "PNG");
    return CompletableFuture.completedFuture(base64);
}
```

### 2. 缓存策略
```java
// Redis缓存配置
@Configuration
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1)) // 1小时过期
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

## 监控和日志

### 1. 关键指标
- 二维码生成成功率
- 解析成功率
- 平均响应时间
- 错误率统计

### 2. 日志配置
```yaml
logging:
  level:
    com.im.imcommunicationsystem.user.service: INFO
    com.im.imcommunicationsystem.user.util.QRCodeUtils: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 3. 监控端点
```java
@RestController
@RequestMapping("/actuator/qrcode")
public class QRCodeMetricsController {
    
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalGenerated", qrCodeMetrics.getTotalGenerated());
        metrics.put("totalParsed", qrCodeMetrics.getTotalParsed());
        metrics.put("successRate", qrCodeMetrics.getSuccessRate());
        return metrics;
    }
}
```

## 故障排查

### 常见问题

1. **二维码无法显示**
   - 检查Base64格式是否正确
   - 验证图片数据完整性
   - 确认前端字段名匹配

2. **解析失败**
   - 验证图片格式支持
   - 检查二维码内容格式
   - 确认用户权限

3. **性能问题**
   - 检查图片尺寸设置
   - 监控内存使用情况
   - 优化数据库查询

### 调试工具
```java
// 调试工具类
@Component
public class QRCodeDebugUtils {
    
    public void debugQRCodeContent(String content) {
        log.debug("QR Code Content: {}", content);
        log.debug("Content Length: {}", content.length());
        
        if (content.startsWith("IM_USER:")) {
            String encoded = content.substring(8);
            try {
                byte[] decoded = Base64.getDecoder().decode(encoded);
                String json = new String(decoded, StandardCharsets.UTF_8);
                log.debug("Decoded JSON: {}", json);
            } catch (Exception e) {
                log.error("Failed to decode content", e);
            }
        }
    }
}
```

---

**文档版本**：v1.0  
**最后更新**：2025年1月  
**适用版本**：IM通信系统 v1.0+