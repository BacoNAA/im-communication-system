<template>
  <div class="moment-card">
    <!-- 用户信息区域 -->
    <div class="moment-header">
      <div class="user-info">
        <el-avatar :size="40" :src="moment.userAvatar">
          {{ getInitials(moment.userNickname) }}
        </el-avatar>
        <div class="user-meta">
          <div class="username">{{ moment.userNickname }}</div>
          <div class="time">{{ formatTime(moment.createdAt) }}</div>
        </div>
      </div>
      <el-dropdown v-if="canManage" trigger="click" @command="handleCommand">
        <div class="more-options-button">
          <i class="more-icon">⋮</i>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="edit" v-if="isOwner">
              <i class="menu-icon edit-icon">✏️</i> 编辑
            </el-dropdown-item>
            <el-dropdown-item command="delete" v-if="isOwner">
              <i class="menu-icon delete-icon">🗑️</i> 删除
            </el-dropdown-item>
            <el-dropdown-item command="report" v-else>
              <i class="menu-icon report-icon">🚩</i> 举报
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    
    <!-- 内容区域 -->
    <div class="moment-content">
      <p class="text" v-if="moment.content">{{ moment.content }}</p>
      
      <!-- 图片区域 -->
      <div class="media-container" v-if="moment.mediaType === 'IMAGE' && moment.mediaUrls?.length">
        <div :class="getImageGridClass(moment.mediaUrls.length)">
          <div 
            v-for="(url, index) in moment.mediaUrls" 
            :key="index" 
            class="image-item"
            @click="previewImage(index)"
          >
            <el-image 
              :src="url"
              fit="cover"
              loading="lazy"
              :preview-src-list="moment.mediaUrls"
              :initial-index="index"
              hide-on-click-modal
            />
          </div>
        </div>
      </div>
      
      <!-- 视频区域 -->
      <div class="media-container" v-else-if="moment.mediaType === 'VIDEO' && moment.mediaUrls?.length">
        <video 
          class="video-player" 
          :src="moment.mediaUrls[0]" 
          controls 
          preload="metadata"
        />
      </div>
    </div>
    
    <!-- 互动区域：点赞、评论 -->
    <div class="moment-actions">
      <el-button 
        class="action-button" 
        type="primary" 
        size="small"
        :plain="!moment.isLiked"
        @click="toggleLike"
      >
        <i :class="[
          'action-icon', 
          moment.isLiked ? 'el-icon-star-filled liked' : 'el-icon-star'
        ]"></i>
        <span class="action-text">{{ moment.isLiked ? '已点赞' : '点赞' }} ({{ moment.likeCount || 0 }})</span>
      </el-button>
      
      <el-button
        class="action-button"
        type="info"
        size="small"
        plain
        @click="openComments"
      >
        <i class="action-icon el-icon-chat-dot-square"></i>
        <span class="action-text">评论 ({{ moment.commentCount || 0 }})</span>
      </el-button>
    </div>

    <!-- 举报对话框 -->
    <el-dialog
      v-model="showReportDialog"
      title="举报动态"
      width="400px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form>
        <el-form-item label="举报原因" required>
          <el-select v-model="reportReason" placeholder="请选择举报原因" style="width: 100%">
            <el-option label="垃圾信息" value="SPAM"></el-option>
            <el-option label="色情内容" value="PORNOGRAPHY"></el-option>
            <el-option label="暴力或恐怖内容" value="VIOLENCE"></el-option>
            <el-option label="诈骗信息" value="SCAM"></el-option>
            <el-option label="侵犯隐私" value="PRIVACY"></el-option>
            <el-option label="侮辱他人" value="INSULT"></el-option>
            <el-option label="其他" value="OTHER"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input
            v-model="reportDescription"
            type="textarea"
            rows="4"
            placeholder="请描述具体情况（选填）"
            maxlength="500"
            show-word-limit
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelReport">取消</el-button>
        <el-button type="primary" @click="submitReport" :loading="reportSubmitting">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, inject } from 'vue';
