<template>
  <div class="global-search-button-container">
    <!-- 搜索按钮 -->
    <div class="search-bar" @click="openGlobalSearch">
      <i class="fas fa-search"></i>
      <span>搜索消息...</span>
    </div>

    <!-- 全局搜索对话框 -->
    <div v-if="showGlobalSearch" class="global-search-dialog">
      <div class="search-overlay" @click="closeGlobalSearch"></div>
      <div class="search-container">
        <div class="search-header">
          <h2>全局搜索</h2>
          <button class="close-btn" @click="closeGlobalSearch">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <GlobalSearch 
          :visible="showGlobalSearch" 
          :initialKeyword="searchKeyword"
          @navigate-to-message="handleNavigateToMessage"
          @close="closeGlobalSearch"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import GlobalSearch from '@/components/search/GlobalSearch.vue';

// 定义事件
const emit = defineEmits(['navigate-to-message']);

// 全局搜索状态
const showGlobalSearch = ref(false);
const searchKeyword = ref('');

// 打开全局搜索
const openGlobalSearch = () => {
  showGlobalSearch.value = true;
};

// 关闭全局搜索
const closeGlobalSearch = () => {
  showGlobalSearch.value = false;
};

// 处理导航到消息
const handleNavigateToMessage = (message: any) => {
  emit('navigate-to-message', message);
  closeGlobalSearch();
};
</script>

<style scoped>
.global-search-button-container {
  width: 100%;
}
</style> 