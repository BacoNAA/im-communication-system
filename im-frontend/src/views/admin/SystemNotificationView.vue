<template>
  <div class="notification-management">
    <div class="page-header">
      <h1>系统通知</h1>
      <div class="header-actions">
        <div class="search-wrapper">
          <select v-model="typeFilter" @change="applyFilters">
            <option value="">所有类型</option>
            <option value="system">系统通知</option>
            <option value="announcement">公告</option>
            <option value="maintenance">维护</option>
            <option value="update">更新</option>
          </select>
        </div>
        <button class="create-btn" @click="showCreateDialog">
          <i class="fas fa-plus"></i> 新建通知
        </button>
      </div>
    </div>
    
    <div class="notification-table-container">
      <table class="notification-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>标题</th>
            <th>类型</th>
            <th>创建时间</th>
            <th>发布状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="notification in notifications" :key="notification.id">
            <td>{{ notification.id }}</td>
            <td class="notification-title">{{ notification.title }}</td>
            <td>
              <span :class="['type-badge', notification.type]">
                {{ getNotificationType(notification.type) }}
              </span>
            </td>
            <td>{{ formatDate(notification.createdAt) }}</td>
            <td>
              <span :class="['status-badge', notification.isPublished ? 'published' : 'draft']">
                {{ notification.isPublished ? '已发布' : '草稿' }}
              </span>
              <span v-if="notification.isPublished" class="publish-date">
                ({{ formatDate(notification.publishedAt) }})
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <button class="action-btn view" @click="viewNotificationDetails(notification.id)" title="查看详情">
                  <i class="fas fa-eye"></i>
                </button>
                <button 
                  v-if="!notification.isPublished" 
                  class="action-btn edit" 
                  @click="showEditDialog(notification)" 
                  title="编辑通知"
                >
                  <i class="fas fa-edit"></i>
                </button>
                <button 
                  v-if="!notification.isPublished" 
                  class="action-btn publish" 
                  @click="publishNotification(notification.id)" 
                  title="发布通知"
                >
                  <i class="fas fa-paper-plane"></i>
                </button>
                <button 
                  v-if="!notification.isPublished" 
                  class="action-btn delete" 
                  @click="deleteNotification(notification.id)" 
                  title="删除通知"
                >
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="isLoading">
            <td colspan="6" class="empty-table loading">
              <div class="loading-spinner"></div> 加载中...
            </td>
          </tr>
          <tr v-else-if="notifications.length === 0">
            <td colspan="6" class="empty-table">
              没有找到系统通知
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div class="table-footer">
      <div class="pagination">
        <button 
          :disabled="currentPage === 0" 
          @click="changePage(currentPage - 1)" 
          class="page-btn"
        >
          <i class="fas fa-chevron-left"></i>
        </button>
        
        <span class="page-info">{{ currentPage + 1 }} / {{ totalPages || 1 }}</span>
        
        <button 
          :disabled="currentPage >= totalPages - 1 || totalPages === 0" 
          @click="changePage(currentPage + 1)" 
          class="page-btn"
        >
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>
    </div>
    
    <!-- 新建/编辑通知对话框 -->
    <div v-if="showDialog" class="modal-overlay" @click="cancelDialog">
      <div class="modal-content" @click.stop>
        <h2>{{ isEditing ? '编辑通知' : '新建通知' }}</h2>
        
        <div class="form-group">
          <label for="title">通知标题</label>
          <input 
            id="title" 
            v-model="notificationForm.title" 
            placeholder="请输入通知标题..."
            maxlength="100"
          />
        </div>
        
        <div class="form-group">
          <label for="type">通知类型</label>
          <select id="type" v-model="notificationForm.type">
            <option value="system">系统通知</option>
            <option value="announcement">公告</option>
            <option value="maintenance">维护</option>
            <option value="update">更新</option>
          </select>
        </div>
        
        <div class="form-group">
          <label for="content">通知内容</label>
          <textarea 
            id="content" 
            v-model="notificationForm.content" 
            placeholder="请输入通知内容..."
            rows="6"
          ></textarea>
        </div>
        
        <div class="form-group">
          <label>发送范围</label>
          <div class="radio-group">
            <label class="radio-label">
              <input type="radio" v-model="targetType" value="all" />
              <span>所有用户</span>
            </label>
            <label class="radio-label">
              <input type="radio" v-model="targetType" value="specific" />
              <span>指定用户</span>
            </label>
          </div>
          
          <div v-if="targetType === 'specific'" class="target-users">
            <div class="user-input">
              <input 
                type="text" 
                v-model="userIdInput" 
                placeholder="输入用户ID（多个ID用逗号分隔）"
                @keyup.enter="addTargetUsers"
              />
              <button class="add-btn" @click="addTargetUsers">添加</button>
            </div>
            
            <div class="user-tags">
              <div v-for="(userId, index) in notificationForm.targetUserIds" :key="index" class="user-tag">
                用户ID: {{ userId }}
                <span class="remove-tag" @click="removeTargetUser(index)">&times;</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="modal-actions">
          <button class="btn cancel" @click="cancelDialog">取消</button>
          <button class="btn confirm" @click="submitNotification">确认</button>
        </div>
      </div>
    </div>
    
    <!-- 通知详情对话框 -->
    <div v-if="showDetail" class="modal-overlay" @click="closeDetailDialog">
      <div class="modal-content detail-modal" @click.stop>
        <h2>通知详情</h2>
        
        <div v-if="notificationDetail" class="notification-detail">
          <div class="detail-header">
            <h3>{{ notificationDetail.title }}</h3>
            <span :class="['type-badge', notificationDetail.type]">
              {{ getNotificationType(notificationDetail.type) }}
            </span>
          </div>
          
          <div class="detail-info">
            <div class="info-item">
              <span class="info-label">创建时间:</span>
              <span>{{ formatDate(notificationDetail.createdAt) }}</span>
            </div>
            
            <div class="info-item">
              <span class="info-label">发布状态:</span>
              <span :class="['status-text', notificationDetail.isPublished ? 'published' : 'draft']">
                {{ notificationDetail.isPublished ? '已发布' : '草稿' }}
              </span>
            </div>
            
            <div v-if="notificationDetail.isPublished" class="info-item">
              <span class="info-label">发布时间:</span>
              <span>{{ formatDate(notificationDetail.publishedAt) }}</span>
            </div>
            
            <div class="info-item">
              <span class="info-label">目标用户:</span>
              <span>{{ notificationDetail.targetType === 'all' ? '所有用户' : '指定用户' }}</span>
            </div>
          </div>
          
          <div class="detail-content">
            <h4>通知内容</h4>
            <div class="content-text">{{ notificationDetail.content }}</div>
          </div>
        </div>
        
        <div v-else class="detail-loading">
          <div class="loading-spinner"></div> 加载中...
        </div>
        
        <div class="modal-actions">
          <button class="btn" @click="closeDetailDialog">关闭</button>
          <button 
            v-if="notificationDetail && !notificationDetail.isPublished" 
            class="btn publish"
            @click="publishAndCloseDetail(notificationDetail.id)"
          >
            <i class="fas fa-paper-plane"></i> 发布
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import adminApi from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

