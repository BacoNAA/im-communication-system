<template>
  <div class="group-management">
    <div class="page-header">
      <h1>群组管理</h1>
      <div class="header-actions">
        <div class="search-wrapper">
          <input 
            type="text" 
            v-model="searchQuery" 
            placeholder="搜索群组ID或名称..." 
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
    
    <div class="group-table-container">
      <table class="group-table">
        <thead>
          <tr>
            <th>
              <input type="checkbox" v-model="selectAll" @change="toggleSelectAll" />
            </th>
            <th>ID</th>
            <th>群组名称</th>
            <th>群主</th>
            <th>成员数</th>
            <th>创建时间</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="group in displayedGroups" :key="group.groupId" :class="{ 'banned': group.status === 'banned' }">
            <td>
              <input type="checkbox" v-model="selectedGroups" :value="group.groupId" />
            </td>
            <td>{{ group.groupId }}</td>
            <td class="group-info">
              <div class="group-avatar">
                <img v-if="group.avatarUrl" :src="group.avatarUrl" alt="群组头像" />
                <div v-else class="avatar-initials">
                  {{ getInitials(group.name) }}
                </div>
              </div>
              <div>{{ group.name }}</div>
            </td>
            <td>{{ group.ownerUsername }}</td>
            <td>{{ group.memberCount }}</td>
            <td>{{ formatDate(group.createdAt) }}</td>
            <td>
              <span :class="['status-badge', group.status]">
                {{ getStatusText(group.status) }}
              </span>
              <span v-if="group.status === 'banned' && group.bannedUntil" class="end-date">
                (至 {{ formatDate(group.bannedUntil) }})
              </span>
            </td>
            <td>
              <div class="action-buttons">
                <button class="action-btn view" @click="viewGroupDetails(group.groupId)" title="查看详情">
                  <i class="fas fa-eye"></i>
                </button>
                <button 
                  v-if="group.status === 'active'" 
                  class="action-btn ban" 
                  @click="showBanDialog(group)" 
                  title="封禁群组"
                >
                  <i class="fas fa-ban"></i>
                </button>
                <button 
                  v-if="group.status === 'banned'" 
                  class="action-btn unban" 
                  @click="unbanGroup(group.groupId)" 
                  title="解除封禁"
                >
                  <i class="fas fa-unlock"></i>
                </button>
                <button 
                  class="action-btn delete" 
                  @click="showDissolveDialog(group)" 
                  title="解散群组"
                >
                  <i class="fas fa-trash-alt"></i>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="isLoading">
            <td colspan="8" class="empty-table loading">
              <div class="loading-spinner"></div> 加载中...
            </td>
          </tr>
          <tr v-else-if="displayedGroups.length === 0">
            <td colspan="8" class="empty-table">
              没有找到匹配的群组
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div class="table-footer">
      <div class="bulk-actions" v-if="selectedGroups.length > 0">
        <span>已选择 {{ selectedGroups.length }} 个群组</span>
        <button class="bulk-action-btn danger" @click="showBulkBanDialog">批量封禁</button>
      </div>
      
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
    
    <!-- 封禁群组对话框 -->
    <div v-if="showBan" class="modal-overlay" @click="cancelBan">
      <div class="modal-content" @click.stop>
        <h2>封禁群组</h2>
        <p>您正在封禁群组 <strong>{{ targetGroup?.name }}</strong></p>
        <p class="warning-text">封禁后，群组成员将无法在此群组中发送消息</p>
        
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

    <!-- 解散群组对话框 -->
    <div v-if="showDissolve" class="modal-overlay" @click="cancelDissolve">
      <div class="modal-content" @click.stop>
        <h2>解散群组</h2>
        <p>您正在解散群组 <strong>{{ targetGroup?.name }}</strong></p>
        <p class="warning-text">解散后，群组将被永久删除，所有成员将被移除，聊天记录将无法恢复！</p>
        
        <div class="form-group">
          <label for="dissolveReason">解散原因</label>
          <textarea 
            id="dissolveReason" 
            v-model="dissolveReason" 
            placeholder="请输入解散原因..."
            rows="3"
          ></textarea>
        </div>
        
        <div class="form-group">
          <label for="confirmDissolve">确认解散</label>
          <input 
            type="text" 
            id="confirmDissolve" 
            v-model="dissolveConfirm" 
            placeholder="请输入'解散'"
          />
        </div>
        
        <div class="modal-actions">
          <button class="btn cancel" @click="cancelDissolve">取消</button>
          <button 
            class="btn confirm danger" 
            @click="confirmDissolve" 
            :disabled="dissolveConfirm !== '解散'"
          >
            确认解散
          </button>
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

// 群组数据
const groups = ref([])

// 选择相关
const selectedGroups = ref([])
const selectAll = ref(false)

// 封禁对话框相关
const showBan = ref(false)
const targetGroup = ref(null)
const banReason = ref('')
const banDuration = ref('7')

// 解散对话框相关
const showDissolve = ref(false)
const dissolveReason = ref('')
const dissolveConfirm = ref('')

// 当前页显示的群组
const displayedGroups = computed(() => {
  return groups.value;
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

// 获取群组名称首字母
const getInitials = (name) => {
  if (!name) return '群'
  return name.charAt(0).toUpperCase()
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 0;
  fetchGroups();
}

// 应用过滤器
const applyFilters = () => {
  currentPage.value = 0;
  fetchGroups();
}

// 切换全选
const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedGroups.value = displayedGroups.value.map(group => group.groupId)
  } else {
    selectedGroups.value = []
  }
}

// 翻页
const changePage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page;
    fetchGroups();
  }
}

