<template>
  <div class="appearance-settings">
    <h3 class="settings-title">界面个性化</h3>
    
    <!-- 消息提示 -->
    <div v-if="showMessage" :class="['settings-message', messageType]">
      <div class="message-content">{{ messageText }}</div>
      <button class="close-message" @click="closeMessage">×</button>
    </div>
    
    <!-- 主题颜色 -->
    <div class="settings-section">
      <h4 class="section-title">主题颜色</h4>
      <div class="color-picker">
        <div 
          v-for="(color, index) in themeColors" 
          :key="index"
          :class="['color-option', { active: selectedThemeColor === color.value }]"
          :style="{ backgroundColor: color.value }"
          @click="selectThemeColor(color.value)"
          :title="color.name"
        >
          <i v-if="selectedThemeColor === color.value" class="fas fa-check"></i>
        </div>
        
        <!-- 自定义颜色 -->
        <div class="color-option custom">
          <input 
            type="color" 
            v-model="customThemeColor" 
            @change="selectThemeColor(customThemeColor)"
            title="自定义颜色"
          />
        </div>
      </div>
    </div>
    
    <!-- 聊天背景 -->
    <div class="settings-section">
      <h4 class="section-title">聊天背景</h4>
      <div class="background-options">
        <div 
          v-for="(bg, index) in backgroundOptions" 
          :key="index"
          :class="['background-option', { active: selectedBackground === bg.value }]"
          @click="selectBackground(bg.value)"
        >
          <img v-if="bg.type === 'image'" :src="bg.preview" :alt="bg.name" />
          <div v-else class="color-preview" :style="{ backgroundColor: bg.value }"></div>
          <span class="bg-name">{{ bg.name }}</span>
        </div>
        
        <!-- 上传自定义背景 -->
        <div class="background-option upload">
          <label for="bg-upload" class="upload-label">
            <i class="fas fa-upload"></i>
            <span>上传图片</span>
          </label>
          <input 
            type="file" 
            id="bg-upload" 
            accept="image/*" 
            @change="handleBackgroundUpload" 
            class="upload-input"
          />
        </div>
      </div>
    </div>
    
    <!-- 字体大小 -->
    <div class="settings-section">
      <h4 class="section-title">字体大小</h4>
      <div class="font-size-selector">
        <div class="size-label">小</div>
        <input 
          type="range" 
          v-model="fontSize" 
          min="12" 
          max="20" 
          step="1" 
          class="size-slider"
          @change="updateFontSize"
        />
        <div class="size-label">大</div>
        <div class="size-value">{{ fontSize }}px</div>
      </div>
      <div class="font-size-preview" :style="{ fontSize: `${fontSize}px` }">
        字体大小预览
      </div>
    </div>
    
    <!-- 保存按钮 -->
    <div class="settings-actions">
      <button class="reset-btn" @click="resetToDefaults">恢复默认设置</button>
      <button class="save-btn" @click="saveSettings" :disabled="!hasChanges">保存设置</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useUserSettings } from '@/composables/useUserSettings';

const { settings, updateSettings, resetSettings } = useUserSettings();

// 主题颜色选项
const themeColors = [
  { name: '蓝色', value: '#1890ff' },
  { name: '绿色', value: '#52c41a' },
  { name: '红色', value: '#f5222d' },
  { name: '橙色', value: '#fa8c16' },
  { name: '紫色', value: '#722ed1' },
  { name: '青色', value: '#13c2c2' }
];

// 背景选项
const backgroundOptions = [
  { name: '默认', value: 'default', type: 'color', preview: '#f5f5f5' },
  { name: '深色', value: '#2d3436', type: 'color', preview: '#2d3436' },
  { name: '浅蓝', value: '#dff9fb', type: 'color', preview: '#dff9fb' },
  { name: '青绿', value: '#e0f7fa', type: 'color', preview: '#e0f7fa' },
  { name: '淡紫', value: '#f3e5f5', type: 'color', preview: '#f3e5f5' },
  { name: '浅粉', value: '#fce4ec', type: 'color', preview: '#fce4ec' },
  { name: '淡黄', value: '#fff8e1', type: 'color', preview: '#fff8e1' },
  { name: '浅绿', value: '#e8f5e9', type: 'color', preview: '#e8f5e9' }
];

// 定义事件
const emit = defineEmits<{
  (e: 'close'): void;
}>();

// 状态
const selectedThemeColor = ref(settings.value?.theme?.color || '#1890ff');
const customThemeColor = ref(selectedThemeColor.value);
const selectedBackground = ref(settings.value?.theme?.chatBackground || 'default');
const fontSize = ref(settings.value?.theme?.fontSize || 14);
const originalSettings = ref({
  themeColor: selectedThemeColor.value,
  background: selectedBackground.value,
  fontSize: fontSize.value
});

// 计算是否有更改
const hasChanges = computed(() => {
  return selectedThemeColor.value !== originalSettings.value.themeColor ||
         selectedBackground.value !== originalSettings.value.background ||
         fontSize.value !== originalSettings.value.fontSize;
});

// 消息提示状态
const showMessage = ref(false);
const messageText = ref('');
const messageType = ref<'success' | 'error'>('success');

