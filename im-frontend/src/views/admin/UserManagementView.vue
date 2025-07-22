<template>
  <div class="user-management">
    <div class="page-header">
      <h1>用户管理</h1>
      <div class="header-actions">
        <div class="search-wrapper">
          <input 
            type="text" 
            v-model="searchQuery" 
            placeholder="搜索用户ID、邮箱或昵称..." 
            @keyup.enter="handleSearch"
          />
          <i class="fas fa-search search-icon"></i>
        </div>
        <div class="filter-wrapper">
          <select v-model="statusFilter" @change="applyFilters">
            <option value="all">所有状态</option>
            <option value="active">正常</option>
            <option value="banned">已封禁</option>
          </select>
        </div>
      </div>
    </div>
    
    <div class="user-table-container">
      <table class="user-table">
        <thead>
          <tr>
            <th>
              <input type="checkbox" v-model="selectAll" @change="toggleSelectAll" />
            </th>
            <th>ID</th>
            <th>用户名</th>
            <th>邮箱</th>
            <th>注册时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in displayedUsers" :key="user.userId" :class="{ 'banned': user.status === 'banned' }">
            <td>
              <input type="checkbox" v-model="selectedUsers" :value="user.userId" />
            </td>
            <td>{{ user.userId }}</td>
            <td class="user-info">
              <div class="user-avatar">
                <img :src="user.avatarUrl || '/images/default-avatar.png'" alt="用户头像" />
              </div>
              <div>{{ user.username || '未设置' }}</div>
            </td>
            <td>{{ user.email }}</td>
            <td>{{ formatDate(user.registeredAt) }}</td>
            <td>
              <span :class="['status-badge', user.status]">
                {{ getStatusText(user.status) }}
              </span>
              <span v-if="user.status === 'banned' && user.freezeEndDate" class="end-date">
                (至 {{ formatDate(user.freezeEndDate) }})
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <button class="action-btn view" @click="viewUserDetails(user.userId)" title="查看详情">
                  <i class="fas fa-eye"></i>
                </button>
                <button 
                  v-if="user.status === 'active'" 
                  class="action-btn ban" 
                  @click="showBanDialog(user)" 
                  title="封禁账户"
                >
                  <i class="fas fa-ban"></i>
                </button>
                <button 
                  v-if="user.status === 'banned'" 
                  class="action-btn unban" 
                  @click="unbanUser(user.userId)" 
                  title="解除封禁"
                >
                  <i class="fas fa-unlock"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="isLoading">
            <td colspan="8" class="empty-table loading">
              <div class="loading-spinner"></div> 加载中...
            </td>
          </tr>
          <tr v-else-if="displayedUsers.length === 0">
            <td colspan="8" class="empty-table">
              没有找到匹配的用户
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div class="table-footer">
      <div class="bulk-actions" v-if="selectedUsers.length > 0">
        <span>已选择 {{ selectedUsers.length }} 个用户</span>
        <button class="bulk-action-btn danger" @click="showBulkBanDialog">批量封禁</button>
      </div>
      
      <div class="pagination">
        <button 
          :disabled="currentPage === 1" 
          @click="changePage(currentPage - 1)" 
          class="page-btn"
        >
          <i class="fas fa-chevron-left"></i>
        </button>
        
        <span class="page-info">{{ currentPage }} / {{ totalPages || 1 }}</span>
        
        <button 
          :disabled="currentPage === totalPages || totalPages === 0" 
          @click="changePage(currentPage + 1)" 
          class="page-btn"
        >
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>
    </div>
    
    <!-- 封禁用户对话框 -->
    <div v-if="showBan" class="modal-overlay" @click="cancelBan">
      <div class="modal-content" @click.stop>
        <h2>封禁用户账户</h2>
        <p>您正在封禁用户 <strong>{{ targetUser?.username || targetUser?.email }}</strong> 的账户</p>
        <p class="warning-text">封禁后，该用户将无法登录系统，且所有内容将被隐藏</p>
        
        <div class="form-group">
          <label for="banReason">封禁原因</label>
          <textarea 
            id="banReason" 
            v-model="banReason" 
            placeholder="请输入封禁原因..."
            rows="3"
          ></textarea>
        </div>
        
        <div class="form-group">
          <label for="banDuration">封禁时长</label>
          <select id="banDuration" v-model="banDuration">
            <option value="1">1天 (24小时)</option>
            <option value="3">3天 (72小时)</option>
            <option value="7">7天 (1周)</option>
            <option value="30">30天 (1个月)</option>
            <option value="permanent">永久封禁</option>
          </select>
        </div>
        
        <div class="modal-actions">
          <button class="btn cancel" @click="cancelBan">取消</button>
          <button class="btn confirm danger" @click="confirmBan">确认封禁</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import adminApi from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const searchQuery = ref('')
const statusFilter = ref('all')
const currentPage = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)

// 用户数据
const users = ref([])

// 选择相关
const selectedUsers = ref([])
const selectAll = ref(false)

// 对话框相关
const showBan = ref(false)
const targetUser = ref(null)
const banReason = ref('')
const banDuration = ref('7')

// 当前页显示的用户
const displayedUsers = computed(() => {
  return users.value;
})

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '未知';
  
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '无效日期';
    
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
    }).format(date);
  } catch (e) {
    console.error('日期格式化错误:', e);
    return '无效日期';
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'active': return '正常'
    case 'banned': return '已封禁'
    default: return '未知'
  }
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 0;
  fetchUsers();
}

// 应用过滤器
const applyFilters = () => {
  currentPage.value = 0;
  fetchUsers();
}

