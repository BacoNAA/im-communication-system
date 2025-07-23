<!-- 饼图组件 -->
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
import { PieChart } from 'echarts/charts';
import { 
  TitleComponent, 
  TooltipComponent, 
  LegendComponent, 
  ToolboxComponent 
} from 'echarts/components';
import { LabelLayout, UniversalTransition } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';

// 注册必要的组件
echarts.use([
  TitleComponent, 
  TooltipComponent, 
  LegendComponent, 
  PieChart,
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
  data: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  // 图表配置
  radius: {
    type: Array,
    default: () => ['50%', '70%']  // 默认是环形图
  },
  center: {
    type: Array,
    default: () => ['50%', '50%']
  },
  roseType: {
    type: String,
    default: ''  // 'radius' 使用南丁格尔图
  },
  colorList: {
    type: Array,
    default: () => [
      '#4299e1', '#48bb78', '#ed8936', '#9f7aea', '#667eea',
      '#38b2ac', '#f6ad55', '#fc8181', '#90cdf4', '#68d391'
    ]
  }
});

// 图表实例
const chartContainer = ref(null);
let chart = null;
const showLegend = ref(true);

// 监听数据变化，更新图表
watch(
  () => [props.data, props.loading],
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

  // 格式化数据
  const formattedData = formatPieData(props.data);

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b} : {c} ({d}%)',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: {
        color: '#4a5568'
      }
    },
    legend: {
      show: showLegend.value,
      orient: 'vertical',
      right: '5%',
      top: 'center',
      icon: 'circle',
      formatter: (name) => {
        // 查找对应数据项
        const item = formattedData.find(dataItem => dataItem.name === name);
        if (!item) return name;
        
        // 计算百分比
        const total = formattedData.reduce((sum, cur) => sum + cur.value, 0);
        const percentage = ((item.value / total) * 100).toFixed(1);
        
        return `${name} (${percentage}%)`;
      },
      textStyle: {
        color: '#718096'
      }
    },
    color: props.colorList,
    series: [
      {
        name: props.title,
        type: 'pie',
        radius: props.radius,
        center: props.center,
        roseType: props.roseType,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.2)'
          }
        },
        data: formattedData
      }
    ]
  };

  chart.setOption(option);
};

// 格式化饼图数据
const formatPieData = (data) => {
  if (Array.isArray(data)) {
    // 如果是数组，检查格式
    if (data.length > 0 && typeof data[0] === 'object') {
      // 如果已经是对象数组，检查属性
      if (data[0].name !== undefined && data[0].value !== undefined) {
        return data;
      }
    }
  } else if (typeof data === 'object') {
    // 如果是对象，将键值对转换为数组
    return Object.entries(data).map(([name, value]) => ({ name, value }));
  }
  
  // 默认返回空数组
  return [];
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