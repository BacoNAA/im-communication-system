import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { messageApi, type Message, type MessageType } from '@/api/message';
import { useMessages } from '@/composables/useMessages';

export const useMessagesStore = defineStore('messages', () => {
  // 状态
  const messages = ref<Message[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // 获取useMessages组合式函数
  const { 
    currentMessages, 
    currentConversation, 
    loadMessages, 
    openConversation 
  } = useMessages();
  
  // 计算属性
  const currentConversationId = computed(() => currentConversation.value?.id);
  
  /**
   * 发送消息
   * @param content 消息内容
   * @param messageType 消息类型
   * @param receiverId 接收者ID（可选）
   * @param mediaFileId 媒体文件ID（可选）
   */
  const sendMessage = async (
    content: string, 
    messageType: MessageType = 'TEXT' as MessageType, 
    receiverId?: number,
    mediaFileId?: number
  ): Promise<void> => {
    if (!content.trim() && messageType === 'TEXT') {
      error.value = '消息内容不能为空';
      return;
    }

    try {
      loading.value = true;
      error.value = null;
      
      const messageData: any = {
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
      
      // 如果有媒体文件ID，添加到请求
      if (mediaFileId) {
        messageData.mediaFileId = mediaFileId;
      }

      const response = await messageApi.sendMessage(messageData);
      
      if (response.success && response.data) {
        console.log('消息发送成功:', response.data);
        
        // 刷新当前会话的消息列表
        if (currentConversationId.value) {
          await getMessages(currentConversationId.value);
        }
      } else {
        throw new Error(response.message || '发送消息失败');
      }
    } catch (err: any) {
      error.value = err.message || '发送消息失败';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 加载指定会话的消息
   * @param conversationId 会话ID
   */
  const getMessages = async (conversationId: number): Promise<void> => {
    try {
      loading.value = true;
      error.value = null;
      
      await loadMessages(conversationId);
      
      // 将当前消息同步到本地状态 - 使用类型断言解决只读数组问题
      messages.value = [...currentMessages.value] as Message[];
    } catch (err: any) {
      error.value = err.message || '获取消息失败';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 打开会话
   * @param conversationId 会话ID
   */
  const openChat = async (conversationId: number): Promise<void> => {
    try {
      loading.value = true;
      error.value = null;
      
      await openConversation(conversationId);
      
      // 将当前消息同步到本地状态 - 使用类型断言解决只读数组问题
      messages.value = [...currentMessages.value] as Message[];
    } catch (err: any) {
      error.value = err.message || '打开会话失败';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  return {
    messages,
    loading,
    error,
    currentConversationId,
    sendMessage,
    getMessages,
    openChat
  };
}); 