// 切换全选
const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedUsers.value = displayedUsers.value.map(user => user.userId)
  } else {
    selectedUsers.value = []
  }
}

// 翻页
const changePage = (page) => {
  if (page >= 0 && (page < totalPages.value || totalPages.value === 0)) {
    currentPage.value = page;
    fetchUsers();
  }
}

// 查看用户详情
const viewUserDetails = (userId) => {
  router.push(`/admin/users/${userId}`)
}

// 显示封禁对话框
const showBanDialog = (user) => {
  targetUser.value = user;
  showBan.value = true;
  banReason.value = '';
  banDuration.value = '7';
}

// 取消封禁
const cancelBan = () => {
  showBan.value = false;
  targetUser.value = null;
  banReason.value = '';
}

// 确认封禁
const confirmBan = async () => {
  if (!banReason.value.trim()) {
    ElMessage.warning('请输入封禁原因');
    return;
  }
  
  try {
    isLoading.value = true;
    
    let duration = null;
    if (banDuration.value !== 'permanent') {
      duration = parseInt(banDuration.value) * 24; // 转换为小时
    }
    
    const response = await adminApi.manageUser(
      targetUser.value.userId, 
      'ban', 
      banReason.value, 
      duration
    );
    
    if (response.code === 200) {
      ElMessage.success('用户已被封禁');
      fetchUsers();
      showBan.value = false;
      targetUser.value = null;
    } else {
      ElMessage.error(response.message || '封禁用户失败');
    }
  } catch (error) {
    console.error('封禁用户失败:', error);
    ElMessage.error('封禁用户失败，请稍后重试');
  } finally {
    isLoading.value = false;
  }
}

// 解除封禁
const unbanUser = async (userId) => {
  try {
    const result = await ElMessageBox.confirm('确定要解除此用户的封禁吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    isLoading.value = true;
    
    const response = await adminApi.manageUser(
      userId,
      'unban',
      '管理员手动解除封禁'
    );
    
    if (response.code === 200) {
      ElMessage.success('用户已解除封禁');
      fetchUsers();
    } else {
      ElMessage.error(response.message || '解除封禁失败');
  }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('解除封禁失败:', error);
      ElMessage.error('解除封禁失败，请稍后重试');
    }
  } finally {
    isLoading.value = false;
  }
}

// 显示批量封禁对话框
const showBulkBanDialog = () => {
  ElMessage.info(`功能开发中：批量封禁 ${selectedUsers.value.length} 个用户`);
}

// 获取用户数据
const fetchUsers = async () => {
  isLoading.value = true;
  selectedUsers.value = [];
  selectAll.value = false;
  
  try {
    const response = await adminApi.getUserList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value || undefined,
      status: statusFilter.value === 'all' ? undefined : statusFilter.value
    });
    
    if (response.code === 200 && response.data) {
      users.value = response.data.content || [];
      totalElements.value = response.data.totalElements || 0;
      totalPages.value = response.data.totalPages || 0;
      
      // 打印调试信息
      console.log('用户数据:', users.value);
      console.log('分页信息:', {
        currentPage: currentPage.value,
        totalPages: totalPages.value,
        totalElements: totalElements.value
      });
    } else {
      ElMessage.error(response.message || '获取用户列表失败');
      users.value = [];
    }
  } catch (error) {
    console.error('获取用户列表失败:', error);
    ElMessage.error('获取用户列表失败，请稍后重试');
    users.value = [];
  } finally {
    isLoading.value = false;
  }
}

// 组件挂载时获取用户数据
onMounted(() => {
  fetchUsers();
})
</script>

<style scoped>
.user-management {
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

.search-wrapper {
  position: relative;
}

.search-wrapper input {
  width: 300px;
  padding: 8px 12px 8px 36px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
}

.search-wrapper input:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 2px rgba(49, 130, 206, 0.2);
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #a0aec0;
}

.filter-wrapper select {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  background-color: white;
}

.filter-wrapper select:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 2px rgba(49, 130, 206, 0.2);
}

.user-table-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin-bottom: 24px;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th,
.user-table td {
  padding: 12px 16px;
  text-align: left;
  font-size: 14px;
}

.user-table th {
  background-color: #f7fafc;
  color: #4a5568;
  font-weight: 500;
  border-bottom: 1px solid #e2e8f0;
}

.user-table td {
  border-bottom: 1px solid #f7fafc;
}

.user-table tr:hover td {
  background-color: #f7fafc;
}

.user-table tr.banned td {
  background-color: #fff5f5;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
  background-color: #e2e8f0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active {
  background-color: #c6f6d5;
  color: #2f855a;
}

.status-badge.banned {
  background-color: #fed7d7;
  color: #c53030;
}

.end-date {
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

.action-btn.ban:hover {
  background-color: #e53e3e;
  color: white;
}

.action-btn.unban:hover {
  background-color: #48bb78;
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
  justify-content: space-between;
  align-items: center;
}

.bulk-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bulk-actions span {
  font-size: 14px;
  color: #4a5568;
}

.bulk-action-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background-color: #4299e1;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.bulk-action-btn:hover {
  background-color: #3182ce;
}

.bulk-action-btn.danger {
  background-color: #f56565;
}

.bulk-action-btn.danger:hover {
  background-color: #e53e3e;
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
}

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 20px;
  color: #2d3748;
}

.warning-text {
  color: #e53e3e;
  font-size: 14px;
  margin-bottom: 16px;
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

.form-group textarea,
.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
}

.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 2px rgba(49, 130, 206, 0.2);
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

.btn.confirm.danger {
  background-color: #f56565;
}

.btn.confirm.danger:hover {
  background-color: #e53e3e;
}
</style> 