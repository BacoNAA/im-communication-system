<template>
  <div class="create-moment-container">
    <el-card class="create-moment-card">
      <div class="header">
        <h3 class="title">发布动态</h3>
        <el-button size="small" @click="$emit('close')">取消</el-button>
      </div>
      
      <el-divider />
      
      <!-- 动态内容输入 -->
      <div class="content-input">
        <el-input
          v-model="content"
          type="textarea"
          :rows="4"
          placeholder="分享此刻的想法..."
          maxlength="2000"
          show-word-limit
          resize="none"
        />
      </div>
      
      <!-- 媒体预览区域 - 图片 -->
      <div class="preview-container" v-if="mediaFiles.length > 0">
        <div class="preview-title">
          已选择 {{ mediaFiles.length }}/9 张图片
          <el-button link @click="clearFiles">清空</el-button>
        </div>
        <div class="preview-list">
          <div 
            v-for="(file, index) in previewUrls" 
            :key="index" 
            class="preview-item"
          >
            <el-image :src="file.url" fit="cover" />
            <div class="preview-delete" @click="removeFile(index)">
              <el-icon><Delete /></el-icon>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 媒体预览区域 - 视频 -->
      <div class="preview-container" v-if="videoFile">
        <div class="preview-title">
          已选择视频: {{ videoFile.name }}
          <el-button link @click="clearVideoFile">清空</el-button>
        </div>
        <div class="video-preview" v-if="videoPreviewUrl">
          <video controls :src="videoPreviewUrl" class="video-player"></video>
        </div>
      </div>
      
      <!-- 底部工具栏 -->
      <div class="footer">
        <div class="tools">
          <el-tooltip content="上传图片" placement="top">
            <div class="tool-item" @click="openUploader('image')">
              <el-icon><Picture /></el-icon>
              <input
                ref="imageInput"
                type="file"
                accept="image/*"
                multiple
                class="hidden-input"
                @change="handleFileChange"
              />
            </div>
          </el-tooltip>
          
          <el-tooltip content="上传视频" placement="top">
            <div class="tool-item" @click="openUploader('video')">
              <el-icon><VideoCamera /></el-icon>
              <input
                ref="videoInput"
                type="file"
                accept="video/*"
                class="hidden-input"
                @change="handleVideoChange"
              />
            </div>
          </el-tooltip>
          
          <el-tooltip content="设置可见性" placement="top">
            <div class="tool-item" @click="openVisibilitySettings">
              <el-icon><View /></el-icon>
            </div>
          </el-tooltip>
        </div>
        
        <el-button 
          type="primary" 
          :loading="publishing"
          :disabled="!isValid"
          @click="publishMoment"
        >
          发布
        </el-button>
      </div>
    </el-card>
    
    <!-- 可见性设置对话框 -->
    <el-dialog
      v-model="visibilityDialogVisible"
      title="设置动态可见范围"
      width="360px"
    >
      <div class="visibility-options">
        <div class="visibility-info">
          <el-alert
            type="info"
            :closable="false"
            show-icon
          >
            <b>提示：</b>可见性设置仅适用于当前这条动态，不会影响其他动态。
          </el-alert>
        </div>
        
        <el-radio-group v-model="visibilityType">
          <el-radio label="PUBLIC">所有好友可见</el-radio>
          <el-radio label="PRIVATE">仅自己可见</el-radio>
          <el-radio label="CUSTOM">自定义</el-radio>
        </el-radio-group>
        
        <div v-if="visibilityType === 'CUSTOM'" class="custom-options">
          <el-radio-group v-model="customType" class="mt-3">
            <el-radio label="allowList">部分好友可见</el-radio>
            <el-radio label="blockList">不让部分好友看</el-radio>
          </el-radio-group>
          
          <div class="select-friends-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ 
              customType === 'allowList' 
                ? '请选择可以看到这条动态的好友' 
                : '请选择不允许看到这条动态的好友' 
            }}</span>
          </div>
          
          <div class="friends-selector">
            <!-- 好友选择器应该在实际实现中集成 -->
            <el-input 
              placeholder="搜索好友" 
              v-model="friendSearch" 
              class="mb-2"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            
            <div class="friends-list">
              <div class="friend-selection-actions">
                <div class="selection-count">
                  已选择 {{ selectedFriends.length }} 位好友
                </div>
                <div class="selection-buttons">
                  <el-button type="text" size="small" @click="selectAllFriends">全选</el-button>
                  <el-button type="text" size="small" @click="unselectAllFriends">全不选</el-button>
                </div>
              </div>
              <el-checkbox-group v-model="selectedFriends">
                <el-checkbox 
                  v-for="friend in filteredFriends" 
                  :key="friend.id"
                  :label="friend.id"
                  class="friend-checkbox-item"
                >
                  <div class="friend-checkbox-content">
                    <el-avatar 
                      :size="24" 
                      :src="friend.avatarUrl" 
                      class="friend-avatar"
                    >{{ friend.nickname?.charAt(0) }}</el-avatar>
                    <span class="friend-name">{{ friend.nickname || friend.alias || 'Unknown' }}</span>
                  </div>
                </el-checkbox>
              </el-checkbox-group>
              <div v-if="filteredFriends.length === 0" class="no-friends">
                {{ friendSearch ? '没有找到匹配的好友' : '暂无好友可选择' }}
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <span>
          <el-button @click="visibilityDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmVisibility">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { api } from '@/api/request';
