<template>
  <div class="chat-panel">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <h3 class="chat-title">{{ chatName }}</h3>
      <div class="chat-actions">
        <!-- WebSocket连接状态指示器 -->
        <div class="ws-status-indicator" :class="{ 'connected': isConnected }">
          <span class="status-dot"></span>
          <span class="status-text">{{ isConnected ? '已连接' : '未连接' }}</span>
          <button v-if="!isConnected" class="reconnect-btn" @click="reconnectWebSocket" title="重新连接">
            <i class="fas fa-redo-alt"></i>
          </button>
        </div>
        <button class="action-btn" @click="refreshMessages" title="刷新消息">
          <i class="fas fa-sync"></i>
        </button>
        <button class="action-btn search-action-btn" @click="toggleSearchPanel" title="搜索消息">
          <i class="fas fa-search"></i>
          <span class="search-text">搜索</span>
        </button>
        <!-- 媒体库按钮 -->
        <MediaLibraryButton :conversation-id="Number(conversationId)" />
        <button class="action-btn" @click="toggleSelectionMode" title="选择消息">
          <i class="fas fa-check-square"></i>
        </button>
        <button class="action-btn">
          <i class="fas fa-ellipsis-v"></i>
        </button>
      </div>
    </div>
    
    <!-- 在消息操作区域（message-container前面）添加明显的多选按钮 -->
    <div class="chat-controls">
      <div class="control-buttons">
        <button 
          class="control-btn selection-mode-btn" 
          @click="toggleSelectionMode" 
          :class="{ 'active': isSelectionMode }"
          title="多选模式"
        >
          <i class="fas fa-check-square"></i>
          <span>多选</span>
        </button>
        
        <!-- 其他控制按钮可以放在这里 -->
        <button v-if="messages.length > 0" class="control-btn" @click="openSearchPanel" title="搜索消息">
          <i class="fas fa-search"></i>
          <span>搜索</span>
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
        <!-- 选择模式提示 -->
        <div v-if="isSelectionMode && messages.length > 0" class="selection-mode-hint">
          <i class="fas fa-info-circle"></i>
          <span>点击消息前的方框选择要转发的消息</span>
        </div>
        
        <div v-if="isSelectionMode" class="selection-mode-banner">
          <div class="selection-info">已选择 {{ selectedMessages.length }} 条消息</div>
          <div class="selection-actions">
            <button class="selection-action-btn forward-btn" @click="forwardSelectedMessages" :disabled="selectedMessages.length === 0">
              <i class="fas fa-share"></i> 转发选中的消息
            </button>
            <button class="selection-action-btn cancel-btn" @click="cancelSelectionMode">
              <i class="fas fa-times"></i> 取消
            </button>
          </div>
        </div>
        
        <div 
          v-for="msg in messages"
          :key="msg.id"
          class="message-item-wrapper"
          :class="{ 'message-selected': isMessageSelected(msg.id) }"
        >
          <div 
            v-if="isSelectionMode" 
            class="message-checkbox"
            @click.stop="toggleMessageSelection(msg.id, msg)"
          >
            <div class="checkbox-indicator" :class="{ 'selected': isMessageSelected(msg.id) }">
              <i v-if="isMessageSelected(msg.id)" class="fas fa-check"></i>
            </div>
          </div>
          
          <chat-message
          :message="msg"
          :current-user-avatar="currentUserAvatar"
          :show-sender-name="!!isGroupChat"
            :is-highlighted="msg.id === highlightedMessageId"
          @retry="retryMessage"
            @recall="handleRecallMessage"
            @edit="handleEditMessage"
        />
        </div>
      </template>
    </div>
    
    <!-- 输入状态指示器 -->
    <div v-if="isTypingVisible" class="typing-indicator">
      <div class="typing-animation">
        <span></span>
        <span></span>
        <span></span>
      </div>
      <div class="typing-text">{{ typingMessage }}</div>
    </div>
    
    <!-- 消息搜索面板 -->
    <message-search
      :conversation-id="conversationId"
      :is-active="isSearchActive"
      @close="closeSearchPanel"
      @jump-to-message="scrollToMessage"
    />
    
    <!-- 消息输入框 -->
    <message-input
      :conversation-id="conversationId"
      :disabled="inputDisabled || isSelectionMode"
      @send-message="handleSendMessage"
    />
    
    <!-- 转发消息对话框 -->
    <forward-message-dialog 
      :is-visible="showForwardDialog" 
      :messages="selectedMessages" 
      @close="closeForwardDialog" 
      @forward-success="handleForwardSuccess"
    />
  </div>
</template>
  
  <script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue';
import { messageApi, MessageType } from '@/api/message';
import type { MarkAsReadRequest, UpdateReadCursorRequest } from '@/api/message'; // 使用type导入类型
import ChatMessage from './ChatMessage.vue';
import MessageInput from './MessageInput.vue';
import MessageSearch from './MessageSearch.vue';
import ForwardMessageDialog from './ForwardMessageDialog.vue';
import { useAuth } from '@/composables/useAuth';
import { useSharedWebSocket } from '@/composables/useWebSocket'; // 导入共享WebSocket
import { useRouter, useRoute } from 'vue-router'; // 导入路由
import MediaLibraryButton from './MediaLibraryButton.vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as groupApi from '@/api/group';

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
const isSearchActive = ref(false);
const highlightedMessageId = ref<number | null>(null);

// 消息选择模式
const isSelectionMode = ref(false);
const selectedMessages = ref<any[]>([]);
const showForwardDialog = ref(false);

// 获取当前用户信息
const { currentUser } = useAuth();
const router = useRouter(); // 获取路由

// 初始化WebSocket连接
const { 
  status: wsStatus, 
  connect: wsConnect, 
  disconnect: wsDisconnect, 
  isConnected: wsConnected,
  lastMessage: wsLastMessage,
  send: wsSend
} = useSharedWebSocket(handleWebSocketMessage); // 使用共享WebSocket

// 更新阅读光标
const updateReadCursor = async (conversationId: number, lastMessageId: number) => {
  if (!conversationId || !lastMessageId) return;
  
  try {
    console.log(`更新会话 ${conversationId} 的阅读光标为消息 ${lastMessageId}`);
    await messageApi.updateReadCursor(conversationId, lastMessageId);
    console.log('阅读光标更新成功');
  } catch (error) {
    console.error('更新阅读光标失败:', error);
  }
};

// 处理WebSocket消息
function handleWebSocketMessage(data: any) {
  console.log('ChatPanel收到WebSocket消息:', data);
  
  try {
    // 标准化消息类型（转换为大写）
    const messageType = data.type ? data.type.toUpperCase() : null;
    
    // 处理不同类型的消息
    switch (messageType) {
      case 'MESSAGE':
      // 新消息
      handleRealTimeMessage(data);
        break;
      case 'MESSAGE_CONFIRMATION':
      // 消息确认
      handleMessageConfirmation(data);
        break;
      case 'STATUS_UPDATE':
      // 状态更新
      handleStatusUpdate(data);
        break;
      case 'RECALL':
      // 消息撤回
      handleMessageRecall(data);
        break;
      case 'EDIT':
        // 消息编辑
        handleMessageEditNotification(data);
        break;
      case 'TYPING':
      // 输入状态
      handleTypingStatus(data);
        break;
      case 'PRESENCE':
      // 在线状态
      console.log('收到在线状态更新:', data);
        break;
      case 'PONG':
        // PONG响应，不需要特殊处理
        console.debug('收到PONG响应');
        break;
      case 'CONNECT_SUCCESS':
        // 连接成功确认
        console.log('WebSocket连接成功确认:', data);
        break;
      default:
      console.log('未处理的WebSocket消息类型:', data.type);
    }
  } catch (error) {
    console.error('处理WebSocket消息出错:', error);
  }
}

