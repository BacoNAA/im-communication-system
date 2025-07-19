import { ref, computed, readonly } from 'vue';
import { contactApi, type Contact, type FriendRequest, type ContactTag, type AddFriendRequest, type SearchUserRequest, type ContactUser } from '@/api/contact';
import { useAuth } from './useAuth';

const contacts = ref<Contact[]>([]);
const friendRequests = ref<FriendRequest[]>([]);
const contactTags = ref<ContactTag[]>([]);
const searchResults = ref<ContactUser[]>([]);
const isLoading = ref(false);
const error = ref<string | null>(null);

export function useContacts() {
  const { currentUser } = useAuth();

  // 计算属性
  const pendingRequestsCount = computed(() => {
    return friendRequests.value.filter(req => req.status === 'PENDING').length;
  });

  const contactsCount = computed(() => contacts.value.length);

  // 按标签分组的联系人
  const contactsByTag = computed(() => {
    const grouped: { [tagName: string]: Contact[] } = {};
    
    contacts.value.forEach(contact => {
      if (contact.tags && contact.tags.length > 0) {
        contact.tags.forEach(tag => {
          if (tag?.name) {
            const tagName = tag.name;
            if (!grouped[tagName]) {
              grouped[tagName] = [];
            }
            grouped[tagName].push(contact);
          }
        });
      } else {
        // 未分组的联系人
        if (!grouped['未分组']) {
          grouped['未分组'] = [];
        }
        grouped['未分组'].push(contact);
      }
    });
    
    return grouped;
  });

  // 获取联系人列表
  const loadContacts = async (includeBlocked: boolean = false, manualUserId?: number): Promise<void> => {
    // 优先使用传入的用户ID，其次使用currentUser中的ID
    const userId = manualUserId || currentUser.value?.id;
    
    if (!userId) {
      console.error('loadContacts: 当前用户未登录或ID不存在');
      error.value = '请先登录';
      return;
    }

    console.log('loadContacts: 开始加载联系人列表, 用户ID:', userId);

    try {
      isLoading.value = true;
      error.value = null;
      
      console.log('loadContacts: 发起API请求, 用户ID:', userId, '包含已屏蔽:', includeBlocked);
      const response = await contactApi.getContacts(userId, includeBlocked);
      console.log('loadContacts: API响应结果:', response);
      
      if (response.success && response.data) {
        console.log('loadContacts: 请求成功, 联系人数量:', response.data.length);
        contacts.value = response.data;
        console.log('loadContacts: 联系人列表已更新, 当前数量:', contacts.value.length);
      } else {
        console.error('loadContacts: 请求成功但数据无效:', response);
        throw new Error(response.message || '获取联系人列表失败');
      }
    } catch (err: any) {
      console.error('loadContacts: 请求失败:', err);
      error.value = err.message || '获取联系人列表失败';
      
      if (err.status === 401) {
        console.error('loadContacts: 未授权错误, 清除登录信息');
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

  // 搜索用户
  const searchUsers = async (keyword: string): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    if (!keyword.trim()) {
      searchResults.value = [];
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const searchData: SearchUserRequest = {
        keyword: keyword.trim()
      };
      
      const response = await contactApi.searchUser(searchData, currentUser.value.id);
      
      if (response.success && response.data) {
        searchResults.value = response.data;
      } else {
        throw new Error(response.message || '搜索用户失败');
      }
    } catch (err: any) {
      error.value = err.message || '搜索用户失败';
      searchResults.value = [];
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 发送好友请求
  const sendFriendRequest = async (friendIdentifier: string, message?: string): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const requestData: AddFriendRequest = {
        friendIdentifier: friendIdentifier.trim()
      };
      
      if (message?.trim()) {
        requestData.message = message.trim();
      }
      
      const response = await contactApi.sendFriendRequest(requestData, currentUser.value.id);
      
      if (response.success) {
        // 重新加载好友请求列表
        await loadFriendRequests('sent');
      } else {
        throw new Error(response.message || '发送好友请求失败');
      }
    } catch (err: any) {
      error.value = err.message || '发送好友请求失败';
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 获取好友请求列表
  const loadFriendRequests = async (type: 'sent' | 'received' = 'received'): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await contactApi.getFriendRequests(currentUser.value.id, type);
      
      if (response.success && response.data) {
        friendRequests.value = response.data;
      } else {
        throw new Error(response.message || '获取好友请求失败');
      }
    } catch (err: any) {
      error.value = err.message || '获取好友请求失败';
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 处理好友请求
  const handleFriendRequest = async (requestId: number, action: 'accept' | 'reject'): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await contactApi.handleFriendRequest(requestId, action, currentUser.value.id);
      
      if (response.success) {
        // 重新加载好友请求和联系人列表
        await Promise.all([
          loadFriendRequests('received'),
          loadContacts()
        ]);
      } else {
        throw new Error(response.message || `${action === 'accept' ? '接受' : '拒绝'}好友请求失败`);
      }
    } catch (err: any) {
      error.value = err.message || `${action === 'accept' ? '接受' : '拒绝'}好友请求失败`;
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 删除好友
  const deleteFriend = async (friendId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await contactApi.deleteFriend(friendId, currentUser.value.id);
      
      if (response.success) {
        // 重新加载联系人列表
        await loadContacts();
      } else {
        throw new Error(response.message || '删除好友失败');
      }
    } catch (err: any) {
      error.value = err.message || '删除好友失败';
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 拉黑好友
  const blockFriend = async (friendId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await contactApi.blockFriend(friendId, currentUser.value.id);
      
      if (response.success) {
        // 重新加载联系人列表
        await loadContacts();
      } else {
        throw new Error(response.message || '拉黑好友失败');
      }
    } catch (err: any) {
      error.value = err.message || '拉黑好友失败';
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 取消拉黑
  const unblockFriend = async (friendId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await contactApi.unblockFriend(friendId, currentUser.value.id);
      
      if (response.success) {
        // 重新加载联系人列表
        await loadContacts();
      } else {
        throw new Error(response.message || '取消拉黑失败');
      }
    } catch (err: any) {
      error.value = err.message || '取消拉黑失败';
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 设置联系人别名
  const setContactAlias = async (friendId: number, alias: string): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await contactApi.setContactAlias(friendId, { alias: alias.trim() }, currentUser.value.id);
      
      if (response.success) {
        // 重新加载联系人列表
        await loadContacts();
      } else {
        throw new Error(response.message || '设置别名失败');
      }
    } catch (err: any) {
      error.value = err.message || '设置别名失败';
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 获取联系人标签
  const loadContactTags = async (): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      const response = await contactApi.getContactTags(currentUser.value.id);
      
      if (response.success && response.data) {
        contactTags.value = response.data;
      } else {
        throw new Error(response.message || '获取标签失败');
      }
    } catch (err: any) {
      error.value = err.message || '获取标签失败';
      throw err;
    }
  };

  // 创建联系人标签
  const createContactTag = async (name: string, color?: string): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const tagData = { name: name.trim(), ...(color && { color }) };
      const response = await contactApi.createContactTag(tagData, currentUser.value.id);
      
      if (response.success) {
        // 重新加载标签列表
        await loadContactTags();
      } else {
        throw new Error(response.message || '创建标签失败');
      }
    } catch (err: any) {
      error.value = err.message || '创建标签失败';
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 为联系人分配标签
  const assignContactTags = async (friendId: number, tagIds: number[]): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await contactApi.assignContactTags(friendId, { tagIds }, currentUser.value.id);
      
      if (response.success) {
        // 重新加载联系人列表
        await loadContacts();
      } else {
        throw new Error(response.message || '分配标签失败');
      }
    } catch (err: any) {
      error.value = err.message || '分配标签失败';
      throw err;
    } finally {
      isLoading.value = false;
    }
  };

  // 清除搜索结果
  const clearSearchResults = (): void => {
    searchResults.value = [];
  };

  // 清除所有数据
  const clearAllData = (): void => {
    contacts.value = [];
    friendRequests.value = [];
    contactTags.value = [];
    searchResults.value = [];
    error.value = null;
  };

  return {
    // 状态
    contacts: readonly(contacts),
    friendRequests: readonly(friendRequests),
    contactTags: readonly(contactTags),
    searchResults: readonly(searchResults),
    isLoading: readonly(isLoading),
    error: readonly(error),
    
    // 计算属性
    pendingRequestsCount,
    contactsCount,
    contactsByTag,
    
    // 方法
    loadContacts,
    searchUsers,
    sendFriendRequest,
    loadFriendRequests,
    handleFriendRequest,
    deleteFriend,
    blockFriend,
    unblockFriend,
    setContactAlias,
    loadContactTags,
    createContactTag,
    assignContactTags,
    clearSearchResults,
    clearAllData
  };
}