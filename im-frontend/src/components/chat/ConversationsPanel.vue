<template>
  <div class="conversations-panel">
    <div class="search-bar">
      <div class="search-input-container">
        <i class="fas fa-search search-icon"></i>
      <input 
        v-model="searchKeyword"
        type="text" 
        placeholder="搜索会话..." 
        class="search-input"
        @input="handleSearch"
      />
      </div>
      <button 
        class="refresh-button" 
        @click="refreshConversations" 
        title="刷新会话列表"
        :class="{ 'refreshing': isRefreshing }"
      >
        <i class="fas fa-sync-alt"></i>
        <span class="refresh-text">刷新</span>
      </button>
    </div>
    
    <!-- 会话类型切换 -->
    <div class="conversation-tabs">
      <div 
        class="tab" 
        :class="{ active: activeTab === 'regular' }"
        @click="switchTab('regular')"
      >
        <i class="fas fa-comments"></i> 会话列表
      </div>
      <div 
        class="tab" 
        :class="{ active: activeTab === 'archived' }"
        @click="switchTab('archived')"
      >
        <i class="fas fa-archive"></i> 已归档
      </div>
    </div>
    
    <div class="chat-list">
      <div v-if="loading && chats.length === 0" class="chats-loading">
        加载中...
      </div>
      <div v-else-if="filteredChats.length === 0 && !loading" class="no-chats">
        {{ searchKeyword ? '未找到匹配的会话' : 
           activeTab === 'regular' ? '暂无会话' : '暂无已归档会话' }}
      </div>
      
      <!-- 常规会话 -->
      <template v-if="activeTab === 'regular'">
      <!-- 置顶会话 -->
      <div v-if="pinnedChats.length > 0" class="pinned-section">
          <div class="section-header">
            <i class="fas fa-thumbtack"></i> 置顶会话
          </div>
        <conversation-item
          v-for="chat in pinnedChats" 
          :key="chat.id"
          :chat="chat"
          :active-chat-id="props.activeChatId"
          @click="handleSelectChat(chat)"
          @context-menu="showContextMenu($event, chat)"
        />
      </div>

      <!-- 普通会话 -->
      <div v-if="regularChats.length > 0" class="regular-section">
          <div class="section-header">
            <i class="fas fa-comments"></i> 会话列表
          </div>
        <conversation-item
          v-for="chat in regularChats" 
          :key="chat.id"
          :chat="chat"
          :active-chat-id="props.activeChatId"
          @click="handleSelectChat(chat)"
          @context-menu="showContextMenu($event, chat)"
        />
      </div>
      </template>
      
      <!-- 已归档会话 -->
      <template v-else>
        <div class="archived-section">
          <div class="section-header archived-header">
            <i class="fas fa-archive"></i> 已归档会话
          </div>
          <conversation-item
            v-for="chat in filteredChats" 
            :key="chat.id"
            :chat="chat"
            :active-chat-id="props.activeChatId"
            :is-archived="true"
            @click="handleSelectChat(chat)"
            @context-menu="showContextMenu($event, chat)"
          />
        </div>
      </template>
    </div>

    <!-- 上下文菜单 -->
    <div 
      v-if="showMenu" 
      class="context-menu"
      :style="{ top: menuPos.y + 'px', left: menuPos.x + 'px' }"
      @click.stop
    >
      <div class="menu-item" @click="handlePin">
        {{ selectedChat?.isPinned ? '取消置顶' : '置顶会话' }}
      </div>
      <div class="menu-item" @click="handleMute">
        {{ selectedChat?.isDnd ? '取消免打扰' : '开启免打扰' }}
      </div>
      <div class="menu-item" @click="handleArchive">
        {{ activeTab === 'archived' ? '取消归档' : '归档会话' }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch, onUnmounted } from 'vue';
import { useAuth } from '@/composables/useAuth';
import { messageApi } from '@/api/message';
import ConversationItem from './ConversationItem.vue';
import { getCurrentUserId } from '@/utils/helpers';
import { useSharedWebSocket } from '@/composables/useWebSocket';
import { ElMessage } from 'element-plus'; // Added ElMessage import

// 定义props和事件
const props = defineProps({
  activeChat: {
    type: Object,
    default: null
  },
  activeChatId: {
    type: String,
    default: null
  }
});

const emit = defineEmits(['select-chat', 'pin-chat', 'mute-chat', 'archive-chat', 'error']);

// 会话数据
const chats = ref<any[]>([]);
const loading = ref(false);
const isRefreshing = ref(false); // 添加刷新状态
const searchKeyword = ref('');
const error = ref<string | null>(null);
const activeTab = ref<'regular' | 'archived'>('regular');

// 上下文菜单相关
const showMenu = ref(false);
const menuPos = ref({ x: 0, y: 0 });
const selectedChat = ref<any>(null);

// 获取当前用户信息
const { currentUser } = useAuth();

// 初始化WebSocket连接
const { 
  status: wsStatus, 
  connect: wsConnect, 
  disconnect: wsDisconnect, 
  isConnected: wsConnected,
  send: wsSend
} = useSharedWebSocket(handleWebSocketMessage);

// WebSocket连接状态指示器
const wsStatusText = computed(() => {
  return wsConnected.value ? '已连接' : '未连接';
});

// 在组件挂载时连接WebSocket
onMounted(() => {
  if (!wsConnected.value) {
    console.log('ConversationsPanel: 连接WebSocket');
    wsConnect();
  }
});

// 在组件卸载时断开WebSocket连接
onUnmounted(() => {
  console.log('ConversationsPanel: 断开WebSocket连接');
  wsDisconnect();
});

// 处理WebSocket消息
function handleWebSocketMessage(data: any) {
  console.log('ConversationsPanel收到WebSocket消息:', data);
  
  try {
    // 标准化消息类型（转换为大写）
    const messageType = data.type ? data.type.toUpperCase() : null;
    
    // 处理不同类型的消息
    switch (messageType) {
      case 'CONVERSATION_UPDATE':
        // 会话更新
        handleConversationUpdate(data);
        break;
      case 'CONVERSATION_PIN':
        // 会话置顶状态更新
        handleConversationPin(data);
        break;
      case 'CONVERSATION_MUTE':
        // 会话免打扰状态更新
        handleConversationMute(data);
        break;
      case 'CONVERSATION_ARCHIVE':
        // 会话归档状态更新
        handleConversationArchive(data);
        break;
      case 'MESSAGE':
      case 'NEW_MESSAGE':
        // 新消息
        console.log('收到新消息WebSocket通知，立即处理未读计数更新');
        handleNewMessage(data);
        break;
      case 'READ_STATUS_UPDATE':
        // 已读状态更新
        handleReadStatusUpdate(data);
        break;
      case 'RECALL':
        // 消息撤回
        handleMessageRecallForConversation(data);
        break;
      case 'GROUP_UPDATE':
        // 群组更新
        handleGroupUpdate(data);
        break;
      default:
        console.log('未处理的WebSocket消息类型:', messageType);
        break;
    }
  } catch (error) {
    console.error('处理WebSocket消息失败:', error);
  }
}

