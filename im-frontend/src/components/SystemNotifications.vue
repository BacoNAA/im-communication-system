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
            <span class="action-text">{{ unreadOnly ? '全部' : '未读' }}</span>
          </button>
          <button 
            v-if="hasUnreadNotifications" 
            class="btn-mark-all" 
            @click="handleMarkAllAsRead"
            title="全部标为已读"
          >
            <i class="fa-solid fa-check-double"></i>
            <span class="action-text">已读</span>
          </button>
          <button 
            class="btn-refresh" 
            @click="refreshNotifications"
            title="刷新通知"
          >
            <i class="fa-solid fa-rotate"></i>
            <span class="action-text">刷新</span>
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
          <span>返回列表</span>
        </button>
        <h2>通知详情</h2>
        <button 
          v-if="currentNotification && !currentNotification.isRead" 
          class="btn-mark-read"
          @click="handleMarkAsRead"
        >
          <i class="fa-solid fa-check-double"></i>
          <span>标为已读</span>
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
  background-color: #f8fafc;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  max-width: 1000px;
  margin: 0 auto;
}

.notifications-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
}

.notifications-header h2 {
  margin: 0;
  font-size: 20px;
  color: #1e293b;
  font-weight: 700;
}

.notifications-actions {
  display: flex;
  gap: 12px;
}

.btn-filter, .btn-mark-all, .btn-refresh {
  padding: 8px 16px;
  border: 2px solid;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: white;
  transition: all 0.3s;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
  font-size: 15px;
  min-width: 90px;
  gap: 8px;
}

.btn-filter, .btn-mark-all, .btn-refresh {
  background-color: #1e293b;
  border-color: #1e293b;
}

.action-text {
  font-weight: 500;
}

.btn-filter:hover, .btn-mark-all:hover, .btn-refresh:hover {
  background-color: #0f172a;
  transform: translateY(-3px);
  border-color: #0f172a;
  box-shadow: 0 6px 15px rgba(15, 23, 42, 0.4);
}

.btn-filter.active {
  color: white;
  background-color: #334155;
  border-color: #334155;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.3);
}

.notifications-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.notification-item {
  padding: 18px 24px;
  border: 2px solid #cbd5e1;
  border-left: 6px solid #94a3b8;
  margin: 12px 16px;
  cursor: pointer;
  position: relative;
  transition: all 0.3s;
  display: flex;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

.notification-item:hover {
  background-color: #f1f5f9;
  transform: translateY(-3px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
  border-left: 6px solid #2563eb;
  border-color: #60a5fa;
}

.notification-item.unread {
  background-color: rgba(59, 130, 246, 0.08);
  border-left: 6px solid #3b82f6;
  border-color: #93c5fd;
}

.notification-badge {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #ef4444;
  position: absolute;
  left: 8px;
  top: 20px;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.25);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(239, 68, 68, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0);
  }
}

.notification-content {
  flex: 1;
  padding-left: 20px;
}

.notification-title {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 8px;
  color: #1e293b;
}

.notification-summary {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 12px;
  line-height: 1.6;
}

.notification-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.notification-type {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.notification-type.system {
  background-color: #dbeafe;
  color: #1d4ed8;
  border-color: #93c5fd;
}

.notification-type.announcement {
  background-color: #dcfce7;
  color: #16a34a;
  border-color: #86efac;
}

.notification-type.maintenance {
  background-color: #fef3c7;
  color: #b45309;
  border-color: #fde68a;
}

.notification-type.update {
  background-color: #f3e8ff;
  color: #7e22ce;
  border-color: #d8b4fe;
}

.notification-time {
  color: #94a3b8;
  font-weight: 500;
}

.empty-notifications {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #64748b;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 24px;
  opacity: 0.8;
}

.empty-notifications p {
  font-size: 16px;
  margin-bottom: 16px;
  color: #475569;
}

.btn-view-all {
  margin-top: 16px;
  padding: 12px 24px;
  border: 2px solid #1e293b;
  border-radius: 10px;
  background-color: #1e293b;
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.2);
  font-size: 15px;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-view-all::before {
  content: "\f0b0"; /* Filter icon */
  font-family: "Font Awesome 6 Free";
  font-weight: 900;
}

.btn-view-all:hover {
  background-color: #0f172a;
  border-color: #0f172a;
  color: white;
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.4);
}

.notifications-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #64748b;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e2e8f0;
  border-top: 3px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px;
  border-top: 1px solid #e2e8f0;
  background: white;
}

.page-btn {
  width: 38px;
  height: 38px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background-color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  color: #3b82f6;
  font-size: 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  margin: 0 4px;
}

.page-btn:hover:not(:disabled) {
  border-color: #3b82f6;
  color: white;
  background-color: #3b82f6;
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  margin: 0 16px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

/* 详情视图样式 */
.notification-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
}

.detail-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.btn-back {
  background-color: #1e293b;
  border: 2px solid #1e293b;
  min-width: 110px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: white;
  margin-right: 15px;
  transition: all 0.3s;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.25);
  font-size: 15px;
  padding: 8px 16px;
  gap: 8px;
}

.btn-back:hover {
  background-color: #0f172a;
  color: white;
  transform: translateX(-4px);
  box-shadow: 0 6px 15px rgba(15, 23, 42, 0.4);
  border-color: #0f172a;
}

.detail-header h2 {
  margin: 0;
  flex: 1;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.btn-mark-read {
  padding: 10px 20px;
  border: 2px solid #1e293b;
  border-radius: 10px;
  background-color: #1e293b;
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 3px 8px rgba(15, 23, 42, 0.2);
  font-size: 15px;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 120px;
  justify-content: center;
}

.btn-mark-read:hover {
  background-color: #0f172a;
  color: white;
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.4);
  border-color: #0f172a;
}

.detail-content {
  flex: 1;
  padding: 30px;
  overflow-y: auto;
  background-color: white;
  border-radius: 0 0 16px 16px;
}

.detail-title {
  margin: 0 0 20px 0;
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.3;
}

.detail-meta {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.detail-type {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  margin-right: 15px;
  border: 1px solid;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.detail-type.system {
  background-color: #dbeafe;
  color: #1d4ed8;
  border-color: #93c5fd;
}

.detail-type.announcement {
  background-color: #dcfce7;
  color: #16a34a;
  border-color: #86efac;
}

.detail-type.maintenance {
  background-color: #fef3c7;
  color: #b45309;
  border-color: #fde68a;
}

.detail-type.update {
  background-color: #f3e8ff;
  color: #7e22ce;
  border-color: #d8b4fe;
}

.detail-time {
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

.detail-body {
  line-height: 1.8;
  color: #334155;
  white-space: pre-line;
  font-size: 16px;
  background-color: #f8fafc;
  padding: 24px;
  border-radius: 12px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
}

.detail-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  flex: 1;
  color: #64748b;
}
</style> 