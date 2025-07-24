<template>
  <div class="global-search">
    <div class="search-header">
      <div class="search-input-container">
        <input 
          type="text"
          class="search-input"
          v-model="keyword"
          @keyup.enter="search"
          @input="handleInput"
          placeholder="搜索..."
          ref="searchInput"
        />
        <i v-if="keyword" class="clear-icon fas fa-times-circle" @click="clearSearch"></i>
        <i class="search-icon fas fa-search" @click="search"></i>
      </div>
    </div>
    
    <div v-if="isSearching" class="loading-container">
      <div class="loading-spinner"></div>
      <span>正在搜索...</span>
    </div>
    
    <div v-else-if="hasSearched && !hasResults" class="no-results">
      <i class="fas fa-search"></i>
      <span>未找到"{{ keyword }}"的相关结果</span>
      <div v-if="searchError" class="error-message">
        {{ searchError }}
      </div>
    </div>
    
    <div v-else-if="hasResults" class="search-results" ref="searchResultsRef">
      <!-- 消息结果 -->
      <div v-if="messageResults.length > 0" class="result-section">
        <div class="section-header">
          <i class="fas fa-comments"></i>
          <span>消息 ({{ totalMessageResults }})</span>
        </div>
        
        <div class="result-list">
          <div 
            v-for="(result, index) in messageResults"
            :key="`message-${index}`"
            class="result-item message-result"
            @click="navigateToMessage(result)"
          >
            <div class="result-avatar">
              <img 
                v-if="result.message.senderAvatar"
                :src="result.message.senderAvatar"
                alt="Avatar"
              />
              <div v-else class="default-avatar">
                {{ getInitials(result.message.senderNickname || '') }}
              </div>
            </div>
            <div class="result-content">
              <div class="result-header">
                <div class="sender-name">{{ result.message.senderNickname || '用户' }}</div>
                <div class="result-time">{{ formatTime(result.message.createdAt) }}</div>
              </div>
              <div class="result-text" v-html="getHighlightedContent(result)"></div>
              <div class="result-conversation">
                <i class="fas fa-comment-dots"></i>
                <span>{{ getConversationName(result.message.conversationId) }}</span>
              </div>
            </div>
          </div>
          
          <!-- 分页组件 -->
          <div v-if="totalMessageResults > pageSize" class="pagination-container">
            <div class="pagination">
              <button 
                class="pagination-btn" 
                :disabled="currentPage === 0" 
                @click="goToPage(currentPage - 1)"
              >
                上一页
              </button>
              
              <div class="pagination-info">
                第 {{ currentPage + 1 }} 页，共 {{ totalPages }} 页
              </div>
              
              <button 
                class="pagination-btn" 
                :disabled="currentPage >= totalPages - 1" 
                @click="goToPage(currentPage + 1)"
              >
                下一页
              </button>
            </div>
            
            <div class="pagination-summary">
              共找到 {{ totalMessageResults }} 条结果
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { messageApi } from '@/api/message';
import type { MessageSearchResult, MessageSearchResponse } from '@/api/message';
import { formatRelativeTime } from '@/utils/helpers';

// 定义props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  initialKeyword: {
    type: String,
    default: ''
  }
});

// 定义事件
const emit = defineEmits(['close', 'navigate-to-message']);

// 搜索输入框
const searchInput = ref<HTMLInputElement | null>(null);

// 搜索状态
const keyword = ref(props.initialKeyword || '');
const isSearching = ref(false);
const hasSearched = ref(false);
const searchResponse = ref<MessageSearchResponse | null>(null);
const searchError = ref<string | null>(null);

// 搜索结果
const messageResults = ref<MessageSearchResult[]>([]);
const totalMessageResults = ref(0);
const currentPage = ref(0);
const pageSize = ref(20);

// 当组件可见时，聚焦搜索框
watch(() => props.visible, (visible) => {
  if (visible) {
    setTimeout(() => {
      searchInput.value?.focus();
    }, 100);
  }
});

// 计算属性
const hasResults = computed(() => messageResults.value.length > 0);
const totalPages = computed(() => Math.ceil(totalMessageResults.value / pageSize.value));

// 处理输入防抖
let debounceTimeout: number | null = null;
const handleInput = () => {
  if (debounceTimeout) {
    clearTimeout(debounceTimeout);
  }
  
  debounceTimeout = window.setTimeout(() => {
    if (keyword.value.trim().length >= 2) {
      search();
    }
  }, 500);
};

// 执行搜索
const search = async () => {
  if (keyword.value.trim().length === 0) return;
  
  // 重置页码
  currentPage.value = 0;
  
  isSearching.value = true;
  hasSearched.value = true;
  searchError.value = null;
  
  try {
    const response = await messageApi.globalSearch({
      keyword: keyword.value,
      page: currentPage.value,
      size: pageSize.value,
      highlight: true
    });
    
    if (response.success && response.data) {
      searchResponse.value = response.data;
      
      // 检查数据结构
      console.log('搜索响应数据结构:', response.data);
      
      // 后端现在直接返回MessageSearchResponse结构
      messageResults.value = response.data.results || [];
      totalMessageResults.value = response.data.total || 0;
      console.log('搜索结果:', messageResults.value.length, '条, 总数:', totalMessageResults.value);
      
      // 调试输出搜索结果
      if (messageResults.value.length > 0) {
        console.log('第一条搜索结果:', messageResults.value[0]);
      } else {
        console.log('没有找到匹配的结果');
      }
    } else {
      searchError.value = response.message || '搜索失败';
      messageResults.value = [];
      totalMessageResults.value = 0;
      console.error('搜索失败:', response.message);
    }
  } catch (error) {
    console.error('搜索出错:', error);
    searchError.value = error instanceof Error ? error.message : '搜索出错';
    messageResults.value = [];
    totalMessageResults.value = 0;
  } finally {
    isSearching.value = false;
  }
};

