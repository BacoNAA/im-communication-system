// HTTP 请求工具

// 基础配置
const BASE_URL = '/api';
const TIMEOUT = 15000; // 增加超时时间到15秒

// 调试模式
const DEBUG_API = true; // 控制是否输出API请求调试信息
const DEBUG_USER_SETTINGS = true; // 特别调试用户设置API

// 请求接口
export interface RequestConfig {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  headers?: Record<string, string>;
  body?: any;
  timeout?: number;
}

// 响应接口
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  success: boolean;
}

// 错误类
export class ApiError extends Error {
  code: number;
  response?: Response;

  constructor(message: string, code: number, response?: Response) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    if (response !== undefined) {
      this.response = response;
    }
  }
}

/**
 * 获取认证头信息
 * @returns {Object} 认证头信息
 */
function getAuthHeaders(): Record<string, string> {
  const headers: Record<string, string> = {};
  
  // 获取认证令牌
  const token = getAuthToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  // 获取用户ID
  const userId = getUserId();
  if (userId) {
    headers['X-User-Id'] = String(userId);
  }
  
  return headers;
}

/**
 * 获取认证token
 * @returns {string|null} 认证token
 */
function getAuthToken(): string | null {
  // 从多个可能的存储位置获取令牌
  const locations = [
    { source: 'localStorage', key: 'accessToken' },
    { source: 'localStorage', key: 'token' },
    { source: 'localStorage', key: 'auth_token' },
    { source: 'sessionStorage', key: 'accessToken' },
    { source: 'sessionStorage', key: 'token' },
    { source: 'sessionStorage', key: 'auth_token' }
  ];
  
  for (const location of locations) {
    const token = location.source === 'localStorage' 
      ? localStorage.getItem(location.key) 
      : sessionStorage.getItem(location.key);
    
    if (token) {
      console.log(`获取到认证令牌: 来源=${location.source}, 键=${location.key}, 令牌=${token.substring(0, 10)}...`);
      return token;
    }
  }
  
  console.warn('未找到认证令牌，尝试从URL参数获取');
  
  // 尝试从URL参数获取令牌
  const urlParams = new URLSearchParams(window.location.search);
  const tokenFromUrl = urlParams.get('token');
  
  if (tokenFromUrl) {
    console.log('从URL参数获取到令牌:', tokenFromUrl.substring(0, 10) + '...');
    
    // 存储令牌以便后续使用
    localStorage.setItem('accessToken', tokenFromUrl);
    
    return tokenFromUrl;
  }
  
  console.warn('所有位置都未找到认证令牌');
  return null;
}

/**
 * 清除认证信息
 */
function clearAuth(): void {
  localStorage.removeItem('auth_token');
  localStorage.removeItem('current_user');
  localStorage.removeItem('refresh_token');
  sessionStorage.removeItem('auth_token');
  sessionStorage.removeItem('current_user');
  sessionStorage.removeItem('refresh_token');
  // 抛出特殊错误，让调用方处理导航
  throw new Error('UNAUTHORIZED');
}

/**
 * 从存储中获取用户ID
 */
function getUserId(): number | null {
  let userId: number | null = null;
  
  // 尝试从localStorage获取
  const localUserId = localStorage.getItem('userId');
  if (localUserId) {
    const parsedId = parseInt(localUserId);
    if (!isNaN(parsedId) && parsedId > 0) {
      return parsedId;
    }
  }
  
  // 尝试从sessionStorage获取
  const sessionUserId = sessionStorage.getItem('userId');
  if (sessionUserId) {
    const parsedId = parseInt(sessionUserId);
    if (!isNaN(parsedId) && parsedId > 0) {
      return parsedId;
    }
  }
  
  // 尝试从localStorage的userInfo获取
  const userInfoStr = localStorage.getItem('userInfo');
  if (userInfoStr) {
    try {
      const userInfo = JSON.parse(userInfoStr);
      if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
        return userInfo.id;
      }
    } catch (e) {
      console.error('解析localStorage.userInfo失败:', e);
    }
  }
  
  // 尝试从sessionStorage的userInfo获取
  const sessionUserInfoStr = sessionStorage.getItem('userInfo');
  if (sessionUserInfoStr) {
    try {
      const userInfo = JSON.parse(sessionUserInfoStr);
      if (userInfo && userInfo.id && typeof userInfo.id === 'number' && userInfo.id > 0) {
        return userInfo.id;
      }
    } catch (e) {
      console.error('解析sessionStorage.userInfo失败:', e);
    }
  }
  
  return null;
}

