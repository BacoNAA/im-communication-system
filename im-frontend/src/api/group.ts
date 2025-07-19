import { api, type ApiResponse } from './request';

interface QueryParams {
  page?: number;
  size?: number;
  sort?: string;
  keyword?: string;
}

/**
 * 创建群组
 * @param {Object} data - 群组数据
 * @returns {Promise}
 */
export async function createGroup(data: {
  name: string;
  description?: string;
  avatar?: string;
  avatarUrl?: string;
  memberIds?: number[];
  requiresApproval?: boolean;
  creatorId?: number;
}): Promise<ApiResponse<any>> {
  console.log('createGroup 函数被调用，参数:', data);
  
  let userId = 0;
  
  if (data.creatorId && data.creatorId > 0) {
    userId = data.creatorId;
  } else {
    const localUserId = localStorage.getItem('userId');
    if (localUserId) {
      const parsedId = parseInt(localUserId);
      if (!isNaN(parsedId) && parsedId > 0) {
        userId = parsedId;
      }
    }
    
    if (userId === 0) {
      const sessionUserId = sessionStorage.getItem('userId');
      if (sessionUserId) {
        const parsedId = parseInt(sessionUserId);
        if (!isNaN(parsedId) && parsedId > 0) {
          userId = parsedId;
        }
      }
    }
    
    if (userId === 0) {
      const userInfoStr = localStorage.getItem('userInfo');
      if (userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr);
          if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
            userId = userInfo.id;
          }
        } catch (e) {
          console.error('解析localStorage.userInfo失败:', e);
        }
      }
    }
    
    if (userId === 0) {
      const userInfoStr = sessionStorage.getItem('userInfo');
      if (userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr);
          if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
            userId = userInfo.id;
          }
        } catch (e) {
          console.error('解析sessionStorage.userInfo失败:', e);
        }
      }
    }
  }
  
  console.log('获取到的用户ID:', userId);
  
  if (userId === 0) {
    console.error('未找到有效的用户ID，无法创建群组');
    throw new Error('未找到有效的用户ID，请先登录');
  }
  
  let memberIds = data.memberIds?.map(id => Number(id)) || [];
  if (userId > 0 && !memberIds.includes(userId)) {
    memberIds.push(userId);
  }
  
  const payload = {
    name: data.name,
    description: data.description || '',
    avatarUrl: data.avatar || data.avatarUrl || '',
    memberIds: memberIds,
    requiresApproval: data.requiresApproval !== undefined ? data.requiresApproval : false,
    creatorId: userId
  };
  
  console.log('createGroup API调用，发送数据:', JSON.stringify(payload, null, 2));
  
  const authTokens = [
    localStorage.getItem('accessToken'),
    localStorage.getItem('auth_token'),
    localStorage.getItem('token'),
    sessionStorage.getItem('accessToken'),
    sessionStorage.getItem('auth_token'),
    sessionStorage.getItem('token')
  ].filter(Boolean);
  
  const token = authTokens[0] || '';
  
  if (!token) {
    console.error('未找到有效的认证令牌，请先登录');
    throw new Error('未找到有效的认证令牌，请先登录');
  }
  
  console.log('使用认证令牌:', token.substring(0, 15) + '...');
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'X-User-Id': String(userId)
  };
  
  console.log('添加用户ID到请求头:', userId);
  
  try {
    console.log('发送POST请求到 /groups');
    console.log('请求头:', headers);
    console.log('请求体:', payload);
    
    const response = await api.post('/groups', payload, { headers });
    console.log('API调用成功:', response);
    return response;
  } catch (error) {
    console.error('API调用失败:', error);
    
    if (error && typeof error === 'object' && 'code' in error && error.code === 401) {
      throw new Error('登录已过期，请重新登录');
    }
    
    throw error;
  }
}

/**
 * 更新群组信息
 * @param {number} groupId - 群组ID
 * @param {Object} data - 群组更新数据
 * @returns {Promise}
 */
