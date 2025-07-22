<!-- 群组搜索组件 -->
<template>
  <div class="group-search">
    <div class="search-header">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索群组名称或ID"
        class="search-input"
        clearable
        @keyup.enter="search"
      >
        <template #append>
          <el-button @click="search">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
    </div>

    <div v-if="isSearching" class="loading-container">
      <el-skeleton animated :rows="5" />
    </div>
    
    <div v-else-if="hasSearched && !hasResults" class="no-results">
      <el-empty description="未找到相关群组" />
    </div>
    
    <div v-else-if="hasSearched && searchResults.length > 0" class="search-results">
      <div
        v-for="group in searchResults"
        :key="group.id"
        class="group-item"
        :class="{ 'banned': group.isBanned }"
        @click="viewGroupDetail(group)"
      >
        <el-avatar :size="50" :src="group.avatarUrl">
          {{ group.name.substring(0, 1) }}
        </el-avatar>
        <div class="group-info">
          <div class="group-name">{{ group.name }}</div>
          <div class="group-meta">
            <span>{{ group.memberCount }}人</span>
            <span v-if="group.isMember" class="group-status">已加入</span>
            <span v-else-if="group.hasPendingRequest" class="group-status pending">待审批</span>
            <span v-else-if="group.isBanned" class="group-status banned">已封禁</span>
          </div>
          <div v-if="group.description" class="group-description">
            {{ truncateText(group.description, 50) }}
          </div>
          <div v-if="group.isBanned" class="banned-reason">
            <span class="banned-icon">🚫</span>
            <span class="banned-text">该群组已被封禁{{group.bannedReason ? '，原因：' + group.bannedReason : ''}}</span>
          </div>
        </div>
      </div>
      
      <div class="pagination" v-if="totalPages > 1">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalCount"
          @current-change="handlePageChange"
          layout="prev, pager, next"
        />
      </div>
    </div>

    <!-- 群组详情对话框 -->
    <el-dialog
      v-model="showGroupDetailDialog"
      :title="selectedGroup?.name"
      width="500px"
    >
      <div class="group-detail" v-if="selectedGroup">
        <div v-if="selectedGroup.isBanned" class="banned-banner">
          <span class="banned-icon">🚫</span>
          <span class="banned-text">该群组已被封禁</span>
        </div>

        <div class="group-header">
          <el-avatar :size="80" :src="selectedGroup.avatarUrl">
            {{ selectedGroup.name.substring(0, 1) }}
          </el-avatar>
          <div class="group-info">
            <h3>{{ selectedGroup.name }}</h3>
            <div class="group-meta">
              <div><strong>群ID：</strong> {{ selectedGroup.id }}</div>
              <div><strong>成员数：</strong> {{ selectedGroup.memberCount }}人</div>
              <div><strong>群主：</strong> {{ selectedGroup.ownerName }}</div>
              <div><strong>创建时间：</strong> {{ safeFormatDate(selectedGroup.createdAt) }}</div>
              <div v-if="selectedGroup.isBanned" class="banned-info">
                <strong>封禁原因：</strong> {{ selectedGroup.bannedReason || '未提供' }}
              </div>
              <div v-if="selectedGroup.isBanned && selectedGroup.bannedUntil" class="banned-info">
                <strong>封禁截止时间：</strong> {{ safeFormatDate(selectedGroup.bannedUntil) }}
              </div>
            </div>
          </div>
        </div>

        <div class="group-description" v-if="selectedGroup.description">
          <strong>群介绍：</strong>
          <p>{{ selectedGroup.description }}</p>
        </div>

        <div class="group-actions">
          <el-button 
            type="primary" 
            :disabled="selectedGroup.isMember || selectedGroup.hasPendingRequest || selectedGroup.isBanned"
            @click="handleJoinGroup"
          >
            {{ joinButtonText }}
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 申请加入群组对话框 -->
    <el-dialog
      v-model="showJoinDialog"
      title="申请加入群组"
      width="500px"
    >
      <div class="join-form" v-if="selectedGroup">
        <el-form :model="joinForm" label-position="top">
          <el-form-item label="群组名称">
            <el-input v-model="selectedGroup.name" disabled />
          </el-form-item>
          <el-form-item label="申请消息">
            <el-input
              v-model="joinForm.message"
              type="textarea"
              :rows="3"
              placeholder="请输入申请消息"
            />
          </el-form-item>
        </el-form>
        <div class="join-actions">
          <el-button @click="showJoinDialog = false">取消</el-button>
          <el-button type="primary" @click="submitJoinRequest" :loading="submittingJoin">提交申请</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { searchGroups, getSearchableGroupById, applyToJoinGroup } from '@/api/group';
import { formatDate } from '@/utils/helpers';

// 安全地格式化日期
const safeFormatDate = (dateStr: string | number | Date | null | undefined): string => {
  if (!dateStr) return '未知';
  
  try {
    const date = new Date(dateStr);
    // 检查是否是有效的日期对象
    if (isNaN(date.getTime())) {
      return '无效日期';
    }
    return formatDate(date, 'yyyy-MM-dd HH:mm');
  } catch (e) {
    console.error('日期格式化错误:', e, dateStr);
    return '无效日期';
  }
};

// 搜索状态
const searchKeyword = ref('');
const isSearching = ref(false);
const hasSearched = ref(false);
const searchResults = ref<any[]>([]);
const totalCount = ref(0);
const totalPages = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

// 群组详情
const showGroupDetailDialog = ref(false);
const selectedGroup = ref<any>(null);

