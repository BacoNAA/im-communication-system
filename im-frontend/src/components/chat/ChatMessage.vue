<template>
  <div class="message-wrapper" :class="{ 'message-self': message.isSelf, 'message-other': !message.isSelf }">
    <!-- 消息容器 -->
    <div class="message-container">
      <!-- 左侧头像 - 仅对方消息显示 -->
      <template v-if="!message.isSelf">
        <div class="avatar-container left">
          <img class="avatar" :src="userAvatar" :alt="message.senderName || '用户'" />
        </div>
        
        <!-- 消息内容 -->
        <div class="message-content-wrapper">
          <!-- 发送者名称 - 仅对方消息且需要显示名称时 -->
          <div class="sender-name" v-if="showSenderName">
            {{ message.senderName || '用户' }}
          </div>
          
          <!-- 消息气泡 -->
          <div class="message-bubble other-bubble">
            <!-- 文本消息 -->
            <template v-if="isTextMessage">
              <span v-html="formatTextWithEmojis(displayContent)"></span>
            </template>
            
            <!-- 图片消息 -->
            <template v-else-if="isImageMessage">
              <img class="message-image" :src="message.content" @click="previewImage(message.content)" alt="图片消息" />
            </template>
            
            <!-- 文件消息 -->
            <template v-else-if="isFileMessage">
              <div class="file-message">
                <i class="fas fa-file"></i>
                <span class="file-name">{{ getFileName(message.content) }}</span>
                <button class="download-btn" @click="downloadFile(message.content)">
                  <i class="fas fa-download"></i>
                </button>
              </div>
            </template>
            
            <!-- 不支持的消息类型 -->
            <template v-else>
              <div class="unsupported-message">不支持的消息类型: {{ message.type }}</div>
            </template>
          </div>
          
          <!-- 消息时间 -->
          <div class="message-time time-other">
            {{ formatMessageTime(message.createdAt) }}
          </div>
        </div>
      </template>
      
      <!-- 自己的消息 -->
      <template v-else>
        <!-- 消息内容 -->
        <div class="message-content-wrapper">
          <!-- 消息气泡 -->
          <div class="message-bubble self-bubble">
            <!-- 文本消息 -->
            <template v-if="isTextMessage">
              <span v-html="formatTextWithEmojis(displayContent)"></span>
            </template>
            
            <!-- 图片消息 -->
            <template v-else-if="isImageMessage">
              <img class="message-image" :src="message.content" @click="previewImage(message.content)" alt="图片消息" />
            </template>
            
            <!-- 文件消息 -->
            <template v-else-if="isFileMessage">
              <div class="file-message">
                <i class="fas fa-file"></i>
                <span class="file-name">{{ getFileName(message.content) }}</span>
                <button class="download-btn" @click="downloadFile(message.content)">
                  <i class="fas fa-download"></i>
                </button>
              </div>
            </template>
            
            <!-- 不支持的消息类型 -->
            <template v-else>
              <div class="unsupported-message">不支持的消息类型: {{ message.type }}</div>
            </template>
          </div>
          
          <!-- 消息时间 -->
          <div class="message-time time-self">
            {{ formatMessageTime(message.createdAt) }}
          </div>
          
          <!-- 消息状态 - 仅自己的消息 -->
          <div class="message-status">
            <span v-if="message.status === 'SENDING'" class="status-sending">发送中...</span>
            <span v-else-if="message.status === 'FAILED'" class="status-failed">
              发送失败
              <button class="retry-btn" @click="retryMessage">重试</button>
            </span>
            <span v-else-if="message.status === 'READ'" class="status-read">已读</span>
          </div>
        </div>
        
        <!-- 右侧头像 - 自己的消息 -->
        <div class="avatar-container right">
          <img class="avatar" :src="userAvatar" alt="我" />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineProps, defineEmits, onMounted } from 'vue';

interface MessageProps {
  id: string | number;
  content: string;
  type?: string;
  senderId?: number | string;
  senderName?: string;
  senderAvatar?: string;
  createdAt: string;
  status?: string;
  isSelf: boolean;
}

