import { ref, computed, readonly } from 'vue';
import { tagApi, type ContactTag, type CreateTagRequest, type UpdateTagRequest, type AssignTagsRequest } from '@/api/tag';
import { useAuth } from './useAuth';

const allTags = ref<ContactTag[]>([]);
const contactTags = ref<ContactTag[]>([]);
const isLoading = ref(false);
const error = ref<string | null>(null);

export function useTags() {
  const { currentUser } = useAuth();

  // 计算属性
  const totalTagsCount = computed(() => allTags.value.length);
  
  const usedTagsCount = computed(() => {
    return allTags.value.filter(tag => (tag.contactCount || 0) > 0).length;
  });
  
  const recentTagsCount = computed(() => {
    // 最近创建的标签数量（简化为最多3个）
    return Math.min(allTags.value.length, 3);
  });

  // 按使用频率排序的标签
  const tagsByUsage = computed(() => {
    return [...allTags.value].sort((a, b) => (b.contactCount || 0) - (a.contactCount || 0));
  });

  // 获取所有标签
  const loadTags = async (): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await tagApi.getTags(currentUser.value.id);
      
      if (response.success && response.data) {
        allTags.value = response.data;
      } else {
        throw new Error(response.message || '加载标签失败');
      }
    } catch (err: any) {
      error.value = err.message || '加载标签失败';
      
      if (err.status === 401) {
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

  // 创建标签
  const createTag = async (name: string, color: string): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    // 参数验证
    if (!name.trim()) {
      throw new Error('标签名称不能为空');
    }

    if (name.length > 20) {
      throw new Error('标签名称不能超过20个字符');
    }

    if (!/^#[0-9A-Fa-f]{6}$/.test(color)) {
      throw new Error('颜色格式不正确');
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const requestData: CreateTagRequest = {
        userId: currentUser.value.id,
        name: name.trim(),
        color
      };
      
      const response = await tagApi.createTag(requestData);
      
      if (response.success) {
        // 重新加载标签列表
        await loadTags();
      } else {
        throw new Error(response.message || '创建标签失败');
      }
    } catch (err: any) {
      error.value = err.message || '创建标签失败';
      
      if (err.status === 401) {
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

  // 更新标签
  const updateTag = async (tagId: number, name: string, color: string): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    if (!name.trim()) {
      throw new Error('标签名称不能为空');
    }

    if (name.length > 20) {
      throw new Error('标签名称不能超过20个字符');
    }

    if (!/^#[0-9A-Fa-f]{6}$/.test(color)) {
      throw new Error('颜色格式不正确');
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const requestData: UpdateTagRequest = {
        name: name.trim(),
        color
      };
      
      const response = await tagApi.updateTag(tagId, requestData);
      
      if (response.success) {
        // 重新加载标签列表
        await loadTags();
      } else {
        throw new Error(response.message || '更新标签失败');
      }
    } catch (err: any) {
      error.value = err.message || '更新标签失败';
      
      if (err.status === 401) {
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

  // 删除标签
  const deleteTag = async (tagId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await tagApi.deleteTag(tagId);
      
      if (response.success) {
        // 重新加载标签列表
        await loadTags();
      } else {
        throw new Error(response.message || '删除标签失败');
      }
    } catch (err: any) {
      error.value = err.message || '删除标签失败';
      
      if (err.status === 401) {
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

  // 获取联系人的标签
  const loadContactTags = async (contactId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      const response = await tagApi.getContactTags(contactId);
      
      if (response.success && response.data) {
        contactTags.value = response.data;
      } else {
        throw new Error(response.message || '获取联系人标签失败');
      }
    } catch (err: any) {
      error.value = err.message || '获取联系人标签失败';
      
      if (err.status === 401) {
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
    }
  };

  // 为联系人分配标签
  const assignContactTags = async (contactId: number, tagIds: number[]): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const requestData: AssignTagsRequest = {
        tagIds
      };
      
      const response = await tagApi.assignContactTags(contactId, requestData);
      
      if (response.success) {
        // 重新加载联系人标签
        await loadContactTags(contactId);
        // 重新加载所有标签以更新使用计数
        await loadTags();
      } else {
        throw new Error(response.message || '分配标签失败');
      }
    } catch (err: any) {
      error.value = err.message || '分配标签失败';
      
      if (err.status === 401) {
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

  // 搜索标签
  const searchTags = (keyword: string): ContactTag[] => {
    if (!keyword.trim()) {
      return allTags.value;
    }
    
    const lowerKeyword = keyword.toLowerCase();
    return allTags.value.filter(tag => 
      tag.name.toLowerCase().includes(lowerKeyword)
    );
  };

  // 切换标签选择状态（用于分配标签界面）
  const toggleTagSelection = (tagId: number): void => {
    const index = contactTags.value.findIndex(tag => tag.id === tagId);
    
    if (index >= 0) {
      // 移除标签
      contactTags.value.splice(index, 1);
    } else {
      // 添加标签
      const tag = allTags.value.find(t => t.id === tagId);
      if (tag) {
        contactTags.value.push(tag);
      }
    }
  };

  // 检查标签是否被选中
  const isTagSelected = (tagId: number): boolean => {
    return contactTags.value.some(tag => tag.id === tagId);
  };

  // 获取标签的显示文本（用于头像生成）
  const getTagAvatarText = (tagName: string): string => {
    return tagName.charAt(0).toUpperCase();
  };

  // 验证标签名称
  const validateTagName = (name: string): { valid: boolean; message?: string } => {
    if (!name.trim()) {
      return { valid: false, message: '标签名称不能为空' };
    }
    
    if (name.length > 20) {
      return { valid: false, message: '标签名称不能超过20个字符' };
    }
    
    // 检查是否已存在同名标签
    const exists = allTags.value.some(tag => tag.name === name.trim());
    if (exists) {
      return { valid: false, message: '标签名称已存在' };
    }
    
    return { valid: true };
  };

  // 验证标签颜色
  const validateTagColor = (color: string): { valid: boolean; message?: string } => {
    if (!/^#[0-9A-Fa-f]{6}$/.test(color)) {
      return { valid: false, message: '颜色格式不正确，请使用十六进制格式（如 #667eea）' };
    }
    
    return { valid: true };
  };

  // 清除联系人标签
  const clearContactTags = (): void => {
    contactTags.value = [];
  };

  // 清除所有数据
  const clearAllData = (): void => {
    allTags.value = [];
    contactTags.value = [];
    error.value = null;
  };

  return {
    // 状态
    allTags: readonly(allTags),
    contactTags: readonly(contactTags),
    isLoading: readonly(isLoading),
    error: readonly(error),
    
    // 计算属性
    totalTagsCount,
    usedTagsCount,
    recentTagsCount,
    tagsByUsage,
    
    // 方法
    loadTags,
    createTag,
    updateTag,
    deleteTag,
    loadContactTags,
    assignContactTags,
    searchTags,
    toggleTagSelection,
    isTagSelected,
    getTagAvatarText,
    validateTagName,
    validateTagColor,
    clearContactTags,
    clearAllData
  };
}