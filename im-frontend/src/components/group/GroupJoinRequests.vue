<!-- 群组加入请求管理组件 -->
<template>
  <div class="group-join-requests">
    <div class="requests-header">
      <h3>群组加入请求</h3>
      <div class="filter-options">
        <el-radio-group v-model="statusFilter" size="small" @change="loadRequests">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="PENDING">待处理</el-radio-button>
          <el-radio-button label="ACCEPTED">已接受</el-radio-button>
          <el-radio-button label="REJECTED">已拒绝</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton animated :rows="3" />
    </div>
    
    <div v-else-if="joinRequests.length === 0" class="empty-container">
      <el-empty description="暂无加入请求" />
    </div>
    
    <div v-else class="requests-list">
      <div
        v-for="request in joinRequests"
        :key="request.id"
        class="request-item"
      >
        <div class="request-header">
          <el-avatar :size="40" :src="request.avatarUrl">
            {{ (request.nickname || request.username || '?').substring(0, 1) }}
          </el-avatar>
          <div class="request-info">
            <div class="request-user">{{ request.nickname || request.username }}</div>
            <div class="request-time">
              申请时间: {{ formatDate(request.createdAt) }}
            </div>
          </div>
          <div class="request-status" :class="getStatusClass(request.status)">
            {{ request.statusDescription }}
          </div>
        </div>
        
        <div v-if="request.message" class="request-message">
          {{ request.message }}
        </div>
        
        <div class="request-actions" v-if="request.canHandle && request.status === 'PENDING'">
          <el-button 
            type="primary" 
            size="small" 
            @click="handleRequest(request.id, true)"
            :loading="processingRequests[request.id]?.approving"
          >
            批准
          </el-button>
          <el-button 
            type="danger" 
            size="small" 
            @click="handleRequest(request.id, false)"
            :loading="processingRequests[request.id]?.rejecting"
          >
            拒绝
          </el-button>
        </div>
        
        <div class="request-actions" v-else-if="request.canCancel && request.status === 'PENDING'">
          <el-button 
            type="warning" 
            size="small" 
            @click="cancelRequest(request.id)"
            :loading="processingRequests[request.id]?.cancelling"
          >
            取消申请
          </el-button>
        </div>
        
        <div class="request-result" v-if="request.status !== 'PENDING' && request.handlerUsername">
          由 <span class="handler-name">{{ request.handlerNickname || request.handlerUsername }}</span> 
          于 {{ formatDate(request.handledAt) }} 处理
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, defineProps, defineEmits, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { getGroupJoinRequests, handleJoinRequest, cancelJoinRequest } from '@/api/group';
import { formatDate } from '@/utils/helpers';

// 定义组件属性
const props = defineProps({
  groupId: {
    type: Number,
    required: true
  },
  autoReload: {
    type: Boolean,
    default: true
  }
});

// 定义组件事件
const emit = defineEmits(['update:count']);

// 状态变量
const loading = ref(false);
const joinRequests = ref<any[]>([]);
const statusFilter = ref('PENDING'); // 默认显示待处理请求
const currentPage = ref(1);
const pageSize = ref(10);
const totalCount = ref(0);
const totalPages = ref(0);
const processingRequests = ref<Record<number, { approving?: boolean, rejecting?: boolean, cancelling?: boolean }>>({});

