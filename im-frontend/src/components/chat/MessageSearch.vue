<template>
  <div class="message-search" :class="{ 'is-active': isActive }">
    <!-- 搜索头部 -->
    <div class="search-header">
      <button class="back-btn" @click="close">
        <i class="fas fa-arrow-right"></i>
        <span class="back-text">返回</span>
      </button>
      <div class="search-input-container">
        <input
          ref="searchInputRef"
          v-model="searchKeyword"
          class="search-input"
          type="text"
          placeholder="搜索消息..."
          @keyup.enter="handleEnterKey"
        />
        <button v-if="searchKeyword" class="clear-btn" @click="clearSearch">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <button class="search-btn" @click="handleSearchClick" :disabled="loading || !searchKeyword.trim()">
        <i class="fas fa-search"></i>
      </button>
    </div>

    <!-- 搜索结果或聊天历史记录 -->
    <div class="search-results">
      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>搜索中...</p>
      </div>
      
      <!-- 错误信息 -->
      <div v-else-if="error" class="error-container">
        <i class="fas fa-exclamation-circle"></i>
        <p>{{ error }}</p>
        <button class="retry-btn" @click="handleRetryClick">重试</button>
      </div>
      
      <!-- 搜索结果为空 -->
      <div v-else-if="hasSearched && (!searchResults || searchResults.length === 0)" class="empty-container">
        <i class="fas fa-search"></i>
        <p>未找到匹配的消息</p>
      </div>
      
      <!-- 搜索结果列表 -->
      <template v-else-if="hasSearched && searchResults && searchResults.length > 0">
        <div class="search-result-count">
          找到 {{ totalElements }} 条匹配消息
        </div>
        
        <div class="search-result-list">
          <div 
            v-for="result in searchResults" 
            :key="result.message.id" 
            class="search-result-item"
            @click="jumpToMessage(result.message)"
          >
            <div class="result-sender">
              <img :src="getSenderAvatarSync(result)" alt="avatar" class="sender-avatar" />
              <span class="sender-name">{{ getSenderNameSync(result) }}</span>
              <span class="result-time">{{ formatTime(result.message.createdAt) }}</span>
            </div>
            
            <div class="result-content" v-if="result.highlights && result.highlights.content && result.highlights.content.length > 0">
              <div v-html="result.highlights.content[0]"></div>
            </div>
            <div class="result-content" v-else>
              {{ result.message.content }}
            </div>
          </div>
        </div>
        
        <!-- 分页控制 -->
        <div v-if="totalPages > 1" class="pagination">
          <button 
            class="pagination-btn"
            :disabled="currentPage <= 0"
            @click="handlePrevPage"
          >
            <i class="fas fa-chevron-left"></i>
          </button>
          <span class="pagination-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button 
            class="pagination-btn"
            :disabled="currentPage >= totalPages - 1"
            @click="handleNextPage"
          >
            <i class="fas fa-chevron-right"></i>
          </button>
        </div>
      </template>

      <!-- 未搜索或搜索关键词为空时显示聊天历史记录 -->
      <template v-else>
        <div class="history-header">
          <h3>聊天历史记录</h3>
        </div>
        <div class="history-list">
          <div v-if="chatHistory.length === 0" class="empty-history">
            <i class="fas fa-history"></i>
            <p>暂无聊天记录</p>
          </div>
          <div 
            v-for="message in chatHistory" 
            :key="message.id" 
            class="history-item"
            @click="jumpToMessage(message)"
          >
            <div class="history-sender">
              <img :src="message.senderAvatar || '/favicon.ico'" alt="avatar" class="sender-avatar" />
              <span class="sender-name">{{ message.senderName }}</span>
              <span class="history-time">{{ formatTime(message.createdAt) }}</span>
            </div>
            <div class="history-content">
              <!-- 根据消息类型显示不同内容 -->
              <span v-if="message.type === 'TEXT'">{{ message.content }}</span>
              <span v-else-if="message.type === 'IMAGE'">[图片消息]</span>
              <span v-else-if="message.type === 'FILE'">[文件: {{ message.fileName || '未命名文件' }}]</span>
              <span v-else-if="message.type === 'VOICE'">[语音消息]</span>
              <span v-else-if="message.type === 'VIDEO'">[视频消息]</span>
              <span v-else>[{{ message.type }}]</span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { messageApi, type MessageSearchRequest, type MessageSearchResult } from '@/api/message';

const props = defineProps<{
  conversationId: string;
  isActive: boolean;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'jump-to-message', messageId: number): void;
}>();