export async function updateGroup(groupId: number, data: {
  name?: string;
  description?: string;
  avatarUrl?: string;
  requiresApproval?: boolean;
}): Promise<ApiResponse<any>> {
  console.log('updateGroup API调用，群组ID:', groupId, '发送数据:', JSON.stringify(data, null, 2));
  
  // 获取用户ID
  let userId = 0;
  const userInfoStr = localStorage.getItem('userInfo');
  if (userInfoStr) {
    try {
      const userInfo = JSON.parse(userInfoStr);
      if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
        userId = userInfo.id;
      }
    } catch (e) {
      console.error('解析localStorage.userInfo失败:', e);
    }
  }
  
  // 获取认证令牌
  const authTokens = [
    localStorage.getItem('accessToken'),
    localStorage.getItem('auth_token'),
    localStorage.getItem('token'),
    sessionStorage.getItem('accessToken'),
    sessionStorage.getItem('auth_token'),
    sessionStorage.getItem('token')
  ].filter(Boolean);
  
  const token = authTokens[0] || '';
  
  if (!token) {
    console.error('未找到有效的认证令牌，请先登录');
    throw new Error('未找到有效的认证令牌，请先登录');
  }
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'X-User-Id': String(userId)
  };
  
  try {
    console.log('发送PUT请求到 /groups/' + groupId);
    console.log('请求头:', headers);
    console.log('请求体:', data);
    
    const response = await api.put(`/groups/${groupId}`, data, { headers });
    console.log('API调用成功:', response);
    return response;
  } catch (error) {
    console.error('API调用失败:', error);
    
    if (error && typeof error === 'object' && 'code' in error && error.code === 401) {
      throw new Error('登录已过期，请重新登录');
    }
    
    throw error;
  }
}

/**
 * 获取群组详情
 * @param {number} groupId - 群组ID
 * @returns {Promise}
 */
export async function getGroupById(groupId: number): Promise<ApiResponse<any>> {
  console.log('getGroupById API调用:', { groupId });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送GET请求到:', `/groups/${groupId}`);
    console.log('请求头:', headers);
    
    const response = await api.get(`/groups/${groupId}`, { headers });
    console.log('获取群组详情API调用成功:', response);
    return response;
  } catch (error) {
    console.error('获取群组详情API调用失败:', error);
    throw error;
  }
}

/**
 * 获取用户的群组列表
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export async function getUserGroups(params: {
  page?: number;
  size?: number;
} = {}): Promise<ApiResponse<any>> {
  console.log('getUserGroups API调用:', { params });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送GET请求到:', `/groups`);
    console.log('请求头:', headers);
    console.log('请求参数:', params);
    
    const response = await api.get('/groups', { body: params, headers });
    console.log('获取用户群组API调用成功:', response);
    return response;
  } catch (error) {
    console.error('获取用户群组API调用失败:', error);
    throw error;
  }
}

/**
 * 解散群组
 * @param {number} groupId - 群组ID
 * @returns {Promise}
 */
export async function dissolveGroup(groupId: number): Promise<ApiResponse<any>> {
  console.log('dissolveGroup API调用:', { groupId });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送DELETE请求到:', `/groups/${groupId}`);
    console.log('请求头:', headers);
    
    const response = await api.delete(`/groups/${groupId}`, { headers });
    console.log('解散群组API调用成功:', response);
    return response;
  } catch (error) {
    console.error('解散群组API调用失败:', error);
    throw error;
  }
}

/**
 * 获取群组成员列表
 * @param {number} groupId - 群组ID
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export async function getGroupMembers(groupId: number, params: {
  page?: number;
  size?: number;
  keyword?: string;
} = {}): Promise<ApiResponse<any>> {
  console.log('getGroupMembers API调用:', { groupId, params });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送GET请求到:', `/groups/${groupId}/members`);
    console.log('请求头:', headers);
    console.log('请求参数:', params);
    
    const response = await api.get(`/groups/${groupId}/members`, { body: params, headers });
    console.log('获取群成员API调用成功:', response);
    return response;
  } catch (error) {
    console.error('获取群成员API调用失败:', error);
    throw error;
  }
}

/**
 * 获取群管理员列表
 * @param {number} groupId - 群组ID
 * @returns {Promise}
 */
export async function getGroupAdmins(groupId: number): Promise<ApiResponse<any>> {
  console.log('getGroupAdmins API调用:', { groupId });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送GET请求到:', `/groups/${groupId}/members/admins`);
    console.log('请求头:', headers);
    
    const response = await api.get(`/groups/${groupId}/members/admins`, { headers });
    console.log('获取群管理员API调用成功:', response);
    return response;
  } catch (error) {
    console.error('获取群管理员API调用失败:', error);
    throw error;
  }
}