// 处理阅读状态更新通知
function handleReadStatusUpdate(data: any) {
  try {
    console.log('[DEBUG] 处理已读状态更新:', JSON.stringify(data));
    
    const updateData = data.data || data;
    const conversationId = updateData.conversationId;
    const lastReadMessageId = updateData.lastReadMessageId;
    const readerId = updateData.readerId;
    const isUserAction = updateData.isUserAction === true; // 检查是否是用户主动操作
    
    console.log(`[DEBUG] 已读状态关键信息: conversationId=${conversationId}, lastReadMessageId=${lastReadMessageId}, readerId=${readerId}, isUserAction=${isUserAction}`);
    
    if (!conversationId) {
      console.warn('[DEBUG] 已读状态更新中缺少会话ID');
      return;
    }
    
    // 获取当前用户ID
    const currentUserId = getCurrentUserId();
    console.log(`[DEBUG] 当前用户ID: ${currentUserId}, 读者ID: ${readerId}`);
    
    // 如果是当前用户的已读状态更新，且是由用户主动操作触发的
    // 或者是当前活跃的会话（用户正在查看）
    const isActiveConversation = props.activeChatId === String(conversationId);
    
    // 调试信息：检查是否有自动更新last_read_message_id的请求
    if (readerId === currentUserId && !isUserAction && !isActiveConversation) {
      console.warn(`[DEBUG] 检测到非用户主动操作的lastReadMessageId更新: ${lastReadMessageId}, 会话ID: ${conversationId}`);
    }
    
    // 只有当是用户主动操作时才重置未读计数
    // 移除isActiveConversation条件，因为仅仅是当前活跃会话不应该自动重置未读计数
    if (readerId === currentUserId && isUserAction) {
      console.log(`[DEBUG] 当前用户已读状态更新，会话: ${conversationId}，lastReadMessageId: ${lastReadMessageId}，是用户主动操作: ${isUserAction}，是当前活跃会话: ${isActiveConversation}`);
    
      // 更新本地未读计数
    const conversation = chats.value.find(c => c.id === conversationId);
    if (conversation) {
      conversation.unreadCount = 0;
        console.log(`[DEBUG] 已将会话 ${conversationId} 的未读计数重置为0`);
      
        // 强制更新视图
      chats.value = [...chats.value];
      }
    } else {
      console.log(`[DEBUG] 忽略非用户主动操作的已读状态更新，会话: ${conversationId}，用户ID: ${readerId}，当前用户: ${currentUserId}，是用户主动操作: ${isUserAction}`);
    }
  } catch (error) {
    console.error('[DEBUG] 处理已读状态更新失败:', error);
  }
}

// 处理会话更新通知
function handleConversationUpdate(data: any) {
  try {
    console.log('处理会话更新通知:', data);
    
    // 提取会话数据
    const updateData = data.data || {};
    const conversationData = updateData.data;
    const conversationId = updateData.conversationId;
    const updateType = updateData.updateType;
    
    if (!conversationId || !conversationData) {
      console.warn('会话更新通知缺少必要数据');
      return;
    }
    
    console.log(`收到会话 ${conversationId} 的更新通知，类型: ${updateType}`);
    
    // 根据更新类型处理
    switch (updateType) {
      case 'UPDATE':
        // 更新现有会话
        updateExistingConversation(conversationId, conversationData);
        break;
      case 'NEW':
        // 添加新会话
        addNewConversation(conversationData);
        break;
      case 'DELETE':
        // 删除会话
        removeConversation(conversationId);
        break;
      default:
        console.log(`未处理的会话更新类型: ${updateType}`);
        break;
    }
  } catch (error) {
    console.error('处理会话更新通知出错:', error);
  }
}

// 处理会话置顶状态更新
function handleConversationPin(data: any) {
  try {
    console.log('处理会话置顶状态更新:', data);
    
    // 提取数据
    const updateData = data.data || {};
    const conversationId = updateData.conversationId;
    const isPinned = updateData.isPinned;
    const userId = updateData.userId;
    
    // 验证数据
    if (conversationId === undefined || isPinned === undefined) {
      console.warn('会话置顶状态更新通知缺少必要数据');
      return;
    }
    
    // 获取当前用户ID
    const currentUserId = getCurrentUserId();
    
    // 检查是否是针对当前用户的通知
    if (userId && currentUserId && userId !== currentUserId) {
      console.log(`置顶状态更新是针对用户 ${userId} 的，当前用户是 ${currentUserId}，仍然更新UI以保持一致性`);
    }
    
    // 查找会话并更新置顶状态
    const conversation = chats.value.find(c => c.id === conversationId);
    if (conversation) {
      console.log(`更新会话 ${conversationId} 的置顶状态为 ${isPinned}`);
      conversation.isPinned = isPinned;
      
      // 触发视图更新
      sortConversations();
      chats.value = [...chats.value];
    } else {
      console.warn(`找不到会话 ${conversationId}，无法更新置顶状态`);
      
      // 如果会话不在列表中，尝试刷新会话列表
      loadConversations().catch(err => {
        console.warn('自动刷新会话列表失败:', err);
      });
    }
  } catch (error) {
    console.error('处理会话置顶状态更新出错:', error);
  }
}

// 处理会话归档状态更新
function handleConversationArchive(data: any) {
  try {
    console.log('处理会话归档状态更新:', data);
    
    // 提取数据
    const updateData = data.data || {};
    const conversationId = updateData.conversationId;
    const isArchived = updateData.isArchived;
    const userId = updateData.userId;
    
    // 验证数据
    if (conversationId === undefined || isArchived === undefined) {
      console.warn('会话归档状态更新通知缺少必要数据');
      return;
    }
    
    // 获取当前用户ID
    const currentUserId = getCurrentUserId();
    
    // 检查是否是针对当前用户的通知
    if (userId && currentUserId && userId !== currentUserId) {
      console.log(`归档状态更新是针对用户 ${userId} 的，当前用户是 ${currentUserId}，仍然更新UI以保持一致性`);
    }
    
    // 查找会话并更新归档状态
    const conversation = chats.value.find(c => c.id === conversationId);
    if (conversation) {
      console.log(`更新会话 ${conversationId} 的归档状态为 ${isArchived}`);
      conversation.isArchived = isArchived;
      
      // 如果是归档操作，从当前列表中移除
      if (isArchived && activeTab.value === 'regular') {
        chats.value = chats.value.filter(c => c.id !== conversationId);
        console.log(`会话 ${conversationId} 已归档，从常规列表中移除`);
      }
      
      // 如果是取消归档操作，从归档列表中移除
      if (!isArchived && activeTab.value === 'archived') {
        chats.value = chats.value.filter(c => c.id !== conversationId);
        console.log(`会话 ${conversationId} 已取消归档，从归档列表中移除`);
      }
    } else {
      console.warn(`找不到会话 ${conversationId}，无法更新归档状态`);
      
      // 如果会话不在列表中，且是取消归档操作，刷新常规会话列表
      if (!isArchived && activeTab.value === 'regular') {
        console.log('会话不在列表中，尝试刷新会话列表');
        loadConversations().catch(err => {
          console.warn('自动刷新会话列表失败:', err);
        });
      }
      
      // 如果会话不在列表中，且是归档操作，刷新归档会话列表
      if (isArchived && activeTab.value === 'archived') {
        console.log('会话不在列表中，尝试刷新归档会话列表');
        loadArchivedConversations().catch(err => {
          console.warn('自动刷新归档会话列表失败:', err);
        });
      }
    }
  } catch (error) {
    console.error('处理会话归档状态更新出错:', error);
  }
}

