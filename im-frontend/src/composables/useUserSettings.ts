import { ref, computed, onMounted } from 'vue';
import { useAuth } from './useAuth';
import { api } from '@/api/request';
import { applyThemeColor, applyFontSize, applyBackground } from '@/utils/themeUtils';

// 全局单例实例
let userSettingsInstance: ReturnType<typeof _useUserSettings> | null = null;

// 获取userSettings实例的函数
export const getUserSettings = () => {
  if (!userSettingsInstance) {
    userSettingsInstance = _useUserSettings();
  }
  return userSettingsInstance;
};

// 设置类型定义
export interface ThemeSettings {
  color?: string;
  chatBackground?: string;
  fontSize?: number;
}

export interface UserSettings {
  theme?: ThemeSettings;
  privacy?: {
    showOnlineStatus?: boolean;
    allowFriendRequests?: boolean;
    showLastSeen?: boolean;
  };
  notification?: {
    enableNotifications?: boolean;
    enableSoundNotifications?: boolean;
    enableVibration?: boolean;
  };
}

export interface UpdateSettingsRequest {
  theme?: ThemeSettings;
  privacy?: {
    showOnlineStatus?: boolean;
    allowFriendRequests?: boolean;
    showLastSeen?: boolean;
  };
  notification?: {
    enableNotifications?: boolean;
    enableSoundNotifications?: boolean;
    enableVibration?: boolean;
  };
}

