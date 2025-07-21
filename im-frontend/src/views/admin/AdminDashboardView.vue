<template>
  <div class="admin-dashboard">
    <admin-sidebar :activeMenu="activeMenu" @menu-change="handleMenuChange" />
    
    <div class="admin-content">
      <admin-header :title="pageTitle" />
      
      <div class="admin-main">
        <!-- 使用组件切换替代router-view -->
        <admin-home-view v-if="activeMenu === 'dashboard'" />
        <user-management-view v-else-if="activeMenu === 'users'" />
        <div v-else-if="activeMenu === 'groups'" class="placeholder-view">
          <h2>群组管理</h2>
          <p>群组管理功能正在开发中...</p>
        </div>
        <div v-else-if="activeMenu === 'reports'" class="placeholder-view">
          <h2>举报处理</h2>
          <p>举报处理功能正在开发中...</p>
        </div>
        <div v-else-if="activeMenu === 'notifications'" class="placeholder-view">
          <h2>系统通知</h2>
          <p>系统通知功能正在开发中...</p>
        </div>
        <div v-else-if="activeMenu === 'settings'" class="placeholder-view">
          <h2>系统设置</h2>
          <p>系统设置功能正在开发中...</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'
import AdminHeader from '@/components/admin/AdminHeader.vue'
import AdminHomeView from '@/views/admin/AdminHomeView.vue'
import UserManagementView from '@/views/admin/UserManagementView.vue'

const activeMenu = ref('dashboard')

// 根据当前活动菜单设置页面标题
const pageTitle = computed(() => {
  switch (activeMenu.value) {
    case 'dashboard':
      return '系统概览'
    case 'users':
      return '用户管理'
    case 'groups':
      return '群组管理'
    case 'reports':
      return '举报处理'
    case 'notifications':
      return '系统通知'
    case 'settings':
      return '系统设置'
    default:
      return '管理后台'
  }
})

// 处理菜单切换
const handleMenuChange = (menu) => {
  activeMenu.value = menu
}
</script>

<style scoped>
.admin-dashboard {
  display: flex;
  height: 100vh;
  background-color: #f7fafc;
}

.admin-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-main {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

.placeholder-view {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 40px;
  text-align: center;
  color: #718096;
}

.placeholder-view h2 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 24px;
  font-weight: 600;
  color: #2d3748;
}

.placeholder-view p {
  font-size: 16px;
}
</style> 