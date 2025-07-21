import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '@/views/LoginView.vue';
import RegisterView from '@/views/RegisterView.vue';
import DashboardView from '@/views/DashboardView.vue';
import WebSocketTest from '@/components/WebSocketTest.vue';
import MomentView from '@/views/MomentView.vue';
import { getUserSettings } from '@/composables/useUserSettings';

// 管理员相关视图
import AdminLoginView from '@/views/AdminLoginView.vue';
import AdminDashboardView from '@/views/admin/AdminDashboardView.vue';

// 移除重复的代码，直接使用useUserSettings.ts中导出的函数
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: LoginView,
      meta: {
        title: '登录 - IM通信系统'
      }
    },
    {
      path: '/register',
      name: 'Register',
      component: RegisterView,
      meta: {
        title: '注册 - IM通信系统'
      }
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: DashboardView,
      meta: {
        title: '仪表板 - IM通信系统',
        requiresAuth: true
      }
    },
    {
      path: '/websocket-test',
      name: 'WebSocketTest',
      component: WebSocketTest,
      meta: {
        title: 'WebSocket测试 - IM通信系统'
      }
    },
    // 添加朋友圈页面路由配置
    {
      path: '/moments',
      name: 'Moments',
      component: MomentView,
      meta: { requiresAuth: true } // 需要登录才能访问
    },
    
    // 管理员路由
    {
      path: '/admin/login',
      name: 'AdminLogin',
      component: AdminLoginView,
      meta: {
        title: '管理员登录 - IM通信系统'
      }
    },
    {
      path: '/admin/dashboard',
      name: 'AdminDashboard',
      component: AdminDashboardView,
      meta: {
        title: '管理后台 - IM通信系统',
        requiresAdminAuth: true
      }
    }
  ],
});

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title as string;
  }
  
  const hasToken = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
  const hasAdminToken = localStorage.getItem('adminToken');
  
  // 处理根路径访问
  if (to.path === '/') {
    if (hasToken) {
      next('/dashboard');
    } else {
      next('/login');
    }
    return;
  }
  
  // 检查是否需要用户认证
  if (to.meta.requiresAuth) {
    if (!hasToken) {
      next('/login');
      return;
    }
  }
  
  // 检查是否需要管理员认证
  if (to.meta.requiresAdminAuth) {
    if (!hasAdminToken) {
      next('/admin/login');
      return;
    }
  }
  
  // 如果已登录用户访问登录或注册页面，重定向到仪表板
  if ((to.name === 'Login' || to.name === 'Register') && hasToken) {
    next('/dashboard');
    return;
  }
  
  // 如果已登录管理员访问管理员登录页面，重定向到管理后台
  if (to.name === 'AdminLogin' && hasAdminToken) {
    next('/admin/dashboard');
    return;
  }
  
  next();
});

// 路由后置钩子，确保在每次路由变化后应用个性化设置
router.afterEach(() => {
  // 检查用户是否已登录
  const hasToken = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
  if (hasToken) {
    console.log('路由变化，重新应用个性化设置');
    // 延迟执行，确保组件已经渲染
    setTimeout(() => {
      // 使用全局单例实例，避免创建新的未初始化实例
      const { applySettingsToUI } = getUserSettings();
      applySettingsToUI();
    }, 100);
  }
});

export default router;