// 处理会话免打扰状态更新
function handleConversationMute(data: any) {
  try {
    console.log('处理会话免打扰状态更新:', data);
    
    // 提取数据
    const updateData = data.data || {};
    const conversationId = updateData.conversationId;
    const isDnd = updateData.isDnd;
    const userId = updateData.userId;
    
    // 验证数据
    if (conversationId === undefined || isDnd === undefined) {
      console.warn('会话免打扰状态更新通知缺少必要数据');
      return;
    }
    
    // 获取当前用户ID
    const currentUserId = getCurrentUserId();
    
    // 检查是否是针对当前用户的通知
    if (userId && currentUserId && userId !== currentUserId) {
      console.log(`免打扰状态更新是针对用户 ${userId} 的，当前用户是 ${currentUserId}，仍然更新UI以保持一致性`);
    }
    
    // 查找会话并更新免打扰状态
    const conversation = chats.value.find(c => c.id === conversationId);
    if (conversation) {
      console.log(`更新会话 ${conversationId} 的免打扰状态为 ${isDnd}`);
      conversation.isDnd = isDnd;
      
      // 触发视图更新
      chats.value = [...chats.value];
    } else {
      console.warn(`找不到会话 ${conversationId}，无法更新免打扰状态`);
      
      // 如果会话不在列表中，尝试刷新会话列表
      loadConversations().catch(err => {
        console.warn('自动刷新会话列表失败:', err);
      });
    }
  } catch (error) {
    console.error('处理会话免打扰状态更新出错:', error);
  }
}

// 处理新消息对会话列表的影响
async function handleNewMessage(data: any) {
  try {
    // 提取消息数据
    const messageData = data.data || data.message || data;
    const conversationId = messageData.conversationId;
    const messageId = messageData.id;
    
    if (!conversationId) {
      console.warn('[DEBUG] 消息数据中缺少会话ID');
      return;
    }
    
    console.log(`[DEBUG] 收到会话 ${conversationId} 的新消息, ID: ${messageId}, 内容: ${messageData.content?.substring(0, 20)}...`);
    
    // 查找对应的会话
    const conversation = chats.value.find(c => c.id === conversationId);
    
    if (conversation) {
      console.log(`[DEBUG] 找到对应的会话: ${conversation.id}, 当前未读计数: ${conversation.unreadCount}`);
      
      // 更新会话的最后一条消息内容
      conversation.lastMessage = messageData.content || '新消息';
      
      // 更新会话的最后活跃时间
      conversation.lastMessageTime = messageData.createdAt || new Date().toISOString();
      
      // 获取当前用户ID
      const currentUserId = getCurrentUserId();
      
      // 确定消息发送者
      const senderId = messageData.senderId ? Number(messageData.senderId) : null;
      // 严格比较senderId和currentUserId，忽略messageData.isSelf和messageData.isSentByCurrentUser
      const isSentByCurrentUser = (senderId !== null && currentUserId !== null && senderId === currentUserId);
      
      console.log(`[DEBUG] 消息发送者检查: senderId=${senderId}, currentUserId=${currentUserId}, isSelf=${messageData.isSelf}, isSentByCurrentUser=${messageData.isSentByCurrentUser}, 严格比较结果=${isSentByCurrentUser}`);
      
      // 如果不是当前用户发送的消息，且不是当前活跃会话，增加未读消息计数
      if (!isSentByCurrentUser) {
        // 检查是否是当前活跃会话，如果是则不增加计数（因为用户正在查看）
        const isActiveConversation = props.activeChatId === String(conversationId);
        if (!isActiveConversation) {
          // 确保unreadCount是数字类型
          if (typeof conversation.unreadCount !== 'number') {
            conversation.unreadCount = 0;
          }
          
          // 获取当前未读计数
          const currentUnreadCount = conversation.unreadCount;
          
          // 从后端获取准确的未读计数
          try {
            const response = await messageApi.getUnreadMessageCount(conversationId);
            if (response.success && typeof response.data === 'number') {
              console.log(`[DEBUG] 会话 ${conversationId} 的后端未读计数: ${response.data}, 本地计数: ${currentUnreadCount}`);
              // 使用后端返回的未读计数，确保准确性
              conversation.unreadCount = response.data;
            } else {
              // 如果API调用失败，则增加本地计数
              conversation.unreadCount = currentUnreadCount + 1;
            }
          } catch (error) {
            console.error(`[DEBUG] 获取会话 ${conversationId} 未读计数失败:`, error);
            // 如果API调用失败，则增加本地计数
            conversation.unreadCount = currentUnreadCount + 1;
          }
          
          console.log(`[DEBUG] 更新会话 ${conversationId} 未读计数为: ${conversation.unreadCount}`);
          
          // 强制视图更新
          chats.value = [...chats.value];
      } else {
          console.log(`[DEBUG] 当前正在查看会话 ${conversationId}，不增加未读计数`);
          // 不自动标记为已读，等待用户交互后再标记
          // 这样可以确保未读消息计数不会自动重置
          
          // 检查是否有自动更新lastReadMessageId的请求
          console.log(`[DEBUG] 当前会话 ${conversationId} 接收到新消息 ${messageId}，检查是否有自动更新lastReadMessageId的请求`);
        }
      } else {
        console.log('[DEBUG] 这是当前用户发送的消息，不增加未读计数');
      }
      
      // 将该会话移到列表顶部（除非是置顶会话，置顶会话之间保持原有顺序）
      const pinnedChats = chats.value.filter(c => c.isPinned);
      const nonPinnedChats = chats.value.filter(c => !c.isPinned);
      
      // 如果是非置顶会话，需要调整顺序
      if (!conversation.isPinned) {
        // 从非置顶列表中移除该会话
        const otherNonPinnedChats = nonPinnedChats.filter(c => c.id !== conversationId);
        
        // 将该会话放到非置顶列表的最前面
        nonPinnedChats.length = 0;
        nonPinnedChats.push(conversation, ...otherNonPinnedChats);
      }
      
      // 重新组合会话列表
      chats.value = [...pinnedChats, ...nonPinnedChats];
      
      // 强制视图更新
      nextTick(() => {
        console.log('[DEBUG] 会话列表已更新，未读计数:', conversation.unreadCount);
      });
    } else {
      console.log(`[DEBUG] 收到新消息的会话 ${conversationId} 不在当前列表中，尝试刷新会话列表`);
      // 如果会话不在列表中，刷新会话列表
      loadConversations(true).catch(err => {
        console.warn('[DEBUG] 自动刷新会话列表失败:', err);
      });
    }
  } catch (error) {
    console.error('[DEBUG] 处理新消息对会话列表的影响时出错:', error);
  }
}

