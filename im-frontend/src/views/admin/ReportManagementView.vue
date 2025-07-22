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
            v-if="scope.row.status === 'pending'"
            size="small" 
            type="success" 
            @click="handleReport(scope.row)"
          >
            处理
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
      width="700px"
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
          
          <!-- 被举报内容详情 -->
          <div v-if="contentDetails" class="content-details">
            <h4>内容详情</h4>
            <el-divider></el-divider>
            
            <!-- 加载状态 -->
            <div v-if="contentLoading" class="content-loading">
              <el-skeleton :rows="3" animated />
            </div>
            
            <!-- 加载失败 -->
            <div v-else-if="contentError" class="content-error">
              <el-alert
                type="error"
                :title="contentError"
                show-icon
              />
              <el-button size="small" type="primary" @click="loadReportedContent" class="mt-2">
                重试加载
              </el-button>
            </div>
            
            <!-- 用户内容 -->
            <div v-else-if="currentReport.reportedContentType === 'USER'" class="user-content">
              <div class="user-profile">
                <div class="user-avatar">
                  <el-avatar :size="64" :src="contentDetails.avatar || '/images/default-avatar.png'" />
                </div>
                <div class="user-info">
                  <h3>{{ contentDetails.nickname || contentDetails.username || '未知用户' }}</h3>
                  <p v-if="contentDetails.email">邮箱: {{ contentDetails.email }}</p>
                  <p v-if="contentDetails.signature">个性签名: {{ contentDetails.signature }}</p>
                  <p>注册时间: {{ formatDate(contentDetails.createdAt) }}</p>
                </div>
              </div>
            </div>
            
            <!-- 消息内容 -->
            <div v-else-if="currentReport.reportedContentType === 'MESSAGE'" class="message-content">
              <div class="message-header">
                <span>发送者: {{ contentDetails.senderName || contentDetails.nickname || `用户#${contentDetails.senderId}` }}</span>
                <span>发送时间: {{ formatDate(contentDetails.createdAt) }}</span>
              </div>
              <div class="message-body">
                <div v-if="contentDetails.messageType === 'TEXT' || !contentDetails.messageType" class="text-message">
                  {{ contentDetails.content || contentDetails.text || contentDetails.message || '无内容' }}
                </div>
                <div v-else-if="contentDetails.messageType === 'IMAGE'" class="image-message">
                  <el-image 
                    :src="contentDetails.fileUrl || contentDetails.content" 
                    :preview-src-list="[contentDetails.fileUrl || contentDetails.content]"
                    fit="contain"
                    style="max-width: 300px; max-height: 200px;"
                  />
                </div>
                <div v-else class="other-message">
                  <p>消息类型: {{ contentDetails.messageType }}</p>
                  <p v-if="contentDetails.content || contentDetails.text">{{ contentDetails.content || contentDetails.text }}</p>
                  <p v-if="contentDetails.fileUrl">
                    <a :href="contentDetails.fileUrl" target="_blank">{{ contentDetails.fileName || '查看文件' }}</a>
                  </p>
                </div>
              </div>
            </div>
            
            <!-- 群组内容 -->
            <div v-else-if="currentReport.reportedContentType === 'GROUP'" class="group-content">
              <div class="group-header">
                <h3>{{ contentDetails.name || `群组#${currentReport.reportedContentId}` }}</h3>
                <p v-if="contentDetails.description">{{ contentDetails.description }}</p>
                <p>成员数: {{ contentDetails.memberCount || '未知' }}</p>
                <p>创建时间: {{ formatDate(contentDetails.createdAt) }}</p>
              </div>
            </div>
            
            <!-- 群组成员内容 -->
            <div v-else-if="currentReport.reportedContentType === 'GROUP_MEMBER'" class="group-member-content">
              <div class="member-info">
                <el-avatar :size="50" :src="contentDetails.avatar || '/images/default-avatar.png'" />
                <div class="member-details">
                  <h3>{{ contentDetails.nickname || contentDetails.username || `用户#${currentReport.reportedContentId}` }}</h3>
                  <p v-if="contentDetails.groupName">所在群组: {{ contentDetails.groupName }}</p>
                  <p v-if="contentDetails.role">角色: {{ contentDetails.role }}</p>
                  <p v-if="contentDetails.joinTime">加入时间: {{ formatDate(contentDetails.joinTime) }}</p>
                </div>
              </div>
            </div>
            
            <!-- 动态内容 -->
            <div v-else-if="currentReport.reportedContentType === 'MOMENT'" class="moment-content">
              <div class="moment-header">
                <div class="moment-user">
                  <el-avatar :size="40" :src="contentDetails.avatar || contentDetails.userAvatar || '/images/default-avatar.png'" />
                  <span>{{ contentDetails.nickname || contentDetails.username || contentDetails.authorName || `用户#${contentDetails.userId || contentDetails.authorId}` }}</span>
                </div>
                <span class="moment-time">{{ formatDate(contentDetails.createdAt || contentDetails.createTime) }}</span>
              </div>
              <div class="moment-body">
                <p class="moment-text">{{ contentDetails.text || contentDetails.content || contentDetails.description || '无内容' }}</p>
                
                <!-- 处理媒体显示 -->
                <div v-if="hasMomentMedia" class="moment-media">
                  <template v-if="Array.isArray(contentDetails.mediaUrls)">
                    <el-image 
                      v-for="(url, index) in contentDetails.mediaUrls" 
                      :key="index"
                      :src="url"
                      :preview-src-list="contentDetails.mediaUrls"
                      fit="cover"
                      class="moment-image"
                    />
                  </template>
                  <template v-else-if="contentDetails.mediaUrl || contentDetails.imageUrl">
                    <el-image 
                      :src="contentDetails.mediaUrl || contentDetails.imageUrl"
                      :preview-src-list="[contentDetails.mediaUrl || contentDetails.imageUrl]"
                      fit="cover"
                      class="moment-image"
                    />
                  </template>
                  <template v-else-if="Array.isArray(contentDetails.images)">
                    <el-image 
                      v-for="(img, index) in contentDetails.images" 
                      :key="index"
                      :src="img.url || img"
                      :preview-src-list="contentDetails.images.map(img => img.url || img)"
                      fit="cover"
                      class="moment-image"
                    />
                  </template>
                </div>
                
                <div class="moment-stats">
                  <span><i class="el-icon-thumb"></i> {{ contentDetails.likeCount || contentDetails.likes || 0 }}</span>
                  <span><i class="el-icon-chat-dot-round"></i> {{ contentDetails.commentCount || contentDetails.comments || 0 }}</span>
                </div>
              </div>
            </div>
            
            <!-- 其他类型内容 -->
            <div v-else class="other-content">
              <el-alert
                type="info"
                title="无法显示此类型内容的详情"
                show-icon
              />
              <div v-if="contentDetails.content" class="raw-content">
                <pre>{{ contentDetails.content }}</pre>
              </div>
            </div>
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
            v-if="currentReport && currentReport.status === 'pending'"
            type="primary" 
            @click="handleReport(currentReport)"
          >
            处理举报
          </el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 处理举报对话框 -->
    <el-dialog
      v-model="showHandleReportDialog"
      title="处理举报"
      width="500px"
      destroy-on-close
    >
      <div v-if="currentReport" class="handle-report-form">
        <el-form :model="handleForm" label-width="100px">
          <el-form-item label="处理方式" required>
            <el-radio-group v-model="handleForm.action">
              <el-radio label="process">标记处理中</el-radio>
              <el-radio label="resolve">解决举报</el-radio>
              <el-radio label="reject">拒绝举报</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="处理结果" required>
            <el-input 
              v-model="handleForm.result" 
              placeholder="请输入处理结果"
              type="textarea"
              :rows="2"
            ></el-input>
          </el-form-item>
          
          <el-form-item label="处理备注">
            <el-input 
              v-model="handleForm.note" 
              placeholder="可选，添加处理备注"
              type="textarea"
              :rows="2"
            ></el-input>
          </el-form-item>
          
          <el-form-item label="对用户操作">
            <el-select v-model="handleForm.userAction" placeholder="选择对用户的操作">
              <el-option label="无操作" value="none"></el-option>
              <el-option label="警告" value="warn"></el-option>
              <el-option label="临时封禁" value="temporary_ban"></el-option>
              <el-option label="永久封禁" value="permanent_ban"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="对内容操作">
            <el-select v-model="handleForm.contentAction" placeholder="选择对内容的操作">
              <el-option label="无操作" value="none"></el-option>
              <el-option label="删除内容" value="delete"></el-option>
              <el-option label="隐藏内容" value="hide"></el-option>
              <el-option label="标记为敏感" value="mark_as_sensitive"></el-option>
            </el-select>
          </el-form-item>
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
  result: '',
  note: '',
  userAction: 'none',
  contentAction: 'none'
});

