<template>
  <div class="conversations-panel">
    <div class="search-bar">
      <input 
        v-model="searchKeyword"
        type="text" 
        placeholder="搜索会话..." 
        class="search-input"
        @input="handleSearch"
      />
    </div>
    
    <div class="chat-list">
      <div v-if="loading && chats.length === 0" class="chats-loading">
        加载中...
      </div>
      <div v-else-if="filteredChats.length === 0 && !loading" class="no-chats">
        {{ searchKeyword ? '未找到匹配的会话' : '暂无会话' }}
      </div>
      
      <!-- 置顶会话 -->
      <div v-if="pinnedChats.length > 0" class="pinned-section">
        <div class="section-header">置顶</div>
        <conversation-item
          v-for="chat in pinnedChats" 
          :key="chat.id"
          :chat="chat"
          :active-chat-id="props.activeChatId"
          @click="$emit('select-chat', chat)"
          @context-menu="showContextMenu($event, chat)"
        />
      </div>

      <!-- 普通会话 -->
      <div v-if="regularChats.length > 0" class="regular-section">
        <div v-if="pinnedChats.length > 0" class="section-header">全部会话</div>
        <conversation-item
          v-for="chat in regularChats" 
          :key="chat.id"
          :chat="chat"
          :active-chat-id="props.activeChatId"
          @click="$emit('select-chat', chat)"
          @context-menu="showContextMenu($event, chat)"
        />
      </div>
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
        归档会话
      </div>
      <div class="menu-item delete" @click="handleDelete">
        删除会话
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { useAuth } from '@/composables/useAuth';
import { messageApi } from '@/api/message';
import ConversationItem from './ConversationItem.vue';
import { getCurrentUserId } from '@/utils/helpers';

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

const emit = defineEmits(['select-chat', 'pin-chat', 'mute-chat', 'archive-chat', 'delete-chat', 'error']);

// 会话数据
const chats = ref<any[]>([]);
const loading = ref(false);
const searchKeyword = ref('');
const error = ref<string | null>(null);

// 上下文菜单相关
const showMenu = ref(false);
const menuPos = ref({ x: 0, y: 0 });
const selectedChat = ref<any>(null);

// 获取当前用户信息
const { currentUser } = useAuth();

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

// 加载会话列表
const loadConversations = async () => {
  console.log('开始加载会话列表，当前用户:', currentUser.value);
  
  // 使用getCurrentUserId获取有效的用户ID
  let userId = getCurrentUserId();
  
  // 如果仍然无法获取用户ID，尝试从URL或其他地方获取
  if (!userId) {
    // 尝试直接从localStorage获取原始用户信息
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
    // 这里使用1作为默认ID，仅用于调试目的
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
        // 如果直接是数组
        conversationsData = responseData;
      } else if (responseData.content && Array.isArray(responseData.content)) {
        // 如果是分页结构中的content
        if (responseData.content.length > 0 && responseData.content[0].conversations) {
          // 如果content中的第一个元素包含conversations数组
          conversationsData = responseData.content[0].conversations;
        } else {
          // 否则直接使用content
        conversationsData = responseData.content;
        }
      } else if (responseData.conversations && Array.isArray(responseData.conversations)) {
        // 如果有conversations字段
        conversationsData = responseData.conversations;
      } else if (responseData.data && Array.isArray(responseData.data)) {
        // 如果有data字段
        conversationsData = responseData.data;
      }
      
      console.log('提取的会话数据:', conversationsData);
      
      // 如果有数据，则进行映射
      if (conversationsData && conversationsData.length > 0) {
        chats.value = conversationsData.map((conv: any) => ({
          id: conv.id,
          name: getConversationDisplayName(conv),
          avatar: getConversationAvatar(conv),
          lastMessage: getLastMessageContent(conv.lastMessage),
          lastMessageTime: conv.lastActiveTime || conv.lastActiveAt,
          unreadCount: conv.unreadCount || 0,
          isPinned: conv.isPinned || false,
          isDnd: conv.isDnd || false,
          rawData: conv // 保存原始数据，以便后续操作
        }));
        
        // 获取每个会话的最新消息
        await fetchLatestMessagesForConversations();
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
      // 通知上层组件处理登录问题
      emit('error', 'UNAUTHORIZED');
    }
  } finally {
    loading.value = false;
  }
};

// 获取每个会话的最新消息
const fetchLatestMessagesForConversations = async () => {
  if (!chats.value || chats.value.length === 0) return;
  
  console.log('开始获取所有会话的最新消息');
  
  // 获取认证token
  const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
  if (!token) {
    console.warn('无法获取认证token，无法获取会话消息');
    return;
  }
  
  // 为每个会话获取最新消息
  for (const chat of chats.value) {
    try {
      console.log(`获取会话 ${chat.id} 的最新消息`);
      const response = await fetch(`/api/messages/conversation/${chat.id}?page=0&size=1`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        console.warn(`获取会话 ${chat.id} 的消息失败: ${response.status}`);
        continue;
      }
      
      const data = await response.json();
      console.log(`会话 ${chat.id} 的消息响应:`, data);
      
      if (data.success && data.data && data.data.content && data.data.content.length > 0) {
        const latestMessage = data.data.content[0];
        console.log(`会话 ${chat.id} 的最新消息:`, latestMessage);
        
        // 更新会话的最新消息
        chat.lastMessage = getLastMessageContent({
          messageType: latestMessage.messageType || latestMessage.type,
          content: latestMessage.content
        });
        
        // 更新最后消息时间
        if (latestMessage.createdAt) {
          chat.lastMessageTime = latestMessage.createdAt;
        }
        
        console.log(`更新会话 ${chat.id} 的最新消息为: ${chat.lastMessage}`);
      }
    } catch (error) {
      console.error(`获取会话 ${chat.id} 的最新消息失败:`, error);
    }
  }
  
  console.log('所有会话的最新消息获取完成');
};

