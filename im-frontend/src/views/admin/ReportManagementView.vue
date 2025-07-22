<template>
  <div class="report-management">
    <div class="page-header">
      <h1>举报管理</h1>
      <div class="filter-controls">
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable>
          <el-option label="全部" value=""></el-option>
          <el-option label="待处理" value="pending"></el-option>
          <el-option label="处理中" value="processing"></el-option>
          <el-option label="已解决" value="resolved"></el-option>
          <el-option label="已拒绝" value="rejected"></el-option>
        </el-select>
        
        <el-select v-model="contentTypeFilter" placeholder="内容类型" clearable>
          <el-option label="全部" value=""></el-option>
          <el-option label="用户" value="USER"></el-option>
          <el-option label="消息" value="MESSAGE"></el-option>
          <el-option label="群组" value="GROUP"></el-option>
          <el-option label="群组成员" value="GROUP_MEMBER"></el-option>
        </el-select>
      </div>
    </div>
    
    <div class="report-stats">
      <el-card class="stat-card" shadow="hover" v-for="(count, status) in reportStats.statusCount" :key="status">
        <div class="stat-content">
          <div class="stat-value">{{ count }}</div>
          <div class="stat-label">{{ formatStatusLabel(status) }}</div>
        </div>
      </el-card>
    </div>
    
    <el-table
      v-loading="loading"
      :data="reports"
      style="width: 100%"
      border
    >
      <el-table-column prop="reportId" label="ID" width="80" />
      <el-table-column label="举报者" width="150">
        <template #default="scope">
          <div class="user-info">
            <span>{{ scope.row.reporterUsername }}</span>
            <span class="user-id">#{{ scope.row.reporterId }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="被举报内容" min-width="200">
        <template #default="scope">
          <div class="reported-content">
            <el-tag size="small">{{ formatContentType(scope.row.reportedContentType) }}</el-tag>
            <span class="content-id">ID: {{ scope.row.reportedContentId }}</span>
            <div v-if="scope.row.reportedUserId" class="reported-user">
              <span>用户: {{ scope.row.reportedUsername || '未知用户' }}</span>
              <span class="user-id">#{{ scope.row.reportedUserId }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="举报原因" width="150">
        <template #default="scope">
          {{ formatReasonType(scope.row.reason) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ formatStatus(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="举报时间" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button 
            size="small" 
            type="primary" 
            @click="viewReportDetails(scope.row)"
          >
            查看详情
          </el-button>
          <el-button 
            v-if="scope.row.status === 'pending' || scope.row.status === 'processing'"
            size="small" 
            :type="scope.row.status === 'pending' ? 'success' : 'primary'" 
            @click="handleReport(scope.row)"
          >
            {{ scope.row.status === 'pending' ? '处理' : '继续处理' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-container">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </div>
    
    <!-- 举报详情对话框 -->
    <el-dialog
      v-model="showReportDetails"
      title="举报详情"
      width="600px"
      destroy-on-close
    >
      <div v-if="currentReport" class="report-details">
        <div class="detail-section">
          <h3>基本信息</h3>
          <div class="detail-item">
            <span class="label">举报ID:</span>
            <span class="value">{{ currentReport.reportId }}</span>
          </div>
          <div class="detail-item">
            <span class="label">举报时间:</span>
            <span class="value">{{ formatDate(currentReport.createdAt) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">状态:</span>
            <span class="value">
              <el-tag :type="getStatusType(currentReport.status)">
                {{ formatStatus(currentReport.status) }}
              </el-tag>
            </span>
          </div>
          <div v-if="currentReport.handledAt" class="detail-item">
            <span class="label">处理时间:</span>
            <span class="value">{{ formatDate(currentReport.handledAt) }}</span>
          </div>
        </div>
        
        <div class="detail-section">
          <h3>举报者信息</h3>
          <div class="detail-item">
            <span class="label">用户名:</span>
            <span class="value">{{ currentReport.reporterUsername }}</span>
          </div>
          <div class="detail-item">
            <span class="label">用户ID:</span>
            <span class="value">{{ currentReport.reporterId }}</span>
          </div>
        </div>
        
        <div class="detail-section">
          <h3>被举报内容</h3>
          <div class="detail-item">
            <span class="label">内容类型:</span>
            <span class="value">
              <el-tag size="small">{{ formatContentType(currentReport.reportedContentType) }}</el-tag>
            </span>
          </div>
          <div class="detail-item">
            <span class="label">内容ID:</span>
            <span class="value">{{ currentReport.reportedContentId }}</span>
          </div>
          <div v-if="currentReport.reportedUserId" class="detail-item">
            <span class="label">被举报用户:</span>
            <span class="value">
              {{ currentReport.reportedUsername || '未知用户' }}
              <span class="user-id">#{{ currentReport.reportedUserId }}</span>
            </span>
          </div>
          
          <!-- 添加被举报内容查看组件 -->
          <div class="reported-content-section">
            <h4>内容详情</h4>
            <ReportedContentViewer 
              :content-type="currentReport.reportedContentType" 
              :content-id="currentReport.reportedContentId"
            />
          </div>
        </div>
        
        <div class="detail-section">
          <h3>举报内容</h3>
          <div class="detail-item">
            <span class="label">举报原因:</span>
            <span class="value">{{ formatReasonType(currentReport.reason) }}</span>
          </div>
          <div class="detail-item description">
            <span class="label">详细描述:</span>
            <div class="value description-text">{{ currentReport.description || '无详细描述' }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showReportDetails = false">关闭</el-button>
          <el-button 
            v-if="currentReport && (currentReport.status === 'pending' || currentReport.status === 'processing')"
            type="primary" 
            @click="handleReport(currentReport)"
          >
            {{ currentReport.status === 'pending' ? '处理举报' : '继续处理' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 处理举报对话框 -->
    <el-dialog
      v-model="showHandleReportDialog"
      :title="currentReport && currentReport.status === 'processing' ? '继续处理举报' : '处理举报'"
      width="500px"
      destroy-on-close
    >
      <div v-if="currentReport" class="handle-report-form">
        <!-- 添加处理流程提示 -->
        <div class="process-tip">
          <el-alert
            :title="currentReport.status === 'pending' ? '处理流程: 待处理 → 处理中 → 已解决/已拒绝' : '处理流程: 处理中 → 已解决/已拒绝'"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <p class="tip-content">
                {{ currentReport.status === 'pending' 
                   ? '您可以将此举报标记为"处理中"进行初步处理，或直接标记为"已解决/已拒绝"完成处理。' 
                   : '此举报当前状态为"处理中"，您可以将其标记为"已解决"或"已拒绝"完成处理。' }}
              </p>
            </template>
          </el-alert>
        </div>
        <el-form :model="handleForm" label-width="100px">
          <el-form-item label="处理方式" required>
            <el-radio-group v-model="handleForm.action">
              <el-radio 
                label="process" 
                :disabled="currentReport && currentReport.status === 'processing'"
              >标记处理中</el-radio>
              <el-radio label="resolve">解决举报</el-radio>
              <el-radio label="reject">拒绝举报</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="处理结果" required>
            <el-select 
              v-model="handleForm.result" 
              placeholder="请选择处理结果"
            >
              <el-option 
                v-for="option in resultOptions" 
                :key="option.value" 
                :label="option.label" 
                :value="option.value"
              ></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="处理备注">
            <el-input 
              v-model="handleForm.note" 
              placeholder="可选，添加处理备注"
              type="textarea"
              :rows="2"
            ></el-input>
          </el-form-item>
          
          <!-- 根据内容类型显示不同的操作选项 -->
          <template v-if="['MESSAGE', 'MOMENT'].includes(currentReport.reportedContentType)">
            <el-form-item label="对用户操作">
              <el-select v-model="handleForm.userAction" placeholder="选择对用户的操作">
                <el-option label="无操作" value="none"></el-option>
                <el-option label="临时封禁" value="temporary_ban"></el-option>
                <el-option label="永久封禁" value="permanent_ban"></el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item v-if="handleForm.userAction === 'temporary_ban'" label="封禁时长(小时)">
              <el-input-number v-model="handleForm.banDuration" :min="1" :max="720" />
            </el-form-item>
            
            <el-form-item label="对内容操作">
              <el-select v-model="handleForm.contentAction" placeholder="选择对内容的操作">
                <el-option label="无操作" value="none"></el-option>
                <el-option label="删除内容" value="delete"></el-option>
              </el-select>
            </el-form-item>
          </template>
          
          <template v-else-if="currentReport.reportedContentType === 'USER'">
            <el-form-item label="对用户操作">
              <el-select v-model="handleForm.userAction" placeholder="选择对用户的操作">
                <el-option label="无操作" value="none"></el-option>
                <el-option label="临时封禁" value="temporary_ban"></el-option>
                <el-option label="永久封禁" value="permanent_ban"></el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item v-if="handleForm.userAction === 'temporary_ban'" label="封禁时长(小时)">
              <el-input-number v-model="handleForm.banDuration" :min="1" :max="720" />
            </el-form-item>
          </template>
          
          <template v-else-if="currentReport.reportedContentType === 'GROUP'">
            <el-form-item label="对群组操作">
              <el-select v-model="handleForm.contentAction" placeholder="选择对群组的操作">
                <el-option label="无操作" value="none"></el-option>
                <el-option label="临时封禁" value="temporary_ban"></el-option>
                <el-option label="永久封禁" value="permanent_ban"></el-option>
                <el-option label="解散群组" value="dissolve"></el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item v-if="handleForm.contentAction === 'temporary_ban'" label="封禁时长(小时)">
              <el-input-number v-model="handleForm.banDuration" :min="1" :max="720" />
            </el-form-item>
          </template>
          
          <template v-else-if="currentReport.reportedContentType === 'GROUP_MEMBER'">
            <el-form-item label="对成员操作">
              <el-select v-model="handleForm.userAction" placeholder="选择对成员的操作">
                <el-option label="无操作" value="none"></el-option>
                <el-option label="移出群组" value="remove_member"></el-option>
                <el-option label="封禁用户" value="ban_user"></el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item v-if="handleForm.userAction === 'ban_user'" label="封禁时长(小时)">
              <el-input-number v-model="handleForm.banDuration" :min="1" :max="720" />
            </el-form-item>
            
            <el-form-item label="对群组操作">
              <el-select v-model="handleForm.contentAction" placeholder="选择对群组的操作">
                <el-option label="无操作" value="none"></el-option>
                <el-option label="封禁群组" value="ban_group"></el-option>
              </el-select>
            </el-form-item>
          </template>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showHandleReportDialog = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="submitHandleReport"
            :loading="submitting"
            :disabled="!handleForm.action || !handleForm.result"
          >
            提交
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { reportApi } from '@/api/report';
import ReportedContentViewer from '@/components/admin/ReportedContentViewer.vue';

// 分页参数
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 筛选参数
const statusFilter = ref('');
const contentTypeFilter = ref('');

// 表格数据
const reports = ref([]);
const loading = ref(false);
const reportStats = ref({
  statusCount: {
    pending: 0,
    processing: 0,
    resolved: 0,
    rejected: 0
  },
  contentTypeCount: {}
});

// 详情对话框
const showReportDetails = ref(false);
const currentReport = ref(null);

// 处理举报对话框
const showHandleReportDialog = ref(false);
const submitting = ref(false);
const handleForm = ref({
  action: 'process',
  result: '正在进行处理',
  note: '',
  userAction: 'none',
  contentAction: 'none',
  banDuration: 1 // 新增封禁时长
});

// 监听action变化，更新默认result
watch(() => handleForm.value.action, (newAction) => {
  // 根据新的处理方式设置对应的处理结果
  if (newAction === 'process') {
    handleForm.value.result = '正在进行处理';
  } else if (newAction === 'resolve') {
    handleForm.value.result = '举报内容违规，已处理';
  } else if (newAction === 'reject') {
    handleForm.value.result = '举报内容不违规，已驳回';
  }
});

// 加载举报列表
const loadReports = async () => {
  loading.value = true;
  try {
    const response = await reportApi.getReportList(
      currentPage.value - 1,
      pageSize.value,
      statusFilter.value,
      contentTypeFilter.value
    );
    
    if (response.success) {
      reports.value = response.data.content || [];
      total.value = response.data.totalElements || 0;
    } else {
      ElMessage.error(response.message || '获取举报列表失败');
    }
  } catch (error) {
    console.error('加载举报列表失败:', error);
    ElMessage.error('加载举报列表失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 加载举报统计信息
const loadReportStats = async () => {
  try {
    const response = await reportApi.getReportStatistics();
    if (response.success) {
      reportStats.value = response.data || {
        statusCount: {
          pending: 0,
          processing: 0,
          resolved: 0,
          rejected: 0
        },
        contentTypeCount: {}
      };
    }
  } catch (error) {
    console.error('加载举报统计信息失败:', error);
  }
};

// 处理页码变化
const handlePageChange = (page) => {
  currentPage.value = page;
  loadReports();
};

// 查看举报详情
const viewReportDetails = async (report) => {
  try {
    const response = await reportApi.getReportDetails(report.reportId);
    if (response.success) {
      currentReport.value = response.data;
      showReportDetails.value = true;
    } else {
      ElMessage.error(response.message || '获取举报详情失败');
    }
  } catch (error) {
    console.error('获取举报详情失败:', error);
    ElMessage.error('获取举报详情失败，请稍后重试');
  }
};

// 处理举报
const handleReport = (report) => {
  currentReport.value = report;
  
  // 根据当前状态设置默认操作
  if (report.status === 'pending') {
    // 待处理状态，默认设置为处理中
  handleForm.value = {
    action: 'process',
    result: '正在进行处理',
    note: '',
    userAction: 'none',
    contentAction: 'none',
    banDuration: 1
  };
  } else if (report.status === 'processing') {
    // 处理中状态，默认设置为已解决
    handleForm.value = {
      action: 'resolve',
      result: '举报内容违规，已处理',
      note: '',
      userAction: 'none',
      contentAction: 'none',
      banDuration: 1
    };
  }
  
  showHandleReportDialog.value = true;
  showReportDetails.value = false; // 关闭详情对话框
};

// 提交处理结果
const submitHandleReport = async () => {
  if (!currentReport.value) return;
  
  if (!handleForm.value.action) {
    ElMessage.warning('请选择处理方式');
    return;
  }
  
  if (!handleForm.value.result) {
    ElMessage.warning('请选择处理结果');
    return;
  }
  
  submitting.value = true;
  try {
    const response = await reportApi.handleReport(
      currentReport.value.reportId,
      handleForm.value.action,
      handleForm.value.result,
      handleForm.value.note,
      handleForm.value.userAction,
      handleForm.value.contentAction,
      handleForm.value.banDuration // 封禁时长参数
    );
    
    if (response.success) {
      ElMessage.success('举报处理成功');
      showHandleReportDialog.value = false;
      loadReports(); // 重新加载列表
      loadReportStats(); // 重新加载统计信息
    } else {
      ElMessage.error(response.message || '处理举报失败');
    }
  } catch (error) {
    console.error('处理举报失败:', error);
    ElMessage.error('处理举报失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
};

// 格式化状态
const formatStatus = (status) => {
  switch (status) {
    case 'pending': return '待处理';
    case 'processing': return '处理中';
    case 'resolved': return '已解决';
    case 'rejected': return '已拒绝';
    default: return status;
  }
};

// 格式化状态标签
const formatStatusLabel = (status) => {
  switch (status) {
    case 'pending': return '待处理';
    case 'processing': return '处理中';
    case 'resolved': return '已解决';
    case 'rejected': return '已拒绝';
    default: return status;
  }
};

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 'pending': return 'warning';
    case 'processing': return 'info';
    case 'resolved': return 'success';
    case 'rejected': return 'danger';
    default: return 'info';
  }
};

// 格式化内容类型
const formatContentType = (type) => {
  switch (type) {
    case 'USER': return '用户';
    case 'MESSAGE': return '消息';
    case 'GROUP': return '群组';
    case 'GROUP_MEMBER': return '群组成员';
    case 'MOMENT': return '动态';
    default: return type;
  }
};

// 格式化举报原因
const formatReasonType = (reason) => {
  switch (reason) {
    case 'SPAM': return '垃圾信息';
    case 'PORNOGRAPHY': return '色情内容';
    case 'VIOLENCE': return '暴力内容';
    case 'SCAM': return '诈骗信息';
    case 'PRIVACY': return '侵犯隐私';
    case 'INSULT': return '侮辱他人';
    case 'OTHER': return '其他';
    default: return reason;
  }
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleString();
};

// 处理结果选项
const resultOptions = computed(() => {
  const options = [];
  if (handleForm.value.action === 'process') {
    options.push({ label: '正在进行处理', value: '正在进行处理' });
  } else if (handleForm.value.action === 'resolve') {
    options.push({ label: '举报内容违规，已处理', value: '举报内容违规，已处理' });
    options.push({ label: '内容已删除', value: '内容已删除' });
    options.push({ label: '用户已封禁', value: '用户已封禁' });
    options.push({ label: '群组已封禁', value: '群组已封禁' });
    options.push({ label: '群组已解散', value: '群组已解散' });
  } else if (handleForm.value.action === 'reject') {
    options.push({ label: '举报内容不违规，已驳回', value: '举报内容不违规，已驳回' });
    options.push({ label: '证据不足，无法处理', value: '证据不足，无法处理' });
  }
  return options;
});

// 监听筛选条件变化
watch([statusFilter, contentTypeFilter], () => {
  currentPage.value = 1; // 重置页码
  loadReports();
});

// 组件挂载时加载数据
onMounted(() => {
  loadReports();
  loadReportStats();
});
</script>

<style scoped>
.report-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.filter-controls {
  display: flex;
  gap: 10px;
}

.report-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  text-align: center;
}

.stat-content {
  padding: 10px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #409EFF;
}

.stat-label {
  margin-top: 5px;
  font-size: 14px;
  color: #606266;
}

.user-info, .reported-content {
  display: flex;
  flex-direction: column;
}

.user-id, .content-id {
  font-size: 12px;
  color: #909399;
}

.reported-user {
  margin-top: 5px;
  font-size: 13px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 举报详情样式 */
.report-details {
  padding: 10px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h3 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #EBEEF5;
  padding-bottom: 5px;
}

.detail-item {
  display: flex;
  margin-bottom: 8px;
}

.detail-item .label {
  width: 100px;
  color: #606266;
  font-weight: 500;
}

.detail-item .value {
  flex: 1;
}

.detail-item.description {
  flex-direction: column;
}

.description-text {
  margin-top: 5px;
  padding: 10px;
  background-color: #F5F7FA;
  border-radius: 4px;
  white-space: pre-wrap;
}

/* 处理举报表单样式 */
.handle-report-form {
  padding: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

.reported-content-section {
  margin-top: 15px;
}

.reported-content-section h4 {
  font-size: 16px;
  margin-bottom: 10px;
  color: #303133;
  font-weight: 500;
}

/* 处理流程提示样式 */
.process-tip {
  margin-bottom: 15px;
}

.tip-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}
</style> 