// 输入状态管理
const typingUsers = ref<Record<string, { name: string, timestamp: number }>>({});
const isTypingVisible = ref(false);
const typingMessage = ref('');
const typingTimer = ref<number | null>(null);

// 处理输入状态
function handleTypingStatus(data: any) {
  try {
    if (!data) return;
    
    const messageData = data.data || data;
    if (!messageData) return;
    
    const userId = messageData.userId || messageData.senderId;
    const userName = messageData.userName || messageData.senderName || '有人';
    const isTyping = messageData.isTyping;
    const conversationId = messageData.conversationId;
    
    // 检查必要的数据是否存在
    if (!userId || conversationId === undefined || isTyping === undefined) {
      console.warn('输入状态消息缺少必要字段:', messageData);
      return;
    }
    
    // 只处理当前会话的输入状态
    if (Number(conversationId) !== Number(props.conversationId)) {
      return;
    }
    
    console.log(`收到用户 ${userName} (${userId}) 的输入状态: ${isTyping ? '正在输入' : '停止输入'}`);
    
    // 如果是当前用户自己，不显示输入状态
    if (currentUser.value && userId === currentUser.value.id) {
      return;
    }
    
    // 更新输入状态
    if (isTyping) {
      // 添加或更新用户输入状态
      typingUsers.value[userId] = {
        name: userName,
        timestamp: Date.now()
      };
    } else {
      // 移除用户输入状态
      delete typingUsers.value[userId];
    }
    
    // 更新显示
    updateTypingDisplay();
    
    // 设置5秒后自动清除输入状态
    if (typingTimer.value) {
      clearTimeout(typingTimer.value);
    }
    
    typingTimer.value = window.setTimeout(() => {
      // 检查是否有超过5秒未更新的输入状态，移除它们
      const now = Date.now();
      let hasChanges = false;
      
      Object.entries(typingUsers.value).forEach(([id, info]) => {
        if (now - info.timestamp > 5000) {
          delete typingUsers.value[id];
          hasChanges = true;
        }
      });
      
      if (hasChanges) {
        updateTypingDisplay();
      }
    }, 5000);
  } catch (error) {
    console.error('处理输入状态出错:', error);
  }
}

// 更新输入状态显示
function updateTypingDisplay() {
  if (!typingUsers.value) {
    isTypingVisible.value = false;
    typingMessage.value = '';
    return;
  }
  
  const typingUserCount = Object.keys(typingUsers.value).length;
  
  if (typingUserCount === 0) {
    isTypingVisible.value = false;
    typingMessage.value = '';
  } else if (typingUserCount === 1) {
    const userName = Object.values(typingUsers.value)[0]?.name || '有人';
    isTypingVisible.value = true;
    typingMessage.value = `${userName} 正在输入...`;
  } else {
    isTypingVisible.value = true;
    typingMessage.value = `${typingUserCount}人正在输入...`;
  }
}

// 消息确认状态跟踪
const messageConfirmations = ref(new Set<string>());
const messageTimeouts = new Map<string, number>();

// 处理消息确认
const handleMessageConfirmation = (data: any) => {
  if (data && data.tempId) {
    console.log('收到消息确认:', data);
    
    // 将临时ID添加到确认集合
    messageConfirmations.value.add(data.tempId);
    
    // 清除相关的超时处理器
    if (messageTimeouts.has(data.tempId)) {
      clearTimeout(messageTimeouts.get(data.tempId));
      messageTimeouts.delete(data.tempId);
      }
  }
};

// 直接从存储中获取当前用户ID
const getCurrentUserIdFromStorage = (): number | null => {
  try {
    // 尝试从localStorage获取
    let userId = localStorage.getItem('userId');
    
    // 如果localStorage中没有，尝试从sessionStorage获取
    if (!userId) {
      userId = sessionStorage.getItem('userId');
    }
    
    // 如果还没有，尝试从userInfo中获取
    if (!userId) {
      const userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
      if (userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr);
          userId = userInfo.id;
        } catch (e) {
          console.error('解析userInfo失败:', e);
        }
      }
    }
    
    // 如果还没有，尝试从current_user中获取
    if (!userId) {
      const currentUserStr = localStorage.getItem('current_user') || sessionStorage.getItem('current_user');
      if (currentUserStr) {
        try {
          const userData = JSON.parse(currentUserStr);
          userId = userData.id;
        } catch (e) {
          console.error('解析current_user失败:', e);
        }
      }
    }
    
    console.log(`从存储中获取的用户ID: ${userId} (${typeof userId})`);
    
    // 转换为数字并返回
    if (userId) {
      const numericId = Number(userId);
      if (!isNaN(numericId)) {
        return numericId;
      }
    }
    
    return null;
  } catch (error) {
    console.error('获取用户ID时出错:', error);
    return null;
  }
};

// 确保消息有正确的isSelf属性
const ensureMessageHasCorrectIsSelf = (message: any) => {
  if (!message) return message;
  
  // 获取当前用户ID，确保是数字类型
  let currentUserId = currentUser.value?.id ? Number(currentUser.value.id) : null;
  
  // 如果从currentUser获取失败，尝试从存储中获取
  if (currentUserId === null) {
    currentUserId = getCurrentUserIdFromStorage();
    console.log(`[ensureMessageHasCorrectIsSelf] 从存储中获取的用户ID: ${currentUserId}`);
  }
  
  // 获取消息发送者ID，确保是数字类型
  const senderId = message.senderId ? Number(message.senderId) : null;
  
  console.log(`[ensureMessageHasCorrectIsSelf] 消息ID: ${message.id}, 发送者ID: ${senderId} (${typeof senderId}), 当前用户ID: ${currentUserId} (${typeof currentUserId})`);
  console.log(`[ensureMessageHasCorrectIsSelf] 原始值 - 发送者ID: ${message.senderId} (${typeof message.senderId}), 当前用户ID: ${currentUser.value?.id} (${typeof currentUser.value?.id})`);
  
  // 检查两个ID是否都有效
  if (currentUserId !== null && senderId !== null) {
    // 判断消息是否由当前用户发送
    const shouldBeSelf = senderId === currentUserId;
    
    console.log(`[ensureMessageHasCorrectIsSelf] 判断结果: ${senderId} === ${currentUserId} = ${shouldBeSelf}`);
    
    // 如果isSelf已经被设置，检查是否正确
    if (message.isSelf !== undefined) {
      // 如果isSelf设置不正确，修正它
      if (message.isSelf !== shouldBeSelf) {
        console.log(`[ensureMessageHasCorrectIsSelf] 修正消息 ${message.id} 的isSelf属性，从 ${message.isSelf} 改为 ${shouldBeSelf}`);
        message.isSelf = shouldBeSelf;
      }
    } else {
      // 如果isSelf未设置，设置它
      message.isSelf = shouldBeSelf;
      console.log(`[ensureMessageHasCorrectIsSelf] 为消息 ${message.id} 设置isSelf属性为 ${message.isSelf}`);
    }
  } else {
    console.warn(`[ensureMessageHasCorrectIsSelf] 无法确定isSelf，因为currentUserId(${currentUserId})或senderId(${senderId})无效`);
    
    // 如果无法确定，默认设置为false
    if (message.isSelf === undefined) {
      message.isSelf = false;
    }
  }
  
  // 强制设置isSentByCurrentUser与isSelf保持一致
  message.isSentByCurrentUser = message.isSelf;
  
  return message;
};