// 获取会话显示名称
const getConversationDisplayName = (conversation: any): string => {
  if (conversation.type === 'PRIVATE') {
    // 获取当前用户ID
    let currentUserId = getCurrentUserId();
    
    // 如果无法获取用户ID，使用硬编码的ID
    if (!currentUserId) {
      currentUserId = 1; // 调试用
    }
    
    // 私聊：查找对方参与者信息
    const otherParticipant = conversation.participants?.find((p: any) => 
      Number(p.userId) !== currentUserId
    );
    
    if (otherParticipant) {
      // 优先使用备注名（alias），然后是用户昵称，最后是邮箱
      if (otherParticipant.alias) {
        return otherParticipant.alias;
      }
      if (otherParticipant.user && otherParticipant.user.nickname) {
        return otherParticipant.user.nickname;
      }
      if (otherParticipant.user && otherParticipant.user.email) {
        return otherParticipant.user.email;
      }
      // 如果都没有，使用用户ID
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
  if (conversation.avatarUrl) {
    return conversation.avatarUrl;
  }
  
  if (conversation.type === 'PRIVATE') {
    // 获取当前用户ID
    let currentUserId = getCurrentUserId();
    
    // 如果无法获取用户ID，使用硬编码的ID
    if (!currentUserId) {
      currentUserId = 1; // 调试用
    }
    
    const otherParticipant = conversation.participants?.find((p: any) => 
      Number(p.userId) !== currentUserId
    );
    
    if (otherParticipant?.user?.avatarUrl) {
      return otherParticipant.user.avatarUrl;
    }
  }
  
  return ''; // 返回空字符串，组件会显示文字头像
};

// 获取消息内容摘要
const getLastMessageContent = (message: any): string => {
  if (!message) {
    return '暂无消息';
  }
  
  // 检查消息结构，处理可能的嵌套结构
  const messageType = message.messageType || message.type;
  const content = message.content;
  
  // 记录消息结构以便调试
  console.log('消息结构:', message);
  
  switch (messageType) {
    case 'TEXT':
      return content || '';
    case 'IMAGE':
      return '[图片]';
    case 'FILE':
      return '[文件]';
    case 'VOICE':
    case 'AUDIO':
      return '[语音]';
    case 'VIDEO':
      return '[视频]';
    case 'LOCATION':
      return '[位置]';
    case 'SYSTEM':
      return '[系统消息]';
    default:
      // 如果有内容但类型不明确，直接显示内容
      if (content) {
        return content;
      }
      return '[消息]';
  }
};

// 处理搜索
const handleSearch = () => {
  // 前端搜索已通过计算属性实现
};

// 显示上下文菜单
const showContextMenu = (event: MouseEvent, chat: any) => {
  event.preventDefault();
  selectedChat.value = chat;
  menuPos.value = {
    x: event.clientX,
    y: event.clientY
  };
  showMenu.value = true;
  
  // 点击其他地方关闭菜单
  nextTick(() => {
    document.addEventListener('click', closeMenu, { once: true });
  });
};

// 关闭上下文菜单
const closeMenu = () => {
  showMenu.value = false;
};

// 处理置顶/取消置顶
const handlePin = async () => {
  if (!selectedChat.value) return;
  
  try {
    emit('pin-chat', selectedChat.value);
    closeMenu();
  } catch (err: any) {
    error.value = err.message || '操作失败';
    emit('error', error.value);
  }
};

// 处理免打扰
const handleMute = () => {
  if (!selectedChat.value) return;
  
  emit('mute-chat', selectedChat.value);
  closeMenu();
};

// 处理归档
const handleArchive = () => {
  if (!selectedChat.value) return;
  
  emit('archive-chat', selectedChat.value);
  closeMenu();
};

// 处理删除
const handleDelete = () => {
  if (!selectedChat.value) return;
  
  if (confirm(`确定要删除与"${selectedChat.value.name}"的会话吗？`)) {
    emit('delete-chat', selectedChat.value);
    closeMenu();
  }
};

// 组件挂载时加载会话列表
onMounted(() => {
  loadConversations();
});

// 监听当前用户变化，重新加载会话列表
watch(() => currentUser.value?.id, (newVal, oldVal) => {
  if (newVal !== oldVal && newVal) {
    loadConversations();
  }
});

// 导出方法给父组件
defineExpose({
  loadConversations,
  chats
});
</script>

<style scoped>
.conversations-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.search-bar {
  padding: 12px;
  border-bottom: 1px solid #e6e6e6;
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #007bff;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.section-header {
  padding: 8px 16px;
  font-size: 12px;
  color: #999;
  background-color: #f5f5f5;
}

.chats-loading,
.no-chats {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100px;
  color: #999;
  font-size: 14px;
}

.context-menu {
  position: fixed;
  z-index: 1000;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  min-width: 150px;
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.menu-item:hover {
  background-color: #f0f0f0;
}

.menu-item.delete {
  color: #ff4d4f;
}

.menu-item.delete:hover {
  background-color: #fff1f0;
}
</style> 