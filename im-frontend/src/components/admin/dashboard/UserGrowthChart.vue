<!-- 用户增长趋势图表组件 -->
<template>
  <line-chart
    title="用户增长趋势"
    :labels="chartData.labels"
    :data="chartData.data"
    :compareData="chartData.compareData"
    :loading="loading"
    @refresh="fetchData"
  />
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import LineChart from './LineChart.vue';
import adminApi from '@/api/admin';

const props = defineProps({
  period: {
    type: String,
    default: 'week'
  }
});

const loading = ref(false);
const chartData = ref({
  labels: [],
  data: [],
  compareData: []
});

// 监听时间段变化
watch(() => props.period, () => {
  fetchData();
}, { immediate: false });

// 获取数据
const fetchData = async () => {
  loading.value = true;
  
  try {
    const response = await adminApi.getStatistics('users', props.period);
    
    if (response.success && response.data) {
      chartData.value = {
        labels: response.data.labels || [],
        data: response.data.data || [],
        compareData: response.data.compareData || []
      };
    } else {
      ElMessage.error(response.message || '获取用户增长趋势数据失败');
    }
  } catch (error) {
    console.error('获取用户增长趋势数据出错:', error);
    ElMessage.error('获取用户增长趋势数据出错，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 组件挂载时获取数据
onMounted(() => {
  fetchData();
});
</script> 