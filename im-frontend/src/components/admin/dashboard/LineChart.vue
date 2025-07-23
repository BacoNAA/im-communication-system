<!-- 折线图组件 -->
<template>
  <div class="chart-card">
    <div class="card-header">
      <h3>{{ title }}</h3>
      <div class="card-actions">
        <el-button 
          type="text" 
          icon="el-icon-download"
          @click="downloadChart"
          title="下载图表"
        >
          <i class="fas fa-download"></i>
        </el-button>
        <el-dropdown trigger="click">
          <el-button type="text">
            <i class="fas fa-ellipsis-v"></i>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="refreshData">刷新数据</el-dropdown-item>
              <el-dropdown-item @click="toggleLegend">{{ showLegend ? '隐藏' : '显示' }}图例</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <div class="chart-container" ref="chartContainer"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { 
  TitleComponent, 
  TooltipComponent, 
  GridComponent,
  DatasetComponent,
  TransformComponent,
  LegendComponent,
  ToolboxComponent
} from 'echarts/components';
import { LabelLayout, UniversalTransition } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';

// 注册必要的组件
echarts.use([
  TitleComponent, 
  TooltipComponent, 
  GridComponent,
  DatasetComponent,
  TransformComponent,
  LegendComponent,
  LineChart,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer,
  ToolboxComponent
]);

const props = defineProps({
  title: {
    type: String,
    default: '数据图表'
  },
  labels: {
    type: Array,
    default: () => []
  },
  data: {
    type: Array,
    default: () => []
  },
  compareData: {
    type: Array,
    default: () => []
  },
  smooth: {
    type: Boolean,
    default: true
  },
  areaStyle: {
    type: Boolean,
    default: true
  },
  loading: {
    type: Boolean,
    default: false
  }
});

// 图表实例
const chartContainer = ref(null);
let chart = null;
const showLegend = ref(true);

// 监听数据变化，更新图表
watch(
  () => [props.labels, props.data, props.compareData, props.loading],
  () => {
    if (chart) {
      updateChart();
    }
  },
  { deep: true }
);

// 组件挂载时初始化图表
onMounted(() => {
  if (chartContainer.value) {
    chart = echarts.init(chartContainer.value);
    updateChart();

    // 监听窗口大小变化，调整图表大小
    window.addEventListener('resize', handleResize);
  }
});

// 组件销毁前清理资源
onBeforeUnmount(() => {
  if (chart) {
    chart.dispose();
    chart = null;
  }
  window.removeEventListener('resize', handleResize);
});

// 更新图表数据和配置
const updateChart = () => {
  if (!chart) return;
  
  if (props.loading) {
    chart.showLoading({
      text: '数据加载中...',
      color: '#4299e1',
      textColor: '#718096',
      maskColor: 'rgba(255, 255, 255, 0.8)'
    });
    return;
  } else {
    chart.hideLoading();
  }

  const series = [];
  
  // 主数据系列
  series.push({
    name: '数据',
    type: 'line',
    data: props.data,
    smooth: props.smooth,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: {
      width: 3,
      color: '#4299e1'
    },
    itemStyle: {
      color: '#4299e1'
    },
    areaStyle: props.areaStyle ? {
      opacity: 0.2,
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: '#4299e1' },
        { offset: 1, color: 'rgba(66, 153, 225, 0.1)' }
      ])
    } : null
  });
  
  // 对比数据系列
  if (props.compareData.length > 0) {
    series.push({
      name: '对比数据',
      type: 'line',
      data: props.compareData,
      smooth: props.smooth,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        width: 2,
        type: 'dashed',
        color: '#9f7aea'
      },
      itemStyle: {
        color: '#9f7aea'
      }
    });
  }

  const option = {
    title: {
      show: false
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: {
        color: '#4a5568'
      },
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      show: showLegend.value,
      bottom: 0,
      icon: 'circle',
      textStyle: {
        color: '#718096'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: showLegend.value ? '40px' : '10px',
      top: '10px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.labels,
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      },
      axisLabel: {
        color: '#718096'
      },
      splitLine: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#718096'
      },
      splitLine: {
        lineStyle: {
          color: '#e2e8f0',
          type: 'dashed'
        }
      }
    },
    series
  };

  chart.setOption(option);
};

// 处理窗口大小变化
const handleResize = () => {
  if (chart) {
    chart.resize();
  }
};

// 下载图表
const downloadChart = () => {
  if (chart) {
    const dataURL = chart.getDataURL({
      type: 'png',
      pixelRatio: 2,
      backgroundColor: '#fff'
    });
    
    const link = document.createElement('a');
    link.download = `${props.title}.png`;
    link.href = dataURL;
    link.click();
  }
};

// 刷新数据
const refreshData = () => {
  // 向父组件发送刷新事件
  emit('refresh');
};

// 切换图例显示/隐藏
const toggleLegend = () => {
  showLegend.value = !showLegend.value;
  updateChart();
};

// 定义事件
const emit = defineEmits(['refresh']);
</script>

<style scoped>
.chart-card {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
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
  align-items: center;
}

.card-actions i {
  font-size: 14px;
  color: #718096;
}

.chart-container {
  flex: 1;
  min-height: 300px;
  padding: 10px;
}
</style> 