import { useContacts } from '@/composables/useContacts';
import { ElMessage } from 'element-plus';
import { Picture, Delete, View, InfoFilled, Search, VideoCamera } from '@element-plus/icons-vue';
import { createMoment } from '@/api/moment';

const emit = defineEmits(['close', 'published']);

// 状态管理
const content = ref('');
const mediaFiles = ref([]);
const previewUrls = ref([]);
const publishing = ref(false);
const imageInput = ref(null);
const videoInput = ref(null);
const visibilityDialogVisible = ref(false);
const visibilityType = ref('PUBLIC'); // PUBLIC, PRIVATE, CUSTOM
const customType = ref('allowList'); // allowList, blockList
const friendSearch = ref('');
const selectedFriends = ref([]);
const videoFile = ref(null);
const videoPreviewUrl = ref(null);

// 获取联系人列表
const { contacts, loadContacts } = useContacts();

// 在组件挂载时加载联系人
onMounted(async () => {
  try {
    await loadContacts();
    console.log('联系人加载成功:', contacts.value);
  } catch (error) {
    console.error('加载联系人失败:', error);
    ElMessage.error('加载联系人失败');
  }
});

// 使用联系人作为好友列表
const friends = computed(() => {
  const contactList = contacts.value || [];
  console.log('当前联系人列表:', contactList);
  
  // 将联系人列表转换为好友格式，适应UI展示
  return contactList.map(contact => ({
    id: contact.friendId,
    nickname: contact.friend?.nickname || contact.alias || `Friend #${contact.friendId}`,
    alias: contact.alias,
    avatarUrl: contact.friend?.avatarUrl || ''
  }));
});

// 过滤好友列表
const filteredFriends = computed(() => {
  if (!friendSearch.value) return friends.value;
  const search = friendSearch.value.toLowerCase();
  return friends.value.filter(friend => 
    friend.nickname?.toLowerCase().includes(search) ||
    friend.alias?.toLowerCase().includes(search)
  );
});

// 动态发布是否有效
const isValid = computed(() => {
  if (content.value.trim()) return true;
  if (mediaFiles.value.length > 0) return true;
  if (videoFile.value) return true;
  return false;
});

// 打开文件选择器
const openUploader = (type) => {
  if (type === 'image') {
    if (mediaFiles.value.length >= 9) {
      ElMessage.warning('最多可选择9张图片');
      return;
    }
    imageInput.value?.click();
  } else if (type === 'video') {
    if (videoFile.value) {
      ElMessage.warning('只能上传1个视频，请先清除已有视频');
      return;
    }
    videoInput.value?.click();
  }
};

// 处理图片文件选择
const handleFileChange = (event) => {
  const files = Array.from(event.target.files);
  
  // 检查文件数量限制
  if (mediaFiles.value.length + files.length > 9) {
    ElMessage.warning('最多可选择9张图片');
    event.target.value = null;
    return;
  }
  
  // 检查文件类型和大小
  const validFiles = files.filter(file => {
    // 检查类型
    if (!file.type.startsWith('image/')) {
      ElMessage.error(`${file.name}不是有效的图片文件`);
      return false;
    }
    
    // 检查大小 (5MB限制)
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.error(`${file.name}超过5MB大小限制`);
      return false;
    }
    
    return true;
  });
  
  // 更新文件列表和预览
  mediaFiles.value = [...mediaFiles.value, ...validFiles];
  
  // 生成预览URL
  validFiles.forEach(file => {
    const reader = new FileReader();
    reader.onload = (e) => {
      previewUrls.value.push({
        name: file.name,
        url: e.target.result
      });
    };
    reader.readAsDataURL(file);
  });
  
  // 重置文件输入框
  event.target.value = null;
};

