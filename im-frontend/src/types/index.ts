// 基础类型定义
export interface User {
  id: number;
  email: string;
  nickname: string;
  avatarUrl?: string;
  signature?: string;
  userIdStr?: string;
  userIdString?: string; // 个人ID字符串
  statusJson?: string;
  phoneNumber?: string;
  gender?: string;
  birthday?: string;
  location?: string;
  occupation?: string;
  createdAt?: string;
  updatedAt?: string;
  // 前端扩展字段
  name?: string; // 显示名称，通常使用nickname
  avatar?: string; // 头像URL的别名，映射到avatarUrl
  registerTime?: string; // 注册时间的别名，映射到createdAt
  isOnline?: boolean; // 在线状态
  status?: 'online' | 'offline' | 'away' | 'busy'; // 简化的状态
}

export interface Message {
  id: string;
  senderId: number;
  receiverId: number;
  content: string;
  type: 'text' | 'image' | 'file' | 'voice';
  timestamp: Date;
  status: 'sent' | 'delivered' | 'read';
}

export interface Chat {
  id: string;
  conversationType: 'PRIVATE' | 'GROUP' | 'SYSTEM';
  name?: string;
  description?: string;
  avatarUrl?: string;
  createdBy: number;
  lastActiveAt?: string;
  lastMessageId?: number;
  deleted?: boolean;
  settings?: string;
  metadata?: string;
  createdAt?: string;
  updatedAt?: string;
  deletedAt?: string;
  participants?: User[];
  // 前端扩展字段
  lastMessage?: Message | string; // 最后一条消息内容
  lastMessageTime?: string; // 最后消息时间的别名，映射到lastActiveAt
  unreadCount?: number; // 未读消息数
  type?: 'private' | 'group'; // 类型的别名，映射到conversationType
  avatar?: string; // 头像URL的别名，映射到avatarUrl
}

export interface ApiResponse<T = unknown> {
  success: boolean
  data?: T
  message?: string
  error?: string
}

export interface PaginationParams {
  page: number;
  limit: number;
}

export interface PaginatedResponse<T> extends ApiResponse<T[]> {
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
}

// 文件相关类型定义
export interface FileItem {
  fileId: string; // 主要ID字段，后端返回
  id?: string; // 兼容字段
  originalFilename: string; // 主要文件名字段，后端返回
  originalName?: string; // 兼容字段
  fileName?: string; // 兼容字段
  storedFilename?: string; // 后端返回的存储文件名
  fileType: string;
  fileSize: number;
  createdAt: string; // 主要时间字段，后端返回
  uploadTime?: string; // 兼容字段
  createTime?: string; // 兼容字段
  fileUrl?: string; // 文件访问URL
  mimeType?: string; // MIME类型
  contentType?: string; // 内容类型
}

export interface FileStats {
  imageCount: number;
  videoCount: number;
  documentCount: number;
  audioCount: number;
  totalFiles: number;
  totalSize: number;
}

export interface FileUploadResult {
  success: boolean;
  message?: string;
  data?: {
    fileId: string;
    fileName: string;
    fileUrl: string;
  };
}
