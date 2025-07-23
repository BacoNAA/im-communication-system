<!-- 统计卡片组件 -->
<template>
  <div class="stat-card">
    <div :class="['stat-icon', iconClass]">
      <i :class="['fas', iconName]"></i>
    </div>
    <div class="stat-content">
      <div class="stat-title">{{ title }}</div>
      <div class="stat-value">{{ formattedValue }}</div>
      <div 
        v-if="showGrowth"
        class="stat-change" 
        :class="growthValue >= 0 ? 'positive' : 'negative'">
        <i :class="growthValue >= 0 ? 'fas fa-arrow-up' : 'fas fa-arrow-down'"></i>
        {{ Math.abs(growthValue).toFixed(1) }}%
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: Number,
    default: 0
  },
  growthValue: {
    type: Number,
    default: 0
  },
  showGrowth: {
    type: Boolean,
    default: true
  },
  iconName: {
    type: String,
    default: 'fa-chart-line'
  },
  iconClass: {
    type: String,
    default: 'default'
  }
});

// 格式化数值，添加千位分隔符
const formattedValue = computed(() => {
  return props.value.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
});
</script>

<style scoped>
.stat-card {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 20px;
  display: flex;
  align-items: center;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
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

.stat-icon.moments {
  background-color: #667eea;
}

.stat-icon.default {
  background-color: #63b3ed;
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
</style> 