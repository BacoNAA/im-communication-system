<template>
  <div class="media-library-button-container">
    <!-- 媒体库按钮 -->
    <button 
      class="media-library-button" 
      :class="{ 'toolbar-style': isToolbarStyle }"
      @click="openMediaLibrary"
      :title="isOpen ? '关闭媒体库' : '打开媒体库'"
    >
      <i class="fas fa-photo-video"></i>
      <span v-if="!isToolbarStyle">媒体库</span>
    </button>
    
    <!-- 媒体库模态框 -->
    <div v-if="isOpen" class="media-library-modal">
      <div class="modal-header">
        <h3>媒体库</h3>
        <button class="close-button" @click="closeMediaLibrary">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="modal-body">
        <MediaLibrary :conversation-id="conversationId" />
      </div>
    </div>
    
    <!-- 遮罩层 -->
    <div v-if="isOpen" class="modal-backdrop" @click="closeMediaLibrary"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import MediaLibrary from './MediaLibrary.vue';

// 定义组件属性
const props = defineProps({
  // 会话ID
  conversationId: {
    type: Number,
    required: true
  },
  // 是否使用工具栏样式
  toolbarStyle: {
    type: Boolean,
    default: false
  }
});

// 组件状态
const isOpen = ref(false);
const isToolbarStyle = computed(() => props.toolbarStyle);

// 打开媒体库
const openMediaLibrary = () => {
  isOpen.value = true;
  document.body.classList.add('modal-open'); // 防止背景滚动
};

// 关闭媒体库
const closeMediaLibrary = () => {
  isOpen.value = false;
  document.body.classList.remove('modal-open');
};
</script>

<style scoped>
.media-library-button-container {
  position: relative;
}

.media-library-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: none;
  border-radius: 4px;
  background-color: transparent;
  color: #616161;
  cursor: pointer;
  transition: background-color 0.2s;
}

.media-library-button:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.media-library-button i {
  font-size: 16px;
}

/* 工具栏样式 */
.media-library-button.toolbar-style {
  padding: 8px;
  font-size: 18px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media-library-button.toolbar-style i {
  font-size: 18px;
}

.media-library-modal {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 90%;
  max-width: 900px;
  height: 80vh;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1001;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.close-button {
  background: none;
  border: none;
  font-size: 20px;
  color: #757575;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  width: 32px;
  height: 32px;
}

.close-button:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.modal-body {
  flex: 1;
  overflow: hidden;
}

.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 1000;
}

/* 防止背景滚动 */
:global(body.modal-open) {
  overflow: hidden;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .media-library-modal {
    width: 100%;
    height: 100%;
    max-width: none;
    border-radius: 0;
  }
}
</style> 