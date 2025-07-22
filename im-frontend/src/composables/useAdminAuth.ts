import { ref } from 'vue';
import adminApi from '@/api/admin';

// 定义AdminAuthResponse类型
interface AdminInfo {
  id: number;
  username: string;
  email?: string;
  role: string;
  permissions?: string[];
  lastLoginAt?: string;
}

interface AdminAuthResponse {
  accessToken: string;
  refreshToken?: string;
  adminInfo: AdminInfo;
}

// 管理员信息和认证状态
const adminToken = ref<string | null>(null);
const currentAdmin = ref<AdminInfo | null>(null);
const isAdminAuthenticated = ref<boolean>(false);

/**
 * 管理员认证相关的组合式API
 */
export const useAdminAuth = () => {
  /**
   * 管理员登录
   * @param username 用户名
   * @param password 密码
   * @param rememberMe 是否记住登录状态
   */
  const adminLogin = async (
    username: string,
    password: string,
    rememberMe: boolean = false
  ): Promise<AdminInfo> => {
    try {
      // 发送登录请求
      const response = await adminApi.login(username.trim(), password);
      
      if (!response.success || !response.data) {
        throw new Error(response.message || '登录失败');
      }
      
      const { accessToken, adminInfo } = response.data;
      
      // 确保管理员信息完整
      if (!adminInfo || !adminInfo.id) {
        throw new Error('登录成功但获取管理员信息失败');
      }
      
      // 更新状态
      adminToken.value = accessToken;
      currentAdmin.value = adminInfo;
      isAdminAuthenticated.value = true;
      
      // 存储认证信息
      if (rememberMe) {
        // 记住登录状态：保存到localStorage（持久化）
        localStorage.setItem('accessToken', accessToken);  // 使用'accessToken'键名
        localStorage.setItem('adminInfo', JSON.stringify(adminInfo));
      } else {
        // 不记住登录状态：保存到sessionStorage（会话级别）
        sessionStorage.setItem('accessToken', accessToken);  // 使用'accessToken'键名
        sessionStorage.setItem('adminInfo', JSON.stringify(adminInfo));
        // 清除localStorage中的登录信息
        localStorage.removeItem('accessToken');  // 使用'accessToken'键名
        localStorage.removeItem('adminInfo');
      }
      
      return adminInfo;
    } catch (error: any) {
      console.error('管理员登录失败:', error);
      throw new Error(error.message || '登录失败，请检查用户名和密码');
    }
  };
  
  /**
   * 检查管理员登录状态
   */
  const checkAdminAuth = async (): Promise<boolean> => {
    // 如果已经认证，直接返回
    if (isAdminAuthenticated.value && currentAdmin.value) {
      return true;
    }
    
    // 从存储中获取token和管理员信息
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
    const adminInfoStr = localStorage.getItem('adminInfo') || sessionStorage.getItem('adminInfo');
    
    if (!token || !adminInfoStr) {
      return false;
    }
    
    try {
      // 设置token
      adminToken.value = token;
      
      // 解析管理员信息
      const adminInfo = JSON.parse(adminInfoStr);
      currentAdmin.value = adminInfo;
      isAdminAuthenticated.value = true;
      
      return true;
    } catch (error) {
      console.error('验证管理员认证失败:', error);
      // 清除认证信息
      adminLogout();
      return false;
    }
  };
  
  /**
   * 管理员退出登录
   */
  const adminLogout = async (): Promise<void> => {
    try {
      // 如果有token，调用登出API
      if (adminToken.value) {
        await adminApi.logout();
      }
    } catch (error) {
      console.error('管理员登出API调用失败:', error);
    } finally {
      // 清除状态
      adminToken.value = null;
      currentAdmin.value = null;
      isAdminAuthenticated.value = false;
      
      // 清除存储的认证信息
      localStorage.removeItem('accessToken');  // 使用'accessToken'键名
      localStorage.removeItem('adminInfo');
      sessionStorage.removeItem('accessToken');  // 使用'accessToken'键名
      sessionStorage.removeItem('adminInfo');
    }
  };
  
  /**
   * 获取当前管理员信息
   */
  const getAdminInfo = (): AdminInfo | null => {
    // 如果内存中有，直接返回
    if (currentAdmin.value) {
      return currentAdmin.value;
    }
    
    // 从存储中获取
    const adminInfoStr = localStorage.getItem('adminInfo') || sessionStorage.getItem('adminInfo');
    
    if (adminInfoStr) {
      try {
        const adminInfo = JSON.parse(adminInfoStr);
        currentAdmin.value = adminInfo;
        return adminInfo;
      } catch (error) {
        console.error('解析管理员信息失败:', error);
      }
    }
    
    return null;
  };
  
  return {
    adminLogin,
    adminLogout,
    checkAdminAuth,
    getAdminInfo,
    adminToken,
    currentAdmin,
    isAdminAuthenticated
  };
};

// 导出单例实例
export default useAdminAuth; 