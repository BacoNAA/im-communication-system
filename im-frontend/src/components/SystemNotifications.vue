<template>
  <div class="system-notifications-container">
    <!-- 通知列表面板 -->
    <div class="notifications-panel" v-if="!showDetailView">
      <div class="notifications-header">
        <h2>系统通知</h2>
        <div class="notifications-actions">
          <button 
            class="btn-filter" 
            :class="{ active: unreadOnly }" 
            @click="toggleUnreadOnly"
            :title="unreadOnly ? '查看所有通知' : '只看未读'"
          >
            <i class="fa-solid" :class="unreadOnly ? 'fa-filter-circle-xmark' : 'fa-filter'"></i>
          </button>
          <button 
            v-if="hasUnreadNotifications" 
            class="btn-mark-all" 
            @click="handleMarkAllAsRead"
            title="全部标为已读"
          >
            <i class="fa-solid fa-check-double"></i>
          </button>
          <button 
            class="btn-refresh" 
            @click="refreshNotifications"
            title="刷新通知"
          >
            <i class="fa-solid fa-rotate"></i>
          </button>
        </div>
      </div>

      <div class="notifications-list" v-if="notifications.length > 0">
        <div 
          v-for="notification in notifications" 
          :key="notification.id"
          class="notification-item"
          :class="{ unread: !notification.isRead }"
          @click="viewNotificationDetail(notification.id)"
        >
          <div class="notification-badge" v-if="!notification.isRead"></div>
          <div class="notification-content">
            <div class="notification-title">{{ notification.title }}</div>
            <div class="notification-summary">{{ notification.summary }}</div>
            <div class="notification-meta">
              <span class="notification-type" :class="notification.type">{{ getNotificationType(notification.type) }}</span>
              <span class="notification-time">{{ formatDate(notification.publishedAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="empty-notifications" v-else-if="!loading">
        <div class="empty-icon">📭</div>
        <p>{{ unreadOnly ? '没有未读通知' : '暂无系统通知' }}</p>
        <button 
          v-if="unreadOnly" 
          class="btn-view-all"
          @click="toggleUnreadOnly"
        >
          查看全部通知
        </button>
      </div>

      <div class="notifications-loading" v-if="loading">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="totalCount > 0">
        <button 
          :disabled="currentPage === 0" 
          @click="changePage(currentPage - 1)" 
          class="page-btn"
        >
          <i class="fa-solid fa-chevron-left"></i>
        </button>
        <span class="page-info">{{ currentPage + 1 }} / {{ Math.ceil(totalCount / pageSize) }}</span>
        <button 
          :disabled="currentPage >= Math.ceil(totalCount / pageSize) - 1" 
          @click="changePage(currentPage + 1)" 
          class="page-btn"
        >
          <i class="fa-solid fa-chevron-right"></i>
        </button>
      </div>
    </div>

    <!-- 通知详情面板 -->
    <div class="notification-detail" v-if="showDetailView">
      <div class="detail-header">
        <button class="btn-back" @click="closeDetailView">
          <i class="fa-solid fa-arrow-left"></i>
        </button>
        <h2>通知详情</h2>
        <button 
          v-if="currentNotification && !currentNotification.isRead" 
          class="btn-mark-read"
          @click="handleMarkAsRead"
        >
          标为已读
        </button>
      </div>

      <div class="detail-content" v-if="currentNotification">
        <h3 class="detail-title">{{ currentNotification.title }}</h3>
        <div class="detail-meta">
          <span class="detail-type" :class="currentNotification.type">
            {{ getNotificationType(currentNotification.type) }}
          </span>
          <span class="detail-time">{{ formatDate(currentNotification.publishedAt) }}</span>
        </div>
        <div class="detail-body">{{ currentNotification.content }}</div>
      </div>

      <div class="detail-loading" v-else-if="detailLoading">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useNotifications } from '@/composables/useNotifications';
import { ElMessage } from 'element-plus';
import type { SystemNotificationDetailResponse } from '@/api/notification';

// 使用通知组合式函数
const {
  notifications,
  loading,
  totalCount,
  currentPage,
  unreadOnly,
  unreadCount,
  hasUnreadNotifications,
  loadNotifications,
  getNotificationDetail,
  markAsRead,
  markAllAsRead,
  toggleUnreadOnly,
  refreshUnreadCount
} = useNotifications();

const pageSize = ref(10);
const showDetailView = ref(false);
const currentNotification = ref<SystemNotificationDetailResponse | null>(null);
const detailLoading = ref(false);

// 首次加载通知
onMounted(() => {
  loadNotifications();
});

// 刷新通知列表
const refreshNotifications = () => {
  loadNotifications(currentPage.value, pageSize.value);
};

// 切换页面
const changePage = (page: number) => {
  loadNotifications(page, pageSize.value);
};

// 查看通知详情
const viewNotificationDetail = async (notificationId: number) => {
  detailLoading.value = true;
  currentNotification.value = null;
  showDetailView.value = true;
  
  try {
    const detail = await getNotificationDetail(notificationId);
    if (detail) {
      currentNotification.value = detail;
    } else {
      ElMessage.error('获取通知详情失败');
      closeDetailView();
    }
  } finally {
    detailLoading.value = false;
  }
};

// 关闭详情视图
const closeDetailView = () => {
  showDetailView.value = false;
  currentNotification.value = null;
  // 刷新列表以更新已读状态
  refreshNotifications();
};

// 手动标记通知为已读
const handleMarkAsRead = async () => {
  if (currentNotification.value) {
    const success = await markAsRead(currentNotification.value.id);
    if (success) {
      currentNotification.value.isRead = true;
      ElMessage.success('已标记为已读');
    }
  }
};

// 标记所有通知为已读
const handleMarkAllAsRead = async () => {
  const confirmed = await ElMessage.confirm('确定将所有通知标记为已读吗？', '确认操作', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).catch(() => false);
  
  if (confirmed) {
    await markAllAsRead();
    refreshNotifications();
  }
};

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const now = new Date();
  
  // 今天的通知显示时间
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  
  // 昨天的通知
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  
  // 今年的通知
  if (date.getFullYear() === now.getFullYear()) {
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' ' +
      date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  
  // 其他时间
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
};

// 获取通知类型显示文本
const getNotificationType = (type: string) => {
  switch (type) {
    case 'system':
      return '系统';
    case 'announcement':
      return '公告';
    case 'maintenance':
      return '维护';
    case 'update':
      return '更新';
    default:
      return '其他';
  }
};
</script>

<style scoped>
.system-notifications-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.notifications-header {
  padding: 16px;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notifications-header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.notifications-actions {
  display: flex;
  gap: 10px;
}

.btn-filter, .btn-mark-all, .btn-refresh {
  background: none;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}

.btn-filter:hover, .btn-mark-all:hover, .btn-refresh:hover {
  background-color: #f0f0f0;
  color: #333;
}

.btn-filter.active {
  color: #1890ff;
  background-color: rgba(24, 144, 255, 0.1);
}

.notifications-list {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  position: relative;
  transition: background-color 0.2s;
  display: flex;
}

.notification-item:hover {
  background-color: #f9f9f9;
}

.notification-item.unread {
  background-color: rgba(24, 144, 255, 0.05);
}

.notification-badge {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #1890ff;
  position: absolute;
  left: 16px;
  top: 16px;
}

.notification-content {
  flex: 1;
  padding-left: 16px;
}

.notification-title {
  font-weight: 500;
  margin-bottom: 6px;
  color: #333;
}

.notification-summary {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
  line-height: 1.5;
}

.notification-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.notification-type {
  padding: 2px 6px;
  border-radius: 12px;
  font-size: 11px;
}

.notification-type.system {
  background-color: #e6f7ff;
  color: #1890ff;
}

.notification-type.announcement {
  background-color: #f6ffed;
  color: #52c41a;
}

.notification-type.maintenance {
  background-color: #fff7e6;
  color: #fa8c16;
}

.notification-type.update {
  background-color: #f9f0ff;
  color: #722ed1;
}

.notification-time {
  color: #999;
}

.empty-notifications {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.7;
}

.btn-view-all {
  margin-top: 16px;
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background-color: white;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-view-all:hover {
  color: #1890ff;
  border-color: #1890ff;
}

.notifications-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px;
  border-top: 1px solid #f0f0f0;
}

.page-btn {
  width: 30px;
  height: 30px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background-color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  margin: 0 10px;
  font-size: 13px;
  color: #666;
}

/* 详情视图样式 */
.notification-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.detail-header {
  padding: 16px;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
}

.btn-back {
  background: none;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #666;
  margin-right: 10px;
}

.btn-back:hover {
  background-color: #f0f0f0;
  color: #333;
}

.detail-header h2 {
  margin: 0;
  flex: 1;
  font-size: 18px;
  color: #333;
}

.btn-mark-read {
  padding: 6px 12px;
  border: 1px solid #1890ff;
  border-radius: 4px;
  background-color: white;
  color: #1890ff;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-mark-read:hover {
  background-color: #1890ff;
  color: white;
}

.detail-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.detail-title {
  margin: 0 0 16px 0;
  font-size: 20px;
  color: #333;
}

.detail-meta {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.detail-type {
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 12px;
  margin-right: 10px;
}

.detail-type.system {
  background-color: #e6f7ff;
  color: #1890ff;
}

.detail-type.announcement {
  background-color: #f6ffed;
  color: #52c41a;
}

.detail-type.maintenance {
  background-color: #fff7e6;
  color: #fa8c16;
}

.detail-type.update {
  background-color: #f9f0ff;
  color: #722ed1;
}

.detail-time {
  color: #999;
  font-size: 13px;
}

.detail-body {
  line-height: 1.8;
  color: #333;
  white-space: pre-line;
}

.detail-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  flex: 1;
  color: #999;
}
</style> 