// 内部实现，不直接导出
function _useUserSettings() {
  const { currentUser } = useAuth();
  const settings = ref<UserSettings | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // 从本地存储获取设置
  const loadLocalSettings = () => {
    console.log('【loadLocalSettings】尝试从本地存储加载设置');
    
    // 确保settings.value至少有默认值
    if (!settings.value) {
      settings.value = {
        theme: {
          color: '#1890ff',
          chatBackground: 'default',
          fontSize: 14
        },
        privacy: {
          showOnlineStatus: true,
          allowFriendRequests: true,
          showLastSeen: true
        },
        notification: {
          enableNotifications: true,
          enableSoundNotifications: true,
          enableVibration: true
        }
      };
      console.log('【loadLocalSettings】初始化默认设置:', settings.value);
    }
    
    try {
      const localSettings = localStorage.getItem('userSettings');
      if (localSettings) {
        console.log('【loadLocalSettings】找到本地存储的设置');
        const parsedSettings = JSON.parse(localSettings);
        
        // 合并设置，确保所有必要的字段都存在
        settings.value = {
          theme: {
            color: parsedSettings.theme?.color || '#1890ff',
            chatBackground: parsedSettings.theme?.chatBackground || 'default',
            fontSize: parsedSettings.theme?.fontSize || 14
          },
          privacy: {
            showOnlineStatus: parsedSettings.privacy?.showOnlineStatus !== undefined ? parsedSettings.privacy.showOnlineStatus : true,
            allowFriendRequests: parsedSettings.privacy?.allowFriendRequests !== undefined ? parsedSettings.privacy.allowFriendRequests : true,
            showLastSeen: parsedSettings.privacy?.showLastSeen !== undefined ? parsedSettings.privacy.showLastSeen : true
          },
          notification: {
            enableNotifications: parsedSettings.notification?.enableNotifications !== undefined ? parsedSettings.notification.enableNotifications : true,
            enableSoundNotifications: parsedSettings.notification?.enableSoundNotifications !== undefined ? parsedSettings.notification.enableSoundNotifications : true,
            enableVibration: parsedSettings.notification?.enableVibration !== undefined ? parsedSettings.notification.enableVibration : true
          }
        };
        
        // 加载本地设置后立即应用
        console.log('【loadLocalSettings】应用从本地加载的设置:', settings.value);
        applySettingsToUI();
        return true;
      } else {
        console.log('【loadLocalSettings】本地存储中没有设置');
        // 确保settings.value有值（已在函数开始时初始化）
        applySettingsToUI(); // 应用默认设置
      }
    } catch (err) {
      console.error('【loadLocalSettings】加载本地设置失败:', err);
      // 确保settings.value有值（已在函数开始时初始化）
      applySettingsToUI(); // 应用默认设置
    }
    return false;
  };

  // 保存设置到本地存储
  const saveLocalSettings = () => {
    try {
      if (settings.value) {
        localStorage.setItem('userSettings', JSON.stringify(settings.value));
      }
    } catch (err) {
      console.error('保存本地设置失败:', err);
    }
  };

  // 应用设置到UI
  const applySettingsToUI = () => {
    console.log('【useUserSettings】正在应用设置到UI', settings.value);
    
    if (!settings.value) {
      console.warn('【useUserSettings】无法应用设置：settings.value为空');
      return;
    }
    
    if (settings.value.theme) {
      if (settings.value.theme.color) {
        console.log('【useUserSettings】应用主题颜色:', settings.value.theme.color);
        applyThemeColor(settings.value.theme.color);
      } else {
        console.log('【useUserSettings】未设置主题颜色，使用默认值');
        applyThemeColor('#1890ff'); // 应用默认值
      }
      
      if (settings.value.theme.fontSize) {
        console.log('【useUserSettings】应用字体大小:', settings.value.theme.fontSize);
        applyFontSize(settings.value.theme.fontSize);
      } else {
        console.log('【useUserSettings】未设置字体大小，使用默认值');
        applyFontSize(14); // 应用默认值
      }
      
      if (settings.value.theme.chatBackground) {
        console.log('【useUserSettings】应用聊天背景:', settings.value.theme.chatBackground);
        applyBackground(settings.value.theme.chatBackground);
      } else {
        console.log('【useUserSettings】未设置聊天背景，使用默认值');
        applyBackground('default'); // 应用默认值
      }
    } else {
      console.log('【useUserSettings】未设置主题，应用默认设置');
      applyThemeColor('#1890ff');
      applyFontSize(14);
      applyBackground('default');
    }
    
    // 强制触发DOM更新
    setTimeout(() => {
      console.log('【useUserSettings】强制刷新DOM样式');
      document.body.style.visibility = 'hidden';
      setTimeout(() => {
        document.body.style.visibility = '';
      }, 5);
    }, 10);
  };

  // 从服务器获取设置
  const fetchSettings = async () => {
    console.log('【fetchSettings】开始获取用户设置');
    
    // 确保settings.value至少有默认值
    if (!settings.value) {
      settings.value = {
        theme: {
          color: '#1890ff',
          chatBackground: 'default',
          fontSize: 14
        },
        privacy: {
          showOnlineStatus: true,
          allowFriendRequests: true,
          showLastSeen: true
        },
        notification: {
          enableNotifications: true,
          enableSoundNotifications: true,
          enableVibration: true
        }
      };
      console.log('【fetchSettings】初始化默认设置:', settings.value);
    }
    
    // 检查用户是否已登录，使用多种方式检测
    let userId = currentUser.value?.id;
    
    // 如果currentUser中没有用户ID，尝试从localStorage或sessionStorage获取
    if (!userId) {
      const localUserId = localStorage.getItem('userId');
      const sessionUserId = sessionStorage.getItem('userId');
      const localUserInfo = localStorage.getItem('userInfo');
      const sessionUserInfo = sessionStorage.getItem('userInfo');
      
      if (localUserId) {
        userId = parseInt(localUserId, 10);
        console.log('【fetchSettings】从localStorage.userId获取用户ID:', userId);
      } else if (sessionUserId) {
        userId = parseInt(sessionUserId, 10);
        console.log('【fetchSettings】从sessionStorage.userId获取用户ID:', userId);
      } else if (localUserInfo) {
        try {
          const userInfo = JSON.parse(localUserInfo);
          if (userInfo && userInfo.id) {
            userId = userInfo.id;
            console.log('【fetchSettings】从localStorage.userInfo获取用户ID:', userId);
          }
        } catch (e) {
          console.error('【fetchSettings】解析localStorage.userInfo失败:', e);
        }
      } else if (sessionUserInfo) {
        try {
          const userInfo = JSON.parse(sessionUserInfo);
          if (userInfo && userInfo.id) {
            userId = userInfo.id;
            console.log('【fetchSettings】从sessionStorage.userInfo获取用户ID:', userId);
          }
        } catch (e) {
          console.error('【fetchSettings】解析sessionStorage.userInfo失败:', e);
        }
      }
    }
    
    // 检查是否有token，这也是用户已登录的标志
    const hasToken = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
    
    if (!userId && !hasToken) {
      console.log('【fetchSettings】用户未登录，使用默认设置');
      return;
    }
    
    if (!userId && hasToken) {
      console.log('【fetchSettings】检测到token但无法获取用户ID，尝试使用默认ID');
      userId = 1; // 使用默认ID
    }
    
    loading.value = true;
    error.value = null;
    
    try {
      console.log(`【fetchSettings】尝试获取用户 ${userId} 的设置`);
      
      // 构建请求URL和头信息 - 修复URL路径，确保不重复/api/
      // 注意：api.get会自动添加/api前缀，所以这里不需要再加
      const url = `/user/settings${userId ? `?userId=${userId}` : ''}`;
      const headers: Record<string, string> = {};
      
      if (userId) {
        headers['X-User-Id'] = String(userId);
      }
      
      if (hasToken) {
        const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken') || '';
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      console.log('【fetchSettings】请求URL:', url);
      console.log('【fetchSettings】请求头:', headers);
      
      const response = await api.get(url, { headers });
      
      if (response.success && response.data) {
        console.log('【fetchSettings】成功获取设置:', response.data);
        
        settings.value = {
          theme: {
            color: response.data.themeColor || '#1890ff',
            chatBackground: response.data.chatBackground || 'default',
            fontSize: response.data.fontSize || 14
          },
          privacy: {
            showOnlineStatus: response.data.showOnlineStatus !== undefined ? response.data.showOnlineStatus : true,
            allowFriendRequests: response.data.allowFriendRequests !== undefined ? response.data.allowFriendRequests : true,
            showLastSeen: response.data.showLastSeen !== undefined ? response.data.showLastSeen : true
          },
          notification: {
            enableNotifications: response.data.enableNotifications !== undefined ? response.data.enableNotifications : true,
            enableSoundNotifications: response.data.enableSoundNotifications !== undefined ? response.data.enableSoundNotifications : true,
            enableVibration: response.data.enableVibration !== undefined ? response.data.enableVibration : true
          }
        };
        
        // 保存到本地存储
        saveLocalSettings();
        
        // 立即应用设置
        applySettingsToUI();
        
        console.log('【fetchSettings】成功从API获取并应用设置');
      } else {
        console.warn('【fetchSettings】API返回失败或无数据:', response);
        // API请求成功但没有数据，使用默认设置
        // settings.value 已在函数开始时初始化，这里不需要重复设置
      }
    } catch (err) {
      console.error('【fetchSettings】获取用户设置失败:', err);
      error.value = '获取设置失败';
      
      // 如果服务器请求失败，尝试从本地加载
      const hasLocalSettings = loadLocalSettings();
      if (!hasLocalSettings) {
        // 本地也没有设置，确保settings.value有值
        // settings.value 已在函数开始时初始化，这里不需要重复设置
        console.log('【fetchSettings】无法从服务器或本地获取设置，使用默认设置');
      }
    } finally {
      loading.value = false;
    }
    
    // 确保在函数结束时应用设置
    console.log('【fetchSettings】函数结束，确保应用设置');
    applySettingsToUI();
  };

  // 更新设置
  const updateSettings = async (newSettings: UpdateSettingsRequest) => {
    if (!currentUser.value?.id) {
      console.warn('无法更新设置：用户未登录');
      return;
    }
    
    
    error.value = null;
    
    console.log('开始更新用户设置:', JSON.stringify(newSettings, null, 2));
    
    try {
      // 合并设置
      settings.value = {
        ...settings.value,
        ...newSettings
      };
      
      // 保存到本地存储
      saveLocalSettings();
      
      // 立即应用设置
      applySettingsToUI();
      
      // 发送到服务器
      const requestData = {
        themeColor: newSettings.theme?.color,
        chatBackground: newSettings.theme?.chatBackground,
        fontSize: newSettings.theme?.fontSize,
        showOnlineStatus: newSettings.privacy?.showOnlineStatus,
        allowFriendRequests: newSettings.privacy?.allowFriendRequests,
        showLastSeen: newSettings.privacy?.showLastSeen,
        enableNotifications: newSettings.notification?.enableNotifications,
        enableSoundNotifications: newSettings.notification?.enableSoundNotifications,
        enableVibration: newSettings.notification?.enableVibration
      };
      
      console.log('向服务器发送设置更新请求:', JSON.stringify(requestData, null, 2));
      console.log('请求URL:', '/user/settings'); // 修复URL路径
      
      try {
        // 添加用户ID作为查询参数
        const userId = currentUser.value.id;
        const response = await api.put(`/user/settings?userId=${userId}`, requestData, {
          headers: {
            'X-User-Id': String(userId) // 添加额外的用户ID头
          }
        });
        console.log('设置更新响应:', response);
      
      if (!response.success) {
        throw new Error(response.message || '更新设置失败');
      }
        
        console.log('设置更新成功');
      } catch (err: any) {
        console.error('服务器设置更新失败:', err);
        console.error('错误详情:', err.message, err.stack);
        error.value = '更新设置失败: ' + (err.message || '未知错误');
        // 尽管服务器请求失败，我们仍然保留了本地设置
      }
    } catch (err: any) {
      console.error('更新用户设置失败:', err);
      error.value = '更新设置失败: ' + (err.message || '未知错误');
    } finally {
      loading.value = false;
    }
  };

  // 重置设置
  const resetSettings = async () => {
    if (!currentUser.value?.id) return;
    
    loading.value = true;
    error.value = null;
    
    try {
      // 默认设置
      const defaultSettings: UserSettings = {
        theme: {
          color: '#1890ff',
          chatBackground: 'default',
          fontSize: 14
        },
        privacy: {
          showOnlineStatus: true,
          allowFriendRequests: true,
          showLastSeen: true
        },
        notification: {
          enableNotifications: true,
          enableSoundNotifications: true,
          enableVibration: true
        }
      };
      
      // 更新本地设置
      settings.value = defaultSettings;
      saveLocalSettings();
      
      // 立即应用默认设置
      applySettingsToUI();
      
      // 发送到服务器
      try {
        const userId = currentUser.value.id;
        const response = await api.post(`/user/settings/reset?userId=${userId}`);
      
      if (!response.success) {
        throw new Error(response.message || '重置设置失败');
        }
        
        console.log('服务器设置重置成功');
      } catch (err) {
        console.error('服务器设置重置失败:', err);
        // 尽管服务器请求失败，我们仍然重置了本地设置
      }
    } catch (err) {
      console.error('重置用户设置失败:', err);
      error.value = '重置设置失败';
    } finally {
      loading.value = false;
    }
  };

  // 初始化
  onMounted(() => {
    // 先尝试从本地加载
    const hasLocalSettings = loadLocalSettings();
    
    // 如果没有本地设置或用户已登录，从服务器获取
    if (!hasLocalSettings || currentUser.value?.id) {
      fetchSettings();
    }
  });

  return {
    settings,
    loading,
    error,
    fetchSettings,
    updateSettings,
    resetSettings,
    applySettingsToUI,
    loadLocalSettings,
    saveLocalSettings
  };
}

// 为了向后兼容，保留原始的useUserSettings导出
export const useUserSettings = getUserSettings; 