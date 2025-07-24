<template>
  <div class="admin-dashboard">
    <admin-sidebar :activeMenu="activeMenu" @menu-change="handleMenuChange" />
    
    <div class="admin-content">
      <admin-header :title="pageTitle" />
      
      <div class="admin-main">
        <!-- 使用组件切换替代router-view -->
        <admin-home-view v-if="activeMenu === 'dashboard'" />
        <user-management-view v-else-if="activeMenu === 'users'" />
        <group-management-view v-else-if="activeMenu === 'groups'" />
        <report-management-view v-else-if="activeMenu === 'reports'" />
        <system-notification-view v-else-if="activeMenu === 'notifications'" />

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
import GroupManagementView from '@/views/admin/GroupManagementView.vue'
import ReportManagementView from '@/views/admin/ReportManagementView.vue'
import SystemNotificationView from '@/views/admin/SystemNotificationView.vue'

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


</style>