// 查看群组详情
const viewGroupDetails = (groupId) => {
  router.push(`/admin/groups/${groupId}`)
}

// 显示封禁对话框
const showBanDialog = (group) => {
  targetGroup.value = group;
  showBan.value = true;
  banReason.value = '';
  banDuration.value = '7';
}

// 取消封禁
const cancelBan = () => {
  showBan.value = false;
  targetGroup.value = null;
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
    
    const response = await adminApi.manageGroup(
      targetGroup.value.groupId, 
      'ban', 
      banReason.value, 
      duration
    );
    
    if (response.success) {
      ElMessage.success('群组已被封禁');
      fetchGroups();
      showBan.value = false;
      targetGroup.value = null;
    } else {
      ElMessage.error(response.message || '封禁群组失败');
    }
  } catch (error) {
    console.error('封禁群组失败:', error);
    ElMessage.error('封禁群组失败，请稍后重试');
  } finally {
    isLoading.value = false;
  }
}

// 解除封禁
const unbanGroup = async (groupId) => {
  try {
    const result = await ElMessageBox.confirm('确定要解除此群组的封禁吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    isLoading.value = true;
    
    const response = await adminApi.manageGroup(
      groupId,
      'unban',
      '管理员手动解除封禁'
    );
    
    if (response.success) {
      ElMessage.success('群组已解除封禁');
      fetchGroups();
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

// 显示解散对话框
const showDissolveDialog = (group) => {
  targetGroup.value = group;
  showDissolve.value = true;
  dissolveReason.value = '';
  dissolveConfirm.value = '';
}

// 取消解散
const cancelDissolve = () => {
  showDissolve.value = false;
  targetGroup.value = null;
  dissolveReason.value = '';
  dissolveConfirm.value = '';
}

// 确认解散
const confirmDissolve = async () => {
  if (!dissolveReason.value.trim()) {
    ElMessage.warning('请输入解散原因');
    return;
  }
  
  if (dissolveConfirm.value !== '解散') {
    ElMessage.warning('请正确输入确认文字');
    return;
  }
  
  try {
    isLoading.value = true;
    
    const response = await adminApi.manageGroup(
      targetGroup.value.groupId,
      'dissolve',
      dissolveReason.value
    );
    
    if (response.success) {
      ElMessage.success('群组已解散');
      fetchGroups();
      showDissolve.value = false;
      targetGroup.value = null;
    } else {
      ElMessage.error(response.message || '解散群组失败');
    }
  } catch (error) {
    console.error('解散群组失败:', error);
    ElMessage.error('解散群组失败，请稍后重试');
  } finally {
    isLoading.value = false;
  }
}

// 显示批量封禁对话框
const showBulkBanDialog = () => {
  ElMessage.info(`功能开发中：批量封禁 ${selectedGroups.value.length} 个群组`);
}

// 获取群组数据
const fetchGroups = async () => {
  isLoading.value = true;
  selectedGroups.value = [];
  selectAll.value = false;
  
  try {
    const response = await adminApi.getGroupList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value || undefined,
      status: statusFilter.value === 'all' ? undefined : statusFilter.value
    });
    
    if (response.success && response.data) {
      groups.value = response.data.content || [];
      totalElements.value = response.data.totalElements || 0;
      totalPages.value = response.data.totalPages || 0;
      
      // 打印调试信息
      console.log('群组数据:', groups.value);
      console.log('分页信息:', {
        currentPage: currentPage.value,
        totalPages: totalPages.value,
        totalElements: totalElements.value
      });
    } else {
      ElMessage.error(response.message || '获取群组列表失败');
      groups.value = [];
    }
  } catch (error) {
    console.error('获取群组列表失败:', error);
    ElMessage.error('获取群组列表失败，请稍后重试');
    groups.value = [];
  } finally {
    isLoading.value = false;
  }
}

// 组件挂载时获取群组数据
onMounted(() => {
  fetchGroups();
})
</script>

<style scoped>
.group-management {
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

.group-table-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin-bottom: 24px;
}

.group-table {
  width: 100%;
  border-collapse: collapse;
}

.group-table th,
.group-table td {
  padding: 12px 16px;
  text-align: left;
  font-size: 14px;
}

.group-table th {
  background-color: #f7fafc;
  color: #4a5568;
  font-weight: 500;
  border-bottom: 1px solid #e2e8f0;
}

.group-table td {
  border-bottom: 1px solid #f7fafc;
}

.group-table tr:hover td {
  background-color: #f7fafc;
}

.group-table tr.banned td {
  background-color: #fff5f5;
}

.group-info {
  display: flex;
  align-items: center;
}

.group-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
  background-color: #e2e8f0;
}

.group-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-initials {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #4299e1;
  color: white;
  font-weight: 600;
  font-size: 14px;
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
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  color: white;
}

.action-btn.view {
  background-color: #4299e1;
}

.action-btn.ban {
  background-color: #e53e3e;
}

.action-btn.unban {
  background-color: #48bb78;
}

.action-btn.delete {
  background-color: #f56565;
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
.form-group select,
.form-group input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
}

.form-group textarea:focus,
.form-group select:focus,
.form-group input:focus {
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

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn.cancel {
  background-color: #e2e8f0;
  color: #4a5568;
}

.btn.cancel:hover:not(:disabled) {
  background-color: #cbd5e0;
}

.btn.confirm {
  background-color: #4299e1;
  color: white;
}

.btn.confirm:hover:not(:disabled) {
  background-color: #3182ce;
}

.btn.confirm.danger {
  background-color: #f56565;
}

.btn.confirm.danger:hover:not(:disabled) {
  background-color: #e53e3e;
}
</style>