// 处理实时消息
function handleRealTimeMessage(messageData: any) {
  console.log('处理实时消息，原始数据:', JSON.stringify(messageData));
  
  try {
    // 格式化消息对象
    const formattedMessage = formatWebSocketMessage(messageData);
    
    // 检查基本有效性
    if (!formattedMessage || !formattedMessage.id) {
      console.warn('收到的消息无效，缺少ID');
      return;
    }
    
    // 确保消息有正确的isSelf属性
    ensureMessageHasCorrectIsSelf(formattedMessage);
    
    // 检查消息是否属于当前会话
    const currentConversationId = Number(props.conversationId);
    const messageConversationId = Number(formattedMessage.conversationId);
    
    console.log(`检查消息会话匹配: 消息会话=${messageConversationId}, 当前会话=${currentConversationId}`);
    console.log(`消息isSelf状态: ${formattedMessage.isSelf}, 发送者ID: ${formattedMessage.senderId}, 当前用户ID: ${currentUser.value?.id}`);
    
    if (messageConversationId !== currentConversationId) {
      console.log(`消息会话ID不匹配，忽略消息: 消息会话=${messageConversationId}, 当前会话=${currentConversationId}`);
      return;
    }
    
    console.log('收到当前会话的新消息:', formattedMessage);
      
    // 检查消息是否已存在（避免重复）
    const isDuplicate = messages.value.some(msg => {
      // 主要检查方式：ID相同
      if (msg.id === formattedMessage.id) {
        console.log(`消息ID ${formattedMessage.id} 已存在，跳过`);
        return true;
      }
      
      // 次要检查方式：内容、发送者和时间接近（5秒内）
      if (msg.content === formattedMessage.content && 
          msg.senderId === formattedMessage.senderId) {
        
        const msgTime = new Date(msg.createdAt).getTime();
        const newMsgTime = new Date(formattedMessage.createdAt).getTime();
        const timeDiff = Math.abs(msgTime - newMsgTime);
        
        if (timeDiff < 5000) {
          console.log(`消息内容和发送者相同，时间差 ${timeDiff}ms < 5000ms，判定为重复消息`);
          return true;
        }
      }
      
      return false;
    });
      
    if (!isDuplicate) {
      console.log('消息不是重复的，添加到当前会话');
      console.log('添加消息的isSelf状态:', formattedMessage.isSelf);
      
      // 将消息添加到当前会话的消息列表中
      messages.value = [...messages.value, formattedMessage];
      console.log('消息已添加到当前会话，当前消息列表长度:', messages.value.length);
      
      // 滚动到底部
      nextTick(() => {
        scrollToBottom();
      });
      
      // 如果是当前会话，自动标记为已读并更新阅读光标
      if (formattedMessage.id) {
        try {
          // 标记消息已读
          messageApi.markMessageAsRead(formattedMessage.id)
            .then(() => console.log('消息已标记为已读:', formattedMessage.id))
            .catch(err => console.error('标记消息已读失败:', err));
            
          // 更新阅读光标
          updateReadCursor(messageConversationId, formattedMessage.id);
        } catch (err) {
          console.error('标记消息已读过程中出错:', err);
        }
      }
    } else {
      console.log('跳过重复消息:', formattedMessage.id);
    }
  } catch (error) {
    console.error('处理实时消息失败:', error);
  }
}

// 格式化WebSocket消息
function formatWebSocketMessage(messageData: any): any {
  console.log('格式化WebSocket消息，原始数据:', JSON.stringify(messageData));
  
  // 创建输出消息对象
  const outputMessage: any = {};
  
  try {
  // 尝试从不同位置获取消息数据
    let sourceData = messageData;
    
    // 如果消息数据在data字段中，则使用data字段
    if (messageData.data && typeof messageData.data === 'object') {
      sourceData = messageData.data;
      console.log('使用data字段中的数据');
    }
    
    // 如果消息数据在message字段中，则使用message字段
    if (sourceData.message && typeof sourceData.message === 'object') {
      sourceData = sourceData.message;
      console.log('使用message字段中的数据');
    }
    
    // 记录提取的数据源
    console.log('提取的数据源:', JSON.stringify(sourceData));
    
    // 设置基本字段
    outputMessage.id = sourceData.id || Date.now();
    outputMessage.conversationId = getNumericValue(sourceData.conversationId);
    outputMessage.senderId = sourceData.senderId;
    outputMessage.content = sourceData.content || '';
    
    // 处理消息类型 - 支持多种命名方式
    outputMessage.type = (sourceData.messageType || sourceData.type || 'TEXT').toUpperCase();
    outputMessage.messageType = outputMessage.type;
    
    // 处理日期字段
    outputMessage.createdAt = sourceData.createdAt || new Date().toISOString();
    outputMessage.updatedAt = sourceData.updatedAt || outputMessage.createdAt;
    
    // 处理发送者信息
    outputMessage.senderName = sourceData.senderName || sourceData.senderNickname || '未知用户';
    outputMessage.senderAvatar = sourceData.senderAvatar;
    outputMessage.mediaFileId = sourceData.mediaFileId;
    
    // 处理状态和已读信息
    outputMessage.status = sourceData.status || 'SENT';
    outputMessage.isRead = sourceData.isRead || false;
    
    // 判断消息是否由当前用户发送
    let currentUserId = currentUser.value?.id ? Number(currentUser.value.id) : null;
    
    // 如果从currentUser获取失败，尝试从存储中获取
    if (currentUserId === null) {
      currentUserId = getCurrentUserIdFromStorage();
      console.log(`[formatWebSocketMessage] 从存储中获取的用户ID: ${currentUserId}`);
    }
    
    const senderId = sourceData.senderId ? Number(sourceData.senderId) : null;
    
    console.log(`[formatWebSocketMessage] 检查消息是否由当前用户发送:`);
    console.log(`[formatWebSocketMessage] 消息发送者ID=${senderId} (${typeof senderId}), 当前用户ID=${currentUserId} (${typeof currentUserId})`);
    console.log(`[formatWebSocketMessage] 原始值 - 发送者ID: ${sourceData.senderId} (${typeof sourceData.senderId}), 当前用户ID: ${currentUser.value?.id} (${typeof currentUser.value?.id})`);
    
    const isSentByCurrentUser = currentUserId !== null && senderId !== null && senderId === currentUserId;
    outputMessage.isSentByCurrentUser = isSentByCurrentUser;
    outputMessage.isSelf = isSentByCurrentUser; // 添加isSelf字段以兼容ChatMessage组件
    
    console.log(`[formatWebSocketMessage] 消息ID ${outputMessage.id} 的isSelf设置为: ${outputMessage.isSelf}, 判断结果: ${senderId} === ${currentUserId} = ${isSentByCurrentUser}`);
    
    // 处理元数据
    if (sourceData.metadata) {
      try {
        if (typeof sourceData.metadata === 'string') {
          outputMessage.metadata = JSON.parse(sourceData.metadata);
        } else {
          outputMessage.metadata = sourceData.metadata;
        }
      } catch (e) {
        console.warn('解析消息元数据失败:', e);
        outputMessage.metadata = sourceData.metadata;
      }
    }
    
    console.log('[formatWebSocketMessage] 格式化后的消息:', outputMessage);
    return outputMessage;
  } catch (error) {
    console.error('格式化WebSocket消息失败:', error);
    
    // 返回基本消息，确保至少有基本字段
    return {
      id: Date.now(),
      conversationId: getNumericValue(messageData.conversationId) || Number(props.conversationId),
      senderId: messageData.senderId || 0,
      content: messageData.content || '消息解析错误',
      type: 'TEXT',
      messageType: 'TEXT',
      status: 'SENT',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      isSentByCurrentUser: false,
      isSelf: false
    };
  }
}