// 举报内容详情
const contentDetails = ref(null);
const contentLoading = ref(false);
const contentError = ref(null);

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

// 判断动态是否有媒体内容
const hasMomentMedia = computed(() => {
  if (!contentDetails.value) return false;
  
  return (
    (Array.isArray(contentDetails.value.mediaUrls) && contentDetails.value.mediaUrls.length > 0) ||
    (contentDetails.value.mediaUrl) ||
    (contentDetails.value.imageUrl) ||
    (Array.isArray(contentDetails.value.images) && contentDetails.value.images.length > 0)
  );
});

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
      
      // 加载举报内容详情
      loadReportedContent();
    } else {
      ElMessage.error(response.message || '获取举报详情失败');
    }
  } catch (error) {
    console.error('获取举报详情失败:', error);
    ElMessage.error('获取举报详情失败，请稍后重试');
  }
};

// 加载举报内容详情
const loadReportedContent = async () => {
  if (!currentReport.value) return;
  
  const contentType = currentReport.value.reportedContentType;
  const contentId = currentReport.value.reportedContentId;
  
  contentLoading.value = true;
  contentError.value = null;
  contentDetails.value = null;
  
  try {
    let response;
    
    switch (contentType) {
      case 'USER':
        response = await reportApi.getUserDetails(contentId);
        break;
      case 'MESSAGE':
        response = await reportApi.getMessageDetails(contentId);
        break;
      case 'GROUP':
        response = await reportApi.getGroupDetails(contentId);
        break;
      case 'MOMENT':
        response = await reportApi.getMomentDetails(contentId);
        break;
      default:
        // 尝试使用通用API
        response = await reportApi.getReportedContentDetails(contentType, contentId);
    }
    
    if (response.success && response.data) {
      contentDetails.value = response.data;
    } else {
      contentError.value = response.message || '获取内容详情失败';
    }
  } catch (error) {
    console.error('加载举报内容详情失败:', error);
    contentError.value = error instanceof Error ? error.message : '加载内容详情失败，请稍后重试';
  } finally {
    contentLoading.value = false;
  }
};

