import { ref, computed, reactive, readonly } from 'vue';
import { messageApi, type Conversation, type Message, type SendMessageRequest, type MessageType } from '@/api/message';
import { useAuth } from './useAuth';

const conversations = ref<Conversation[]>([]);
const currentConversation = ref<Conversation | null>(null);
const currentMessages = ref<Message[]>([]);
const isLoading = ref(false);
const error = ref<string | null>(null);

export function useMessages() {
  const { currentUser } = useAuth();

  // 计算属性
  const totalUnreadCount = computed(() => {
    return conversations.value.reduce((total, conv) => total + conv.unreadCount, 0);
  });

  const currentChatId = computed(() => currentConversation.value?.id || null);
  const currentChatName = computed(() => {
    if (!currentConversation.value || !currentUser.value) return '';
    return getConversationDisplayName(currentConversation.value);
  });

  // 获取会话列表
  const loadConversations = async (): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await messageApi.getConversations(currentUser.value.id, 0, 20);
      
      if (response.success && response.data) {
        // 处理嵌套的响应结构
        const content = response.data.content;
        if (content && content.length > 0) {
          const conversationResponse = content[0];
          if (conversationResponse && conversationResponse.conversations) {
            conversations.value = conversationResponse.conversations;
          } else {
            conversations.value = [];
          }
        } else {
          conversations.value = [];
        }
      } else {
        throw new Error(response.message || '获取会话列表失败');
      }
    } catch (err: any) {
      error.value = err.message || '获取会话列表失败';
      
      // 处理401错误
      if (err.status === 401) {
        // 清除认证信息并跳转到登录页
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

  // 获取指定会话的消息
  const loadMessages = async (conversationId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      isLoading.value = true;
      error.value = null;
      
      const response = await messageApi.getMessages(conversationId, currentUser.value.id, 0, 50);
      
      if (response.success && response.data) {
        // 消息按时间排序（最新的在底部）
        currentMessages.value = response.data.content.sort((a, b) => 
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        );
      } else {
        throw new Error(response.message || '获取消息失败');
      }
    } catch (err: any) {
      error.value = err.message || '获取消息失败';
      
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

  // 打开会话
  const openConversation = async (conversationId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      // 获取会话信息
      const convResponse = await messageApi.getConversation(conversationId, currentUser.value.id);
      
      if (convResponse.success && convResponse.data) {
        currentConversation.value = convResponse.data;
        
        // 加载消息
        await loadMessages(conversationId);
        
        // 标记为已读
        try {
          await messageApi.markAsRead(conversationId, currentUser.value.id);
          
          // 更新本地会话的未读数
          const conv = conversations.value.find(c => c.id === conversationId);
          if (conv) {
            conv.unreadCount = 0;
          }
        } catch (readError) {
          console.warn('标记已读失败:', readError);
        }
      } else {
        throw new Error(convResponse.message || '获取会话信息失败');
      }
    } catch (err: any) {
      error.value = err.message || '打开会话失败';
      throw err;
    }
  };

  // 发送消息
  const sendMessage = async (content: string, messageType: MessageType = 'TEXT', receiverId?: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    if (!content.trim()) {
      error.value = '消息内容不能为空';
      return;
    }

    try {
      const messageData: SendMessageRequest = {
        content: content.trim(),
        messageType,
        autoCreateConversation: true
      };

      // 如果有当前会话，使用会话ID
      if (currentConversation.value) {
        messageData.conversationId = currentConversation.value.id;
      }
      
      // 如果指定了接收者，使用接收者ID
      if (receiverId) {
        messageData.receiverId = receiverId;
      }

      const response = await messageApi.sendMessage(messageData, currentUser.value.id);
      
      if (response.success && response.data) {
        // 添加消息到当前消息列表
        const newMessage: Message = {
          id: response.data.id,
          conversationId: response.data.conversationId,
          senderId: response.data.senderId,
          content: response.data.content,
          messageType: response.data.messageType,
          createdAt: response.data.createdAt,
          updatedAt: response.data.createdAt,
          isRead: false
        };
        
        currentMessages.value.push(newMessage);
        
        // 重新加载会话列表以更新最后消息
        setTimeout(() => {
          loadConversations();
        }, 100);
      } else {
        throw new Error(response.message || '发送消息失败');
      }
    } catch (err: any) {
      error.value = err.message || '发送消息失败';
      throw err;
    }
  };

  // 获取或创建私聊会话
  const openPrivateChat = async (contactId: number): Promise<void> => {
    if (!currentUser.value?.id) {
      error.value = '请先登录';
      return;
    }

    try {
      const response = await messageApi.getOrCreatePrivateConversation(contactId, currentUser.value.id);
      
      if (response.success && response.data?.conversation) {
        const conversationId = response.data.conversation.id;
        await openConversation(conversationId);
        
        // 刷新会话列表
        setTimeout(() => {
          loadConversations();
        }, 500);
      } else {
        throw new Error(response.message || '创建会话失败');
      }
    } catch (err: any) {
      error.value = err.message || '创建会话失败';
      throw err;
    }
  };

  // 获取会话显示名称
  const getConversationDisplayName = (conversation: Conversation): string => {
    if (!currentUser.value) return '';
    
    if (conversation.type === 'PRIVATE') {
      // 私聊：查找对方参与者信息
      const otherParticipant = conversation.participants?.find(p => p.userId !== currentUser.value!.id);
      if (otherParticipant) {
        // 优先使用备注名，然后是用户昵称，最后是邮箱
        if (otherParticipant.alias) {
          return otherParticipant.alias;
        }
        if (otherParticipant.user?.nickname) {
          return otherParticipant.user.nickname;
        }
        if (otherParticipant.user?.email) {
          return otherParticipant.user.email;
        }
        return `用户${otherParticipant.userId}`;
      }
      return '私聊';
    } else {
      // 群聊：使用会话名称
      return conversation.name || '群聊';
    }
  };

  // 获取会话头像
  const getConversationAvatar = (conversation: Conversation): string => {
    if (!currentUser.value) return '';
    
    if (conversation.type === 'PRIVATE') {
      // 私聊：使用对方用户的头像
      const otherParticipant = conversation.participants?.find(p => p.userId !== currentUser.value!.id);
      if (otherParticipant?.user?.avatarUrl) {
        return otherParticipant.user.avatarUrl;
      }
      // 如果没有头像，返回空字符串，让组件显示默认头像
      return '';
    } else {
      // 群聊：使用会话头像
      return conversation.avatarUrl || '';
    }
  };

  // 格式化消息时间
  const formatMessageTime = (timeStr: string): string => {
    if (!timeStr) return '';
    
    const time = new Date(timeStr);
    const now = new Date();
    const diffMs = now.getTime() - time.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) {
      // 今天，显示时间
      return time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    } else if (diffDays === 1) {
      return '昨天';
    } else if (diffDays < 7) {
      return `${diffDays}天前`;
    } else {
      return time.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
    }
  };

  // 获取最后一条消息内容
  const getLastMessageContent = (lastMessage?: Message): string => {
    if (!lastMessage) {
      return '暂无消息';
    }
    
    // 根据消息类型显示不同内容
    switch (lastMessage.messageType) {
      case 'TEXT':
        return lastMessage.content || '';
      case 'IMAGE':
        return '[图片]';
      case 'FILE':
        return '[文件]';
      case 'AUDIO':
        return '[语音]';
      case 'VIDEO':
        return '[视频]';
      default:
        return '[消息]';
    }
  };

  // 清除当前会话
  const clearCurrentConversation = (): void => {
    currentConversation.value = null;
    currentMessages.value = [];
  };

  // 清除所有数据
  const clearAllData = (): void => {
    conversations.value = [];
    currentConversation.value = null;
    currentMessages.value = [];
    error.value = null;
  };

  return {
    // 状态
    conversations: readonly(conversations),
    currentConversation: readonly(currentConversation),
    currentMessages: readonly(currentMessages),
    isLoading: readonly(isLoading),
    error: readonly(error),
    
    // 计算属性
    totalUnreadCount,
    currentChatId,
    currentChatName,
    
    // 方法
    loadConversations,
    loadMessages,
    openConversation,
    sendMessage,
    openPrivateChat,
    getConversationDisplayName,
    getConversationAvatar,
    formatMessageTime,
    getLastMessageContent,
    clearCurrentConversation,
    clearAllData
  };
}