const props = defineProps<{
  message: MessageProps;
  currentUserAvatar?: string;
  showSenderName?: boolean;
}>();

const emit = defineEmits<{
  (e: 'retry', messageId: string | number): void;
}>();

// 默认头像
const defaultAvatar = '/favicon.ico'; // 使用项目中的本地图标作为默认头像

// 计算属性：用户头像
const userAvatar = computed(() => {
  if (props.message.isSelf) {
    // 如果是自己的消息，优先使用传入的currentUserAvatar
    return props.currentUserAvatar || defaultAvatar;
  } else {
    // 如果是对方的消息，使用消息中的senderAvatar
    return props.message.senderAvatar || defaultAvatar;
  }
});

// 调试头像
onMounted(() => {
  if (props.message.isSelf) {
    console.log('自己的消息头像信息:');
    console.log('- 传入的currentUserAvatar:', props.currentUserAvatar);
    console.log('- 消息中的senderAvatar:', props.message.senderAvatar);
    console.log('- 最终使用的头像:', userAvatar.value);
  }
});

// 计算消息类型
const messageType = computed(() => {
  return (props.message.type || '').toUpperCase();
});

const isTextMessage = computed(() => {
  return messageType.value === 'TEXT';
});

const isImageMessage = computed(() => {
  return messageType.value === 'IMAGE';
});

const isFileMessage = computed(() => {
  return messageType.value === 'FILE';
});

// 处理消息内容显示
const displayContent = computed(() => {
  const content = props.message.content;
  
  // 检查内容是否为空
  if (content === undefined || content === null) {
    console.warn('消息内容为空:', props.message.id);
    return '(空消息)';
  }
  
  // 检查是否为对象或数组（可能是JSON字符串被解析为对象）
  if (typeof content === 'object') {
    try {
      console.log('消息内容是对象:', content);
      return JSON.stringify(content);
    } catch (e) {
      console.error('消息内容序列化失败:', e);
      return '(无法显示的内容)';
    }
  }
  
  // 检查是否为空字符串
  if (content === '') {
    console.warn('消息内容为空字符串:', props.message.id);
    return '(空消息)';
  }
  
  return content;
});

// 调试消息内容
onMounted(() => {
  console.log('ChatMessage组件挂载，消息内容:', props.message);
  console.log('消息内容类型:', typeof props.message.content);
  console.log('消息内容值:', props.message.content);
  console.log('消息类型:', messageType.value);
  console.log('是否为文本消息:', isTextMessage.value);
  console.log('显示内容:', displayContent.value);
  console.log('消息是否由当前用户发送 (isSelf):', props.message.isSelf);
  console.log('消息样式类:', {
    'message-self': props.message.isSelf,
    'message-other': !props.message.isSelf
  });
  
  // 调试头像信息
  console.log('消息发送者头像:', props.message.senderAvatar);
  console.log('当前用户头像:', props.currentUserAvatar);
  console.log('计算后的头像URL:', userAvatar.value);
  
  // 检查头像URL是否有效
  if (userAvatar.value && userAvatar.value !== defaultAvatar) {
    const img = new Image();
    img.onload = () => console.log('头像加载成功:', userAvatar.value);
    img.onerror = () => console.error('头像加载失败:', userAvatar.value);
    img.src = userAvatar.value;
  }
});

// 方法
const formatMessageTime = (timeString: string): string => {
  try {
    const date = new Date(timeString);
    const now = new Date();
    const isToday = date.toDateString() === now.toDateString();
    
    if (isToday) {
      // 今天的消息只显示时间
      return date.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
      });
    } else {
      // 非今天的消息显示日期和时间
      return date.toLocaleString('zh-CN', { 
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit', 
        minute: '2-digit' 
      });
    }
  } catch (error) {
    console.error('格式化消息时间出错:', error);
    return '';
  }
};