// 状态
const searchInputRef = ref<HTMLInputElement | null>(null);
const searchKeyword = ref('');
const searchResults = ref<MessageSearchResult[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const hasSearched = ref(false);
const currentPage = ref(0);
const totalPages = ref(0);
const totalElements = ref(0);
const pageSize = ref(10);
const chatHistory = ref<any[]>([]);

// 用户信息缓存
const userCache = ref<Record<number, any>>({});
const userCacheService = {
  // 获取用户信息
  async getUserInfo(userId: number): Promise<any> {
    // 如果缓存中已有该用户信息，直接返回
    if (userCache.value[userId]) {
      return userCache.value[userId];
    }
    
    // 检查是否是当前用户
    const currentUserId = getCurrentUserId();
    if (userId === currentUserId) {
      return this.loadCurrentUserInfo();
    }
    
    // 尝试从会话参与者获取用户信息
    const userFromConversation = await this.getUserFromConversation(userId);
    if (userFromConversation) {
      return userFromConversation;
    }
    
    // 尝试从联系人列表获取用户信息
    const contactInfo = await this.getContactInfo(userId);
    if (contactInfo) {
      return contactInfo;
    }
    
    // 如果都没有获取到，创建默认用户信息
    return this.createDefaultUserInfo(userId);
  },
  
  // 加载当前用户信息
  async loadCurrentUserInfo(): Promise<any> {
    const currentUserId = getCurrentUserId();
    
    // 尝试从会话参与者获取当前用户信息
    try {
      const conversationId = props.conversationId;
      if (conversationId) {
        const response = await messageApi.getConversation(Number(conversationId), currentUserId);
        
        if (response.success && response.data && response.data.participants) {
          const currentParticipant = response.data.participants.find(p => p.userId === currentUserId);
          if (currentParticipant) {
            const userInfo = {
              id: currentUserId,
              nickname: currentParticipant.nickname,
              username: currentParticipant.username,
              avatarUrl: currentParticipant.avatarUrl
            };
            
            // 缓存当前用户信息
            userCache.value[currentUserId] = userInfo;
            console.log(`从会话参与者中获取当前用户信息: ID=${currentUserId}, 昵称=${userInfo.nickname}, 头像=${userInfo.avatarUrl}`);
            return userInfo;
          }
        }
      }
    } catch (error) {
      console.error('从会话参与者获取当前用户信息失败:', error);
    }
    
    // 尝试从本地存储获取用户信息
    try {
      // 从userInfo获取
      const userInfoStr = localStorage.getItem('userInfo');
      if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo) {
          const currentUserInfo = {
            id: currentUserId,
            nickname: userInfo.nickname || userInfo.name,
            username: userInfo.username || userInfo.email,
            avatarUrl: userInfo.avatarUrl || userInfo.avatar
          };
          
          // 缓存当前用户信息
          userCache.value[currentUserId] = currentUserInfo;
          console.log(`从localStorage缓存当前用户信息: ID=${currentUserId}, 昵称=${currentUserInfo.nickname}, 头像=${currentUserInfo.avatarUrl}`);
          return currentUserInfo;
        }
      }
      
      // 尝试从current_user获取
      const currentUserStr = localStorage.getItem('current_user');
      if (currentUserStr) {
        const currentUser = JSON.parse(currentUserStr);
        if (currentUser) {
          const currentUserInfo = {
            id: currentUserId,
            nickname: currentUser.nickname || currentUser.name,
            username: currentUser.username || currentUser.email,
            avatarUrl: currentUser.avatarUrl || currentUser.avatar
          };
          
          // 缓存当前用户信息
          userCache.value[currentUserId] = currentUserInfo;
          console.log(`从current_user缓存当前用户信息: ID=${currentUserId}, 昵称=${currentUserInfo.nickname}, 头像=${currentUserInfo.avatarUrl}`);
          return currentUserInfo;
        }
      }
      
      // 尝试从sessionStorage获取
      const sessionUserInfoStr = sessionStorage.getItem('userInfo');
      if (sessionUserInfoStr) {
        const userInfo = JSON.parse(sessionUserInfoStr);
        if (userInfo) {
          const currentUserInfo = {
            id: currentUserId,
            nickname: userInfo.nickname || userInfo.name,
            username: userInfo.username || userInfo.email,
            avatarUrl: userInfo.avatarUrl || userInfo.avatar
          };
          
          // 缓存当前用户信息
          userCache.value[currentUserId] = currentUserInfo;
          console.log(`从sessionStorage缓存当前用户信息: ID=${currentUserId}, 昵称=${currentUserInfo.nickname}, 头像=${currentUserInfo.avatarUrl}`);
          return currentUserInfo;
        }
      }
      
      // 尝试从sessionStorage的current_user获取
      const sessionCurrentUserStr = sessionStorage.getItem('current_user');
      if (sessionCurrentUserStr) {
        const currentUser = JSON.parse(sessionCurrentUserStr);
        if (currentUser) {
          const currentUserInfo = {
            id: currentUserId,
            nickname: currentUser.nickname || currentUser.name,
            username: currentUser.username || currentUser.email,
            avatarUrl: currentUser.avatarUrl || currentUser.avatar
          };
          
          // 缓存当前用户信息
          userCache.value[currentUserId] = currentUserInfo;
          console.log(`从sessionStorage的current_user缓存当前用户信息: ID=${currentUserId}, 昵称=${currentUserInfo.nickname}, 头像=${currentUserInfo.avatarUrl}`);
          return currentUserInfo;
        }
      }
    } catch (error) {
      console.error('解析当前用户信息失败:', error);
    }
    
    // 如果无法获取当前用户信息，创建默认信息
    return this.createDefaultUserInfo(currentUserId);
  },
  
  // 从会话参与者中获取用户信息
  async getUserFromConversation(userId: number): Promise<any> {
    try {
      const conversationId = props.conversationId;
      if (!conversationId) return null;
      
      const currentUserId = getCurrentUserId();
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      
      if (!token) {
        console.warn('未找到认证令牌，无法获取会话信息');
        return null;
      }
      
      // 获取会话详情
      const response = await fetch(`/api/conversations/${conversationId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': String(currentUserId)
        }
      });
      
      if (!response.ok) {
        console.warn(`获取会话信息失败: ${response.status} ${response.statusText}`);
        return null;
      }
      
      const data = await response.json();
      
      if (data.success && data.data && data.data.participants) {
        // 查找指定用户
        const participant = data.data.participants.find((p: any) => p.userId === userId);
        if (participant) {
          // 缓存用户信息
          const userInfo = {
            id: participant.userId,
            nickname: participant.nickname,
            username: participant.username,
            avatarUrl: participant.avatarUrl,
            alias: participant.alias
          };
          userCache.value[userId] = userInfo;
          console.log(`从会话参与者中获取用户信息: ID=${userId}, 昵称=${participant.nickname}`);
          return userInfo;
        }
      }
      
      return null;
    } catch (error) {
      console.error(`从会话参与者获取用户信息失败: ${error}`);
      return null;
    }
  },
  
  // 创建默认用户信息
  createDefaultUserInfo(userId: number): any {
    const defaultInfo = {
      id: userId,
      nickname: `用户${userId}`,
      username: `user${userId}`,
      avatarUrl: '/favicon.ico'
    };
    
    // 缓存默认信息
    userCache.value[userId] = defaultInfo;
    console.log(`创建默认用户信息: ID=${userId}`);
    return defaultInfo;
  },
  
  // 获取联系人信息
  async getContactInfo(userId: number): Promise<any> {
    // 如果缓存中已有该用户信息，直接返回
    if (userCache.value[userId]) {
      return userCache.value[userId];
    }
    
    try {
      // 从API获取联系人列表
      const currentUserId = getCurrentUserId();
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      
      if (!token) {
        console.warn('未找到认证令牌，无法获取联系人信息');
        return null;
      }
      
      // 获取联系人列表
      const response = await fetch(`/api/contacts?userId=${currentUserId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': String(currentUserId)
        }
      });
      
      if (!response.ok) {
        console.warn(`获取联系人列表失败: ${response.status} ${response.statusText}`);
        return null;
      }
      
      const data = await response.json();
      
      if (data.success && data.data && Array.isArray(data.data)) {
        // 查找指定联系人
        const contact = data.data.find((c: any) => {
          // 检查多种可能的ID字段
          const contactId = c.contactId || c.friendId || (c.friend ? c.friend.id : null);
          return contactId === userId;
        });
        
        if (contact) {
          // 提取联系人信息
          const contactInfo = {
            id: userId,
            nickname: contact.nickname || (contact.friend ? contact.friend.nickname : null),
            username: contact.username || (contact.friend ? contact.friend.username : null),
            avatarUrl: contact.avatarUrl || (contact.friend ? contact.friend.avatarUrl : null),
            alias: contact.alias
          };
          
          // 缓存联系人信息
          userCache.value[userId] = contactInfo;
          console.log(`从联系人列表中获取用户信息: ID=${userId}, 昵称=${contactInfo.nickname}, 备注=${contactInfo.alias}`);
          return contactInfo;
        }
      }
      
      return null;
    } catch (error) {
      console.error(`获取联系人信息失败: ${error}`);
      return null;
    }
  },
  
  // 批量获取用户信息
  async batchGetUserInfo(userIds: number[]): Promise<void> {
    if (!userIds.length) return;
    
    // 过滤掉已缓存的用户ID
    const uncachedUserIds = userIds.filter(id => !userCache.value[id]);
    if (!uncachedUserIds.length) return;
    
    // 先尝试从联系人列表获取
    await this.batchGetFromContacts(uncachedUserIds);
    
    // 检查还有哪些用户没有获取到信息
    const remainingIds = uncachedUserIds.filter(id => !userCache.value[id]);
    
    // 对于剩余的用户，创建默认信息
    remainingIds.forEach(id => this.createDefaultUserInfo(id));
  },
  
  // 从联系人列表批量获取用户信息
  async batchGetFromContacts(userIds: number[]): Promise<void> {
    try {
      const currentUserId = getCurrentUserId();
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      
      if (!token) {
        console.warn('未找到认证令牌，无法获取联系人列表');
        return;
      }
      
      // 获取联系人列表
      const response = await fetch(`/api/contacts?userId=${currentUserId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'X-User-Id': String(currentUserId)
        }
      });
      
      if (!response.ok) {
        console.warn(`获取联系人列表失败: ${response.status} ${response.statusText}`);
        return;
      }
      
      const data = await response.json();
      
      if (data.success && data.data && Array.isArray(data.data)) {
        // 遍历联系人列表，找出匹配的用户
        data.data.forEach((contact: any) => {
          // 检查多种可能的ID字段
          const contactId = contact.contactId || contact.friendId || (contact.friend ? contact.friend.id : null);
          
          // 如果是我们要查找的用户之一
          if (contactId && userIds.includes(contactId)) {
            // 提取联系人信息
            const contactInfo = {
              id: contactId,
              nickname: contact.nickname || (contact.friend ? contact.friend.nickname : null),
              username: contact.username || (contact.friend ? contact.friend.username : null),
              avatarUrl: contact.avatarUrl || (contact.friend ? contact.friend.avatarUrl : null),
              alias: contact.alias
            };
            
            // 缓存联系人信息
            userCache.value[contactId] = contactInfo;
            console.log(`批量缓存联系人信息: ID=${contactId}, 昵称=${contactInfo.nickname}, 备注=${contactInfo.alias}`);
          }
        });
      }
    } catch (error) {
      console.error(`批量获取联系人信息失败: ${error}`);
    }
  }
};

// 加载聊天历史记录
const loadChatHistory = async () => {
  try {
    console.log('加载聊天历史记录，会话ID:', props.conversationId);
    
    if (!props.conversationId) {
      console.warn('无效的会话ID');
      return;
    }
    
    // 加载所有聊天记录
    await loadAllMessages();
    
  } catch (err) {
    console.error('加载聊天历史出错:', err);
    error.value = err instanceof Error ? err.message : '加载聊天历史失败';
  }
};

// 加载所有聊天记录
const loadAllMessages = async () => {
  try {
    const allMessages: any[] = [];
    let currentPage = 0;
    let hasMore = true;
    const pageSize = 50; // 每次加载更多消息
    
    while (hasMore) {
      const response = await messageApi.getMessages(Number(props.conversationId), currentPage, pageSize);
      
      if (response.success && response.data) {
        const messages = response.data.content;
        
        if (messages.length === 0) {
          hasMore = false;
        } else {
          allMessages.push(...messages);
          currentPage++;
          
          // 如果返回的消息数量小于请求的数量，说明没有更多消息了
          if (messages.length < pageSize) {
            hasMore = false;
          }
        }
      } else {
        console.error('加载消息失败:', response.message);
        hasMore = false;
      }
    }
    
    // 处理所有消息
    chatHistory.value = allMessages.map(msg => {
      // 确保消息对象有必要的字段
      return {
        id: msg.id,
        conversationId: msg.conversationId,
        senderId: msg.senderId || (msg.message ? msg.message.senderId : null),
        senderName: msg.senderName || msg.senderNickname || (msg.message ? msg.message.senderNickname : null) || '用户',
        senderAvatar: msg.senderAvatar || (msg.message ? msg.message.senderAvatar : null) || '',
        content: msg.content || (msg.message ? msg.message.content : ''),
        type: (msg.type || msg.messageType || (msg.message ? msg.message.messageType : null) || 'TEXT').toUpperCase(),
        fileName: msg.fileName,
        createdAt: msg.createdAt || (msg.message ? msg.message.createdAt : new Date().toISOString())
      };
    });
    
    console.log('加载聊天历史成功，消息数量:', chatHistory.value.length);
    
    // 预加载所有用户信息
    await preloadUserInfo();
    
  } catch (err) {
    console.error('加载所有消息出错:', err);
    throw err;
  }
};

// 预加载所有用户信息
const preloadUserInfo = async () => {
  try {
    // 收集所有发送者ID
    const senderIds = new Set<number>();
    chatHistory.value.forEach(msg => {
      if (msg.senderId) {
        senderIds.add(msg.senderId);
      }
    });
    
    console.log('预加载用户信息，用户数量:', senderIds.size);
    
    // 获取会话详情，包括参与者信息
    const response = await messageApi.getConversation(Number(props.conversationId), getCurrentUserId());
    
    if (response.success && response.data && response.data.participants) {
      // 缓存所有参与者信息
      const participants = response.data.participants;
      participants.forEach(participant => {
        if (participant.userId) {
          userCache.value[participant.userId] = {
            id: participant.userId,
            alias: participant.alias, // 备注
            nickname: participant.nickname,
            username: participant.username,
            avatarUrl: participant.avatarUrl
          };
          console.log(`缓存参与者信息: ID=${participant.userId}, 昵称=${participant.nickname}, 头像=${participant.avatarUrl}`);
        }
      });
    }
    
    // 对于缓存中没有的用户，尝试从联系人列表获取
    const uncachedUserIds = Array.from(senderIds).filter(id => !userCache.value[id]);
    if (uncachedUserIds.length > 0) {
      await loadContactsInfo();
    }
    
    // 更新消息中的发送者信息
    chatHistory.value.forEach(msg => {
      if (msg.senderId && userCache.value[msg.senderId]) {
        const user = userCache.value[msg.senderId];
        msg.senderName = user.alias || user.nickname || user.username || `用户${msg.senderId}`;
        msg.senderAvatar = user.avatarUrl || '/favicon.ico';
      }
    });
    
  } catch (error) {
    console.error('预加载用户信息失败:', error);
  }
};

// 加载会话中的用户信息
const loadConversationUsers = async () => {
  try {
    if (!props.conversationId) {
      console.warn('无效的会话ID，无法加载用户信息');
      return;
    }
    
    console.log('加载会话用户信息，会话ID:', props.conversationId);
    
    // 获取会话详情，包括参与者信息
    const response = await messageApi.getConversation(Number(props.conversationId), getCurrentUserId());
    
    if (response.success && response.data) {
      const conversation = response.data;
      
      // 如果有参与者信息，缓存用户信息
      if (conversation.participants && conversation.participants.length > 0) {
        const newCache: Record<number, any> = {};
        
        conversation.participants.forEach(participant => {
          if (participant.userId) {
            newCache[participant.userId] = {
              id: participant.userId,
              alias: participant.alias, // 备注
              nickname: participant.nickname,
              username: participant.username,
              avatarUrl: participant.avatarUrl
            };
            
            console.log(`缓存用户信息: ID=${participant.userId}, 备注=${participant.alias}, 昵称=${participant.nickname}, 头像=${participant.avatarUrl}`);
          }
        });
        
        // 更新缓存
        userCache.value = { ...userCache.value, ...newCache };
        console.log('用户信息缓存已更新:', userCache.value);
        
        // 确保当前用户信息也被正确加载
        const currentUserId = getCurrentUserId();
        if (currentUserId && !userCache.value[currentUserId]) {
          await userCacheService.loadCurrentUserInfo();
        }
        
        // 也保存到localStorage，以便在页面刷新后仍然可用
        try {
          localStorage.setItem('userCache', JSON.stringify(userCache.value));
        } catch (e) {
          console.error('保存用户缓存到localStorage失败:', e);
        }
      } else {
        console.warn('会话没有参与者信息');
        
        // 尝试从联系人API获取用户信息
        await loadContactsInfo();
      }
    } else {
      console.error('获取会话详情失败:', response.message);
      
      // 尝试从联系人API获取用户信息
      await loadContactsInfo();
    }
  } catch (err) {
    console.error('加载会话用户信息出错:', err);
    
    // 尝试从联系人API获取用户信息
    await loadContactsInfo();
  }
};

// 从联系人API加载用户信息
const loadContactsInfo = async () => {
  try {
    console.log('尝试从联系人API加载用户信息');
    
    // 获取当前用户的联系人列表
    const userId = getCurrentUserId();
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
    
    if (!token) {
      console.warn('未找到认证令牌，无法获取联系人信息');
      return;
    }
    
    const response = await fetch(`/api/contacts?userId=${userId}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Id': userId.toString()
      }
    });
    
    if (response.ok) {
      const data = await response.json();
      
      if (data.success && data.data && Array.isArray(data.data)) {
        const newCache: Record<number, any> = {};
        
        data.data.forEach((contact: any) => {
          // 获取联系人ID
          const contactId = contact.contactId || contact.friendId || (contact.friend ? contact.friend.id : null);
          
          if (contactId) {
            newCache[contactId] = {
              id: contactId,
              alias: contact.alias, // 备注
              nickname: contact.nickname || (contact.friend ? contact.friend.nickname : null),
              username: contact.username || (contact.friend ? contact.friend.username : null),
              avatarUrl: contact.avatarUrl || (contact.friend ? contact.friend.avatarUrl : null)
            };
            
            console.log(`从联系人加载用户信息: ID=${contactId}, 备注=${contact.alias}, 昵称=${newCache[contactId].nickname}`);
          }
        });
        
        // 更新缓存
        userCache.value = { ...userCache.value, ...newCache };
        console.log('从联系人加载的用户信息已更新:', userCache.value);
        
        // 保存到localStorage
        try {
          localStorage.setItem('userCache', JSON.stringify(userCache.value));
        } catch (e) {
          console.error('保存用户缓存到localStorage失败:', e);
        }
      }
    } else {
      console.warn(`获取联系人列表失败: ${response.status} ${response.statusText}`);
    }
    
    // 确保当前用户信息也被加载
    await loadCurrentUserFromConversation();
    
  } catch (err) {
    console.error('从联系人API加载用户信息失败:', err);
  }
};

