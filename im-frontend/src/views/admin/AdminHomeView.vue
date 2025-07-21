<template>
  <div class="admin-home">
    <div class="dashboard-header">
      <h1>系统概览</h1>
      <div class="date-filter">
        <button 
          v-for="period in datePeriods" 
          :key="period.value"
          :class="['period-btn', { active: selectedPeriod === period.value }]"
          @click="selectedPeriod = period.value"
        >
          {{ period.label }}
        </button>
      </div>
    </div>
    
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon users">
          <i class="fas fa-users"></i>
        </div>
        <div class="stat-content">
          <div class="stat-title">总用户数</div>
          <div class="stat-value">{{ formatNumber(stats.totalUsers) }}</div>
          <div class="stat-change" :class="stats.userGrowth >= 0 ? 'positive' : 'negative'">
            <i :class="stats.userGrowth >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
            {{ Math.abs(stats.userGrowth) }}%
          </div>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon active">
          <i class="fas fa-chart-line"></i>
        </div>
        <div class="stat-content">
          <div class="stat-title">日活跃用户</div>
          <div class="stat-value">{{ formatNumber(stats.activeUsers) }}</div>
          <div class="stat-change" :class="stats.activeGrowth >= 0 ? 'positive' : 'negative'">
            <i :class="stats.activeGrowth >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
            {{ Math.abs(stats.activeGrowth) }}%
          </div>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon messages">
          <i class="fas fa-comment-dots"></i>
        </div>
        <div class="stat-content">
          <div class="stat-title">消息总量</div>
          <div class="stat-value">{{ formatNumber(stats.totalMessages) }}</div>
          <div class="stat-change" :class="stats.messageGrowth >= 0 ? 'positive' : 'negative'">
            <i :class="stats.messageGrowth >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
            {{ Math.abs(stats.messageGrowth) }}%
          </div>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon groups">
          <i class="fas fa-user-friends"></i>
        </div>
        <div class="stat-content">
          <div class="stat-title">群组总数</div>
          <div class="stat-value">{{ formatNumber(stats.totalGroups) }}</div>
          <div class="stat-change" :class="stats.groupGrowth >= 0 ? 'positive' : 'negative'">
            <i :class="stats.groupGrowth >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
            {{ Math.abs(stats.groupGrowth) }}%
          </div>
        </div>
      </div>
    </div>
    
    <div class="dashboard-row">
      <div class="chart-card">
        <div class="card-header">
          <h3>用户增长趋势</h3>
          <div class="card-actions">
            <button class="action-btn">
              <i class="fas fa-download"></i>
            </button>
            <button class="action-btn">
              <i class="fas fa-ellipsis-v"></i>
            </button>
          </div>
        </div>
        <div class="chart-container">
          <!-- 这里应该放置用户增长趋势图表 -->
          <div class="placeholder-chart">用户增长趋势图表</div>
        </div>
      </div>
      
      <div class="chart-card">
        <div class="card-header">
          <h3>活跃度分布</h3>
          <div class="card-actions">
            <button class="action-btn">
              <i class="fas fa-download"></i>
            </button>
            <button class="action-btn">
              <i class="fas fa-ellipsis-v"></i>
            </button>
          </div>
        </div>
        <div class="chart-container">
          <!-- 这里应该放置活跃度分布图表 -->
          <div class="placeholder-chart">活跃度分布图表</div>
        </div>
      </div>
    </div>
    
    <div class="dashboard-row">
      <div class="table-card">
        <div class="card-header">
          <h3>待处理举报</h3>
          <router-link to="/admin/reports" class="view-all">查看全部</router-link>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>类型</th>
                <th>举报者</th>
                <th>举报时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="report in pendingReports" :key="report.id">
                <td>{{ report.id }}</td>
                <td>{{ report.type }}</td>
                <td>{{ report.reporter }}</td>
                <td>{{ formatDate(report.createdAt) }}</td>
                <td>
                  <span class="status-badge pending">待处理</span>
                </td>
                <td>
                  <router-link :to="`/admin/reports/${report.id}`" class="action-link">
                    处理
                  </router-link>
                </td>
              </tr>
              <tr v-if="pendingReports.length === 0">
                <td colspan="6" class="empty-table">暂无待处理举报</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <div class="table-card">
        <div class="card-header">
          <h3>系统状态</h3>
        </div>
        <div class="system-status">
          <div class="status-item">
            <div class="status-label">CPU 使用率</div>
            <div class="progress-bar">
              <div 
                class="progress" 
                :style="{ width: `${systemStatus.cpu}%` }"
                :class="getStatusClass(systemStatus.cpu)"
              ></div>
            </div>
            <div class="status-value">{{ systemStatus.cpu }}%</div>
          </div>
          
          <div class="status-item">
            <div class="status-label">内存使用率</div>
            <div class="progress-bar">
              <div 
                class="progress" 
                :style="{ width: `${systemStatus.memory}%` }"
                :class="getStatusClass(systemStatus.memory)"
              ></div>
            </div>
            <div class="status-value">{{ systemStatus.memory }}%</div>
          </div>
          
          <div class="status-item">
            <div class="status-label">磁盘使用率</div>
            <div class="progress-bar">
              <div 
                class="progress" 
                :style="{ width: `${systemStatus.disk}%` }"
                :class="getStatusClass(systemStatus.disk)"
              ></div>
            </div>
            <div class="status-value">{{ systemStatus.disk }}%</div>
          </div>
          
          <div class="status-item">
            <div class="status-label">API 响应时间</div>
            <div class="status-value">{{ systemStatus.apiLatency }}ms</div>
          </div>
          
          <div class="status-item">
            <div class="status-label">在线服务</div>
            <div class="status-value">{{ systemStatus.onlineServices }}/{{ systemStatus.totalServices }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// 日期筛选选项
const datePeriods = [
  { label: '今日', value: 'today' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '全年', value: 'year' }
]

const selectedPeriod = ref('today')

// 模拟数据 - 实际应用中应该从API获取
const stats = ref({
  totalUsers: 12568,
  userGrowth: 5.2,
  activeUsers: 3245,
  activeGrowth: 2.8,
  totalMessages: 1458962,
  messageGrowth: -1.5,
  totalGroups: 2876,
  groupGrowth: 3.7
})

const pendingReports = ref([
  {
    id: 'R-2025-07-21-001',
    type: '不当内容',
    reporter: 'user123',
    createdAt: new Date(Date.now() - 3600000 * 2)
  },
  {
    id: 'R-2025-07-21-002',
    type: '骚扰行为',
    reporter: 'user456',
    createdAt: new Date(Date.now() - 3600000 * 5)
  },
  {
    id: 'R-2025-07-20-008',
    type: '垃圾信息',
    reporter: 'user789',
    createdAt: new Date(Date.now() - 3600000 * 12)
  }
])

const systemStatus = ref({
  cpu: 42,
  memory: 68,
  disk: 53,
  apiLatency: 78,
  onlineServices: 12,
  totalServices: 12
})

// 格式化数字，添加千位分隔符
const formatNumber = (num) => {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")
}

// 格式化日期
const formatDate = (date) => {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

// 根据使用率获取状态类名
const getStatusClass = (value) => {
  if (value < 50) return 'normal'
  if (value < 80) return 'warning'
  return 'critical'
}

// 组件挂载时获取数据
onMounted(() => {
  // 这里应该调用API获取实际数据
  // fetchDashboardData()
})
</script>

<style scoped>
.admin-home {
  padding: 0 10px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.dashboard-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a202c;
}

.date-filter {
  display: flex;
  background-color: #edf2f7;
  border-radius: 8px;
  padding: 4px;
}

.period-btn {
  padding: 8px 16px;
  border: none;
  background: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  color: #4a5568;
}

.period-btn.active {
  background-color: #ffffff;
  color: #3182ce;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 20px;
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 20px;
  color: white;
}

.stat-icon.users {
  background-color: #4299e1;
}

.stat-icon.active {
  background-color: #48bb78;
}

.stat-icon.messages {
  background-color: #ed8936;
}

.stat-icon.groups {
  background-color: #9f7aea;
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #718096;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 4px;
}

.stat-change {
  font-size: 14px;
  display: flex;
  align-items: center;
}

.stat-change i {
  margin-right: 4px;
}

.stat-change.positive {
  color: #48bb78;
}

.stat-change.negative {
  color: #e53e3e;
}

.dashboard-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card, .table-card {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

.card-actions {
  display: flex;
}

.action-btn {
  background: none;
  border: none;
  color: #718096;
  cursor: pointer;
  padding: 4px 8px;
}

.chart-container {
  padding: 20px;
  height: 300px;
}

.placeholder-chart {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f7fafc;
  border-radius: 6px;
  color: #a0aec0;
  font-size: 14px;
}

.table-container {
  padding: 0 20px 20px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th, .data-table td {
  padding: 12px 8px;
  text-align: left;
  font-size: 14px;
}

.data-table th {
  color: #718096;
  font-weight: 500;
  border-bottom: 1px solid #e2e8f0;
}

.data-table td {
  border-bottom: 1px solid #f7fafc;
}

.status-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.pending {
  background-color: #feebc8;
  color: #c05621;
}

.action-link {
  color: #3182ce;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
}

.empty-table {
  text-align: center;
  color: #a0aec0;
  padding: 20px;
}

.view-all {
  color: #3182ce;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
}

.system-status {
  padding: 20px;
}

.status-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.status-item:last-child {
  margin-bottom: 0;
}

.status-label {
  width: 120px;
  font-size: 14px;
  color: #4a5568;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background-color: #edf2f7;
  border-radius: 4px;
  overflow: hidden;
  margin-right: 12px;
}

.progress {
  height: 100%;
  border-radius: 4px;
}

.progress.normal {
  background-color: #48bb78;
}

.progress.warning {
  background-color: #ed8936;
}

.progress.critical {
  background-color: #e53e3e;
}

.status-value {
  width: 60px;
  text-align: right;
  font-size: 14px;
  font-weight: 500;
  color: #2d3748;
}
</style> 