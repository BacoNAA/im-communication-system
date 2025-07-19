import { ref, computed, onMounted } from 'vue';
import { useAuth } from './useAuth';
import { api } from '@/api/request';
import { applyThemeColor, applyFontSize, applyBackground } from '@/utils/themeUtils';

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

export function useUserSettings() {
  const { currentUser } = useAuth();
  const settings = ref<UserSettings | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // 从本地存储获取设置
  const loadLocalSettings = () => {
    try {
      const localSettings = localStorage.getItem('userSettings');
      if (localSettings) {
        settings.value = JSON.parse(localSettings);
        // 加载本地设置后立即应用
        applySettingsToUI();
        return true;
      }
    } catch (err) {
      console.error('加载本地设置失败:', err);
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
    console.log('正在应用设置到UI', settings.value);
    if (settings.value?.theme) {
      if (settings.value.theme.color) {
        console.log('应用主题颜色:', settings.value.theme.color);
        applyThemeColor(settings.value.theme.color);
      }
      
      if (settings.value.theme.fontSize) {
        console.log('应用字体大小:', settings.value.theme.fontSize);
        applyFontSize(settings.value.theme.fontSize);
      }
      
      if (settings.value.theme.chatBackground) {
        console.log('应用聊天背景:', settings.value.theme.chatBackground);
        applyBackground(settings.value.theme.chatBackground);
      }
    }
  };

  // 从服务器获取设置
  const fetchSettings = async () => {
    if (!currentUser.value?.id) return;
    
    loading.value = true;
    error.value = null;
    
    try {
      // 添加用户ID作为参数，以防认证问题
      const userId = currentUser.value.id;
      const response = await api.get(`/api/user/settings?userId=${userId}`);
      
      if (response.success && response.data) {
        settings.value = {
          theme: {
            color: response.data.themeColor || '#1890ff',
            chatBackground: response.data.chatBackground || 'default',
            fontSize: response.data.fontSize || 14
          },
          privacy: {
            showOnlineStatus: response.data.showOnlineStatus,
            allowFriendRequests: response.data.allowFriendRequests,
            showLastSeen: response.data.showLastSeen
          },
          notification: {
            enableNotifications: response.data.enableNotifications,
            enableSoundNotifications: response.data.enableSoundNotifications,
            enableVibration: response.data.enableVibration
          }
        };
        
        // 保存到本地存储
        saveLocalSettings();
        
        // 立即应用设置
        applySettingsToUI();
        
        console.log('成功从API获取并应用设置');
      }
    } catch (err) {
      console.error('获取用户设置失败:', err);
      error.value = '获取设置失败';
      
      // 如果服务器请求失败，尝试从本地加载
      loadLocalSettings();
    } finally {
      loading.value = false;
    }
  };

  // 更新设置
  const updateSettings = async (newSettings: UpdateSettingsRequest) => {
    if (!currentUser.value?.id) {
      console.warn('无法更新设置：用户未登录');
      return;
    }
    
    loading.value = true;
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
      console.log('请求URL:', '/api/user/settings');
      
      try {
        // 添加用户ID作为查询参数
        const userId = currentUser.value.id;
        const response = await api.put(`/api/user/settings?userId=${userId}`, requestData, {
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
        const response = await api.post(`/api/user/settings/reset?userId=${userId}`);
        
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
    applySettingsToUI
  };
} 