// 加载加入请求数据
const loadRequests = async () => {
  loading.value = true;
  
  try {
    const params: {
      status?: string;
      page: number;
      size: number;
    } = {
      page: currentPage.value - 1,
      size: pageSize.value
    };
    
    if (statusFilter.value) {
      params.status = statusFilter.value;
    }
    
    // 确保使用正确的群组ID
    if (!props.groupId) {
      ElMessage.warning('未指定群组ID');
      loading.value = false;
      return;
    }
    
    console.log(`正在获取群组[${props.groupId}]的加入请求...`);
    const response = await getGroupJoinRequests(props.groupId, params);
    
    if (response.code === 200) {
      joinRequests.value = response.data.content || [];
      totalCount.value = response.data.totalElements || 0;
      totalPages.value = response.data.totalPages || 0;
      
      // 发送待处理请求数量
      const pendingCount = statusFilter.value === 'PENDING' ? 
        totalCount.value : 
        joinRequests.value.filter(req => req.status === 'PENDING').length;
      
      emit('update:count', pendingCount);
    } else {
      ElMessage.error(response.message || '获取加入请求失败');
    }
  } catch (error) {
    console.error('获取加入请求失败:', error);
    ElMessage.error('获取加入请求失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 处理分页
const handlePageChange = (page: number) => {
  currentPage.value = page;
  loadRequests();
};

// 处理请求（批准或拒绝）
const handleRequest = async (requestId: number, approve: boolean) => {
  if (!requestId) return;
  
  // 设置处理状态
  if (!processingRequests.value[requestId]) {
    processingRequests.value[requestId] = {};
  }
  
  if (approve) {
    processingRequests.value[requestId].approving = true;
  } else {
    processingRequests.value[requestId].rejecting = true;
  }
  
  try {
    const response = await handleJoinRequest(props.groupId, requestId, approve);
    
    if (response.code === 200) {
      ElMessage.success(approve ? '已批准加入请求' : '已拒绝加入请求');
      loadRequests(); // 重新加载请求列表
    } else {
      ElMessage.error(response.message || '处理请求失败');
    }
  } catch (error) {
    console.error('处理请求失败:', error);
    ElMessage.error('处理请求失败，请稍后重试');
  } finally {
    if (approve) {
      processingRequests.value[requestId].approving = false;
    } else {
      processingRequests.value[requestId].rejecting = false;
    }
  }
};

// 取消请求
const cancelRequest = async (requestId: number) => {
  if (!requestId) return;
  
  // 设置处理状态
  if (!processingRequests.value[requestId]) {
    processingRequests.value[requestId] = {};
  }
  
  processingRequests.value[requestId].cancelling = true;
  
  try {
    const response = await cancelJoinRequest(requestId);
    
    if (response.code === 200) {
      ElMessage.success('已取消加入请求');
      loadRequests(); // 重新加载请求列表
    } else {
      ElMessage.error(response.message || '取消请求失败');
    }
  } catch (error) {
    console.error('取消请求失败:', error);
    ElMessage.error('取消请求失败，请稍后重试');
  } finally {
    processingRequests.value[requestId].cancelling = false;
  }
};

// 获取状态对应的样式类名
const getStatusClass = (status: string) => {
  switch (status) {
    case 'PENDING': return 'status-pending';
    case 'ACCEPTED': return 'status-accepted';
    case 'REJECTED': return 'status-rejected';
    case 'CANCELLED': return 'status-cancelled';
    default: return '';
  }
};

// 组件挂载时加载数据
onMounted(() => {
  loadRequests();
  
  // 如果自动刷新，设置定时器
  let timer: number | null = null;
  
  if (props.autoReload) {
    timer = window.setInterval(() => {
      loadRequests();
    }, 30000); // 每30秒刷新一次
  }
  
  // 组件卸载时清除定时器
  onUnmounted(() => {
    if (timer !== null) {
      clearInterval(timer);
    }
  });
});

// 监视groupId变化，重新加载数据
watch(() => props.groupId, () => {
  currentPage.value = 1;
  loadRequests();
});
</script>

<style scoped>
.group-join-requests {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.requests-header {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.requests-header h3 {
  margin: 0;
}

.loading-container, .empty-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.requests-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.request-item {
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  margin-bottom: 16px;
  background-color: #fff;
}

.request-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.request-info {
  margin-left: 12px;
  flex: 1;
}

.request-user {
  font-weight: 500;
  margin-bottom: 4px;
}

.request-time {
  font-size: 12px;
  color: #909399;
}

.request-status {
  font-weight: 500;
}

.status-pending {
  color: #e6a23c;
}

.status-accepted {
  color: #67c23a;
}

.status-rejected {
  color: #f56c6c;
}

.status-cancelled {
  color: #909399;
}

.request-message {
  margin: 8px 0;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  color: #606266;
}

.request-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.request-result {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}

.handler-name {
  font-weight: 500;
  color: #606266;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style> 