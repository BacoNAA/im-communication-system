<template>
  <header class="admin-header">
    <div class="page-title">
      <h2>{{ title }}</h2>
    </div>
    
    <div class="header-actions">
      <div class="search-box">
        <input 
          type="text" 
          placeholder="搜索..." 
          v-model="searchQuery"
          @keyup.enter="handleSearch"
        />
        <button @click="handleSearch">
          <i class="fas fa-search"></i>
        </button>
      </div>
      
      <div class="notification-dropdown">
        <button class="notification-btn" @click="toggleNotifications">
          <i class="fas fa-bell"></i>
          <span v-if="unreadCount" class="notification-badge">{{ unreadCount }}</span>
        </button>
        
        <div v-if="showNotifications" class="dropdown-menu">
          <div class="dropdown-header">
            <h3>系统通知</h3>
            <button @click="markAllAsRead">全部已读</button>
          </div>
          
          <div class="notification-list">
            <div v-if="notifications.length === 0" class="empty-notification">
              暂无通知
            </div>
            <div 
              v-for="notification in notifications" 
              :key="notification.id"
              class="notification-item"
              :class="{ unread: !notification.read }"
            >
              <div class="notification-content">
                <div class="notification-title">{{ notification.title }}</div>
                <div class="notification-time">{{ formatTime(notification.time) }}</div>
              </div>
            </div>
          </div>
          
          <div class="dropdown-footer">
            <router-link to="/admin/notifications">查看全部</router-link>
          </div>
        </div>
      </div>
      
      <div class="admin-profile">
        <div class="admin-info" @click="toggleProfileMenu">
          <span class="admin-name">{{ adminName }}</span>
          <i class="fas fa-chevron-down"></i>
        </div>
        
        <div v-if="showProfileMenu" class="dropdown-menu profile-menu">
          <router-link to="/admin/profile" class="dropdown-item">
            <i class="fas fa-user"></i>
            <span>个人资料</span>
          </router-link>
          <div class="dropdown-divider"></div>
          <button class="dropdown-item" @click="handleLogout">
            <i class="fas fa-sign-out-alt"></i>
            <span>退出登录</span>
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

// 接收父组件传递的页面标题
const props = defineProps({
  title: {
    type: String,
    default: '管理后台'
  }
})

const router = useRouter()
const searchQuery = ref('')
const showNotifications = ref(false)
const showProfileMenu = ref(false)

// 模拟数据
const adminName = ref('管理员')
const unreadCount = ref(2)
const notifications = ref([
  { 
    id: 1, 
    title: '系统更新完成', 
    time: new Date(Date.now() - 3600000), 
    read: false 
  },
  { 
    id: 2, 
    title: '新的举报需要处理', 
    time: new Date(Date.now() - 86400000), 
    read: false 
  },
  { 
    id: 3, 
    title: '每周系统报告', 
    time: new Date(Date.now() - 172800000), 
    read: true 
  }
])

// 处理搜索
const handleSearch = () => {
  if (searchQuery.value.trim()) {
    console.log('搜索:', searchQuery.value)
    // 这里可以实现搜索逻辑
  }
}

// 切换通知下拉菜单
const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value
  if (showProfileMenu.value) {
    showProfileMenu.value = false
  }
}

// 切换个人资料下拉菜单
const toggleProfileMenu = () => {
  showProfileMenu.value = !showProfileMenu.value
  if (showNotifications.value) {
    showNotifications.value = false
  }
}

// 标记所有通知为已读
const markAllAsRead = () => {
  notifications.value.forEach(notification => {
    notification.read = true
  })
  unreadCount.value = 0
}

// 格式化时间
const formatTime = (time) => {
  const now = new Date()
  const diff = now - time
  
  if (diff < 3600000) { // 小于1小时
    return '刚刚'
  } else if (diff < 86400000) { // 小于24小时
    return Math.floor(diff / 3600000) + '小时前'
  } else {
    return Math.floor(diff / 86400000) + '天前'
  }
}

// 处理退出登录
const handleLogout = () => {
  localStorage.removeItem('adminToken')
  router.push('/admin/login')
}

// 点击外部关闭下拉菜单
const handleClickOutside = (event) => {
  if (showNotifications.value && !event.target.closest('.notification-dropdown')) {
    showNotifications.value = false
  }
  
  if (showProfileMenu.value && !event.target.closest('.admin-profile')) {
    showProfileMenu.value = false
  }
}

// 添加全局点击事件监听
document.addEventListener('click', handleClickOutside)

// 组件卸载时移除事件监听
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
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

.search-box {
  position: relative;
  margin-right: 20px;
}

.search-box input {
  width: 200px;
  padding: 8px 12px;
  padding-right: 36px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
}

.search-box input:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 2px rgba(49, 130, 206, 0.2);
}

.search-box button {
  position: absolute;
  right: 0;
  top: 0;
  height: 100%;
  width: 36px;
  background: none;
  border: none;
  color: #718096;
  cursor: pointer;
}

.notification-dropdown {
  position: relative;
  margin-right: 20px;
}

.notification-btn {
  position: relative;
  background: none;
  border: none;
  color: #718096;
  font-size: 16px;
  cursor: pointer;
  padding: 8px;
}

.notification-badge {
  position: absolute;
  top: 0;
  right: 0;
  background-color: #e53e3e;
  color: white;
  font-size: 10px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dropdown-menu {
  position: absolute;
  right: 0;
  top: 100%;
  width: 300px;
  background-color: white;
  border-radius: 6px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  z-index: 10;
  overflow: hidden;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
}

.dropdown-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.dropdown-header button {
  background: none;
  border: none;
  color: #3182ce;
  font-size: 12px;
  cursor: pointer;
}

.notification-list {
  max-height: 300px;
  overflow-y: auto;
}

.empty-notification {
  padding: 16px;
  text-align: center;
  color: #718096;
  font-size: 14px;
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f7fafc;
  cursor: pointer;
}

.notification-item:hover {
  background-color: #f7fafc;
}

.notification-item.unread {
  background-color: #ebf8ff;
}

.notification-title {
  font-size: 14px;
  margin-bottom: 4px;
}

.notification-time {
  font-size: 12px;
  color: #718096;
}

.dropdown-footer {
  padding: 12px 16px;
  text-align: center;
  border-top: 1px solid #e2e8f0;
}

.dropdown-footer a {
  color: #3182ce;
  text-decoration: none;
  font-size: 14px;
}

.admin-profile {
  position: relative;
}

.admin-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px;
}

.admin-name {
  margin-right: 8px;
  font-size: 14px;
  font-weight: 500;
}

.profile-menu {
  width: 180px;
}

.dropdown-item {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 16px;
  background: none;
  border: none;
  text-align: left;
  font-size: 14px;
  color: #4a5568;
  cursor: pointer;
  text-decoration: none;
}

.dropdown-item:hover {
  background-color: #f7fafc;
}

.dropdown-item i {
  width: 20px;
  margin-right: 8px;
}

.dropdown-divider {
  height: 1px;
  background-color: #e2e8f0;
  margin: 4px 0;
}
</style> 