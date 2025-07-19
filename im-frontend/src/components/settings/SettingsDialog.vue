<template>
  <div v-if="visible" class="settings-dialog">
    <div class="settings-dialog-backdrop" @click="closeDialog"></div>
    <div class="settings-dialog-container">
      <div class="settings-dialog-header">
        <h2>设置</h2>
        <button class="close-btn" @click="closeDialog">×</button>
      </div>
      
      <div class="settings-dialog-tabs">
        <div 
          v-for="tab in tabs" 
          :key="tab.id"
          :class="['tab-item', { active: activeTab === tab.id }]"
          :data-tab="tab.id"
          @click="activeTab = tab.id"
        >
          {{ tab.name }}
        </div>
      </div>
      
      <div class="settings-dialog-content">
        <!-- 界面个性化设置 -->
        <div v-if="activeTab === 'appearance'" class="tab-content">
          <AppearanceSettings @close="closeDialog" />
        </div>
        
        <!-- 隐私设置 -->
        <div v-else-if="activeTab === 'privacy'" class="tab-content">
          <h3>隐私设置</h3>
          <p>此功能正在开发中...</p>
        </div>
        
        <!-- 通知设置 -->
        <div v-else-if="activeTab === 'notifications'" class="tab-content">
          <h3>通知设置</h3>
          <p>此功能正在开发中...</p>
        </div>
        
        <!-- 关于 -->
        <div v-else-if="activeTab === 'about'" class="tab-content">
          <h3>关于</h3>
          <div class="about-content">
            <div class="app-logo">IM</div>
            <div class="app-name">IM通信系统</div>
            <div class="app-version">版本 1.0.0</div>
            <div class="app-copyright">© 2023-2025 IM团队</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import AppearanceSettings from './AppearanceSettings.vue';

// 定义属性
const props = defineProps<{
  visible: boolean;
}>();

// 定义事件
const emit = defineEmits<{
  (e: 'close'): void;
}>();

// 标签页
const tabs = [
  { id: 'appearance', name: '界面个性化' },
  { id: 'privacy', name: '隐私设置' },
  { id: 'notifications', name: '通知设置' },
  { id: 'about', name: '关于' }
];

// 当前激活的标签页
const activeTab = ref('appearance');

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

.settings-dialog-tabs {
  display: flex;
  border-bottom: 1px solid #e8e8e8;
}

.tab-item {
  padding: 12px 16px;
  cursor: pointer;
  color: #666;
  font-size: 14px;
  position: relative;
  transition: all 0.3s;
}

.tab-item:hover {
  color: var(--primary-color, #1890ff);
}

.tab-item.active {
  color: var(--primary-color, #1890ff);
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background-color: var(--primary-color, #1890ff);
}

.settings-dialog-content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.tab-content {
  padding: 0;
  height: 100%;
}

/* 关于页面样式 */
.about-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.app-logo {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  background-color: var(--primary-color, #1890ff);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: bold;
  margin-bottom: 16px;
}

.app-name {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 8px;
}

.app-version {
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
}

.app-copyright {
  font-size: 12px;
  color: #999;
}
</style> 