// 标记消息为已读
async function markMessageAsRead(messageId: number, conversationId: number, isUserAction: boolean = false) {
  // 如果不是用户主动操作，记录并拒绝执行
  if (!isUserAction) {
    console.warn(`[DEBUG] 拒绝非用户主动操作的标记已读请求，消息ID: ${messageId}`);
    return;
  }
  
  try {
    console.log(`标记消息 ${messageId} 为已读，用户主动操作: ${isUserAction}`);
    await messageApi.markMessageAsRead(messageId, isUserAction);
    
    // 更新本地未读计数
    const conversation = chats.value.find(c => c.id === conversationId);
    if (conversation) {
      conversation.unreadCount = 0;
      console.log(`已将会话 ${conversationId} 的未读计数重置为0`);
    }
    
    console.log('消息已标记为已读');
  } catch (error) {
    console.error('标记消息已读失败:', error);
  }
}

// 处理消息撤回对会话列表的影响
function handleMessageRecallForConversation(data: any) {
  try {
    // 提取撤回数据
    const recallData = data.data || data;
    const conversationId = recallData.conversationId;
    
    if (!conversationId) {
      console.warn('消息撤回通知缺少会话ID');
      return;
    }
    
    // 如果撤回的是会话的最后一条消息，可能需要更新会话列表中的显示
    // 最简单的方法是刷新该会话的最后一条消息
    const conversation = chats.value.find(c => c.id === conversationId);
    
    if (conversation) {
      // 获取会话的最新消息
      messageApi.getMessages(conversationId, 0, 1)
        .then(response => {
          if (response.success && response.data && response.data.content && response.data.content.length > 0) {
            const latestMessage = response.data.content[0];
            
            // 确保latestMessage存在
            if (latestMessage) {
              // 更新会话的最后一条消息
              conversation.lastMessage = getLastMessageContent(latestMessage);
              
              // 如果有创建时间，更新最后消息时间
              if (latestMessage.createdAt) {
                conversation.lastMessageTime = latestMessage.createdAt;
              }
              
              // 触发视图更新
              chats.value = [...chats.value];
            }
          }
        })
        .catch(err => {
          console.warn(`获取会话 ${conversationId} 的最新消息失败:`, err);
        });
    }
  } catch (error) {
    console.error('处理消息撤回对会话列表的影响时出错:', error);
  }
}

// 处理群组更新
function handleGroupUpdate(data: any) {
  if (data && data.data && data.data.updateType === 'GROUP_DISSOLVED') {
    console.log('群组已解散，刷新会话列表');
    loadConversations(); // 刷新会话列表
    ElMessage.warning(`群组"${data.data.data?.groupName || ''}"已被解散`);
  }
}

// 更新现有会话
function updateExistingConversation(conversationId: number, conversationData: any) {
  // 查找会话
  const index = chats.value.findIndex(c => c.id === conversationId);
  
  if (index !== -1) {
    console.log(`更新现有会话 ${conversationId}`);
    
    // 保留原有的一些UI状态
    const originalIsPinned = chats.value[index].isPinned;
    
    // 更新会话数据
    chats.value[index] = {
      ...chats.value[index],
      ...mapConversationData(conversationData),
      isPinned: originalIsPinned // 保留原有的置顶状态
    };
    
    // 触发视图更新
    chats.value = [...chats.value];
  } else {
    console.log(`会话 ${conversationId} 不在当前列表中，尝试添加`);
    addNewConversation(conversationData);
  }
}

// 添加新会话
function addNewConversation(conversationData: any) {
  // 检查会话是否已存在
  const conversationId = conversationData.id;
  const existingIndex = chats.value.findIndex(c => c.id === conversationId);
  
  if (existingIndex !== -1) {
    console.log(`会话 ${conversationId} 已存在，更新现有会话`);
    updateExistingConversation(conversationId, conversationData);
    return;
  }
  
  // 映射会话数据
  const newConversation = mapConversationData(conversationData);
  
  console.log(`添加新会话 ${conversationId}`);
  
  // 添加到会话列表
  chats.value.push(newConversation);
  
  // 根据置顶状态和最后消息时间排序
  sortConversations();
  
  // 触发视图更新
  chats.value = [...chats.value];
}

// 删除会话
function removeConversation(conversationId: number) {
  console.log(`删除会话 ${conversationId}`);
  
  // 从列表中移除会话
  chats.value = chats.value.filter(c => c.id !== conversationId);
}

// 映射会话数据
function mapConversationData(conversationData: any) {
  // 尝试从多个位置获取最后消息时间
  let lastMessageTime = null;
  if (conversationData.lastMessage && conversationData.lastMessage.createdAt) {
    lastMessageTime = conversationData.lastMessage.createdAt;
  } else if (conversationData.lastActiveTime) {
    lastMessageTime = conversationData.lastActiveTime;
  } else if (conversationData.lastActiveAt) {
    lastMessageTime = conversationData.lastActiveAt;
  } else if (conversationData.updatedAt) {
    lastMessageTime = conversationData.updatedAt;
  } else if (conversationData.createdAt) {
    lastMessageTime = conversationData.createdAt;
  }
  
  // 确保时间戳是有效的
  if (!lastMessageTime) {
    // 使用当前时间作为默认值
    lastMessageTime = new Date().toISOString();
  }
  
  return {
    id: conversationData.id,
    name: getConversationDisplayName(conversationData),
    avatar: getConversationAvatar(conversationData),
    lastMessage: getLastMessageContent(conversationData.lastMessage),
    lastMessageTime: lastMessageTime,
    unreadCount: conversationData.unreadCount || 0,
    isPinned: conversationData.isPinned || false,
    isDnd: conversationData.isDnd || false,
    rawData: conversationData
  };
}

// 根据置顶状态和最后消息时间排序会话
function sortConversations() {
  // 分离置顶和非置顶会话
  const pinnedChats = chats.value.filter(c => c.isPinned);
  const nonPinnedChats = chats.value.filter(c => !c.isPinned);
  
  // 按最后消息时间排序（新消息在前）
  const sortByTime = (a: any, b: any) => {
    const timeA = new Date(a.lastMessageTime).getTime();
    const timeB = new Date(b.lastMessageTime).getTime();
    return timeB - timeA;
  };
  
  // 分别排序
  pinnedChats.sort(sortByTime);
  nonPinnedChats.sort(sortByTime);
  
  // 合并结果
  chats.value = [...pinnedChats, ...nonPinnedChats];
}

// 计算属性：筛选会话
const filteredChats = computed(() => {
  if (!searchKeyword.value) {
    return chats.value;
  }
  
  const keyword = searchKeyword.value.toLowerCase();
  return chats.value.filter(chat => 
    chat.name.toLowerCase().includes(keyword) || 
    (chat.lastMessage && chat.lastMessage.toLowerCase().includes(keyword))
  );
});

// 计算属性：置顶会话
const pinnedChats = computed(() => {
  return filteredChats.value.filter(chat => chat.isPinned);
});

// 计算属性：非置顶会话
const regularChats = computed(() => {
  return filteredChats.value.filter(chat => !chat.isPinned);
});

// 切换标签页
const switchTab = async (tab: 'regular' | 'archived') => {
  activeTab.value = tab;
  
  // 切换时重新加载对应类型的会话
  if (tab === 'regular') {
    await loadConversations();
  } else {
    await loadArchivedConversations();
  }
};

