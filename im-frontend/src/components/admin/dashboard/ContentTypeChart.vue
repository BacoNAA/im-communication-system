<!-- 内容类型分布图表组件 -->
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
          <h4>关于内容类型分布</h4>
          <p>此图表显示系统中各类型消息内容的分布情况。</p>
          <p>数据来源：系统中所有消息按类型的分组统计。</p>
          <p>更新频率：根据选择的时间周期实时统计。</p>
        </div>
      </el-popover>
    </div>
    <div class="chart-container" v-loading="loading">
      <pie-chart
        :data="chartData"
        :loading="loading"
        @refresh="fetchData"
        :title="title"
      />
    </div>
    <div class="legend-container">
      <div class="legend-item" v-for="(value, key) in typeData" :key="key">
        <div class="legend-color" :style="{backgroundColor: getColorByType(key)}"></div>
        <div class="legend-label">{{ getTypeName(key) }}</div>
        <div class="legend-value">{{ formatPercent(value) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { ElMessage, ElPopover } from 'element-plus';
import PieChart from './PieChart.vue';
import adminApi from '@/api/admin';

const props = defineProps({
  title: {
    type: String,
    default: '内容类型分布'
  },
  period: {
    type: String,
    default: 'week'
  }
});

const loading = ref(false);
const chartData = ref([]);
const typeData = ref({});
const total = ref(0);

// 内容类型名称映射
const typeNames = {
  text: '文本消息',
  image: '图片消息',
  video: '视频消息',
  file: '文件消息',
  voice: '语音消息',
  system: '系统消息',
  other: '其他'
};

// 内容类型颜色映射
const typeColors = {
  text: '#4299e1',
  image: '#48bb78',
  video: '#ed8936',
  file: '#9f7aea',
  voice: '#38b2ac',
  system: '#f6ad55',
  other: '#cbd5e0'
};

// 监听时间段变化
watch(() => props.period, () => {
  fetchData();
}, { immediate: false });

// 获取数据
const fetchData = async () => {
  loading.value = true;
  
  try {
    const response = await adminApi.getContentTypeDistribution(props.period);
    
    if (response.success && response.data && response.data.distribution) {
      typeData.value = response.data.distribution;
      total.value = response.data.total || 0;
      
      // 转换为饼图数据格式
      chartData.value = Object.entries(response.data.distribution).map(([type, count]) => ({
        name: getTypeName(type),
        value: count,
        itemStyle: {
          color: getColorByType(type)
        }
      }));
    } else {
      ElMessage.error(response.message || '获取内容类型分布数据失败');
    }
  } catch (error) {
    console.error('获取内容类型分布数据出错:', error);
    ElMessage.error('获取内容类型分布数据出错，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 获取类型名称
const getTypeName = (type) => {
  return typeNames[type] || type;
};

// 获取类型颜色
const getColorByType = (type) => {
  return typeColors[type] || '#cbd5e0';
};

// 格式化百分比
const formatPercent = (value) => {
  if (!total.value) return '0%';
  return Math.round((value / total.value) * 100) + '%';
};

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

.chart-container {
  flex: 1;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.legend-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 10px;
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px dashed #e2e8f0;
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 12px;
}

.legend-color {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 5px;
}

.legend-label {
  flex: 1;
  color: #4a5568;
}

.legend-value {
  font-weight: 600;
  color: #2d3748;
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