const closeMessage = () => {
  showMessage.value = false;
  messageText.value = '';
  messageType.value = 'success';
};

// 选择主题颜色
const selectThemeColor = (color: string) => {
  selectedThemeColor.value = color;
  customThemeColor.value = color;
  applyThemeColor(color);
};

// 选择背景
const selectBackground = (bg: string) => {
  selectedBackground.value = bg;
  applyBackground(bg);
};

// 更新字体大小
const updateFontSize = () => {
  applyFontSize(fontSize.value);
};

// 处理背景上传
const handleBackgroundUpload = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files[0]) {
    const file = input.files[0];
    const reader = new FileReader();
    
    reader.onload = (e) => {
      const result = e.target?.result as string;
      selectedBackground.value = result;
      applyBackground(result);
    };
    
    reader.readAsDataURL(file);
  }
};

// 应用主题颜色
const applyThemeColor = (color: string) => {
  document.documentElement.style.setProperty('--primary-color', color);
  
  // 计算衍生颜色
  const r = parseInt(color.slice(1, 3), 16);
  const g = parseInt(color.slice(3, 5), 16);
  const b = parseInt(color.slice(5, 7), 16);
  
  // 浅色变体
  document.documentElement.style.setProperty(
    '--primary-light-color', 
    `rgba(${r}, ${g}, ${b}, 0.2)`
  );
  
  // 深色变体
  const darken = (c: number) => Math.max(0, c - 40);
  document.documentElement.style.setProperty(
    '--primary-dark-color',
    `rgb(${darken(r)}, ${darken(g)}, ${darken(b)})`
  );
};

// 应用背景
const applyBackground = (bg: string) => {
  const chatPanels = document.querySelectorAll('.message-container');
  chatPanels.forEach((panel) => {
    const element = panel as HTMLElement;
    if (bg === 'default') {
      element.style.background = '';
    } else if (bg.startsWith('#')) {
      element.style.background = bg;
      element.style.backgroundImage = 'none';
    } else {
      element.style.backgroundImage = `url(${bg})`;
      element.style.backgroundSize = 'cover';
      element.style.backgroundPosition = 'center';
    }
  });
};

// 应用字体大小
const applyFontSize = (size: number) => {
  document.documentElement.style.setProperty('--font-size-base', `${size}px`);
};