// 辅助函数：将任何值转换为数字
function getNumericValue(value: any): number {
  if (value === undefined || value === null) {
    return 0;
  }
  
  if (typeof value === 'number') {
    return value;
  }
  
  if (typeof value === 'string') {
    const parsed = parseInt(value, 10);
    return isNaN(parsed) ? 0 : parsed;
  }
  
  return 0;
}

// 处理状态更新
function handleStatusUpdate(data: any) {
  if (!data || !data.messageId) {
    console.warn('无效的状态更新数据');
    return;
  }
  
  const { messageId, status } = data;
  
  console.log(`收到消息 ${messageId} 状态更新: ${status}`);
  
  // 更新消息状态
  const messageToUpdate = messages.value.find(m => m.id === messageId);
  if (messageToUpdate) {
    console.log(`更新消息 ${messageId} 状态从 ${messageToUpdate.status} 到 ${status}`);
    messageToUpdate.status = status;
  }
}

// 处理消息撤回
function handleMessageRecall(data: any) {
  console.log('收到消息撤回WebSocket通知:', data);
  
  try {
    // 标准化数据结构
    const recallData = data.data || data;
    
    // 提取消息ID
    const messageId = recallData.messageId;
    if (!messageId) {
      console.warn('收到的消息撤回通知缺少messageId字段:', data);
    return;
  }
  
    // 提取会话ID
    const conversationId = recallData.conversationId;
    
    // 验证会话ID是否匹配当前会话
    if (conversationId && Number(conversationId) !== Number(props.conversationId)) {
      console.log(`消息撤回通知会话ID ${conversationId} 与当前会话 ${props.conversationId} 不匹配，忽略`);
      return;
    }
    
    console.log(`准备处理消息ID ${messageId} 的撤回操作`);
    
    // 查找消息
    const messageToUpdate = messages.value.find(msg => {
      // 支持不同类型ID的比较
      if (typeof msg.id === typeof messageId) {
        return msg.id === messageId;
      } else if (typeof msg.id === 'string' && typeof messageId === 'number') {
        return Number(msg.id) === messageId;
      } else if (typeof msg.id === 'number' && typeof messageId === 'string') {
        return msg.id === Number(messageId);
      }
      return false;
    });
    
  if (messageToUpdate) {
      console.log(`找到要撤回的消息: ID=${messageId}, 原状态=${messageToUpdate.status}, 内容="${messageToUpdate.content}"`);
      
      // 更新消息状态为已撤回
    messageToUpdate.status = 'RECALLED';
      
      // 记录撤回信息
      const recalledBy = recallData.recalledBy;
      const recalledAt = recallData.recalledAt;
      const reason = recallData.reason;
      
      if (recalledBy) {
        console.log(`消息被用户 ${recalledBy} 撤回`);
        messageToUpdate.recalledBy = recalledBy;
      }
      
      if (recalledAt) {
        console.log(`撤回时间: ${recalledAt}`);
        messageToUpdate.recalledAt = recalledAt;
      }
      
      if (reason) {
        console.log(`撤回原因: ${reason}`);
        messageToUpdate.recallReason = reason;
      }
      
      console.log(`消息 ${messageId} 已标记为已撤回`);
      
      // 更新消息数组，触发视图更新
      messages.value = [...messages.value];
      
      // 如果是最后一条消息，还需要更新会话列表的最后一条消息状态
      const lastMessageIndex = messages.value.length - 1;
      if (lastMessageIndex >= 0 && messages.value[lastMessageIndex].id === messageId) {
        console.log('已撤回的消息是会话的最后一条消息，需要更新会话列表');
        // 这里可以发出事件通知父组件更新会话列表
        // 或者直接调用refreshMessages重新加载消息
        setTimeout(() => {
          refreshMessages().catch((err: Error) => {
            console.warn('自动刷新消息列表失败:', err);
          });
        }, 500); // 延迟500ms执行，避免过于频繁的API调用
      }
    } else {
      console.warn(`在当前消息列表中找不到ID为 ${messageId} 的消息，无法标记为已撤回`);
      console.debug('当前消息列表中的消息ID:', messages.value.map(m => ({ id: m.id, type: typeof m.id })));
      
      // 尝试检查是否存在临时ID或数字转换的问题
      const alternativeIds = [];
      
      // 尝试字符串转数字
      if (typeof messageId === 'string') {
        const numericId = Number(messageId);
        if (!isNaN(numericId)) {
          alternativeIds.push(numericId);
        }
      } 
      // 尝试数字转字符串
      else if (typeof messageId === 'number') {
        alternativeIds.push(String(messageId));
      }
      
      // 尝试使用替代ID查找
      for (const altId of alternativeIds) {
        const msgWithAltId = messages.value.find(m => m.id === altId);
        if (msgWithAltId) {
          console.log(`使用替代ID ${altId} 找到了消息，标记为已撤回`);
          msgWithAltId.status = 'RECALLED';
          messages.value = [...messages.value]; // 触发视图更新
          break;
        }
      }
    }
  } catch (error) {
    console.error('处理消息撤回通知时出错:', error);
  }
}

