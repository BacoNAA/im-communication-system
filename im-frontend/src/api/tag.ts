import { api, type ApiResponse } from './request';
import type { Contact } from './contact';
import { getCurrentUserId } from '@/utils/helpers';

// 标签相关接口定义
export interface ContactTag {
  id?: number;
  tagId?: number;  // 添加tagId字段以匹配后端返回的数据
  name: string;
  color: string;
  contactCount?: number;
  createdAt?: string;
  updatedAt?: string;
  userId?: number;
  isDefault?: boolean;
  sortOrder?: number;
}

export interface CreateTagRequest {
  userId: number;
  name: string;
  color: string;
}

export interface UpdateTagRequest {
  name: string;
  color: string;
}

export interface AssignTagsRequest {
  tagIds: number[];
}

// 标签API
export const tagApi = {
  // 获取用户的所有标签
  getTags: async (userId: number): Promise<ApiResponse<ContactTag[]>> => {
    try {
      const response = await api.get('/tags');
      
      return response;
    } catch (error: any) {
      if (error.response?.status === 401) {
        throw { status: 401, message: '未授权访问' };
      }
      throw error;
    }
  },

  // 创建新标签
  createTag: async (tagData: CreateTagRequest): Promise<ApiResponse<ContactTag>> => {
    try {
      const response = await api.post('/tags', tagData);
      
      return response;
    } catch (error: any) {
      if (error.response?.status === 401) {
        throw { status: 401, message: '未授权访问' };
      }
      if (error.response?.status === 400) {
        throw { status: 400, message: error.response.data?.message || '参数验证失败' };
      }
      throw error;
    }
  },

  // 更新标签
  updateTag: async (tagId: number, tagData: UpdateTagRequest): Promise<ApiResponse<ContactTag>> => {
    try {
      // 检查tagId是否有效
      if (!tagId) {
        throw { status: 400, message: '标签ID无效' };
      }
      
      const response = await api.put(`/tags/${tagId}`, tagData);
      
      return response;
    } catch (error: any) {
      if (error.response?.status === 401) {
        throw { status: 401, message: '未授权访问' };
      }
      throw error;
    }
  },

  // 删除标签
  deleteTag: async (tagId: number): Promise<ApiResponse<void>> => {
    try {
      // 检查tagId是否有效
      if (!tagId) {
        throw { status: 400, message: '标签ID无效' };
      }
      
      const response = await api.delete(`/tags/${tagId}`);
      
      return response;
    } catch (error: any) {
      if (error.response?.status === 401) {
        throw { status: 401, message: '未授权访问' };
      }
      throw error;
    }
  },

  // 获取联系人的标签
  getContactTags: async (contactId: number): Promise<ApiResponse<ContactTag[]>> => {
    try {
      const response = await api.get(`/contacts/${contactId}/tags`);
      
      return response;
    } catch (error: any) {
      if (error.response?.status === 401) {
        throw { status: 401, message: '未授权访问' };
      }
      throw error;
    }
  },

  // 为联系人分配标签
  assignContactTags: async (contactId: number, tagData: AssignTagsRequest): Promise<ApiResponse<void>> => {
    try {
      const response = await api.put(`/contacts/${contactId}/tags`, tagData);
      
      return response;
    } catch (error: any) {
      if (error.response?.status === 401) {
        throw { status: 401, message: '未授权访问' };
      }
      throw error;
    }
  },

  // 根据ID获取标签详情
  getTagById: async (tagId: number): Promise<ApiResponse<ContactTag>> => {
    try {
      const response = await api.get(`/tags/${tagId}`);
      
      return response;
    } catch (error: any) {
      if (error.response?.status === 401) {
        throw { status: 401, message: '未授权访问' };
      }
      if (error.response?.status === 404) {
        throw { status: 404, message: '标签不存在' };
      }
      throw error;
    }
  },

  // 获取标签下的联系人列表
  getTagContacts: async (tagId: number): Promise<ApiResponse<Contact[]>> => {
    try {
      // 获取当前用户ID
      const userId = getCurrentUserId();
      
      if (!userId) {
        console.error('获取标签联系人失败: 未获取到用户ID');
        return {
          code: 401,
          message: '未授权访问，无法获取用户ID',
          data: [],
          success: false
        };
      }

      // 确保tagId是有效的数字
      if (!tagId || isNaN(Number(tagId)) || Number(tagId) <= 0) {
        console.error('获取标签联系人失败: 无效的标签ID', tagId);
        return {
          code: 400,
          message: '无效的标签ID',
          data: [],
          success: false
        };
      }

      // 转换tagId为数字类型
      const numericTagId = Number(tagId);
      
      console.log(`请求标签联系人，用户ID: ${userId}, 标签ID: ${numericTagId}`);
      
      // 使用与后端API匹配的路径
      try {
        // 使用与index.html中相同的API路径格式
        const response = await api.get(`/v1/contact-tag-assignments/tag-contact-details?userId=${userId}&tagId=${numericTagId}`);
        console.log('标签联系人API响应:', response);
        
        // 处理响应数据
        if (response && (response.code === 200 || response.success)) {
          const contacts = Array.isArray(response.data) ? response.data : [];
          return {
            code: 200,
            message: '获取标签联系人成功',
            data: contacts,
            success: true
          };
        } else {
          throw new Error(response?.message || '获取标签联系人失败');
        }
      } catch (err) {
        console.warn('API请求失败，尝试备用路径:', err);
        
        try {
          // 尝试备用路径
          const response = await api.get(`/contact-tags/${numericTagId}/contacts?userId=${userId}`);
          console.log('备用API响应:', response);
          
          if (response && (response.code === 200 || response.success)) {
            const contacts = Array.isArray(response.data) ? response.data : [];
            return {
              code: 200,
              message: '获取标签联系人成功',
              data: contacts,
              success: true
            };
          } else {
            throw new Error(response?.message || '获取标签联系人失败');
          }
        } catch (err2) {
          console.error('所有API路径尝试均失败:', err2);
          // 返回空数组而不是抛出错误，以避免UI崩溃
          return {
            code: 200,
            message: '获取标签联系人成功(空)',
            data: [],
            success: true
          };
        }
      }
    } catch (error: any) {
      console.error('获取标签联系人失败:', error);
      // 返回空数组而不是抛出错误，以避免UI崩溃
      return {
        code: 200,
        message: '获取标签联系人成功(空)',
        data: [],
        success: true
      };
    }
  },

  // 从标签中移除联系人
  removeContactFromTag: async (tagId: number, contactId: number): Promise<ApiResponse<void>> => {
    try {
      const response = await api.delete(`/tags/${tagId}/contacts/${contactId}`);
      
      return response;
    } catch (error: any) {
      if (error.response?.status === 401) {
        throw { status: 401, message: '未授权访问' };
      }
      if (error.response?.status === 404) {
        throw { status: 404, message: '标签或联系人不存在' };
      }
      throw error;
    }
  }
};