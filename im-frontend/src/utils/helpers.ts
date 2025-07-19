// HTML转义函数
export function escapeHtml(text: string): string {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// 格式化消息时间
export function formatMessageTime(date: Date | string): string {
  const targetDate = typeof date === 'string' ? new Date(date) : date;
  const now = new Date();
  
  // 如果是今天，只显示时间
  if (targetDate.toDateString() === now.toDateString()) {
    return targetDate.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
  
  // 如果是昨天
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (targetDate.toDateString() === yesterday.toDateString()) {
    return '昨天 ' + targetDate.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
  
  // 如果是本周内
  const weekStart = new Date(now);
  weekStart.setDate(now.getDate() - now.getDay());
  if (targetDate >= weekStart) {
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return weekdays[targetDate.getDay()] + ' ' + targetDate.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
  
  // 其他情况显示完整日期
  return targetDate.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric'
  }) + ' ' + targetDate.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

// 格式化相对时间
export function formatRelativeTime(date: Date): string {
  const now = new Date();
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);
  
  // 小于1分钟
  if (diffInSeconds < 60) {
    return '刚刚';
  }
  
  // 小于1小时
  const diffInMinutes = Math.floor(diffInSeconds / 60);
  if (diffInMinutes < 60) {
    return `${diffInMinutes}分钟前`;
  }
  
  // 小于24小时
  const diffInHours = Math.floor(diffInMinutes / 60);
  if (diffInHours < 24) {
    return `${diffInHours}小时前`;
  }
  
  // 小于30天
  const diffInDays = Math.floor(diffInHours / 24);
  if (diffInDays < 30) {
    return `${diffInDays}天前`;
  }
  
  // 大于30天，显示具体日期
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  
  // 如果是今年，只显示月-日
  if (year === now.getFullYear()) {
    return `${month}-${day}`;
  }
  
  // 不是今年，显示年-月-日
  return `${year}-${month}-${day}`;
}

// 获取头像文本（显示名称的首字母或首个emoji）
export function getAvatarText(name: string | undefined): string {
  if (!name) return '?';
  
  // 检查是否为表情符号开头
  const emojiRegex = /^[\p{Emoji}]/u;
  if (emojiRegex.test(name)) {
    return name.match(emojiRegex)?.[0] || name.charAt(0);
  }
  
  // 取首字母
  return name.charAt(0).toUpperCase();
}

// 格式化会话时间
export function formatConversationTime(timestamp: string | number | Date): string {
  if (!timestamp) return '';
  
  const date = new Date(timestamp);
  const now = new Date();
  
  // 今天
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  
  // 昨天
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天';
  }
  
  // 本周
  const dayDiff = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));
  if (dayDiff < 7) {
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return weekdays[date.getDay()] ?? '未知';
  }
  
  // 本年
  if (date.getFullYear() === now.getFullYear()) {
    return `${date.getMonth() + 1}月${date.getDate()}日`;
  }
  
  // 其他
  return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;
}

// 生成随机颜色
export function generateRandomColor(): string {
  const colors = [
    '#667eea', '#764ba2', '#f093fb', '#4facfe',
    '#43e97b', '#38f9d7', '#ffecd2', '#fcb69f',
    '#a8edea', '#fed6e3', '#d299c2', '#fef9d7',
    '#667eea', '#f093fb', '#4facfe', '#43e97b'
  ];
  
  const randomIndex = Math.floor(Math.random() * colors.length);
  const selectedColor = colors[randomIndex];
  
  // 确保返回值不为undefined，提供默认颜色
  return selectedColor ?? '#667eea';
}

// 验证邮箱格式
export function validateEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

// 验证手机号格式
export function validatePhone(phone: string): boolean {
  const phoneRegex = /^1[3-9]\d{9}$/;
  return phoneRegex.test(phone);
}

// 验证用户名格式
export function validateUsername(username: string): boolean {
  // 用户名：4-20位，字母、数字、下划线
  const usernameRegex = /^[a-zA-Z0-9_]{4,20}$/;
  return usernameRegex.test(username);
}

// 验证密码强度
export function validatePassword(password: string): { valid: boolean; message?: string } {
  if (password.length < 6) {
    return { valid: false, message: '密码长度至少6位' };
  }
  
  if (password.length > 20) {
    return { valid: false, message: '密码长度不能超过20位' };
  }
  
  // 检查是否包含字母和数字
  const hasLetter = /[a-zA-Z]/.test(password);
  const hasNumber = /\d/.test(password);
  
  if (!hasLetter || !hasNumber) {
    return { valid: false, message: '密码必须包含字母和数字' };
  }
  
  return { valid: true };
}

// 文件大小格式化
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 防抖函数
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: number;
  
  return function executedFunction(...args: Parameters<T>) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    
    clearTimeout(timeout);
    timeout = setTimeout(later, wait) as number;
  };
}

// 节流函数
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle: boolean;
  
  return function executedFunction(this: any, ...args: Parameters<T>) {
    if (!inThrottle) {
      func.apply(this, args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
}

// 深拷贝函数
export function deepClone<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') {
    return obj;
  }
  
  if (obj instanceof Date) {
    return new Date(obj.getTime()) as T;
  }
  
  if (obj instanceof Array) {
    return obj.map(item => deepClone(item)) as T;
  }
  
  if (typeof obj === 'object') {
    const clonedObj = {} as T;
    for (const key in obj) {
      if (obj.hasOwnProperty(key)) {
        clonedObj[key] = deepClone(obj[key]);
      }
    }
    return clonedObj;
  }
  
  return obj;
}

