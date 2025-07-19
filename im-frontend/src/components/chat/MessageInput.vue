<template>
  <div class="message-input-container">
    <div class="message-toolbar">
      <button 
        class="toolbar-button emoji-button"
        @click.stop="toggleEmojiPicker"
        title="表情"
      >
        😊
      </button>
      <button 
        class="toolbar-button"
        @click="triggerImageUpload"
        title="发送图片"
      >
        🖼️
      </button>
      <button 
        class="toolbar-button"
        @click="triggerVideoUpload"
        title="发送视频"
      >
        🎥
      </button>
      <button 
        class="toolbar-button"
        @click="triggerFileUpload"
        title="发送文件"
      >
        📎
      </button>
      <MediaLibraryButton :conversation-id="Number(props.conversationId)" :toolbar-style="true" />
      <!-- 隐藏的文件上传输入框 -->
      <input 
        type="file" 
        ref="imageInputRef" 
        style="display: none" 
        accept="image/*" 
        @change="handleImageSelected" 
      />
      <input 
        type="file" 
        ref="videoInputRef" 
        style="display: none" 
        accept="video/*" 
        @change="handleVideoSelected" 
      />
      <input 
        type="file" 
        ref="fileInputRef" 
        style="display: none" 
        accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar" 
        @change="handleFileSelected" 
      />
    </div>
    
    <!-- 媒体预览区域 -->
    <div v-if="previewMedia" class="media-preview">
      <div class="preview-header">
        <span>{{ 
          previewMedia.type === 'image' ? '图片预览' : 
          previewMedia.type === 'video' ? '视频预览' : '文件预览' 
        }}</span>
        <button class="close-preview" @click="cancelMediaUpload">×</button>
      </div>
      <div class="preview-content">
        <img v-if="previewMedia.type === 'image'" :src="previewMedia.url" class="preview-image" />
        <video v-else-if="previewMedia.type === 'video'" :src="previewMedia.url" controls class="preview-video"></video>
        <div v-else-if="previewMedia.type === 'file'" class="preview-file">
          <div class="file-icon">📄</div>
          <div class="file-info">
            <div class="file-name">{{ previewMedia.file.name }}</div>
            <div class="file-size">{{ formatFileSize(previewMedia.file.size) }}</div>
          </div>
        </div>
      </div>
      <div class="debug-info">
        <div>{{ previewMedia.file.name }} ({{ formatFileSize(previewMedia.file.size) }})</div>
        <div class="debug-status">媒体类型: {{ previewMedia.type }} | ID: {{ previewMedia.file.lastModified }}</div>
      </div>
    </div>
    
    <div class="input-area">
      <textarea
        ref="inputRef"
        v-model="messageText"
        class="message-textarea"
        placeholder="输入消息..."
        @keydown.enter.prevent="handleEnterKey"
      ></textarea>
    </div>
    
    <div class="message-actions">
      <button 
        class="send-button"
        :disabled="!canSend"
        @click="sendMessage"
      >
        发送
      </button>
    </div>
    
    <!-- 表情选择器 -->
    <div 
      v-if="showEmojiPicker" 
      class="emoji-picker-container"
      v-click-outside="closeEmojiPicker"
    >
      <div class="emoji-picker-debug">表情选择器已打开</div>
      <EmojiPicker :onSelect="insertEmoji" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed, watch } from 'vue';
import EmojiPicker from './EmojiPicker.vue';
import { MessageType } from '@/api/message';
import { messageApi } from '@/api';
import { useWebSocket, DEFAULT_WS_URL } from '@/composables/useWebSocket';
import MediaLibraryButton from './MediaLibraryButton.vue';

// 定义props
const props = defineProps({
  conversationId: {
    type: [Number, String],
    required: true
  },
  disabled: {
    type: Boolean,
    default: false
  }
});

// 定义事件
const emit = defineEmits(['send-message']);

// 消息文本
const messageText = ref('');
const inputRef = ref<HTMLTextAreaElement | null>(null);
const showEmojiPicker = ref(false);
const imageInputRef = ref<HTMLInputElement | null>(null);
const videoInputRef = ref<HTMLInputElement | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);

// WebSocket连接
const { 
  status: wsStatus, 
  send: wsSend, 
  isConnected: wsConnected 
} = useWebSocket(DEFAULT_WS_URL);

// 输入状态变量
const isTyping = ref(false);
const typingTimeout = ref<number | null>(null);

