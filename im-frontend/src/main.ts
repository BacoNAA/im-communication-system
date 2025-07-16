import './assets/main.css';

import { createApp } from 'vue';
import { createPinia } from 'pinia';

import App from './App.vue';
import router from './router';
import { useAuth } from './composables/useAuth';

const app = createApp(App);

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

app.use(createPinia());
app.use(router);

// 初始化认证状态
const { initAuth } = useAuth();
initAuth();

app.mount('#app');
