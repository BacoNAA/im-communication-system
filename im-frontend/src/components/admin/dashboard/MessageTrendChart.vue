<!-- 消息趋势图表组件 -->
<template>
  <div class="chart-wrapper">
    <div class="chart-header">
      <h3>{{ title }}</h3>
      <el-popover
        placement="bottom"
        trigger="hover"
        :width="300"
        popper-class="tooltip-popover"
      >
        <template #reference>
          <i class="fas fa-info-circle info-icon"></i>
        </template>
        <div class="tooltip-content">
          <h4>关于消息趋势图</h4>
          <p>此图表显示一段时间内的消息量变化趋势。</p>
          <p>增长率计算方式：(当前周期消息量 - 上一周期消息量) / 上一周期消息量 × 100%</p>
          <p>例如：当前周期有100条消息，上一周期有80条，则增长率为(100-80)/80×100% = 25%</p>
        </div>
      </el-popover>
    </div>
    <line-chart
      :title="title"
      :labels="chartData.labels"
      :data="chartData.data"
      :compareData="chartData.compareData"
      :loading="loading"
      @refresh="fetchData"
    />
    <div class="growth-info">
      <div class="growth-value" :class="growthClass">
        <i :class="growthIcon"></i>
        {{ formatGrowth(chartData.growthRate) }}
      </div>
      <div class="growth-label">
        与上一{{ periodLabel }}相比
        <el-tooltip content="增长率计算公式：(当前周期值 - 上一周期值) / 上一周期值 × 100%" placement="top">
          <i class="fas fa-question-circle"></i>
        </el-tooltip>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { ElMessage, ElPopover, ElTooltip } from 'element-plus';
import LineChart from './LineChart.vue';
import adminApi from '@/api/admin';

const props = defineProps({
  title: {
    type: String,
    default: '消息趋势'
  },
  period: {
    type: String,
    default: 'week'
  }
});

const loading = ref(false);
const chartData = ref({
  labels: [],
  data: [],
  compareData: [],
  growthRate: 0
});

// 监听时间段变化
watch(() => props.period, () => {
  fetchData();
}, { immediate: false });

// 获取数据
const fetchData = async () => {
  loading.value = true;
  
  try {
    const response = await adminApi.getMessageTrend(props.period);
    
    if (response.success && response.data) {
      chartData.value = {
        labels: response.data.labels || [],
        data: response.data.data || [],
        compareData: response.data.compareData || [],
        growthRate: response.data.growthRate || 0
      };
    } else {
      ElMessage.error(response.message || '获取消息趋势数据失败');
    }
  } catch (error) {
    console.error('获取消息趋势数据出错:', error);
    ElMessage.error('获取消息趋势数据出错，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 根据增长率计算样式类名
const growthClass = computed(() => {
  const rate = chartData.value.growthRate;
  if (rate > 0) return 'positive';
  if (rate < 0) return 'negative';
  return 'neutral';
});

// 根据增长率计算图标
const growthIcon = computed(() => {
  const rate = chartData.value.growthRate;
  if (rate > 0) return 'fas fa-arrow-up';
  if (rate < 0) return 'fas fa-arrow-down';
  return 'fas fa-minus';
});

// 格式化增长率显示
const formatGrowth = (value) => {
  if (value === undefined || value === null) return '0%';
  
  // 处理极端值，超过1000%显示为1000%+
  if (value > 1000) return '1000%+';
  if (value < -1000) return '-1000%+';
  
  return Math.abs(value).toFixed(1) + '%';
};

// 根据周期显示标签
const periodLabel = computed(() => {
  switch (props.period) {
    case 'today': return '小时';
    case 'week': return '天';
    case 'month': return '天';
    case 'year': return '月';
    default: return '周期';
  }
});

// 组件挂载时获取数据
onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.chart-wrapper {
  position: relative;
  height: 100%;
  padding: 10px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.chart-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

.info-icon {
  color: #718096;
  cursor: pointer;
  font-size: 14px;
}

.info-icon:hover {
  color: #3182ce;
}

.growth-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e2e8f0;
}

.growth-value {
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.growth-value i {
  margin-right: 5px;
}

.growth-value.positive {
  color: #48bb78;
}

.growth-value.negative {
  color: #e53e3e;
}

.growth-value.neutral {
  color: #718096;
}

.growth-label {
  font-size: 12px;
  color: #718096;
  margin-top: 4px;
}

.tooltip-content h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 14px;
  color: #2d3748;
}

.tooltip-content p {
  margin: 5px 0;
  font-size: 12px;
  color: #4a5568;
}
</style> 