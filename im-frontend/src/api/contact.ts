import { api } from './request';
import type { ApiResponse } from './request';

// 联系人状态
export type ContactStatus = 'PENDING' | 'ACCEPTED' | 'BLOCKED' | 'DELETED';

// 用户信息接口
export interface ContactUser {
  id: number;
  email: string;
  nickname?: string;
  avatarUrl?: string;
  personalId?: string;
  status?: string;
  signature?: string;
}

// 联系人接口
export interface Contact {
  id: number;
  userId: number;
  friendId: number;
  alias?: string;
  status: ContactStatus;
  createdAt: string;
  updatedAt: string;
  friend: ContactUser;
  tags?: ContactTag[];
}

// 联系人标签接口
export interface ContactTag {
  id: number;
  name: string;
  color?: string;
}

// 好友请求接口
export interface FriendRequest {
  requestId: number;
  requesterId: number;
  requesterUsername: string;
  requesterNickname?: string;
  requesterAvatarUrl?: string;
  requesterUserIdStr?: string;
  recipientId: number;
  recipientUsername: string;
  recipientNickname?: string;
  recipientAvatarUrl?: string;
  recipientUserIdStr?: string;
  verificationMessage?: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  statusDescription?: string;
  source?: string;
  createdAt: string;
  processedAt?: string;
  canProcess?: boolean;
  canWithdraw?: boolean;
}

// 添加好友请求接口
export interface AddFriendRequest {
  friendIdentifier: string; // 邮箱或个人ID
  message?: string;
}

// 搜索用户请求接口
export interface SearchUserRequest {
  keyword: string; // 邮箱或个人ID
}

// 设置联系人别名请求接口
export interface SetContactAliasRequest {
  alias: string;
}

// 分配标签请求接口
export interface AssignTagRequest {
  tagIds: number[];
}

export const contactApi = {
  // 获取联系人列表
  async getContacts(userId: number, includeBlocked: boolean = false): Promise<ApiResponse<Contact[]>> {
    return api.get<ApiResponse<Contact[]>>(`/contacts?userId=${userId}&includeBlocked=${includeBlocked}`, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 搜索用户
  async searchUser(searchData: SearchUserRequest, userId: number): Promise<ApiResponse<ContactUser[]>> {
    return api.post<ApiResponse<ContactUser[]>>('/users/search', searchData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 发送好友请求
  async sendFriendRequest(requestData: AddFriendRequest, userId: number): Promise<ApiResponse<FriendRequest>> {
    return api.post<ApiResponse<FriendRequest>>('/contact-requests/send', requestData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 获取好友请求列表
  async getFriendRequests(userId: number, type: 'sent' | 'received' = 'received'): Promise<ApiResponse<FriendRequest[]>> {
    if (type === 'received') {
      return api.get<ApiResponse<FriendRequest[]>>('/contact-requests/received');
    } else {
      return api.get<ApiResponse<FriendRequest[]>>('/contact-requests/sent');
    }
  },

  // 处理好友请求（接受/拒绝）
  async handleFriendRequest(requestId: number, action: 'accept' | 'reject', userId: number): Promise<ApiResponse<string>> {
    return api.post<ApiResponse<string>>(`/contact-requests/${requestId}/${action}`, undefined, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 删除好友
  async deleteFriend(friendId: number, userId: number): Promise<ApiResponse<string>> {
    return api.delete<ApiResponse<string>>(`/contacts/${friendId}`, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 拉黑好友
  async blockFriend(friendId: number, userId: number): Promise<ApiResponse<string>> {
    return api.post<ApiResponse<string>>(`/contacts/${friendId}/block`, undefined, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 取消拉黑
  async unblockFriend(friendId: number, userId: number): Promise<ApiResponse<string>> {
    return api.post<ApiResponse<string>>(`/contacts/${friendId}/unblock`, undefined, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 设置联系人别名
  async setContactAlias(friendId: number, aliasData: SetContactAliasRequest, userId: number): Promise<ApiResponse<string>> {
    // 确保friendId是有效的数字
    if (isNaN(friendId) || friendId <= 0) {
      console.error('无效的联系人ID:', friendId);
      return {
        code: 400,
        message: '无效的联系人ID',
        data: '',
        success: false
      };
    }
    
    console.log('设置联系人别名:', {
      friendId,
      alias: aliasData.alias,
      userId
    });
    
    return api.put<ApiResponse<string>>(`/contacts/${friendId}/alias`, aliasData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 获取联系人标签
  async getContactTags(userId: number): Promise<ApiResponse<ContactTag[]>> {
    return api.get<ApiResponse<ContactTag[]>>(`/contact-tags?userId=${userId}`, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 创建联系人标签
  async createContactTag(tagData: { name: string; color?: string }, userId: number): Promise<ApiResponse<ContactTag>> {
    return api.post<ApiResponse<ContactTag>>('/contact-tags', tagData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 更新联系人标签
  async updateContactTag(tagId: number, tagData: { name: string; color?: string }, userId: number): Promise<ApiResponse<ContactTag>> {
    return api.put<ApiResponse<ContactTag>>(`/contact-tags/${tagId}`, tagData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 删除联系人标签
  async deleteContactTag(tagId: number, userId: number): Promise<ApiResponse<string>> {
    return api.delete<ApiResponse<string>>(`/contact-tags/${tagId}`, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  },

  // 为联系人分配标签
  async assignContactTags(friendId: number, tagData: AssignTagRequest, userId: number): Promise<ApiResponse<string>> {
    // 确保friendId是有效的数字
    if (isNaN(friendId) || friendId <= 0) {
      console.error('无效的联系人ID:', friendId);
      return {
        code: 400,
        message: '无效的联系人ID',
        data: '',
        success: false
      };
    }
    
    console.log('为联系人分配标签:', {
      friendId,
      tagIds: tagData.tagIds,
      userId
    });
    
    return api.put<ApiResponse<string>>(`/contacts/${friendId}/tags`, tagData, {
      headers: {
        'X-User-Id': userId.toString()
      }
    });
  }
};