// 处理消息编辑通知
function handleMessageEditNotification(data: any) {
  console.log('收到消息编辑WebSocket通知:', data);
  
  try {
    // 标准化数据结构
    const editData = data.data || data;
    
    // 提取消息ID
    const messageId = editData.messageId;
    if (!messageId) {
      console.warn('收到的消息编辑通知缺少messageId字段:', data);
      return;
    }
    
    // 提取会话ID
    const conversationId = editData.conversationId;
    
    // 验证会话ID是否匹配当前会话
    if (conversationId && Number(conversationId) !== Number(props.conversationId)) {
      console.log(`消息编辑通知会话ID ${conversationId} 与当前会话 ${props.conversationId} 不匹配，忽略`);
      return;
    }
    
    // 提取新内容
    const newContent = editData.content;
    if (newContent === undefined) {
      console.warn('消息编辑通知缺少content字段:', data);
      return;
    }
    
    console.log(`准备处理消息ID ${messageId} 的编辑操作，新内容: ${newContent}`);
    
    // 查找消息
    const messageToUpdate = messages.value.find(msg => {
      // 支持不同类型ID的比较
      if (typeof msg.id === typeof messageId) {
        return msg.id === messageId;
      } else if (typeof msg.id === 'string' && typeof messageId === 'number') {
        return Number(msg.id) === messageId;
      } else if (typeof msg.id === 'number' && typeof messageId === 'string') {
        return msg.id === Number(messageId);
      }
      return false;
    });
    
    if (messageToUpdate) {
      console.log(`找到要编辑的消息: ID=${messageId}, 原内容="${messageToUpdate.content}"`);
      
      // 更新消息内容
      messageToUpdate.content = newContent;
      
      // 标记消息为已编辑
      messageToUpdate.edited = true;
      
      // 记录编辑信息
      const editedBy = editData.editedBy;
      const editedAt = editData.editedAt || new Date().toISOString();
      const reason = editData.editReason;
      
      if (editedBy) {
        console.log(`消息被用户 ${editedBy} 编辑`);
        messageToUpdate.editedBy = editedBy;
      }
      
      if (editedAt) {
        console.log(`编辑时间: ${editedAt}`);
        messageToUpdate.editedAt = editedAt;
      }
      
      if (reason) {
        console.log(`编辑原因: ${reason}`);
        messageToUpdate.editReason = reason;
      }
      
      console.log(`消息 ${messageId} 已更新为新内容: "${newContent}"`);
      
      // 更新消息数组，触发视图更新
      messages.value = [...messages.value];
    } else {
      console.warn(`在当前消息列表中找不到ID为 ${messageId} 的消息，无法更新内容`);
      console.debug('当前消息列表中的消息ID:', messages.value.map(m => ({ id: m.id, type: typeof m.id })));
      
      // 尝试检查是否存在临时ID或数字转换的问题
      const alternativeIds = [];
      
      // 尝试字符串转数字
      if (typeof messageId === 'string') {
        const numericId = Number(messageId);
        if (!isNaN(numericId)) {
          alternativeIds.push(numericId);
        }
      } 
      // 尝试数字转字符串
      else if (typeof messageId === 'number') {
        alternativeIds.push(String(messageId));
      }
      
      // 尝试使用替代ID查找
      for (const altId of alternativeIds) {
        const msgWithAltId = messages.value.find(m => m.id === altId);
        if (msgWithAltId) {
          console.log(`使用替代ID ${altId} 找到了消息，更新内容`);
          msgWithAltId.content = newContent;
          msgWithAltId.edited = true;
          messages.value = [...messages.value]; // 触发视图更新
          break;
        }
      }
    }
  } catch (error) {
    console.error('处理消息编辑通知时出错:', error);
  }
}

// 在用户登录后自动连接WebSocket
watch(() => currentUser.value?.id, (newVal) => {
  if (newVal) {
    console.log('用户已登录，连接WebSocket');
    wsConnect();
  } else {
    console.log('用户已登出，断开WebSocket');
    wsDisconnect();
  }
}, { immediate: true });

// 监听WebSocket连接状态
watch(wsConnected, (isConnected) => {
  console.log('WebSocket连接状态变化:', isConnected);
  // 如果连接断开，尝试重新连接
  if (!isConnected) {
    setTimeout(() => {
      wsConnect();
    }, 3000);
  }
});

// 组件卸载时断开WebSocket连接
onUnmounted(() => {
  console.log('组件卸载，断开WebSocket');
  wsDisconnect();
});

// 计算属性
const chatName = computed(() => {
  return props.chatName || '聊天';
});

// WebSocket连接状态
const isConnected = computed(() => {
  return wsStatus.value === 'connected';
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

// 滚动到底部
const scrollToBottom = () => {
  if (messageContainerRef.value) {
    messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight;
  }
};

// 标记会话为已读
const markConversationAsRead = async () => {
  if (!props.conversationId || !currentUser.value?.id) {
    console.warn('无法标记会话为已读：会话ID或用户ID不存在', {
      conversationId: props.conversationId,
      userId: currentUser.value?.id
    });
    return;
  }

      try {
      console.log('开始标记会话为已读，会话ID:', props.conversationId);
      const response = await messageApi.markConversationAsRead(Number(props.conversationId));
      console.log('标记会话已读API响应:', JSON.stringify(response));
      if (response.success) {
        console.log('会话已标记为已读');
        // 在消息列表中找到当前会话的最后一条消息，并标记为已读
        const lastMessage = messages.value.find(msg => msg.conversationId === Number(props.conversationId));
        if (lastMessage) {
          lastMessage.isRead = true;
          console.log(`已将最后一条消息 ${lastMessage.id} 标记为已读`);
          
          // 更新阅读光标
          updateReadCursor(Number(props.conversationId), lastMessage.id);
        }
      } else {
        console.warn('标记会话已读失败:', response.message);
      }
    } catch (err) {
      console.error('标记会话已读出错:', err);
    }
};

// 加载消息
const loadMessages = async (conversationId: string) => {
  if (!conversationId) {
    console.error('无效的会话ID');
    return;
  }
  
  try {
    loading.value = true;
    error.value = null;
    
    console.log('正在加载会话消息，conversationId:', conversationId);
    
    // 获取当前用户ID
    let currentUserId = currentUser.value?.id ? Number(currentUser.value.id) : null;
    
    // 如果从currentUser获取失败，尝试从存储中获取
    if (currentUserId === null) {
      currentUserId = getCurrentUserIdFromStorage();
    }
    
    console.log('当前用户ID:', currentUserId, '类型:', typeof currentUserId);
    
    // 清空当前消息，避免显示之前会话的消息
    messages.value = [];
    
    // 直接调用API获取消息
    const response = await messageApi.getMessages(Number(conversationId));
    
    if (response.success && response.data) {
      console.log(`获取到${response.data.content?.length || 0}条消息`);
      
      // 转换消息格式并按时间排序
      const validMessages = response.data.content
        .filter(msg => msg && msg.id) // 过滤掉无效消息
        .map(msg => {
          // 确保每条消息都有必要的字段
          const senderId = msg.senderId ? Number(msg.senderId) : null;
          const isSentByCurrentUser = currentUserId !== null && senderId !== null && senderId === currentUserId;
          
          console.log(`消息ID ${msg.id} 发送者ID: ${senderId} (${typeof senderId}), 当前用户ID: ${currentUserId} (${typeof currentUserId}), isSelf: ${isSentByCurrentUser}`);
          console.log(`原始值 - 发送者ID: ${msg.senderId} (${typeof msg.senderId}), 当前用户ID: ${currentUser.value?.id} (${typeof currentUser.value?.id})`);
          
          const formattedMsg = {
            ...msg,
            id: msg.id || Date.now() + Math.random(),
            conversationId: msg.conversationId || Number(conversationId),
            createdAt: msg.createdAt || new Date().toISOString(),
            type: msg.type || msg.messageType || 'TEXT',
            content: msg.content || '',
            isSentByCurrentUser: isSentByCurrentUser,
            isSelf: isSentByCurrentUser // 添加isSelf属性以兼容ChatMessage组件
          };
          
          // 确保消息有正确的isSelf属性
          return ensureMessageHasCorrectIsSelf(formattedMsg);
        })
        .sort((a, b) => {
          // 安全地排序，处理无效日期
          const dateA = new Date(a.createdAt || 0).getTime();
          const dateB = new Date(b.createdAt || 0).getTime();
          return dateA - dateB;
        });
      
      // 更新消息列表
      messages.value = validMessages;
      
      console.log('会话消息已加载，共', validMessages.length, '条消息');
      console.log('消息列表中的isSelf状态:', validMessages.map(msg => ({ id: msg.id, senderId: msg.senderId, isSelf: msg.isSelf })));
    } else {
      console.error('加载会话消息失败:', response.message);
      if (!response.data || !response.data.content) {
        console.warn('服务器返回的消息数据为空');
        // 设置为空数组，而不是undefined
        messages.value = [];
      }
    }
    
    // 滚动到底部
    nextTick(() => {
      try {
        scrollToBottom();
      } catch (scrollError) {
        console.error('滚动到底部失败:', scrollError);
      }
    });
    
    // 标记消息为已读
    try {
      await markConversationAsRead();
    } catch (markError) {
      console.error('标记消息为已读失败:', markError);
      // 不阻止主流程继续
    }
  } catch (err: any) {
    console.error('加载消息失败:', err);
    error.value = err.message || '加载消息失败';
    
    // 确保在出错时消息列表是空的而不是undefined
    messages.value = [];
  } finally {
    loading.value = false;
  }
};

// 监听会话ID变化
watch(() => props.conversationId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    console.log('会话ID变化，加载新会话:', newId);
    loadMessages(newId);
  }
}, { immediate: true });

