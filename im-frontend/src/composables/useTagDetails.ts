import { ref, computed, readonly } from 'vue';
import { tagApi } from '@/api';
import type { ContactTag } from '@/api/tag';
import type { Contact } from '@/api/contact';
import { useAuth } from './useAuth';
import { useMessage } from './useMessage';
import { useRouter } from 'vue-router';

// 标签详情页面状态
const currentTag = ref<ContactTag | null>(null);
const tagContacts = ref<Contact[]>([]);
const isLoading = ref(false);
const error = ref<string | null>(null);
const isTagDetailsVisible = ref(false);

export function useTagDetails() {
  const { currentUser } = useAuth();
  const { showError, showSuccess } = useMessage();
  const router = useRouter();

  // 计算属性
  const hasContacts = computed(() => tagContacts.value.length > 0);
  const contactsCount = computed(() => tagContacts.value.length);

  // 显示指定标签下的好友
  const viewTagContacts = async (tagId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      showError('请先登录');
      return;
    }

    const token = localStorage.getItem('accessToken');
    if (!token) {
      showError('用户未登录');
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      // 加载标签详情
      await loadTagDetails(tagId);
      
      // 显示标签详情页面
      isTagDetailsVisible.value = true;
      
      // 设置页面标题
      if (currentTag.value) {
        document.title = `${currentTag.value.name} - 标签详情`;
      }
      
    } catch (err: any) {
      error.value = err.message || '加载标签详情失败';
      showError(error.value || '加载标签详情失败');
    } finally {
      isLoading.value = false;
    }
  };

  // 根据标签ID加载标签详情
  const loadTagDetails = async (tagId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      throw new Error('请先登录');
    }

    const token = localStorage.getItem('accessToken');
    if (!token) {
      throw new Error('用户未登录');
    }

    try {
      isLoading.value = true;
      error.value = null;
      console.log('加载标签详情，标签ID:', tagId);
      
      // 获取标签信息
      const tagResponse = await tagApi.getTagById(tagId);
      console.log('获取标签信息响应:', tagResponse);
      
      if (!tagResponse.success || !tagResponse.data) {
        throw new Error(tagResponse.message || '获取标签信息失败');
      }
      
      // 确保标签对象有id字段，同时保留tagId字段
      const tagData = tagResponse.data;
      if (tagData.tagId && !tagData.id) {
        tagData.id = tagData.tagId;
      } else if (tagData.id && !tagData.tagId) {
        tagData.tagId = tagData.id;
      }
      
      currentTag.value = tagData;
      console.log('当前标签数据:', currentTag.value);
      
      // 获取标签下的好友列表
      const contactsResponse = await tagApi.getTagContacts(tagId);
      console.log('获取标签联系人响应:', contactsResponse);
      
      if (contactsResponse.success && contactsResponse.data) {
        tagContacts.value = contactsResponse.data;
        console.log('标签联系人数据:', tagContacts.value);
      } else {
        tagContacts.value = [];
        console.log('标签联系人为空');
      }
      
    } catch (err: any) {
      console.error('加载标签详情失败:', err);
      
      if (err.status === 401) {
        // 处理认证失败
        const accessToken = localStorage.getItem('accessToken');
        const refreshToken = localStorage.getItem('refreshToken');
        const userInfo = localStorage.getItem('userInfo');
        
        if (accessToken !== null) localStorage.removeItem('accessToken');
        if (refreshToken !== null) localStorage.removeItem('refreshToken');
        if (userInfo !== null) localStorage.removeItem('userInfo');
        
        const sessionAccessToken = sessionStorage.getItem('accessToken');
        const sessionRefreshToken = sessionStorage.getItem('refreshToken');
        const sessionUserInfo = sessionStorage.getItem('userInfo');
        
        if (sessionAccessToken !== null) sessionStorage.removeItem('accessToken');
        if (sessionRefreshToken !== null) sessionStorage.removeItem('refreshToken');
        if (sessionUserInfo !== null) sessionStorage.removeItem('userInfo');
        
        router.push('/login');
        return;
      }
      
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 显示标签下的好友列表
  const displayTagContacts = (contacts: Contact[]): void => {
    tagContacts.value = contacts;
  };

  // 从标签中移除好友
  const removeContactFromTag = async (contactId: number, contactName: string): Promise<void> => {
    if (!currentTag.value) {
      showError('标签信息不存在');
      return;
    }

    // 显示确认对话框
    const confirmed = confirm(`确定要将 "${contactName}" 从标签 "${currentTag.value.name}" 中移除吗？`);
    if (!confirmed) {
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      // 确保有有效的ID，使用非空断言或提供默认值
      const tagId = currentTag.value.id || currentTag.value.tagId;
      if (!tagId) {
        throw new Error('标签ID无效');
      }
      
      // 调用API移除好友
      const response = await tagApi.removeContactFromTag(tagId, contactId);
      
      if (response.success) {
        showSuccess('移除成功');
        // 刷新标签详情
        await refreshTagDetails();
      } else {
        throw new Error(response.message || '移除失败');
      }
      
    } catch (err: any) {
      error.value = err.message || '移除失败';
      showError(error.value || '移除失败');
      
      if (err.status === 401) {
        const accessToken = localStorage.getItem('accessToken');
        const refreshToken = localStorage.getItem('refreshToken');
        const userInfo = localStorage.getItem('userInfo');
        
        if (accessToken !== null) localStorage.removeItem('accessToken');
        if (refreshToken !== null) localStorage.removeItem('refreshToken');
        if (userInfo !== null) localStorage.removeItem('userInfo');
        
        const sessionAccessToken = sessionStorage.getItem('accessToken');
        const sessionRefreshToken = sessionStorage.getItem('refreshToken');
        const sessionUserInfo = sessionStorage.getItem('userInfo');
        
        if (sessionAccessToken !== null) sessionStorage.removeItem('accessToken');
        if (sessionRefreshToken !== null) sessionStorage.removeItem('refreshToken');
        if (sessionUserInfo !== null) sessionStorage.removeItem('userInfo');
        
        router.push('/login');
      }
    } finally {
      isLoading.value = false;
    }
  };

  // 刷新标签详情
  const refreshTagDetails = async (): Promise<void> => {
    if (!currentTag.value) {
      return;
    }
    
    // 确保有有效的ID，使用非空断言或提供默认值
    const tagId = currentTag.value.id || currentTag.value.tagId || 0;
    if (tagId === 0) {
      console.error('标签ID无效');
      return;
    }
    
    await loadTagDetails(tagId);
  };

  // 返回标签页面
  const backToTags = (): void => {
    isTagDetailsVisible.value = false;
    currentTag.value = null;
    tagContacts.value = [];
    error.value = null;
    
    // 恢复页面标题
    document.title = '即时通讯系统';
    
    // 如果在路由环境中，可以使用路由跳转
    if (router) {
      router.push('/dashboard?tab=contacts');
    }
  };

  // 编辑标签（从标签详情页面）
  const editTagFromDetails = (): void => {
    if (!currentTag.value) {
      showError('标签信息不存在');
      return;
    }
    
    // 确保标签ID存在
    const tagId = currentTag.value.id || currentTag.value.tagId;
    if (!tagId) {
      showError('标签ID无效');
      return;
    }
    
    // 触发编辑标签事件
    // 这里可以通过事件总线或者其他方式通知父组件打开编辑模态框
    const event = new CustomEvent('edit-tag', {
      detail: { tag: currentTag.value }
    });
    window.dispatchEvent(event);
  };

  // 显示添加好友到标签的模态框
  const showAddContactToTagModal = (): void => {
    if (!currentTag.value) {
      showError('标签信息不存在');
      return;
    }
    
    // 触发显示添加好友模态框事件
    const event = new CustomEvent('show-add-contact-to-tag', {
      detail: { tag: currentTag.value }
    });
    window.dispatchEvent(event);
  };

  // 关闭添加好友到标签的模态框
  const closeAddContactToTagModal = (): void => {
    // 触发关闭添加好友模态框事件
    const event = new CustomEvent('close-add-contact-to-tag');
    window.dispatchEvent(event);
  };

  // 清除所有数据
  const clearAllData = (): void => {
    currentTag.value = null;
    tagContacts.value = [];
    error.value = null;
    isTagDetailsVisible.value = false;
  };

  return {
    // 状态
    currentTag: readonly(currentTag),
    tagContacts: readonly(tagContacts),
    isLoading: readonly(isLoading),
    error: readonly(error),
    isTagDetailsVisible: readonly(isTagDetailsVisible),
    
    // 计算属性
    hasContacts,
    contactsCount,
    
    // 方法
    viewTagContacts,
    loadTagDetails,
    displayTagContacts,
    removeContactFromTag,
    refreshTagDetails,
    backToTags,
    editTagFromDetails,
    showAddContactToTagModal,
    closeAddContactToTagModal,
    clearAllData
  };
}

// 导出单例实例供全局使用
export const tagDetailsInstance = useTagDetails();