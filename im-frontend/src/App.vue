<script setup lang="ts">
import { onMounted } from 'vue';
import { RouterView } from 'vue-router';
import { useSharedWebSocket } from '@/composables/useWebSocket';
import { useUserSettings } from './composables/useUserSettings';
import { applyThemeColor, applyFontSize } from './utils/themeUtils';

// 初始化WebSocket连接
const { connect: connectWebSocket } = useSharedWebSocket();

// 获取用户设置
const { settings, fetchSettings } = useUserSettings();

// 应用存储的设置到全局样式
const applyGlobalSettings = () => {
  console.log('应用全局设置:', settings.value);
  
  if (settings.value?.theme) {
    // 应用主题颜色
    if (settings.value.theme.color) {
      console.log('应用主题颜色:', settings.value.theme.color);
      applyThemeColor(settings.value.theme.color);
    }
    
    // 应用字体大小
    if (settings.value.theme.fontSize) {
      console.log('应用字体大小:', settings.value.theme.fontSize);
      applyFontSize(settings.value.theme.fontSize);
    }
    
    // 应用聊天背景（如果需要）
    // 这部分可能需要在聊天组件内处理
  }
};

// 在应用初始化时检查用户是否已登录
onMounted(async () => {
  // 检查本地存储中是否有token，表示用户已登录
  const hasToken = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
  
  if (hasToken) {
    // 用户已登录，立即激活WebSocket连接
    console.log('检测到用户已登录，立即激活WebSocket连接');
    connectWebSocket();
  } else {
    console.log('用户未登录，WebSocket连接将在登录后激活');
  }

  console.log('App组件已挂载，开始加载用户设置');
  
  try {
    // 尝试从API获取设置
    await fetchSettings();
    console.log('成功从API获取设置');
  } catch (error) {
    console.error('从API获取设置失败，将使用本地存储的设置:', error);
  }
  
  // 无论是从API还是本地存储加载，都应用设置
  applyGlobalSettings();
});
</script>

<template>
  <RouterView />
</template>

<style>
/* 全局CSS变量 */
:root {
  --primary-color: #1890ff;
  --primary-light-color: rgba(24, 144, 255, 0.2);
  --primary-dark-color: #096dd9;
  --font-size-base: 14px;
  --font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: var(--font-family);
  font-size: var(--font-size-base);
  line-height: 1.5;
  color: #333;
  background: #f5f5f5;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  height: 100%;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 通用按钮样式 */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.3s ease;
  outline: none;
  user-select: none;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: #007bff;
  color: white;
  padding: 8px 16px;
}

.btn-primary:hover:not(:disabled) {
  background: #0056b3;
}

.btn-secondary {
  background: #6c757d;
  color: white;
  padding: 8px 16px;
}

.btn-secondary:hover:not(:disabled) {
  background: #545b62;
}

.btn-outline {
  background: transparent;
  color: #007bff;
  border: 1px solid #007bff;
  padding: 8px 16px;
}

.btn-outline:hover:not(:disabled) {
  background: #007bff;
  color: white;
}

.btn-icon {
  width: 32px;
  height: 32px;
  padding: 0;
  background: #f8f9fa;
  color: #6c757d;
}

.btn-icon:hover:not(:disabled) {
  background: #e9ecef;
  color: #495057;
}

/* 表单元素样式 */
.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
  color: #495057;
  background: #fff;
  transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
}

.form-control:focus {
  border-color: #80bdff;
  outline: 0;
  box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
}

.form-control::placeholder {
  color: #6c757d;
  opacity: 1;
}

/* 工具类 */
.text-center {
  text-align: center;
}

.text-left {
  text-align: left;
}

.text-right {
  text-align: right;
}

.d-flex {
  display: flex;
}

.d-none {
  display: none;
}

.justify-content-center {
  justify-content: center;
}

.justify-content-between {
  justify-content: space-between;
}

.align-items-center {
  align-items: center;
}

.flex-column {
  flex-direction: column;
}

.flex-1 {
  flex: 1;
}

.w-100 {
  width: 100%;
}

.h-100 {
  height: 100%;
}

.mb-0 {
  margin-bottom: 0;
}

.mb-1 {
  margin-bottom: 0.25rem;
}

.mb-2 {
  margin-bottom: 0.5rem;
}

.mb-3 {
  margin-bottom: 1rem;
}

.mb-4 {
  margin-bottom: 1.5rem;
}

.mt-0 {
  margin-top: 0;
}

.mt-1 {
  margin-top: 0.25rem;
}

.mt-2 {
  margin-top: 0.5rem;
}

.mt-3 {
  margin-top: 1rem;
}

.mt-4 {
  margin-top: 1.5rem;
}

.p-0 {
  padding: 0;
}

.p-1 {
  padding: 0.25rem;
}

.p-2 {
  padding: 0.5rem;
}

.p-3 {
  padding: 1rem;
}

.p-4 {
  padding: 1.5rem;
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from {
  transform: translateY(100%);
  opacity: 0;
}

.slide-up-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

/* 响应式断点 */
@media (max-width: 576px) {
  .container {
    padding: 0 15px;
  }
}

@media (max-width: 768px) {
  .btn {
    font-size: 13px;
    padding: 6px 12px;
  }
  
  .form-control {
    font-size: 16px; /* 防止 iOS Safari 缩放 */
  }
}

@media (max-width: 992px) {
  html {
    font-size: 13px;
  }
}
</style>
