import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '@/views/LoginView.vue';
import RegisterView from '@/views/RegisterView.vue';
import DashboardView from '@/views/DashboardView.vue';

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
  
  // 处理根路径访问
  if (to.path === '/') {
    if (hasToken) {
      next('/dashboard');
    } else {
      next('/login');
    }
    return;
  }
  
  // 检查是否需要认证
  if (to.meta.requiresAuth) {
    if (!hasToken) {
      next('/login');
      return;
    }
  }
  
  // 如果已登录用户访问登录或注册页面，重定向到仪表板
  if ((to.name === 'Login' || to.name === 'Register') && hasToken) {
    next('/dashboard');
    return;
  }
  
  next();
});

export default router;
