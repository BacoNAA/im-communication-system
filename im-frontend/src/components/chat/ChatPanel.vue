<template>
  <div class="chat-panel">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <h3 class="chat-title">{{ chatName }}</h3>
      <div class="chat-actions">
        <button class="action-btn">
          <i class="fas fa-search"></i>
        </button>
        <button class="action-btn">
          <i class="fas fa-ellipsis-v"></i>
        </button>
      </div>
    </div>
    
    <!-- 聊天消息列表 -->
    <div ref="messageContainerRef" class="message-container">
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <div>加载中...</div>
      </div>
      
      <div v-else-if="messages.length === 0" class="empty-container">
        <div class="empty-message">
          <i class="fas fa-comments"></i>
          <p>没有消息</p>
          <p class="empty-tip">开始发送消息吧</p>
        </div>
      </div>
      
      <template v-else>
        <chat-message
          v-for="msg in messages"
          :key="msg.id"
          :message="msg"
          :current-user-avatar="currentUserAvatar"
          :show-sender-name="!!isGroupChat"
          @retry="retryMessage"
        />
      </template>
    </div>
    
    <!-- 消息输入框 -->
    <message-input
      :conversation-id="conversationId"
      :disabled="inputDisabled"
      @send-message="handleSendMessage"
    />
  </div>
</template>
  
  <script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue';
import { messageApi, MessageType } from '@/api/message';
import ChatMessage from './ChatMessage.vue';
import MessageInput from './MessageInput.vue';
import { useAuth } from '@/composables/useAuth';

const props = defineProps<{
  conversationId: string;
  chatName?: string;
  isGroupChat?: boolean;
}>();

// 状态
const messages = ref<any[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const messageContainerRef = ref<HTMLElement | null>(null);
const inputDisabled = ref(false);

// 获取当前用户信息
const { currentUser } = useAuth();

// 计算属性
const chatName = computed(() => {
  return props.chatName || '聊天';
});

// 当前用户头像
const currentUserAvatar = computed(() => {
  // 尝试从多个可能的位置获取用户头像
  if (currentUser.value?.avatarUrl) {
    return currentUser.value.avatarUrl;
  }
  
  if (currentUser.value?.avatar) {
    return currentUser.value.avatar;
  }
  
  // 尝试从localStorage/sessionStorage获取
  try {
    // 从localStorage获取
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr);
      if (userInfo?.avatar) return userInfo.avatar;
      if (userInfo?.avatarUrl) return userInfo.avatarUrl;
    }
    
    // 从sessionStorage获取
    const sessionUserInfoStr = sessionStorage.getItem('userInfo');
    if (sessionUserInfoStr) {
      const userInfo = JSON.parse(sessionUserInfoStr);
      if (userInfo?.avatar) return userInfo.avatar;
      if (userInfo?.avatarUrl) return userInfo.avatarUrl;
    }
    
    // 从current_user获取
    const currentUserStr = localStorage.getItem('current_user') || sessionStorage.getItem('current_user');
    if (currentUserStr) {
      const userData = JSON.parse(currentUserStr);
      if (userData?.avatar) return userData.avatar;
      if (userData?.avatarUrl) return userData.avatarUrl;
    }
  } catch (e) {
    console.error('获取用户头像失败:', e);
  }
  
  // 如果都没有，返回默认头像
  return '/favicon.ico';
});

