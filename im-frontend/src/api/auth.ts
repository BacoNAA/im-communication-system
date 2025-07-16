import { api, type ApiResponse } from './request';

// 认证相关类型定义
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userInfo: {
    id: number;
    name: string;
    email: string;
    avatar?: string;
  };
}

/**
 * 认证相关 API
 */
export const authApi = {
  /**
   * 用户登录
   * @param loginData 登录数据
   * @returns 认证响应
   */
  async login(loginData: LoginRequest): Promise<ApiResponse<AuthResponse>> {
    return api.post<ApiResponse<AuthResponse>>('/auth/login', loginData);
  },

  /**
   * 验证码登录
   * @param email 邮箱
   * @param verificationCode 验证码
   * @returns 认证响应
   */
  async loginWithCode(email: string, verificationCode: string): Promise<ApiResponse<AuthResponse>> {
    return api.post<ApiResponse<AuthResponse>>('/auth/login/code', {
      email,
      verificationCode
    });
  },

  /**
   * 用户注册
   * @param userData 注册数据
   * @returns 认证响应
   */
  async register(userData: RegisterRequest): Promise<ApiResponse<AuthResponse>> {
    return api.post<ApiResponse<AuthResponse>>('/auth/register', userData);
  },

  /**
   * 发送登录验证码
   * @param email 邮箱
   * @returns 发送结果
   */
  async sendLoginCode(email: string): Promise<ApiResponse<{ message: string }>> {
    return api.post<ApiResponse<{ message: string }>>('/auth/send-login-code', { email });
  },

  /**
   * 发送注册验证码
   * @param email 邮箱
   * @returns 发送结果
   */
  async sendRegisterCode(email: string): Promise<ApiResponse<{ message: string }>> {
    return api.post<ApiResponse<{ message: string }>>('/auth/send-register-code', { email });
  },

  /**
   * 刷新令牌
   * @param refreshToken 刷新令牌
   * @returns 新的认证响应
   */
  async refreshToken(refreshToken: string): Promise<ApiResponse<AuthResponse>> {
    return api.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken });
  },

  /**
   * 用户登出
   * @returns 登出结果
   */
  async logout(): Promise<ApiResponse<{ message: string }>> {
    return api.post<ApiResponse<{ message: string }>>('/auth/logout');
  },

  /**
   * 验证令牌
   * @returns 用户信息
   */
  async verifyToken(): Promise<ApiResponse<AuthResponse['userInfo']>> {
    return api.get<ApiResponse<AuthResponse['userInfo']>>('/auth/verify');
  },

  /**
   * 通用请求方法
   * @param url 请求URL
   * @param config 请求配置
   * @returns 响应数据
   */
  async request<T = any>(url: string, config: any): Promise<ApiResponse<T>> {
    const { method = 'GET', headers = {}, body } = config;
    
    // 处理请求体：如果是字符串则直接使用，如果是JSON字符串则解析
    let requestBody = body;
    if (typeof body === 'string' && body.startsWith('{')) {
      try {
        requestBody = JSON.parse(body);
      } catch (e) {
        // 如果解析失败，保持原字符串（可能是form-urlencoded格式）
        requestBody = body;
      }
    }
    
    switch (method.toUpperCase()) {
      case 'GET':
        return api.get<ApiResponse<T>>(url, { headers });
      case 'POST':
        return api.post<ApiResponse<T>>(url, requestBody, { headers });
      case 'PUT':
        return api.put<ApiResponse<T>>(url, requestBody, { headers });
      case 'DELETE':
        return api.delete<ApiResponse<T>>(url, { headers });
      case 'PATCH':
        return api.patch<ApiResponse<T>>(url, requestBody, { headers });
      default:
        throw new Error(`Unsupported HTTP method: ${method}`);
    }
  }
};

export default authApi;