// 处理视频文件选择
const handleVideoChange = (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  // 检查文件类型
  if (!file.type.startsWith('video/')) {
    ElMessage.error(`${file.name}不是有效的视频文件`);
    event.target.value = null;
    return;
  }
  
  // 检查大小 (50MB限制)
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error(`${file.name}超过50MB大小限制`);
    event.target.value = null;
    return;
  }
  
  videoFile.value = file;
  
  // 生成视频预览URL
  const reader = new FileReader();
  reader.onload = (e) => {
    videoPreviewUrl.value = e.target.result;
  };
  reader.readAsDataURL(file);
  
  // 重置文件输入框
  event.target.value = null;
};

// 移除文件
const removeFile = (index) => {
  mediaFiles.value.splice(index, 1);
  previewUrls.value.splice(index, 1);
};

// 清空所有图片文件
const clearFiles = () => {
  mediaFiles.value = [];
  previewUrls.value = [];
};

// 清空视频文件
const clearVideoFile = () => {
  videoFile.value = null;
  videoPreviewUrl.value = null;
};

// 打开可见性设置对话框
const openVisibilitySettings = () => {
  visibilityDialogVisible.value = true;
};

// 确认可见性设置
const confirmVisibility = () => {
  visibilityDialogVisible.value = false;
};

// 发布动态
const publishMoment = async () => {
  if (!isValid.value) return;
  
  publishing.value = true;
  
  try {
    // 准备要上传的媒体文件
    let imageUrls = [];
    let videoUrl = null;
    let mediaType = 'TEXT'; // 默认为文本
    
    // 上传图片
    if (mediaFiles.value.length > 0) {
      const formData = new FormData();
      
      console.log('准备上传图片，数量:', mediaFiles.value.length);
      
      // 检查每个文件并添加到FormData
      mediaFiles.value.forEach((file, index) => {
        console.log(`准备文件 ${index + 1}/${mediaFiles.value.length}: ${file.name}, ${file.size} 字节, ${file.type}`);
        formData.append('files', file);
      });
      
      const token = localStorage.getItem('accessToken') || '';
      
      try {
        console.log('开始上传图片文件...');
        const response = await fetch('/api/moments/upload/images', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`
          },
          body: formData
        });
        
        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(`图片上传失败: ${errorData.message || response.statusText}`);
        }
        
        const data = await response.json();
        console.log('图片上传API响应:', data);
        
        if (data && data.success && data.data) {
          imageUrls = data.data;
          mediaType = videoFile.value ? 'MIXED' : 'IMAGE';
          console.log('图片上传成功:', imageUrls);
        } else {
          console.error('图片上传API未返回有效数据:', data);
          throw new Error('图片上传失败: ' + (data?.message || '服务器未返回有效数据'));
        }
      } catch (error) {
        console.error('图片上传过程中出错:', error);
        throw new Error('图片上传失败: ' + (error.message || '未知错误'));
      }
    }
    
    // 上传视频
    if (videoFile.value) {
      const formData = new FormData();
      formData.append('file', videoFile.value);
      
      console.log(`准备上传视频: ${videoFile.value.name}, ${videoFile.value.size} 字节, ${videoFile.value.type}`);
      
      const token = localStorage.getItem('accessToken') || '';
      
      try {
        console.log('开始上传视频文件...');
        const response = await fetch('/api/moments/upload/video', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`
          },
          body: formData
        });
        
        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(`视频上传失败: ${errorData.message || response.statusText}`);
        }
        
        const data = await response.json();
        console.log('视频上传API响应:', data);
        
        if (data && data.success && data.data) {
          videoUrl = data.data;
          mediaType = imageUrls.length > 0 ? 'MIXED' : 'VIDEO';
          console.log('视频上传成功:', videoUrl);
        } else {
          console.error('视频上传API未返回有效数据:', data);
          throw new Error('视频上传失败: ' + (data?.message || '服务器未返回有效数据'));
        }
      } catch (error) {
        console.error('视频上传过程中出错:', error);
        throw new Error('视频上传失败: ' + (error.message || '未知错误'));
      }
    }
    
    // 准备可见性规则
    let visibilityRules = null;
    
    if (visibilityType.value === 'CUSTOM') {
      visibilityRules = {
        allowedUserIds: customType.value === 'allowList' ? selectedFriends.value : null,
        blockedUserIds: customType.value === 'blockList' ? selectedFriends.value : null
      };
    }
    
    // 组合所有媒体URL
    let mediaUrls = [...imageUrls];
    if (videoUrl) {
      mediaUrls.push(videoUrl);
    }
    
    // 创建动态请求体
    const momentData = {
      content: content.value,
      mediaUrls,
      mediaType,
      visibilityType: visibilityType.value,
      visibilityRules
    };
    
    console.log('准备发布动态数据:', momentData);
    
    // 调用createMoment函数发布动态
    const response = await createMoment(momentData);
    console.log('发布动态响应:', response);
    
    if (response && response.success) {
      ElMessage.success('动态发布成功');
      emit('published', response.data);
      resetForm();
    } else {
      throw new Error(response?.message || '发布失败');
    }
  } catch (error) {
    console.error('Failed to publish moment:', error);
    ElMessage.error(`发布失败：${error.message || '请稍后重试'}`);
  } finally {
    publishing.value = false;
  }
};

