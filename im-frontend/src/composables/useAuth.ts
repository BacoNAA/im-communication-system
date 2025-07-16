import { ref, computed } from 'vue';
import type { User } from '@/types';
import { storage } from '@/utils';
import { authApi, type AuthResponse } from '@/api/auth';

// 获取设备信息
function getDeviceType(): string {
  const userAgent = navigator.userAgent.toLowerCase();
  
  if (/mobile|android|iphone|ipad|phone|blackberry|opera mini|iemobile|wpdesktop/.test(userAgent)) {
    if (/ipad|tablet/.test(userAgent)) {
      return 'Tablet';
    }
    return 'Mobile';
  }
  
  return 'Web';
}

const currentUser = ref<User | null>(null);
const isAuthenticated = computed(() => !!currentUser.value);
const token = ref<string | null>(storage.get('auth_token'));

// 初始化认证状态函数
const initAuth = (): void => {
  // 优先从localStorage获取，如果没有则从sessionStorage获取
  let savedToken = storage.get<string>('auth_token');
  let savedUser = storage.get<User>('current_user');
  
  // 如果localStorage中没有，检查sessionStorage
  if (!savedToken) {
    const sessionToken = sessionStorage.getItem('auth_token');
    const sessionUser = sessionStorage.getItem('current_user');
    
    if (sessionToken && sessionUser) {
      try {
        savedToken = sessionToken;
        savedUser = JSON.parse(sessionUser);
      } catch (error) {
        console.error('解析sessionStorage用户数据失败:', error);
      }
    }
  }

  if (savedToken && savedUser) {
    // 确保用户ID是数字类型
    if (savedUser.id && typeof savedUser.id === 'string') {
      savedUser.id = parseInt(savedUser.id, 10);
    }
    
    token.value = savedToken;
    currentUser.value = savedUser;
    
    // 确保userId也被单独存储
    if (savedUser.id) {
      // 如果是从localStorage获取的，存储到localStorage
      if (storage.get<string>('auth_token')) {
        localStorage.setItem('userId', String(savedUser.id));
      } 
      // 如果是从sessionStorage获取的，存储到sessionStorage
      else if (sessionStorage.getItem('auth_token')) {
        sessionStorage.setItem('userId', String(savedUser.id));
      }
      
      // 确保userInfo中也有userId
      if (localStorage.getItem('userInfo')) {
        try {
          const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
          userInfo.id = savedUser.id;
          localStorage.setItem('userInfo', JSON.stringify(userInfo));
        } catch (e) {
          console.error('更新localStorage中的userInfo失败:', e);
        }
      }
      
      if (sessionStorage.getItem('userInfo')) {
        try {
          const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}');
          userInfo.id = savedUser.id;
          sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
        } catch (e) {
          console.error('更新sessionStorage中的userInfo失败:', e);
        }
      }
    }
    
    console.log('已从存储中恢复用户信息:', currentUser.value);
  }
};

// 确保在模块加载时初始化认证状态
initAuth();