// 手动刷新消息
const refreshMessages = async () => {
  console.log('手动刷新消息列表');
  try {
    loading.value = true;
    await loadMessages(props.conversationId);
    console.log('消息列表刷新成功');
  } catch (error) {
    console.error('刷新消息失败:', error);
  } finally {
    loading.value = false;
  }
};

// 发送消息处理
const handleSendMessage = async (messageData: { content: string, type: string, mediaFileId?: number }) => {
  // 对于非媒体消息，需要检查内容是否为空
  if (!messageData.mediaFileId && (!messageData.content || !messageData.content.trim())) {
    console.log('消息内容为空且没有媒体ID，不发送消息');
    return;
  }
  
  // 如果是媒体消息，记录额外信息以便调试
  if (messageData.mediaFileId) {
    console.log('发送媒体消息:', {
      type: messageData.type,
      mediaFileId: messageData.mediaFileId,
      content: messageData.content,
      contentEmpty: !messageData.content || messageData.content.trim() === '',
      contentLength: messageData.content ? messageData.content.length : 0
    });
  }
  
  try {
    // 检查是否为群聊会话，如果是，则需要验证用户是否仍在群组中
    if (props.isGroupChat && props.conversationId) {
      const groupId = Number(props.conversationId);
      
      // 检查用户是否在群组中
      const isInGroup = await checkUserInGroup(groupId);
      if (!isInGroup) {
        ElMessage.warning('您已不是群成员，无法发送消息');
        return;
      }
    }
    
    // 生成唯一的临时消息ID
    const tempId = `temp-${Date.now()}-${Math.floor(Math.random() * 10000)}`;
    
    // 构建WebSocket消息
    const wsMessage = {
      type: 'message',
      data: {
        conversationId: Number(props.conversationId),
        content: messageData.content || '',
        messageType: messageData.type.toUpperCase(), // 确保消息类型是大写
        mediaFileId: messageData.mediaFileId,
        tempId: tempId // 包含临时ID以便后续匹配
      }
    };
    
    // 确保媒体消息有必要的字段
    if (messageData.mediaFileId) {
      console.log('添加媒体文件ID到WebSocket消息:', messageData.mediaFileId);
      wsMessage.data.mediaFileId = messageData.mediaFileId;
      
      // 确保内容字段不为空，即使前端不显示
      if (!wsMessage.data.content) {
        wsMessage.data.content = messageData.type === 'IMAGE' ? '图片消息' : 
                                messageData.type === 'VIDEO' ? '视频消息' : '文件消息';
        console.log('为空内容的媒体消息设置默认内容:', wsMessage.data.content);
      }
    }
    
    console.log('准备通过WebSocket发送消息:', wsMessage);
    
    // 尝试通过WebSocket发送消息
    const wsSent = wsSend(wsMessage);
    
    // 如果WebSocket发送失败，回退到HTTP API
    if (!wsSent) {
      console.log('WebSocket发送失败，回退到HTTP API');
      
      // 构建API请求数据
      const apiRequest: any = {
        conversationId: Number(props.conversationId),
        content: messageData.content || '',
        messageType: messageData.type.toUpperCase() // 确保消息类型是大写
      };
      
      // 如果有媒体文件ID，添加到请求
      if (messageData.mediaFileId) {
        apiRequest.mediaFileId = messageData.mediaFileId;
        
        // 确保内容字段不为空，即使前端不显示
        if (!apiRequest.content) {
          apiRequest.content = messageData.type === 'IMAGE' ? '图片消息' : 
                              messageData.type === 'VIDEO' ? '视频消息' : '文件消息';
          console.log('为空内容的媒体消息设置默认内容:', apiRequest.content);
        }
      }
      
      console.log('准备通过API发送消息:', apiRequest);
      
      // 调用API发送消息
      const response = await messageApi.sendMessage(apiRequest);
      
      // 简化的响应处理
      if (response.success && response.data) {
        console.log('消息通过API发送成功:', response.data);
        // 不执行任何UI更新，等待WebSocket通知
      } else {
        // 处理发送失败情况
        const errorMessage = response.message || '发送消息失败';
        
        // 检查是否是特定的错误类型，并显示友好的提示
        if (errorMessage.includes('不是对方好友') || errorMessage.includes('已不是对方好友')) {
          error.value = '您不是对方好友，无法发送消息';
          ElMessage.error('您不是对方好友，无法发送消息');
        } else if (errorMessage.includes('被对方屏蔽')) {
          error.value = '您已被对方屏蔽，无法发送消息';
          ElMessage.error('您已被对方屏蔽，无法发送消息');
        } else if (errorMessage.includes('不是群成员') || errorMessage.includes('已不是群成员')) {
          error.value = '您已不是群成员，无法发送消息';
          ElMessage.error('您已不是群成员，无法发送消息');
        } else {
          error.value = errorMessage;
          ElMessage.error(errorMessage);
        }
        
        throw new Error(errorMessage);
      }
    } else {
      console.log('消息已通过WebSocket发送，等待服务器确认');
      
      // 添加WebSocket错误处理
      // 设置一个超时，如果一段时间内没有收到确认，则显示错误
      const messageTimeout = setTimeout(() => {
        // 检查是否收到了消息确认
        if (!messageConfirmations.value.has(tempId)) {
          console.warn('消息发送超时，可能发送失败:', tempId);
          error.value = '消息发送失败，请检查您的网络连接或与对方的关系状态';
          ElMessage.error('消息发送失败，请检查您的网络连接或与对方的关系状态');
        }
      }, 5000); // 5秒超时
      
      // 存储超时处理器，以便在收到确认时清除
      messageTimeouts.set(tempId, messageTimeout);
    }
  } catch (err: any) {
    console.error('发送消息失败:', err);
    
    // 显示友好的错误消息
    const errorMessage = err.message || '发送消息失败';
    
    // 检查是否是特定的错误类型，并显示友好的提示
    if (errorMessage.includes('不是对方好友') || errorMessage.includes('已不是对方好友')) {
      error.value = '您不是对方好友，无法发送消息';
      ElMessage.error('您不是对方好友，无法发送消息');
    } else if (errorMessage.includes('被对方屏蔽')) {
      error.value = '您已被对方屏蔽，无法发送消息';
      ElMessage.error('您已被对方屏蔽，无法发送消息');
    } else if (errorMessage.includes('不是群成员') || errorMessage.includes('已不是群成员')) {
      error.value = '您已不是群成员，无法发送消息';
      ElMessage.error('您已不是群成员，无法发送消息');
    } else {
      error.value = errorMessage;
      ElMessage.error(errorMessage);
    }
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

// 处理消息撤回
const handleRecallMessage = (messageId: string | number) => {
  console.log('处理消息撤回:', messageId, '类型:', typeof messageId);
  
  if (messageId === undefined || messageId === null) {
    console.error('撤回消息时收到无效的消息ID');
    return;
  }
  
  // 查找消息
  const index = messages.value.findIndex(msg => {
    // 严格比较ID
    if (typeof msg.id === typeof messageId) {
      return msg.id === messageId;
    } 
    // 字符串和数字类型的ID需要特殊处理
    else if (typeof msg.id === 'string' && typeof messageId === 'number') {
      return Number(msg.id) === messageId;
    }
    else if (typeof msg.id === 'number' && typeof messageId === 'string') {
      return msg.id === Number(messageId);
    }
    return false;
  });
  
  if (index !== -1) {
    // 更新消息状态为已撤回
    messages.value[index].status = 'RECALLED';
    console.log('消息已标记为已撤回:', messageId, '索引:', index);
    
    // 如果是最后一条消息，更新会话的最后一条消息状态
    if (index === messages.value.length - 1) {
      console.log('更新会话的最后一条消息状态');
      // 这里可以触发事件通知父组件更新会话列表
    }
  } else {
    console.error('找不到要撤回的消息:', messageId);
    console.log('当前消息列表:', messages.value.map(m => ({ id: m.id, type: typeof m.id })));
    
    // 尝试通过数字转换再次查找
    if (typeof messageId === 'string') {
      const numericId = Number(messageId);
      if (!isNaN(numericId)) {
        const numericIndex = messages.value.findIndex(msg => msg.id === numericId);
        if (numericIndex !== -1) {
          messages.value[numericIndex].status = 'RECALLED';
          console.log('通过数字转换找到并标记消息为已撤回:', numericId, '索引:', numericIndex);
        }
      }
    } else if (typeof messageId === 'number') {
      const stringId = String(messageId);
      const stringIndex = messages.value.findIndex(msg => msg.id === stringId);
      if (stringIndex !== -1) {
        messages.value[stringIndex].status = 'RECALLED';
        console.log('通过字符串转换找到并标记消息为已撤回:', stringId, '索引:', stringIndex);
      }
    }
  }
};

// 处理消息编辑
const handleEditMessage = (messageId: string | number, newContent: string) => {
  console.log('处理消息编辑:', messageId, '新内容:', newContent);
  
  if (messageId === undefined || messageId === null) {
    console.error('编辑消息时收到无效的消息ID');
    return;
  }
  
  // 查找消息
  const index = messages.value.findIndex(msg => {
    // 严格比较ID
    if (typeof msg.id === typeof messageId) {
      return msg.id === messageId;
    } 
    // 字符串和数字类型的ID需要特殊处理
    else if (typeof msg.id === 'string' && typeof messageId === 'number') {
      return Number(msg.id) === messageId;
    }
    else if (typeof msg.id === 'number' && typeof messageId === 'string') {
      return msg.id === Number(messageId);
    }
    return false;
  });
  
  if (index !== -1) {
    // 更新消息内容和编辑状态
    messages.value[index].content = newContent;
    messages.value[index].edited = true;
    console.log('消息已更新:', messageId, '索引:', index);
  } else {
    console.error('找不到要编辑的消息:', messageId);
    console.log('当前消息列表:', messages.value.map(m => ({ id: m.id, type: typeof m.id })));
  }
};

// 搜索相关方法
const toggleSearchPanel = () => {
  console.log('切换搜索面板，当前状态:', isSearchActive.value);
  isSearchActive.value = !isSearchActive.value;
  console.log('搜索面板新状态:', isSearchActive.value);
  
  // 如果激活搜索面板，确保消息容器可见
  if (isSearchActive.value) {
    console.log('搜索面板已激活');
  }
};

const closeSearchPanel = () => {
  console.log('关闭搜索面板');
  isSearchActive.value = false;
};

// 滚动到指定消息
const scrollToMessage = (messageId: number) => {
  // 关闭搜索面板
  closeSearchPanel();
  
  console.log('滚动到消息:', messageId);
  
  // 设置高亮消息ID
  highlightedMessageId.value = messageId;
  
  // 查找消息元素
  const messageElement = document.querySelector(`[data-message-id="${messageId}"]`);
  if (messageElement) {
    // 滚动到消息位置
    messageElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
    
    // 3秒后移除高亮
    setTimeout(() => {
      highlightedMessageId.value = null;
    }, 3000);
  } else {
    console.warn('找不到消息元素:', messageId);
    
    // 如果找不到消息，可能需要重新加载
    loadMessages(props.conversationId).then(() => {
      // 延迟尝试再次查找和滚动
      setTimeout(() => {
        const element = document.querySelector(`[data-message-id="${messageId}"]`);
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'center' });
          // 3秒后移除高亮
          setTimeout(() => {
            highlightedMessageId.value = null;
          }, 3000);
        }
      }, 300);
    });
  }
};