// 分页与筛选
const currentPage = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const typeFilter = ref('')

// 通知列表
const notifications = ref([])

// 对话框相关
const showDialog = ref(false)
const isEditing = ref(false)
const notificationForm = reactive({
  title: '',
  content: '',
  type: 'system',
  targetUserIds: []
})
const targetType = ref('all')
const userIdInput = ref('')

// 详情对话框
const showDetail = ref(false)
const notificationDetail = ref(null)

// 获取通知类型显示文本
const getNotificationType = (type) => {
  switch (type) {
    case 'system': return '系统通知'
    case 'announcement': return '公告'
    case 'maintenance': return '维护'
    case 'update': return '更新'
    default: return '其他'
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '未知'
  
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime())) return '无效日期'
    
    return new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date)
  } catch (e) {
    console.error('日期格式化错误:', e)
    return '无效日期'
  }
}

// 应用筛选器
const applyFilters = () => {
  currentPage.value = 0
  fetchNotifications()
}

// 切换页面
const changePage = (page) => {
  if (page >= 0 && (page < totalPages.value || totalPages.value === 0)) {
    currentPage.value = page
    fetchNotifications()
  }
}

// 显示创建对话框
const showCreateDialog = () => {
  isEditing.value = false
  notificationForm.title = ''
  notificationForm.content = ''
  notificationForm.type = 'system'
  notificationForm.targetUserIds = []
  targetType.value = 'all'
  showDialog.value = true
}

