import { api } from './index';
import type { ApiResponse } from './index';
import { getCurrentUserId } from '@/utils/helpers';

// 用户接口
export interface User {
  id: number;
  email: string;
  nickname?: string;
  avatarUrl?: string;
}

// 消息类型枚举
export enum MessageType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  FILE = 'FILE',
  VOICE = 'VOICE',
  AUDIO = 'AUDIO',
  VIDEO = 'VIDEO',
  LOCATION = 'LOCATION',
  SYSTEM = 'SYSTEM'
}

// 会话类型枚举
export enum ConversationType {
  PRIVATE = 'PRIVATE',
  GROUP = 'GROUP',
  SYSTEM = 'SYSTEM'
}

// 消息状态枚举
export enum MessageStatus {
  SENDING = 'SENDING',
  SENT = 'SENT',
  DELIVERED = 'DELIVERED',
  FAILED = 'FAILED',
  RECALLED = 'RECALLED'
}

// 消息接口
export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  type: MessageType;
  content: string;
  mediaFileId?: number; // 媒体文件ID
  fileName?: string; // 文件名，用于文件类型消息
  status: MessageStatus;
  isRead?: boolean; // 是否已读
  createdAt: string;
  updatedAt: string;
  edited?: boolean; // 消息是否被编辑过
  editedAt?: string; // 消息编辑时间
  attachments?: MessageAttachment[];
  senderName?: string;
  senderAvatar?: string;
  senderNickname?: string;
  messageType?: MessageType;
  isSentByCurrentUser?: boolean;
  // 支持嵌套消息结构
  message?: {
    id: number;
    conversationId: number;
    senderId: number;
    messageType: MessageType;
    content: string;
    mediaFileId?: number; // 媒体文件ID
    fileName?: string; // 文件名，用于文件类型消息
    status: MessageStatus;
    isRead?: boolean; // 是否已读
    createdAt: string;
    updatedAt: string;
    edited?: boolean; // 消息是否被编辑过
    editedAt?: string; // 消息编辑时间
    senderNickname?: string;
    senderAvatar?: string;
    isSentByCurrentUser?: boolean;
  };
}

// 消息附件接口
export interface MessageAttachment {
  id: number;
  messageId: number;
  type: string;
  url: string;
  name?: string;
  size?: number;
  thumbnailUrl?: string;
}

// 会话参与者接口
export interface ConversationParticipant {
  userId: number;
  alias?: string;
  username?: string;
  nickname?: string;
  avatarUrl?: string;
  user?: User;
}

// 会话接口
export interface Conversation {
  id: number;
  name?: string;
  type: ConversationType;
  avatarUrl?: string;
  lastMessage?: Message;
  lastActiveTime: string;
  unreadCount: number;
  isDnd: boolean;
  isPinned: boolean;
  participants?: ConversationParticipant[];
}

// 发送消息请求
export interface SendMessageRequest {
  conversationId?: number;
  receiverId?: number;
  messageType: MessageType;
  content: string;
  mediaFileId?: number; // 媒体文件ID，用于媒体类型消息
  fileName?: string; // 文件名，用于文件类型消息
  replyToMessageId?: number; // 回复消息ID
  forwardFromMessageId?: number; // 转发消息ID
  metadata?: string; // 消息元数据
  tempMessageId?: string; // 临时消息ID
  attachments?: {
    type: string;
    url: string;
    name?: string;
    size?: number;
    thumbnailUrl?: string;
  }[];
  autoCreateConversation?: boolean; // 是否自动创建会话
}

// 分页响应
export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
  // 添加以下字段以兼容现有代码
  data?: any;
  message?: any;
}

// 会话响应
export interface ConversationResponse {
  success: boolean;
  message: string;
  data?: Conversation;
  conversations?: Conversation[];
}

// 消息响应
export interface MessageResponse {
  success: boolean;
  message?: string;
  data?: Message;
}

// 标记已读请求
export interface MarkAsReadRequest {
  conversationId: number;
  messageIds?: number[];
  lastReadMessageId?: number;
  markAllAsRead?: boolean;
}

// New interface for read cursor updates
export interface UpdateReadCursorRequest {
  conversationId: number;
  lastReadMessageId: number;
}

// 创建会话请求
export interface CreateConversationRequest {
  type: ConversationType;
  participantIds: number[];
  name?: string;
  avatarUrl?: string;
}

// 文件上传响应接口
export interface MediaUploadResponse {
  id?: number;
  fileName?: string;
  originalFileName?: string;
  fileType?: string;
  fileSize?: number;
  fileUrl?: string;
  url?: string;
  thumbnailUrl?: string;
  uploadTime?: string;
  uploaderId?: number;
  status?: string;
  [key: string]: any; // 允许其他字段
}

// 撤回消息请求
export interface RecallMessageRequest {
  messageId: number;
  reason?: string | undefined;
  recallForAll?: boolean;
}

// 编辑消息请求
export interface EditMessageRequest {
  messageId: number;
  content: string;
  editReason?: string | undefined;
}

// 消息搜索请求
export interface MessageSearchRequest {
  conversationId: number;
  keyword: string;
  page?: number;
  size?: number;
}