// 重新连接WebSocket
const reconnectWebSocket = () => {
  console.log('手动重新连接WebSocket');
  wsConnect();
};

// 切换选择模式
const toggleSelectionMode = () => {
  isSelectionMode.value = !isSelectionMode.value;
  
  // 如果退出选择模式，清空已选消息
  if (!isSelectionMode.value) {
    selectedMessages.value = [];
  }
};

// 取消选择模式
const cancelSelectionMode = () => {
  isSelectionMode.value = false;
  selectedMessages.value = [];
};

// 检查消息是否被选中
const isMessageSelected = (messageId: number | string) => {
  return selectedMessages.value.some(msg => msg.id === messageId);
};

// 切换消息选择状态
const toggleMessageSelection = (messageId: number | string, message: any) => {
  const index = selectedMessages.value.findIndex(msg => msg.id === messageId);
  
  if (index === -1) {
    // 添加到选中列表
    selectedMessages.value.push(message);
  } else {
    // 从选中列表移除
    selectedMessages.value.splice(index, 1);
  }
};

// 转发选中的消息
const forwardSelectedMessages = () => {
  if (selectedMessages.value.length === 0) {
    return;
  }
  
  showForwardDialog.value = true;
};

// 关闭转发对话框
const closeForwardDialog = () => {
  showForwardDialog.value = false;
};

// 处理转发成功
const handleForwardSuccess = () => {
  console.log('消息转发成功');
  
  // 退出选择模式
  cancelSelectionMode();
};

// 生命周期钩子
onMounted(() => {
  console.log('ChatPanel组件挂载');
  
  // 调试当前用户信息
  console.log('当前用户信息:', currentUser.value);
  console.log('当前用户ID (currentUser.value?.id):', currentUser.value?.id, '类型:', typeof currentUser.value?.id);
  
  // 尝试从存储中获取用户ID
  const storageUserId = getCurrentUserIdFromStorage();
  console.log('从存储中获取的用户ID:', storageUserId);
  
  // 如果currentUser.value?.id为null但存储中有用户ID，尝试手动设置currentUser
  if (!currentUser.value?.id && storageUserId) {
    console.log('尝试手动设置currentUser');
    
    // 尝试从localStorage获取完整的用户信息
    try {
      const userStr = localStorage.getItem('current_user') || sessionStorage.getItem('current_user');
      if (userStr) {
        const userData = JSON.parse(userStr);
        console.log('从存储中获取的用户数据:', userData);
        
        // 如果useAuth的currentUser为null，但我们有用户数据，可以考虑手动设置
        // 注意：这里不直接修改currentUser.value，因为它是响应式的，可能会导致问题
        // 但我们可以记录这个情况
        console.log('注意: currentUser.value为空，但存储中有用户数据');
      }
    } catch (e) {
      console.error('解析用户数据失败:', e);
    }
  }
  
  // 如果会话ID存在，加载消息
  if (props.conversationId) {
    loadMessages(props.conversationId);
  }
});