// 加载会话列表
const loadConversations = async (forceRefresh = false, markAsRead = false) => {
  console.log('[DEBUG] 开始加载会话列表，当前用户:', currentUser.value, '强制刷新:', forceRefresh, '标记为已读:', markAsRead);
  
  // 记录调用栈，帮助确定哪里调用了这个方法
  console.log('[DEBUG] loadConversations 调用栈:', new Error().stack);
  
  // 如果尝试标记为已读，记录警告
  if (markAsRead) {
    console.warn('[DEBUG] 警告：loadConversations 不再支持自动标记为已读功能，请在加载后单独调用 markConversationAsRead');
  }
  
  // 如果已有会话数据且不是强制刷新且WebSocket已连接，则不重新加载
  if (chats.value.length > 0 && !forceRefresh && wsConnected.value) {
    console.log('已有会话数据且WebSocket已连接，跳过HTTP加载，依赖WebSocket实时更新');
    return;
  }
  
  // 使用getCurrentUserId获取有效的用户ID
  let userId = getCurrentUserId();
  
  // 如果仍然无法获取用户ID，尝试从localStorage获取
  if (!userId) {
    try {
      const userInfoStr = localStorage.getItem('userInfo');
      if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr);
        console.log('从userInfo获取到的原始用户数据:', userInfo);
        if (userInfo && userInfo.id) {
          userId = Number(userInfo.id);
          console.log('从userInfo获取到用户ID:', userId);
        }
      }
    } catch (e) {
      console.error('解析userInfo失败:', e);
    }
  }
  
  // 如果仍然无法获取用户ID，使用硬编码的ID（仅用于调试）
  if (!userId) {
    userId = 1;
    console.warn('使用硬编码的用户ID (1) 进行调试');
  }
  
  console.log('使用用户ID加载会话:', userId);

  try {
    loading.value = true;
    error.value = null;
    
    const response = await messageApi.getConversations(userId, 0, 20);
    console.log('会话API原始响应:', response);
    
    if (response.success && response.data) {
      // 记录原始响应数据结构，帮助调试
      console.log('会话API响应结构:', JSON.stringify(response.data, null, 2));
      
      // 使用类型断言处理响应数据
      const responseData = response.data as any;
      
      // 尝试从不同的位置获取会话数据
      let conversationsData: any[] = [];
      
      // 检查各种可能的数据结构
      if (Array.isArray(responseData)) {
        conversationsData = responseData;
      } else if (responseData.content && Array.isArray(responseData.content)) {
        if (responseData.content.length > 0 && responseData.content[0].conversations) {
          conversationsData = responseData.content[0].conversations;
        } else {
          conversationsData = responseData.content;
        }
      } else if (responseData.conversations && Array.isArray(responseData.conversations)) {
        conversationsData = responseData.conversations;
      } else if (responseData.data && Array.isArray(responseData.data)) {
        conversationsData = responseData.data;
      }
      
      console.log('提取的会话数据:', conversationsData);
      
      // 如果有数据，则进行映射
      if (conversationsData && conversationsData.length > 0) {
        console.log('映射会话数据前的原始数据:', JSON.stringify(conversationsData, null, 2));
        
        // 使用mapConversationData函数映射会话数据
        chats.value = conversationsData.map((conv: any) => mapConversationData(conv));
        
        // 根据置顶状态和最后消息时间排序
        sortConversations();
        
        // 获取每个会话的最新消息，但不标记为已读
        // 完全分离加载会话和标记已读的逻辑
        await fetchLatestMessagesForConversations(false);
        
        // 如果原来的调用要求标记为已读，记录警告
        if (markAsRead) {
          console.warn('[DEBUG] 注意：已忽略markAsRead=true参数，需要标记为已读请单独调用markConversationAsRead');
        }
      } else {
        console.log('未找到会话数据');
        chats.value = [];
      }
    } else {
      throw new Error(response.message || '获取会话列表失败');
    }
  } catch (err: any) {
    error.value = err.message || '获取会话列表失败';
    emit('error', error.value);
    console.error('获取会话列表失败:', err);
    
    // 处理401错误
    if (err.status === 401) {
      emit('error', 'UNAUTHORIZED');
    }
  } finally {
    loading.value = false;
  }
};

// 加载已归档会话列表
const loadArchivedConversations = async () => {
  console.log('开始加载已归档会话列表');
  
  // 获取用户ID
  let userId = getCurrentUserId();
  
  if (!userId) {
    try {
      const userInfoStr = localStorage.getItem('userInfo');
      if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && userInfo.id) {
          userId = Number(userInfo.id);
        }
      }
    } catch (e) {
      console.error('解析userInfo失败:', e);
    }
  }
  
  if (!userId) {
    userId = 1; // 调试用
    console.warn('使用硬编码的用户ID (1) 进行调试');
  }
  
  try {
    loading.value = true;
    error.value = null;
    
    const response = await messageApi.getArchivedConversations(userId, 0, 20);
    console.log('已归档会话API响应:', response);
    
    if (response.success && response.data) {
      // 提取会话数据
      const responseData = response.data as any;
      let conversationsData: any[] = [];
      
      // 检查各种可能的数据结构
      if (Array.isArray(responseData)) {
        conversationsData = responseData;
      } else if (responseData.content && Array.isArray(responseData.content)) {
        if (responseData.content.length > 0 && responseData.content[0].conversations) {
          conversationsData = responseData.content[0].conversations;
        } else {
          conversationsData = responseData.content;
        }
      } else if (responseData.conversations && Array.isArray(responseData.conversations)) {
        conversationsData = responseData.conversations;
      } else if (responseData.data && Array.isArray(responseData.data)) {
        conversationsData = responseData.data;
      }
      
      // 如果有数据，则进行映射
      if (conversationsData && conversationsData.length > 0) {
        chats.value = conversationsData.map((conv: any) => {
          // 获取最后消息时间
          let lastMessageTime = null;
          if (conv.lastMessage && conv.lastMessage.createdAt) {
            lastMessageTime = conv.lastMessage.createdAt;
          } else if (conv.lastActiveTime) {
            lastMessageTime = conv.lastActiveTime;
          } else if (conv.lastActiveAt) {
            lastMessageTime = conv.lastActiveAt;
          } else if (conv.updatedAt) {
            lastMessageTime = conv.updatedAt;
          } else if (conv.createdAt) {
            lastMessageTime = conv.createdAt;
          }
          
          if (!lastMessageTime) {
            lastMessageTime = new Date().toISOString();
          }
          
          return {
            id: conv.id,
            name: getConversationDisplayName(conv),
            avatar: getConversationAvatar(conv),
            lastMessage: getLastMessageContent(conv.lastMessage),
            lastMessageTime: lastMessageTime,
            unreadCount: conv.unreadCount || 0,
            isPinned: false, // 归档会话不显示置顶状态
            isDnd: conv.isDnd || false,
            isArchived: true,
            rawData: conv
          };
        });
        
        // 获取每个会话的最新消息，但不标记为已读
        await fetchLatestMessagesForConversations(false);
      } else {
        console.log('未找到已归档会话数据');
        chats.value = [];
      }
    } else {
      throw new Error(response.message || '获取已归档会话列表失败');
    }
  } catch (err: any) {
    error.value = err.message || '获取已归档会话列表失败';
    emit('error', error.value);
    console.error('获取已归档会话列表失败:', err);
  } finally {
    loading.value = false;
  }
};