// 处理举报
const handleReport = (report) => {
  currentReport.value = report;
  handleForm.value = {
    action: 'process',
    result: '',
    note: '',
    userAction: 'none',
    contentAction: 'none'
  };
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
    ElMessage.warning('请输入处理结果');
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
      handleForm.value.contentAction
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

.content-details {
  margin-top: 15px;
  background-color: #f9f9f9;
  border-radius: 4px;
  padding: 15px;
}

.content-loading {
  padding: 20px 0;
}

.content-error {
  padding: 10px 0;
}

.mt-2 {
  margin-top: 8px;
}

/* 用户内容样式 */
.user-profile {
  display: flex;
  align-items: flex-start;
  gap: 15px;
}

.user-info h3 {
  margin-top: 0;
  margin-bottom: 8px;
}

.user-info p {
  margin: 5px 0;
  color: #666;
}

/* 消息内容样式 */
.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #666;
  font-size: 14px;
}

.message-body {
  padding: 10px;
  background-color: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

/* 群组内容样式 */
.group-header h3 {
  margin-top: 0;
  margin-bottom: 8px;
}

.group-header p {
  margin: 5px 0;
  color: #666;
}

/* 群组成员样式 */
.member-info {
  display: flex;
  align-items: flex-start;
  gap: 15px;
}

.member-details h3 {
  margin-top: 0;
  margin-bottom: 8px;
}

.member-details p {
  margin: 5px 0;
  color: #666;
}

/* 动态内容样式 */
.moment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.moment-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.moment-time {
  color: #999;
  font-size: 13px;
}

.moment-body {
  padding: 10px;
  background-color: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.moment-text {
  margin-top: 0;
  margin-bottom: 10px;
  white-space: pre-wrap;
}

.moment-media {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.moment-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 4px;
}

.moment-stats {
  display: flex;
  gap: 15px;
  color: #666;
  font-size: 14px;
}

/* 其他内容样式 */
.raw-content {
  margin-top: 10px;
  max-height: 200px;
  overflow-y: auto;
  background-color: #f2f2f2;
  padding: 10px;
  border-radius: 4px;
  font-family: monospace;
}

.raw-content pre {
  margin: 0;
  white-space: pre-wrap;
}
</style> 