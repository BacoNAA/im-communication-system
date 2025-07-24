<template>
  <header class="admin-header">
    <div class="page-title">
      <h2>{{ title }}</h2>
    </div>
    
    <div class="header-actions">

      

      

    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminAuth } from '@/composables/useAdminAuth'

// 接收父组件传递的页面标题
const props = defineProps({
  title: {
    type: String,
    default: '管理后台'
  }
})

const router = useRouter()
const { adminLogout, getAdminInfo } = useAdminAuth()



</script>

<style scoped>
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
  padding: 0 20px;
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
}

.page-title h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a202c;
}

.header-actions {
  display: flex;
  align-items: center;
}






</style>