// 方法
const loadMessages = async () => {
  if (!props.conversationId) return;
  
  try {
    loading.value = true;
    error.value = null;
    
    console.log('正在加载会话消息，conversationId:', props.conversationId);
    
    // 调用API获取消息
    const response = await messageApi.getMessages(Number(props.conversationId));
    
    console.log('消息API响应:', JSON.stringify(response));
    
    if (response.success && response.data) {
      console.log('获取到消息数据:', JSON.stringify(response.data));
      
      if (response.data.content && response.data.content.length > 0) {
        // 检查第一条消息的内容
        const firstMsg = response.data.content[0];
        if (firstMsg) {
          console.log('第一条消息详情:', JSON.stringify(firstMsg));
          console.log('消息内容类型:', typeof firstMsg.content);
          console.log('消息内容:', firstMsg.content);
          
          // 检查消息结构
          console.log('消息对象结构:', Object.keys(firstMsg));
          // 检查是否有message字段
          if (firstMsg.message) {
            console.log('消息有message字段:', Object.keys(firstMsg.message));
            console.log('message.content:', firstMsg.message.content);
          }
        }
      }
      
      // 转换消息格式并按时间排序
      messages.value = response.data.content
        .map(msg => {
          console.log('处理消息原始数据:', JSON.stringify(msg));
          
          // 正确获取消息内容 - 检查是否有message字段
          let messageContent = '';
          if (msg.message && msg.message.content !== undefined) {
            // 如果消息在message字段中
            messageContent = msg.message.content;
            console.log('从message字段获取内容:', messageContent);
          } else if (msg.content !== undefined) {
            // 直接从content字段获取
            messageContent = msg.content;
            console.log('从content字段获取内容:', messageContent);
          }
          
          // 检查消息内容
          if (!messageContent && messageContent !== '') {
            console.warn('消息内容为null或undefined:', msg.id);
          }
          
          // 获取发送者ID
          const senderId = msg.message?.senderId || msg.senderId;
          
          // 使用API拦截器中添加的isSentByCurrentUser字段
          const isSelf = msg.isSentByCurrentUser === true;
          
          // 确保所有必要字段都存在
          const processedMsg = {
            id: msg.id || (msg.message ? msg.message.id : null),
            content: messageContent || '',
            type: (msg.messageType || msg.type || (msg.message ? msg.message.messageType : null))?.toUpperCase() || 'TEXT',
            senderId: senderId,
            senderName: msg.senderName || msg.senderNickname || (msg.message ? msg.message.senderNickname : null) || '用户',
            senderAvatar: msg.senderAvatar || (msg.message ? msg.message.senderAvatar : null) || '',
            createdAt: msg.createdAt || (msg.message ? msg.message.createdAt : null) || new Date().toISOString(),
            status: msg.status || (msg.message ? msg.message.status : null) || 'SENT',
            isSelf: msg.isSentByCurrentUser === true
          };
          
          console.log('处理后的消息:', JSON.stringify(processedMsg));
          console.log('消息是否由当前用户发送:', processedMsg.isSelf);
          
          return processedMsg;
        })
        .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
      
      console.log('处理后的消息列表:', messages.value);
      console.log('消息列表中的isSelf值:', messages.value.map(m => m.isSelf));
    } else {
      console.error('加载消息失败:', response.message);
      error.value = response.message || '加载消息失败';
    }
  } catch (err: any) {
    console.error('加载消息失败:', err);
    error.value = err.message || '加载消息失败';
  } finally {
    loading.value = false;
    // 加载完成后滚动到底部
    scrollToBottom();
  }
};

const handleSendMessage = async (data: { conversationId: number | string, content: string, messageType: string }) => {
  if (!props.conversationId || !data.content.trim()) return;
  
  try {
    inputDisabled.value = true;
    console.log('准备发送消息:', data);
    console.log('消息内容类型:', typeof data.content);
    console.log('消息内容长度:', data.content.length);
    
    // 获取当前用户头像
    const userAvatar = currentUserAvatar.value;
    console.log('用户头像URL:', userAvatar);
    
    // 创建临时消息对象
    const tempId = `temp-${Date.now()}`;
    const tempMessage = {
      id: tempId,
      content: data.content,
      type: data.messageType?.toUpperCase() || 'TEXT',
      senderId: currentUser.value?.id,
      senderName: currentUser.value?.nickname || currentUser.value?.name || '我',
      senderAvatar: userAvatar,
      createdAt: new Date().toISOString(),
      status: 'SENDING',
      isSelf: true
    };
    
    console.log('创建临时消息:', JSON.stringify(tempMessage));
    console.log('临时消息isSelf属性:', tempMessage.isSelf);
    console.log('临时消息头像URL:', tempMessage.senderAvatar);
    
    // 添加到消息列表
    messages.value.push(tempMessage);
    
    // 滚动到底部
    scrollToBottom();
    
    // 调用API发送消息
    console.log('调用API发送消息，参数:', {
      conversationId: Number(props.conversationId),
      messageType: data.messageType,
      content: data.content
    });
    
    const response = await messageApi.sendMessage({
      conversationId: Number(props.conversationId),
      messageType: data.messageType as MessageType || MessageType.TEXT,
      content: data.content
    });
    
    console.log('发送消息API响应:', JSON.stringify(response));
    
    if (response.success && response.data) {
      console.log('消息发送成功，返回数据:', JSON.stringify(response.data));
      console.log('返回的消息内容:', response.data.content);
      console.log('返回的消息内容类型:', typeof response.data.content);
      
      // 检查消息结构
      let messageContent = '';
      if (response.data.message && response.data.message.content !== undefined) {
        messageContent = response.data.message.content;
        console.log('从message字段获取内容:', messageContent);
      } else if (response.data.content !== undefined) {
        messageContent = response.data.content;
        console.log('从content字段获取内容:', messageContent);
      }
      
      // 更新临时消息
      const index = messages.value.findIndex(m => m.id === tempId);
      if (index !== -1) {
        // 确保内容字段存在
        const updatedContent = messageContent || messages.value[index].content;
        
        messages.value[index] = {
          ...messages.value[index],
          id: response.data.id,
          content: updatedContent,
          status: 'SENT',
          createdAt: response.data.createdAt || messages.value[index].createdAt
        };
        
        console.log('临时消息已更新:', JSON.stringify(messages.value[index]));
      }
    } else {
      console.error('消息发送失败:', response.message);
      
      // 标记为发送失败
      const index = messages.value.findIndex(m => m.id === tempId);
      if (index !== -1) {
        messages.value[index] = {
          ...messages.value[index],
          status: 'FAILED'
        };
        
        console.log('临时消息已标记为失败:', messages.value[index]);
      }
      
      throw new Error(response.message || '发送消息失败');
    }
  } catch (err: any) {
    console.error('发送消息失败:', err);
    error.value = err.message || '发送消息失败';
  } finally {
    inputDisabled.value = false;
  }
};

