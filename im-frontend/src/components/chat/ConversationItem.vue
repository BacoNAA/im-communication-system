<template>
  <div 
    class="conversation-item" 
    :class="{ 
      active: isActive, 
      'is-pinned': chat.isPinned,
      'is-archived': isArchived
    }"
    @click="$emit('click')"
    @contextmenu.prevent="$emit('context-menu', $event)"
  >
    <div v-if="chat.isPinned" class="pin-indicator"></div>
    <div v-if="isArchived" class="archive-indicator" title="已归档">
      <i class="fas fa-archive"></i>
    </div>
    <div class="avatar">
      <img v-if="chat.avatar" :src="chat.avatar" alt="Avatar" />
      <div v-else class="default-avatar">
        {{ getInitials(chat.name) }}
      </div>
      <div v-if="chat.isDnd" class="dnd-badge" title="免打扰">
        <i class="fas fa-bell-slash"></i>
      </div>
    </div>
    
    <div class="content">
      <div class="header">
        <div class="name" :class="{ 
          'pinned-name': chat.isPinned,
          'archived-name': isArchived
        }">
          {{ chat.name }}
          <span v-if="chat.isPinned" class="pin-icon" title="已置顶">
            <i class="fas fa-thumbtack"></i>
          </span>
          <span v-if="isArchived" class="archive-icon" title="已归档">
            <i class="fas fa-archive"></i>
          </span>
          <span v-if="chat.isDnd" class="dnd-icon" title="已开启免打扰">
            <i class="fas fa-bell-slash"></i>
          </span>
        </div>
        <div class="time">{{ formatTime(chat.lastMessageTime) }}</div>
      </div>
      <div class="message">
        <div class="text">{{ chat.lastMessage }}</div>
        <div v-if="chat.unreadCount > 0" 
             class="badge" 
             :class="{'unread-badge': !chat.isDnd, 'muted-badge': chat.isDnd, 'pulse': !chat.isDnd && chat.unreadCount > 0}">
          {{ chat.unreadCount > 99 ? '99+' : chat.unreadCount }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { formatRelativeTime } from '@/utils/helpers';

// 定义props
const props = defineProps({
  chat: {
    type: Object,
    required: true
  },
  activeChatId: {
    type: [Number, String],
    default: null
  },
  isArchived: {
    type: Boolean,
    default: false
  }
});

// 定义事件
defineEmits(['click', 'context-menu']);

// 计算属性：是否是当前活跃会话
const isActive = computed(() => {
  // 将两者都转换为字符串进行比较，确保类型匹配
  return String(props.activeChatId) === String(props.chat.id);
});

// 获取头像文字（首字母或表情）
const getAvatarText = (name: string): string => {
  if (!name) return '?';
  
  // 检查是否为表情符号开头
  const emojiRegex = /^[\p{Emoji}]/u;
  if (emojiRegex.test(name)) {
    return name.match(emojiRegex)?.[0] || name.charAt(0);
  }
  
  // 取首字母
  return name.charAt(0).toUpperCase();
};

// 获取名称的首字母作为头像显示
const getInitials = (name: string): string => {
  if (!name || typeof name !== 'string') return '?';
  
  // 简单地取第一个字符
  return name.charAt(0).toUpperCase();
};

// 格式化时间
const formatTime = (timestamp: string | number | Date): string => {
  if (!timestamp) return '';
  
  try {
    // 将时间戳转换为Date对象
    const date = new Date(timestamp);
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      console.warn('无效的时间戳:', timestamp);
      return '';
    }
    
    // 使用相对时间格式化
    return formatRelativeTime(date);
  } catch (error) {
    console.error('格式化时间出错:', error, timestamp);
    return '';
  }
};
</script>

<style scoped>
.conversation-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.conversation-item:hover {
  background-color: #f5f5f5;
}

.conversation-item.active {
  background-color: #e6f7ff;
  border-right: 3px solid #1890ff;
}

.conversation-item.is-pinned {
  background-color: #e6f7ff;
  border-left: 4px solid #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
  margin: 4px 0;
  border-radius: 4px;
}

.conversation-item.is-archived {
  background-color: #f9f0ff;
  border-left: 4px solid #722ed1;
  box-shadow: 0 2px 8px rgba(114, 46, 209, 0.15);
  margin: 4px 0;
  border-radius: 4px;
}

.pin-indicator {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 8px 8px 0 0;
  border-color: #1890ff transparent transparent transparent;
}

.archive-indicator {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 8px 8px 0 0;
  border-color: #722ed1 transparent transparent transparent;
}

.avatar {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.default-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background-color: #1890ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
}

.dnd-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 16px;
  height: 16px;
  background-color: white;
  border: 1px solid #ccc;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 8px;
  color: #666;
}

.content {
  flex: 1;
  min-width: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.name {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.pinned-name {
  font-weight: 600;
  color: #1890ff;
}

.archived-name {
  font-weight: 600;
  color: #722ed1;
}

.pin-icon {
  margin-left: 4px;
  color: #1890ff;
  font-size: 14px;
  transform: rotate(45deg);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.archive-icon {
  margin-left: 4px;
  color: #722ed1;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.dnd-icon {
  margin-left: 4px;
  color: #ff4d4f; /* 免打扰图标颜色 */
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  margin-left: 8px;
}

.message {
  display: flex;
  align-items: center;
}

.text {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}

.badge {
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  background-color: #8c8c8c;
  color: white;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
}

.unread-badge {
  background-color: #ff4d4f;
  color: white;
  border-radius: 10px;
  padding: 0 6px;
  min-width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.muted-badge {
  background-color: #ccc; /* 灰色徽章背景 */
  color: #333; /* 灰色徽章文字 */
  border-radius: 10px;
  padding: 0 6px;
  min-width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.pulse {
  animation: pulse-animation 1s infinite;
}

@keyframes pulse-animation {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.7);
  }
  
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 10px rgba(255, 77, 79, 0);
  }
  
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0);
  }
}
</style> 