// 显示编辑对话框
const showEditDialog = (notification) => {
  isEditing.value = true
  notificationForm.title = notification.title
  notificationForm.content = notification.content
  notificationForm.type = notification.type
  
  if (notification.targetType === 'specific_users' && notification.targetUsers) {
    try {
      const targetUsers = JSON.parse(notification.targetUsers)
      notificationForm.targetUserIds = Array.isArray(targetUsers) ? targetUsers : []
      targetType.value = 'specific'
    } catch (e) {
      console.error('解析目标用户错误:', e)
      notificationForm.targetUserIds = []
      targetType.value = 'all'
    }
  } else {
    notificationForm.targetUserIds = []
    targetType.value = 'all'
  }
  
  showDialog.value = true
}

// 取消对话框
const cancelDialog = () => {
  showDialog.value = false
}

// 添加目标用户
const addTargetUsers = () => {
  if (!userIdInput.value.trim()) return
  
  const userIds = userIdInput.value.split(',')
    .map(id => id.trim())
    .filter(id => /^\d+$/.test(id))
    .map(id => parseInt(id))
    .filter(id => !notificationForm.targetUserIds.includes(id))
  
  notificationForm.targetUserIds.push(...userIds)
  userIdInput.value = ''
}

// 移除目标用户
const removeTargetUser = (index) => {
  notificationForm.targetUserIds.splice(index, 1)
}

// 提交通知
const submitNotification = async () => {
  if (!notificationForm.title.trim()) {
    ElMessage.warning('请输入通知标题');
    return;
  }
  
  if (!notificationForm.content.trim()) {
    ElMessage.warning('请输入通知内容');
    return;
  }
  
  const data = {
    title: notificationForm.title,
    content: notificationForm.content,
    type: notificationForm.type
  };
  
  // 只有在选择特定用户时才添加targetUserIds
  if (targetType.value === 'specific' && notificationForm.targetUserIds.length > 0) {
    data.targetUserIds = notificationForm.targetUserIds;
  }
  
  try {
    isLoading.value = true;
    let response;
    
    if (isEditing.value) {
      // 获取当前编辑的通知ID
      const notificationId = notifications.value.find(n => 
        n.title === notificationForm.title && 
        n.type === notificationForm.type
      )?.id;
      
      if (!notificationId) {
        ElMessage.error('无法确定要编辑的通知ID');
        return;
      }
      
      response = await adminApi.updateSystemNotification(notificationId, data);
    } else {
      response = await adminApi.createSystemNotification(data);
    }
    
    if (response.code === 200) {
      ElMessage.success(isEditing.value ? '通知更新成功' : '通知创建成功');
      showDialog.value = false;
      fetchNotifications();
    } else {
      ElMessage.error(response.message || (isEditing.value ? '更新通知失败' : '创建通知失败'));
    }
  } catch (error) {
    console.error(isEditing.value ? '更新通知失败:' : '创建通知失败:', error);
    
    // 特别处理管理员未登录的情况
    if (error.message === '管理员未登录' || error.message === '无法获取管理员ID') {
      ElMessage.error('管理员会话已过期，请重新登录')
      router.push('/admin/login')
    } else {
      ElMessage.error(isEditing.value ? '更新通知失败，请稍后重试' : '创建通知失败，请稍后重试')
    }
  } finally {
    isLoading.value = false;
  }
};