// 保存设置
const saveSettings = async () => {
  try {
    console.log('保存外观设置:', {
      theme: {
        color: selectedThemeColor.value,
        chatBackground: selectedBackground.value,
        fontSize: fontSize.value
      }
    });
    
    // 使用 useUserSettings 中的方法保存设置
    const result = await updateSettings({
      theme: {
        color: selectedThemeColor.value,
        chatBackground: selectedBackground.value,
        fontSize: fontSize.value
      }
    });
    
    // 同时尝试直接发送API请求（用于调试）
    try {
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken') || '';
      let userId: string = '';
      
      // 从多个可能的位置获取用户ID
      const localStorageUserId = localStorage.getItem('userId');
      const sessionStorageUserId = sessionStorage.getItem('userId');
      let localStorageUserInfo = null;
      let sessionStorageUserInfo = null;
      
      try {
        if (localStorage.getItem('userInfo')) {
          localStorageUserInfo = JSON.parse(localStorage.getItem('userInfo') || '{}').id;
        }
      } catch (e) {
        console.error('解析localStorage.userInfo失败', e);
      }
      
      try {
        if (sessionStorage.getItem('userInfo')) {
          sessionStorageUserInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}').id;
        }
      } catch (e) {
        console.error('解析sessionStorage.userInfo失败', e);
      }
      
      // 按优先级获取
      if (localStorageUserId) userId = localStorageUserId;
      else if (sessionStorageUserId) userId = sessionStorageUserId;
      else if (localStorageUserInfo) userId = String(localStorageUserInfo);
      else if (sessionStorageUserInfo) userId = String(sessionStorageUserInfo);
      
      console.log('调试信息 - 用户ID:', userId, '令牌:', token ? token.substring(0, 10) + '...' : '无');
      
      if (!userId) {
        console.error('找不到用户ID，无法更新设置');
        messageType.value = 'error';
        messageText.value = '找不到用户ID，请确保您已登录';
        showMessage.value = true;
        return;
      }
      
      // 尝试原始端点
      const requestData = {
        themeColor: selectedThemeColor.value,
        chatBackground: selectedBackground.value,
        fontSize: fontSize.value
      };
      
      // 测试原始端点 - 添加userId作为参数
      console.log('尝试调用原始API端点');
      try {
        const response1 = await fetch(`/api/user/settings?userId=${userId}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': token ? `Bearer ${token}` : '',
            'X-User-Id': userId.toString() // 添加额外的用户ID头
          },
          body: JSON.stringify(requestData)
        });
        
        console.log('原始API调用结果:', response1.status, response1.statusText);
        if (response1.ok) {
          const data = await response1.json();
          console.log('原始API响应数据:', data);
        } else {
          console.error('原始API响应错误:', response1.status, response1.statusText);
          try {
            const errorText = await response1.text();
            console.error('错误详情:', errorText);
          } catch (e) {
            console.error('无法读取错误详情');
          }
        }
      } catch (apiError1) {
        console.error('原始API调用失败:', apiError1);
      }
      
      // 无需再测试V2端点，已合并回原始控制器
    } catch (apiError) {
      console.error('直接API调用失败:', apiError);
    }
    
    // 显示成功提示
    messageType.value = 'success';
    messageText.value = '外观设置已保存';
    showMessage.value = true;
    
    // 定时隐藏消息
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
    
    // 更新原始设置以重置hasChanges状态
    originalSettings.value = {
      themeColor: selectedThemeColor.value,
      background: selectedBackground.value,
      fontSize: fontSize.value
    };
    
    // 关闭设置对话框
    emit('close');
    
    // 刷新页面
    setTimeout(() => {
      window.location.reload();
    }, 500);
  } catch (error: any) {
    console.error('保存设置失败:', error);
    
    // 显示错误提示
    messageType.value = 'error';
    messageText.value = '保存设置失败: ' + (error.message || '未知错误');
    showMessage.value = true;
  }
};

// 重置为默认设置
const resetToDefaults = async () => {
  try {
    await resetSettings();
    
    // 重置本地状态
    selectedThemeColor.value = '#1890ff';
    customThemeColor.value = '#1890ff';
    selectedBackground.value = 'default';
    fontSize.value = 14;
    
    // 应用默认设置
    applyThemeColor(selectedThemeColor.value);
    applyBackground(selectedBackground.value);
    applyFontSize(fontSize.value);
    
    // 更新原始设置
    originalSettings.value = {
      themeColor: selectedThemeColor.value,
      background: selectedBackground.value,
      fontSize: fontSize.value
    };
    
    // 显示成功消息
    messageType.value = 'success';
    messageText.value = '已恢复默认设置';
    showMessage.value = true;
    
    // 定时隐藏消息
    setTimeout(() => {
      showMessage.value = false;
    }, 3000);
    
    // 关闭设置对话框
    emit('close');
    
    // 刷新页面
    setTimeout(() => {
      window.location.reload();
    }, 500);
  } catch (error) {
    console.error('重置设置失败:', error);
    
    // 显示错误消息
    messageType.value = 'error';
    messageText.value = '重置设置失败: ' + ((error as Error)?.message || '未知错误');
    showMessage.value = true;
  }
};

// 初始化
onMounted(() => {
  // 应用当前设置
  applyThemeColor(selectedThemeColor.value);
  applyBackground(selectedBackground.value);
  applyFontSize(fontSize.value);
});
</script>

<style scoped>
.appearance-settings {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.settings-title {
  font-size: 18px;
  margin-bottom: 20px;
  color: var(--primary-color, #1890ff);
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.settings-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 16px;
  margin-bottom: 15px;
  font-weight: 500;
}

/* 颜色选择器样式 */
.color-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.color-option {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.color-option.active {
  border-color: #333;
}

.color-option i {
  color: white;
  font-size: 16px;
}

.color-option.custom {
  position: relative;
  overflow: hidden;
  background: linear-gradient(45deg, #f00, #ff0, #0f0, #0ff, #00f, #f0f, #f00);
}

.color-option.custom input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

/* 背景选项样式 */
.background-options {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 15px;
}

.background-option {
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  height: 80px;
  position: relative;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.background-option.active {
  border-color: var(--primary-color, #1890ff);
}

.background-option img,
.background-option .color-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.bg-name {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  padding: 4px;
  font-size: 12px;
  text-align: center;
}

.background-option.upload {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
}

.upload-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.upload-label i {
  font-size: 24px;
  margin-bottom: 5px;
}

.upload-input {
  display: none;
}

/* 字体大小选择器样式 */
.font-size-selector {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.size-slider {
  flex: 1;
  height: 6px;
  -webkit-appearance: none;
  appearance: none;
  background: #ddd;
  outline: none;
  border-radius: 3px;
}

.size-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--primary-color, #1890ff);
  cursor: pointer;
}

.size-value {
  min-width: 40px;
  text-align: center;
}

.font-size-preview {
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-top: 10px;
  text-align: center;
}

/* 按钮样式 */
.settings-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 20px;
}

.save-btn, .reset-btn {
  padding: 8px 16px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.save-btn {
  background-color: var(--primary-color, #1890ff);
  color: white;
}

.save-btn:hover:not(:disabled) {
  background-color: var(--primary-dark-color, #096dd9);
}

.save-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.reset-btn {
  background-color: #f5f5f5;
  color: #333;
}

.reset-btn:hover {
  background-color: #e8e8e8;
}

/* 消息提示样式 */
.settings-message {
  position: fixed;
  top: 20px;
  right: 20px;
  background-color: #4CAF50; /* 绿色 */
  color: white;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 1000;
  opacity: 0.9;
  transition: opacity 0.5s ease-in-out;
}

.settings-message.error {
  background-color: #f44336; /* 红色 */
}

.message-content {
  flex-grow: 1;
  margin-right: 10px;
}

.close-message {
  background: none;
  border: none;
  color: white;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}
</style> 