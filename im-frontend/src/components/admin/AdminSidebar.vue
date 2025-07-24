<template>
  <div class="admin-sidebar">
    <div class="sidebar-header">
      <div class="logo">
        <h1>IM Admin</h1>
      </div>
    </div>
    
    <div class="sidebar-menu">
      <div 
        class="menu-item" 
        :class="{ active: activeMenu === 'dashboard' }"
        @click="changeMenu('dashboard')"
      >
        <i class="fas fa-tachometer-alt"></i>
        <span>系统概览</span>
      </div>
      
      <div 
        class="menu-item" 
        :class="{ active: activeMenu === 'users' }"
        @click="changeMenu('users')"
      >
        <i class="fas fa-users"></i>
        <span>用户管理</span>
      </div>
      
      <div 
        class="menu-item" 
        :class="{ active: activeMenu === 'groups' }"
        @click="changeMenu('groups')"
      >
        <i class="fas fa-user-friends"></i>
        <span>群组管理</span>
      </div>
      
      <div 
        class="menu-item" 
        :class="{ active: activeMenu === 'reports' }"
        @click="changeMenu('reports')"
      >
        <i class="fas fa-flag"></i>
        <span>举报处理</span>
      </div>
      
      <div 
        class="menu-item" 
        :class="{ active: activeMenu === 'notifications' }"
        @click="changeMenu('notifications')"
      >
        <i class="fas fa-bell"></i>
        <span>系统通知</span>
      </div>
      

    </div>
    
    <div class="sidebar-footer">
      <button class="logout-btn" @click="handleLogout">
        <i class="fas fa-sign-out-alt"></i>
        <span>退出登录</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

// 接收父组件传递的当前活动菜单
const props = defineProps({
  activeMenu: {
    type: String,
    default: 'dashboard'
  }
})

// 定义事件，用于通知父组件菜单切换
const emit = defineEmits(['menu-change'])

const router = useRouter()

// 切换菜单
const changeMenu = (menu) => {
  emit('menu-change', menu)
}

// 处理退出登录
const handleLogout = () => {
  // 清除登录状态
  localStorage.removeItem('accessToken')
  sessionStorage.removeItem('accessToken')
  localStorage.removeItem('adminInfo')
  sessionStorage.removeItem('adminInfo')
  
  // 跳转到登录页面
  router.push('/admin/login')
}
</script>

<style scoped>
.admin-sidebar {
  width: 250px;
  height: 100%;
  background-color: #1a202c;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #2d3748;
}

.logo h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #ffffff;
}

.sidebar-menu {
  flex: 1;
  padding: 20px 0;
  overflow-y: auto;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: #cbd5e0;
  text-decoration: none;
  transition: all 0.3s;
  cursor: pointer;
}

.menu-item:hover {
  background-color: #2d3748;
  color: #ffffff;
}

.menu-item.active {
  background-color: #3182ce;
  color: #ffffff;
}

.menu-item i {
  width: 20px;
  margin-right: 10px;
  text-align: center;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid #2d3748;
}

.logout-btn {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px;
  background-color: #2d3748;
  color: #e2e8f0;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.logout-btn:hover {
  background-color: #e53e3e;
  color: #ffffff;
}

.logout-btn i {
  margin-right: 10px;
}
</style>