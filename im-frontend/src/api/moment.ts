import { api } from '@/api/request';

// 动态API模块

/**
 * 获取用户动态列表
 * @param userId 用户ID
 * @param page 页码
 * @param size 每页大小
 */
export const getUserMoments = async (userId: number, page = 0, size = 10) => {
  console.log(`【getUserMoments】获取用户${userId}的动态列表, 页码: ${page}, 大小: ${size}`);
  
  // 获取当前用户ID
  const currentUserId = localStorage.getItem('userId') || 
                        sessionStorage.getItem('userId') || 
                        (localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo') || '{}').id : null);
  
  console.log(`【getUserMoments】当前用户ID: ${currentUserId}, 请求的用户ID: ${userId}`);
  
  try {
    // 确保发送了认证头和用户ID头
    const headers: Record<string, string> = {};
    
    // 添加用户ID头
    if (currentUserId) {
      headers['X-User-Id'] = String(currentUserId);
    }
    
    console.log(`【getUserMoments】请求头: `, headers);
    
    const response = await api.get(`/moments/user/${userId}?page=${page}&size=${size}`, {
      headers
    });
    console.log('【getUserMoments】请求成功, 响应:', response);
    
    // 检查响应中是否包含私有动态
    if (response?.data?.content) {
      const privateCount = response.data.content.filter((m: any) => m.visibilityType === 'PRIVATE').length;
      console.log(`【getUserMoments】响应中包含${privateCount}条私有动态, 总共${response.data.content.length}条动态`);
    }
    
    return response;
  } catch (error) {
    console.error('【getUserMoments】请求失败:', error);
    throw error;
  }
};

/**
 * 获取好友动态时间线
 * @param page 页码
 * @param size 每页大小
 * @returns 包含自己和好友的所有动态（后端会根据可见性规则过滤）
 */
export const getMomentTimeline = async (page = 0, size = 10) => {
  console.log(`【getMomentTimeline】获取动态时间线, 页码: ${page}, 大小: ${size}`);
  
  // 获取当前用户ID
  const currentUserId = localStorage.getItem('userId') || 
                        sessionStorage.getItem('userId') || 
                        (localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo') || '{}').id : null);
  
  console.log(`【getMomentTimeline】当前用户ID: ${currentUserId}`);
  
  try {
    const response = await api.get(`/moments/timeline?page=${page}&size=${size}`, {
      headers: {
        'X-User-Id': currentUserId
      }
    });
    console.log('【getMomentTimeline】请求成功, 响应:', response);
    return response;
  } catch (error) {
    console.error('【getMomentTimeline】请求失败:', error);
    throw error;
  }
};

/**
 * 获取动态详情
 * @param momentId 动态ID
 */
export const getMomentDetail = async (momentId: number) => {
  return await api.get(`/moments/${momentId}`);
};

/**
 * 创建动态
 * @param data 动态数据
 */
export const createMoment = async (data: {
  content?: string;
  mediaUrls?: string[];
  mediaType: 'TEXT' | 'IMAGE' | 'VIDEO';
  visibilityType: 'PUBLIC' | 'PRIVATE' | 'CUSTOM';
  visibilityRules?: {
    allowedUserIds?: number[];
    blockedUserIds?: number[];
  };
}) => {
  console.log('【createMoment】准备发送请求，数据:', data);
  
  // 获取用户ID
  const userId = localStorage.getItem('userId') || 
                sessionStorage.getItem('userId') || 
                (localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo') || '{}').id : null);
  
  console.log('【createMoment】当前用户ID:', userId);
  
  try {
    const response = await api.post('/moments', data, {
      headers: {
        'X-User-Id': userId
      }
    });
    console.log('【createMoment】请求成功，响应数据:', response);
    return response;
  } catch (error) {
    console.error('【createMoment】请求失败:', error);
    throw error;
  }
};

/**
 * 上传图片文件
 * @param files 图片文件列表
 */
export const uploadMomentImages = async (files: File[]) => {
  const formData = new FormData();
  files.forEach(file => {
    formData.append('files', file);
  });
  
  return await api.post('/moments/upload/images', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};

/**
 * 上传视频文件
 * @param file 视频文件
 */
export const uploadMomentVideo = async (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  
  return await api.post('/moments/upload/video', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};

/**
 * 更新动态内容
 * @param momentId 动态ID
 * @param content 内容
 */
export const updateMoment = async (momentId: number, content: string) => {
  return await api.put(`/moments/${momentId}`, content);
};

/**
 * 删除动态
 * @param momentId 动态ID
 */
export const deleteMoment = async (momentId: number) => {
  return await api.delete(`/moments/${momentId}`);
};

/**
 * 点赞动态
 * @param momentId 动态ID
 */
export const likeMoment = async (momentId: number) => {
  return await api.post(`/moments/${momentId}/like`);
};

/**
 * 取消点赞
 * @param momentId 动态ID
 */
export const unlikeMoment = async (momentId: number) => {
  return await api.post(`/moments/${momentId}/unlike`);
};

/**
 * 获取动态点赞列表
 * @param momentId 动态ID
 * @param page 页码
 * @param size 每页大小
 */
export const getMomentLikes = async (momentId: number, page = 0, size = 10) => {
  return await api.get(`/moments/${momentId}/likes?page=${page}&size=${size}`);
};

/**
 * 评论动态
 * @param momentId 动态ID
 * @param content 评论内容
 * @param isPrivate 是否为私密评论
 */
export const commentMoment = async (
  momentId: number,
  content: string,
  isPrivate = false
) => {
  // 获取当前用户ID
  const userId = localStorage.getItem('userId') || 
                sessionStorage.getItem('userId') || 
                (localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo') || '{}').id : null);
  
  console.log('【commentMoment】当前用户ID:', userId);
  
  try {
    const response = await api.post(`/moments/${momentId}/comments`, {
      content,
      isPrivate
    }, {
      headers: {
        'X-User-Id': userId
      }
    });
    console.log('【commentMoment】请求成功，响应数据:', response);
    return response;
  } catch (error) {
    console.error('【commentMoment】请求失败:', error);
    throw error;
  }
};

/**
 * 回复评论
 * @param momentId 动态ID
 * @param commentId 评论ID
 * @param content 评论内容
 * @param replyToUserId 回复的用户ID (可选)
 * @param isPrivate 是否为私密回复
 */
export const replyComment = async (
  momentId: number,
  commentId: number,
  content: string,
  replyToUserId?: number,
  isPrivate = false
) => {
  // 获取当前用户ID
  const userId = localStorage.getItem('userId') || 
                sessionStorage.getItem('userId') || 
                (localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo') || '{}').id : null);
  
  console.log('【replyComment】当前用户ID:', userId);
  
  try {
    const response = await api.post(`/moments/${momentId}/comments/${commentId}/replies`, {
      content,
      replyToUserId,
      isPrivate
    }, {
      headers: {
        'X-User-Id': userId
      }
    });
    console.log('【replyComment】请求成功，响应数据:', response);
    return response;
  } catch (error) {
    console.error('【replyComment】请求失败:', error);
    throw error;
  }
};

/**
 * 获取动态评论
 * @param momentId 动态ID
 * @param page 页码
 * @param size 每页大小
 */
export const getMomentComments = async (momentId: number, page = 0, size = 20) => {
  // 获取当前用户ID
  const userId = localStorage.getItem('userId') || 
                sessionStorage.getItem('userId') || 
                (localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo') || '{}').id : null);
                
  return await api.get(`/moments/${momentId}/comments?page=${page}&size=${size}`, {
    headers: {
      'X-User-Id': userId
    }
  });
};

/**
 * 获取带有回复的评论
 * @param momentId 动态ID
 */
export const getMomentCommentsWithReplies = async (momentId: number) => {
  // 获取当前用户ID
  const userId = localStorage.getItem('userId') || 
                sessionStorage.getItem('userId') || 
                (localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo') || '{}').id : null);
                
  return await api.get(`/moments/${momentId}/comments/with-replies`, {
    headers: {
      'X-User-Id': userId
    }
  });
};

/**
 * 删除评论
 * @param momentId 动态ID
 * @param commentId 评论ID
 */
export const deleteComment = async (momentId: number, commentId: number) => {
  // 获取当前用户ID
  const userId = localStorage.getItem('userId') || 
                sessionStorage.getItem('userId') || 
                (localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo') || '{}').id : null);
                
  return await api.delete(`/moments/${momentId}/comments/${commentId}`, {
    headers: {
      'X-User-Id': userId
    }
  });
};