// 仅获取每个会话的最新消息（不标记已读）
const fetchLatestMessagesForConversations = async (markAsRead = false) => {
  if (!chats.value || chats.value.length === 0) return;
  
  console.log('[DEBUG] 开始获取所有会话的最新消息，标记为已读:', markAsRead);
  
  // 记录调用栈，帮助确定哪里调用了这个方法
  console.log('[DEBUG] fetchLatestMessagesForConversations 调用栈:', new Error().stack);
  
  // 如果尝试标记为已读，记录警告并拒绝
  if (markAsRead) {
    console.warn('[DEBUG] 警告：fetchLatestMessagesForConversations 不再支持标记为已读功能，请使用单独的 markConversationAsRead 方法');
    console.log('[DEBUG] 继续执行，但忽略 markAsRead 参数，只获取最新消息');
  }
  
  // 获取认证token
  const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
  if (!token) {
    console.warn('无法获取认证token，无法获取会话消息');
    return;
  }
  
  // 记录当前时间，用于跟踪执行时间
  const startTime = Date.now();
  
  // 遍历所有会话获取最新消息
  for (const chat of chats.value) {
    try {
      // 获取会话的最新消息，但不更新已读状态
      // 使用自定义请求头和URL参数确保不会自动标记为已读
      const response = await messageApi.getMessages(chat.id, 0, 1);
      
      // 记录API调用耗时
      const apiCallTime = Date.now() - startTime;
      console.log(`[DEBUG] getMessages API调用耗时: ${apiCallTime}ms`);
      
      if (response.success && response.data && response.data.content && response.data.content.length > 0) {
        const latestMessage = response.data.content[0];
        
        if (latestMessage) {
          // 更新会话的最新消息内容
          chat.lastMessage = getLastMessageContent(latestMessage);
          
          // 确保时间戳有效，再进行更新
          if (latestMessage.createdAt) {
            // 备份旧的时间戳
            const oldTimestamp = chat.lastMessageTime;
            
            // 更新为新的时间戳
            chat.lastMessageTime = latestMessage.createdAt || chat.lastMessageTime;
            
            console.log(
              `会话 ${chat.id} (${chat.name}) 时间戳已更新:`,
              `旧: ${oldTimestamp}`,
              `新: ${chat.lastMessageTime}`
            );
          }
          
          // 记录未读消息数量，但不自动标记为已读
          if (chat.unreadCount > 0) {
            console.log(`[DEBUG] 会话 ${chat.id} 有 ${chat.unreadCount} 条未读消息，但不自动标记为已读`);
          }
          
          // 确保消息不被标记为已读
          console.log(`[DEBUG] 消息 ${latestMessage.id} 的已读状态: ${latestMessage.isRead}`);
          if (latestMessage.isRead === true) {
            console.warn(`[DEBUG] 警告: 消息 ${latestMessage.id} 被错误地标记为已读，这不应该发生`);
            console.warn(`[DEBUG] 这可能是后端自动标记为已读的问题，请检查后端代码`);
          }
        }
      }
    } catch (err) {
      console.warn(`[DEBUG] 获取会话 ${chat.id} 的最新消息失败:`, err);
    }
  }
};

// 单独的标记会话为已读方法
const markConversationAsRead = async (conversationId: number, isUserAction = true) => {
  if (!isUserAction) {
    console.warn(`[DEBUG] 拒绝非用户主动操作的标记已读请求，会话ID: ${conversationId}`);
    return;
  }
  
  try {
    console.log(`[DEBUG] 开始标记会话 ${conversationId} 为已读，用户主动操作: ${isUserAction}`);
    
    // 获取会话的最新消息
    const response = await messageApi.getMessages(conversationId, 0, 1);
    
    if (response.success && response.data && response.data.content && response.data.content.length > 0) {
      const latestMessage = response.data.content[0];
      
      // 确保latestMessage存在且有id
      if (latestMessage && latestMessage.id) {
        console.log(`[DEBUG] 找到最新消息ID: ${latestMessage.id}，准备标记为已读`);
        
        // 使用isUserAction标志确保这是用户主动操作
        const request = {
          conversationId: conversationId,
          markAllAsRead: true,
          isUserAction: true, // 标记这是用户主动操作
          lastReadMessageId: latestMessage.id
        };
        
        const markAsReadResponse = await messageApi.markAsRead(request);
        console.log(`[DEBUG] 标记会话已读响应:`, markAsReadResponse);
        
        // 更新本地未读计数
        const chat = chats.value.find(c => c.id === conversationId);
        if (chat) {
          chat.unreadCount = 0;
          console.log(`[DEBUG] 已将会话 ${conversationId} 的未读计数重置为0`);
        }
        
        return true;
      } else {
        console.warn(`[DEBUG] 无法获取会话 ${conversationId} 的最新消息ID，无法标记为已读`);
      }
    } else {
      console.warn(`[DEBUG] 获取会话 ${conversationId} 的消息失败，无法标记为已读`);
    }
  } catch (error) {
    console.error(`[DEBUG] 标记会话 ${conversationId} 为已读失败:`, error);
  }
  
  return false;
};

// 获取会话显示名称
const getConversationDisplayName = (conversation: any): string => {
  if (!conversation) return '未知会话';
  
  // 获取当前用户ID
  const currentUserId = getCurrentUserId();
  
  if (conversation.type === 'PRIVATE') {
    // 私聊：查找对方参与者信息
    
    // 检查participants是否存在，如果不存在，尝试其他属性
    if (!conversation.participants || conversation.participants.length === 0) {
      // 尝试从name属性获取
      if (conversation.name && conversation.name !== 'PRIVATE') {
        return conversation.name;
      }
      
      // 如果有用户信息，显示用户昵称
      if (conversation.user?.nickname) {
        return conversation.user.nickname;
      }
      
      // 尝试其他可能的属性
      if (conversation.displayName) {
        return conversation.displayName;
      }
      
      // 如果都没有，使用会话ID
      return `会话${conversation.id}`;
    }
    
    // 从participants中获取
    const otherParticipant = conversation.participants.find((p: any) => p.userId !== currentUserId);
    
    if (otherParticipant) {
      // 优先使用备注名，然后是用户昵称，最后是用户名
      if (otherParticipant.alias) {
        return otherParticipant.alias;
      }
      
      // 检查user对象中的信息
      if (otherParticipant.user) {
        if (otherParticipant.user.nickname) {
          return otherParticipant.user.nickname;
        }
        if (otherParticipant.user.username) {
          return otherParticipant.user.username;
        }
        if (otherParticipant.user.email) {
          return otherParticipant.user.email;
        }
      }
      
      // 检查参与者自身的属性
      if (otherParticipant.nickname) {
        return otherParticipant.nickname;
      }
      if (otherParticipant.username) {
        return otherParticipant.username;
      }
      if (otherParticipant.email) {
        return otherParticipant.email;
      }
      
      // 如果什么都没有，显示用户ID
      return `用户${otherParticipant.userId}`;
    }
    return '私聊';
  } else {
    // 群聊：使用会话名称
    return conversation.name || '群聊';
  }
};

