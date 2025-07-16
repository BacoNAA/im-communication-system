<template>
  <div 
    class="conversation-item"
    :class="{ 'pinned': chat.isPinned, 'active': isActive }" 
    @click="$emit('click')"
    @contextmenu.prevent="$emit('context-menu', $event)"
  >
    <div class="avatar">
      <img v-if="chat.avatar" :src="chat.avatar" :alt="chat.name" />
      <span v-else class="avatar-text">{{ getAvatarText(chat.name) }}</span>
    </div>
    <div class="content">
      <div class="header">
        <div class="name" :class="{ 'unread': chat.unreadCount > 0 }">{{ chat.name }}</div>
        <div class="time">{{ formatTime(chat.lastMessageTime) }}</div>
      </div>
      <div class="preview">
        <span v-if="chat.isDnd" class="mute-icon">🔕</span>
        <span class="message">{{ chat.lastMessage || '暂无消息' }}</span>
        <span v-if="chat.unreadCount > 0" class="badge">{{ chat.unreadCount }}</span>
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

// 格式化时间
const formatTime = (timestamp: string | number | Date): string => {
  if (!timestamp) return '';
  
  // 使用相对时间格式化
  return formatRelativeTime(new Date(timestamp));
};
</script>

<style scoped>
.conversation-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 4px solid transparent;
}

.conversation-item:hover {
  background-color: #f5f5f5;
}

.conversation-item.active {
  background-color: #e6f7ff;
  border-left: 4px solid #1890ff;
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  padding-left: 16px;
}

.conversation-item.active .name {
  color: #1890ff;
  font-weight: 600;
}

.conversation-item.pinned {
  background-color: #f7f7f7;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 24px;
  overflow: hidden;
  margin-right: 12px;
  flex-shrink: 0;
  background-color: #e1e1e1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  font-size: 20px;
  color: #666;
}

.content {
  flex: 1;
  min-width: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.name {
  font-weight: 500;
  font-size: 16px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%;
}

.name.unread {
  font-weight: 600;
  color: #000;
}

.time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.preview {
  display: flex;
  align-items: center;
  color: #666;
  font-size: 14px;
}

.mute-icon {
  margin-right: 4px;
  font-size: 12px;
}

.message {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.badge {
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  background-color: #ff4d4f;
  color: white;
  font-size: 12px;
  padding: 0 6px;
  margin-left: 8px;
}
</style> 