/**
 * 添加群成员
 * @param {number} groupId - 群组ID
 * @param {Array} memberIds - 成员ID列表
 * @returns {Promise}
 */
export async function addGroupMembers(groupId: number, memberIds: number[]): Promise<ApiResponse<any>> {
  console.log('addGroupMembers API调用:', { groupId, memberIds });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送POST请求到:', `/groups/${groupId}/members`);
    console.log('请求头:', headers);
    console.log('请求体:', { memberIds });
    
    const response = await api.post(`/groups/${groupId}/members`, { memberIds }, { headers });
    console.log('添加成员API调用成功:', response);
    return response;
  } catch (error) {
    console.error('添加成员API调用失败:', error);
    throw error;
  }
}

/**
 * 移除群成员
 * @param {number} groupId - 群组ID
 * @param {number} userId - 用户ID
 * @returns {Promise}
 */
export async function removeGroupMember(groupId: number, userId: number): Promise<ApiResponse<any>> {
  console.log('调用removeGroupMember API:', { groupId, userId });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
          console.log('从localStorage获取到当前用户ID:', currentUserId);
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    if (!currentUserId) {
      console.warn('未能获取当前用户ID，尝试从其他存储位置获取');
      // 尝试从其他可能的存储位置获取用户ID
      const userId = localStorage.getItem('userId') || sessionStorage.getItem('userId');
      if (userId) {
        currentUserId = parseInt(userId);
        console.log('从其他存储位置获取到当前用户ID:', currentUserId);
      }
    }
    
    if (!currentUserId) {
      console.error('无法获取当前用户ID，这可能导致权限验证失败');
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    console.log('准备发送移除成员请求，参数:', { 
      groupId, 
      userId, 
      currentUserId,
      hasToken: !!token,
      tokenPreview: token ? `${token.substring(0, 10)}...` : 'none'
    });
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送DELETE请求到:', `/api/groups/${groupId}/members/${userId}`);
    console.log('请求头:', headers);
    
    const response = await api.delete(`/groups/${groupId}/members/${userId}`, { headers });
    console.log('移除成员API调用成功，响应:', response);
    
    return response;
  } catch (error: any) {
    console.error('移除成员API调用失败:', error);
    
    // 增强错误信息
    let errorMessage = '移除成员失败';
    let errorCode = 500;
    
    if (error.response) {
      // 服务器响应了错误状态码
      errorCode = error.response.status;
      
      if (error.response.data && error.response.data.message) {
        errorMessage = error.response.data.message;
      } else {
        errorMessage = `服务器错误 (${error.response.status})`;
      }
      
      console.error('服务器错误响应:', {
        status: error.response.status,
        data: error.response.data
      });
    } else if (error.request) {
      // 请求已发送但没有收到响应
      errorMessage = '服务器无响应，请检查网络连接';
      console.error('请求已发送但无响应:', error.request);
    } else {
      // 请求设置时出错
      errorMessage = error.message || '请求设置错误';
      console.error('请求设置错误:', error.message);
    }
    
    return {
      code: errorCode,
      success: false,
      message: errorMessage,
      data: null
    };
  }
}

/**
 * 设置或取消管理员
 * @param {number} groupId - 群组ID
 * @param {number} userId - 用户ID
 * @param {boolean} isAdmin - 是否设为管理员
 * @returns {Promise}
 */
export async function setGroupAdmin(groupId: number, userId: number, isAdmin: boolean): Promise<ApiResponse<any>> {
  console.log('setGroupAdmin API调用:', {
    groupId,
    userId,
    isAdmin
  });
  
  try {
    // 获取用户ID和认证令牌
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送PUT请求到:', `/groups/${groupId}/members/${userId}/admin`);
    console.log('请求头:', headers);
    console.log('请求体:', { isAdmin });
    
    const response = await api.put(`/groups/${groupId}/members/${userId}/admin`, { isAdmin }, { headers });
    console.log('设置管理员API调用成功:', response);
    return response;
  } catch (error) {
    console.error('设置管理员API调用失败:', error);
    throw error;
  }
}

