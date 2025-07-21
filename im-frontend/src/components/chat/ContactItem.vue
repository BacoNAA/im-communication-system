<template>
  <div 
    :class="['contact-item', { 'blocked': contact.isBlocked }]" 
    @click="handleClick"
    @contextmenu.prevent="$emit('context-menu', $event)"
  >
    <div class="avatar-container">
      <img 
        v-if="contact.avatarUrl" 
        :src="contact.avatarUrl" 
        :alt="contact.name" 
        class="avatar"
      />
      <div v-else class="avatar text-avatar">
        {{ getInitials(contact.name) }}
      </div>
      <div v-if="contact.online" class="online-indicator"></div>
    </div>
    
    <div class="contact-info">
      <div class="contact-name">
        {{ contact.name }}
        <span v-if="contact.isBlocked" class="blocked-indicator">已拉黑</span>
      </div>
      <div class="contact-signature" v-if="contact.signature">{{ contact.signature }}</div>
      <!-- 添加标签显示 -->
      <div v-if="contact.tags && contact.tags.length > 0" class="contact-tags">
        <div 
          v-for="tag in contact.tags" 
          :key="typeof tag === 'object' ? tag.id : tag" 
          class="contact-tag"
          :style="{ backgroundColor: getTagColor(tag) }"
        >
          {{ getTagName(tag) }}
        </div>
      </div>
    </div>
    
    <div class="contact-actions">
      <button 
        class="chat-button" 
        @click.stop="startChat"
        title="开始聊天"
        :disabled="contact.isBlocked"
      >
        <span class="icon">💬</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { messageApi } from '@/api/message';

// 定义props
const props = defineProps({
  contact: {
    type: Object,
    required: true
  },
  currentUserId: {
    type: Number,
    required: true
  }
});

// 定义事件
const emit = defineEmits(['click', 'context-menu', 'start-chat', 'chat-error']);

const router = useRouter();
const isLoading = ref(false);
const error = ref<string | null>(null);

// 获取名字首字母作为头像
const getInitials = (name: string): string => {
  if (!name) return '?';
  return name.charAt(0).toUpperCase();
};

// 获取标签名称
const getTagName = (tag: any): string => {
  if (typeof tag === 'object') {
    return tag.name || '未命名';
  } else {
    // 如果标签是ID，尝试从联系人的完整标签数据中查找
    if (props.contact.fullTags && Array.isArray(props.contact.fullTags)) {
      const tagObj = props.contact.fullTags.find((t: any) => t.id === tag || t.tagId === tag);
      return tagObj ? tagObj.name : '未命名';
    }
    return '未命名';
  }
}

// 获取标签颜色
const getTagColor = (tag: any): string => {
  if (typeof tag === 'object') {
    return tag.color || '#667eea';
  } else {
    // 如果标签是ID，尝试从联系人的完整标签数据中查找
    if (props.contact.fullTags && Array.isArray(props.contact.fullTags)) {
      const tagObj = props.contact.fullTags.find((t: any) => t.id === tag || t.tagId === tag);
      return tagObj ? tagObj.color : '#667eea';
    }
    return '#667eea';
  }
}

// 处理点击事件
const handleClick = () => {
  emit('click', props.contact);
};

// 开始聊天
const startChat = async (event: MouseEvent) => {
  event.stopPropagation();
  
  if (isLoading.value) return;
  
  try {
    isLoading.value = true;
    error.value = null;
    
    // 打印联系人信息以便调试
    console.log('联系人信息:', props.contact);
    
    // 尝试从不同的属性中获取联系人ID
    let rawContactId = props.contact.id;
    
    // 如果id为undefined，尝试从其他属性获取
    if (rawContactId === undefined) {
      if (props.contact.friendId !== undefined) {
        rawContactId = props.contact.friendId;
        console.log('使用friendId作为联系人ID:', rawContactId);
      } else if (props.contact.rawData && props.contact.rawData.id !== undefined) {
        rawContactId = props.contact.rawData.id;
        console.log('使用rawData.id作为联系人ID:', rawContactId);
      } else if (props.contact.rawData && props.contact.rawData.friendId !== undefined) {
        rawContactId = props.contact.rawData.friendId;
        console.log('使用rawData.friendId作为联系人ID:', rawContactId);
      } else if (props.contact.friend && props.contact.friend.id !== undefined) {
        rawContactId = props.contact.friend.id;
        console.log('使用friend.id作为联系人ID:', rawContactId);
      } else {
        throw new Error('无法获取有效的联系人ID');
      }
    }
    
    console.log(`开始与联系人聊天，ID: ${rawContactId}，ID类型:`, typeof rawContactId);
    
    // 确保ID是数字类型
    let contactId: number;
    if (typeof rawContactId === 'string') {
      contactId = parseInt(rawContactId, 10);
      if (isNaN(contactId)) {
        throw new Error(`无效的联系人ID: ${rawContactId}`);
      }
    } else if (typeof rawContactId === 'number') {
      contactId = rawContactId;
    } else {
      throw new Error(`无效的联系人ID类型: ${typeof rawContactId}`);
    }
    
    console.log('处理后的联系人ID:', contactId, '类型:', typeof contactId);
    
    // 调用API获取或创建私聊会话
    const response = await messageApi.getOrCreatePrivateConversation(
      contactId,
      props.currentUserId
    );
    
    if (response.success && response.data) {
      console.log('成功获取或创建私聊会话:', response.data);
      
      // 获取会话ID
      let conversationId: number | undefined;
      
      // 处理不同的响应结构
      if (response.data.id) {
        // 直接返回了会话对象
        conversationId = response.data.id;
      } else if (response.data.conversation && response.data.conversation.id) {
        // 返回了包装的会话对象
        conversationId = response.data.conversation.id;
      } else if (typeof response.data === 'number') {
        // 直接返回了ID
        conversationId = response.data;
      }
      
      if (conversationId) {
        console.log('提取到会话ID:', conversationId);
        // 触发事件通知父组件
        emit('start-chat', {
          contact: props.contact,
          conversationId: conversationId
        });
      } else {
        console.error('无法从响应中提取会话ID:', response.data);
        throw new Error('无法获取会话ID');
      }
    } else {
      throw new Error(response.message || '创建会话失败');
    }
  } catch (err: any) {
    console.error('开始聊天失败:', err);
    error.value = err.message || '开始聊天失败';
    emit('chat-error', error.value);
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.contact-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.contact-item:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.avatar-container {
  position: relative;
  margin-right: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.text-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #3498db;
  color: white;
  font-weight: bold;
  font-size: 16px;
}

.online-indicator {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #2ecc71;
  border: 2px solid white;
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-name {
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.contact-signature {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.contact-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.contact-tag {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: bold;
  color: white;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 80px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.contact-actions {
  display: flex;
  align-items: center;
}

.chat-button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.chat-button:hover {
  background-color: rgba(0, 0, 0, 0.1);
}

.chat-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.contact-item.blocked {
  background-color: rgba(0, 0, 0, 0.03);
  opacity: 0.8;
  border-left: 3px solid #e74c3c;
}

.blocked-indicator {
  font-size: 12px;
  color: #e74c3c;
  background-color: rgba(231, 76, 60, 0.1);
  border-radius: 4px;
  padding: 2px 6px;
  margin-left: 6px;
}

.icon {
  font-size: 18px;
}
</style> 