// 处理响应
async function handleResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get('content-type');
  const url = response.url;
  
  // 处理401未授权
  if (response.status === 401) {
    clearAuth();
    throw new ApiError('未授权，请重新登录', 401, response);
  }

  // 检查响应是否成功
  if (!response.ok) {
    let errorMessage = `请求失败: ${response.status} ${response.statusText}`;
    let errorCode = response.status;
    let errorData = null;
    
    try {
      if (contentType && contentType.includes('application/json')) {
        errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        errorCode = errorData.code || errorCode;
        
        console.error('API错误响应:', {
          status: response.status,
          statusText: response.statusText,
          errorData,
          url
        });
      } else {
        const errorText = await response.text();
        if (errorText) {
          errorMessage = errorText;
          console.error('API错误响应(非JSON):', {
            status: response.status,
            statusText: response.statusText,
            errorText,
            url
          });
        }
      }
    } catch (e) {
      // 解析错误时提供更详细的日志
      console.error('解析错误响应失败:', e, '原始状态:', response.status, response.statusText, '请求URL:', url);
    }
    
    // 对于500错误，提供更友好的错误消息
    if (response.status === 500) {
      errorMessage = '系统异常，请稍后重试';
      console.error('服务器500错误:', errorData || errorMessage, '请求URL:', url);
    }
    
    // 对于400错误，可能是请求参数问题
    if (response.status === 400) {
      console.error('请求参数错误:', errorData || errorMessage, '请求URL:', url);
    }
    
    throw new ApiError(errorMessage, errorCode, response);
  }

  // 处理空响应
  if (response.status === 204 || !contentType) {
    return null as T;
  }

  // 解析JSON响应
  if (contentType && contentType.includes('application/json')) {
    try {
      const data = await response.json();
      
      // 添加调试日志
      if (url.includes('/messages/send')) {
        console.log('消息发送响应数据:', data);
      }
      
      // 检查业务逻辑错误
      if (data.code !== undefined && data.code !== 200 && data.code !== 0) {
        throw new ApiError(data.message || '请求失败', data.code);
      }
      
      // 检查响应数据结构
      if (data.data && typeof data.data === 'object') {
        // 处理消息数据相关逻辑
        if (data.data.content) {
          console.log('消息内容类型:', typeof data.data.content);
          console.log('消息内容示例:', 
            Array.isArray(data.data.content) && data.data.content.length > 0
              ? JSON.stringify(data.data.content[0]).substring(0, 100)
              : '无内容'
          );
          
          // 获取当前用户ID
          const currentUserId = localStorage.getItem('userId') || 
                               sessionStorage.getItem('userId') || 
                               localStorage.getItem('userInfo') && JSON.parse(localStorage.getItem('userInfo') || '{}').id || 
                               sessionStorage.getItem('userInfo') && JSON.parse(sessionStorage.getItem('userInfo') || '{}').id;
          
          console.log('当前用户ID:', currentUserId);
          console.log('localStorage中的userId:', localStorage.getItem('userId'));
          console.log('sessionStorage中的userId:', sessionStorage.getItem('userId'));
          console.log('localStorage中的userInfo:', localStorage.getItem('userInfo'));
          console.log('sessionStorage中的userInfo:', sessionStorage.getItem('userInfo'));
          
          if (currentUserId) {
            // 为每条消息添加正确的发送者信息
            data.data.content = data.data.content.map((msg: any) => {
              // 获取实际发送者ID
              const senderId = msg.message?.senderId || msg.senderId;
              console.log('消息发送者ID:', senderId, '当前用户ID:', currentUserId);
              
              // 检查消息是否包含mediaFileId
              const mediaFileId = msg.mediaFileId || (msg.message && msg.message.mediaFileId);
              if (mediaFileId) {
                console.log(`消息 ${msg.id} 包含媒体文件ID: ${mediaFileId}`);
                // 确保mediaFileId存在于顶级对象
                msg.mediaFileId = mediaFileId;
              }
              
              // 添加isSentByCurrentUser字段
              if (senderId !== undefined) {
                const isSentByCurrentUser = String(senderId) === String(currentUserId);
                
                // 添加到顶层
                msg.isSentByCurrentUser = isSentByCurrentUser;
                
                // 添加到嵌套结构
                if (msg.message) {
                  msg.message.isSentByCurrentUser = isSentByCurrentUser;
                }
                
                console.log('已添加isSentByCurrentUser字段:', isSentByCurrentUser);
              }
              
              return msg;
            });
          }
        } else if (data.data.message) {
          // 单个消息对象的处理
          console.log('处理单个消息对象');
          
          // 检查mediaFileId
          if (data.data.message.mediaFileId) {
            console.log('消息包含媒体文件ID:', data.data.message.mediaFileId);
            
            // 确保mediaFileId也存在于顶层数据中
            data.data.mediaFileId = data.data.message.mediaFileId;
          }
        }
      }
    
      return data;
    } catch (e) {
      console.error('解析JSON响应失败:', e);
      if (e instanceof ApiError) {
        throw e;
      }
      throw new ApiError('解析响应数据失败', 0, response);
    }
  }

  // 返回文本响应
  try {
    return (await response.text()) as T;
  } catch (e) {
    console.error('解析文本响应失败:', e);
    throw new ApiError('解析响应数据失败', 0, response);
  }
}

