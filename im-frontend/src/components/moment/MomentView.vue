<template>
  <div class="moment-view">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <h2 class="view-title">朋友圈</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon class="mr-1"><Plus /></el-icon>
        发布动态
      </el-button>
    </div>
    
    <!-- 内容区域 -->
    <div class="content-area">
      <!-- 动态列表 -->
      <moment-list ref="momentListRef" :userId="currentUser.id" @refresh="handleRefresh" @comment="openCommentDialog" />
    </div>
    
    <!-- 发布动态对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="发布动态"
      width="650px"
      :show-close="false"
      :close-on-click-modal="false"
    >
      <create-moment @close="createDialogVisible = false" @published="handlePublished" />
    </el-dialog>
    
    <!-- 评论对话框 -->
    <el-dialog
      v-model="commentDialogVisible"
      title="评论"
      width="500px"
      destroy-on-close
    >
      <comment-dialog 
        v-if="commentDialogVisible" 
        :moment-id="activeMomentId" 
        :current-user-id="currentUser.id" 
        @close="commentDialogVisible = false"
        @updated="handleCommentUpdated"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, provide } from 'vue';
import { ElButton, ElDialog } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import MomentList from './MomentList.vue';
import CreateMoment from './CreateMoment.vue';
import CommentDialog from './CommentDialog.vue';
import { userApi } from '@/api/user';

// 组件状态
const momentListRef = ref(null);
const createDialogVisible = ref(false);
const commentDialogVisible = ref(false);
const activeMomentId = ref(null);
const currentUser = ref({});

// 获取当前用户信息
const fetchCurrentUser = async () => {
  try {
    console.log('获取当前用户信息...');
    const response = await userApi.getProfile();
    if (response.code === 200 && response.data) {
      currentUser.value = response.data;
      console.log('成功获取用户信息:', currentUser.value);
      
      // 获取到用户信息后刷新动态列表
      setTimeout(() => {
        momentListRef.value?.refresh();
      }, 100);
    } else {
      console.error('获取用户信息响应无效:', response);
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
};

// 将当前用户信息提供给子组件
provide('currentUser', currentUser);

// 打开创建动态对话框
const openCreateDialog = () => {
  createDialogVisible.value = true;
};

// 处理动态发布成功事件
const handlePublished = (moment) => {
  createDialogVisible.value = false;
  // 刷新动态列表，显示最新发布的内容
  momentListRef.value?.refresh();
};

// 处理列表刷新事件
const handleRefresh = () => {
  console.log('List refreshed');
};

// 打开评论对话框
const openCommentDialog = (momentId) => {
  activeMomentId.value = momentId;
  commentDialogVisible.value = true;
};

// 评论更新事件处理
const handleCommentUpdated = () => {
  // 刷新动态列表，更新评论数
  momentListRef.value?.refresh();
};

onMounted(() => {
  // 组件挂载时获取用户信息
  fetchCurrentUser();
});

// 暴露刷新方法
defineExpose({
  refresh: () => momentListRef.value?.refresh()
});
</script>

<style scoped>
.moment-view {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.top-bar {
  padding: 18px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 2px solid #cbd5e1;
  border-left: 6px solid #94a3b8;
  border-radius: 12px;
  margin: 12px 16px;
  background-color: white;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.top-bar:hover {
  background-color: #f1f5f9;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
  border-left: 6px solid #2563eb;
  border-color: #60a5fa;
}

.view-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.content-area {
  flex: 1;
  overflow: hidden;
}

.mr-1 {
  margin-right: 4px;
}

/* 发布动态按钮样式 */
:deep(.el-button--primary) {
  border: 2px solid #1e293b;
  border-radius: 10px;
  padding: 12px 20px;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.2);
  transition: all 0.3s;
  background-color: #1e293b;
  border-color: #1e293b;
}

:deep(.el-button--primary:hover) {
  background-color: #0f172a;
  border-color: #0f172a;
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(15, 23, 42, 0.4);
}

/* 修改对话框样式，移除padding使CreateMoment组件可以占满 */
:deep(.el-dialog__body) {
  padding: 0;
}
</style>