export const useAuth = () => {
  const login = async (loginData: {
    email: string;
    password?: string;
    verificationCode?: string;
    rememberMe?: boolean;
  }): Promise<void> => {
    try {
      // 获取设备信息
      const deviceInfo = {
        deviceType: getDeviceType(),
        deviceInfo: navigator.userAgent
      };
      
      // 构建请求数据
      const requestData = {
        email: loginData.email,
        deviceType: deviceInfo.deviceType,
        deviceInfo: deviceInfo.deviceInfo,
        rememberMe: loginData.rememberMe || false
      };
      
      let endpoint: string;
      
      // 根据是否有验证码决定使用哪个登录接口
      if (loginData.verificationCode) {
        // 验证码登录
        endpoint = '/api/auth/login/verification-code';
        Object.assign(requestData, {
          verificationCode: loginData.verificationCode
        });
      } else if (loginData.password) {
        // 密码登录
        endpoint = '/api/auth/login/password';
        Object.assign(requestData, {
          password: loginData.password
        });
      } else {
        throw new Error('请提供密码或验证码');
      }
      
      const response = await authApi.request<AuthResponse>(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      });

      if (!response.success || !response.data) {
        throw new Error(response.message || '登录失败');
      }

      const { accessToken, refreshToken, userInfo } = response.data;
      
      // 确保用户信息完整
      if (!userInfo || !userInfo.id) {
        throw new Error('登录成功但获取用户信息失败');
      }
      
      // 处理用户信息
      const currentUserData: User = {
        id: userInfo.id,
        email: userInfo.email,
        nickname: userInfo.name || userInfo.email.split('@')[0] || 'User', // 使用name作为nickname，确保有默认值
        ...(userInfo.name && { name: userInfo.name }),
        ...(userInfo.avatar && { avatarUrl: userInfo.avatar, avatar: userInfo.avatar }),
        status: 'online' as const
      };
      
      console.log('登录成功，用户信息:', currentUserData);
      console.log('用户头像URL:', currentUserData.avatarUrl || currentUserData.avatar);

      currentUser.value = currentUserData;
      token.value = accessToken;

      // 根据rememberMe决定存储位置
      if (loginData.rememberMe) {
        // 记住登录状态：保存到localStorage（持久化）
        storage.set('auth_token', accessToken);
        storage.set('current_user', currentUserData);
        storage.set('refresh_token', refreshToken);
        localStorage.setItem('userId', String(currentUserData.id)); // 添加userId单独存储
      } else {
        // 不记住登录状态：保存到sessionStorage（会话级别）
        sessionStorage.setItem('auth_token', accessToken);
        sessionStorage.setItem('current_user', JSON.stringify(currentUserData));
        sessionStorage.setItem('refresh_token', refreshToken);
        sessionStorage.setItem('userId', String(currentUserData.id)); // 添加userId单独存储
        // 清除localStorage中的登录信息
        storage.remove('auth_token');
        storage.remove('current_user');
        storage.remove('refresh_token');
        localStorage.removeItem('userId'); // 清除localStorage中的userId
      }
      
      // 确保userInfo中也有userId
      if (localStorage.getItem('userInfo')) {
        try {
          const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
          userInfo.id = currentUserData.id;
          localStorage.setItem('userInfo', JSON.stringify(userInfo));
        } catch (e) {
          console.error('更新localStorage中的userInfo失败:', e);
        }
      }
      
      if (sessionStorage.getItem('userInfo')) {
        try {
          const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}');
          userInfo.id = currentUserData.id;
          sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
        } catch (e) {
          console.error('更新sessionStorage中的userInfo失败:', e);
        }
      }
    } catch (error: any) {
      console.error('登录失败:', error);
      throw new Error(error.message || '登录失败，请检查用户名和密码');
    }
  };

  const sendLoginCode = async (email: string): Promise<void> => {
    try {
      const response = await authApi.request<string>('/api/auth/verification/send/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `email=${encodeURIComponent(email)}`
      });
      
      if (!response.success) {
        throw new Error(response.message || '发送验证码失败');
      }
    } catch (error: any) {
      console.error('发送验证码失败:', error);
      throw new Error(error.message || '发送验证码失败，请稍后重试');
    }
  };

  const sendRegisterCode = async (email: string): Promise<void> => {
    try {
      const response = await authApi.request<string>('/api/auth/verification/send/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `email=${encodeURIComponent(email)}`
      });
      
      if (!response.success) {
        throw new Error(response.message || '发送验证码失败');
      }
    } catch (error: any) {
      console.error('发送验证码失败:', error);
      throw new Error(error.message || '发送验证码失败，请稍后重试');
    }
  };

  const logout = (): void => {
    currentUser.value = null;
    token.value = null;

    // 清除本地存储
    storage.remove('auth_token');
    storage.remove('current_user');
    storage.remove('refresh_token');
    localStorage.removeItem('userId');
    
    // 清除会话存储
    sessionStorage.removeItem('auth_token');
    sessionStorage.removeItem('current_user');
    sessionStorage.removeItem('refresh_token');
    sessionStorage.removeItem('userId');
  };

  const register = async (userData: {
    username?: string;
    email: string;
    password: string;
    confirmPassword?: string;
    verificationCode: string;
    nickname?: string;
  }): Promise<void> => {
    try {
      const requestData = {
        email: userData.email,
        password: userData.password,
        verificationCode: userData.verificationCode,
        nickname: userData.nickname || userData.username || null,
        deviceType: getDeviceType(),
        deviceInfo: navigator.userAgent
      };
      
      const response = await authApi.request<AuthResponse>('/api/auth/register/email', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      });
      
      if (!response.success || !response.data) {
        throw new Error(response.message || '注册失败');
      }

      const { accessToken, refreshToken, userInfo } = response.data;
      
      // 转换为内部用户类型
      const currentUserData: User = {
        id: userInfo.id,
        email: userInfo.email,
        nickname: userInfo.name || userInfo.email,
        name: userInfo.name || userInfo.email,
        ...(userInfo.avatar && { avatarUrl: userInfo.avatar, avatar: userInfo.avatar }),
        status: 'online' as const,
      };

      currentUser.value = currentUserData;
      token.value = accessToken;

      // 保存到本地存储
      storage.set('auth_token', accessToken);
      storage.set('current_user', currentUserData);
      storage.set('refresh_token', refreshToken);
    } catch (error: any) {
      console.error('注册失败:', error);
      throw new Error(error.message || '注册失败，请稍后重试');
    }
  };

  // 已在模块级别定义了initAuth函数，这里不需要重复定义

  const updateUserStatus = (status: NonNullable<User['status']>): void => {
    if (currentUser.value) {
      currentUser.value.status = status;
      storage.set('current_user', currentUser.value);
    }
  };

  // 获取当前用户ID（确保为数字类型）
  const getCurrentUserId = (): number => {
    console.log('getCurrentUserId被调用，当前用户信息:', currentUser.value);
    
    if (!currentUser.value) {
      console.error('getCurrentUserId: 当前用户为空');
      
      // 尝试从存储中直接获取用户ID
      try {
        const storedUser = storage.get<User>('current_user');
        if (storedUser && storedUser.id) {
          const userId = typeof storedUser.id === 'string' 
            ? parseInt(storedUser.id, 10) 
            : storedUser.id;
          
          console.log('从存储中恢复用户ID:', userId);
          return userId;
        }
        
        // 尝试从sessionStorage获取
        const sessionUser = sessionStorage.getItem('current_user');
        if (sessionUser) {
          try {
            const parsedUser = JSON.parse(sessionUser);
            if (parsedUser && parsedUser.id) {
              const userId = typeof parsedUser.id === 'string'
                ? parseInt(parsedUser.id, 10)
                : parsedUser.id;
              
              console.log('从sessionStorage恢复用户ID:', userId);
              return userId;
            }
          } catch (e) {
            console.error('解析sessionStorage用户数据失败:', e);
          }
        }
      } catch (e) {
        console.error('从存储获取用户ID失败:', e);
      }
      
      return 0;
    }
    
    const userId = typeof currentUser.value.id === 'string' 
      ? parseInt(currentUser.value.id, 10) 
      : (currentUser.value.id || 0);
    
    console.log('返回用户ID:', userId);
    return userId;
  };

  return {
    currentUser: computed(() => currentUser.value),
    isAuthenticated,
    token: computed(() => token.value),
    login,
    logout,
    register,
    initAuth,
    updateUserStatus,
    sendLoginCode,
    sendRegisterCode,
    getCurrentUserId // 导出新函数
  };
};