// 发送HTTP请求
export async function request<T = any>(
  url: string,
  config: RequestConfig = {}
): Promise<T> {
  // 默认配置
  const defaultConfig: RequestConfig = {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    },
    timeout: TIMEOUT
  };

  // 合并配置
  const mergedConfig = {
    ...defaultConfig,
    ...config,
    headers: {
      ...defaultConfig.headers,
      ...config.headers
    }
  };

  // 添加认证头
  const token = getAuthToken();
  if (token) {
    mergedConfig.headers = {
      ...mergedConfig.headers,
      'Authorization': `Bearer ${token}`
    };
  }

  // 添加用户ID头
  const userId = getUserId();
  if (userId) {
    mergedConfig.headers = {
      ...mergedConfig.headers,
      'X-User-Id': String(userId)
    };
    console.log('添加用户ID到请求头:', userId);
  } else {
    console.warn('未找到用户ID，请求可能会失败');
  }

  // 添加调试日志
  const fullUrl = `${BASE_URL}${url}`;
  
  if (DEBUG_API) {
    console.log(`发送${mergedConfig.method}请求: ${fullUrl}`);
    console.log('请求头:', mergedConfig.headers);
    if (mergedConfig.method === 'POST' || mergedConfig.method === 'PUT' || mergedConfig.method === 'PATCH') {
      console.log('请求体:', JSON.stringify(mergedConfig.body, null, 2));
    }
  }
  
  // 特别调试用户设置相关API
  if (DEBUG_USER_SETTINGS && url.includes('/user/settings')) {
    console.log('------- 用户设置 API 调试 -------');
    console.log(`${mergedConfig.method} ${fullUrl}`);
    console.log('请求头:', mergedConfig.headers);
    if (mergedConfig.body) {
      console.log('请求体:', mergedConfig.body);
    }
    console.trace('调用堆栈');
  }
  
  // 为特定请求添加更详细的日志
  if (url.includes('/groups') && mergedConfig.method === 'POST') {
    console.log('创建群组请求详情:', {
      url: fullUrl,
      method: mergedConfig.method,
      headers: mergedConfig.headers,
      body: mergedConfig.body ? JSON.parse(JSON.stringify(mergedConfig.body)) : undefined
    });
    
    // 检查请求体是否符合后端要求
    const requestBody = mergedConfig.body;
    if (requestBody) {
      console.log('验证创建群组请求体:');
      
      // 检查必填字段
      if (!requestBody.name) {
        console.warn('警告: 缺少必填字段 name');
      }
      
      // 检查成员列表
      if (!requestBody.memberIds || !Array.isArray(requestBody.memberIds) || requestBody.memberIds.length === 0) {
        console.warn('警告: memberIds 为空或不是数组');
      } else {
        // 检查成员ID是否都是数字
        const nonNumberIds = requestBody.memberIds.filter((id: any) => typeof id !== 'number');
        if (nonNumberIds.length > 0) {
          console.warn('警告: memberIds 包含非数字值:', nonNumberIds);
        }
      }
      
      // 检查创建者ID是否包含在成员列表中
      const userId = getUserId();
      if (userId && requestBody.memberIds && !requestBody.memberIds.includes(userId)) {
        console.warn('警告: 创建者ID不在成员列表中');
      }
    }
  }

  try {
    // 设置超时
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), mergedConfig.timeout || TIMEOUT);

    // 构建请求选项
    const fetchOptions: RequestInit = {
      method: mergedConfig.method || 'GET',
      headers: mergedConfig.headers as HeadersInit,
      signal: controller.signal
    };

    // 添加请求体
    if (mergedConfig.body !== undefined && mergedConfig.method !== 'GET') {
      fetchOptions.body = typeof mergedConfig.body === 'string'
        ? mergedConfig.body
        : JSON.stringify(mergedConfig.body);
    }

    // 发送请求
    const response = await fetch(fullUrl, fetchOptions);
    
    // 清除超时
    clearTimeout(timeoutId);
    
    // 处理响应
    const result = await handleResponse<T>(response);
    
    // 调试用户设置API响应
    if (DEBUG_USER_SETTINGS && url.includes('/user/settings')) {
      console.log('------- 用户设置 API 响应 -------');
      console.log(`${mergedConfig.method} ${fullUrl} 响应:`, result);
    }
      
    // 为特定请求添加更详细的日志
    if (url.includes('/messages/read')) {
      console.log('标记已读响应详情:', result);
    }
    
    return result;
  } catch (error) {
    // 处理错误
    console.error(`请求失败: ${fullUrl}`, error);
    
    if (error instanceof ApiError) {
      throw error;
    }
    
    if (error instanceof DOMException && error.name === 'AbortError') {
        throw new ApiError('请求超时', 408);
      }
      
    throw new ApiError(error instanceof Error ? error.message : '请求失败', 500);
  }
}