// 监听输入变化，发送输入状态
watch(messageText, (newValue) => {
  // 只有当文本不为空时才发送输入状态
  if (newValue.trim() !== '') {
    if (!isTyping.value) {
      // 状态变为正在输入
      isTyping.value = true;
      sendTypingStatus(true);
    }
    
    // 重置超时
    if (typingTimeout.value !== null) {
      clearTimeout(typingTimeout.value);
    }
    
    // 设置新的超时 - 3秒后停止输入状态
    typingTimeout.value = window.setTimeout(() => {
      isTyping.value = false;
      sendTypingStatus(false);
    }, 3000);
  } else {
    // 文本为空，停止输入状态
    if (isTyping.value) {
      isTyping.value = false;
      sendTypingStatus(false);
      
      // 清除超时
      if (typingTimeout.value !== null) {
        clearTimeout(typingTimeout.value);
        typingTimeout.value = null;
      }
    }
  }
});

// 发送输入状态
const sendTypingStatus = (isTyping: boolean) => {
  if (wsConnected.value && props.conversationId) {
    console.log(`发送输入状态: ${isTyping ? '正在输入' : '停止输入'}`);
    
    wsSend({
      type: 'typing',
      data: {
        conversationId: Number(props.conversationId),
        isTyping: isTyping
      }
    });
  }
};

// 媒体预览
interface PreviewMedia {
  type: 'image' | 'video' | 'file';
  file: any; // 使用any类型避免TypeScript错误
  url: string;
}
const previewMedia = ref<PreviewMedia | null>(null);

// 判断是否可以发送消息
const canSend = computed(() => {
  return messageText.value.trim() !== '' || previewMedia.value !== null;
});

// 切换表情选择器显示状态
const toggleEmojiPicker = (event?: Event) => {
  if (event) {
    event.preventDefault();
    event.stopPropagation();
  }
  showEmojiPicker.value = !showEmojiPicker.value;
  console.log('表情选择器状态切换:', showEmojiPicker.value);
};

// 关闭表情选择器
const closeEmojiPicker = () => {
  console.log('关闭表情选择器');
  showEmojiPicker.value = false;
};

// 插入表情
const insertEmoji = (emoji: string) => {
  console.log('插入表情:', emoji);
  // 获取当前光标位置
  const textarea = inputRef.value;
  if (!textarea) return;
  
  const start = textarea.selectionStart || 0;
  const end = textarea.selectionEnd || 0;
  
  // 在光标位置插入表情
  messageText.value = messageText.value.substring(0, start) + emoji + messageText.value.substring(end);
  
  // 更新光标位置
  nextTick(() => {
    const newPosition = start + emoji.length;
    textarea.focus();
    textarea.setSelectionRange(newPosition, newPosition);
  });
};

// 处理回车键
const handleEnterKey = (event: KeyboardEvent) => {
  // Shift+Enter 换行，单独的Enter发送消息
  if (!event.shiftKey) {
    sendMessage();
  } else {
    // 在光标位置插入换行符
    const textarea = inputRef.value;
    if (!textarea) return;
    
    const start = textarea.selectionStart || 0;
    const end = textarea.selectionEnd || 0;
    
    messageText.value = messageText.value.substring(0, start) + '\n' + messageText.value.substring(end);
    
    // 更新光标位置
    nextTick(() => {
      const newPosition = start + 1;
      textarea.focus();
      textarea.setSelectionRange(newPosition, newPosition);
    });
  }
};

// 触发图片上传
const triggerImageUpload = () => {
  if (imageInputRef.value) {
    imageInputRef.value.click();
  }
};

// 触发视频上传
const triggerVideoUpload = () => {
  if (videoInputRef.value) {
    videoInputRef.value.click();
  }
};

// 触发文件上传
const triggerFileUpload = () => {
  if (fileInputRef.value) {
    fileInputRef.value.click();
  }
};

// 处理图片选择
const handleImageSelected = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    const file = input.files[0];
    
    // 检查文件是否存在
    if (!file) {
      alert('未能获取文件');
      return;
    }
    
    // 检查文件大小 (限制为10MB)
    if (file.size > 10 * 1024 * 1024) {
      alert('图片大小不能超过10MB');
      return;
    }
    
    // 创建预览URL
    // 使用类型断言解决TypeScript错误
    const url = URL.createObjectURL(file as unknown as Blob);
    previewMedia.value = {
      type: 'image',
      file: file as any,
      url
    };
    
    // 重置输入框，以便可以重新选择相同的文件
    input.value = '';
  }
};