// 从会话中加载当前用户信息
const loadCurrentUserFromConversation = async () => {
  try {
    const currentUserId = getCurrentUserId();
    if (!currentUserId || userCache.value[currentUserId]) return;
    
    console.log('尝试从会话中加载当前用户信息');
    
    const conversationId = props.conversationId;
    if (!conversationId) return;
    
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
    if (!token) return;
    
    const response = await fetch(`/api/conversations/${conversationId}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'X-User-Id': currentUserId.toString()
      }
    });
    
    if (response.ok) {
      const data = await response.json();
      
      if (data.success && data.data && data.data.participants) {
        // 查找当前用户
        const currentParticipant = data.data.participants.find((p: any) => p.userId === currentUserId);
        
        if (currentParticipant) {
          userCache.value[currentUserId] = {
            id: currentUserId,
            nickname: currentParticipant.nickname,
            username: currentParticipant.username,
            avatarUrl: currentParticipant.avatarUrl,
            alias: currentParticipant.alias
          };
          
          console.log(`从会话加载当前用户信息: ID=${currentUserId}, 昵称=${currentParticipant.nickname}, 头像=${currentParticipant.avatarUrl}`);
        }
      }
    }
  } catch (error) {
    console.error('从会话加载当前用户信息失败:', error);
  }
};

// 获取当前用户ID
const getCurrentUserId = (): number => {
  const userIdStr = localStorage.getItem('userId') || sessionStorage.getItem('userId');
  if (userIdStr) {
    return Number(userIdStr);
  }
  
  // 尝试从userInfo中获取
  const userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
  if (userInfoStr) {
    try {
      const userInfo = JSON.parse(userInfoStr);
      if (userInfo && userInfo.id) {
        return Number(userInfo.id);
      }
    } catch (e) {
      console.error('解析userInfo失败:', e);
    }
  }
  
  // 默认返回1，实际项目中应该处理未登录情况
  return 1;
};

// 方法
const search = async (page: number) => {
  if (!searchKeyword.value || searchKeyword.value.trim() === '') {
    // 如果搜索关键词为空，清除搜索状态并显示聊天历史
    clearSearch();
    return;
  }
  
  loading.value = true;
  error.value = null;
  
  try {
    currentPage.value = page;
    
    const searchRequest: MessageSearchRequest = {
      conversationId: parseInt(props.conversationId),
      keyword: searchKeyword.value.trim(),
      page,
      size: pageSize.value
    };
    
    console.log('发送搜索请求:', JSON.stringify(searchRequest));
    
    const response = await messageApi.searchMessages(searchRequest);
    
    console.log('搜索响应:', response);
    
    if (response.success && response.data) {
      console.log('搜索成功，处理响应数据:', response.data);
      
      // 直接使用response.data.results作为搜索结果
      if (response.data.results && Array.isArray(response.data.results)) {
        // 处理搜索结果，确保每个结果都有必要的字段
        searchResults.value = response.data.results.map(result => {
          // 记录原始数据，用于调试
          console.log('处理搜索结果项:', JSON.stringify(result));
          
          // 确保消息对象有完整的发送者信息
          if (result.message) {
            // 如果消息中没有发送者昵称，尝试从缓存获取
            if (!result.message.senderNickname && result.message.senderId) {
              const cachedUser = userCache.value[result.message.senderId];
              if (cachedUser) {
                result.message.senderNickname = cachedUser.alias || cachedUser.nickname || cachedUser.username;
                result.message.senderAvatar = cachedUser.avatarUrl;
              }
            }
            
            // 如果还是没有昵称，设置默认值
            if (!result.message.senderNickname) {
              const currentUserId = getCurrentUserId();
              if (result.message.senderId === currentUserId) {
                result.message.senderNickname = '我';
              } else {
                result.message.senderNickname = `用户${result.message.senderId}`;
              }
            }
            
            // 如果没有头像，设置默认头像
            if (!result.message.senderAvatar) {
              result.message.senderAvatar = '/favicon.ico';
            }
          }
          
          return result;
        });
        
        // 预加载搜索结果中的用户信息
        const senderIds = searchResults.value
          .map(result => result.message.senderId)
          .filter((id): id is number => id !== undefined && id !== null);
        
        if (senderIds.length > 0) {
          userCacheService.batchGetUserInfo(senderIds).then(() => {
            // 用户信息加载完成后，更新搜索结果
            searchResults.value = [...searchResults.value];
          });
        }
        
        totalPages.value = response.data.totalPages || 0;
        totalElements.value = response.data.total || 0;
        hasSearched.value = true;
        console.log('搜索成功，找到结果数量:', response.data.total);
      } else {
        console.error('搜索响应中没有results数组:', response.data);
        searchResults.value = [];
        error.value = '搜索结果格式不正确';
      }
    } else {
      error.value = response.message || '搜索失败';
      searchResults.value = [];
      console.error('搜索失败:', error.value);
    }
  } catch (err) {
    console.error('搜索消息出错:', err);
    // 提供更详细的错误信息
    if (err instanceof Error) {
      error.value = `搜索失败: ${err.message}`;
    } else {
      error.value = '搜索失败，请稍后重试';
    }
    searchResults.value = [];
  } finally {
    loading.value = false;
  }
};

// 获取发送者头像（同步版本，用于模板）
const getSenderAvatarSync = (result: MessageSearchResult): string => {
  // 获取发送者ID
  const senderId = result.message.senderId;
  if (!senderId) {
    return '/favicon.ico'; // 默认头像
  }
  
  // 首先尝试从消息对象中获取
  if (result.message.senderAvatar && result.message.senderAvatar !== '/favicon.ico') {
    return result.message.senderAvatar;
  }
  
  // 检查是否是当前用户
  const currentUserId = getCurrentUserId();
  if (senderId === currentUserId) {
    // 尝试从localStorage或sessionStorage获取当前用户头像
    try {
      const userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
      if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo && (userInfo.avatarUrl || userInfo.avatar)) {
          return userInfo.avatarUrl || userInfo.avatar;
        }
      }
    } catch (e) {
      console.error('解析用户信息失败:', e);
    }
  }
  
  // 尝试从缓存获取
  if (userCache.value[senderId]?.avatarUrl) {
    return userCache.value[senderId].avatarUrl;
  }
  
  // 如果没有缓存，触发异步加载但返回默认头像
  loadUserInfo(senderId);
  
  // 默认头像
  return '/favicon.ico';
};

// 获取发送者昵称（同步版本，用于模板）
const getSenderNameSync = (result: MessageSearchResult): string => {
  // 获取发送者ID
  const senderId = result.message.senderId;
  if (!senderId) {
    return '未知用户';
  }
  
  // 首先尝试从消息对象中获取
  if (result.message.senderNickname && !result.message.senderNickname.startsWith('用户')) {
    return result.message.senderNickname;
  }
  
  // 检查是否是当前用户
  const currentUserId = getCurrentUserId();
  if (senderId === currentUserId) {
    // 尝试从localStorage或sessionStorage获取当前用户昵称
    try {
      const userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
      if (userInfoStr) {
        const userInfo = JSON.parse(userInfoStr);
        if (userInfo) {
          return userInfo.nickname || userInfo.name || userInfo.username || '我';
        }
      }
      
      // 如果没有找到，返回"我"
      return '我';
    } catch (e) {
      console.error('解析用户信息失败:', e);
    }
  }
  
  // 尝试从缓存获取，优先使用备注
  if (userCache.value[senderId]) {
    return userCache.value[senderId].alias || 
           userCache.value[senderId].nickname || 
           userCache.value[senderId].username || 
           `用户${senderId}`;
  }
  
  // 如果没有缓存，触发异步加载但返回用户ID
  loadUserInfo(senderId);
  
  // 默认名称
  return `用户${senderId}`;
};

// 异步加载用户信息
const loadUserInfo = async (userId: number): Promise<void> => {
  // 如果已经在加载中，不重复加载
  if (loadingUsers.has(userId)) return;
  
  loadingUsers.add(userId);
  
  try {
    // 检查是否是当前用户
    const currentUserId = getCurrentUserId();
    if (userId === currentUserId) {
      // 直接加载当前用户信息
      const currentUserInfo = await userCacheService.loadCurrentUserInfo();
      if (currentUserInfo) {
        console.log('成功加载当前用户信息:', currentUserInfo);
        loadingUsers.delete(userId);
        return;
      }
    }
    
    // 先尝试获取联系人信息
    const contactInfo = await userCacheService.getContactInfo(userId);
    if (contactInfo) {
      loadingUsers.delete(userId);
      return;
    }
    
    // 如果联系人信息不存在，尝试获取用户信息
    const userInfo = await userCacheService.getUserInfo(userId);
    if (userInfo) {
      loadingUsers.delete(userId);
      return;
    }
  } catch (error) {
    console.error(`加载用户信息失败: ${error}`);
  } finally {
    loadingUsers.delete(userId);
  }
};

// 正在加载中的用户ID集合
const loadingUsers = new Set<number>();

// 组件挂载时
onMounted(() => {
  console.log('MessageSearch组件已挂载，初始激活状态:', props.isActive);
  
  // 优先加载当前用户信息
  const currentUserId = getCurrentUserId();
  userCacheService.loadCurrentUserInfo();
  
  loadChatHistory(); // 组件挂载时加载聊天历史记录
  loadConversationUsers(); // 加载会话用户信息
  
  // 预加载当前会话中的所有用户信息
  if (chatHistory.value.length > 0) {
    const userIds = chatHistory.value
      .map(msg => msg.senderId)
      .filter((id): id is number => id !== undefined && id !== null);
    
    // 确保当前用户ID也被包含在内
    if (!userIds.includes(currentUserId)) {
      userIds.push(currentUserId);
    }
    
    if (userIds.length > 0) {
      userCacheService.batchGetUserInfo(userIds);
    }
  }
});

// 事件处理函数
const handleEnterKey = () => {
  search(0);
};

const handleSearchClick = () => {
  search(0);
};

const handleRetryClick = () => {
  search(currentPage.value);
};

const handlePrevPage = () => {
  changePage(currentPage.value - 1);
};

const handleNextPage = () => {
  changePage(currentPage.value + 1);
};

const clearSearch = () => {
  searchKeyword.value = '';
  searchResults.value = [];
  hasSearched.value = false;
  error.value = null;
  currentPage.value = 0;
  totalPages.value = 0;
  totalElements.value = 0;
  
  // 重新加载聊天历史
  loadChatHistory();
  
  // 聚焦搜索框
  nextTick(() => {
    searchInputRef.value?.focus();
  });
};

const changePage = (page: number) => {
  if (page < 0 || page >= totalPages.value) return;
  search(page);
};

// 跳转到指定消息
const jumpToMessage = (message: any) => {
  console.log('跳转到消息:', message);
  
  // 获取消息ID
  let messageId: number;
  
  if (typeof message === 'object') {
    if ('id' in message) {
      // 直接是消息对象
      messageId = Number(message.id);
    } else if (message.message && 'id' in message.message) {
      // 搜索结果中的消息对象
      messageId = Number(message.message.id);
    } else {
      console.error('无法获取消息ID:', message);
      return;
    }
  } else {
    console.error('无效的消息对象:', message);
    return;
  }
  
  if (isNaN(messageId) || messageId <= 0) {
    console.error('无效的消息ID:', messageId);
    return;
  }
  
  console.log('跳转到消息ID:', messageId);
  emit('jump-to-message', messageId);
  
  // 关闭搜索面板
  close();
};

// 关闭搜索面板
const close = () => {
  // 先添加向右划出的动画类
  const searchPanel = document.querySelector('.message-search');
  if (searchPanel) {
    // 先添加向右划出的类
    searchPanel.classList.add('slide-out-right');
    
    // 等待动画完成后再关闭面板
    setTimeout(() => {
      // 移除划出类
      searchPanel.classList.remove('slide-out-right');
      // 发出关闭事件
  emit('close');
    }, 300); // 动画持续时间
  } else {
    // 如果找不到面板元素，直接关闭
    emit('close');
  }
};

// 格式化时间
const formatTime = (timestamp: string) => {
  if (!timestamp) return '';
  
  const date = new Date(timestamp);
  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  
  if (isToday) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
  
  return date.toLocaleDateString([], { 
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 监听激活状态
watch(() => props.isActive, (isActive) => {
  console.log('MessageSearch组件激活状态变更:', isActive);
  if (isActive) {
    console.log('搜索面板已激活，聚焦输入框');
    // 如果激活且没有搜索过，加载聊天历史
    if (!hasSearched.value) {
      loadChatHistory();
    }
    nextTick(() => {
      searchInputRef.value?.focus();
    });
  }
});

// 监听会话ID变化
watch(() => props.conversationId, (newId, oldId) => {
  console.log('会话ID变更:', oldId, '->', newId);
  if (newId && newId !== oldId) {
    // 清空搜索结果
    clearSearch();
    // 重新加载聊天历史
    loadChatHistory();
    // 加载会话用户信息
    loadConversationUsers();
  }
});

// 处理搜索结果，预加载用户信息
watch(() => searchResults.value, (results) => {
  if (results && results.length > 0) {
    // 收集所有发送者ID
    const senderIds = results
      .map(result => result.message.senderId)
      .filter((id): id is number => id !== undefined && id !== null);
    
    // 确保当前用户ID也被包含在内
    const currentUserId = getCurrentUserId();
    if (!senderIds.includes(currentUserId)) {
      senderIds.push(currentUserId);
    }
    
    // 批量获取用户信息
    if (senderIds.length > 0) {
      userCacheService.batchGetUserInfo(senderIds);
    }
    
    // 单独确保当前用户信息被加载
    userCacheService.loadCurrentUserInfo();
  }
});
</script>

<style scoped>
.message-search {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  background-color: #fff;
  z-index: 100;
  display: flex;
  flex-direction: column;
  transform: translateX(-100%); /* 初始位置在左侧屏幕外 */
  transition: transform 0.3s ease;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  will-change: transform;
  backface-visibility: hidden;
}

.message-search.is-active {
  transform: translateX(0); /* 激活时滑入视图 */
}

/* 添加向右划出的动画类 */
.message-search.slide-out-right {
  transform: translateX(100%) !important; /* 向右侧划出 */
}

.search-header {
  padding: 10px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #eee;
  background-color: #f5f5f5;
}

.search-input-container {
  display: flex;
  align-items: center;
  flex: 1;
  background-color: #fff;
  border-radius: 20px;
  padding: 5px 10px;
  margin-right: 10px;
}

.back-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 5px;
  color: #555;
  display: flex;
  align-items: center;
  font-weight: 500;
  transition: all 0.2s;
}

.back-btn:hover {
  color: #1890ff;
}

.back-text {
  margin-left: 5px;
  font-size: 14px;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 5px 10px;
  font-size: 14px;
}

.clear-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 5px;
  color: #999;
}

.search-btn {
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.search-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.search-results {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.loading-container,
.error-container,
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #666;
}

.loading-spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-left-color: #4caf50;
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

.error-container i {
  font-size: 40px;
  color: #f44336;
  margin-bottom: 10px;
}

.empty-container i {
  font-size: 40px;
  color: #999;
  margin-bottom: 10px;
}

.retry-btn {
  margin-top: 10px;
  padding: 5px 15px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.search-result-count {
  padding: 10px 0;
  color: #666;
  font-size: 14px;
}

.search-result-list {
  margin-bottom: 15px;
}

.search-result-item {
  padding: 10px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}

.search-result-item:hover {
  background-color: #f9f9f9;
}

.result-sender {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.sender-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin-right: 8px;
}

.sender-name {
  font-weight: bold;
  margin-right: 10px;
  font-size: 14px;
}

.result-time {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}

.result-content {
  font-size: 14px;
  line-height: 1.4;
  color: #333;
  word-break: break-word;
}

.result-content :deep(em) {
  background-color: rgba(255, 235, 59, 0.5);
  font-style: normal;
  padding: 0 2px;
  border-radius: 2px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 15px;
  padding: 10px 0;
}

.pagination-btn {
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 15px;
  margin: 0 5px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.pagination-btn:hover:not(:disabled) {
  background-color: #45a049;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}

.pagination-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.pagination-btn:disabled {
  background-color: #cccccc;
  opacity: 0.7;
  cursor: not-allowed;
  box-shadow: none;
}

.pagination-info {
  margin: 0 10px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.history-header {
  padding: 10px;
  border-bottom: 1px solid #eee;
  background-color: #f9f9f9;
}

.history-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.history-list {
  overflow-y: auto;
}

.history-item {
  padding: 10px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}

.history-item:hover {
  background-color: #f9f9f9;
}

.history-sender {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.sender-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin-right: 8px;
  object-fit: cover;
}

.history-time {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}

.history-content {
  font-size: 14px;
  color: #333;
  word-break: break-word;
}

.empty-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 0;
  color: #999;
}

.empty-history i {
  font-size: 40px;
  margin-bottom: 10px;
}

.highlight {
  background-color: #ffeb3b;
  padding: 0 2px;
  border-radius: 2px;
}
</style>