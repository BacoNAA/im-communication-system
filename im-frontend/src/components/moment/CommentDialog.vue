<template>
  <div class="comment-dialog">
    <el-card class="comment-card" v-loading="loading">
      <template #header>
        <div class="header-content">
          <span class="comment-title">评论 ({{ totalComments }})</span>
          <el-button link icon="Close" @click="$emit('close')" />
        </div>
      </template>
      
      <!-- 评论列表 -->
      <div class="comment-list" ref="commentListRef">
        <el-empty v-if="!loading && comments.length === 0" description="暂无评论" />
        
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-content">
            <!-- 用户头像 -->
            <el-avatar 
              :size="32" 
              :src="comment.userAvatar" 
              class="comment-avatar"
            >{{ getInitials(comment.userNickname) }}</el-avatar>
            
            <!-- 评论信息 -->
            <div class="comment-info">
              <!-- 评论元数据 -->
              <div class="comment-meta">
                <span class="username">{{ comment.userNickname }}</span>
                <span class="time">{{ formatTime(comment.createdAt) }}</span>
              </div>
              
              <!-- 评论内容 -->
              <div class="comment-text">
                {{ comment.content }}
              </div>
              
              <!-- 回复链接 -->
              <div class="comment-actions">
                <el-button 
                  link 
                  size="small" 
                  @click="openReplyInput(comment.id, comment.userId, comment.userNickname)"
                >
                  回复
                </el-button>
                <el-button 
                  v-if="canDeleteComment(comment.userId)" 
                  link 
                  type="danger" 
                  size="small"
                  @click="handleDeleteComment(comment.id)"
                >
                  删除
                </el-button>
              </div>
              
              <!-- 回复输入框 -->
              <div v-if="activeReplyId === comment.id" class="reply-input-wrapper">
                <el-input
                  v-model="replyContent"
                  placeholder="回复评论..."
                  size="small"
                  maxlength="1000"
                  show-word-limit
                >
                  <template #append>
                    <div class="reply-controls">
                      <el-checkbox v-model="isPrivateReply" label="私密" size="small" />
                      <el-button type="primary" size="small" @click="submitReply" :disabled="!replyContent.trim() || submitting">
                        回复
                      </el-button>
                    </div>
                  </template>
                </el-input>
              </div>
              
              <!-- 回复列表 -->
              <div v-if="comment.replies && comment.replies.length > 0" class="replies-container">
                <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                  <el-avatar 
                    :size="24" 
                    :src="reply.userAvatar" 
                    class="reply-avatar"
                  >{{ getInitials(reply.userNickname) }}</el-avatar>
                  
                  <div class="reply-content">
                    <div class="reply-meta">
                      <span class="username">{{ reply.userNickname }}</span>
                      <template v-if="reply.replyToUserId">
                        <span class="reply-to">回复</span>
                        <span class="username">{{ reply.replyToUserNickname }}</span>
                      </template>
                      <span class="time">{{ formatTime(reply.createdAt) }}</span>
                    </div>
                    
                    <div class="reply-text">
                      {{ reply.content }}
                    </div>
                    
                    <div class="reply-actions">
                      <el-button 
                        link 
                        size="small" 
                        @click="openReplyInput(comment.id, reply.userId, reply.userNickname)"
                      >
                        回复
                      </el-button>
                      <el-button 
                        v-if="canDeleteComment(reply.userId)" 
                        link 
                        type="danger" 
                        size="small"
                        @click="handleDeleteComment(reply.id)"
                      >
                        删除
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 底部评论输入框 -->
      <div class="comment-input-container">
        <el-input
          v-model="commentContent"
          placeholder="添加评论..."
          maxlength="1000"
          show-word-limit
        >
          <template #append>
            <div class="comment-controls">
              <el-checkbox v-model="isPrivate" label="私密" />
              <el-button type="primary" @click="submitComment" :disabled="!commentContent.trim() || submitting">
                评论
              </el-button>
            </div>
          </template>
        </el-input>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { commentMoment, replyComment, getMomentComments, getMomentCommentsWithReplies, deleteComment } from '@/api/moment';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const props = defineProps({
  momentId: {
    type: Number,
    required: true
  },
  currentUserId: {
    type: Number,
    required: true
  }
});

const emit = defineEmits(['close', 'updated']);

// 状态
const comments = ref([]);
const totalComments = ref(0);
const loading = ref(false);
const submitting = ref(false);
const commentContent = ref('');
const replyContent = ref('');
const isPrivate = ref(false);
const isPrivateReply = ref(false);
const activeReplyId = ref(null);
const activeReplyToUserId = ref(null);
const activeReplyToUserName = ref('');
const commentListRef = ref(null);

