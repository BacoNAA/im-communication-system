// HTTP 请求工具

// 基础配置
const BASE_URL = '/api';
const TIMEOUT = 10000;

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

// 获取认证token
function getAuthToken(): string | null {
  // 优先从localStorage获取，如果没有则从sessionStorage获取
  // 支持多种token键名以保持兼容性
  return localStorage.getItem('accessToken') || 
         localStorage.getItem('token') || 
         localStorage.getItem('auth_token') || 
         sessionStorage.getItem('accessToken') || 
         sessionStorage.getItem('token') || 
         sessionStorage.getItem('auth_token');
}

// 清除认证信息
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

// 处理响应
async function handleResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get('content-type');
  
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
          errorData
        });
      } else {
        const errorText = await response.text();
        if (errorText) {
          errorMessage = errorText;
          console.error('API错误响应(非JSON):', {
            status: response.status,
            statusText: response.statusText,
            errorText
          });
        }
      }
    } catch (e) {
      // 解析错误时提供更详细的日志
      console.error('解析错误响应失败:', e, '原始状态:', response.status, response.statusText);
    }
    
    // 对于500错误，提供更友好的错误消息
    if (response.status === 500) {
      errorMessage = '系统异常，请稍后重试';
      console.error('服务器500错误:', errorData || errorMessage);
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

// 主要的请求函数
export async function request<T = any>(
  url: string,
  config: RequestConfig = {}
): Promise<T> {
  const {
    method = 'GET',
    headers = {},
    body,
    timeout = TIMEOUT
  } = config;

  // 构建完整URL
  const fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`;

  // 构建请求头
  const requestHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...headers
  };

  // 添加认证头
  const token = getAuthToken();
  if (token) {
    requestHeaders['Authorization'] = `Bearer ${token}`;
  }

  // 构建请求体
  let requestBody: string | FormData | null = null;
  if (body) {
    if (body instanceof FormData) {
      requestBody = body;
      // FormData会自动设置Content-Type，删除手动设置的
      delete requestHeaders['Content-Type'];
    } else if (typeof body === 'string') {
      // 直接使用字符串（用于form-urlencoded格式）
      requestBody = body;
    } else if (typeof body === 'object') {
      requestBody = JSON.stringify(body);
    } else {
      requestBody = body;
    }
  }

  // 创建AbortController用于超时控制
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    const response = await fetch(fullUrl, {
      method,
      headers: requestHeaders,
      body: requestBody,
      signal: controller.signal,
      credentials: 'include' // 包含cookies
    });

    clearTimeout(timeoutId);
    return await handleResponse<T>(response);
  } catch (error) {
    clearTimeout(timeoutId);
    
    if (error instanceof ApiError) {
      throw error;
    }
    
    if (error instanceof Error) {
      if (error.name === 'AbortError') {
        throw new ApiError('请求超时', 408);
      }
      throw new ApiError(error.message || '网络错误', 0);
    }
    
    throw new ApiError('未知错误', 0);
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
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }

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
    xhr.send(formData);
  });
}

// 导出默认实例
export default api;