// 获取会话头像
const getConversationAvatar = (conversation: any): string => {
  if (!conversation) return '';
  
  // 获取当前用户ID
  const currentUserId = getCurrentUserId();
  
  if (conversation.type === 'PRIVATE') {
    // 私聊：使用对方用户的头像
    const otherParticipant = conversation.participants?.find((p: any) => p.userId !== currentUserId);
    if (otherParticipant?.avatarUrl) {
      return otherParticipant.avatarUrl;
    }
    // 如果没有头像，返回空字符串，让组件显示默认头像
    return '';
  } else {
    // 群聊：使用会话头像
    return conversation.avatarUrl || '';
  }
};

// 获取最后一条消息内容
const getLastMessageContent = (lastMessage: any): string => {
  if (!lastMessage) {
    return '暂无消息';
  }
  
  // 根据消息类型显示不同内容
  switch (lastMessage.messageType || lastMessage.type) {
    case 'TEXT':
      return lastMessage.content || '';
    case 'IMAGE':
      return '[图片]';
    case 'FILE':
      return '[文件]';
    case 'AUDIO':
    case 'VOICE':
      return '[语音]';
    case 'VIDEO':
      return '[视频]';
    default:
      return lastMessage.content || '[消息]';
  }
};

// 处理搜索
const handleSearch = () => {
  // 这里不需要额外处理，因为已经有计算属性filteredChats
};

// 显示上下文菜单
const showContextMenu = (event: MouseEvent, chat: any) => {
  event.preventDefault();
  
  // 设置菜单位置
  menuPos.value = {
    x: event.clientX,
    y: event.clientY
  };
  
  // 设置选中的会话
  selectedChat.value = chat;
  
  // 显示菜单
  showMenu.value = true;
  
  // 点击其他区域关闭菜单
  const handleClickOutside = () => {
    showMenu.value = false;
    document.removeEventListener('click', handleClickOutside);
  };
  
  // 延迟添加事件监听，避免立即触发
  setTimeout(() => {
    document.addEventListener('click', handleClickOutside);
  }, 0);
};

// 处理置顶/取消置顶
const handlePin = async () => {
  if (!selectedChat.value) return;
  
  try {
    const chatId = selectedChat.value.id;
    const isPinned = !selectedChat.value.isPinned;
    
    // 显示加载状态
    const actionText = isPinned ? '置顶' : '取消置顶';
    console.log(`正在${actionText}会话 ${chatId}...`);
    
    // 调用API
    const response = await messageApi.pinConversation(chatId, isPinned);
    
    if (response.success) {
      console.log(`会话${actionText}成功: ${chatId}`);
    
      // 更新本地数据
      const chat = chats.value.find(c => c.id === chatId);
      if (chat) {
        chat.isPinned = isPinned;
        
        // 如果是原始数据中的对象也需要更新
        if (chat.rawData) {
          chat.rawData.isPinned = isPinned;
        }
        
        // 重新排序会话列表
        sortConversations();
        
        // 通知父组件
        emit('pin-chat', { chatId, isPinned });
      }
    } else {
      console.error(`会话${actionText}失败:`, response.message);
      emit('error', response.message || `${actionText}失败`);
    }
  } catch (err: any) {
    console.error('置顶/取消置顶会话失败:', err);
    emit('error', err.message || '操作失败');
  } finally {
    showMenu.value = false;
  }
};

// 处理免打扰/取消免打扰
const handleMute = async () => {
  if (!selectedChat.value) return;
  
  try {
    const chatId = selectedChat.value.id;
    const isDnd = selectedChat.value.isDnd;
    
    // 调用API
    await messageApi.muteConversation(chatId, isDnd);
    
    // 更新本地数据
    const chat = chats.value.find(c => c.id === chatId);
    if (chat) {
      chat.isDnd = isDnd;
      
      // 同时更新selectedChat的状态
      selectedChat.value.isDnd = isDnd;
    }
    
    // 强制重新渲染会话列表
    chats.value = [...chats.value];
    
    // 通知父组件
    emit('mute-chat', { chatId, isDnd });
  } catch (err: any) {
    console.error('设置免打扰失败:', err);
    emit('error', err.message || '操作失败');
  } finally {
    showMenu.value = false;
  }
};

// 处理归档/取消归档
const handleArchive = async () => {
  if (!selectedChat.value) return;
  
  try {
    const chatId = selectedChat.value.id;
    const isArchived = activeTab.value !== 'archived'; // 如果当前在归档标签页，则是取消归档
    
    console.log(`执行${isArchived ? '归档' : '取消归档'}操作, 会话ID: ${chatId}`);
    
    // 调用API
    const response = await messageApi.archiveConversation(chatId, isArchived);
    
    if (response.success) {
    // 从列表中移除
    chats.value = chats.value.filter(c => c.id !== chatId);
      
      // 更新选中会话的isArchived状态，然后传递给父组件
      const updatedChat = { ...selectedChat.value, isArchived: isArchived };
    
    // 通知父组件
      emit('archive-chat', updatedChat);
      
      // 如果是归档操作，可以显示提示
      if (isArchived) {
        console.log(`会话 ${chatId} 已归档`);
      } else {
        console.log(`会话 ${chatId} 已取消归档`);
        // 如果是在已归档列表中取消归档，可以刷新已归档列表
        if (activeTab.value === 'archived') {
          await loadArchivedConversations();
        }
      }
    } else {
      console.error('归档/取消归档会话失败:', response.message);
      emit('error', response.message || '操作失败');
    }
  } catch (err: any) {
    console.error('归档/取消归档会话失败:', err);
    emit('error', err.message || '操作失败');
  } finally {
    showMenu.value = false;
  }
};

// 处理删除


// 选择会话
const handleSelectChat = async (chat: any) => {
  console.log('[DEBUG] 选择会话:', chat.id);
  
  try {
    // 发出选择会话事件
    emit('select-chat', chat);
    
    // 只有当用户点击会话时才标记为已读，这里是用户主动点击
    if (chat.unreadCount > 0) {
      console.log(`[DEBUG] 用户点击会话 ${chat.id}，有 ${chat.unreadCount} 条未读消息，标记为已读`);
      
      // 保存原始未读计数，用于恢复（如果API调用失败）
      const originalUnreadCount = chat.unreadCount;
      
      // 立即更新未读计数（乐观更新）
      chat.unreadCount = 0;
      // 强制更新视图
      chats.value = [...chats.value];
      
      try {
        // 使用单独的标记为已读方法，明确传递isUserAction=true
        const success = await markConversationAsRead(chat.id, true);
        
        if (success) {
          console.log(`[DEBUG] 会话 ${chat.id} 已成功标记为已读`);
          // 再次确保未读计数为0
          chat.unreadCount = 0;
          // 再次强制更新视图
          chats.value = [...chats.value];
        } else {
          console.warn(`[DEBUG] 标记会话 ${chat.id} 为已读失败`);
          // 如果标记失败，恢复原始未读计数
          chat.unreadCount = originalUnreadCount;
          // 强制更新视图
          chats.value = [...chats.value];
        }
      } catch (apiError) {
        console.error('[DEBUG] API调用失败，恢复原始未读计数:', apiError);
        // 如果API调用失败，恢复原始未读计数
        chat.unreadCount = originalUnreadCount;
        // 强制更新视图
        chats.value = [...chats.value];
      }
    }
  } catch (error) {
    console.error('[DEBUG] 处理会话选择失败:', error);
  }
};

