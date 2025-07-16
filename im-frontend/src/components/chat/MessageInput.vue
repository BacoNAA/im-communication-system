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
      <!-- 可以在这里添加更多工具按钮，如图片上传、文件上传等 -->
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
        :disabled="!messageText.trim()"
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
import { ref, onMounted, nextTick } from 'vue';
import EmojiPicker from './EmojiPicker.vue';

// 定义props
const props = defineProps({
  conversationId: {
    type: [Number, String],
    required: true
  }
});

// 定义事件
const emit = defineEmits(['send-message']);

// 消息文本
const messageText = ref('');
const inputRef = ref<HTMLTextAreaElement | null>(null);
const showEmojiPicker = ref(false);

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

// 发送消息
const sendMessage = () => {
  const trimmedMessage = messageText.value.trim();
  if (!trimmedMessage) return;
  
  emit('send-message', {
    conversationId: props.conversationId,
    content: trimmedMessage,
    messageType: 'TEXT'
  });
  
  // 清空输入框
  messageText.value = '';
  
  // 关闭表情选择器
  closeEmojiPicker();
};

// 组件挂载时聚焦输入框
onMounted(() => {
  console.log('MessageInput 组件已挂载');
  if (inputRef.value) {
    inputRef.value.focus();
  }
});
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
</style> 