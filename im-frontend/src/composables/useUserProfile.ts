import { ref, computed, readonly } from 'vue';
import { userApi, type UserProfile, type UserStatus, type PersonalId } from '@/api/user';

const userProfile = ref<UserProfile | null>(null);
const isLoading = ref(false);
const error = ref<string | null>(null);

export function useUserProfile() {
  // 计算属性
  const isProfileLoaded = computed(() => userProfile.value !== null);
  const displayName = computed(() => {
    if (!userProfile.value) return '';
    return userProfile.value.name || userProfile.value.email;
  });

  // 加载用户资料
  const loadUserProfile = async (): Promise<void> => {
    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await userApi.getProfile();
      
      if (response.success && response.data) {
        userProfile.value = response.data;
      } else {
        throw new Error(response.message || '获取用户资料失败');
      }
    } catch (err: any) {
      error.value = err.message || '获取用户资料失败';
      
      // 如果是401错误，可能需要重新登录
      if (err.status === 401) {
        // 清除本地存储的认证信息
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userInfo');
        sessionStorage.removeItem('accessToken');
        sessionStorage.removeItem('refreshToken');
        sessionStorage.removeItem('userInfo');
        
        // 抛出特殊错误，让调用方处理导航
        throw new Error('UNAUTHORIZED');
      }
      
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 保存用户资料
  const saveProfile = async (profileData: {
    nickname?: string;
    signature?: string;
    phoneNumber?: string;
    birthday?: string;
    gender?: string;
    location?: string;
    occupation?: string;
    personalId?: string;
    status?: string;
  }): Promise<void> => {
    try {
      isLoading.value = true;
      error.value = null;
      
      const promises: Promise<any>[] = [];
      
      // 1. 更新基本资料（如果有相关字段）
      const basicProfileFields = {
        nickname: profileData.nickname,
        phoneNumber: profileData.phoneNumber,
        birthday: profileData.birthday,
        gender: profileData.gender,
        location: profileData.location,
        occupation: profileData.occupation
      };
      
      // 过滤掉undefined的字段
      const filteredBasicFields = Object.fromEntries(
        Object.entries(basicProfileFields).filter(([_, value]) => value !== undefined)
      );
      
      if (Object.keys(filteredBasicFields).length > 0) {
        promises.push(userApi.updateProfile(filteredBasicFields));
      }
      
      // 2. 设置个人ID（如果提供）
      if (profileData.personalId !== undefined) {
        promises.push(userApi.setPersonalId(profileData.personalId));
      }
      
      // 3. 更新状态和签名（如果提供）
      if (profileData.status !== undefined || profileData.signature !== undefined) {
        const statusData: UserStatus = {
          status: profileData.status || userProfile.value?.status || 'online'
        };
        
        if (profileData.signature !== undefined) {
          statusData.signature = profileData.signature;
        }
        
        promises.push(userApi.updateStatus(statusData));
      }
      
      // 执行所有API调用
      const results = await Promise.all(promises);
      
      // 检查所有请求是否成功
      for (const result of results) {
        if (!result.success) {
          throw new Error(result.message || '保存失败');
        }
      }
      
      // 重新加载用户资料以获取最新数据
      await loadUserProfile();
      
    } catch (err: any) {
      error.value = err.message || '保存用户资料失败';
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 上传头像
  const uploadAvatar = async (file: File): Promise<void> => {
    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await userApi.uploadAvatar(file);
      
      if (response.success && response.data) {
        // 更新本地用户资料中的头像URL
        if (userProfile.value) {
          userProfile.value.avatar = response.data.avatarUrl;
        }
      } else {
        throw new Error(response.message || '上传头像失败');
      }
    } catch (err: any) {
      error.value = err.message || '上传头像失败';
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 清除用户资料
  const clearProfile = (): void => {
    userProfile.value = null;
    error.value = null;
  };

  return {
    // 状态
    userProfile: readonly(userProfile),
    isLoading: readonly(isLoading),
    error: readonly(error),
    
    // 计算属性
    isProfileLoaded,
    displayName,
    
    // 方法
    loadUserProfile,
    saveProfile,
    uploadAvatar,
    clearProfile
  };
}