/**
 * 设置成员禁言状态
 * @param {number} groupId - 群组ID
 * @param {number} userId - 用户ID
 * @param {boolean} isMuted - 是否禁言
 * @param {number} minutes - 禁言时长（分钟）
 * @returns {Promise}
 */
export async function setMemberMuteStatus(groupId: number, userId: number, isMuted: boolean, minutes?: number): Promise<ApiResponse<any>> {
  console.log('setMemberMuteStatus API调用:', { groupId, userId, isMuted, minutes });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送PUT请求到:', `/groups/${groupId}/members/${userId}/mute`);
    console.log('请求头:', headers);
    console.log('请求体:', { isMuted, minutes });
    
    const response = await api.put(`/groups/${groupId}/members/${userId}/mute`, { isMuted, minutes }, { headers });
    console.log('设置禁言API调用成功:', response);
    return response;
  } catch (error: any) {
    console.error('设置禁言API调用失败:', error);
    
    // 增强错误信息
    let errorMessage = '设置禁言状态失败';
    let errorCode = 500;
    
    if (error.response) {
      // 服务器响应了错误状态码
      errorCode = error.response.status;
      
      if (error.response.data && error.response.data.message) {
        errorMessage = error.response.data.message;
      } else {
        errorMessage = `服务器错误 (${error.response.status})`;
      }
      
      console.error('服务器错误响应:', {
        status: error.response.status,
        data: error.response.data
      });
    } else if (error.request) {
      // 请求已发送但没有收到响应
      errorMessage = '服务器无响应，请检查网络连接';
      console.error('请求已发送但无响应:', error.request);
    } else {
      // 请求设置时出错
      errorMessage = error.message || '请求设置错误';
      console.error('请求设置错误:', error.message);
    }
    
    return {
      code: errorCode,
      success: false,
      message: errorMessage,
      data: null
    };
  }
} 

/**
 * 检查当前用户是否在指定群组中
 * @param {number} groupId - 群组ID
 * @returns {Promise} 返回API响应，success表示用户在群组中，失败表示用户不在群组中
 */
export async function checkUserInGroup(groupId: number): Promise<ApiResponse<any>> {
  console.log('检查用户是否在群组中，群组ID:', groupId);
  
  // 获取用户ID
  let userId = 0;
  const userInfoStr = localStorage.getItem('userInfo');
  if (userInfoStr) {
    try {
      const userInfo = JSON.parse(userInfoStr);
      if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
        userId = userInfo.id;
      }
    } catch (e) {
      console.error('解析localStorage.userInfo失败:', e);
    }
  }
  
  if (userId === 0) {
    const localUserId = localStorage.getItem('userId');
    if (localUserId) {
      userId = parseInt(localUserId);
    }
  }
  
  if (userId === 0) {
    const sessionUserId = sessionStorage.getItem('userId');
    if (sessionUserId) {
      userId = parseInt(sessionUserId);
    }
  }
  
  // 获取认证令牌
  const authTokens = [
    localStorage.getItem('accessToken'),
    localStorage.getItem('auth_token'),
    localStorage.getItem('token'),
    sessionStorage.getItem('accessToken'),
    sessionStorage.getItem('auth_token'),
    sessionStorage.getItem('token')
  ].filter(Boolean);
  
  const token = authTokens[0] || '';
  
  if (!token) {
    console.error('未找到有效的认证令牌，请先登录');
    throw new Error('未找到有效的认证令牌，请先登录');
  }
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'X-User-Id': String(userId)
  };
  
  try {
    console.log(`发送GET请求到 /groups/${groupId}/members/check`);
    console.log('请求头:', headers);
    
    const response = await api.get(`/groups/${groupId}/members/check`, { headers });
    console.log('检查用户是否在群组中API调用成功:', response);
    return response;
  } catch (error) {
    console.error('检查用户是否在群组中API调用失败:', error);
    
    // 如果是401错误，表示登录过期
    if (error && typeof error === 'object' && 'code' in error && error.code === 401) {
      throw new Error('登录已过期，请重新登录');
    }
    
    // 如果是404或403错误，表示用户不在群组中
    if (error && typeof error === 'object' && 'code' in error && 
        (error.code === 404 || error.code === 403)) {
      return {
        success: false,
        code: error.code,
        message: '您不是群组成员',
        data: null
      };
    }
    
    throw error;
  }
} 