// 清除搜索
const clearSearch = () => {
  keyword.value = '';
  hasSearched.value = false;
  searchResponse.value = null;
  messageResults.value = [];
  totalMessageResults.value = 0;
  currentPage.value = 0;
  searchInput.value?.focus();
};

// 搜索结果容器引用
const searchResultsRef = ref<HTMLElement | null>(null);

// 跳转到指定页面
const goToPage = async (page: number) => {
  if (isSearching.value || page < 0 || page >= totalPages.value) return;
  
  currentPage.value = page;
  isSearching.value = true;
  
  try {
    const response = await messageApi.globalSearch({
      keyword: keyword.value,
      page: currentPage.value,
      size: pageSize.value,
      highlight: true
    });
    
    if (response.success && response.data) {
      // 替换当前页结果
      messageResults.value = response.data.results || [];
      
      // 滚动到顶部
      if (searchResultsRef.value) {
        searchResultsRef.value.scrollTop = 0;
      }
    }
  } catch (error) {
    console.error('翻页出错:', error);
  } finally {
    isSearching.value = false;
  }
};

// 导航到消息
const navigateToMessage = (result: MessageSearchResult) => {
  // 构造包含会话ID和消息ID的对象，用于消息导航
  const navigationData = {
    id: result.message.id, // 消息ID
    conversationId: result.message.conversationId, // 会话ID
    content: result.message.content,
    senderNickname: result.message.senderNickname,
    createdAt: result.message.createdAt
  };
  console.log('导航到消息:', navigationData);
  emit('navigate-to-message', navigationData);
};

// 获取会话名称
const getConversationName = (conversationId: number) => {
  // TODO: 根据会话ID获取会话名称，这里暂时返回一个占位符
  return `会话 ${conversationId}`;
};

// 获取高亮内容
const getHighlightedContent = (result: MessageSearchResult) => {
  if (result.highlights?.content && result.highlights.content.length > 0) {
    return result.highlights.content[0];
  }
  return result.message.content || '';
};

// 获取首字母作为头像
const getInitials = (name: string) => {
  if (!name) return '?';
  return name.charAt(0).toUpperCase();
};

// 格式化时间
const formatTime = (timestamp: string) => {
  if (!timestamp) return '';
  try {
    return formatRelativeTime(new Date(timestamp));
  } catch (error) {
    return '';
  }
};

// 组件挂载时
onMounted(() => {
  // 如果有初始关键词，执行搜索
  if (props.initialKeyword) {
    keyword.value = props.initialKeyword;
    search();
  }
});

// 组件卸载时
onUnmounted(() => {
  // 清除定时器
  if (debounceTimeout) {
    clearTimeout(debounceTimeout);
  }
});
</script>

<style scoped>
.global-search {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background-color: #fff;
}

.search-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
}

.search-input-container {
  display: flex;
  align-items: center;
  position: relative;
  width: 100%;
}

.search-input {
  flex: 1;
  padding: 8px 36px 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  width: 100%;
}

.search-input:focus {
  border-color: #40a9ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.search-icon {
  position: absolute;
  right: 10px;
  color: #999;
  cursor: pointer;
}

.clear-icon {
  position: absolute;
  right: 32px;
  color: #999;
  cursor: pointer;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #999;
}

.loading-spinner {
  border: 2px solid #f3f3f3;
  border-top: 2px solid #3498db;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.no-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
  color: #999;
}

.error-message {
  margin-top: 10px;
  color: #ff4d4f;
  font-size: 12px;
}

.no-results i {
  font-size: 32px;
  margin-bottom: 16px;
}

.search-results {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
}

.result-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  padding: 12px 0;
  font-weight: 500;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}

.section-header i {
  margin-right: 8px;
  color: #1890ff;
}

.result-list {
  padding: 8px 0;
}

.result-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.result-item:hover {
  background-color: #f9f9f9;
}

.result-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
  overflow: hidden;
}

.result-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar {
  width: 100%;
  height: 100%;
  background-color: #1890ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.result-content {
  flex: 1;
  min-width: 0;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.sender-name {
  font-weight: 500;
  color: #333;
}

.result-time {
  font-size: 12px;
  color: #999;
}

.result-text {
  font-size: 14px;
  color: #666;
  margin-bottom: 6px;
  line-height: 1.5;
  word-break: break-word;
}

.result-text :deep(.highlight) {
  background-color: rgba(255, 255, 0, 0.4);
  padding: 0 2px;
  border-radius: 2px;
}

.result-conversation {
  font-size: 12px;
  color: #1890ff;
  display: flex;
  align-items: center;
}

.result-conversation i {
  margin-right: 4px;
}

.pagination-container {
  margin-top: 16px;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 8px;
}

.pagination-btn {
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background-color: #fff;
  color: #333;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  border-color: #40a9ff;
  color: #40a9ff;
}

.pagination-btn:disabled {
  background-color: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
  border-color: #f0f0f0;
}

.pagination-info {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.pagination-summary {
  text-align: center;
  font-size: 12px;
  color: #999;
}
</style>