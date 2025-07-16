import { api, type ApiResponse } from './request';

// 用户资料接口
export interface UserProfile {
  id: number;
  email: string;
  nickname: string;  // 对应后端的nickname字段
  avatar?: string;
  userIdString?: string;  // 个人ID字段
  status?: string;
  signature?: string;
  phoneNumber?: string;  // 对应后端的phoneNumber字段
  birthday?: string;
  gender?: string;
  location?: string;
  occupation?: string;  // 对应后端的occupation字段
  
  // 为了兼容性，保留旧字段名作为别名
  name?: string;  // 映射到nickname
  phone?: string;  // 映射到phoneNumber
  personalId?: string;  // 映射到userIdString
}

// 用户状态接口
export interface UserStatus {
  status: string;
  signature?: string;
}

// 个人ID接口
export interface PersonalId {
  personalId: string;
}

export const userApi = {
  // 获取用户资料
  async getProfile(): Promise<ApiResponse<UserProfile>> {
    return api.get<ApiResponse<UserProfile>>('/user/profile');
  },

  // 更新用户基本资料
  async updateProfile(profileData: Partial<UserProfile>): Promise<ApiResponse<UserProfile>> {
    return api.put<ApiResponse<UserProfile>>('/user/profile', profileData);
  },

  // 设置个人ID
  async setPersonalId(userIdString: string): Promise<ApiResponse<string>> {
    return api.post<ApiResponse<string>>('/user/personal-id', { userIdString });
  },

  // 更新用户状态
  async updateStatus(statusData: UserStatus): Promise<ApiResponse<string>> {
    return api.post<ApiResponse<string>>('/user/status', statusData);
  },

  // 上传头像
  async uploadAvatar(file: File): Promise<ApiResponse<{ avatarUrl: string }>> {
    const formData = new FormData();
    formData.append('avatar', file);

    return api.post<ApiResponse<{ avatarUrl: string }>>('/user/avatar', formData);
  }
};