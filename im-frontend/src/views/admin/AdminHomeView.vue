<!-- 管理员首页视图 -->
<template>
  <div class="admin-home">
    <div class="page-header">
      <h1>管理员控制台</h1>
      <div class="toolbar">
        <el-popover
          placement="bottom"
          :width="300"
          trigger="click"
        >
          <template #reference>
            <el-button type="info" plain size="small">
              <el-icon><InfoFilled /></el-icon>
              增长率说明
            </el-button>
          </template>
          <div class="growth-rate-info">
            <h4>增长率计算方式</h4>
            <p>增长率 = (当前期间数值 - 上一期间数值) / 上一期间数值 × 100%</p>
            <p>当上一期间数值为0且当前期间有增长时，增长率显示为100%</p>
            <p>当增长率超过1000%时，显示为1000%+</p>
            <p>负增长率代表数值下降</p>
          </div>
        </el-popover>
      </div>
    </div>
    
    <!-- 系统概览部分 -->
    <system-overview />
    
    <!-- 统计图表部分 -->
    <div class="dashboard-row">
      <user-growth-chart />
      <activity-distribution />
    </div>
    
    <div class="dashboard-row">
      <message-trend-chart />
      <content-type-chart />
    </div>
  </div>
</template>

<script setup>
import { InfoFilled } from '@element-plus/icons-vue';
import SystemOverview from '@/components/admin/dashboard/SystemOverview.vue';
import UserGrowthChart from '@/components/admin/dashboard/UserGrowthChart.vue';
import ActivityDistribution from '@/components/admin/dashboard/ActivityDistribution.vue';
import MessageTrendChart from '@/components/admin/dashboard/MessageTrendChart.vue';
import ContentTypeChart from '@/components/admin/dashboard/ContentTypeChart.vue';
</script>

<style scoped>
.admin-home {
  padding: 0 10px;
}

.dashboard-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.chart-card-wrapper {
  height: 400px;
}

.distribution-type-selector {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.distribution-type-selector span {
  margin-right: 10px;
  font-size: 14px;
  color: #4a5568;
}

.growth-rate-explanation {
  margin-left: auto;
}

.explanation-link {
  color: #3182ce;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.explanation-link i {
  margin-right: 5px;
}

.explanation-content h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 16px;
  color: #2d3748;
}

.explanation-content p {
  margin: 8px 0;
  font-size: 14px;
  color: #4a5568;
}

.explanation-content ul {
  margin: 8px 0;
  padding-left: 20px;
}

.explanation-content li {
  font-size: 13px;
  color: #4a5568;
  margin-bottom: 4px;
}

.explanation-content .note {
  font-style: italic;
  font-size: 12px;
  color: #718096;
  margin-top: 10px;
}
</style> 