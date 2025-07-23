<template>
  <div v-if="visible" class="settings-dialog">
    <div class="settings-dialog-backdrop" @click="closeDialog"></div>
    <div class="settings-dialog-container">
      <div class="settings-dialog-header">
        <h2>界面个性化</h2>
        <button class="close-btn" @click="closeDialog">×</button>
      </div>
      
      <div class="settings-dialog-content">
        <!-- 界面个性化设置 -->
        <AppearanceSettings @close="closeDialog" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppearanceSettings from './AppearanceSettings.vue';

// 定义属性
const props = defineProps<{
  visible: boolean;
}>();

// 定义事件
const emit = defineEmits<{
  (e: 'close'): void;
}>();

// 关闭对话框
const closeDialog = () => {
  emit('close');
};
</script>

<style scoped>
.settings-dialog {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.settings-dialog-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(3px);
}

.settings-dialog-container {
  position: relative;
  width: 80%;
  max-width: 800px;
  height: 80%;
  max-height: 600px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.settings-dialog-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.settings-dialog-header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s;
}

.close-btn:hover {
  background-color: #f5f5f5;
  color: #333;
}

.settings-dialog-content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}
</style> 