// 便捷方法
export const api = {
  get: <T = any>(url: string, config?: Omit<RequestConfig, 'method'>) =>
    request<T>(url, { ...config, method: 'GET' }),

  post: <T = any>(url: string, data?: any, config?: Omit<RequestConfig, 'method' | 'body'>) =>
    request<T>(url, { ...config, method: 'POST', body: data }),

  put: <T = any>(url: string, data?: any, config?: Omit<RequestConfig, 'method' | 'body'>) =>
    request<T>(url, { ...config, method: 'PUT', body: data }),

  delete: <T = any>(url: string, config?: Omit<RequestConfig, 'method'>) =>
    request<T>(url, { ...config, method: 'DELETE' }),

  patch: <T = any>(url: string, data?: any, config?: Omit<RequestConfig, 'method' | 'body'>) =>
    request<T>(url, { ...config, method: 'PATCH', body: data })
};

// 文件上传
export async function uploadFile(
  url: string,
  file: File,
  onProgress?: (progress: number) => void
): Promise<any> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    const formData = new FormData();
    formData.append('file', file);

    // 添加认证头
    const token = getAuthToken();

    // 监听上传进度
    if (onProgress) {
      xhr.upload.addEventListener('progress', (event) => {
        if (event.lengthComputable) {
          const progress = (event.loaded / event.total) * 100;
          onProgress(progress);
        }
      });
    }

    // 监听响应
    xhr.addEventListener('load', () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const response = JSON.parse(xhr.responseText);
          resolve(response);
        } catch (e) {
          resolve(xhr.responseText);
        }
      } else {
        reject(new ApiError(`上传失败: ${xhr.status} ${xhr.statusText}`, xhr.status));
      }
    });

    // 监听错误
    xhr.addEventListener('error', () => {
      reject(new ApiError('上传失败', 0));
    });

    // 监听超时
    xhr.addEventListener('timeout', () => {
      reject(new ApiError('上传超时', 408));
    });

    // 设置超时时间
    xhr.timeout = 30000; // 30秒

    // 发送请求
    const fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`;
    xhr.open('POST', fullUrl);
    
    // 在open之后，send之前设置请求头
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }
    
    xhr.send(formData);
  });
}

// 导出默认实例
export default api;