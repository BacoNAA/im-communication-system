<!-- 系统概览组件 -->
<template>
  <div class="system-overview">
    <div class="dashboard-header">
      <h1>系统概览</h1>
      <div class="date-filter">
        <button 
          v-for="period in datePeriods" 
          :key="period.value"
          :class="['period-btn', { active: selectedPeriod === period.value }]"
          @click="selectPeriod(period.value)"
        >
          {{ period.label }}
        </button>
      </div>
    </div>
    
    <el-row v-loading="loading" :gutter="20">
      <el-col :span="6">
        <stat-card
          title="总用户数"
          :value="stats.totalUsers"
          :growth-value="stats.userGrowth"
          icon-name="fa-users"
          icon-class="users"
        />
      </el-col>
      <el-col :span="6">
        <stat-card
          title="日活跃用户"
          :value="stats.activeUsers"
          :growth-value="stats.activeGrowth"
          icon-name="fa-chart-line"
          icon-class="active"
        />
      </el-col>
      <el-col :span="6">
        <stat-card
          title="消息总量"
          :value="stats.totalMessages"
          :growth-value="stats.messageGrowth"
          icon-name="fa-comment-dots"
          icon-class="messages"
        />
      </el-col>
      <el-col :span="6">
        <stat-card
          title="群组总数"
          :value="stats.totalGroups"
          :growth-value="stats.groupGrowth"
          icon-name="fa-user-friends"
          icon-class="groups"
        />
      </el-col>
    </el-row>

    <el-row style="margin-top: 20px" :gutter="20" v-loading="loading">
      <el-col :span="6">
        <stat-card
          title="动态总数"
          :value="stats.totalMoments"
          :growth-value="stats.momentGrowth"
          icon-name="fa-images"
          icon-class="moments"
        />
      </el-col>
      <el-col :span="6">
        <stat-card
          title="新注册用户"
          :value="stats.newUsers"
          icon-name="fa-user-plus"
          icon-class="active"
          :show-growth="false"
        />
      </el-col>
      <el-col :span="12">
        <div class="refresh-info">
          <span>
            {{ formatUpdateTime() }}
            <el-button type="text" @click="fetchData" icon="el-icon-refresh">
              <i class="fas fa-sync-alt"></i> 刷新
            </el-button>
          </span>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElRow, ElCol } from 'element-plus';
import StatCard from './StatCard.vue';
import adminApi from '@/api/admin';

// 日期筛选选项
const datePeriods = [
  { label: '今日', value: 'today' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '全年', value: 'year' }
];

const selectedPeriod = ref('today');
const loading = ref(false);
const lastUpdateTime = ref(null);

// 统计数据
const stats = ref({
  totalUsers: 0,
  userGrowth: 0,
  activeUsers: 0,
  activeGrowth: 0,
  totalMessages: 0,
  messageGrowth: 0,
  totalGroups: 0,
  groupGrowth: 0,
  totalMoments: 0,
  momentGrowth: 0,
  newUsers: 0
});

// 选择时间段
const selectPeriod = (period) => {
  selectedPeriod.value = period;
  fetchData();
};

// 获取数据
const fetchData = async () => {
  loading.value = true;
  
  try {
    const response = await adminApi.getSystemOverview(selectedPeriod.value);
    
    if (response.success && response.data) {
      stats.value = response.data;
      lastUpdateTime.value = new Date();
    } else {
      ElMessage.error(response.message || '获取系统概览数据失败');
    }
  } catch (error) {
    console.error('获取系统概览数据出错:', error);
    ElMessage.error('获取系统概览数据出错，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 格式化更新时间
const formatUpdateTime = () => {
  if (!lastUpdateTime.value) return '数据未更新';
  
  const now = new Date();
  const diff = Math.floor((now - lastUpdateTime.value) / 1000);
  
  if (diff < 60) {
    return `数据更新于 ${diff} 秒前`;
  } else if (diff < 3600) {
    return `数据更新于 ${Math.floor(diff / 60)} 分钟前`;
  } else {
    return `数据更新于 ${lastUpdateTime.value.toLocaleTimeString()}`;
  }
};

// 组件挂载时获取数据
onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.system-overview {
  margin-bottom: 24px;
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
  transition: all 0.2s;
}

.period-btn:hover {
  color: #3182ce;
}

.period-btn.active {
  background-color: #ffffff;
  color: #3182ce;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.refresh-info {
  height: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  padding-bottom: 8px;
  color: #718096;
  font-size: 14px;
}

.refresh-info i {
  margin-right: 4px;
}
</style> 