const getFileName = (fileUrl: string): string => {
  // 如果URL为空，返回默认文件名
  if (!fileUrl || typeof fileUrl !== 'string') {
    return '文件';
  }
  
  try {
    // 从URL中提取文件名
    const urlParts = fileUrl.split('/');
    const lastPart = urlParts[urlParts.length - 1];
    
    // 如果最后一部分为空，返回默认文件名
    if (!lastPart) {
      return '文件';
    }
    
    // 移除查询参数
    const fileNameParts = lastPart.split('?');
    const fileName = fileNameParts[0];
    
    // 如果文件名为空，返回默认文件名
    if (!fileName) {
      return '文件';
    }
    
    // 解码URL编码的字符
    return decodeURIComponent(fileName);
  } catch (error) {
    console.error('获取文件名出错:', error);
    return '文件';
  }
};

const downloadFile = (fileUrl: string) => {
  try {
    if (!fileUrl) {
      alert('文件链接无效');
      return;
    }
    
    // 创建一个隐藏的a标签来下载文件
    const link = document.createElement('a');
    link.href = fileUrl;
    link.download = getFileName(fileUrl) || '下载文件';
    link.target = '_blank';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (error) {
    console.error('下载文件出错:', error);
    alert('下载文件失败，请稍后重试');
  }
};

const previewImage = (imageUrl: string) => {
  // 这里可以实现图片预览功能
  // 例如打开一个模态框显示大图
  window.open(imageUrl, '_blank');
};

const retryMessage = () => {
  emit('retry', props.message.id);
};

const formatTextWithEmojis = (text: string): string => {
  if (!text) return '';
  
  // 使用正则表达式检测文本中是否包含表情符号
  const emojiRegex = /[\p{Emoji}]/gu;
  
  // 如果文本中包含表情符号，添加适当的样式
  if (emojiRegex.test(text)) {
    // 将文本中的表情符号包装在span标签中，以便应用样式
    return text.replace(emojiRegex, (match) => {
      return `<span class="emoji">${match}</span>`;
    });
  }
  
  // 如果没有表情符号，直接返回原文本
  return text;
};
</script>

<style scoped>
.message-wrapper {
  width: 100%;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
}

.message-container {
  display: flex;
  align-items: flex-start;
  width: 100%;
}

.avatar-container {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  position: relative;
}

.avatar-container.left {
  margin-right: 12px;
}

.avatar-container.right {
  margin-left: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e0e0e0;
}

.message-content-wrapper {
  flex: 1;
  max-width: calc(100% - 60px);
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.message-content-wrapper::after {
  content: "";
  display: table;
  clear: both;
}

.message-self .message-content-wrapper {
  align-items: flex-end;
}

.message-other .message-content-wrapper {
  align-items: flex-start;
}

/* 强化对方消息气泡边框 */
.other-bubble {
  background-color: #ffffff;
  border: 2px solid #555555;
  border-top-left-radius: 0;
  float: left;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  color: #333;
}

.other-bubble::after {
  content: '';
  position: absolute;
  top: 0;
  left: -8px;
  width: 0;
  height: 0;
  border-right: 8px solid #555555;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
}

.sender-name {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.emoji {
  font-size: 1.5em;
  line-height: 1;
  vertical-align: middle;
  display: inline-block;
  margin: 0 2px;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  max-width: 100%;
  word-break: break-word;
  position: relative;
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.5;
  display: inline-block;
  margin-bottom: 4px;
}

.self-bubble {
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
  border-top-right-radius: 0;
  float: right;
  color: #0050b3;
}

.self-bubble::after {
  content: '';
  position: absolute;
  top: 0;
  right: -8px;
  width: 0;
  height: 0;
  border-left: 8px solid #91d5ff;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.message-status {
  font-size: 11px;
  margin-top: 2px;
}

.status-sending {
  color: #999;
}

.status-failed {
  color: #ff4d4f;
}

.status-read {
  color: #52c41a;
}

.retry-btn {
  background: none;
  border: none;
  color: #1890ff;
  cursor: pointer;
  padding: 0;
  font-size: 11px;
  margin-left: 4px;
}

.retry-btn:hover {
  text-decoration: underline;
}

.message-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 4px;
  cursor: pointer;
}

.file-message {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.download-btn {
  background: none;
  border: none;
  color: #1890ff;
  cursor: pointer;
  padding: 4px;
}

.unsupported-message {
  color: #999;
  font-style: italic;
}
</style> 