// 生命周期钩子
onMounted(async () => {
  // 加载会话列表
  await loadConversations();
  
  // 不再需要定时刷新，依赖WebSocket实时更新
  console.log('ConversationsPanel: 使用WebSocket实时更新，无需定时轮询');
});

// 定期刷新未读消息计数
let unreadCountRefreshInterval: number | null = null;

// 启动定期刷新未读消息计数
const startUnreadCountRefresh = () => {
  if (unreadCountRefreshInterval) return;
  
  console.log('启动定期刷新未读消息计数');
  
  unreadCountRefreshInterval = window.setInterval(async () => {
    // 只有在用户已登录时才刷新
    if (!getCurrentUserId()) return;
    
    try {
      // 记录调用栈，帮助确定是哪里触发了定时刷新
      console.log('[DEBUG] 30秒定时刷新调用栈:', new Error().stack);
      
      // 遍历当前会话列表，更新每个会话的未读消息计数
      for (const chat of chats.value) {
        if (chat.id === Number(props.activeChatId)) continue; // 跳过当前活跃会话
        
        // 获取未读消息数量，但不更新已读状态
        // messageApi.getUnreadMessageCount 已添加 preventReadUpdate=true 参数
        const response = await messageApi.getUnreadMessageCount(chat.id);
        
        if (response.success && typeof response.data === 'number') {
          // 记录当前值和新值，用于调试
          const currentCount = chat.unreadCount;
          const newCount = response.data;
          
          console.log(`[DEBUG] 会话 ${chat.id} 未读计数: 当前=${currentCount}, 后端=${newCount}`);
          
          // 只有当未读计数不同时才更新
          if (currentCount !== newCount) {
            console.log(`[DEBUG] 更新会话 ${chat.id} 的未读消息计数: ${currentCount} -> ${newCount}`);
            
            // 更新未读计数
            chat.unreadCount = newCount;
            
            // 强制更新视图
            chats.value = [...chats.value];
          }
        }
      }
      
      // 完全禁止在定时刷新中获取最新消息
      // 这里不再调用 fetchLatestMessagesForConversations
      console.log('[DEBUG] 定时刷新只更新未读计数，不获取任何消息内容，避免触发已读状态更新');
      
    } catch (error) {
      console.error('刷新未读消息计数失败:', error);
    }
  }, 30000); // 每30秒刷新一次
};

// 停止定期刷新未读消息计数
const stopUnreadCountRefresh = () => {
  if (unreadCountRefreshInterval) {
    console.log('停止定期刷新未读消息计数');
    clearInterval(unreadCountRefreshInterval);
    unreadCountRefreshInterval = null;
  }
};

// 在组件挂载时启动定期刷新
onMounted(() => {
  startUnreadCountRefresh();
});

// 在组件卸载时停止定期刷新
onUnmounted(() => {
  stopUnreadCountRefresh();
});

// 刷新会话列表
const refreshConversations = async () => {
  if (isRefreshing.value) return; // 防止重复点击
  
  isRefreshing.value = true;
  
  try {
    console.log('[DEBUG] 手动刷新会话列表');
    await loadConversations(true); // 强制刷新
    ElMessage.success('会话列表已刷新');
  } catch (error) {
    console.error('刷新会话列表失败:', error);
    ElMessage.error('刷新失败，请重试');
  } finally {
    isRefreshing.value = false;
    
    // 添加一个短暂的延迟，避免用户连续点击
    setTimeout(() => {
      isRefreshing.value = false;
    }, 500);
  }
};

// 暴露方法给父组件
defineExpose({
  loadConversations,
  loadArchivedConversations,
  markConversationAsRead,
  refreshConversations,
  chats
});
</script>

<style scoped>
.conversations-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f9f9f9;
  border-right: 1px solid #e0e0e0;
}

.search-bar {
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  background-color: #f9f9f9;
}

.search-input-container {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  color: #aaa;
  font-size: 12px;
}

.search-input {
  flex: 1;
  padding: 8px 12px 8px 30px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  background-color: white;
}

.refresh-button {
  min-width: 60px;
  height: 32px;
  padding: 0 8px;
  background-color: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8px;
  cursor: pointer;
  color: #333;
  transition: all 0.2s;
  font-size: 14px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.refresh-text {
  margin-left: 4px;
  font-size: 12px;
}

.refresh-button:hover {
  background-color: #e6f7ff;
  color: #1890ff;
  border-color: #1890ff;
}

.refresh-button:active {
  background-color: #dcf5ff;
  transform: scale(0.95);
}

.refresh-button.refreshing {
  background-color: #e6f7ff;
  color: #1890ff;
  border-color: #1890ff;
  pointer-events: none; /* 防止重复点击 */
}

.refresh-button.refreshing i {
  animation: spin 1s linear infinite;
}

/* 添加一个高亮效果 */
@keyframes highlight {
  0% { box-shadow: 0 0 0 0 rgba(24, 144, 255, 0.4); }
  70% { box-shadow: 0 0 0 8px rgba(24, 144, 255, 0); }
  100% { box-shadow: 0 0 0 0 rgba(24, 144, 255, 0); }
}

/* 页面加载时添加高亮动画 */
.refresh-button {
  animation: highlight 2s ease-out 1s;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 会话类型切换标签 */
.conversation-tabs {
  display: flex;
  border-bottom: 1px solid #e0e0e0;
}

.tab {
  flex: 1;
  padding: 10px;
  text-align: center;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}

.tab:hover {
  background-color: #f5f5f5;
}

.tab.active {
  color: #1890ff;
  font-weight: 500;
  border-bottom: 2px solid #1890ff;
}

.tab i {
  margin-right: 6px;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 16px;
}

.section-header {
  padding: 8px 16px;
  font-size: 13px;
  color: #666;
  background-color: #f0f0f0;
  font-weight: 500;
  display: flex;
  align-items: center;
}

.section-header i {
  margin-right: 6px;
  font-size: 12px;
}

.pinned-section .section-header {
  color: #1890ff;
  background-color: #e6f7ff;
  border-bottom: 1px solid #91d5ff;
  font-weight: 600;
}

.pinned-section .section-header i {
  transform: rotate(45deg);
  color: #1890ff;
  font-size: 14px;
}

.pinned-section {
  margin-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

/* 归档会话样式 */
.archived-header {
  color: #722ed1;
  background-color: #f9f0ff;
  border-bottom: 1px solid #d3adf7;
  font-weight: 600;
}

.archived-header i {
  color: #722ed1;
}

.chats-loading,
.no-chats {
  padding: 24px;
  text-align: center;
  color: #999;
}

/* 上下文菜单样式 */
.context-menu {
  position: fixed;
  z-index: 1000;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  min-width: 150px;
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.menu-item:hover {
  background-color: #f5f5f5;
}
</style>