/**
 * 获取群组公告列表
 * @param {number} groupId - 群组ID
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export async function getGroupAnnouncements(groupId: number, params: {
  page?: number;
  size?: number;
} = {}): Promise<ApiResponse<any>> {
  console.log('getGroupAnnouncements API调用:', { groupId, params });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送GET请求到:', `/groups/${groupId}/announcements`);
    console.log('请求头:', headers);
    console.log('请求参数:', params);
    
    const response = await api.get(`/groups/${groupId}/announcements`, { body: params, headers });
    console.log('获取群组公告API调用成功:', response);
    return response;
  } catch (error: any) {
    console.error('获取群组公告API调用失败:', error);
    
    // 增强错误信息
    let errorMessage = '获取群组公告失败';
    let errorCode = 500;
    
    if (error.response) {
      errorCode = error.response.status;
      errorMessage = error.response.data?.message || `服务器错误 (${error.response.status})`;
    } else if (error.request) {
      errorMessage = '服务器无响应，请检查网络连接';
    } else {
      errorMessage = error.message || '请求设置错误';
    }
    
    return {
      code: errorCode,
      success: false,
      message: errorMessage,
      data: null
    };
  }
}

/**
 * 获取群组置顶公告
 * @param {number} groupId - 群组ID
 * @returns {Promise}
 */
export async function getPinnedAnnouncements(groupId: number): Promise<ApiResponse<any>> {
  console.log('getPinnedAnnouncements API调用:', { groupId });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送GET请求到:', `/groups/${groupId}/announcements/pinned`);
    console.log('请求头:', headers);
    
    const response = await api.get(`/groups/${groupId}/announcements/pinned`, { headers });
    console.log('获取置顶公告API调用成功:', response);
    return response;
  } catch (error: any) {
    console.error('获取置顶公告API调用失败:', error);
    
    // 增强错误信息
    let errorMessage = '获取置顶公告失败';
    let errorCode = 500;
    
    if (error.response) {
      errorCode = error.response.status;
      errorMessage = error.response.data?.message || `服务器错误 (${error.response.status})`;
    } else if (error.request) {
      errorMessage = '服务器无响应，请检查网络连接';
    } else {
      errorMessage = error.message || '请求设置错误';
    }
    
    return {
      code: errorCode,
      success: false,
      message: errorMessage,
      data: null
    };
  }
}

/**
 * 发布群组公告
 * @param {number} groupId - 群组ID
 * @param {Object} data - 公告数据
 * @returns {Promise}
 */
export async function publishAnnouncement(groupId: number, data: {
  title: string;
  content: string;
  isPinned?: boolean;
}): Promise<ApiResponse<any>> {
  console.log('publishAnnouncement API调用:', { groupId, data });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送POST请求到:', `/groups/${groupId}/announcements`);
    console.log('请求头:', headers);
    console.log('请求体:', data);
    
    const response = await api.post(`/groups/${groupId}/announcements`, data, { headers });
    console.log('发布公告API调用成功:', response);
    return response;
  } catch (error: any) {
    console.error('发布公告API调用失败:', error);
    
    // 增强错误信息
    let errorMessage = '发布公告失败';
    let errorCode = 500;
    
    if (error.response) {
      errorCode = error.response.status;
      errorMessage = error.response.data?.message || `服务器错误 (${error.response.status})`;
    } else if (error.request) {
      errorMessage = '服务器无响应，请检查网络连接';
    } else {
      errorMessage = error.message || '请求设置错误';
    }
    
    return {
      code: errorCode,
      success: false,
      message: errorMessage,
      data: null
    };
  }
}

/**
 * 更新群组公告
 * @param {number} groupId - 群组ID
 * @param {number} announcementId - 公告ID
 * @param {Object} data - 公告数据
 * @returns {Promise}
 */