// 发布通知
const publishNotification = async (notificationId) => {
  try {
    const result = await ElMessageBox.confirm('确定要发布此通知吗？发布后将无法修改。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    isLoading.value = true
    const response = await adminApi.publishSystemNotification(notificationId)
    
    if (response.code === 200) {
      ElMessage.success('通知已发布')
      fetchNotifications()
    } else {
      ElMessage.error(response.message || '发布通知失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('发布通知失败:', error)
      
      // 特别处理管理员未登录的情况
      if (error.message === '管理员未登录' || error.message === '无法获取管理员ID') {
        ElMessage.error('管理员会话已过期，请重新登录')
        router.push('/admin/login')
      } else {
        ElMessage.error('发布通知失败，请稍后重试')
      }
    }
  } finally {
    isLoading.value = false
  }
}

// 删除通知
const deleteNotification = async (notificationId) => {
  try {
    const result = await ElMessageBox.confirm('确定要删除此通知吗？此操作不可撤销。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    isLoading.value = true
    const response = await adminApi.deleteSystemNotification(notificationId)
    
    if (response.code === 200) {
      ElMessage.success('通知已删除')
      fetchNotifications()
    } else {
      ElMessage.error(response.message || '删除通知失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除通知失败:', error)
      
      // 特别处理管理员未登录的情况
      if (error.message === '管理员未登录' || error.message === '无法获取管理员ID') {
        ElMessage.error('管理员会话已过期，请重新登录')
        router.push('/admin/login')
      } else {
        ElMessage.error('删除通知失败，请稍后重试')
      }
    }
  } finally {
    isLoading.value = false
  }
}

// 查看通知详情
const viewNotificationDetails = async (notificationId) => {
  try {
    notificationDetail.value = null
    showDetail.value = true
    
    const response = await adminApi.getSystemNotificationDetail(notificationId)
    if (response.code === 200) {
      notificationDetail.value = response.data
    } else {
      ElMessage.error(response.message || '获取通知详情失败')
      closeDetailDialog()
    }
  } catch (error) {
    console.error('获取通知详情失败:', error)
    
    // 特别处理管理员未登录的情况
    if (error.message === '管理员未登录' || error.message === '无法获取管理员ID') {
      ElMessage.error('管理员会话已过期，请重新登录')
      router.push('/admin/login')
    } else {
      ElMessage.error('获取通知详情失败，请稍后重试')
    }
    
    closeDetailDialog()
  }
}

// 关闭详情对话框
const closeDetailDialog = () => {
  showDetail.value = false
  setTimeout(() => {
    notificationDetail.value = null
  }, 300)
}

// 在详情对话框中发布通知
const publishAndCloseDetail = async (notificationId) => {
  try {
    const result = await ElMessageBox.confirm('确定要发布此通知吗？发布后将无法修改。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    isLoading.value = true
    const response = await adminApi.publishSystemNotification(notificationId)
    
    if (response.code === 200) {
      ElMessage.success('通知已发布')
      closeDetailDialog()
      fetchNotifications()
    } else {
      ElMessage.error(response.message || '发布通知失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('发布通知失败:', error)
      
      // 特别处理管理员未登录的情况
      if (error.message === '管理员未登录' || error.message === '无法获取管理员ID') {
        ElMessage.error('管理员会话已过期，请重新登录')
        router.push('/admin/login')
      } else {
        ElMessage.error('发布通知失败，请稍后重试')
      }
    }
  } finally {
    isLoading.value = false
  }
}

// 获取通知列表
const fetchNotifications = async () => {
  isLoading.value = true
  
  try {
    const response = await adminApi.getSystemNotifications({
      page: currentPage.value,
      size: pageSize.value,
      type: typeFilter.value || undefined
    })
    
    if (response.code === 200 && response.data) {
      notifications.value = response.data.content || []
      totalElements.value = response.data.totalElements || 0
      totalPages.value = response.data.totalPages || 0
    } else {
      ElMessage.error(response.message || '获取通知列表失败')
      notifications.value = []
    }
  } catch (error) {
    console.error('获取通知列表失败:', error)
    
    // 特别处理管理员未登录的情况
    if (error.message === '管理员未登录' || error.message === '无法获取管理员ID') {
      ElMessage.error('管理员会话已过期，请重新登录')
      router.push('/admin/login')
    } else {
      ElMessage.error('获取通知列表失败，请稍后重试')
    }
    
    notifications.value = []
  } finally {
    isLoading.value = false
  }
}

// 组件挂载时获取通知数据
onMounted(() => {
  fetchNotifications()
})
</script>

<style scoped>
.notification-management {
  padding: 0 10px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a202c;
}

.header-actions {
  display: flex;
  gap: 16px;
}

.search-wrapper select {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  background-color: white;
  min-width: 150px;
}

.search-wrapper select:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 2px rgba(49, 130, 206, 0.2);
}

.create-btn {
  padding: 8px 16px;
  background-color: #4299e1;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.create-btn:hover {
  background-color: #3182ce;
}

.notification-table-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin-bottom: 24px;
}

.notification-table {
  width: 100%;
  border-collapse: collapse;
}

.notification-table th,
.notification-table td {
  padding: 12px 16px;
  text-align: left;
  font-size: 14px;
}

.notification-table th {
  background-color: #f7fafc;
  color: #4a5568;
  font-weight: 500;
  border-bottom: 1px solid #e2e8f0;
}

.notification-table td {
  border-bottom: 1px solid #f7fafc;
}

.notification-table tr:hover td {
  background-color: #f7fafc;
}

.notification-title {
  font-weight: 500;
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.type-badge.system {
  background-color: #bee3f8;
  color: #2b6cb0;
}

.type-badge.announcement {
  background-color: #c6f6d5;
  color: #2f855a;
}

.type-badge.maintenance {
  background-color: #fed7d7;
  color: #c53030;
}

.type-badge.update {
  background-color: #feebc8;
  color: #c05621;
}

.status-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.published {
  background-color: #c6f6d5;
  color: #2f855a;
}

.status-badge.draft {
  background-color: #e2e8f0;
  color: #4a5568;
}

.publish-date {
  font-size: 12px;
  color: #718096;
  margin-left: 6px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  border: none;
  background-color: #f7fafc;
  color: #4a5568;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.action-btn:hover {
  background-color: #e2e8f0;
}

.action-btn.view:hover {
  background-color: #4299e1;
  color: white;
}

.action-btn.edit:hover {
  background-color: #f6ad55;
  color: white;
}

.action-btn.publish:hover {
  background-color: #48bb78;
  color: white;
}

.action-btn.delete:hover {
  background-color: #e53e3e;
  color: white;
}

.empty-table {
  text-align: center;
  color: #a0aec0;
  padding: 40px;
}

.empty-table.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e2e8f0;
  border-top-color: #4299e1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background-color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background-color: #f7fafc;
  border-color: #cbd5e0;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #4a5568;
}

/* 模态对话框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: 8px;
  padding: 24px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 20px;
  color: #2d3748;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #4a5568;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 2px rgba(49, 130, 206, 0.2);
}

.radio-group {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.radio-label {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.radio-label input {
  margin-right: 8px;
  width: auto;
}

.target-users {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px;
  margin-top: 8px;
}

.user-input {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.user-input input {
  flex: 1;
}

.add-btn {
  padding: 8px 12px;
  background-color: #4299e1;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.add-btn:hover {
  background-color: #3182ce;
}

.user-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.user-tag {
  background-color: #edf2f7;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 12px;
  display: flex;
  align-items: center;
}

.remove-tag {
  margin-left: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
  color: #718096;
}

.remove-tag:hover {
  color: #e53e3e;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn.cancel {
  background-color: #e2e8f0;
  color: #4a5568;
}

.btn.cancel:hover {
  background-color: #cbd5e0;
}

.btn.confirm {
  background-color: #4299e1;
  color: white;
}

.btn.confirm:hover {
  background-color: #3182ce;
}

.btn.publish {
  background-color: #48bb78;
  color: white;
}

.btn.publish:hover {
  background-color: #38a169;
}

/* 详情对话框样式 */
.detail-modal {
  max-width: 600px;
}

.notification-detail {
  padding: 16px 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.detail-header h3 {
  margin: 0;
  font-size: 18px;
  color: #2d3748;
}

.detail-info {
  background-color: #f7fafc;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  margin-bottom: 8px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  width: 80px;
  color: #718096;
  font-size: 14px;
}

.status-text {
  font-weight: 500;
}

.status-text.published {
  color: #2f855a;
}

.status-text.draft {
  color: #4a5568;
}

.detail-content {
  margin-top: 16px;
}

.detail-content h4 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #4a5568;
}

.content-text {
  background-color: #f7fafc;
  border-radius: 6px;
  padding: 12px;
  font-size: 14px;
  white-space: pre-wrap;
}

.detail-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
  color: #a0aec0;
}
</style> 