// 处理视频选择
const handleVideoSelected = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    const file = input.files[0];
    
    // 检查文件是否存在
    if (!file) {
      alert('未能获取文件');
      return;
    }
    
    // 检查文件大小 (限制为50MB)
    if (file.size > 50 * 1024 * 1024) {
      alert('视频大小不能超过50MB');
      return;
    }
    
    // 创建预览URL
    // 使用类型断言解决TypeScript错误
    const url = URL.createObjectURL(file as unknown as Blob);
    previewMedia.value = {
      type: 'video',
      file: file as any,
      url
    };
    
    // 重置输入框，以便可以重新选择相同的文件
    input.value = '';
  }
};

// 处理文件选择
const handleFileSelected = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    const file = input.files[0];
    
    // 检查文件是否存在
    if (!file) {
      alert('未能获取文件');
      return;
    }
    
    // 检查文件大小 (限制为10MB)
    if (file.size > 10 * 1024 * 1024) {
      alert('文件大小不能超过10MB');
      return;
    }
    
    // 创建预览URL
    // 使用类型断言解决TypeScript错误
    const url = URL.createObjectURL(file as unknown as Blob);
    previewMedia.value = {
      type: 'file',
      file: file as any,
      url
    };
    
    // 重置输入框，以便可以重新选择相同的文件
    input.value = '';
  }
};

// 取消媒体上传
const cancelMediaUpload = () => {
  if (previewMedia.value) {
    URL.revokeObjectURL(previewMedia.value.url);
    previewMedia.value = null;
  }
};

// 上传媒体文件
const uploadMediaFile = async (file: any): Promise<string> => {
  try {
    console.log('开始上传文件，会话ID:', props.conversationId);
    
    // 使用messageApi上传文件
    const response = await messageApi.uploadMedia(file, Number(props.conversationId));
    
    // 检查响应数据是否存在
    if (!response || !response.success) {
      console.error('上传失败，响应:', response);
      throw new Error(response?.message || '上传失败');
    }
    
    // 检查响应数据
    if (!response.data) {
      console.error('响应数据为空:', response);
      throw new Error('服务器返回的数据为空');
    }
    
    console.log('文件上传成功，响应数据:', response.data);
    
    // 从响应数据中获取URL
    let fileUrl = '';
    const data = response.data;
    
    if (data.url) {
      // 如果直接返回了url字段
      fileUrl = data.url;
    } else if (data.fileUrl) {
      // 如果返回了fileUrl字段
      fileUrl = data.fileUrl;
    } else if (data.fileName) {
      // 如果返回了fileName，则构建URL
      // 这里假设文件可以通过/api/files/访问
      fileUrl = `/api/files/${data.fileName}`;
    } else {
      console.error('响应数据中没有URL信息:', data);
      throw new Error('服务器返回的数据格式不正确：缺少URL信息');
    }
    
    console.log('文件上传成功，URL:', fileUrl);
    // 返回文件URL
    return fileUrl;
  } catch (error) {
    console.error('上传文件失败:', error);
    
    // 根据错误类型提供更具体的错误信息
    if (error instanceof Error) {
      if (error.message === 'Failed to fetch' || error.message.includes('network')) {
        throw new Error('网络连接失败，请检查您的网络连接或服务器状态');
      } else {
        throw error;
      }
    } else {
      throw new Error('上传过程中发生未知错误');
    }
  }
};