export async function updateAnnouncement(groupId: number, announcementId: number, data: {
  title: string;
  content: string;
  isPinned?: boolean;
}): Promise<ApiResponse<any>> {
  console.log('updateAnnouncement API调用:', { groupId, announcementId, data });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送PUT请求到:', `/groups/${groupId}/announcements/${announcementId}`);
    console.log('请求头:', headers);
    console.log('请求体:', data);
    
    const response = await api.put(`/groups/${groupId}/announcements/${announcementId}`, data, { headers });
    console.log('更新公告API调用成功:', response);
    return response;
  } catch (error: any) {
    console.error('更新公告API调用失败:', error);
    
    // 增强错误信息
    let errorMessage = '更新公告失败';
    let errorCode = 500;
    
    if (error.response) {
      errorCode = error.response.status;
      errorMessage = error.response.data?.message || `服务器错误 (${error.response.status})`;
    } else if (error.request) {
      errorMessage = '服务器无响应，请检查网络连接';
    } else {
      errorMessage = error.message || '请求设置错误';
    }
    
    return {
      code: errorCode,
      success: false,
      message: errorMessage,
      data: null
    };
  }
}

/**
 * 删除群组公告
 * @param {number} groupId - 群组ID
 * @param {number} announcementId - 公告ID
 * @returns {Promise}
 */
export async function deleteAnnouncement(groupId: number, announcementId: number): Promise<ApiResponse<any>> {
  console.log('deleteAnnouncement API调用:', { groupId, announcementId });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送DELETE请求到:', `/groups/${groupId}/announcements/${announcementId}`);
    console.log('请求头:', headers);
    
    const response = await api.delete(`/groups/${groupId}/announcements/${announcementId}`, { headers });
    console.log('删除公告API调用成功:', response);
    return response;
  } catch (error: any) {
    console.error('删除公告API调用失败:', error);
    
    // 增强错误信息
    let errorMessage = '删除公告失败';
    let errorCode = 500;
    
    if (error.response) {
      errorCode = error.response.status;
      errorMessage = error.response.data?.message || `服务器错误 (${error.response.status})`;
    } else if (error.request) {
      errorMessage = '服务器无响应，请检查网络连接';
    } else {
      errorMessage = error.message || '请求设置错误';
    }
    
    return {
      code: errorCode,
      success: false,
      message: errorMessage,
      data: null
    };
  }
}

/**
 * 置顶或取消置顶群组公告
 * @param {number} groupId - 群组ID
 * @param {number} announcementId - 公告ID
 * @param {boolean} isPinned - 是否置顶
 * @returns {Promise}
 */
export async function pinAnnouncement(groupId: number, announcementId: number, isPinned: boolean): Promise<ApiResponse<any>> {
  console.log('pinAnnouncement API调用:', { groupId, announcementId, isPinned });
  
  try {
    // 获取当前用户ID
    let currentUserId = 0;
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
          currentUserId = userInfo.id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败:', e);
      }
    }
    
    // 获取认证令牌
    const authTokens = [
      localStorage.getItem('accessToken'),
      localStorage.getItem('auth_token'),
      localStorage.getItem('token'),
      sessionStorage.getItem('accessToken'),
      sessionStorage.getItem('auth_token'),
      sessionStorage.getItem('token')
    ].filter(Boolean);
    
    const token = authTokens[0] || '';
    
    if (!token) {
      console.error('未找到有效的认证令牌，请先登录');
      throw new Error('未找到有效的认证令牌，请先登录');
    }
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'X-User-Id': String(currentUserId)
    };
    
    console.log('发送PUT请求到:', `/groups/${groupId}/announcements/${announcementId}/pin?isPinned=${isPinned}`);
    console.log('请求头:', headers);
    
    const response = await api.put(`/groups/${groupId}/announcements/${announcementId}/pin?isPinned=${isPinned}`, {}, { headers });
    console.log('置顶公告API调用成功:', response);
    return response;
  } catch (error: any) {
    console.error('置顶公告API调用失败:', error);
    
    // 增强错误信息
    let errorMessage = '置顶公告失败';
    let errorCode = 500;
    
    if (error.response) {
      errorCode = error.response.status;
      errorMessage = error.response.data?.message || `服务器错误 (${error.response.status})`;
    } else if (error.request) {
      errorMessage = '服务器无响应，请检查网络连接';
    } else {
      errorMessage = error.message || '请求设置错误';
    }
    
    return {
      code: errorCode,
      success: false,
      message: errorMessage,
      data: null
    };
  }
} 