// 获取评论列表
const fetchComments = async () => {
  loading.value = true;
  try {
    const response = await getMomentCommentsWithReplies(props.momentId);
    if (response.code === 200 && response.data) {
      comments.value = response.data;
      totalComments.value = countTotalComments(response.data);
    } else {
      console.error('Failed to fetch comments:', response);
      ElMessage.error('获取评论失败');
    }
  } catch (error) {
    console.error('Failed to fetch comments:', error);
    ElMessage.error('获取评论失败');
  } finally {
    loading.value = false;
  }
};

// 计算总评论数（包括回复）
const countTotalComments = (commentList) => {
  if (!commentList || !commentList.length) return 0;
  
  return commentList.reduce((total, comment) => {
    const replyCount = comment.replies ? comment.replies.length : 0;
    return total + 1 + replyCount;
  }, 0);
};

// 提交评论
const submitComment = async () => {
  if (!commentContent.value.trim() || submitting.value) return;
  
  submitting.value = true;
  try {
    const response = await commentMoment(props.momentId, commentContent.value, isPrivate.value);
    if (response.code === 200 && response.data) {
      ElMessage.success('评论成功');
      commentContent.value = '';
      isPrivate.value = false;
      await fetchComments();
      emit('updated');
    } else {
      ElMessage.error('评论失败');
    }
  } catch (error) {
    console.error('Failed to submit comment:', error);
    ElMessage.error('评论失败');
  } finally {
    submitting.value = false;
  }
};

// 打开回复输入框
const openReplyInput = (commentId, userId, nickname) => {
  activeReplyId.value = commentId;
  activeReplyToUserId.value = userId;
  activeReplyToUserName.value = nickname;
  replyContent.value = '';
  isPrivateReply.value = false;
  
  // 滚动到评论区域
  nextTick(() => {
    const replyInput = document.querySelector('.reply-input-wrapper');
    if (replyInput) {
      replyInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
      replyInput.querySelector('input').focus();
    }
  });
};

// 提交回复
const submitReply = async () => {
  if (!replyContent.value.trim() || submitting.value) return;
  
  submitting.value = true;
  try {
    const response = await replyComment(
      props.momentId, 
      activeReplyId.value, 
      replyContent.value, 
      activeReplyToUserId.value, 
      isPrivateReply.value
    );
    
    if (response.code === 200 && response.data) {
      ElMessage.success('回复成功');
      replyContent.value = '';
      isPrivateReply.value = false;
      activeReplyId.value = null;
      await fetchComments();
      emit('updated');
    } else {
      ElMessage.error('回复失败');
    }
  } catch (error) {
    console.error('Failed to submit reply:', error);
    ElMessage.error('回复失败');
  } finally {
    submitting.value = false;
  }
};

// 删除评论
const handleDeleteComment = async (commentId) => {
  try {
    const confirmed = await ElMessageBox.confirm(
      '确认删除此评论？此操作不可撤销。',
      '删除评论',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    if (confirmed) {
      const response = await deleteComment(props.momentId, commentId);
      if (response.code === 200 && response.data) {
        ElMessage.success('删除成功');
        await fetchComments();
        emit('updated');
      } else {
        ElMessage.error('删除失败');
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete comment:', error);
      ElMessage.error('删除失败');
    }
  }
};

// 判断是否可以删除评论
const canDeleteComment = (userId) => {
  return userId === props.currentUserId;
};

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '';
  return dayjs(timestamp).fromNow();
};

// 获取用户名首字母作为头像备用显示
const getInitials = (name) => {
  if (!name) return '?';
  return name.charAt(0).toUpperCase();
};

// 组件挂载时获取评论
onMounted(() => {
  fetchComments();
});

// 导出方法给父组件使用
defineExpose({
  refresh: fetchComments
});
</script>

<style scoped>
.comment-dialog {
  width: 100%;
  height: 100%;
}

.comment-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-title {
  font-size: 16px;
  font-weight: 500;
}

.comment-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
  max-height: 60vh;
}

.comment-item {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-content {
  display: flex;
  gap: 12px;
}

.comment-info {
  flex: 1;
}

.comment-meta {
  margin-bottom: 4px;
}

.username {
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-right: 8px;
}

.time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.comment-text {
  margin-bottom: 8px;
  word-break: break-word;
  white-space: pre-wrap;
}

.comment-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.reply-input-wrapper {
  margin: 8px 0;
}

.replies-container {
  margin-top: 12px;
  padding-left: 8px;
  border-left: 2px solid var(--el-border-color-lighter);
}

.reply-item {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.reply-item:last-child {
  margin-bottom: 0;
}

.reply-content {
  flex: 1;
}

.reply-meta {
  margin-bottom: 4px;
}

.reply-to {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin: 0 4px;
}

.reply-text {
  margin-bottom: 4px;
  word-break: break-word;
}

.reply-actions {
  display: flex;
  gap: 8px;
}

.comment-input-container {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.comment-controls, .reply-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

:deep(.el-card__body) {
  padding-top: 0;
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}
</style>