import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
// 导入自定义样式，确保它们在Element Plus样式之后，以便可以覆盖Element Plus的样式
import './assets/main.css';
import './assets/theme-variables.css'; // 导入主题变量CSS
import './assets/custom-theme.css'; // 导入自定义主题样式，确保最高优先级
import './assets/admin/admin-styles.css'; // 引入管理员界面样式

import App from './App.vue';
import router from './router';
import { useAuth } from './composables/useAuth';
import { useSharedWebSocket } from './composables/useWebSocket';
import { initTheme } from './utils/themeUtils'; // 导入主题初始化函数

// 初始化主题设置
initTheme();

// 创建应用实例
const app = createApp(App)

// 使用Element Plus
app.use(ElementPlus);

// 注册全局点击外部指令
app.directive('click-outside', {
  mounted(el: HTMLElement, binding: any) {
    console.log('v-click-outside directive mounted');
    const element = el as HTMLElement & { _clickOutside?: (event: Event) => void };
    
    element._clickOutside = (event: Event) => {
      console.log('Click outside event triggered');
      // 检查点击是否在元素外部
      const isOutside = !(el === event.target || el.contains(event.target as Node));
      console.log('Click is outside:', isOutside);
      
      if (isOutside) {
        binding.value(event);
      }
    };
    
    // 使用捕获阶段以确保在其他事件处理程序之前执行
    document.addEventListener('click', element._clickOutside, true);
    console.log('Click outside event listener added');
  },
  unmounted(el: HTMLElement) {
    console.log('v-click-outside directive unmounted');
    const element = el as HTMLElement & { _clickOutside?: (event: Event) => void };
    
    if (element._clickOutside) {
      document.removeEventListener('click', element._clickOutside, true);
      console.log('Click outside event listener removed');
    }
  }
});

// 使用Pinia
app.use(createPinia())

// 使用路由
app.use(router)

// 初始化认证状态
const { initAuth, getToken } = useAuth();
initAuth();

// 初始化WebSocket连接（如果用户已登录）
const { connect: connectWebSocket } = useSharedWebSocket();
const token = getToken();
if (token) {
  console.log('应用启动时检测到用户已登录，立即初始化WebSocket连接');
  connectWebSocket();
}

// 挂载应用
app.mount('#app')
