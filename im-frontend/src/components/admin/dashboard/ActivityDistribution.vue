<!-- 用户活跃度分布组件 -->
<template>
  <pie-chart
    title="用户活跃度分布"
    :data="chartData"
    :loading="loading"
    @refresh="fetchData"
  />
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import PieChart from './PieChart.vue';
import adminApi from '@/api/admin';

const props = defineProps({
  period: {
    type: String,
    default: 'week'
  },
  type: {
    type: String,
    default: 'timeDistribution',
    validator: value => ['timeDistribution', 'deviceDistribution', 'actionDistribution'].includes(value)
  }
});

const loading = ref(false);
const chartData = ref([]);

// 活动分布类型名称映射
const distributionLabels = {
  timeDistribution: {
    morning: '早上 (6-12点)',
    afternoon: '下午 (12-18点)',
    evening: '晚上 (18-24点)',
    night: '凌晨 (0-6点)'
  },
  deviceDistribution: {
    android: '安卓设备',
    ios: 'iOS设备',
    web: '网页端',
    desktop: '桌面端'
  },
  actionDistribution: {
    message: '发送消息',
    moment: '发布动态',
    group: '群组活动',
    profile: '个人资料'
  }
};

// 监听时间段变化
watch(() => [props.period, props.type], () => {
  fetchData();
}, { immediate: false });

// 格式化分布数据
const formatDistributionData = (data) => {
  if (!data || typeof data !== 'object') return [];
  
  const labels = distributionLabels[props.type] || {};
  
  return Object.entries(data).map(([key, value]) => ({
    name: labels[key] || key,
    value
  }));
};

// 获取数据
const fetchData = async () => {
  loading.value = true;
  
  try {
    const response = await adminApi.getUserActivityDistribution(props.period);
    
    if (response.success && response.data) {
      const distributionData = response.data[props.type];
      chartData.value = formatDistributionData(distributionData);
    } else {
      ElMessage.error(response.message || '获取用户活跃度分布数据失败');
    }
  } catch (error) {
    console.error('获取用户活跃度分布数据出错:', error);
    ElMessage.error('获取用户活跃度分布数据出错，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 组件挂载时获取数据
onMounted(() => {
  fetchData();
});
</script> 