import { ElAvatar, ElDropdown, ElDropdownMenu, ElDropdownItem, ElImage, ElButton, ElDialog, ElForm, ElFormItem, ElSelect, ElOption, ElInput, ElMessage } from 'element-plus';
import { userApi } from '@/api/user';
import { reportApi } from '@/api/report';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const props = defineProps({
  moment: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['like', 'comment']);

// 获取当前用户信息（可以通过provide/inject方式从父组件获取）
const currentUser = inject('currentUser', {});

// 计算属性
const isOwner = computed(() => {
  return currentUser.id === props.moment.userId;
});

const canManage = computed(() => {
  return true; // 所有用户都可以操作下拉菜单，只是选项不同
});

// 举报相关状态
const showReportDialog = ref(false);
const reportReason = ref('');
const reportDescription = ref('');
const reportSubmitting = ref(false);

// 获取用户名首字母作为头像备用显示
const getInitials = (name) => {
  if (!name) return '?';
  return name.charAt(0).toUpperCase();
};

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '';
  return dayjs(timestamp).fromNow();
};

// 根据图片数量确定网格布局类型
const getImageGridClass = (count) => {
  if (count === 1) return 'image-grid-1';
  if (count === 2) return 'image-grid-2';
  if (count === 3) return 'image-grid-3';
  if (count === 4) return 'image-grid-4';
  return 'image-grid-multiple';
};

// 预览图片
const previewImage = (index) => {
  // 处理图片预览逻辑
};

// 切换点赞状态
const toggleLike = () => {
  // 向父组件发送点赞/取消点赞事件
  emit('like', props.moment.id, props.moment.isLiked);
};

// 打开评论列表
const openComments = () => {
  emit('comment', props.moment.id);
};

// 处理下拉菜单命令
const handleCommand = (command) => {
  switch (command) {
    case 'edit':
      // 编辑动态
      break;
    case 'delete':
      // 删除动态
      break;
    case 'report':
      // 举报动态
      showReportDialog.value = true;
      break;
  }
};

// 取消举报
const cancelReport = () => {
  showReportDialog.value = false;
  reportReason.value = '';
  reportDescription.value = '';
};

// 提交举报
const submitReport = async () => {
  if (!reportReason.value) {
    ElMessage.warning('请选择举报原因');
    return;
  }
  
  try {
    reportSubmitting.value = true;
    console.log('提交举报动态，动态ID:', props.moment.id);
    
    const reportRequest = {
      reportedContentType: 'MOMENT',
      reportedContentId: props.moment.id,
      reportedUserId: props.moment.userId,
      reason: reportReason.value,
      description: reportDescription.value || ''
    };
    
    const response = await reportApi.submitReport(reportRequest);
    
    if (response.success) {
      ElMessage.success('举报已提交，我们将尽快处理');
      cancelReport();
    } else {
      ElMessage.error(response.message || '举报提交失败');
    }
  } catch (error) {
    console.error('举报动态失败:', error);
    ElMessage.error('举报提交失败，请稍后重试');
  } finally {
    reportSubmitting.value = false;
  }
};
</script>

<style scoped>
.moment-card {
  padding: 16px;
  background-color: var(--el-bg-color);
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.moment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-meta {
  display: flex;
  flex-direction: column;
}

.username {
  font-weight: 500;
  font-size: 16px;
  color: var(--el-text-color-primary);
}

.time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.moment-content {
  margin-bottom: 16px;
}

.text {
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 15px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.media-container {
  margin-top: 12px;
  border-radius: 8px;
  overflow: hidden;
}

/* 图片网格布局 */
.image-grid-1 {
  width: 100%;
  height: 240px;
}

.image-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-gap: 4px;
  height: 180px;
}

.image-grid-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  grid-gap: 4px;
  height: 180px;
}

.image-grid-4 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  grid-gap: 4px;
  height: 240px;
}

.image-grid-multiple {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-auto-rows: 120px;
  grid-gap: 4px;
}

.image-item {
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.image-item .el-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-player {
  width: 100%;
  max-height: 400px;
  background-color: #000;
  border-radius: 8px;
}

.moment-actions {
  display: flex;
  justify-content: space-around;
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  margin-top: 12px;
}

.action-button {
  flex: 1;
  margin: 0 8px;
  border-radius: 20px;
  transition: all 0.3s;
}

.action-icon {
  font-size: 16px;
  margin-right: 4px;
}

.action-icon.liked {
  color: #e74c3c;
}

.action-text {
  font-size: 14px;
}

/* 更多选项按钮样式 */
.more-options-button {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.3s;
  background-color: #f5f5f5;
}

.more-options-button:hover {
  background-color: #e8e8e8;
}

.more-icon {
  font-size: 20px;
  font-weight: bold;
  color: #666;
  line-height: 1;
}

.menu-icon {
  display: inline-block;
  margin-right: 8px;
  font-size: 16px;
}

.report-icon {
  color: #ff4d4f;
}

.edit-icon {
  color: #1890ff;
}

.delete-icon {
  color: #ff7875;
}
</style> 