// 获取文件扩展名
export function getFileExtension(filename: string): string {
  return filename.slice((filename.lastIndexOf('.') - 1 >>> 0) + 2);
}

// 检查是否为图片文件
export function isImageFile(filename: string): boolean {
  const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];
  const extension = getFileExtension(filename).toLowerCase();
  return imageExtensions.includes(extension);
}

// 生成唯一ID
export function generateUniqueId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substr(2);
}

// 复制文本到剪贴板
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    } else {
      // 降级方案
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      const result = document.execCommand('copy');
      textArea.remove();
      return result;
    }
  } catch (error) {
    console.error('复制到剪贴板失败:', error);
    return false;
  }
}

// URL参数解析
export function parseUrlParams(url: string): Record<string, string> {
  const params: Record<string, string> = {};
  const urlObj = new URL(url);
  
  urlObj.searchParams.forEach((value, key) => {
    params[key] = value;
  });
  
  return params;
}

// 构建URL参数
export function buildUrlParams(params: Record<string, any>): string {
  const searchParams = new URLSearchParams();
  
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined) {
      searchParams.append(key, String(value));
    }
  });
  
  return searchParams.toString();
}

// 移动设备检测
export function isMobileDevice(): boolean {
  const userAgent = navigator.userAgent || navigator.vendor || (window as any).opera;
  
  // 检测移动设备的用户代理字符串
  const mobileRegex = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i;
  
  // 检测屏幕宽度
  const screenWidth = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
  
  return mobileRegex.test(userAgent) || screenWidth <= 768;
}

// 获取URL参数
export function getUrlParameter(name: string): string | null {
  const urlParams = new URLSearchParams(window.location.search);
  return urlParams.get(name);
}

// 设置URL参数
export function setUrlParameter(name: string, value: string): void {
  const url = new URL(window.location.href);
  url.searchParams.set(name, value);
  window.history.replaceState({}, '', url.toString());
}

/**
 * 获取当前用户ID
 * @returns 当前用户ID，如果未登录返回0
 */
export function getCurrentUserId(): number {
  try {
    // 添加日志以便调试
    console.log('getCurrentUserId被调用');
    
    // 尝试从localStorage获取
    const userStr = localStorage.getItem('current_user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        if (user && user.id) {
          const userId = typeof user.id === 'string' ? parseInt(user.id, 10) : user.id;
          console.log('从localStorage的current_user获取到用户ID:', userId);
          return userId || 0;
        }
      } catch (e) {
        console.error('解析localStorage中的current_user失败:', e);
      }
    }
    
    // 尝试从sessionStorage获取
    const sessionUserStr = sessionStorage.getItem('current_user');
    if (sessionUserStr) {
      try {
        const user = JSON.parse(sessionUserStr);
        if (user && user.id) {
          const userId = typeof user.id === 'string' ? parseInt(user.id, 10) : user.id;
          console.log('从sessionStorage的current_user获取到用户ID:', userId);
          return userId || 0;
        }
      } catch (e) {
        console.error('解析sessionStorage中的current_user失败:', e);
      }
    }
    
    // 尝试从userInfo获取
    const userInfoStr = localStorage.getItem('userInfo');
    if (userInfoStr) {
      try {
        const user = JSON.parse(userInfoStr);
        if (user && user.id) {
          const userId = typeof user.id === 'string' ? parseInt(user.id, 10) : user.id;
          console.log('从localStorage的userInfo获取到用户ID:', userId);
          return userId || 0;
        }
      } catch (e) {
        console.error('解析localStorage中的userInfo失败:', e);
      }
    }
    
    // 尝试从sessionStorage的userInfo获取
    const sessionUserInfoStr = sessionStorage.getItem('userInfo');
    if (sessionUserInfoStr) {
      try {
        const user = JSON.parse(sessionUserInfoStr);
        if (user && user.id) {
          const userId = typeof user.id === 'string' ? parseInt(user.id, 10) : user.id;
          console.log('从sessionStorage的userInfo获取到用户ID:', userId);
          return userId || 0;
        }
      } catch (e) {
        console.error('解析sessionStorage中的userInfo失败:', e);
      }
    }
    
    // 尝试从localStorage的userId直接获取
    const directUserId = localStorage.getItem('userId');
    if (directUserId) {
      const userId = parseInt(directUserId, 10);
      if (!isNaN(userId)) {
        console.log('从localStorage的userId直接获取到用户ID:', userId);
        return userId;
      }
    }
    
    // 尝试从sessionStorage的userId直接获取
    const sessionDirectUserId = sessionStorage.getItem('userId');
    if (sessionDirectUserId) {
      const userId = parseInt(sessionDirectUserId, 10);
      if (!isNaN(userId)) {
        console.log('从sessionStorage的userId直接获取到用户ID:', userId);
        return userId;
      }
    }
    
    console.warn('无法从任何存储中获取用户ID，返回0');
    return 0;
  } catch (e) {
    console.error('获取用户ID过程中出错:', e);
    return 0;
  }
}