// 发送消息
const sendMessage = async () => {
  try {
    let messageType = MessageType.TEXT;
    let content = messageText.value.trim();
    let mediaFileId = null;
    
    // 如果有媒体文件，先上传
    if (previewMedia.value) {
      try {
        console.log('开始上传媒体文件:', previewMedia.value.file.name, '类型:', previewMedia.value.type);
        
        // 上传媒体文件
        const response = await messageApi.uploadMedia(previewMedia.value.file, Number(props.conversationId));
        
        if (!response || !response.success || !response.data) {
          throw new Error(response?.message || '上传失败');
        }
        
        console.log('文件上传成功，响应数据:', response.data);
        
        // 获取媒体文件ID和URL
        // 尝试从多个可能的字段中获取ID
        mediaFileId = response.data.id || response.data.mediaFileId;
        
        if (!mediaFileId) {
          console.error('响应数据中没有媒体文件ID:', response.data);
          throw new Error('服务器返回的数据格式不正确：缺少媒体文件ID');
        }
        
        // 确保mediaFileId是数字类型
        mediaFileId = Number(mediaFileId);
        
        console.log('获取到媒体文件ID:', mediaFileId, '类型:', typeof mediaFileId);
        
        // 设置消息类型
        if (previewMedia.value.type === 'image') {
          messageType = MessageType.IMAGE;
          // 使用文件名作为内容，但不会在气泡中显示
          content = previewMedia.value.file.name;
          console.log('设置图片消息内容为文件名:', content);
        } else if (previewMedia.value.type === 'video') {
          messageType = MessageType.VIDEO;
          // 使用文件名作为内容，但不会在气泡中显示
          content = previewMedia.value.file.name;
          console.log('设置视频消息内容为文件名:', content);
        } else if (previewMedia.value.type === 'file') {
          messageType = MessageType.FILE;
          // 对于文件消息，使用文件名作为内容
          content = previewMedia.value.file.name;
          console.log('设置文件消息内容为文件名:', content);
        }
      } catch (error) {
        const mediaTypeText = previewMedia.value.type === 'image' ? '图片' : 
                             previewMedia.value.type === 'video' ? '视频' : '文件';
        alert(`上传${mediaTypeText}失败: ${error instanceof Error ? error.message : '未知错误'}`);
        return;
      }
    } else if (!content) {
      return;
    }
    
    console.log('准备发送消息:', {
      content,
      type: messageType,
      mediaFileId
    });
    
    // 通过事件通知父组件发送消息，不再直接调用API
    emit('send-message', {
      content: content,
      type: messageType,
      mediaFileId: mediaFileId
    });
    
    // 清空输入框和预览
    messageText.value = '';
    if (previewMedia.value) {
      URL.revokeObjectURL(previewMedia.value.url);
      previewMedia.value = null;
    }
    
    // 关闭表情选择器
    closeEmojiPicker();
  } catch (error) {
    console.error('发送消息失败:', error);
    alert('发送消息失败，请稍后重试');
  }
};

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// 组件挂载时聚焦输入框
onMounted(() => {
  console.log('MessageInput 组件已挂载');
  if (inputRef.value) {
    inputRef.value.focus();
  }
});

// 组件卸载时清理
const cleanupResources = () => {
  if (previewMedia.value) {
    URL.revokeObjectURL(previewMedia.value.url);
  }
};
</script>

<style scoped>
.message-input-container {
  position: relative;
  display: flex;
  flex-direction: column;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background-color: #fff;
  padding: 8px;
  width: 100%;
}

.message-toolbar {
  display: flex;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.toolbar-button {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.toolbar-button:hover {
  background-color: #f0f0f0;
}

.emoji-button {
  margin-right: 8px;
  font-size: 20px;
  color: #1890ff;
}

.emoji-button:hover {
  transform: scale(1.1);
}

.input-area {
  flex: 1;
  min-height: 60px;
  padding: 8px 0;
  width: 100%;
}

.message-textarea {
  width: 100%;
  height: 100%;
  min-height: 60px;
  resize: none;
  border: none;
  outline: none;
  font-size: 14px;
  line-height: 1.5;
  font-family: inherit;
  box-sizing: border-box;
}

.message-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.send-button {
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 6px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.send-button:hover {
  background-color: #40a9ff;
}

.send-button:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

.emoji-picker-container {
  position: fixed;
  bottom: auto;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  margin-bottom: 0;
  z-index: 9999;
  background-color: white;
  border: 2px solid #1890ff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.emoji-picker-debug {
  padding: 8px;
  background-color: #f0f8ff;
  color: #1890ff;
  text-align: center;
  font-weight: bold;
  border-bottom: 1px solid #e6f7ff;
}

/* 媒体预览样式 */
.media-preview {
  margin: 8px 0;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
  max-width: 250px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background-color: #f5f5f5;
  border-bottom: 1px solid #e0e0e0;
}

.close-preview {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #999;
}

.close-preview:hover {
  color: #ff4d4f;
}

.preview-content {
  padding: 8px;
  display: flex;
  justify-content: center;
  background-color: #fafafa;
}

.preview-image {
  max-width: 150px;
  max-height: 120px;
  object-fit: contain;
}

.preview-video {
  max-width: 150px;
  max-height: 120px;
}

.preview-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background-color: #e0e0e0;
  border-radius: 4px;
}

.file-icon {
  font-size: 24px;
  color: #555;
}

.file-info {
  display: flex;
  flex-direction: column;
}

.file-name {
  font-weight: bold;
  color: #333;
}

.file-size {
  font-size: 12px;
  color: #666;
}

.debug-info {
  padding: 8px;
  background-color: #f0f0f0;
  border-top: 1px solid #e0e0e0;
  font-size: 12px;
  color: #555;
}

.debug-status {
  margin-top: 4px;
  font-size: 10px;
  color: #888;
  font-style: italic;
}
</style> 