// 暴露方法供父组件调用
defineExpose({
  loadMessages,
  refreshMessages
});



// 加载聊天消息
async function loadChatMessages() {
  if (!props.conversationId) {
    error.value = '无效的会话ID';
    return;
  }
  
  loading.value = true;
  messages.value = [];
  error.value = null;
  
  try {
    console.log(`加载会话 ${props.conversationId} 的消息`);
    
    const response = await messageApi.getMessages(Number(props.conversationId), 0, 20);
    
    if (response.success && response.data) {
      let messagesData: any[] = [];
      
      if (Array.isArray(response.data)) {
        messagesData = response.data;
      } else if (response.data.content && Array.isArray(response.data.content)) {
        messagesData = response.data.content;
      }
      
      console.log(`成功加载 ${messagesData.length} 条消息`);
      
      // 处理消息数据
      messages.value = messagesData.map((msg: any) => {
        // 添加必要的字段
        const isSelf = msg.senderId === currentUser.value?.id;
        return {
          ...msg,
          isSelf,
          isSentByCurrentUser: isSelf
        };
      });
      
      // 按时间排序
      messages.value.sort((a, b) => {
        return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      });
      
      // 所有消息加载完成后，如果有消息，更新阅读光标
      if (messages.value.length > 0) {
        const lastMessage = messages.value[messages.value.length - 1];
        updateReadCursor(Number(props.conversationId), lastMessage.id);
      }
    } else {
      throw new Error(response.message || '加载消息失败');
    }
    
    nextTick(() => {
      scrollToBottom();
    });
  } catch (err: any) {
    console.error('加载消息失败:', err);
    error.value = typeof err === 'string' ? err : err.message || '加载消息失败';
    
    if (err.status === 401) {
      // 认证失败，跳转到登录页
      router.push('/login');
    }
  } finally {
    loading.value = false;
  }
}

// 检查用户是否在群组中
const checkUserInGroup = async (groupId: number): Promise<boolean> => {
  try {
    const response = await groupApi.checkUserInGroup(groupId);
    return response.success;
  } catch (error) {
    console.error('检查用户是否在群组中出错:', error);
    return false;
  }
};

const openSearchPanel = () => {
  console.log('打开搜索面板');
  isSearchActive.value = true;
};
</script>

<style scoped>
@keyframes highlight-fade {
  0%, 25% { background-color: rgba(76, 175, 80, 0.2); }
  100% { background-color: transparent; }
}

.highlight-message {
  animation: highlight-fade 3s ease-out;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
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
  color: var(--primary-color);
}

.search-action-btn {
  background-color: #f0f0f0;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
}

.search-action-btn:hover {
  background-color: #e0e0e0;
}

.search-text {
  font-size: 12px;
}

.message-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #f9f9f9;
  font-size: var(--font-size-base);
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
  border-top: 3px solid var(--primary-color);
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

/* 选择模式样式 */
.selection-mode-banner {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background-color: #f0f8ff;
  border-bottom: 1px solid #cce0ff;
}

.selection-info {
  font-weight: bold;
  color: #1976d2;
}

.selection-actions {
  display: flex;
  gap: 10px;
}

.selection-action-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.selection-action-btn.forward-btn {
  background-color: #1976d2;
  color: white;
  font-size: 14px;
}

.selection-action-btn.forward-btn:hover {
  background-color: #1565c0;
}

.selection-action-btn.forward-btn:disabled {
  background-color: #bbdefb;
  cursor: not-allowed;
}

.selection-action-btn.cancel-btn {
  background-color: #f5f5f5;
  color: #666;
}

.selection-action-btn.cancel-btn:hover {
  background-color: #e0e0e0;
}

.message-item-wrapper {
  display: flex;
  align-items: flex-start;
  margin-bottom: 10px;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
  position: relative;
}

.message-item-wrapper:hover {
  background-color: rgba(0, 0, 0, 0.03);
}

.message-selected {
  background-color: rgba(25, 118, 210, 0.08);
  border: 1px solid rgba(25, 118, 210, 0.2);
}

.message-checkbox {
  margin-right: 8px;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: #ccc;
  transition: color 0.2s;
}

.message-selected .message-checkbox {
  color: #1976d2;
}

/* WebSocket连接状态指示器样式 */
.ws-status-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: #ffebee;
  color: #c62828;
  font-size: 12px;
  margin-right: 8px;
}

.ws-status-indicator.connected {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.ws-status-indicator .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #c62828; /* 红色 */
}

.ws-status-indicator.connected .status-dot {
  background-color: #2e7d32; /* 绿色 */
}

/* WebSocket重连按钮 */
.reconnect-btn {
  background: none;
  border: none;
  color: #c62828;
  cursor: pointer;
  padding: 2px 5px;
  font-size: 10px;
  border-radius: 2px;
  transition: all 0.2s;
}

.reconnect-btn:hover {
  background-color: rgba(198, 40, 40, 0.1);
}

/* 输入状态指示器样式 */
.typing-indicator {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background-color: #f5f5f5;
  border-top: 1px solid #e0e0e0;
}

.typing-animation {
  display: flex;
  align-items: center;
  margin-right: 8px;
}

.typing-animation span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #666;
  margin: 0 2px;
  animation: typing-animation 1.4s infinite ease-in-out both;
}

.typing-animation span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-animation span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing-animation {
  0%, 80%, 100% { 
    transform: scale(0);
  } 
  40% { 
    transform: scale(1);
  }
}

.typing-text {
  font-size: 14px;
  color: #666;
  font-style: italic;
}

/* 控制按钮样式 */
.chat-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background-color: #f5f5f5;
  border-top: 1px solid #e0e0e0;
}

.control-buttons {
  display: flex;
  gap: 10px;
}

.control-btn {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 4px;
  transition: all 0.2s;
}

.control-btn:hover {
  background-color: #e0e0e0;
}

.selection-mode-btn {
  background-color: #1976d2;
  color: white;
}

.selection-mode-btn.active {
  background-color: #1565c0;
}

.checkbox-indicator {
  width: 24px;
  height: 24px;
  border: 2px solid #ccc;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.checkbox-indicator.selected {
  background-color: #1976d2;
  border-color: #1976d2;
}

.message-checkbox {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  cursor: pointer;
  margin-right: 10px;
}

.checkbox-indicator {
  width: 24px;
  height: 24px;
  border: 2px solid #1976d2;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  background-color: #fff;
}

.checkbox-indicator:hover {
  border-color: #1565c0;
  background-color: #e3f2fd;
}

.checkbox-indicator.selected {
  background-color: #1976d2;
  border-color: #1976d2;
  color: white;
}

.message-selected {
  background-color: rgba(25, 118, 210, 0.05);
  border-left: 3px solid #1976d2;
}

/* 选择模式提示 */
.selection-mode-hint {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background-color: #f0f8ff;
  border-bottom: 1px solid #cce0ff;
  color: #1976d2;
}

.selection-mode-hint i {
  margin-right: 10px;
}

.selection-mode-hint span {
  font-size: 12px;
  font-weight: bold;
}
</style> 