// 重置表单
const resetForm = () => {
  content.value = '';
  mediaFiles.value = [];
  previewUrls.value = [];
  videoFile.value = null;
  videoPreviewUrl.value = null;
  visibilityType.value = 'PUBLIC';
  customType.value = 'allowList';
  selectedFriends.value = [];
};

// 监听可见性类型变化，重置选中的好友
watch(visibilityType, (val) => {
  if (val !== 'CUSTOM') {
    selectedFriends.value = [];
  }
});

// 监听自定义类型变化，重置选中的好友
watch(customType, () => {
  selectedFriends.value = [];
});

// 监听可见性对话框的显示状态
watch(visibilityDialogVisible, (val) => {
  if (val) {
    console.log('打开可见性设置对话框');
    console.log('当前联系人列表:', contacts.value);
    console.log('好友列表:', friends.value);
    console.log('过滤后的好友列表:', filteredFriends.value);
  }
});

// 全选好友
const selectAllFriends = () => {
  selectedFriends.value = filteredFriends.value.map(friend => friend.id);
};

// 取消全选好友
const unselectAllFriends = () => {
  selectedFriends.value = [];
};
</script>

<style scoped>
.create-moment-container {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.create-moment-card {
  border-radius: 8px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  margin: 0;
}

.content-input {
  margin: 16px 0;
}

.preview-container {
  margin-top: 16px;
}

.preview-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  margin-bottom: 12px;
}

.preview-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  grid-gap: 8px;
}

.preview-item {
  position: relative;
  width: 100%;
  height: 100px;
  border-radius: 4px;
  overflow: hidden;
}

.preview-item .el-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-delete {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  background-color: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: white;
}

/* 视频预览样式 */
.video-preview {
  margin-top: 10px;
  width: 100%;
}

.video-player {
  width: 100%;
  max-height: 300px;
  border-radius: 4px;
  background-color: #000;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
}

.tools {
  display: flex;
  gap: 16px;
}

.tool-item {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.tool-item:hover {
  background-color: var(--el-fill-color-light);
}

.hidden-input {
  display: none;
}

.visibility-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.visibility-info {
  margin-bottom: 16px;
}

/* 覆盖 Element Plus alert 组件的样式 */
:deep(.el-alert) {
  border-radius: 4px;
}

:deep(.el-alert__content) {
  padding: 8px 0;
}

:deep(.el-alert__title) {
  font-size: 14px;
  line-height: 1.5;
}

.custom-options {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-light);
}

.select-friends-hint {
  margin: 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.friends-list {
  max-height: 300px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background-color: var(--el-bg-color);
}

.friend-selection-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 0 4px;
}

.selection-count {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-right: 10px;
}

.selection-buttons {
  display: flex;
  gap: 8px;
}

.friend-checkbox-item {
  display: block;
  margin-bottom: 4px !important;
}

.friend-checkbox-content {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
}

.friend-avatar {
  flex-shrink: 0;
  border: 1px solid var(--el-border-color-light);
  background-color: var(--el-color-primary-light-9);
}

.friend-name {
  font-size: 14px;
  color: var(--el-text-color-primary);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.no-friends {
  text-align: center;
  padding: 10px 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.mb-2 {
  margin-bottom: 8px;
}

.mt-3 {
  margin-top: 12px;
}
</style> 