const retryMessage = async (messageId: string | number) => {
  const message = messages.value.find(m => m.id === messageId);
  if (!message) return;
  
  try {
    // 更新状态为重新发送中
    const index = messages.value.findIndex(m => m.id === messageId);
    if (index !== -1) {
      messages.value[index] = {
        ...messages.value[index],
        status: 'SENDING'
      };
    }
    
    // 重新发送消息
    const response = await messageApi.sendMessage({
      conversationId: Number(props.conversationId),
      messageType: message.type as MessageType || MessageType.TEXT,
      content: message.content
    });
    
    if (response.success && response.data) {
      // 检查消息结构
      let messageContent = '';
      if (response.data.message && response.data.message.content !== undefined) {
        messageContent = response.data.message.content;
        console.log('从message字段获取内容:', messageContent);
      } else if (response.data.content !== undefined) {
        messageContent = response.data.content;
        console.log('从content字段获取内容:', messageContent);
      }
      
      // 更新消息
      if (index !== -1) {
        messages.value[index] = {
          ...messages.value[index],
          id: response.data.id,
          content: messageContent || messages.value[index].content,
          status: 'SENT',
          createdAt: response.data.createdAt || messages.value[index].createdAt
        };
      }
    } else {
      // 保持失败状态
      if (index !== -1) {
        messages.value[index] = {
          ...messages.value[index],
          status: 'FAILED'
        };
      }
      
      throw new Error(response.message || '重新发送消息失败');
    }
  } catch (err: any) {
    console.error('重新发送消息失败:', err);
    error.value = err.message || '重新发送消息失败';
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainerRef.value) {
      messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight;
    }
  });
};

// 生命周期钩子
onMounted(() => {
  if (props.conversationId) {
    loadMessages();
  }
});

// 监听会话ID变化
watch(() => props.conversationId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    messages.value = [];
    loadMessages();
  }
});
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #ffffff;
  border-left: 1px solid #e0e0e0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #ffffff;
}

.chat-title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.action-btn:hover {
  background-color: #f0f0f0;
  color: #1890ff;
}

.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #f9f9f9;
}

.loading-container,
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 8px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-message {
  text-align: center;
}

.empty-message i {
  font-size: 48px;
  color: #d9d9d9;
  margin-bottom: 16px;
}

.empty-tip {
  font-size: 12px;
  color: #bbb;
  margin-top: 4px;
}

/* 调试面板样式 */
.debug-panel {
  background-color: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
  margin-bottom: 15px;
  max-height: 300px;
  overflow-y: auto;
}

.debug-message {
  margin-bottom: 8px;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.debug-message pre {
  background-color: #f1f1f1;
  padding: 8px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 12px;
}

.debug-message summary {
  cursor: pointer;
  font-weight: bold;
  margin-bottom: 5px;
}

.close-debug {
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 5px 10px;
  cursor: pointer;
  margin-top: 10px;
}

.debug-button {
  position: absolute;
  right: 10px;
  bottom: 70px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 5px 10px;
  font-size: 12px;
  opacity: 0.7;
  cursor: pointer;
}

.debug-button:hover {
  opacity: 1;
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
}

.loading-spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 错误状态 */
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
}

.error-message {
  color: #dc3545;
  margin-bottom: 10px;
  text-align: center;
}

.retry-button {
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 15px;
  cursor: pointer;
}

/* 无消息状态 */
.no-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6c757d;
  font-style: italic;
}
</style> 