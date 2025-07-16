import { api } from './index';
import type { ApiResponse } from './index';

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
  READ = 'READ',
  FAILED = 'FAILED'
}

// 消息接口
export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  type: MessageType;
  content: string;
  status: MessageStatus;
  createdAt: string;
  updatedAt: string;
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
    status: MessageStatus;
    createdAt: string;
    updatedAt: string;
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
  recipientId?: number;
  messageType: MessageType;
  content: string;
  attachments?: {
    type: string;
    url: string;
    name?: string;
    size?: number;
    thumbnailUrl?: string;
  }[];
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
  messageId?: number;
}

// 创建会话请求
export interface CreateConversationRequest {
  type: ConversationType;
  participantIds: number[];
  name?: string;
  avatarUrl?: string;
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
    
    return api.get<ApiResponse<PageResponse<Message>>>(url);
  },

  // 发送消息
  async sendMessage(message: SendMessageRequest): Promise<ApiResponse<Message>> {
    return api.post<ApiResponse<Message>>('/messages/send', message);
  },

  // 标记消息已读
  async markAsRead(request: MarkAsReadRequest): Promise<ApiResponse<void>> {
    return api.put<ApiResponse<void>>('/messages/read', request);
  },

  // 创建会话
  async createConversation(request: CreateConversationRequest): Promise<ApiResponse<Conversation>> {
    return api.post<ApiResponse<Conversation>>('/conversations', request);
  },

  // 置顶/取消置顶会话
  async pinConversation(conversationId: number, isPinned: boolean): Promise<ApiResponse<void>> {
    return api.put<ApiResponse<void>>(`/conversations/${conversationId}/pin`, { isPinned });
  },

  // 设置会话免打扰
  async muteConversation(conversationId: number, isMuted: boolean): Promise<ApiResponse<void>> {
    return api.put<ApiResponse<void>>(`/conversations/${conversationId}/mute`, { isMuted });
  },

  // 归档会话
  async archiveConversation(conversationId: number, isArchived: boolean): Promise<ApiResponse<void>> {
    return api.put<ApiResponse<void>>(`/conversations/${conversationId}/archive`, { isArchived });
  },

  // 删除会话
  async deleteConversation(conversationId: number): Promise<ApiResponse<void>> {
    return api.delete<ApiResponse<void>>(`/conversations/${conversationId}`);
  }
};