// 加入群组
const showJoinDialog = ref(false);
const joinForm = ref({
  message: '你好，我想加入贵群'
});
const submittingJoin = ref(false);

// 计算属性
const hasResults = computed(() => searchResults.value.length > 0);
const joinButtonText = computed(() => {
  if (!selectedGroup.value) return '加入群组';
  if (selectedGroup.value.isMember) return '已加入';
  if (selectedGroup.value.hasPendingRequest) return '已申请加入';
  if (selectedGroup.value.isBanned) return '群组已封禁';
  return selectedGroup.value.requiresApproval ? '申请加入' : '加入群组';
});

// 方法
const search = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词');
    return;
  }

  isSearching.value = true;
  hasSearched.value = true;

  try {
    const response = await searchGroups({
      keyword: searchKeyword.value,
      page: currentPage.value - 1,
      size: pageSize.value
    });

    if (response.code === 200 && response.data) {
      searchResults.value = response.data.content || [];
      totalCount.value = response.data.totalElements || 0;
      totalPages.value = response.data.totalPages || 0;
    } else {
      searchResults.value = [];
      totalCount.value = 0;
      totalPages.value = 0;
      ElMessage.error(response.message || '搜索失败');
    }
  } catch (error) {
    console.error('搜索群组失败:', error);
    ElMessage.error('搜索群组失败，请稍后重试');
    searchResults.value = [];
    totalCount.value = 0;
    totalPages.value = 0;
  } finally {
    isSearching.value = false;
  }
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
  search();
};

const viewGroupDetail = async (group: any) => {
  selectedGroup.value = group;
  
  try {
    // 获取最新的群组详情
    const response = await getSearchableGroupById(group.id);
    
    if (response.code === 200 && response.data) {
      selectedGroup.value = response.data;
    }
  } catch (error) {
    console.error('获取群组详情失败:', error);
  }
  
  showGroupDetailDialog.value = true;
};

const handleJoinGroup = () => {
  if (!selectedGroup.value) return;
  
  // 如果已经是成员或已申请加入，则不处理
  if (selectedGroup.value.isMember || selectedGroup.value.hasPendingRequest) {
    return;
  }
  
  // 如果需要审批，显示申请对话框
  if (selectedGroup.value.requiresApproval) {
    joinForm.value.message = '你好，我想加入贵群';
    showJoinDialog.value = true;
  } else {
    // 不需要审批，直接加入
    submitJoinRequest();
  }
};

const submitJoinRequest = async () => {
  if (!selectedGroup.value) return;
  
  submittingJoin.value = true;
  
  try {
    const response = await applyToJoinGroup({
      groupId: selectedGroup.value.id,
      message: joinForm.value.message
    });
    
    if (response.code === 200) {
      if (selectedGroup.value.requiresApproval) {
        ElMessage.success('申请已发送，请等待管理员审批');
        selectedGroup.value.hasPendingRequest = true;
      } else {
        ElMessage.success('已成功加入群组');
        selectedGroup.value.isMember = true;
      }
      
      showJoinDialog.value = false;
    } else {
      ElMessage.error(response.message || '申请加入群组失败');
    }
  } catch (error) {
    console.error('申请加入群组失败:', error);
    ElMessage.error('申请加入群组失败，请稍后重试');
  } finally {
    submittingJoin.value = false;
  }
};

const truncateText = (text: string, maxLength: number) => {
  if (!text || text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
};

// 当组件挂载时，如果已有关键词，则自动搜索
watch(() => searchKeyword.value, (newValue) => {
  if (!newValue) {
    hasSearched.value = false;
    searchResults.value = [];
  }
}, { immediate: true });
</script>

<style scoped>
.group-search {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-header {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
}

.search-input {
  width: 100%;
}

.loading-container {
  flex: 1;
  padding: 16px;
}

.no-results {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-results {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.group-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.group-item:hover {
  background-color: #f5f7fa;
}

.group-item.banned {
  background-color: #fde2e2; /* 浅红色背景 */
  border-left: 4px solid #f56c6c; /* 红色边框 */
  padding-left: 12px; /* 调整内边距 */
}

.group-info {
  margin-left: 12px;
  flex: 1;
}

.group-name {
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 4px;
}

.group-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
}

.group-meta span {
  margin-right: 12px;
}

.group-status {
  color: #67c23a;
}

.group-status.pending {
  color: #e6a23c;
}

.group-status.banned {
  color: #f56c6c; /* 红色 */
}

.group-description {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banned-reason {
  margin-top: 8px;
  font-size: 12px;
  color: #f56c6c; /* 红色 */
  display: flex;
  align-items: center;
}

.banned-icon {
  margin-right: 4px;
}

.banned-text {
  display: inline-block;
  word-break: break-all;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.group-detail {
  padding: 8px;
}

.group-header {
  display: flex;
  margin-bottom: 20px;
}

.group-header .group-info {
  margin-left: 16px;
}

.group-header h3 {
  margin-top: 0;
  margin-bottom: 8px;
}

.group-header .group-meta {
  font-size: 14px;
  color: #606266;
}

.group-header .group-meta div {
  margin-bottom: 4px;
}

.banned-banner {
  background-color: #fde2e2; /* 浅红色背景 */
  color: #f56c6c; /* 红色文字 */
  padding: 8px 12px;
  margin-bottom: 20px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
}

.banned-info {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
}

.group-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.join-form {
  padding: 8px;
}

.join-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style> 