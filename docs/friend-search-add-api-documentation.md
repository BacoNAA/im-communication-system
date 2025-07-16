# 好友搜索和添加功能 API 文档

## 概述

本文档详细描述了IM通信系统中好友搜索和添加功能的API接口设计、数据模型和实现细节。

## API 接口列表

### 1. 用户搜索接口

#### 接口信息
- **URL**: `/api/users/search`
- **方法**: `GET`
- **描述**: 根据关键词搜索用户
- **认证**: 需要Bearer Token

#### 请求参数
| 参数名 | 类型 | 必填 | 描述 | 示例 |
|--------|------|------|------|------|
| keyword | String | 是 | 搜索关键词（用户ID或昵称） | "张三" 或 "user123" |
| page | Integer | 否 | 页码，默认为1 | 1 |
| size | Integer | 否 | 每页大小，默认为20 | 20 |

#### 请求示例
```http
GET /api/users/search?keyword=张三&page=1&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

#### 响应格式
```json
{
  "success": true,
  "message": "搜索成功",
  "data": [
    {
      "id": "user123",
      "username": "zhangsan",
      "nickname": "张三",
      "avatarUrl": "https://example.com/avatar/user123.jpg",
      "status": "ACTIVE",
      "isOnline": true,
      "lastActiveTime": "2025-01-12T10:30:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 1,
    "totalElements": 1,
    "size": 10
  }
}
```

#### 错误响应
```json
{
  "success": false,
  "message": "搜索关键词不能为空",
  "errorCode": "INVALID_PARAMETER"
}
```

### 2. 发送好友请求接口

#### 接口信息
- **URL**: `/api/friend-requests/send`
- **方法**: `POST`
- **描述**: 发送好友请求
- **认证**: 需要Bearer Token

#### 请求体
```json
{
  "senderId": "user123",
  "receiverId": "user456",
  "message": "你好，我是张三，希望能成为朋友！"
}
```

#### 请求参数说明
| 参数名 | 类型 | 必填 | 描述 | 限制 |
|--------|------|------|------|------|
| senderId | String | 是 | 发送者用户ID | 必须是当前登录用户 |
| receiverId | String | 是 | 接收者用户ID | 不能是自己 |
| message | String | 否 | 好友请求消息 | 最大200字符 |

#### 响应格式
```json
{
  "success": true,
  "message": "好友请求发送成功",
  "data": {
    "requestId": 12345,
    "status": "PENDING",
    "createdAt": "2025-01-12T10:30:00Z"
  }
}
```

#### 错误响应示例
```json
{
  "success": false,
  "message": "不能向自己发送好友请求",
  "errorCode": "INVALID_OPERATION"
}
```

### 3. 获取好友请求列表接口

#### 接口信息
- **URL**: `/api/friend-requests`
- **方法**: `GET`
- **描述**: 获取好友请求列表
- **认证**: 需要Bearer Token

#### 请求参数
| 参数名 | 类型 | 必填 | 描述 | 可选值 |
|--------|------|------|------|--------|
| type | String | 否 | 请求类型，默认为received | received, sent |
| status | String | 否 | 请求状态，默认为all | pending, accepted, rejected, all |
| page | Integer | 否 | 页码，默认为1 | ≥1 |
| size | Integer | 否 | 每页大小，默认为20 | 1-100 |

#### 响应格式
```json
{
  "success": true,
  "message": "获取成功",
  "data": [
    {
      "id": 12345,
      "senderId": "user123",
      "receiverId": "user456",
      "senderInfo": {
        "id": "user123",
        "nickname": "张三",
        "avatarUrl": "https://example.com/avatar/user123.jpg"
      },
      "receiverInfo": {
        "id": "user456",
        "nickname": "李四",
        "avatarUrl": "https://example.com/avatar/user456.jpg"
      },
      "message": "你好，我是张三，希望能成为朋友！",
      "status": "PENDING",
      "createdAt": "2025-01-12T10:30:00Z",
      "updatedAt": "2025-01-12T10:30:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "totalPages": 1,
    "totalElements": 1,
    "size": 20
  }
}
```

### 4. 处理好友请求接口

#### 接口信息
- **URL**: `/api/friend-requests/{requestId}/handle`
- **方法**: `PUT`
- **描述**: 接受或拒绝好友请求
- **认证**: 需要Bearer Token

#### 路径参数
| 参数名 | 类型 | 描述 |
|--------|------|------|
| requestId | Long | 好友请求ID |

#### 请求体
```json
{
  "action": "ACCEPT",
  "message": "很高兴认识你！"
}
```

#### 请求参数说明
| 参数名 | 类型 | 必填 | 描述 | 可选值 |
|--------|------|------|------|--------|
| action | String | 是 | 处理动作 | ACCEPT, REJECT |
| message | String | 否 | 回复消息 | 最大200字符 |

#### 响应格式
```json
{
  "success": true,
  "message": "好友请求已接受",
  "data": {
    "requestId": 12345,
    "status": "ACCEPTED",
    "updatedAt": "2025-01-12T11:00:00Z"
  }
}
```

### 5. 删除好友接口

#### 接口信息
- **URL**: `/api/contacts/delete`
- **方法**: `DELETE`
- **描述**: 删除好友关系
- **认证**: 需要Bearer Token

#### 请求体
```json
{
  "userId": "user123",
  "contactId": "user456"
}
```

#### 请求参数说明
| 参数名 | 类型 | 必填 | 描述 | 限制 |
|--------|------|------|------|------|
| userId | String | 是 | 当前用户ID | 必须是当前登录用户 |
| contactId | String | 是 | 要删除的好友ID | 必须是已存在的好友 |

#### 响应格式
```json
{
  "success": true,
  "message": "好友删除成功",
  "data": {
    "deletedAt": "2025-01-12T11:30:00Z"
  }
}
```

#### 错误响应示例
```json
{
  "success": false,
  "message": "好友关系不存在",
  "errorCode": "FRIEND_NOT_FOUND"
}
```

### 6. 设置联系人备注接口

#### 接口信息
- **URL**: `/api/contacts/alias`
- **方法**: `PUT`
- **描述**: 设置或更新联系人备注
- **认证**: 需要Bearer Token

#### 请求体
```json
{
  "userId": "user123",
  "contactId": "user456",
  "alias": "小明"
}
```

#### 请求参数说明
| 参数名 | 类型 | 必填 | 描述 | 限制 |
|--------|------|------|------|------|
| userId | String | 是 | 当前用户ID | 必须是当前登录用户 |
| contactId | String | 是 | 联系人ID | 必须是已存在的好友 |
| alias | String | 否 | 备注名称 | 最大50字符，空字符串表示清除备注 |

#### 响应格式
```json
{
  "success": true,
  "message": "备注设置成功",
  "data": {
    "contactId": "user456",
    "alias": "小明",
    "updatedAt": "2025-01-12T11:45:00Z"
  }
}
```

#### 错误响应示例
```json
{
  "success": false,
  "message": "备注名称过长",
  "errorCode": "ALIAS_TOO_LONG"
}
```

## 数据模型

### 用户信息模型 (UserProfileDTO)
```java
public class UserProfileDTO {
    private String id;              // 用户ID
    private String username;        // 用户名
    private String nickname;        // 昵称
    private String email;           // 邮箱
    private String avatarUrl;       // 头像URL
    private String status;          // 用户状态 (ACTIVE, INACTIVE, BANNED)
    private Boolean isOnline;       // 是否在线
    private LocalDateTime lastActiveTime; // 最后活跃时间
    private String personalStatus;  // 个性签名
    
    // getters and setters...
}
```

### 好友请求模型 (FriendRequestDTO)
```java
public class FriendRequestDTO {
    private Long id;                // 请求ID
    private String senderId;        // 发送者ID
    private String receiverId;      // 接收者ID
    private UserProfileDTO senderInfo;   // 发送者信息
    private UserProfileDTO receiverInfo; // 接收者信息
    private String message;         // 请求消息
    private String status;          // 请求状态 (PENDING, ACCEPTED, REJECTED)
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
    
    // getters and setters...
}
```

### 分页信息模型 (PaginationInfo)
```java
public class PaginationInfo {
    private Integer currentPage;    // 当前页码
    private Integer totalPages;     // 总页数
    private Long totalElements;     // 总元素数
    private Integer size;           // 每页大小
    
    // getters and setters...
}
```

## 业务逻辑

### 用户搜索逻辑

1. **输入验证**
   - 检查搜索关键词是否为空
   - 验证分页参数的有效性
   - 确认用户已登录

2. **搜索执行**
   - 根据关键词在用户名和昵称字段中进行模糊匹配
   - 排除当前用户自己
   - 排除已被屏蔽的用户
   - 按相关度排序结果

3. **结果处理**
   - 脱敏处理敏感信息
   - 添加在线状态信息
   - 分页返回结果

### 好友请求处理逻辑

1. **发送请求**
   ```
   开始 → 验证用户身份 → 检查目标用户存在性 → 检查是否已是好友 
   → 检查是否已有待处理请求 → 创建好友请求记录 → 发送通知 → 返回结果
   ```

2. **处理请求**
   ```
   开始 → 验证用户身份 → 检查请求归属 → 检查请求状态 → 更新请求状态 
   → 如果接受则创建好友关系 → 发送通知 → 返回结果
   ```

## 错误码定义

| 错误码 | HTTP状态码 | 描述 | 解决方案 |
|--------|------------|------|----------|
| INVALID_PARAMETER | 400 | 请求参数无效 | 检查请求参数格式和值 |
| UNAUTHORIZED | 401 | 未授权访问 | 重新登录获取有效token |
| FORBIDDEN | 403 | 禁止访问 | 检查用户权限 |
| USER_NOT_FOUND | 404 | 用户不存在 | 确认用户ID正确 |
| REQUEST_NOT_FOUND | 404 | 好友请求不存在 | 确认请求ID正确 |
| FRIEND_NOT_FOUND | 404 | 好友关系不存在 | 检查好友ID是否正确 |
| DUPLICATE_REQUEST | 409 | 重复的好友请求 | 等待之前的请求处理完成 |
| ALREADY_FRIENDS | 409 | 已经是好友关系 | 无需重复添加 |
| SELF_REQUEST | 409 | 不能向自己发送请求 | 选择其他用户 |
| ALIAS_TOO_LONG | 400 | 备注名称过长 | 最多50个字符 |
| INVALID_OPERATION | 400 | 无效操作 | 检查操作类型和参数 |
| INTERNAL_ERROR | 500 | 服务器内部错误 | 联系技术支持 |

## 性能优化

### 数据库优化

1. **索引设计**
   ```sql
   -- 用户搜索索引
   CREATE INDEX idx_users_search ON users(username, nickname);
   
   -- 好友请求查询索引
   CREATE INDEX idx_friend_requests_receiver ON friend_requests(receiver_id, status, created_at);
   CREATE INDEX idx_friend_requests_sender ON friend_requests(sender_id, status, created_at);
   
   -- 复合索引
   CREATE INDEX idx_friend_requests_unique ON friend_requests(sender_id, receiver_id);
   ```

2. **查询优化**
   - 使用LIMIT限制返回结果数量
   - 避免SELECT *，只查询需要的字段
   - 使用JOIN减少多次查询

### 缓存策略

1. **搜索结果缓存**
   ```java
   @Cacheable(value = "userSearch", key = "#keyword + '_' + #page + '_' + #size")
   public List<UserProfileDTO> searchUsers(String keyword, int page, int size) {
       // 搜索逻辑
   }
   ```

2. **用户信息缓存**
   ```java
   @Cacheable(value = "userProfile", key = "#userId")
   public UserProfileDTO getUserProfile(String userId) {
       // 获取用户信息逻辑
   }
   ```

### 并发控制

1. **防重复提交**
   ```java
   @Transactional
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   public void sendFriendRequest(String senderId, String receiverId) {
       // 发送好友请求逻辑
   }
   ```

2. **分布式锁**
   ```java
   @RedisLock(key = "friend_request_#{#senderId}_#{#receiverId}")
   public void sendFriendRequest(String senderId, String receiverId) {
       // 发送好友请求逻辑
   }
   ```

## 安全措施

### 1. 输入验证
```java
@Valid
public class SendFriendRequestDTO {
    @NotBlank(message = "发送者ID不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$", message = "用户ID格式不正确")
    private String senderId;
    
    @NotBlank(message = "接收者ID不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$", message = "用户ID格式不正确")
    private String receiverId;
    
    @Size(max = 200, message = "消息长度不能超过200字符")
    private String message;
}
```

### 2. 权限控制
```java
@PreAuthorize("#senderId == authentication.name")
public void sendFriendRequest(String senderId, String receiverId) {
    // 确保只能以自己的身份发送请求
}
```

### 3. 频率限制
```java
@RateLimiter(name = "friendRequest", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> sendFriendRequest(@RequestBody SendFriendRequestDTO request) {
    // 限制每分钟最多发送5个好友请求
}
```

## 监控和日志

### 1. 关键指标监控
- 搜索请求量和成功率
- 好友请求发送量和接受率
- API响应时间
- 错误率统计

### 2. 日志记录
```java
@Slf4j
public class FriendRequestService {
    
    public void sendFriendRequest(String senderId, String receiverId) {
        log.info("用户 {} 向用户 {} 发送好友请求", senderId, receiverId);
        
        try {
            // 业务逻辑
            log.info("好友请求发送成功: {} -> {}", senderId, receiverId);
        } catch (Exception e) {
            log.error("好友请求发送失败: {} -> {}, 错误: {}", senderId, receiverId, e.getMessage(), e);
            throw e;
        }
    }
}
```

## 测试用例

### 1. 用户搜索测试
```java
@Test
public void testSearchUsers_Success() {
    // 准备测试数据
    String keyword = "张三";
    
    // 执行搜索
    List<UserProfileDTO> results = userService.searchUsers(keyword, 1, 10);
    
    // 验证结果
    assertThat(results).isNotEmpty();
    assertThat(results.get(0).getNickname()).contains(keyword);
}

@Test
public void testSearchUsers_EmptyKeyword() {
    // 测试空关键词
    assertThatThrownBy(() -> userService.searchUsers("", 1, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("搜索关键词不能为空");
}
```

### 2. 好友请求测试
```java
@Test
public void testSendFriendRequest_Success() {
    // 准备测试数据
    String senderId = "user123";
    String receiverId = "user456";
    
    // 执行发送请求
    FriendRequestDTO result = friendRequestService.sendFriendRequest(senderId, receiverId, "你好");
    
    // 验证结果
    assertThat(result.getStatus()).isEqualTo("PENDING");
    assertThat(result.getSenderId()).isEqualTo(senderId);
    assertThat(result.getReceiverId()).isEqualTo(receiverId);
}

@Test
public void testSendFriendRequest_SelfRequest() {
    // 测试向自己发送请求
    String userId = "user123";
    
    assertThatThrownBy(() -> friendRequestService.sendFriendRequest(userId, userId, "你好"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("不能向自己发送好友请求");
}
```

## 部署配置

### 1. 应用配置
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/im_system?useUnicode=true&characterEncoding=utf8
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:password}
    
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5分钟缓存
      
app:
  friend-request:
    max-pending-requests: 100  # 最大待处理请求数
    rate-limit:
      requests-per-minute: 5   # 每分钟最多5个请求
```

### 2. Nginx配置
```nginx
location /api/users/search {
    proxy_pass http://backend;
    proxy_cache search_cache;
    proxy_cache_valid 200 5m;
    proxy_cache_key "$request_uri$request_body";
}

location /api/friend-requests {
    proxy_pass http://backend;
    # 不缓存好友请求相关接口
    proxy_no_cache 1;
}
```

## 总结

本API文档详细描述了好友搜索和添加功能的完整实现，包括接口设计、数据模型、业务逻辑、安全措施和性能优化等方面。通过规范的API设计和完善的错误处理机制，确保了功能的稳定性和用户体验。

后续可以根据实际使用情况，进一步优化搜索算法、增加推荐功能，并扩展更多的社交功能。