// 消息搜索结果
export interface MessageSearchResult {
  message: {
    id: number;
    conversationId: number;
    senderId: number;
    content: string;
    messageType?: MessageType;
    type?: MessageType;
    status?: MessageStatus;
    createdAt: string;
    updatedAt?: string;
    senderName?: string;
    senderAvatar?: string;
    senderNickname?: string;
    fileName?: string;
    sender?: {
      id?: number;
      nickname?: string;
      username?: string;
      avatarUrl?: string;
    };
  };
  score?: number;
  highlights?: {
    content?: string[];
    [key: string]: string[] | undefined;
  };
  matchedFields?: string[];
}

// 更新MessageSearchResponse接口定义，使其与后端返回的数据结构匹配
export interface MessageSearchResponse {
  results: MessageSearchResult[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  keyword: string;
  searchTime: number;
  aggregations?: Record<string, any>;
  suggestions?: string[];
}

// 转发消息请求
export interface ForwardMessageRequest {
  messageIds: number[];               // 要转发的消息ID列表
  targetConversationIds: number[];    // 目标会话ID列表
  forwardType: 'MERGE' | 'SEPARATE';  // 转发类型：合并转发或逐条转发
  comment?: string | undefined;       // 可选的附加评论
}

// 全局搜索请求
export interface GlobalSearchRequest {
  keyword: string;
  page?: number;
  size?: number;
  searchTypes?: string[]; // 搜索类型，如 'conversation', 'message'
  conversationIds?: number[]; // 限制搜索的会话ID列表
  messageTypes?: MessageType[]; // 限制搜索的消息类型列表
  sortBy?: string; // 排序字段
  sortDirection?: 'asc' | 'desc'; // 排序方向
  highlight?: boolean; // 是否高亮匹配内容
}

export const messageApi = {
  // 获取会话列表
  async getConversations(userId: number, page: number = 0, size: number = 20): Promise<ApiResponse<any>> {
    console.log(`调用getConversations API，userId=${userId}, page=${page}, size=${size}`);
    
    if (!userId || userId <= 0) {
      console.error('无效的用户ID:', userId);
      return {
        success: false,
        message: '无效的用户ID',
        code: 400,
        data: []
      };
    }
    
    try {
      console.log(`准备发送请求: /conversations?userId=${userId}&page=${page}&size=${size}`);
      
      // 构建请求头
      const headers: Record<string, string> = {
        'X-User-Id': userId.toString(),
        'Content-Type': 'application/json'
      };
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      console.log('请求头:', headers);
      
      const response = await api.get<ApiResponse<any>>(`/conversations?userId=${userId}&page=${page}&size=${size}`, {
        headers
      });
      
      console.log('会话API响应状态:', response.success, '数据结构:', response.data ? typeof response.data : 'null');
      
      if (response.data) {
        console.log('会话数据示例:', JSON.stringify(response.data).substring(0, 200) + '...');
        
        // 检查响应数据结构
        if (response.data.content && response.data.content.length > 0) {
          // 检查是否包含会话列表
          const firstItem = response.data.content[0];
          if (firstItem.conversations && Array.isArray(firstItem.conversations)) {
            console.log(`找到会话列表，包含 ${firstItem.conversations.length} 个会话`);
          } else if (firstItem.conversation) {
            console.log('找到单个会话对象');
          }
        } else if (response.data.conversations && Array.isArray(response.data.conversations)) {
          console.log(`找到会话列表，包含 ${response.data.conversations.length} 个会话`);
        }
      }
      
      return response;
    } catch (error) {
      console.error('获取会话列表失败:', error);
      
      // 增强错误处理
      let errorMessage = '获取会话列表失败';
      let errorCode = 500;
      
      if (error instanceof Error) {
        errorMessage = error.message;
        
        // 处理特定类型的错误
        if ('status' in error) {
          // @ts-ignore
          errorCode = error.status || 500;
          
          // 处理401错误
          // @ts-ignore
          if (error.status === 401) {
            errorMessage = '用户未登录或会话已过期，请重新登录';
          }
          // @ts-ignore
          else if (error.status === 403) {
            errorMessage = '无权限访问会话列表';
          }
        }
      }
      
      return {
        success: false,
        message: errorMessage,
        code: errorCode,
        data: []
      };
    }
  },

  // 获取已归档的会话列表
  async getArchivedConversations(userId: number, page: number = 0, size: number = 20): Promise<ApiResponse<any>> {
    console.log(`调用getArchivedConversations API，userId=${userId}, page=${page}, size=${size}`);
    
    if (!userId || userId <= 0) {
      console.error('无效的用户ID:', userId);
      return {
        success: false,
        message: '无效的用户ID',
        code: 400,
        data: []
      };
    }
    
    try {
      console.log(`准备发送请求: /conversations/archived?userId=${userId}&page=${page}&size=${size}`);
      
      // 构建请求头
      const headers: Record<string, string> = {
        'X-User-Id': userId.toString(),
        'Content-Type': 'application/json'
      };
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      // 发送请求
      const response = await api.get<ApiResponse<any>>(
        `/conversations/archived?page=${page}&size=${size}`,
        { headers }
      );
      
      console.log('已归档会话列表响应:', response);
      return response;
    } catch (error) {
      console.error('获取已归档会话列表失败:', error);
      
      let errorMessage = '获取已归档会话列表失败';
      let errorCode = 500;
      
      if (error instanceof Error) {
        errorMessage = error.message;
        
        if ('status' in error) {
          // @ts-ignore
          errorCode = error.status || 500;
        }
      }
      
      return {
        success: false,
        message: errorMessage,
        code: errorCode,
        data: []
      };
    }
  },

  // 获取指定会话信息
  async getConversation(chatId: number, userId: number): Promise<ApiResponse<Conversation>> {
    return api.get<ApiResponse<Conversation>>(`/conversations/${chatId}?userId=${userId}`, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 获取或创建私聊会话
  async getOrCreatePrivateConversation(contactId: number, userId: number): Promise<ApiResponse<any>> {
    console.log(`调用getOrCreatePrivateConversation API，contactId=${contactId}, userId=${userId}`);
    
    if (!contactId || contactId <= 0 || !userId || userId <= 0) {
      console.error('无效的用户ID或联系人ID:', { userId, contactId });
      return {
        success: false,
        message: '无效的用户ID或联系人ID',
        code: 400,
        data: null
      };
    }
    
    try {
      // 构建请求头
      const headers: Record<string, string> = {
        'X-User-Id': userId.toString(),
        'Content-Type': 'application/json'
      };
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      console.log(`准备发送请求: /conversations/private/${contactId}`);
      const response = await api.get<ApiResponse<any>>(`/conversations/private/${contactId}`, {
        headers
      });
      
      console.log('获取或创建私聊会话响应:', response);
      return response;
    } catch (error) {
      console.error('获取或创建私聊会话失败:', error);
      
      let errorMessage = '获取或创建私聊会话失败';
      let errorCode = 500;
      
      if (error instanceof Error) {
        errorMessage = error.message;
        
        if ('status' in error) {
          // @ts-ignore
          errorCode = error.status || 500;
        }
      }
      
      return {
        success: false,
        message: errorMessage,
        code: errorCode,
        data: null
      };
    }
  },

  // 获取会话消息列表
  async getMessages(conversationId: number, page: number = 0, size: number = 20): Promise<ApiResponse<PageResponse<Message>>> {
    console.log(`调用getMessages API，conversationId=${conversationId}, page=${page}, size=${size}`);
    
    // 构建请求URL
    const url = `/messages/conversation/${conversationId}?page=${page}&size=${size}`;
    
    console.log(`准备发送请求: ${url}`);
    
    try {
      const response = await api.get<ApiResponse<PageResponse<Message>>>(url);
      
      // 添加调试日志
      console.log('getMessages原始响应:', JSON.stringify(response).substring(0, 300) + '...');
      
      // 修复可能的嵌套结构问题
      if (response && response.data) {
        // 检查是否有嵌套的data字段
        if (response.data.data && !response.data.content) {
          console.log('检测到嵌套的data字段，提取到顶层');
          response.data = { 
            ...response.data,
            content: Array.isArray(response.data.data) ? response.data.data : [],
          };
        }
        
        // 检查是否有content字段但为空
        if (!response.data.content) {
          console.log('未检测到content字段，创建空数组');
          response.data.content = [];
        }
        
        // 检查每条消息是否有message嵌套
        if (Array.isArray(response.data.content) && response.data.content.length > 0) {
          console.log(`检测到${response.data.content.length}条消息`);
          
          if (response.data.content[0]?.message) {
            console.log('检测到嵌套的message对象，提取内部属性');
            response.data.content = response.data.content.map(item => {
              if (item && item.message) {
                // 合并消息对象，优先使用外层属性
                return { ...item.message, ...item };
              }
              return item;
            });
          }
        }
        
        // 打印处理后的消息
        console.log(`处理后的消息数量: ${response.data.content ? response.data.content.length : 0}`);
        if (response.data.content && response.data.content.length > 0) {
          console.log('消息示例:', JSON.stringify(response.data.content[0]).substring(0, 200) + '...');
        }
      }
      
      return response;
    } catch (error) {
      console.error('获取消息失败:', error);
      throw error;
    }
  },

  // 发送消息
  async sendMessage(message: SendMessageRequest): Promise<ApiResponse<Message>> {
    console.log('发送消息请求数据:', JSON.stringify(message));
    
    // 确保mediaFileId是数字类型
    if (message.mediaFileId && typeof message.mediaFileId === 'string') {
      message.mediaFileId = Number(message.mediaFileId);
    }
    
    // 确保conversationId是数字类型
    if (message.conversationId && typeof message.conversationId === 'string') {
      message.conversationId = Number(message.conversationId);
    }
    
    // 确保messageType是正确的枚举值
    if (typeof message.messageType === 'string') {
      message.messageType = message.messageType.toUpperCase() as MessageType;
    }
    
    // 创建一个新对象，只包含后端需要的字段
    const requestData = {
      conversationId: message.conversationId,
      recipientId: message.receiverId, // 使用receiverId作为recipientId
      messageType: message.messageType,
      content: message.content,
      mediaFileId: message.mediaFileId,
      replyToMessageId: message.replyToMessageId,
      forwardFromMessageId: message.forwardFromMessageId,
      metadata: message.metadata,
      tempMessageId: message.tempMessageId
    };
    
    console.log('处理后的请求数据:', JSON.stringify(requestData));
    return api.post<ApiResponse<Message>>('/messages/send', requestData);
  },

  // 上传媒体文件
  async uploadMedia(file: any, conversationId: number): Promise<ApiResponse<MediaUploadResponse>> {
    if (!file) {
      throw new Error('文件不能为空');
    }
    
    console.log(`准备上传媒体文件 - 文件名: ${file.name}, 类型: ${file.type}, 大小: ${file.size} 字节, 会话ID: ${conversationId}`);
    
    const formData = new FormData();
    formData.append('file', file);
    formData.append('conversationId', conversationId.toString());
    
    // 添加重试逻辑
    const maxRetries = 3;
    let retryCount = 0;
    let lastError: any = null;
    
    while (retryCount < maxRetries) {
      try {
        console.log(`尝试上传文件 (尝试 ${retryCount + 1}/${maxRetries})...`);
        
        // 获取认证令牌并记录
        const token = localStorage.getItem('accessToken') || 
                     localStorage.getItem('token') || 
                     localStorage.getItem('auth_token') || 
                     sessionStorage.getItem('accessToken') || 
                     sessionStorage.getItem('token') || 
                     sessionStorage.getItem('auth_token');
        
        console.log(`认证令牌: ${token ? '已获取 (' + token.substring(0, 10) + '...)' : '未获取'}`);
        
        // 构建请求头
        const headers: Record<string, string> = {};
        if (token) {
          headers['Authorization'] = `Bearer ${token}`;
        }
        
        // 使用自定义headers发送请求
        const response = await fetch('/api/media/upload', {
          method: 'POST',
          headers,
          body: formData
        });
        
        if (!response.ok) {
          console.error(`上传失败 - HTTP状态: ${response.status} ${response.statusText}`);
          
          // 尝试解析错误响应
          try {
            const errorData = await response.json();
            console.error('服务器错误响应:', errorData);
            throw new Error(errorData.message || `服务器错误 (${response.status})`);
          } catch (e) {
            // 如果无法解析JSON，则抛出原始错误
            throw new Error(`上传失败: ${response.status} ${response.statusText}`);
          }
        }
        
        // 解析成功响应
        const data = await response.json();
        console.log('上传成功 - 服务器响应:', data);
        
        return data;
      } catch (error) {
        lastError = error;
        retryCount++;
        
        console.error(`上传失败，尝试 ${retryCount}/${maxRetries}`, error);
        
        if (retryCount < maxRetries) {
          // 等待一段时间后重试
          const waitTime = 1000 * retryCount;
          console.log(`等待 ${waitTime}ms 后重试...`);
          await new Promise(resolve => setTimeout(resolve, waitTime));
        }
      }
    }
    
    // 所有重试都失败了，抛出最后一个错误
    console.error('所有重试都失败了', lastError);
    throw lastError;
  },
  
  // 获取媒体库
  async getMediaLibrary(
    conversationId?: number, 
    mediaType?: string, 
    page: number = 0, 
    size: number = 20
  ): Promise<ApiResponse<PageResponse<MediaUploadResponse>>> {
    console.log(`获取媒体库 - 会话ID: ${conversationId || '全部'}, 类型: ${mediaType || '全部'}, 页码: ${page}, 大小: ${size}`);
    
    try {
      // 获取当前用户ID
      const userId = getCurrentUserId();
      console.log(`当前用户ID: ${userId}`);
      
      // 构建查询参数
      const params = new URLSearchParams();
      if (conversationId) {
        params.append('conversationId', conversationId.toString());
      }
      if (mediaType) {
        params.append('mediaType', mediaType);
      }
      params.append('page', page.toString());
      params.append('size', size.toString());
      
      // 构建请求头
      const headers: Record<string, string> = {};
      
      // 添加用户ID头
      if (userId) {
        headers['X-User-Id'] = userId.toString();
      }
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      // 发送请求
      const response = await api.get<ApiResponse<PageResponse<MediaUploadResponse>>>(`/media?${params.toString()}`, { headers });
      console.log('获取媒体库成功:', response);
      return response;
    } catch (error) {
      console.error('获取媒体库失败:', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : '获取媒体库失败',
        code: 500,
        data: {} as PageResponse<MediaUploadResponse>
      };
    }
  },
  
  // 获取媒体文件
  async getMediaFile(mediaId: number): Promise<ApiResponse<MediaUploadResponse>> {
    console.log(`获取媒体文件 - ID: ${mediaId}`);
    
    try {
      const response = await api.get<ApiResponse<MediaUploadResponse>>(`/media/files/${mediaId}`);
      console.log('获取媒体文件成功:', response);
      return response;
    } catch (error) {
      console.error('获取媒体文件失败:', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : '获取媒体文件失败',
        code: 500,
        data: {} as MediaUploadResponse
      };
    }
  },
  
  // 删除媒体文件
  async deleteMediaFile(mediaId: number): Promise<ApiResponse<void>> {
    console.log(`删除媒体文件 - ID: ${mediaId}`);
    
    try {
      const response = await api.delete<ApiResponse<void>>(`/media/${mediaId}`);
      console.log('删除媒体文件成功:', response);
      return response;
    } catch (error) {
      console.error('删除媒体文件失败:', error);
      return {
        success: false,
        message: error instanceof Error ? error.message : '删除媒体文件失败',
        code: 500,
        data: undefined
      };
    }
  },

  // 标记消息已读
  async markAsRead(request: MarkAsReadRequest): Promise<ApiResponse<void>> {
    console.log('调用markAsRead API，请求参数:', JSON.stringify(request));
    
    if (!request.conversationId) {
      console.error('无效的会话ID:', request.conversationId);
      return {
        success: false,
        message: '无效的会话ID',
        code: 400,
        data: undefined
      };
    }
    
    try {
      // 构建请求头
      const headers: Record<string, string> = {
        'Content-Type': 'application/json'
      };
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      console.log('发送标记已读请求: /messages/read', request);
      
      const response = await api.put<ApiResponse<void>>(
        '/messages/read', 
        request,
        { headers }
      );
      
      console.log('标记已读响应:', response);
      return response;
    } catch (error) {
      console.error('标记已读请求失败:', error);
      
      let errorMessage = '标记消息已读失败';
      let errorCode = 500;
      
      if (error instanceof Error) {
        errorMessage = error.message;
        
        // 处理特定类型的错误
        if ('status' in error) {
          // @ts-ignore
          errorCode = error.status || 500;
        }
      }
      
      return {
        success: false,
        message: errorMessage,
        code: errorCode,
        data: undefined
      };
    }
  },
  
  // 标记单条消息为已读
  async markMessageAsRead(messageId: number): Promise<ApiResponse<void>> {
    return api.put<ApiResponse<void>>(`/messages/${messageId}/read`);
  },
  
  // 标记整个会话为已读
  async markConversationAsRead(conversationId: number): Promise<ApiResponse<void>> {
    const request: MarkAsReadRequest = {
      conversationId,
      markAllAsRead: true
    };
    return this.markAsRead(request);
  },
  
  // 获取消息已读状态
  async getMessageReadStatus(messageId: number): Promise<ApiResponse<{isRead: boolean}>> {
    return api.get<ApiResponse<{isRead: boolean}>>(`/messages/${messageId}/read-status`);
  },
  
  // 获取消息状态
  async getMessageStatus(messageId: number): Promise<ApiResponse<{status: MessageStatus}>> {
    return api.get<ApiResponse<{status: MessageStatus}>>(`/messages/${messageId}/status`);
  },
  
  // 更新消息状态
  async updateMessageStatus(messageId: number, status: MessageStatus): Promise<ApiResponse<void>> {
    return api.put<ApiResponse<void>>(`/messages/${messageId}/status`, { status });
  },

  // 创建会话
  async createConversation(request: CreateConversationRequest): Promise<ApiResponse<Conversation>> {
    return api.post<ApiResponse<Conversation>>('/conversations', request);
  },

  // 置顶/取消置顶会话
  async pinConversation(conversationId: number, isPinned: boolean): Promise<ApiResponse<void>> {
    console.log(`调用pinConversation API，conversationId=${conversationId}, isPinned=${isPinned}`);
    
    if (!conversationId || conversationId <= 0) {
      console.error('无效的会话ID:', conversationId);
      return {
        success: false,
        message: '无效的会话ID',
        code: 400,
        data: undefined
      };
    }
    
    try {
      // 获取当前用户ID
      const userId = getCurrentUserId();
      console.log('获取到当前用户ID:', userId);
      
      // 如果无法获取用户ID，返回错误
      if (!userId) {
        console.error('无法获取用户ID，无法进行置顶操作');
        return {
          success: false,
          message: '未登录或无法获取用户信息',
          code: 401,
          data: undefined
        };
      }
      
      // 构建请求数据
      const requestData = {
        conversationId: conversationId,
        pinned: isPinned
      };
      
      // 构建请求头
      const headers: Record<string, string> = {
        'X-User-Id': String(userId),
        'Content-Type': 'application/json'
      };
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      console.log(`准备发送置顶请求: /conversations/${conversationId}/pin`, requestData);
      console.log('请求头:', headers);
      
      const response = await api.put<ApiResponse<void>>(
        `/conversations/${conversationId}/pin`, 
        requestData,
        { headers }
      );
      
      console.log('置顶会话响应:', response);
      return response;
    } catch (error) {
      console.error('置顶会话请求失败:', error);
      
      let errorMessage = '置顶会话失败';
      let errorCode = 500;
      
      if (error instanceof Error) {
        errorMessage = error.message;
        
        // 处理特定类型的错误
        if ('status' in error) {
          // @ts-ignore
          errorCode = error.status || 500;
        }
      }
      
      return {
        success: false,
        message: errorMessage,
        code: errorCode,
        data: undefined
      };
    }
  },

  // 设置会话免打扰
  async muteConversation(conversationId: number, isMuted: boolean): Promise<ApiResponse<void>> {
    console.log(`调用muteConversation API，conversationId=${conversationId}, isMuted=${isMuted}`);
    
    if (!conversationId || conversationId <= 0) {
      console.error('无效的会话ID:', conversationId);
      return {
        success: false,
        message: '无效的会话ID',
        code: 400,
        data: undefined
      };
    }
    
    try {
      // 获取当前用户ID
      const userId = getCurrentUserId();
      console.log('获取到当前用户ID:', userId);
      
      // 如果无法获取用户ID，返回错误
      if (!userId) {
        console.error('无法获取用户ID，无法进行免打扰操作');
        return {
          success: false,
          message: '未登录或无法获取用户信息',
          code: 401,
          data: undefined
        };
      }
      
      // 构建请求数据
      const requestData = {
        isMuted
      };
      
      // 构建请求头
      const headers: Record<string, string> = {
        'X-User-Id': String(userId),
        'Content-Type': 'application/json'
      };
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      console.log(`准备发送免打扰请求: /conversations/${conversationId}/mute`, requestData);
      console.log('请求头:', headers);
      
      const response = await api.put<ApiResponse<void>>(
        `/conversations/${conversationId}/mute`, 
        requestData,
        { headers }
      );
      
      console.log('设置免打扰响应:', response);
      return response;
    } catch (error) {
      console.error('设置免打扰请求失败:', error);
      
      let errorMessage = '设置免打扰失败';
      let errorCode = 500;
      
      if (error instanceof Error) {
        errorMessage = error.message;
        
        if ('status' in error) {
          // @ts-ignore
          errorCode = error.status || 500;
        }
      }
      
      return {
        success: false,
        message: errorMessage,
        code: errorCode,
        data: undefined
      };
    }
  },

  // 归档会话
  async archiveConversation(conversationId: number, isArchived: boolean): Promise<ApiResponse<void>> {
    console.log(`调用archiveConversation API，conversationId=${conversationId}, isArchived=${isArchived}`);
    
    if (!conversationId || conversationId <= 0) {
      console.error('无效的会话ID:', conversationId);
      return {
        success: false,
        message: '无效的会话ID',
        code: 400,
        data: undefined
      };
    }
    
    try {
      // 获取当前用户ID
      const userId = getCurrentUserId();
      console.log('获取到当前用户ID:', userId);
      
      // 如果无法获取用户ID，返回错误
      if (!userId) {
        console.error('无法获取用户ID，无法进行归档操作');
        return {
          success: false,
          message: '未登录或无法获取用户信息',
          code: 401,
          data: undefined
        };
      }
      
      // 构建请求数据
      const requestData = {
        isArchived
      };
      
      // 构建请求头
      const headers: Record<string, string> = {
        'X-User-Id': String(userId),
        'Content-Type': 'application/json'
      };
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      console.log(`准备发送归档请求: /conversations/${conversationId}/archive`, requestData);
      console.log('请求头:', headers);
      
      const response = await api.put<ApiResponse<void>>(
        `/conversations/${conversationId}/archive`, 
        requestData,
        { headers }
      );
      
      console.log('归档会话响应:', response);
      
      // 确保响应成功
      if (response && response.success) {
        return response;
      } else {
        throw new Error(response?.message || '归档操作失败');
      }
    } catch (error) {
      console.error('归档会话请求失败:', error);
      
      let errorMessage = '归档会话失败';
      let errorCode = 500;
      
      if (error instanceof Error) {
        errorMessage = error.message;
        
        if ('status' in error) {
          // @ts-ignore
          errorCode = error.status || 500;
        }
      }
      
      return {
        success: false,
        message: errorMessage,
        code: errorCode,
        data: undefined
      };
    }
  },

  // 删除会话
  async deleteConversation(conversationId: number): Promise<ApiResponse<void>> {
    console.log(`调用deleteConversation API，conversationId=${conversationId}`);
    
    if (!conversationId || conversationId <= 0) {
      console.error('无效的会话ID:', conversationId);
      return {
        success: false,
        message: '无效的会话ID',
        code: 400,
        data: undefined
      };
    }
    
    try {
      // 获取当前用户ID
      const userId = getCurrentUserId();
      console.log('获取到当前用户ID:', userId);
      
      // 如果无法获取用户ID，返回错误
      if (!userId) {
        console.error('无法获取用户ID，无法进行删除操作');
        return {
          success: false,
          message: '未登录或无法获取用户信息',
          code: 401,
          data: undefined
        };
      }
      
      // 构建请求头
      const headers: Record<string, string> = {
        'X-User-Id': String(userId),
        'Content-Type': 'application/json'
      };
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      console.log(`准备发送删除请求: /conversations/${conversationId}`);
      console.log('请求头:', headers);
      
      const response = await api.delete<ApiResponse<void>>(
        `/conversations/${conversationId}`,
        { headers }
      );
      
      console.log('删除会话响应:', response);
      return response;
    } catch (error) {
      console.error('删除会话请求失败:', error);
      
      let errorMessage = '删除会话失败';
      let errorCode = 500;
      
      if (error instanceof Error) {
        errorMessage = error.message;
        
        if ('status' in error) {
          // @ts-ignore
          errorCode = error.status || 500;
        }
      }
      
      return {
        success: false,
        message: errorMessage,
        code: errorCode,
        data: undefined
      };
    }
  },

  // 撤回消息
  async recallMessage(messageId: number | string, reason?: string): Promise<ApiResponse<void>> {
    console.log(`调用recallMessage API，messageId=${messageId}, 类型=${typeof messageId}, reason=${reason || '无'}`);
    
    // 确保messageId是数字类型
    let numericMessageId: number;
    
    if (typeof messageId === 'string') {
      if (messageId.startsWith('temp-')) {
        console.error('临时消息ID无法撤回:', messageId);
        return {
          success: false,
          message: '临时消息无法撤回，请等待消息发送完成',
          code: 400,
          data: undefined
        };
      }
      
      try {
        numericMessageId = Number(messageId);
        if (isNaN(numericMessageId)) {
          throw new Error('无效的消息ID');
        }
        console.log('将字符串消息ID转换为数字:', numericMessageId);
      } catch (e) {
        console.error('无法将消息ID转换为数字:', messageId, e);
        return {
          success: false,
          message: '无效的消息ID',
          code: 400,
          data: undefined
        };
      }
    } else if (typeof messageId === 'number') {
      numericMessageId = messageId;
    } else {
      console.error('无效的消息ID类型:', typeof messageId);
      return {
        success: false,
        message: '无效的消息ID类型',
        code: 400,
        data: undefined
      };
    }
    
    if (!numericMessageId || isNaN(numericMessageId) || numericMessageId <= 0) {
      console.error('无效的消息ID:', numericMessageId);
      return {
        success: false,
        message: '无效的消息ID',
        code: 400,
        data: undefined
      };
    }
    
    const request: RecallMessageRequest = {
      messageId: numericMessageId,
      reason
    };
    
    try {
      console.log(`准备发送撤回请求: /messages/${numericMessageId}/recall`, request);
      const response = await api.put<ApiResponse<void>>(`/messages/${numericMessageId}/recall`, request);
      console.log('撤回消息响应:', response);
      return response;
    } catch (error) {
      console.error('撤回消息请求失败:', error);
      throw error;
    }
  },

  // 编辑消息
  async editMessage(messageId: number | string, content: string, editReason?: string): Promise<ApiResponse<void>> {
    console.log(`调用editMessage API，messageId=${messageId}, content=${content}, editReason=${editReason || '无'}`);
    
    // 确保messageId是数字类型
    let numericMessageId: number;
    
    if (typeof messageId === 'string') {
      if (messageId.startsWith('temp-')) {
        console.error('临时消息ID无法编辑:', messageId);
        return {
          success: false,
          message: '临时消息无法编辑，请等待消息发送完成',
          code: 400,
          data: undefined
        };
      }
      
      try {
        numericMessageId = Number(messageId);
        if (isNaN(numericMessageId)) {
          throw new Error('无效的消息ID');
        }
        console.log('将字符串消息ID转换为数字:', numericMessageId);
      } catch (e) {
        console.error('无法将消息ID转换为数字:', messageId, e);
        return {
          success: false,
          message: '无效的消息ID',
          code: 400,
          data: undefined
        };
      }
    } else if (typeof messageId === 'number') {
      numericMessageId = messageId;
    } else {
      console.error('无效的消息ID类型:', typeof messageId);
      return {
        success: false,
        message: '无效的消息ID类型',
        code: 400,
        data: undefined
      };
    }
    
    if (!numericMessageId || isNaN(numericMessageId) || numericMessageId <= 0) {
      console.error('无效的消息ID:', numericMessageId);
      return {
        success: false,
        message: '无效的消息ID',
        code: 400,
        data: undefined
      };
    }
    
    const request: EditMessageRequest = {
      messageId: numericMessageId,
      content: content,
      editReason: editReason
    };
    
    try {
      console.log(`准备发送编辑请求: /messages/${numericMessageId}/edit`, request);
      const response = await api.put<ApiResponse<void>>(`/messages/${numericMessageId}/edit`, request);
      console.log('编辑消息响应:', response);
      return response;
    } catch (error) {
      console.error('编辑消息请求失败:', error);
      throw error;
    }
  },

  // 搜索会话消息
  async searchMessages(request: MessageSearchRequest): Promise<ApiResponse<MessageSearchResponse>> {
    try {
      const { conversationId, keyword, page = 0, size = 20 } = request;
      
      console.log('搜索消息请求参数:', { conversationId, keyword, page, size });
      
      // 获取当前用户ID
      const userId = getCurrentUserId();
      console.log('当前用户ID:', userId);
      
      // 构建请求头
      const headers: Record<string, string> = {
        'Content-Type': 'application/json'
      };
      
      // 添加用户ID头
      if (userId) {
        headers['X-User-Id'] = userId.toString();
        console.log('添加X-User-Id请求头:', userId);
      }
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
        console.log('添加Authorization请求头');
      } else {
        console.warn('未找到认证令牌');
      }
      
      // 构建查询参数
      const params = new URLSearchParams({
        conversationId: String(conversationId),
        keyword: keyword,
        page: String(page),
        size: String(size),
        sortBy: "createdAt",
        sortDirection: "desc",
        highlight: "true"
      });
      
      const url = `/messages/search/conversation?${params.toString()}`;
      console.log('发送搜索请求到:', url);
      console.log('请求头:', headers);
      
      // 使用GET请求
      const response = await api.get<ApiResponse<MessageSearchResponse>>(url, { headers });
      console.log('搜索响应:', response);
      
      if (!response || !response.success) {
        throw new Error(response?.message || '搜索失败');
      }
      
      // 直接返回响应，后端已经返回正确的MessageSearchResponse对象
      return response;
    } catch (error) {
      console.error('搜索消息失败:', error);
      
      // 构建错误响应
      const errorMessage = error instanceof Error ? error.message : '未知错误';
      
      return {
        success: false,
        message: `搜索消息失败: ${errorMessage}`,
        code: 500,
        data: {
          results: [],
          total: 0,
          page: 0,
          size: 0,
          totalPages: 0,
          hasNext: false,
          hasPrevious: false,
          keyword: '',
          searchTime: Date.now()
        }
      };
    }
  },
  
  // 全局搜索
  async globalSearch(request: GlobalSearchRequest): Promise<ApiResponse<MessageSearchResponse>> {
    try {
      const { keyword, page = 0, size = 20, searchTypes = [], conversationIds = [], messageTypes = [] } = request;
      
      console.log('全局搜索请求参数:', { keyword, page, size, searchTypes, conversationIds, messageTypes });
      
      // 获取当前用户ID
      const userId = getCurrentUserId();
      console.log('当前用户ID:', userId);
      
      // 构建请求头
      const headers: Record<string, string> = {
        'Content-Type': 'application/json'
      };
      
      // 添加用户ID头
      if (userId) {
        headers['X-User-Id'] = userId.toString();
      }
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      // 构建请求体
      const requestBody = {
        keyword,
        searchTypes,
        conversationIds,
        messageTypes,
        page,
        size,
        sortBy: request.sortBy || "createdAt",
        sortDirection: request.sortDirection || "desc",
        highlight: request.highlight !== false
      };
      
      console.log('发送全局搜索请求:', requestBody);
      
      // 发送POST请求
      const response = await api.post<ApiResponse<MessageSearchResponse>>(
        '/messages/search/global', 
        requestBody,
        { headers }
      );
      
      console.log('全局搜索响应:', response);
      
      if (!response || !response.success) {
        throw new Error(response?.message || '搜索失败');
      }
      
      return response;
    } catch (error) {
      console.error('全局搜索失败:', error);
      
      // 构建错误响应
      const errorMessage = error instanceof Error ? error.message : '未知错误';
      
      return {
        success: false,
        message: `全局搜索失败: ${errorMessage}`,
        code: 500,
        data: {
          results: [],
          total: 0,
          page: 0,
          size: 0,
          totalPages: 0,
          hasNext: false,
          hasPrevious: false,
          keyword: '',
          searchTime: Date.now()
        }
      };
    }
  },
  
  // 转发消息
  async forwardMessage(request: ForwardMessageRequest): Promise<ApiResponse<void>> {
    try {
      console.log('转发消息请求参数:', request);
      
      // 验证请求参数
      if (!request.messageIds || request.messageIds.length === 0) {
        console.error('无效的消息ID列表');
        return {
          success: false,
          message: '请选择要转发的消息',
          code: 400,
          data: undefined
        };
      }
      
      if (!request.targetConversationIds || request.targetConversationIds.length === 0) {
        console.error('无效的目标会话ID列表');
        return {
          success: false,
          message: '请选择转发目标',
          code: 400,
          data: undefined
        };
      }
      
      // 获取当前用户ID
      const userId = getCurrentUserId();
      console.log('当前用户ID:', userId);
      
      // 构建请求头
      const headers: Record<string, string> = {
        'Content-Type': 'application/json'
      };
      
      // 添加用户ID头
      if (userId) {
        headers['X-User-Id'] = userId.toString();
      }
      
      // 添加认证头
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      console.log('发送转发消息请求:', request);
      const response = await api.post<ApiResponse<void>>('/messages/forward', request, { headers });
      console.log('转发消息响应:', response);
      
      return response;
    } catch (error) {
      console.error('转发消息失败:', error);
      
      // 构建错误响应
      const errorMessage = error instanceof Error ? error.message : '未知错误';
      
      return {
        success: false,
        message: `转发消息失败: ${errorMessage}`,
        code: 500,
        data: undefined
      };
    }
  },

  /**
   * 获取会话中的未读消息数量
   * 
   * @param conversationId 会话ID
   * @returns 未读消息数量
   */
  async getUnreadMessageCount(conversationId: number): Promise<ApiResponse<number>> {
    console.log(`获取会话 ${conversationId} 的未读消息数量`);
    
    try {
      const response = await api.get<ApiResponse<number>>(`/messages/conversation/${conversationId}/unread-count`);
      console.log(`会话 ${conversationId} 的未读消息数量:`, response);
      return response;
    } catch (error) {
      console.error(`获取会话 ${conversationId} 的未读消息数量失败:`, error);
      return {
        success: false,
        message: '获取未读消息数量失败',
        code: 500,
        data: 0
      };
    }
  },

  /**
   * 更新阅读光标（最后读到的消息ID）
   * 
   * @param conversationId 会话ID
   * @param lastReadMessageId 最后读到的消息ID
   * @returns 
   */
  async updateReadCursor(conversationId: number, lastReadMessageId: number): Promise<ApiResponse<void>> {
    console.log(`更新会话 ${conversationId} 的阅读光标: ${lastReadMessageId}`);
    
    try {
      const request: UpdateReadCursorRequest = {
        conversationId,
        lastReadMessageId
      };
      
      const response = await api.put<ApiResponse<void>>(`/conversations/${conversationId}/read-cursor`, request);
      console.log(`更新阅读光标响应:`, response);
      return response;
    } catch (error) {
      console.error(`更新阅读光标失败:`, error);
      return {
        success: false,
        message: '更新阅读光标失败',
        code: 500,
        data: undefined
      };
    }
  }
};